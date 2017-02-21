/** Service
 *
 * Loads init config and records PLC data.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.controls.*;
import javaforce.*;

public class Service {
  private static ArrayList<Tag> tags = new ArrayList<Tag>();
  private static Object tagsLock = new Object();

  public static String dataPath;
  public static String databaseName = "jfdataloggerplus";
  public static String logsPath;
  public static String derbyURI;

  private static TagListener listener = new TagListener() {
    public void tagChanged(Tag tag) {
      logChange(tag, tag.getValue());
    }
  };

  public static Tag[] getTags() {
    synchronized (tagsLock) {
      return tags.toArray(new Tag[tags.size()]);
    }
  }
  public static void addTag(Tag tag) {
    tag.setListener(listener);
    synchronized (tagsLock) {
      tags.add(tag);
      SQL sql = new SQL();
      sql.connect(derbyURI);
      String query = String.format("insert into tags (host,type,tag,size,color,minvalue,maxvalue,delay) values ('%s','%s','%s','%s',%d,'%s','%s',%d)", tag.host, tag.type, tag.tag, tag.size, tag.color, tag.getmin(), tag.getmax(), tag.delay);
      sql.execute(query);
      if (sql.lastException != null) {
        JFLog.log("query=" + query);
        JFLog.log(sql.lastException);
      }
      query = String.format("select id from tags where host='%s' and tag='%s'", tag.host, tag.tag);
      String id = sql.select1value(query);
      tag.setData("id", JF.atoi(id));
      sql.close();
      restart();
    }
  }
  public static boolean exists(String host, String tag) {
    Tag[] list = getTags();
    for(int a=0;a<list.length;a++) {
      Tag t = list[a];
      if (t.host.equals(host) && t.tag.equals(tag)) return true;
    }
    return false;
  }
  public static void removeTag(Tag tag) {
    synchronized (tagsLock) {
      tag.stop();
      tags.remove(tag);
      SQL sql = new SQL();
      sql.connect(derbyURI);
      String query = String.format("delete from tags where id=%d", tag.getData("id"));
      sql.execute(query);
      query = String.format("delete from history where id=%d", tag.getData("id"));
      sql.execute(query);
      sql.close();
      restart();
    }
  }
  public static void restart() {
    synchronized(tagsLock) {
      Tag tags[] = getTags();
      //stop all tags
      for(int a=0;a<tags.length;a++) {
        tags[a].stop();
      }
      //start all tags
      for(int a=0;a<tags.length;a++) {
        Tag parent = null;
        for(int b=0;b<tags.length;b++) {
          if (tags[a].host.equals(tags[b].host)) {
            parent = tags[b];
            break;
          }
        }
        tags[a].start(parent);
      }
    }
  }
  public static void updateTag(Tag tag) {
    synchronized(tagsLock) {
      restart();
    }
  }
  public static void logMsg(String msg) {
    JFLog.log(msg);
  }
  public static void logChange(Tag tag, String value) {
    if (value == null || value.equals("error")) return;
    SQL sql = new SQL();
    sql.connect(derbyURI);
    String query = String.format("insert into history (id, value, when) values (%s,'%s',current_timestamp)", tag.getData("id"), value);
    sql.execute(query);
    if (sql.lastException != null) {
      JFLog.log(sql.lastException);
    }
    sql.close();
  }
  public static String[][] queryHistory(String query) {
    SQL sql = new SQL();
    sql.connect(derbyURI);
    String out[][] = sql.select(query);
    if (sql.lastException != null) {
      JFLog.log(sql.lastException);
    }
    sql.close();
    return out;
  }
  private static void initDB() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfdataloggerplus";
    } else {
      dataPath = "/var/jfdataloggerplus";
    }
    logsPath = dataPath + "/logs";
    derbyURI = "jdbc:derby:jfdataloggerplus";

    new File(logsPath).mkdirs();
    JFLog.append(logsPath + "/service.log", true);
    System.setProperty("derby.system.home", dataPath);
    if (!new File(dataPath + "/" + databaseName + "/service.properties").exists()) {
      //create database
      createDB();
    } else {
      //update database if required
      SQL sql = new SQL();
      sql.connect(derbyURI);
      String version = sql.select1value("select value from config where id='version'");
      JFLog.log("DB version=" + version);
      if (version.equals("0.0")) {
        //drop old tables - sorry no upgrade
        sql.execute("drop table tags");
        sql.execute("delete from history");
        createDB();
        sql.execute("update config set value='0.1' where id='version'");
      }
      sql.close();
    }
  }
  private static void createDB() {
    SQL sql = new SQL();
    JFLog.log("DB creating...");
    sql.connect(derbyURI + ";create=true");
    //create tables
    sql.execute("create table tags (id int not null generated always as identity (start with 1, increment by 1) primary key, host varchar(64), type varchar(32), tag varchar(128), size varchar(32), color int, minvalue varchar(32), maxvalue varchar(32), delay int, unique (host, tag))");
    sql.execute("create table config (id varchar(32), value varchar(128))");
    sql.execute("create table history (id int, value varchar(128), when timestamp)");
    sql.execute("insert into config (id, value) values ('version', '0.1')");
    sql.close();
  }
  public static void start() {
    //load init tags from database
    initDB();
    SQL sql = new SQL();
    sql.connect(derbyURI);
    String query[][] = sql.select("select id,host,type,tag,size,color,minvalue,maxvalue,delay from tags");
    sql.close();
    if (query == null) return;
    for(int a=0;a<query.length;a++) {
      Tag tag = new Tag();
      tags.add(tag);
      tag.setListener(listener);
      tag.setData("id", query[a][0]);
      tag.host = query[a][1];
      switch (query[a][2]) {
        case "S7": tag.type = Controller.types.S7; break;
        case "AB": tag.type = Controller.types.AB; break;
        case "MB": tag.type = Controller.types.MB; break;
        case "NI": tag.type = Controller.types.NI; break;
      }
      tag.tag = query[a][3];
      switch (query[a][4]) {
        case "bit": tag.size = Controller.sizes.bit; break;
        case "int8": tag.size = Controller.sizes.int8; break;
        case "int16": tag.size = Controller.sizes.int16; break;
        case "int32": tag.size = Controller.sizes.int32; break;
        case "float32": tag.size = Controller.sizes.float32; break;
        case "float64": tag.size = Controller.sizes.float64; break;
      }
      tag.color = JF.atoi(query[a][5]);
      if (tag.isFloat()) {
        tag.fmin = JF.atof(query[a][6]);
        tag.fmax = JF.atof(query[a][7]);
      } else {
        tag.min = JF.atoi(query[a][6]);
        tag.max = JF.atoi(query[a][7]);
      }
      tag.delay = JF.atoi(query[a][8]);
    }
    restart();
  }
  public static void stop() {
    synchronized (tagsLock) {
      Tag list[] = getTags();
      for(int a=0;a<list.length;a++) {
        list[a].stop();
      }
      tags.clear();
    }
  }
}

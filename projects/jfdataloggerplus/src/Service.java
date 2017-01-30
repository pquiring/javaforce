/** Service
 *
 * Loads init config and records PLC data.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.webui.*;
import javaforce.*;

public class Service {
  private static ArrayList<Tag> tags = new ArrayList<Tag>();
  private static Object tagsLock = new Object();

  public static String dataPath;
  public static String databaseName = "jfcontrols";
  public static String logsPath;
  public static String derbyURI;

  public static Tag[] getTags() {
    synchronized (tagsLock) {
      return tags.toArray(new Tag[tags.size()]);
    }
  }
  public static void addTag(Tag tag) {
    synchronized (tagsLock) {
      tags.add(tag);
      tag.start();
    }
  }
  public static void removeTag(Tag tag) {
    synchronized (tagsLock) {
      tag.stop();
      tags.remove(tag);
    }
  }
  public static void updateTag(Tag tag) {
    synchronized (tagsLock) {
      tag.stop();
      tag.start();
    }
  }
  public static void logMsg(String msg) {
    JFLog.log(msg);
  }
  public static void logChange(Tag tag, String value) {
    SQL sql = new SQL();
    sql.connect(derbyURI);
    String query = String.format("insert into history (id, value, when) values (%d,'%s',current_timestamp)", tag.id, value);
    sql.execute(query);
    sql.close();
  }
  public static String[][] queryHistory(String query) {
    SQL sql = new SQL();
    sql.connect(derbyURI);
    String out[][] = sql.select(query);
    sql.close();
    return out;
  }
  private static void initDB() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfdataloggerplus";
    } else {
      dataPath = "/var/jfdataloggerplus";
    }
    logsPath = dataPath + "/logs/service.log";
    derbyURI = "jdbc:derby:jfdataloggerplus";

    new File(logsPath).mkdirs();
    JFLog.init(logsPath, true);
    System.setProperty("derby.system.home", dataPath);
    if (!new File(dataPath + "/" + databaseName + "/service.properties").exists()) {
      //create database
      SQL sql = new SQL();
      sql.connect(derbyURI + ";create=true");
      //create tables
      sql.execute("create table tags (id int primary key, host varchar(64), type int, tag varchar(128), size int, color int, min varchar(32), max varchar(32), delay int)");
      sql.execute("create table config (id varchar(32), value varchar(128))");
      sql.execute("create table history (id int, value varchar(128), when timestamp)");
      sql.execute("insert into config (id, value) values ('version', '0.0')");
      sql.close();
    } else {
      //update database if required
      SQL sql = new SQL();
      sql.connect(derbyURI);
      //TODO
      sql.close();
    }
  }
  public static void start() {
    //load init tags from database
    initDB();
    SQL sql = new SQL();
    sql.connect(derbyURI);
    String query[][] = sql.select("select id,host,type,tag,size,color,min,max,delay from tags");
    sql.close();
    for(int a=0;a<query.length;a++) {
      Tag tag = new Tag();
      tag.id = JF.atoi(query[a][0]);
      tag.host = query[a][1];
      tag.type = Tag.types.values()[JF.atoi(query[a][2])];
      tag.tag = query[a][3];
      tag.size = Tag.sizes.values()[JF.atoi(query[a][4])];
      tag.color = JF.atox(query[a][5]);
      if (tag.isFloat()) {
        tag.fmin = JF.atof(query[a][6]);
        tag.fmax = JF.atof(query[a][7]);
      } else {
        tag.min = JF.atoi(query[a][6]);
        tag.max = JF.atoi(query[a][7]);
      }
      tag.delay = JF.atoi(query[a][8]);
      addTag(tag);
    }
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

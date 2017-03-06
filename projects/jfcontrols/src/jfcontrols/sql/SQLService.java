package jfcontrols.sql;

/** SQL Service
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class SQLService {
  public static String dataPath;
  public static String databaseName = "jfcontrols";
  public static String logsPath;
  public static String derbyURI;

  public static SQL getSQL() {
    SQL sql = new SQL();
    sql.connect(derbyURI);
    return sql;
  }

  private static void initDB() {
    if (JF.isWindows()) {
      dataPath = System.getenv("ProgramData") + "/jfcontrols";
    } else {
      dataPath = "/var/jfcontrols";
    }
    logsPath = dataPath + "/logs";
    derbyURI = "jdbc:derby:jfcontrols";

    new File(logsPath).mkdirs();
    JFLog.append(logsPath + "/service.log", true);
    System.setProperty("derby.system.home", dataPath);
    if (!new File(dataPath + "/" + databaseName + "/service.properties").exists()) {
      //create database
      createDB();
    } else {
      SQL sql = getSQL();
      //update database if required
      String version = sql.select1value("select value from config where id='version'");
      JFLog.log("DB version=" + version);
      sql.close();
    }
  }
  private static void createDB() {
    SQL sql = new SQL();
    JFLog.log("DB creating...");
    sql.connect(derbyURI + ";create=true");
    //create tables
    sql.execute("create table conts (id int not null generated always as identity (start with 1, increment by 1) primary key)");
    sql.execute("create table tags (id int not null generated always as identity (start with 1, increment by 1) primary key)");
    sql.execute("create table panels (id int not null generated always as identity (start with 1, increment by 1) primary key)");
    sql.execute("create table cells (id int not null generated always as identity (start with 1, increment by 1) primary key, pid int)");
    sql.execute("create table funcs (id int not null generated always as identity (start with 1, increment by 1) primary key)");
    sql.execute("create table rungs (id int not null generated always as identity (start with 1, increment by 1) primary key, fid int)");
    sql.execute("create table config (id varchar(32), value varchar(512))");
    sql.execute("insert into config (id, value) values ('version', '0.1')");
    if (!JF.isWindows()) {
      sql.execute("insert into config (id, value) values ('ip_addr', '10.1.1.10')");
      sql.execute("insert into config (id, value) values ('ip_mask', '255.255.255.0')");
      sql.execute("insert into config (id, value) values ('ip_gateway', '10.1.1.1')");
      sql.execute("insert into config (id, value) values ('ip_dns', '8.8.8.8')");
    }
    sql.close();
  }
  public static void start() {
    initDB();
  }
  public static void stop() {
    //TODO
  }
}

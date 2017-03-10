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
    sql.execute("create table panels (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(32), builtin boolean)");
    sql.execute("create table cells (id int not null generated always as identity (start with 1, increment by 1) primary key, pid int, x int, y int, w int, h int, name varchar(32), text varchar(512), tag varchar(32), func varchar(32), arg varchar(32), style varchar(512))");
    sql.execute("create table funcs (id int not null generated always as identity (start with 1, increment by 1) primary key)");
    sql.execute("create table rungs (id int not null generated always as identity (start with 1, increment by 1) primary key, fid int)");
    sql.execute("create table users (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(32), pass varchar(32))");
    sql.execute("insert into users (name, pass) values ('admin', 'admin')");
    sql.execute("insert into users (name, pass) values ('oper', 'oper')");
    sql.execute("create table config (id varchar(32), value varchar(512))");
    sql.execute("insert into config (id, value) values ('version', '0.1')");
    if (!JF.isWindows()) {
      sql.execute("insert into config (id, value) values ('ip_addr', '10.1.1.10')");
      sql.execute("insert into config (id, value) values ('ip_mask', '255.255.255.0')");
      sql.execute("insert into config (id, value) values ('ip_gateway', '10.1.1.1')");
      sql.execute("insert into config (id, value) values ('ip_dns', '8.8.8.8')");
    }
    //create builtin panels
    sql.execute("insert into panels (name, builtin) values ('_jfc_login', true)");
    String pid = sql.select1value("select id from panels where name='_jfc_login'");
    sql.execute("insert into cells (pid,x,y,w,h,name,text) values (" + pid + ",1,1,3,1,'label','Username:')");
    sql.execute("insert into cells (pid,x,y,w,h,name,text) values (" + pid + ",5,1,3,1,'textfield','')");
    sql.execute("insert into cells (pid,x,y,w,h,name,text) values (" + pid + ",1,3,3,1,'label','Password:')");
    sql.execute("insert into cells (pid,x,y,w,h,name,text) values (" + pid + ",5,3,3,1,'textfield','')");
    sql.execute("insert into cells (pid,x,y,w,h,name,text,func) values (" + pid + ",1,5,3,1,'button','Login','_jfc_login_ok')");
    sql.execute("insert into cells (pid,x,y,w,h,name,text,func) values (" + pid + ",5,5,3,1,'button','Cancel','_jfc_login_cancel')");
    sql.execute("insert into panels (name, builtin) values ('main', false)");
    pid = sql.select1value("select id from panels where name='main'");
    sql.execute("insert into cells (pid,x,y,w,h,name,text,func) values (" + pid + ",0,0,1,1,'button','X','showMenu')");
    sql.execute("insert into cells (pid,x,y,w,h,name,text) values (" + pid + ",1,1,7,1,'label','Welcome to jfControls!')");
    sql.execute("insert into cells (pid,x,y,w,h,name,text) values (" + pid + ",1,3,10,1,'label','Click on the Menu Icon in the top left corner to get started.')");
    sql.close();
  }
  public static void start() {
    initDB();
  }
  public static void stop() {
    //TODO
  }
}

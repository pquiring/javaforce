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
  public static String dbVersion = "0.1B";

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
      if (!version.equals(dbVersion)) {
        //TODO : upgrade database
      }
      sql.close();
    }
  }
  private static void createDB() {
    String id;
    SQL sql = new SQL();
    JFLog.log("DB creating...");
    sql.connect(derbyURI + ";create=true");
    //create tables
    sql.execute("create table tags (id int not null generated always as identity (start with 1, increment by 1) primary key)");
    sql.execute("create table panels (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(32) unique, popup boolean)");
    sql.execute("create table cells (id int not null generated always as identity (start with 1, increment by 1) primary key, pid int, x int, y int, w int, h int,comp  varchar(32), name varchar(32), text varchar(512), tag varchar(32), func varchar(32), arg varchar(32), style varchar(512))");
    sql.execute("create table funcs (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(32) unique)");
    sql.execute("create table rungs (id int not null generated always as identity (start with 1, increment by 1) primary key, fid int, logic varchar(32000))");
    sql.execute("create table users (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(32), pass varchar(32))");
    sql.execute("create table lists (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(32) unique)");
    sql.execute("create table listdata (id int not null generated always as identity (start with 1, increment by 1) primary key, lid int, value int, text varchar(128))");
    sql.execute("create table config (id varchar(32) unique, value varchar(512))");

    //create users
    sql.execute("insert into users (name, pass) values ('admin', 'admin')");
    sql.execute("insert into users (name, pass) values ('oper', 'oper')");
    //create default config
    sql.execute("insert into config (id, value) values ('version', '" + dbVersion + "')");
    if (!JF.isWindows()) {
      sql.execute("insert into config (id, value) values ('ip_addr', '10.1.1.10')");
      sql.execute("insert into config (id, value) values ('ip_mask', '255.255.255.0')");
      sql.execute("insert into config (id, value) values ('ip_gateway', '10.1.1.1')");
      sql.execute("insert into config (id, value) values ('ip_dns', '8.8.8.8')");
    }
    //create lists
    sql.execute("insert into lists (name) values ('jfc_ctrl_type')");
    id = sql.select1value("select id from lists where name='jfc_ctrl_type'");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",0,'JFC')");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",1,'S7')");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",2,'AB')");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",3,'MB')");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",4,'NI')");
    sql.execute("insert into lists (name) values ('jfc_ctrl_speed')");
    id = sql.select1value("select id from lists where name='jfc_ctrl_speed'");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",0,'Auto')");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",1,'1s')");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",2,'100ms')");
    sql.execute("insert into listdata (lid,value,text) values (" +  id + ",3,'10ms')");
    //create panels
    sql.execute("insert into panels (name, popup) values ('jfc_login', true)");
    id = sql.select1value("select id from panels where name='jfc_login'");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,0,3,1,'label','','Username:')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,0,3,1,'textfield','user','')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,2,3,1,'label','','Password:')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,2,3,1,'textfield','pass','')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,3,8,1,'label','errmsg','')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,4,3,1,'button','','Login','jfc_login_ok')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",4,4,3,1,'button','','Cancel','jfc_login_cancel')");

    sql.execute("insert into panels (name, popup) values ('_main', false)");
    id = sql.select1value("select id from panels where name='_main'");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",1,1,7,1,'label','','Welcome to jfControls!')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",1,3,12,1,'label','','Click on the Menu Icon in the top left corner to get started.')");

    sql.execute("insert into panels (name, popup) values ('jfc_main', true)");
    id = sql.select1value("select id from panels where name='jfc_main'");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,0,3,1,'button','','Main Panel','setPanel','_main')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,1,3,1,'button','','Controllers','setPanel', 'jfc_controllers')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,2,3,1,'button','','Tags','setPanel','jfc_tags')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,3,3,1,'button','','Panels','setPanel','jfc_panels')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,4,3,1,'button','','Functions','setPanel','jfc_funcs')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,5,3,1,'button','','Config','setPanel','jfc_config')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func) values ("     + id + ",0,6,3,1,'button','','Logoff','jfc_logout')");

    sql.execute("insert into panels (name, popup) values ('jfc_controllers', false)");
    id = sql.select1value("select id from panels where name='jfc_controllers'");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,3,1,'label','','IP')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",5,1,2,1,'label','','Type')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",7,1,2,1,'label','','Speed')");
    sql.execute("insert into cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",10,0,3,1,'button','','Save','jfc_ctrl_save')");
    for(int a=1;a<=10;a++) {
      sql.execute("insert into cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0," + (a+1) + ",2,1,'label','','C" + a + "')");
      sql.execute("insert into cells (pid,x,y,w,h,comp,name,tag) values (" +  id + ",2," + (a+1) + ",3,1,'textfield','c" + a + "_ip','jfc_c" + a + "_ip')");
      sql.execute("insert into cells (pid,x,y,w,h,comp,name,tag,arg) values (" +  id + ",5," + (a+1) + ",2,1,'combobox','c" + a + "_type','jfc_c" + a + "_type','jfc_ctrl_type')");
      sql.execute("insert into cells (pid,x,y,w,h,comp,name,tag,arg) values (" +  id + ",7," + (a+1) + ",2,1,'combobox','c" + a + "_speed','jfc_c" + a + "_speed','jfc_ctrl_speed')");
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

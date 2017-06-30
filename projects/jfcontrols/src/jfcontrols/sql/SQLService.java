package jfcontrols.sql;

/** SQL Service
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.controls.*;

import jfcontrols.app.*;
import jfcontrols.tags.*;

public class SQLService {
  public static String derbyURI;
  public static String databaseName = "database";
  public static String dbVersion = "0.0.1";
  public static boolean running;

  public static SQL getSQL() {
    SQL sql = new SQL();
    sql.connect(derbyURI);
    return sql;
  }

  private static void initDB() {
    derbyURI = "jdbc:derby:database";
    System.setProperty("derby.system.home", Paths.dataPath);
    SQL.initClass(SQL.derbySQL);

    if (!new File(Paths.dataPath + "/" + databaseName + "/service.properties").exists()) {
      //create database
      createDB();
    } else {
      SQL sql = getSQL();
      //update database if required
      String version = sql.select1value("select value from jfc_config where id='version'");
      JFLog.log("DB version=" + version);
      if (!version.equals(dbVersion)) {
        //TODO : upgrade database
      }
      sql.close();
    }
    running = true;
  }
  private static void createDB() {
    String id;
    SQL sql = new SQL();
    JFLog.log("DB creating...");
    sql.connect(derbyURI + ";create=true");
    //create tables
    sql.execute("create table jfc_ctrls (id int not null generated always as identity (start with 1, increment by 1) primary key, cid int unique, ip varchar(32), type int, speed int)");
    sql.execute("create table jfc_tags (id int not null generated always as identity (start with 1, increment by 1) primary key, cid int, name varchar(64), type int, array boolean, unsigned boolean, comment varchar(64), builtin boolean, unique (cid, name))");
    sql.execute("create table jfc_tagvalues (id int not null generated always as identity (start with 1, increment by 1) primary key, tid int, idx int, mid int, midx int, value varchar(128))");
    sql.execute("create table jfc_iocomments (id int not null generated always as identity (start with 1, increment by 1) primary key, mid int, idx int, comment varchar(64), unique (mid, idx))");
    sql.execute("create table jfc_udts (id int not null generated always as identity (start with 1, increment by 1) primary key, uid int, name varchar(64) unique)");
    sql.execute("create table jfc_udtmems (id int not null generated always as identity (start with 1, increment by 1) primary key, uid int, mid int, name varchar(64), type int, array boolean, unsigned boolean, comment varchar(64), builtin boolean)");
    sql.execute("create table jfc_panels (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(64) unique, display varchar(64), popup boolean, builtin boolean)");
    sql.execute("create table jfc_cells (id int not null generated always as identity (start with 1, increment by 1) primary key, pid int, x int, y int, w int, h int,comp varchar(64), name varchar(64), text varchar(512), tag varchar(64), func varchar(64), arg varchar(64), style varchar(512), events varchar(1024))");
    sql.execute("create table jfc_funcs (id int not null generated always as identity (start with 1, increment by 1) primary key, cid int, name varchar(64) unique, revision bigint, comment varchar(8192))");
    sql.execute("create table jfc_rungs (id int not null generated always as identity (start with 1, increment by 1) primary key, fid int, rid int, comment varchar(512), logic varchar(16384))");
    sql.execute("create table jfc_blocks (id int not null generated always as identity (start with 1, increment by 1) primary key, fid int, rid int, bid int, name varchar(64), tags varchar(512))");
    sql.execute("create table jfc_users (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(64) unique, pass varchar(64))");
    sql.execute("create table jfc_lists (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(64) unique)");
    sql.execute("create table jfc_listdata (id int not null generated always as identity (start with 1, increment by 1) primary key, lid int, value int, text varchar(128))");
    sql.execute("create table jfc_config (id varchar(64) unique, value varchar(512))");
    sql.execute("create table jfc_alarmhistory (id int not null generated always as identity (start with 1, increment by 1) primary key, idx int, when varchar(22))");
    sql.execute("create table jfc_logics (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(64) unique, shortname varchar(64), gid varchar(64))");
    sql.execute("create table jfc_watch (id int not null generated always as identity (start with 1, increment by 1) primary key, name varchar(64) unique)");
    sql.execute("create table jfc_watchtags (id int not null generated always as identity (start with 1, increment by 1) primary key, wid int, tag varchar(64))");

    //create users
    sql.execute("insert into jfc_users (name, pass) values ('admin','admin')");
    sql.execute("insert into jfc_users (name, pass) values ('oper','oper')");
    //create default config
    sql.execute("insert into jfc_config (id, value) values ('version','" + dbVersion + "')");
    sql.execute("insert into jfc_config (id, value) values ('strict_tags','false')");
    //create lists
    sql.execute("insert into jfc_lists (name) values ('jfc_ctrl_type')");
    id = sql.select1value("select id from jfc_lists where name='jfc_ctrl_type'");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",0,'JFC')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",1,'S7')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",2,'AB')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",3,'MB')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",4,'NI')");

    sql.execute("insert into jfc_lists (name) values ('jfc_ctrl_speed')");
    id = sql.select1value("select id from jfc_lists where name='jfc_ctrl_speed'");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",0,'Auto')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",1,'1s')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",2,'100ms')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",3,'10ms')");

    sql.execute("insert into jfc_lists (name) values ('jfc_tag_type')");
    id = sql.select1value("select id from jfc_lists where name='jfc_tag_type'");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.bit + ",'bit')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.int8 + ",'int8')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.int16 + ",'int16')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.int32 + ",'int32')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.int64 + ",'int64')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.float32 + ",'float32')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.float64 + ",'float64')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.char8 + ",'char8')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.char16 + ",'char16')");
//    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + "," + TagType.string + ",'string')");

    sql.execute("insert into jfc_lists (name) values ('jfc_panel_type')");
    id = sql.select1value("select id from jfc_lists where name='jfc_panel_type'");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",0,'label')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",1,'button')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",2,'togglebutton')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",3,'light')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",4,'light3')");
    sql.execute("insert into jfc_listdata (lid,value,text) values (" +  id + ",5,'progressbar')");

    //create local controller
    sql.execute("insert into jfc_ctrls (cid,ip,type,speed) values (0,'127.0.0.1',0,0)");

    //create logic blocks
    sql.execute("insert into jfc_logics (name,gid) values ('xon','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('xoff','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('coil','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('set','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('reset','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('pos','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('neg','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('not','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('shl','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('shr','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('or','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('and','bit')");
    sql.execute("insert into jfc_logics (name,gid) values ('xor','bit')");

    sql.execute("insert into jfc_logics (name,gid) values ('cmp_eq','compare')");
    sql.execute("insert into jfc_logics (name,gid) values ('cmp_ne','compare')");
    sql.execute("insert into jfc_logics (name,gid) values ('cmp_gt','compare')");
    sql.execute("insert into jfc_logics (name,gid) values ('cmp_ge','compare')");
    sql.execute("insert into jfc_logics (name,gid) values ('cmp_lt','compare')");
    sql.execute("insert into jfc_logics (name,gid) values ('cmp_le','compare')");

    sql.execute("insert into jfc_logics (name,gid) values ('move','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('add','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('sub','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('mul','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('div','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('mod','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('abs','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('sqrt','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('round','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('floor','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('ceil','math')");

    sql.execute("insert into jfc_logics (name,gid) values ('sin','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('cos','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('tan','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('asin','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('acos','math')");
    sql.execute("insert into jfc_logics (name,gid) values ('atan','math')");

    sql.execute("insert into jfc_logics (name,gid) values ('call','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('sleep','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('do','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('do_end','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('while','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('while_end','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('if','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('if_end','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('break','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('continue','function')");
    sql.execute("insert into jfc_logics (name,gid) values ('ret','function')");

    sql.execute("insert into jfc_logics (name,gid) values ('array_copy','array')");
    sql.execute("insert into jfc_logics (name,gid) values ('array_length','array')");
    sql.execute("insert into jfc_logics (name,gid) values ('array_size','array')");
    sql.execute("insert into jfc_logics (name,gid) values ('array_remove','array')");

    sql.execute("insert into jfc_logics (name,gid) values ('get_date','system')");
    sql.execute("insert into jfc_logics (name,gid) values ('get_time','system')");
    sql.execute("insert into jfc_logics (name,gid) values ('get_millis','system')");

    sql.execute("insert into jfc_logics (shortname,name,gid) values ('on_delay','timer_on_delay','timer')");
    sql.execute("insert into jfc_logics (shortname,name,gid) values ('off_delay','timer_off_delay','timer')");

    sql.execute("insert into jfc_logics (name,gid) values ('alarm_active','alarms')");
    sql.execute("insert into jfc_logics (name,gid) values ('alarm_not_ack','alarms')");
    sql.execute("insert into jfc_logics (name,gid) values ('alarm_ack_all','alarms')");

    //create SDTs
    int uid = IDs.uid_sys;
    sql.execute("insert into jfc_udts (uid,name) values (" + uid + ",'system')");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",0,'scantime'," + TagType.int32 + ",false,false,true)");
    sql.execute("insert into jfc_tags (cid,name,type,array,unsigned,builtin) values (0,'system'," + uid + ",false,false,true)");
    //udtmems are created in hardware config panel
    uid = IDs.uid_date;
    sql.execute("insert into jfc_udts (uid,name) values (" + uid + ",'date')");  //yyyy mm dd
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",0,'year'," + TagType.int32 + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",1,'month'," + TagType.int32 + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",2,'day'," + TagType.int32 + ",false,false,true)");
    uid = IDs.uid_time;
    sql.execute("insert into jfc_udts (uid,name) values (" + uid + ",'time')");  //hh MM ss mm
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",0,'hour'," + TagType.int32 + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",1,'minute'," + TagType.int32 + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",2,'second'," + TagType.int32 + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",3,'milli'," + TagType.int32 + ",false,false,true)");
    uid = IDs.uid_timer;
    sql.execute("insert into jfc_udts (uid,name) values (" + uid + ",'timer')");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",0,'time'," + TagType.int64 + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",1,'last'," + TagType.int64 + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",2,'run'," + TagType.bit + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",3,'done'," + TagType.bit + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",4,'enabled'," + TagType.bit + ",false,false,true)");

    //create default UDTs
    uid = IDs.uid_alarms;
    sql.execute("insert into jfc_udts (uid,name) values (" + uid + ",'alarms')");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",0,'text'," + TagType.string + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",1,'active'," + TagType.bit + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",2,'ack'," + TagType.bit + ",false,false,true)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",3,'stop'," + TagType.bit + ",false,false,false)");
    sql.execute("insert into jfc_udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",4,'audio'," + TagType.int32 + ",false,false,false)");

    //create default user tags
    uid = IDs.uid_alarms;
    sql.execute("insert into jfc_tags (cid,name,type,array,unsigned,builtin) values (0,'alarms'," + uid + ",true,false,true)");

    //create panels
    sql.execute("insert into jfc_panels (name, popup, builtin) values ('jfc_login', true, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_login'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,0,3,1,'label','','Username:')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,0,3,1,'textfield','user','')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,2,3,1,'label','','Password:')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,2,3,1,'password','pass','')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,3,8,1,'label','errmsg','')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,4,3,1,'button','','Login','jfc_login_ok')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",4,4,3,1,'button','','Cancel','jfc_login_cancel')");

    sql.execute("insert into jfc_panels (name, popup, builtin) values ('jfc_confirm', true, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_confirm'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,0,7,1,'label','jfc_confirm_msg','msg')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,1,3,1,'button','','Ok','jfc_confirm_ok')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",4,1,3,1,'button','','Cancel','jfc_confirm_cancel')");

    sql.execute("insert into jfc_panels (name, popup, builtin) values ('jfc_change_password', true, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_change_password'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,0,7,1,'label','','Change Password')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,1,4,1,'label','','Old Password')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,1,4,1,'password','jfc_password_old','')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,2,4,1,'label','','New Password')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,2,4,1,'password','jfc_password_new','')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,3,4,1,'label','','Confirm Password')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,3,4,1,'password','jfc_password_confirm','')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,4,3,1,'button','','Ok','jfc_change_password_ok')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",4,4,3,1,'button','','Cancel','jfc_change_password_cancel')");

    sql.execute("insert into jfc_panels (name, popup, builtin) values ('jfc_error', true, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_error'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,0,7,1,'label','jfc_error_msg','msg')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,1,3,1,'button','','Ok','jfc_error_ok')");

    sql.execute("insert into jfc_panels (name, popup, builtin) values ('jfc_error_textarea', true, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_error_textarea'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,0,7,1,'label','jfc_error_textarea_msg','msg')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,1,14,7,'textarea','jfc_error_textarea_textarea','msg')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,8,3,1,'button','','Ok','jfc_error_textarea_ok')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('main','Main', false, false)");
    id = sql.select1value("select id from jfc_panels where name='main'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",1,1,7,1,'label','','Welcome to jfControls!')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",1,3,12,1,'label','','Click on the Menu Icon in the top left corner to get started.')");

    sql.execute("insert into jfc_panels (name, popup, builtin) values ('jfc_menu', true, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_menu'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,0,3,1,'button','','Main Panel','setPanel','main')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,1,3,1,'button','','Controllers','setPanel','jfc_controllers')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,2,3,1,'button','','Tags','jfc_ctrl_tags','0')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,3,3,1,'button','','UserDataTypes','setPanel','jfc_udts')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,4,3,1,'button','','SysDataTypes','setPanel','jfc_sdts')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,5,3,1,'button','','Panels','setPanel','jfc_panels')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,6,3,1,'button','','Functions','setPanel','jfc_funcs')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,7,3,1,'button','','Alarms','setPanel','jfc_alarm_editor')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,8,3,1,'button','','Config','setPanel','jfc_config')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",0,9,3,1,'button','','Watch','setPanel','jfc_watch')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values ("     + id + ",0,10,3,1,'button','','Logoff','jfc_logout')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_controllers','Controllers', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_controllers'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,1,1,'label','','ID')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",3,1,3,1,'label','','IP')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",6,1,2,1,'label','','Type')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",8,1,2,1,'label','','Speed')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",12,1,3,1,'button','','New','jfc_ctrl_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",16,1,3,1,'button','','Save','jfc_ctrl_save')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_ctrls')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_config','Config', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_config'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",5,1,4,1,'button','','Change Password','jfc_config_password')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,tag) values (" + id + ",10,1,4,1,'togglebutton','','Strict Tag Checking','jfc_config_value_boolean_strict_tags')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",1,2,3,1,'label','','Database')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",4,2,2,1,'button','','Shutdown','jfc_config_shutdown')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",7,2,2,1,'button','','Restart','jfc_config_restart')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",10,2,2,1,'button','','Backup','jfc_config_backup')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",13,2,2,1,'button','','Restore','jfc_config_restore')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,arg) values (" + id + ",16,2,7,1,'combobox','backups','','jfc_config_backups')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",1,3,20,1,'label','jfc_config_status', '')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",1,4,1,1,'table','jfc_config_errors')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_alarm_editor','Alarm Editor', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_alarm_editor'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,2,1,'label','','Index')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,1,8,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",12,1,3,1,'button','','New','jfc_alarm_editor_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_alarm_editor')");

    //display active alarms
    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_alarms','Alarms', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_alarms'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,2,1,'label','','Ack')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,1,10,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,arg) values (" + id + ",16,1,3,1,'button','','History','setPanel','jfc_alarm_history')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'autoscroll','jfc_alarms')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_alarm_history','Alarm History', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_alarm_history'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,4,1,'label','','Time')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",6,1,10,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'autoscroll','jfc_alarm_history')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_tags','Tags', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_tags'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,6,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",8,1,3,1,'label','','Type')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",12,1,3,1,'button','','New','jfc_tags_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",16,1,3,1,'button','','Save','jfc_tags_save')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",20,1,6,1,'label','','Comment')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_tags')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_xref','Cross Reference', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_xref'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",1,1,0,0,'table','jfc_xref')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_watch','Watch Tables', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_watch'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,6,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",9,1,2,1,'button','','New','jfc_watch_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_watch')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_watch_tags','Watch Tags', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_watch_tags'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,6,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",8,1,6,1,'label','','Value')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",15,1,2,1,'button','','New','jfc_watch_tags_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",18,1,2,1,'button','','Start','jfc_watch_tags_start')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_watch_tags')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_udts','User Data Types', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_udts'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,7,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",12,1,3,1,'button','','New','jfc_udts_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",16,1,3,1,'button','','Save','jfc_udts_save')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_udts')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_udt_editor','User Data Type Editor', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_udt_editor'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,7,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",9,1,2,1,'label','','Type')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",12,1,3,1,'button','','New','jfc_udt_editor_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",16,1,3,1,'button','','Save','jfc_udt_editor_save')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_udt_editor')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_sdts','System Data Types', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_sdts'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,7,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_sdts')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_sdt_editor','System Data Type Viewer', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_sdt_editor'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,7,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",9,1,2,1,'label','','Type')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_sdt_editor')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_panels','Panels', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_panels'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,7,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",12,1,3,1,'button','','New','jfc_panels_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_panels')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_panel_editor','Panel Editor', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_panel_editor'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,arg) values (" + id + ",1,1,3,1,'combobox','panel_type','','jfc_panel_type')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",5,1,2,1,'button','','Add','jfc_panel_editor_add')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",8,1,2,1,'button','','Delete','jfc_panel_editor_del')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",11,1,2,1,'button','','Props','jfc_panel_editor_props')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,style) values (" + id + ",14,1,1,1,'label','','Move:','smallfont')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",15,1,1,1,'button','','!image:m_u','jfc_panel_editor_move_u','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",16,1,1,1,'button','','!image:m_d','jfc_panel_editor_move_d','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",17,1,1,1,'button','','!image:m_l','jfc_panel_editor_move_l','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",18,1,1,1,'button','','!image:m_r','jfc_panel_editor_move_r','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,style) values (" + id + ",20,1,1,1,'label','','Size:','smallfont')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",21,1,1,1,'button','','!image:s_w_i','jfc_panel_editor_size_w_inc','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",22,1,1,1,'button','','!image:s_w_d','jfc_panel_editor_size_w_dec','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",23,1,1,1,'button','','!image:s_h_i','jfc_panel_editor_size_h_inc','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func,style) values (" + id + ",24,1,1,1,'button','','!image:s_h_d','jfc_panel_editor_size_h_dec','border')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",0,2,1,1,'table','jfc_panel_editor')");

    sql.execute("insert into jfc_panels (name, popup, builtin) values ('jfc_panel_props', true, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_panel_props'");

    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,0,2,1,'label','textLbl','Text')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",3,0,5,1,'textfield','text')");

    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,1,1,1,'label','c0Lbl','0')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,func) values (" + id + ",1,1,1,1,'light','c0','jfc_panel_props_c0')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,1,1,'label','c1Lbl','1')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,func) values (" + id + ",3,1,1,1,'light','c1','jfc_panel_props_c1')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,1,1,1,'label','cnLbl','-1')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,func) values (" + id + ",5,1,1,1,'light','cn','jfc_panel_props_cn')");

    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,2,1,1,'label','v0Lbl','Low')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",1,2,1,1,'textfield','v0')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,2,1,1,'label','v1Lbl','Mid')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",3,2,1,1,'textfield','v1')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",4,2,1,1,'label','v2Lbl','Max')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",5,2,1,1,'textfield','v2')");

    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",0,3,2,1,'label','dir','Dir')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",2,3,2,1,'togglebutton','h','Horz','jfc_panel_props_h')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",4,3,2,1,'togglebutton','v','Vert','jfc_panel_props_v')");

    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,text) values (" + id + ",0,4,2,1,'label','Tag')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",3,4,5,1,'textfield','tag')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,text) values (" + id + ",0,5,2,1,'label','Press')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",3,5,5,1,'textfield','press')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,text) values (" + id + ",0,6,2,1,'label','Release')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",3,6,5,1,'textfield','release')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,text) values (" + id + ",0,7,2,1,'label','Click')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" + id + ",3,7,5,1,'textfield','click')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,8,2,1,'button','','OK','jfc_panel_props_ok')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",3,8,2,1,'button','','Cancel','jfc_panel_props_cancel')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_funcs','Functions', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_funcs'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text) values (" + id + ",2,1,7,1,'label','','Name')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",12,1,3,1,'button','','New','jfc_funcs_new')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",2,2,0,0,'table','jfc_funcs')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_func_editor','Function Editor', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_func_editor'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",1,1,1,1,'button','','+','jfc_func_editor_add_rung')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",3,1,1,1,'button','','-','jfc_func_editor_del_rung')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",5,1,2,1,'button','','Edit','jfc_func_editor_edit_rung')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",8,1,2,1,'button','','Compile','jfc_func_editor_compile')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",11,1,2,1,'button','','Debug','jfc_func_editor_debug')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,arg) values (" + id + ",14,1,2,1,'link','','Help','func_editor')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",0,0,0,0,'autoscroll','jfc_rungs_viewer')");

    sql.execute("insert into jfc_panels (name, display, popup, builtin) values ('jfc_rung_editor','Rung Editor', false, true)");
    id = sql.select1value("select id from jfc_panels where name='jfc_rung_editor'");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",0,1,1,1,'button','','!image:cancel','jfc_rung_editor_cancel')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",2,1,1,1,'button','','!image:delete','jfc_rung_editor_del')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",4,1,1,1,'button','','!image:fork','jfc_rung_editor_fork')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,func) values (" + id + ",6,1,1,1,'button','','!image:save','jfc_rung_editor_save')");

    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name,text,arg) values (" + id + ",8,1,3,1,'combobox','group_type','','jfc_logic_groups')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",12,1,24,1,'table','jfc_logic_groups')");
//    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",0,2,0,0,'table','jfc_rung_args')");
    sql.execute("insert into jfc_cells (pid,x,y,w,h,comp,name) values (" +  id + ",0,0,0,0,'autoscroll','jfc_rung_editor')");

    //insert system funcs
    sql.execute("insert into jfc_funcs (name, revision) values ('main', 0)");
    sql.execute("insert into jfc_funcs (name, revision) values ('init', 0)");

    sql.close();
  }

  public static String quote(String value, String type) {
    if (type.equals("str") || type.equals("tag") || type.equals("tagid")) {
      return SQL.quote(value);
    } else {
      return value;
    }
  }

  public static void start() {
    initDB();
  }
  public static void stop() {
    SQL sql = new SQL();
    JFLog.log("Shutting down database...");
    sql.connect("jdbc:derby:;shutdown=true");
    JFLog.log("Shutdown complete!");
    running = false;
  }
  public static void restart() {
    SQL.initClass(SQL.derbySQL);
    running = true;
  }
  public static String backup() {
    try {
      if (!running) throw new Exception("Database not running");
      Calendar now = Calendar.getInstance();
      String date = String.format("%04d%02d%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
      String time = String.format("%02d%02d%02d", now.get(Calendar.HOUR), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
      String tempPath = Paths.backupPath + "/" + date + "-" + time;
      new File(tempPath).mkdirs();
      SQL sql = getSQL();
      sql.execute("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE('" + tempPath + "')");
      File props = new File(tempPath + "/jfcontrols.properties");
      FileOutputStream fos = new FileOutputStream(props);
      StringBuilder sb = new StringBuilder();
      sb.append("type=backup\r\n");
      sb.append("date=" + date + "\r\n");
      sb.append("time=" + time + "\r\n");
      fos.write(sb.toString().getBytes());
      fos.close();
      //zip temp folder
      JF.zipPath(tempPath, Paths.backupPath + "/backup-" + date + "-" + time + ".zip");
      //delete temp folder
      JF.deletePathEx(tempPath);
      restart();
      return "Backup Complete";
    } catch (Exception e) {
      JFLog.log(e);
      return e.toString();
    }
  }
  public static String restore(String zip) {
    JF.deletePathEx(Paths.dataPath + "/restore");
    JF.unzip(zip, Paths.dataPath + "/restore");
    try {
      Properties props = new Properties();
      FileInputStream fis = new FileInputStream(Paths.dataPath + "/restore/jfcontrols.properties");
      props.load(fis);
      fis.close();
      String type = props.getProperty("type");
      if (type == null || !type.equals("backup")) throw new Exception("not a valid backup");
      Main.stop();
      stop();
      JF.deletePathEx(Paths.dataPath + "/database");
      File src = new File(Paths.dataPath + "/restore/database");
      File dst = new File(Paths.dataPath + "/database");
      src.renameTo(dst);
      JF.deletePathEx(Paths.dataPath + "/restore");
      initDB();  //upgrade if needed
      Main.restart();
      return "Restore complete!";
    } catch (Exception e) {
      JFLog.log(e);
      return e.toString();
    }
  }
}

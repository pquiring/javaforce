package jfcontrols.db;

/** Database Service
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.db.*;
import javaforce.controls.*;

import jfcontrols.app.*;
import jfcontrols.tags.*;
import jfcontrols.db.*;

public class Database {

  public static Table config;
  public static TableLog alarms;
  public static Table panels;
  public static TableList cells;
  public static Table controllers;
  public static Table users;
  public static TableList lists;
  public static Table logics;
  public static Table udts;
  public static Table udtmembers;
  public static Table tags;
  public static Table funcs;

  public static TableList rungs;
  public static TableList blocks;
  public static TableList watches;

  public static String Version = "1.0";

  public static void start() {
    JFLog.log("Database starting...");

    //create/load tables
    config = Table.load(Paths.dataPath + "/config/config.dat");
    alarms = new TableLog(Paths.logsPath);
    panels = Table.load(Paths.dataPath + "/config/panels.dat");
    cells = TableList.load(Paths.dataPath + "/config/cells");
    controllers = Table.load(Paths.dataPath + "/config/ctrls.dat");
    users = Table.load(Paths.dataPath + "/config/users.dat");
    lists = TableList.load(Paths.dataPath + "/config/lists");
    logics = Table.load(Paths.dataPath + "/config/logics.dat");
    udts = Table.load(Paths.dataPath + "/config/udts.dat");
    udtmembers = Table.load(Paths.dataPath + "/config/udtmembers.dat");
    tags = Table.load(Paths.dataPath + "/config/tags.dat");
    funcs = Table.load(Paths.dataPath + "/config/funcs.dat");
    rungs = TableList.load(Paths.dataPath + "/config/rungs");
    blocks = TableList.load(Paths.dataPath + "/config/blocks");
    watches = TableList.load(Paths.dataPath + "/config/watches");

    String version = getConfig("version");
    if (version == null) version = "0.0";
    JFLog.log("Database version=" + version);
    switch (version) {
      case "1.0": return;
      default: create(); break;
    }
    udts.setMinId(IDs.uid_user);
  }

  public static void stop() {
    if (alarms != null) {
      alarms.close();
    }
    config = null;
    alarms = null;
    panels = null;
    cells = null;
    controllers = null;
    users = null;
    lists = null;
    logics = null;
    udts = null;
    udtmembers = null;
    tags = null;
    funcs = null;
    rungs = null;
    blocks = null;
  }

  public static void restart() {
    stop();
    start();
  }

  private static void create() {
    JFLog.log("Database create...");
    Table list;
    addUser("admin", "admin");
    addUser("oper", "oper");
    //create default config
    setConfig("version", Version);
    setConfig("strict_tags", "false");
    //create lists
    list = addList("jfc_ctrl_type");
    list.add(new ListRow(0, "JFC"));
    list.add(new ListRow(1, "S7"));
    list.add(new ListRow(2, "AB"));
    list.add(new ListRow(3, "MB"));
    list.add(new ListRow(4, "NI"));
    list.save();

    list = addList("jfc_ctrl_speed");
    list.add(new ListRow(0, "Auto"));
    list.add(new ListRow(1, "1ms"));
    list.add(new ListRow(2, "100ms"));
    list.add(new ListRow(3, "10ms"));
    list.save();

    list = addList("jfc_tag_type");
    list.add(new ListRow(TagType.bit, "bit"));
    list.add(new ListRow(TagType.int8, "int8"));
    list.add(new ListRow(TagType.int16, "int16"));
    list.add(new ListRow(TagType.int32, "int32"));
    list.add(new ListRow(TagType.int64, "int64"));
    list.add(new ListRow(TagType.uint8, "uint8"));
    list.add(new ListRow(TagType.uint16, "uint16"));
    list.add(new ListRow(TagType.uint32, "uint32"));
    list.add(new ListRow(TagType.uint64, "uint64"));
    list.add(new ListRow(TagType.float32, "float32"));
    list.add(new ListRow(TagType.float64, "float64"));
    list.add(new ListRow(TagType.char8, "char8"));
    list.add(new ListRow(TagType.char16, "char16"));
    list.add(new ListRow(TagType.string, "string"));
    list.save();

    list = addList("jfc_panel_type");
    list.add(new ListRow(0, "label"));
    list.add(new ListRow(1, "button"));
    list.add(new ListRow(2, "togglebutton"));
    list.add(new ListRow(3, "light"));
    list.add(new ListRow(4, "light3"));
    list.add(new ListRow(5, "progressbar"));
    list.add(new ListRow(6, "image"));
    list.save();

    //create local controller
    addController(0, "127.0.0.1", 0, 0);

    //create logic blocks
    addLogic("xon", "bit");
    addLogic("xoff", "bit");
    addLogic("coil", "bit");
    addLogic("set", "bit");
    addLogic("reset", "bit");
    addLogic("pos", "bit");
    addLogic("neg", "bit");
    addLogic("not", "bit");
    addLogic("shl", "bit");
    addLogic("shr", "bit");
    addLogic("or", "bit");
    addLogic("and", "bit");
    addLogic("xor", "bit");

    addLogic("cmp_eq", "compare");
    addLogic("cmp_ne", "compare");
    addLogic("cmp_gt", "compare");
    addLogic("cmp_ge", "compare");
    addLogic("cmp_lt", "compare");
    addLogic("cmp_le", "compare");

    addLogic("move", "math");
    addLogic("add", "math");
    addLogic("sub", "math");
    addLogic("mul", "math");
    addLogic("div", "math");
    addLogic("mod", "math");
    addLogic("abs", "math");
    addLogic("sqrt", "math");
    addLogic("round", "math");
    addLogic("floor", "math");
    addLogic("ceil", "math");

    addLogic("sin", "math");
    addLogic("cos", "math");
    addLogic("tan", "math");
    addLogic("asin", "math");
    addLogic("acos", "math");
    addLogic("atan", "math");

    addLogic("call", "function");
    addLogic("sleep", "function");
    addLogic("do", "function");
    addLogic("do_end", "function");
    addLogic("while", "function");
    addLogic("while_end", "function");
    addLogic("if", "function");
    addLogic("if_end", "function");
    addLogic("break", "function");
    addLogic("continue", "function");
    addLogic("ret", "function");

    addLogic("array_copy", "array");
    addLogic("array_length", "array");
    addLogic("array_size", "array");
    addLogic("array_remove", "array");
    addLogic("array_shift", "array");

    addLogic("get_date", "system");
    addLogic("get_time", "system");
    addLogic("get_millis", "system");

    addLogic("on_delay", "timer_on_delay", "timer");
    addLogic("off_delay", "timer_off_delay", "timer");

    addLogic("alarm_active", "alarms");
    addLogic("alarm_not_ack", "alarms");
    addLogic("alarm_ack_all", "alarms");
    logics.save();

    //create SDTs
    int uid = IDs.uid_sys;
    setUDTid(uid);
    addUDT("system");
    addUDTMember(uid, 0, "scantime", TagType.int32, 0, true);
    addTag(0, "system", uid, 0, true);

    //udtmems are created in hardware config panel
    uid = IDs.uid_date;
    setUDTid(uid);
    addUDT("date");
    addUDTMember(uid, 0, "year", TagType.int32, 0, true);
    addUDTMember(uid, 1, "month", TagType.int32, 0, true);
    addUDTMember(uid, 2, "day", TagType.int32, 0, true);
    uid = IDs.uid_time;
    setUDTid(uid);
    addUDT("time");
    addUDTMember(uid, 0, "hour", TagType.int32, 0, true);
    addUDTMember(uid, 1, "minute", TagType.int32, 0, true);
    addUDTMember(uid, 2, "second", TagType.int32, 0, true);
    addUDTMember(uid, 3, "milli", TagType.int32, 0, true);
    uid = IDs.uid_timer;
    setUDTid(uid);
    addUDT("timer");
    addUDTMember(uid, 0, "time", TagType.int64, 0, true);
    addUDTMember(uid, 1, "last", TagType.int64, 0, true);
    addUDTMember(uid, 2, "run", TagType.bit, 0, true);
    addUDTMember(uid, 3, "done", TagType.bit, 0, true);
    addUDTMember(uid, 4, "enabled", TagType.bit, 0, true);

    //create default UDTs
    uid = IDs.uid_alarms;
    setUDTid(uid);
    addUDT("alarms");
    addUDTMember(uid, 0, "text", TagType.string, 0, true);
    addUDTMember(uid, 0, "active", TagType.bit, 0, true);
    addUDTMember(uid, 0, "ack", TagType.bit, 0, true);
    addUDTMember(uid, 0, "stop", TagType.bit, 0, false);
    addUDTMember(uid, 0, "audio", TagType.int32, 0, false);

    //create default user tags
    uid = IDs.uid_alarms;
    addTag(0, "alarms", uid, 256, true);

    //create panels
    int id = addPanel("jfc_login", true, true);
    Table celltable = addCellTable("jfc_login", id);
    celltable.add(new CellRow(id,0,0,3,1,"label", "", "Username:"));
    celltable.add(new CellRow(id,4,0,3,1,"textfield", "user", ""));
    celltable.add(new CellRow(id,0,2,3,1,"label", "", "Password:"));
    celltable.add(new CellRow(id,4,2,3,1,"textfield", "password", ""));
    celltable.add(new CellRow(id,0,3,8,1,"label", "errmsg", ""));
    celltable.add(new CellRow(id,0,4,3,1,"button", "", "Login").setFunc("jfc_login_ok"));
    celltable.add(new CellRow(id,4,4,3,1,"button", "", "Cancel").setFunc("jfc_login_cancel"));
    celltable.save();

    id = addPanel("jfc_confirm", true, true);
    celltable = addCellTable("jfc_confirm", id);
    celltable.add(new CellRow(id,0,0,7,1,"label", "jfc_confirm_msg", ""));
    celltable.add(new CellRow(id,0,1,3,1,"button", "", "Ok").setFunc("jfc_confirm_ok"));
    celltable.add(new CellRow(id,4,1,3,1,"button", "", "Cancel").setFunc("jfc_confirm_cancel"));
    celltable.save();

    id = addPanel("jfc_change_password", true, true);
    celltable = addCellTable("jfc_change_password", id);
    celltable.add(new CellRow(id,0,0,7,1,"label", "", "Change Password:"));
    celltable.add(new CellRow(id,0,1,4,1,"label", "", "Old Password:"));
    celltable.add(new CellRow(id,4,1,4,1,"textfield", "jfc_password_old", ""));
    celltable.add(new CellRow(id,0,2,4,1,"label", "", "New Password:"));
    celltable.add(new CellRow(id,4,2,4,1,"textfield", "jfc_password_new", ""));
    celltable.add(new CellRow(id,0,3,4,1,"label", "", "Confirm Password:"));
    celltable.add(new CellRow(id,4,3,4,1,"textfield", "jfc_password_confirm", ""));
    celltable.add(new CellRow(id,0,4,3,1,"button", "", "Ok").setFunc("jfc_change_password_ok"));
    celltable.add(new CellRow(id,4,4,3,1,"button", "", "Cancel").setFunc("jfc_change_password_cancel"));
    celltable.save();

    id = addPanel("jfc_error", true, true);
    celltable = addCellTable("jfc_error", id);
    celltable.add(new CellRow(id,0,0,7,1,"label", "jfc_error_msg", ""));
    celltable.add(new CellRow(id,0,1,3,1,"button", "", "Ok").setFunc("jfc_error_ok"));
    celltable.save();

    id = addPanel("jfc_error_textarea", true, true);
    celltable = addCellTable("jfc_error_textarea", id);
    celltable.add(new CellRow(id,0,0,7,1,"label", "jfc_error_textarea_msg", ""));
    celltable.add(new CellRow(id,0,1,14,7,"textarea", "jfc_error_textarea_textarea", ""));
    celltable.add(new CellRow(id,0,8,3,1,"button", "", "Ok").setFunc("jfc_error_textarea_ok"));
    celltable.save();

    id = addPanel("main", "Main", false, false);
    celltable = addCellTable("main", id);
    celltable.add(new CellRow(id,1,2,7,1,"label", "", "Welcome to jfControls!").setStyle("left"));
    celltable.add(new CellRow(id,1,4,16,1,"label", "", "Use the Menu Icon in the top left corner to get started.").setStyle("left"));
    celltable.add(new CellRow(id,1,6,16,1,"label", "", "License : LGPL : This program comes with no warranty.").setStyle("left"));
    celltable.add(new CellRow(id,1,8,24,1,"label", "", "This program has completed minimal testing and is not recommended for production environments.").setStyle("left"));
    celltable.add(new CellRow(id,1,10,16,1,"label", "", "This program should NEVER be used for safety or life support systems!").setStyle("left"));
    celltable.add(new CellRow(id,1,12,16,1,"label", "", "Please test your logic extensively!").setStyle("left"));
    celltable.add(new CellRow(id,1,16,1,1,"image", "", "detroit"));
    celltable.add(new CellRow(id,2,16,6,1,"label", "", "Built in Detroit, MI, USA"));
    celltable.save();

    id = addPanel("jfc_menu", true, true);
    celltable = addCellTable("jfc_menu", id);
//    celltable.add(new Cell(id,2,16,6,1,"label", "", "Built in Detroit, MI, USA"));
    celltable.add(new CellRow(id,0,0,3,1,"button","","Main Panel").setFuncArg("setPanel","main"));
    celltable.add(new CellRow(id,0,1,3,1,"button","","Controllers").setFuncArg("setPanel","jfc_controllers"));
    celltable.add(new CellRow(id,0,2,3,1,"button","","Tags").setFuncArg("jfc_ctrl_tags","1"));
    celltable.add(new CellRow(id,0,3,3,1,"button","","UserDataTypes").setFuncArg("setPanel","jfc_udts"));
    celltable.add(new CellRow(id,0,4,3,1,"button","","SysDataTypes").setFuncArg("setPanel","jfc_sdts"));
    celltable.add(new CellRow(id,0,5,3,1,"button","","Panels").setFuncArg("setPanel","jfc_panels"));
    celltable.add(new CellRow(id,0,6,3,1,"button","","Functions").setFuncArg("setPanel","jfc_funcs"));
    celltable.add(new CellRow(id,0,7,3,1,"button","","Alarms").setFuncArg("setPanel","jfc_alarm_editor"));
    celltable.add(new CellRow(id,0,8,3,1,"button","","Config").setFuncArg("setPanel","jfc_config"));
    celltable.add(new CellRow(id,0,9,3,1,"button","","Watch").setFuncArg("setPanel","jfc_watch"));
    celltable.add(new CellRow(id,0,10,3,1,"button","","Logoff").setFunc("jfc_logout"));
    celltable.save();

    id = addPanel("jfc_controllers", "Controllers", false, true);
    celltable = addCellTable("jfc_controllers", id);
    celltable.add(new CellRow(id,2,1,1,1,"label","","ID"));
    celltable.add(new CellRow(id,3,1,3,1,"label","","IP"));
    celltable.add(new CellRow(id,6,1,2,1,"label","","Type"));
    celltable.add(new CellRow(id,8,1,2,1,"label","","Speed"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_ctrl_new"));
    celltable.add(new CellRow(id,16,1,3,1,"button","","Save").setFunc("jfc_ctrl_save"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("controllers"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_ctrls",""));
    celltable.save();

    id = addPanel("jfc_config", "Config", false, true);
    celltable = addCellTable("jfc_config", id);
    celltable.add(new CellRow(id,5,1,4,1,"button","","Change Password").setFunc("jfc_config_password"));
    celltable.add(new CellRow(id,10,1,4,1,"togglebutton","","Strict Tag Checking").setTag("jfc_config_value_boolean_strict_tags"));
    celltable.add(new CellRow(id,15,1,2,1,"link","","Help").setArg("config"));
    celltable.add(new CellRow(id,1,2,3,1,"label","","Database"));
    celltable.add(new CellRow(id,4,2,2,1,"button","","Shutdown").setFunc("jfc_config_shutdown"));
    celltable.add(new CellRow(id,7,2,2,1,"button","","Restart").setFunc("jfc_config_restart"));
    celltable.add(new CellRow(id,10,2,2,1,"button","","Backup").setFunc("jfc_config_backup"));
    celltable.add(new CellRow(id,13,2,2,1,"button","","Restore").setFunc("jfc_config_restore"));
    celltable.add(new CellRow(id,16,2,7,1,"combobox","backups","").setArg("jfc_config_backups"));
    celltable.add(new CellRow(id,1,3,20,1,"label","jfc_config_status", ""));
    celltable.add(new CellRow(id,1,4,3,1,"label","","ErrorLog"));
    celltable.add(new CellRow(id,1,5,1,1,"table","jfc_config_errors", ""));
    celltable.save();

    id = addPanel("jfc_alarm_editor", "Alarm Editor", false, true);
    celltable = addCellTable("jfc_alarm_editor", id);
    celltable.add(new CellRow(id,2,1,2,1,"label","","Index"));
    celltable.add(new CellRow(id,4,1,8,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_alarm_editor_new"));
    celltable.add(new CellRow(id,16,1,2,1,"link","","Help").setArg("alarms"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_alarm_editor_table", ""));
    celltable.save();

    //display active alarms
    id = addPanel("jfc_alarms", "Alarms", false, true);
    celltable = addCellTable("jfc_alarms", id);
    celltable.add(new CellRow(id,2,1,2,1,"label","","Ack"));
    celltable.add(new CellRow(id,4,1,10,1,"label","","Name"));
    celltable.add(new CellRow(id,16,1,3,1,"button","","History").setFuncArg("setPanel","jfc_alarm_history"));
    celltable.add(new CellRow(id,2,2,0,0,"autoscroll","jfc_alarms", ""));
    celltable.save();

    id = addPanel("jfc_alarm_history", "Alarm History", false, true);
    celltable = addCellTable("jfc_alarm_history", id);
    celltable.add(new CellRow(id,2,1,4,1,"label","","Time"));
    celltable.add(new CellRow(id,6,1,10,1,"label","","Name"));
    celltable.add(new CellRow(id,2,2,0,0,"autoscroll","jfc_alarm_history", ""));
    celltable.save();

    id = addPanel("jfc_tags", "Tags", false, true);
    celltable = addCellTable("jfc_tags", id);
    celltable.add(new CellRow(id,2,1,6,1,"label","","Name"));
    celltable.add(new CellRow(id,8,1,3,1,"label","","Type"));
    celltable.add(new CellRow(id,11,1,3,1,"label","","ArraySize"));
    celltable.add(new CellRow(id,14,1,6,1,"label","","Comment"));
    celltable.add(new CellRow(id,21,1,3,1,"button","","New").setFunc("jfc_tags_new"));
    celltable.add(new CellRow(id,26,1,2,1,"link","","Help").setArg("tags"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_tags", ""));
    celltable.save();

    id = addPanel("jfc_new_tag", true, true);
    celltable = addCellTable("jfc_new_tag", id);
    celltable.add(new CellRow(id,0,0,3,1,"label","","Name:"));
    celltable.add(new CellRow(id,4,0,3,1,"textfield","tag_name",""));
    celltable.add(new CellRow(id,0,2,3,1,"label","","Type:"));
    celltable.add(new CellRow(id,4,2,3,1,"combobox","tag_type","").setArg("jfc_tag_type"));
    celltable.add(new CellRow(id,0,3,3,1,"checkbox","tag_array","ArraySize:"));
    celltable.add(new CellRow(id,4,3,3,1,"textfield","tag_arraysize","0"));
    celltable.add(new CellRow(id,0,4,3,1,"button","","Ok").setFunc("jfc_tag_new_ok"));
    celltable.add(new CellRow(id,4,4,3,1,"button","","Cancel").setFunc("jfc_tag_new_cancel"));
    celltable.save();

    id = addPanel("jfc_new_tag_udt", true, true);
    celltable = addCellTable("jfc_new_tag_udt", id);
    celltable.add(new CellRow(id,0,0,3,1,"label","","Name:"));
    celltable.add(new CellRow(id,4,0,3,1,"textfield","tag_udt_name",""));
    celltable.add(new CellRow(id,0,2,3,1,"label","","Type:"));
    celltable.add(new CellRow(id,4,2,3,1,"combobox","tag_udt_type","").setArg("jfc_tag_type_udt"));
    celltable.add(new CellRow(id,0,3,3,1,"checkbox","tag_udt_array","ArraySize:"));
    celltable.add(new CellRow(id,4,3,3,1,"textfield","tag_udt_arraysize","0"));
    celltable.add(new CellRow(id,0,4,3,1,"button","","Ok").setFunc("jfc_tag_new_ok_udt"));
    celltable.add(new CellRow(id,4,4,3,1,"button","","Cancel").setFunc("jfc_tag_new_cancel_udt"));
    celltable.save();

    id = addPanel("jfc_xref", "Cross Reference", false, true);
    celltable = addCellTable("jfc_xref", id);
    celltable.add(new CellRow(id,1,1,0,0,"table","jfc_xref", ""));
    celltable.save();

    id = addPanel("jfc_watch", "Watch Tables", false, true);
    celltable = addCellTable("jfc_watch", id);
    celltable.add(new CellRow(id,2,1,6,1,"label","","Name"));
    celltable.add(new CellRow(id,9,1,2,1,"button","","New").setFunc("jfc_watch_new"));
    celltable.add(new CellRow(id,12,1,2,1,"link","","Help").setArg("watch"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_watch", ""));
    celltable.save();

    id = addPanel("jfc_watch_tags", "Watch Tag", false, true);
    celltable = addCellTable("jfc_watch_tags", id);
    celltable.add(new CellRow(id,2,1,6,1,"label","","Name"));
    celltable.add(new CellRow(id,8,1,6,1,"label","","Value"));
    celltable.add(new CellRow(id,15,1,2,1,"button","","New").setFunc("jfc_watch_tags_new"));
    celltable.add(new CellRow(id,18,1,2,1,"button","","Start").setFunc("jfc_watch_tags_start"));
    celltable.add(new CellRow(id,21,1,2,1,"link","","Help").setArg("watch_tags"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_watch_tags", ""));
    celltable.save();

    id = addPanel("jfc_udts", "User Data Types", false, true);
    celltable = addCellTable("jfc_udts", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_udts_new"));
    celltable.add(new CellRow(id,16,1,3,1,"button","","Save").setFunc("jfc_udts_save"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("udt"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_udts", ""));
    celltable.save();

    id = addPanel("jfc_udt_editor", "User Data Type Editor", false, true);
    celltable = addCellTable("jfc_udt_editor", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,9,1,2,1,"label","","Type"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_udt_editor_new"));
    celltable.add(new CellRow(id,16,1,3,1,"button","","Save").setFunc("jfc_udt_editor_save"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("udt_editor"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_udt_editor", ""));
    celltable.save();

    id = addPanel("jfc_sdts", "System Data Types", false, true);
    celltable = addCellTable("jfc_sdts", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,10,1,2,1,"link","","Help").setArg("sdt"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_sdts", ""));
    celltable.save();

    id = addPanel("jfc_sdt_editor", "System Data Type Viewer", false, true);
    celltable = addCellTable("jfc_sdt_editor", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,9,1,2,1,"label","","Type"));
    celltable.add(new CellRow(id,12,1,2,1,"link","","Help").setArg("sdt_viewer"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_sdt_editor", ""));
    celltable.save();

    id = addPanel("jfc_panels", "Panels", false, true);
    celltable = addCellTable("jfc_panels", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_panels_new"));
    celltable.add(new CellRow(id,16,1,2,1,"link","","Help").setArg("panels"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_panels", ""));
    celltable.save();

    id = addPanel("jfc_panel_editor", "Panel Editor", false, true);
    celltable = addCellTable("jfc_panel_editor", id);
    celltable.add(new CellRow(id,1,1,3,1,"combobox","panel_type","").setArg("jfc_panel_type"));
    celltable.add(new CellRow(id,5,1,1,1,"button","","!image:add").setFunc("jfc_panel_editor_add"));
    celltable.add(new CellRow(id,7,1,1,1,"button","","!image:minus").setFunc("jfc_panel_editor_del"));
    celltable.add(new CellRow(id,9,1,2,1,"button","","Props").setFunc("jfc_panel_editor_props"));
    celltable.add(new CellRow(id,12,1,1,1,"label","","Move:").setStyle("smallfont"));
    celltable.add(new CellRow(id,13,1,1,1,"button","","!image:m_u").setFunc("jfc_panel_editor_move_u").setStyle("border"));
    celltable.add(new CellRow(id,14,1,1,1,"button","","!image:m_d").setFunc("jfc_panel_editor_move_d").setStyle("border"));
    celltable.add(new CellRow(id,15,1,1,1,"button","","!image:m_l").setFunc("jfc_panel_editor_move_l").setStyle("border"));
    celltable.add(new CellRow(id,16,1,1,1,"button","","!image:m_r").setFunc("jfc_panel_editor_move_r").setStyle("border"));
    celltable.add(new CellRow(id,18,1,1,1,"label","","Size:").setStyle("smallfont"));
    celltable.add(new CellRow(id,19,1,1,1,"button","","!image:s_w_i").setFunc("jfc_panel_editor_size_w_inc").setStyle("border"));
    celltable.add(new CellRow(id,20,1,1,1,"button","","!image:s_w_d").setFunc("jfc_panel_editor_size_w_dec").setStyle("border"));
    celltable.add(new CellRow(id,21,1,1,1,"button","","!image:s_h_i").setFunc("jfc_panel_editor_size_h_inc").setStyle("border"));
    celltable.add(new CellRow(id,22,1,1,1,"button","","!image:s_h_d").setFunc("jfc_panel_editor_size_h_dec").setStyle("border"));
    celltable.add(new CellRow(id,24,1,2,1,"link","","Help").setArg("panel_editor"));
    celltable.add(new CellRow(id,0,2,1,1,"table","jfc_panel_editor", ""));
    celltable.save();

    id = addPanel("jfc_panel_props", true, true);
    celltable = addCellTable("jfc_panel_props", id);
    celltable.add(new CellRow(id,0,0,2,1,"label","textLbl","Text"));
    celltable.add(new CellRow(id,3,0,5,1,"textfield","text", ""));

    celltable.add(new CellRow(id,0,1,1,1,"label","c0Lbl","0"));
    celltable.add(new CellRow(id,1,1,1,1,"light","c0","jfc_panel_props_c0"));
    celltable.add(new CellRow(id,2,1,1,1,"label","c1Lbl","1"));
    celltable.add(new CellRow(id,3,1,1,1,"light","c1","jfc_panel_props_c1"));
    celltable.add(new CellRow(id,4,1,1,1,"label","cnLbl","-1"));
    celltable.add(new CellRow(id,5,1,1,1,"light","cn","jfc_panel_props_cn"));

    celltable.add(new CellRow(id,0,2,1,1,"label","v0Lbl","Low"));
    celltable.add(new CellRow(id,1,2,1,1,"textfield","v0", ""));
    celltable.add(new CellRow(id,2,2,1,1,"label","v1Lbl","Mid"));
    celltable.add(new CellRow(id,3,2,1,1,"textfield","v1", ""));
    celltable.add(new CellRow(id,4,2,1,1,"label","v2Lbl","Max"));
    celltable.add(new CellRow(id,5,2,1,1,"textfield","v2", ""));

    celltable.add(new CellRow(id,0,3,2,1,"label","dir","Dir"));
    celltable.add(new CellRow(id,2,3,2,1,"togglebutton","h","Horz").setFunc("jfc_panel_props_h"));
    celltable.add(new CellRow(id,4,3,2,1,"togglebutton","v","Vert").setFunc("jfc_panel_props_v"));

    celltable.add(new CellRow(id,0,4,2,1,"label","Tag", ""));
    celltable.add(new CellRow(id,3,4,5,1,"textfield","tag", ""));
    celltable.add(new CellRow(id,0,5,2,1,"label","Press", ""));
    celltable.add(new CellRow(id,3,5,5,1,"textfield","press", ""));
    celltable.add(new CellRow(id,0,6,2,1,"label","Release", ""));
    celltable.add(new CellRow(id,3,6,5,1,"textfield","release", ""));
    celltable.add(new CellRow(id,0,7,2,1,"label","Click", ""));
    celltable.add(new CellRow(id,3,7,5,1,"textfield","click", ""));
    celltable.add(new CellRow(id,0,8,2,1,"button","","OK").setFunc("jfc_panel_props_ok"));
    celltable.add(new CellRow(id,3,8,2,1,"button","","Cancel").setFunc("jfc_panel_props_cancel"));
    celltable.save();

    id = addPanel("jfc_funcs", "Functions", false, true);
    celltable = addCellTable("jfc_funcs", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_funcs_new"));
    celltable.add(new CellRow(id,16,1,2,1,"link","","Help").setArg("funcs"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_funcs", ""));
    celltable.save();

    id = addPanel("jfc_func_editor", "Function Editor", false, true);
    celltable = addCellTable("jfc_func_editor", id);
    celltable.add(new CellRow(id,1,1,1,1,"button","","!image:add").setFunc("jfc_func_editor_add_rung"));
    celltable.add(new CellRow(id,3,1,1,1,"button","","!image:minus").setFunc("jfc_func_editor_del_rung"));
    celltable.add(new CellRow(id,5,1,2,1,"button","","Edit").setFunc("jfc_func_editor_edit_rung"));
    celltable.add(new CellRow(id,8,1,2,1,"button","","Compile").setFunc("jfc_func_editor_compile"));
    celltable.add(new CellRow(id,11,1,2,1,"button","jfc_debug","Debug").setFunc("jfc_func_editor_debug"));
    celltable.add(new CellRow(id,14,1,2,1,"link","","Help").setArg("func_editor"));
    celltable.add(new CellRow(id,0,0,0,0,"autoscroll","jfc_rungs_viewer", ""));
    celltable.save();

    id = addPanel("jfc_rung_editor", "Rung Editor", false, true);
    celltable = addCellTable("jfc_rung_editor", id);
    celltable.add(new CellRow(id,0,1,1,1,"button","","!image:cancel").setFunc("jfc_rung_editor_cancel"));
    celltable.add(new CellRow(id,2,1,1,1,"button","","!image:delete").setFunc("jfc_rung_editor_del"));
    celltable.add(new CellRow(id,4,1,1,1,"button","","!image:fork").setFunc("jfc_rung_editor_fork"));
    celltable.add(new CellRow(id,6,1,1,1,"button","","!image:save").setFunc("jfc_rung_editor_save"));
    celltable.add(new CellRow(id,8,1,2,1,"link","","Help").setArg("rung_editor"));

    celltable.add(new CellRow(id,11,1,3,1,"combobox","group_type","").setArg("jfc_logic_groups"));
    celltable.add(new CellRow(id,15,1,24,1,"table","jfc_logic_groups", ""));
//    celltable.add(new Cell(id,0,2,0,0,"table","jfc_rung_args"));
    celltable.add(new CellRow(id,0,0,0,0,"autoscroll","jfc_rung_editor", ""));
    celltable.save();

    //insert system funcs
    addFunction("main");
    addFunction("init");
  }

  public static String quote(String value, String type) {
    if (type.equals("str") || type.equals("tag") || type.equals("tagid")) {
      return SQL.quote(value);
    } else {
      return value;
    }
  }

  public static String backup() {
    try {
      Calendar c = Calendar.getInstance();
      String filename = Paths.backupPath + "/"
        + String.format("%04d-%02d-%02d %02d-%02d-%02d"
          , c.get(Calendar.YEAR)
          , c.get(Calendar.MONTH)+1
          , c.get(Calendar.DAY_OF_MONTH)
          , c.get(Calendar.HOUR_OF_DAY)
          , c.get(Calendar.MINUTE)
          , c.get(Calendar.SECOND)
        )
        + ".zip";
      JFLog.log("filename=" + filename);
      JF.zipPath(Paths.configPath, filename);
      return "Backup Complete";
    } catch (Exception e) {
      JFLog.log(e);
      return e.toString();
    }
  }
  public static String restore(String zip) {
    try {
      JF.deletePathEx(Paths.dataPath + "/config");
      JF.unzip(zip, Paths.dataPath + "/config");
      return "Restore complete!";
    } catch (Exception e) {
      JFLog.log(e);
      return e.toString();
    }
  }

  public static String getConfig(String name) {
    ArrayList<Row> rows = config.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      ConfigRow row = (ConfigRow)rows.get(a);
      if (row.name.equals(name)) {
        return row.value;
      }
    }
    return null;
  }
  public static void setConfig(String name, String value) {
    ArrayList<Row> rows = config.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      ConfigRow row = (ConfigRow)rows.get(a);
      if (row.name.equals(name)) {
        row.value = value;
        config.save();
        return;
      }
    }
    ConfigRow row = new ConfigRow();
    row.name = name;
    row.value = value;
    config.add(row);
    config.save();
  }

  public static UserRow getUser(String name) {
    ArrayList<Row> rows = users.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      UserRow row = (UserRow)rows.get(a);
      if (row.name.equals(name)) {
        return row;
      }
    }
    return null;
  }
  public static void addUser(String name, String pass) {
    UserRow user = new UserRow();
    user.name = name;
    user.pass = pass;
    users.add(user);
    users.save();
  }
  public static void setUserPassword(String name, String pass) {
    UserRow user = getUser(name);
    user.pass = pass;
    users.save();
  }

  public static Table addList(String name) {
    Table list = new Table();
    list.name = name;
    lists.add(list);
    return list;
  }
  public static Table getList(String name) {
    return lists.get(name);
  }

  public static void addController(int cid, String ip, int type, int speed) {
    ControllerRow c = new ControllerRow();
    c.cid = cid;
    c.ip = ip;
    c.type = type;
    c.speed = speed;
    controllers.add(c);
    controllers.save();
  }
  public static int getControllerCID(int id) {
    ArrayList<Row> rows = controllers.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      ControllerRow ctrl = (ControllerRow)rows.get(a);
      if (ctrl.id == id) {
        return ctrl.cid;
      }
    }
    return -1;
  }
  public static ControllerRow getControllerById(int cid) {
    ArrayList<Row> rows = controllers.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      ControllerRow ctrl = (ControllerRow)rows.get(a);
      if (ctrl.cid == cid) {
        return ctrl;
      }
    }
    return null;
  }
  public static void deleteControllerById(int cid) {
    ArrayList<Row> rows = controllers.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      ControllerRow ctrl = (ControllerRow)rows.get(a);
      if (ctrl.cid == cid) {
        rows.remove(a);
        break;
      }
    }
    controllers.save();
  }
  public static boolean isControllerInUse(int cid) {
    String match = ",c" + cid + "#";
    ArrayList<Table> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      ArrayList<Row> blockRows = table.getRows();
      for(int r=0;r<blockRows.size();r++) {
        BlockRow row = (BlockRow)blockRows.get(r);
        if (row.tags.contains(match)) return true;
      }
    }
    return false;
  }

  //Add Logic row : does NOT save table
  public static void addLogic(String name, String gid) {
    addLogic(name, name, gid);
  }
  //Add Logic row : does NOT save table
  public static void addLogic(String shortname, String name, String gid) {
    LogicRow logic = new LogicRow();
    logic.shortname = shortname;
    logic.name = name;
    logic.group = gid;
    logics.add(logic);
  }
  public static LogicRow[] getLogicsByGroupId(String group) {
    ArrayList<Row> rows = logics.getRows();
    ArrayList<Row> ret = new ArrayList<Row>();
    for(int r=0;r<rows.size();r++) {
      LogicRow row = (LogicRow)rows.get(r);
      if (row.group.equals(group)) {
        ret.add(row);
      }
    }
    return ret.toArray(new LogicRow[0]);
  }
  public static String[] getLogicGroups() {
    ArrayList<Row> rows = logics.getRows();
    ArrayList<String> ret = new ArrayList<String>();
    for(int r=0;r<rows.size();r++) {
      LogicRow row = (LogicRow)rows.get(r);
      if (!ret.contains(row.group)) {
        ret.add(row.group);
      }
    }
    return ret.toArray(new String[0]);
  }

  private static void setUDTid(int id) {
    udts.setMinId(id);
  }
  public static void addUDT(String name) {
    UDT udt = new UDT();
    udt.name = name;
    udts.add(udt);
    udts.save();
  }
  public static UDT getUDTById(int uid) {
    return (UDT)udts.get(uid);
  }
  public static void deleteUDTById(int uid) {
    udts.remove(uid);
    udts.save();
  }
  public static boolean isUDTInUse(int uid) {
    ArrayList<Row> rows = tags.getRows();
    for(int r=0;r<rows.size();r++) {
      TagRow tag = (TagRow)rows.get(r);
      if (tag.type == uid) return true;
    }
    return false;
  }
  public static void addUDTMember(int uid, int mid, String name, int type, int length, boolean builtin) {
    UDTMember member = new UDTMember();
    member.uid = uid;
    member.mid = mid;
    member.name = name;
    member.type = type;
    member.length = length;
    member.builtin = builtin;
    udtmembers.add(member);
    udtmembers.save();
  }
  public static UDTMember[] getUDTMembersById(int id) {
    ArrayList<UDTMember> list = new ArrayList<UDTMember>();
    ArrayList<Row> rows = udtmembers.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      UDTMember member = (UDTMember)rows.get(a);
      if (member.uid == id) {
        list.add(member);
      }
    }
    return list.toArray(new UDTMember[0]);
  }
  public static UDTMember getUDTMemberById(int uid, int mid) {
    ArrayList<Row> rows = udtmembers.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      UDTMember member = (UDTMember)rows.get(a);
      if (member.uid == uid && member.mid == mid) {
        return member;
      }
    }
    return null;
  }
  public static void deleteUDTMembersById(int uid) {
    ArrayList<Row> rows = udtmembers.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      UDTMember member = (UDTMember)rows.get(a);
      if (member.uid == uid) {
        udtmembers.remove(member.id);
      }
    }
    udtmembers.save();
  }
  public static void deleteUDTMemberById(int uid, int mid) {
    ArrayList<Row> rows = udtmembers.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      UDTMember member = (UDTMember)rows.get(a);
      if (member.uid == uid && member.mid == mid) {
        udtmembers.remove(member.id);
        udtmembers.save();
        return;
      }
    }
  }

  public static int addTag(int cid, String name, int type, int length, boolean builtin) {
    TagRow tag = new TagRow();
    tag.cid = cid;
    tag.name = name;
    tag.type = type;
    tag.length = length;
    tag.builtin = builtin;
    tags.add(tag);
    tags.save();
    return tag.id;
  }
  public static TagRow getTagByName(int cid, String name) {
    ArrayList<Row> rows = tags.getRows();
    for(int r=0;r<rows.size();r++) {
      TagRow tag = (TagRow)rows.get(r);
      if (tag.cid == cid && tag.name.equals(name)) {
        return tag;
      }
    }
    return null;
  }
  public static TagRow getTagById(int id) {
    return (TagRow)tags.get(id);
  }
  public static TagRow[] getTags() {
    return tags.getRows().toArray(new TagRow[0]);
  }
  public static TagRow[] getTagsByCid(int cid) {
    ArrayList<Row> rows = tags.getRows();
    ArrayList<TagRow> ret = new ArrayList<TagRow>();
    for(int r=0;r<rows.size();r++) {
      TagRow tag = (TagRow)rows.get(r);
      if (tag.cid == cid) {
        ret.add(tag);
      }
    }
    return ret.toArray(new TagRow[0]);
  }
  public static boolean isTagInUse(int cid, String name) {
    String match1 = ",c" + cid + "#" + name + ",";
    String match2 = "," + name + ",";  //cid == 0 only
    ArrayList<Table> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      ArrayList<Row> blockRows = table.getRows();
      for(int r=0;r<blockRows.size();r++) {
        BlockRow row = (BlockRow)blockRows.get(r);
        if (row.tags.contains(match1)) return true;
        if (cid == 0 && row.tags.contains(match2)) return true;
      }
    }
    return false;
  }

  public static int addPanel(String name, boolean popup, boolean builtin) {
    PanelRow panel = new PanelRow();
    panel.name = name;
    panel.popup = popup;
    panel.builtin = builtin;
    panels.add(panel);
    panels.save();
    return panel.id;
  }

  public static int addPanel(String name, String display, boolean popup, boolean builtin) {
    PanelRow panel = new PanelRow();
    panel.name = name;
    panel.display = display;
    panel.popup = popup;
    panel.builtin = builtin;
    panels.add(panel);
    panels.save();
    return panel.id;
  }
  public static PanelRow getPanelByName(String name) {
    ArrayList<Row> rows = panels.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      PanelRow panel = (PanelRow)rows.get(a);
      if (panel.name.equals(name)) {
        return panel;
      }
    }
    return null;
  }
  public static PanelRow getPanelById(int pid) {
    return (PanelRow)panels.get(pid);
  }
  public static int getPanelIdByName(String name) {
    ArrayList<Row> rows = panels.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      PanelRow panel = (PanelRow)rows.get(a);
      if (panel.name.equals(name)) {
        return panel.id;
      }
    }
    return -1;
  }
  public static void deletePanelById(int id) {
    panels.remove(id);
    panels.save();
    Table table = getCellTableById(id);
    cells.remove(table.id);
  }
  public static PanelRow[] getPanelsUsingTagId(int id) {
    TagRow tag = (TagRow)tags.get(id);
    if (tag == null) return new PanelRow[0];
    String eq = "c" + tag.cid + "#" + tag.name;
    String start1 = "c" + tag.cid + "#" + tag.name+ "[";
    String start2 = "c" + tag.cid + "#" + tag.name+ ".";
    //cid == 0 only
    String eq3 = tag.name + ",";
    String start4 = tag.name+ "[";
    String start5 = tag.name+ ".";
    ArrayList<Integer> pids = new ArrayList<Integer>();
    ArrayList<Table> tables = cells.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      ArrayList<Row> cellRows = table.getRows();
      for(int r=0;r<cellRows.size();r++) {
        CellRow row = (CellRow)cellRows.get(r);
        int pid = row.pid;
        if (pids.contains(pid)) break;
        if (row.tag.equals(eq) || row.tag.startsWith(start1) || row.tag.startsWith(start2)) {
          pids.add(row.pid);
          break;
        }
        if (tag.cid == 0) {
          if (row.tag.equals(eq3) || row.tag.startsWith(start4) || row.tag.startsWith(start5)) {
            pids.add(row.pid);
            break;
          }
        }
      }
    }
    ArrayList<PanelRow> panels = new ArrayList<PanelRow>();
    for(int p=0;p<pids.size();p++) {
      PanelRow panel = getPanelById(pids.get(p));
      panels.add(panel);
    }
    return panels.toArray(new PanelRow[0]);
  }

  public static Table addCellTable(String name, int pid) {
    Table table = new Table();
    table.name = name;
    table.xid = pid;
    cells.add(table);
    return table;
  }
  public static Table getCellTableById(int pid) {
    ArrayList<Table> tables = cells.getTables();
    for(int a=0;a<tables.size();a++) {
      Table table = tables.get(a);
      if (table.xid == pid) return table;
    }
    return null;
  }
  public static void saveCellTable(String name) {
    cells.get(name).save();
  }
  public static CellRow[] getCells(String name) {
    Table table = cells.get(name);
    return table.getRows().toArray(new CellRow[0]);
  }
  public static CellRow getCell(int pid, int x, int y) {
    PanelRow panel = getPanelById(pid);
    String name = panel.name;
    Table table = cells.get(name);
    ArrayList<Row> rows = table.getRows();
    for(int r=0;r<rows.size();r++) {
      CellRow cell = (CellRow)rows.get(r);
      if (cell.x == x && cell.y == y) {
        return cell;
      }
    }
    return null;
  }
  public static void deleteCell(int pid, int x, int y) {
    PanelRow panel = getPanelById(pid);
    String name = panel.name;
    Table table = cells.get(name);
    ArrayList<Row> rows = table.getRows();
    for(int r=0;r<rows.size();r++) {
      CellRow cell = (CellRow)rows.get(r);
      if (cell.x == x && cell.y == y) {
        rows.remove(r);
        table.save();
        break;
      }
    }
  }

  public static void addFunction(String name) {
    FunctionRow func = new FunctionRow();
    func.name = name;
    func.revision = 1;
    func.comment = "";
    funcs.add(func);
    funcs.save();
    Table rungsTable = new Table();
    rungsTable.xid = func.id;
    rungs.add(rungsTable);
    rungsTable.save();
    Table blocksTable = new Table();
    blocksTable.xid = func.id;
    blocks.add(blocksTable);
    blocksTable.save();
  }
  public static int getFunctionIdByName(String name) {
    ArrayList<Row> rows = funcs.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      FunctionRow func = (FunctionRow)rows.get(a);
      if (func.name.equals(name)) {
        return func.id;
      }
    }
    return -1;
  }
  public static FunctionRow getFunctionById(int id) {
    ArrayList<Row> rows = funcs.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      FunctionRow func = (FunctionRow)rows.get(a);
      if (func.id == id) {
        return func;
      }
    }
    return null;
  }
  public static boolean isFunctionInUse(int id) {
    FunctionRow func = getFunctionById(id);
    String tags = "," + func.name + ",";
    ArrayList<Table> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      ArrayList<Row> blockRows = table.getRows();
      for(int r=0;r<blockRows.size();r++) {
        BlockRow row = (BlockRow)blockRows.get(r);
        if (row.name.equals("CALL") && row.tags.equals(tags)) return true;
      }
    }
    return false;
  }
  public static void deleteFunctionById(int id) {
    ArrayList<Row> rows = funcs.getRows();
    int cnt = rows.size();
    for(int a=0;a<cnt;a++) {
      FunctionRow func = (FunctionRow)rows.get(a);
      if (func.id == id) {
        rows.remove(a);
        break;
      }
    }
    funcs.save();
  }
  private static int getRungId(int fid) {
    ArrayList<Table> tables = rungs.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      if (table.xid == fid) return table.id;
    }
    return -1;
  }
  public static void addRung(int fid, int rid, String logic, String comment) {
    RungRow rung = new RungRow();
    rung.fid = fid;
    rung.rid = rid;
    rung.logic = logic;
    rung.comment = comment;
    int xid = getRungId(fid);
    Table table = rungs.get(xid);
    table.add(rung);
    table.save();
  }
  public static RungRow getRungById(int fid, int rid) {
    int xid = getRungId(fid);
    Table table = rungs.get(xid);
    ArrayList<Row> rows = table.getRows();
    for(int r=0;r<rows.size();r++) {
      RungRow rung = (RungRow)rows.get(r);
      if (rung.rid == rid) return rung;
    }
    return null;
  }
  public static RungRow[] getRungsById(int fid) {
    int xid = getRungId(fid);
    Table table = rungs.get(xid);
    return table.getRows().toArray(new RungRow[0]);
  }
  public static void saveRungById(int fid, int rid) {
    int xid = getRungId(fid);
    Table table = rungs.get(xid);
    table.save();
  }
  public static void deleteRungsById(int fid) {
    int xid = getRungId(fid);
    rungs.remove(xid);
  }
  public static void deleteRungById(int fid, int rid) {
    int xid = getRungId(fid);
    {
      Table table = rungs.get(xid);
      int id = -1;
      ArrayList<Row> rows = table.getRows();
      for(int r=0;r<rows.size();r++) {
        RungRow rung = (RungRow)rows.get(r);
        if (rung.rid == rid) {
          id = rung.id;
        } else if (rung.rid > rid) {
          rung.rid--;
        }
      }
      if (id != -1) table.remove(id);
      table.save();
    }
    {
      Table table = blocks.get(xid);
      int id = -1;
      ArrayList<Row> rows = table.getRows();
      for(int r=0;r<rows.size();r++) {
        BlockRow blk = (BlockRow)rows.get(r);
        if (blk.rid > rid) {
          blk.rid--;
        }
      }
      if (id != -1) table.remove(id);
      table.save();
    }
  }
  private static int getBlockId(int fid) {
    ArrayList<Table> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      if (table.xid == fid) return table.id;
    }
    return -1;
  }
  public static BlockRow[] getFunctionBlocksById(int fid) {
    int xid = getBlockId(fid);
    Table table = blocks.get(xid);
    return table.getRows().toArray(new BlockRow[0]);
  }
  public static void addBlock(int fid, int rid, int bid, String name, String tags) {
    JFLog.log("addBlock:" + fid + "," + rid + "," + bid);
    int xid = getBlockId(fid);
    BlockRow block = new BlockRow();
    block.fid = fid;
    block.rid = rid;
    block.bid = bid;
    block.name = name;
    block.tags = tags;
    Table table = blocks.get(xid);
    table.add(block);
    table.save();
  }
  public static BlockRow[] getRungBlocksById(int fid, int rid) {
    int xid = getBlockId(fid);
    Table table = blocks.get(xid);
    ArrayList<Row> rows = table.getRows();
    ArrayList<Row> ret = new ArrayList<Row>();
    for(int r=0;r<rows.size();r++) {
      BlockRow row = (BlockRow)rows.get(r);
      if (row.rid == rid) {
        ret.add(row);
      }
    }
    return ret.toArray(new BlockRow[0]);
  }
  public static BlockRow[] getBlocksUsingTagId(int id) {
    TagRow tag = getTagById(id);
    ArrayList<Table> tables = blocks.getTables();
    ArrayList<BlockRow> blocks = new ArrayList<BlockRow>();
    if (tag.cid == 0) {
      String match1 = ",t" + tag.name + ",";
      String match2 = ",t" + tag.name + "[";
      String match3 = ",t" + tag.name + ".";
      String match4 = ",tc0#" + tag.name + ",";
      String match5 = ",tc0#" + tag.name + "[";
      String match6 = ",tc0#" + tag.name + ".";
      for(int t=0;t<tables.size();t++) {
        Table table = tables.get(t);
        ArrayList<Row> blockRows = table.getRows();
        for(int r=0;r<blockRows.size();r++) {
          BlockRow row = (BlockRow)blockRows.get(r);
          String tags = row.tags;
          if (tags.contains(match1)) {
            blocks.add(row);
          }
          else if (tags.contains(match2)) {
            blocks.add(row);
          }
          else if (tags.contains(match3)) {
            blocks.add(row);
          }
          else if (tags.contains(match4)) {
            blocks.add(row);
          }
          else if (tags.contains(match5)) {
            blocks.add(row);
          }
          else if (tags.contains(match6)) {
            blocks.add(row);
          }
        }
      }
    } else {
      String match1 = ",tc" + tag.cid + "#" + tag.name + ",";
      String match2 = ",tc" + tag.cid + "#" + tag.name + "[";
      String match3 = ",tc" + tag.cid + "#" + tag.name + ".";
      for(int t=0;t<tables.size();t++) {
        Table table = tables.get(t);
        ArrayList<Row> blockRows = table.getRows();
        for(int r=0;r<blockRows.size();r++) {
          BlockRow row = (BlockRow)blockRows.get(r);
          String tags = row.tags;
          if (tags.contains(match1)) {
            blocks.add(row);
          }
          else if (tags.contains(match2)) {
            blocks.add(row);
          }
          else if (tags.contains(match3)) {
            blocks.add(row);
          }
        }
      }
    }
    return blocks.toArray(new BlockRow[0]);
  }
  public static void deleteBlocksById(int fid) {
    ArrayList<Table> tables = blocks.getTables();
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table table = tables.get(a);
      if (table.xid == fid) {
        blocks.remove(table.id);
        break;
      }
    }
  }
  public static void clearBlocksById(int fid) {
    int xid = getBlockId(fid);
    Table table = blocks.get(xid);
    table.clear();
    table.save();
  }
  public static void deleteRungBlocksById(int fid, int rid) {
    int xid = getBlockId(fid);
    Table table = blocks.get(xid);
    ArrayList<Row> rows = table.getRows();
    ArrayList<Row> ret = new ArrayList<Row>();
    for(int r=0;r<rows.size();) {
      BlockRow row = (BlockRow)rows.get(r);
      if (row.rid == rid) {
        rows.remove(r);
      } else {
        r++;
      }
    }
    table.save();
  }

  public static void addWatchTable(String name) {
    Table table = new Table();
    table.name = name;
    watches.add(table);
  }
  public static void deleteWatchTable(int wid) {
    watches.remove(wid);
  }
  public static void addWatchTag(int wid, String tag) {
    Table table = watches.get(wid);
    WatchRow row = new WatchRow();
    row.tag = tag;
    table.add(row);
    table.save();
  }
  public static WatchRow[] getWatchTagsById(int wid) {
    return watches.get(wid).getRows().toArray(new WatchRow[0]);
  }
  public static void deleteWatchTagById(int wid, int id) {
    Table table = watches.get(wid);
    table.remove(id);
    table.save();
  }

  public static void logAlarm(int id) {
    alarms.add(new AlarmRow(id));
  }

  public static AlarmRow[] getAlarms(long start, long end) {
    return (AlarmRow[])alarms.get(start, end);
  }


  public static boolean update(String tableName, String id, String col, String value, String type) {
    Table table = null;
    TableList tablelist = null;
    switch (tableName) {
      case "config": {
        table = config;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        for(int a=0;a<cnt;a++) {
          ConfigRow row = (ConfigRow)rows.get(a);
          if (row.name.equals(id)) {
            row.value = value;
            table.save();
            return true;
          }
        }
        break;
      }
      case "ctrls": {
        table = controllers;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "cid": {
            int newcid = Integer.valueOf(value);
            if (newcid == 0) return false;
            int rowidx = -1;
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.cid == newcid) return false; //already in use
              if (row.id == rowid) {
                rowidx = a;
              }
            }
            if (rowidx == -1) return false;  //not found
            ControllerRow row = (ControllerRow)rows.get(rowidx);
            row.cid = newcid;
            table.save();
            return true;
          }
          case "ip": {
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.id == rowid) {
                row.ip = value;
                table.save();
                return true;
              }
            }
            break;
          }
          case "type": {
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.id == rowid) {
                row.type = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "speed": {
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.id == rowid) {
                row.speed = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "tags": {
        table = tags;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                row.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
          case "type": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                row.type = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "arraysize": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                row.length = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "comment": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                row.comment = value;
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "watch": {
        tablelist = watches;
        ArrayList<Table> tables = tablelist.getTables();
        int cnt = tables.size();
        int tableid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              table = tables.get(a);
              if (table.id == tableid) {
                table.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "watchtags": {
        tablelist = watches;
        ArrayList<Table> tables = tablelist.getTables();
        int cnt = tables.size();
        String ids[] = id.split("_");
        int tableid = Integer.valueOf(ids[0]);
        int rowid = Integer.valueOf(ids[1]);
        switch (col) {
          case "tag": {
            for(int a=0;a<cnt;a++) {
              table = tables.get(a);
              if (table.id == tableid) {
                ArrayList<Row> rows = table.getRows();
                for(int b=0;b<rows.size();b++) {
                  WatchRow row = (WatchRow)rows.get(b);
                  if (row.id == rowid) {
                    row.tag = value;
                    table.save();
                    return true;
                  }
                }
              }
            }
            break;
          }
        }
        break;
      }
      case "udts": {
        table = udts;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              UDT row = (UDT)rows.get(a);
              if (row.id == rowid) {
                row.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
        }
      }
      case "udtmems": {
        table = udtmembers;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              UDTMember row = (UDTMember)rows.get(a);
              if (row.id == rowid) {
                row.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
          case "type": {
            for(int a=0;a<cnt;a++) {
              UDTMember row = (UDTMember)rows.get(a);
              if (row.id == rowid) {
                row.type = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "comment": {
            for(int a=0;a<cnt;a++) {
              UDTMember row = (UDTMember)rows.get(a);
              if (row.id == rowid) {
                row.comment = value;
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "panels": {
        table = panels;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "display": {
            for(int a=0;a<cnt;a++) {
              PanelRow row = (PanelRow)rows.get(a);
              if (row.id == rowid) {
                row.display = value;
                table.save();
                return true;
              }
            }
            break;
          }
        }
      }
      case "funcs": {
        table = funcs;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              FunctionRow row = (FunctionRow)rows.get(a);
              if (row.id == rowid) {
                row.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
        }
      }
      default: {
        JFLog.log("Error:Database.update():unknown table:" + tableName);
        return false;
      }
    }
    JFLog.log("Error:Database.update() failed for table:" + tableName + ":col=" + col + ":id=" + id);
    return false;
  }

  public static String select(String tableName, String id, String col, String type) {
    Table table = null;
    TableList tablelist = null;
    if (tableName.equals("null")) {
      JFLog.log("table == null");
      Main.trace();
      return null;
    }
    switch (tableName) {
      case "jfc_config": {
        table = Database.config;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        for(int a=0;a<cnt;a++) {
          ConfigRow row = (ConfigRow)rows.get(a);
          if (row.name.equals(id)) {
            return row.value;
          }
        }
        break;
      }
      case "ctrls": {
        table = controllers;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "cid": {
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.cid);
              }
            }
            break;
          }
          case "ip": {
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.id == rowid) {
                return row.ip;
              }
            }
            break;
          }
          case "type": {
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.type);
              }
            }
            break;
          }
          case "speed": {
            for(int a=0;a<cnt;a++) {
              ControllerRow row = (ControllerRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.speed);
              }
            }
            break;
          }
        }
        break;
      }
      case "tags": {
        table = tags;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                return row.name;
              }
            }
            break;
          }
          case "type": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.type);
              }
            }
            break;
          }
          case "arraysize": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.length);
              }
            }
            break;
          }
          case "comment": {
            for(int a=0;a<cnt;a++) {
              TagRow row = (TagRow)rows.get(a);
              if (row.id == rowid) {
                return row.comment;
              }
            }
            break;
          }
        }
        break;
      }
      case "watch": {
        tablelist = watches;
        ArrayList<Table> tables = tablelist.getTables();
        int cnt = tables.size();
        int tableid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              table = tables.get(a);
              if (table.id == tableid) {
                return table.name;
              }
            }
            break;
          }
        }
        break;
      }
      case "watchtags": {
        tablelist = watches;
        ArrayList<Table> tables = tablelist.getTables();
        int cnt = tables.size();
        String ids[] = id.split("_");
        int tableid = Integer.valueOf(ids[0]);
        int rowid = Integer.valueOf(ids[1]);
        switch (col) {
          case "tag": {
            for(int a=0;a<cnt;a++) {
              table = tables.get(a);
              if (table.id == tableid) {
                ArrayList<Row> rows = table.getRows();
                for(int b=0;b<rows.size();b++) {
                  WatchRow row = (WatchRow)rows.get(b);
                  if (row.id == rowid) {
                    return row.tag;
                  }
                }
              }
            }
            break;
          }
        }
        break;
      }
      case "udts": {
        table = udts;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              UDT row = (UDT)rows.get(a);
              if (row.id == rowid) {
                return row.name;
              }
            }
            break;
          }
        }
      }
      case "udtmems": {
        table = udtmembers;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              UDTMember row = (UDTMember)rows.get(a);
              if (row.id == rowid) {
                return row.name;
              }
            }
            break;
          }
          case "type": {
            for(int a=0;a<cnt;a++) {
              UDTMember row = (UDTMember)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.type);
              }
            }
            break;
          }
          case "comment": {
            for(int a=0;a<cnt;a++) {
              UDTMember row = (UDTMember)rows.get(a);
              if (row.id == rowid) {
                return row.comment;
              }
            }
            break;
          }
        }
        break;
      }
      case "panels": {
        table = panels;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "display": {
            for(int a=0;a<cnt;a++) {
              PanelRow row = (PanelRow)rows.get(a);
              if (row.id == rowid) {
                return row.display;
              }
            }
            break;
          }
        }
      }
      case "funcs": {
        table = funcs;
        ArrayList<Row> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              FunctionRow row = (FunctionRow)rows.get(a);
              if (row.id == rowid) {
                return row.name;
              }
            }
            break;
          }
        }
      }
      default: {
        JFLog.log("Error:Database.select():unknown table:" + tableName);
      }
    }
    return null;
  }
}

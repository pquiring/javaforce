package jfcontrols.db;

/** Database Service
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.db.*;
import javaforce.controls.*;

import jfcontrols.app.*;
import jfcontrols.tags.*;
import jfcontrols.db.*;

public class Database {

  //core system
  public static Table<ConfigRow> config;
  public static TableLog<AlarmRow> alarms;
  public static Table<PanelRow> panels;
  public static TableList<CellRow> cells;
  public static Table<ControllerRow> controllers;
  public static Table<UserRow> users;
  public static TableList<ListRow> lists;
  public static Table<LogicRow> logics;
  public static Table<UDT> udts;
  public static Table<UDTMember> udtmembers;
  public static Table<TagRow> tags;
  public static Table<FunctionRow> funcs;
  //card access system
  public static Table<CardReaderRow> readers;
  public static Table<CardRow> cards;
  public static Table<GroupRow> groups;
  public static Table<TimezoneRow> timezones;
  //vision system
  public static Table<VisionCameraRow> visioncameras;
  public static Table<VisionProgramRow> visionprograms;
  public static Table<VisionShotRow> visionshots;
  public static Table<VisionAreaRow> visionareas;  //ROI

  public static TableList<RungRow> rungs;
  public static TableList<BlockRow> blocks;
  public static TableList<WatchRow> watches;

  public static String Version = "1.2";

  public static int FUNC_INIT = 1;
  public static int FUNC_MAIN = 2;

  public static void start() {
    JFLog.log("Database starting...");

    //create/load tables
    config = new Table<ConfigRow>(() -> {return new ConfigRow();});
    config.load(Paths.dataPath + "/config/config.dat");
    alarms = new TableLog<AlarmRow>(Paths.logsPath, () -> {return new AlarmRow();});
    panels = new Table<PanelRow>(() -> {return new PanelRow();});
    panels.load(Paths.dataPath + "/config/panels.dat");
    cells = new TableList<CellRow>(() -> {return new CellRow();});
    cells.load(Paths.dataPath + "/config/cells");
    controllers = new Table<ControllerRow>(() -> {return new ControllerRow();});
    controllers.load(Paths.dataPath + "/config/ctrls.dat");
    users = new Table<UserRow>(() -> {return new UserRow();});
    users.load(Paths.dataPath + "/config/users.dat");
    lists = new TableList<ListRow>(() -> {return new ListRow();});
    lists.load(Paths.dataPath + "/config/lists");
    logics = new Table<LogicRow>(() -> {return new LogicRow();});
    logics.load(Paths.dataPath + "/config/logics.dat");
    udts = new Table<UDT>(() -> {return new UDT();});
    udts.load(Paths.dataPath + "/config/udts.dat");
    udtmembers = new Table<UDTMember>(() -> {return new UDTMember();});
    udtmembers.load(Paths.dataPath + "/config/udtmembers.dat");
    tags = new Table<TagRow>(() -> {return new TagRow();});
    tags.load(Paths.dataPath + "/config/tags.dat");
    funcs = new Table<FunctionRow>(() -> {return new FunctionRow();});
    funcs.load(Paths.dataPath + "/config/funcs.dat");
    rungs = new TableList<RungRow>(() -> {return new RungRow();});
    rungs.load(Paths.dataPath + "/config/rungs");
    blocks = new TableList<BlockRow>(() -> {return new BlockRow();});
    blocks.load(Paths.dataPath + "/config/blocks");
    watches = new TableList<WatchRow>(() -> {return new WatchRow();});
    watches.load(Paths.dataPath + "/config/watches");
    //card system tables
    readers = new Table<CardReaderRow>(() -> {return new CardReaderRow();});
    readers.load(Paths.dataPath + "/config/readers.dat");
    cards = new Table<CardRow>(() -> {return new CardRow();});
    cards.load(Paths.dataPath + "/config/cards.dat");
    groups = new Table<GroupRow>(() -> {return new GroupRow();});
    groups.load(Paths.dataPath + "/config/groups.dat");
    timezones = new Table<TimezoneRow>(() -> {return new TimezoneRow();});
    timezones.load(Paths.dataPath + "/config/timezones.dat");
    //vision system
    visioncameras = new Table<VisionCameraRow>(() -> {return new VisionCameraRow();});
    visioncameras.load(Paths.dataPath + "/config/visioncameras.dat");
    visionprograms = new Table<VisionProgramRow>(() -> {return new VisionProgramRow();});
    visionprograms.load(Paths.dataPath + "/config/visionprograms.dat");
    visionshots = new Table<VisionShotRow>(() -> {return new VisionShotRow();});
    visionshots.load(Paths.dataPath + "/config/visionshots.dat");
    visionareas = new Table<VisionAreaRow>(() -> {return new VisionAreaRow();});
    visionareas.load(Paths.dataPath + "/config/visionareas.dat");

    String version = getConfig("version");
    if (version == null) version = "0.0";
    JFLog.log("Database version=" + version);
    switch (version) {
      case "1.2":
        break;
      case "1.1":
        update_panels();
        update_version();
        break;
      case "1.0":
//        create_card_system();
        create_vision_system();
        update_version();
        break;
      case "0.0":
        create();
//        create_card_system();
        create_vision_system();
        break;
    }
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
    watches = null;
    readers = null;
    cards = null;
    groups = null;
    timezones = null;
    visioncameras = null;
    visionprograms = null;
    visionshots = null;
    visionareas = null;
  }

  public static void restart() {
    stop();
    start();
  }

  private static void update_version() {
    setConfig("version", Version);
    create_main_menu(getPanelByName("jfc_menu").id);
  }

  private static void update_panels() {
    //rename panels : add usr_ to user visible panels
    {
      ArrayList<PanelRow> rows = panels.getRows();
      for(PanelRow row : rows) {
        String name = row.name;
        JFLog.log("panel:" + row.name + ":" + row.id);
        if (name.startsWith("jfc_")) continue;
        if (name.startsWith("usr_")) continue;
        //add usr_
        row.name = "usr_" + name;
        Table<CellRow> table = (Table<CellRow>)cells.get(name);
        if (table == null) {
          table = addCellTable(row.name, row.id);
        } else {
          table.name = "usr_" + name;
        }
        table.save();
      }
    }
    panels.save();
    {
      ArrayList<Table<CellRow>> rows = cells.getTables();
      int cnt = rows.size();
      for(int idx = 0;idx<cnt;) {
        Table<CellRow> row = rows.get(idx);
        JFLog.log("cellTable:" + row.name + ":" + row.xid);
        if (Database.getPanelById(row.xid) == null) {
          JFLog.log("deleteCellTable:" + row.name + ":" + row.xid);
          cells.remove(row.id);
          cnt--;
        } else {
          idx++;
        }
      }
    }
  }

  private static void create() {
    JFLog.log("Database create...");
    Table<ListRow> list;
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
    Table<CellRow> celltable = addCellTable("jfc_login", id);
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

    id = addPanel("usr_Main", false, false);
    celltable = addCellTable("usr_Main", id);
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
    create_main_menu(id);

    id = addPanel("jfc_controllers", false, true);
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

    id = addPanel("jfc_config", false, true);
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

    id = addPanel("jfc_alarm_editor", false, true);
    celltable = addCellTable("jfc_alarm_editor", id);
    celltable.add(new CellRow(id,2,1,2,1,"label","","Index"));
    celltable.add(new CellRow(id,4,1,8,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_alarm_editor_new"));
    celltable.add(new CellRow(id,16,1,2,1,"link","","Help").setArg("alarms"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_alarm_editor_table", ""));
    celltable.save();

    //display active alarms
    id = addPanel("jfc_alarms", false, true);
    celltable = addCellTable("jfc_alarms", id);
    celltable.add(new CellRow(id,2,1,2,1,"label","","Ack"));
    celltable.add(new CellRow(id,4,1,10,1,"label","","Name"));
    celltable.add(new CellRow(id,16,1,3,1,"button","","History").setFuncArg("setPanel","jfc_alarm_history"));
    celltable.add(new CellRow(id,2,2,0,0,"autoscroll","jfc_alarms", ""));
    celltable.save();

    id = addPanel("jfc_alarm_history", false, true);
    celltable = addCellTable("jfc_alarm_history", id);
    celltable.add(new CellRow(id,2,1,4,1,"label","","Time"));
    celltable.add(new CellRow(id,6,1,10,1,"label","","Name"));
    celltable.add(new CellRow(id,2,2,0,0,"autoscroll","jfc_alarm_history", ""));
    celltable.save();

    id = addPanel("jfc_tags", false, true);
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

    id = addPanel("jfc_xref", false, true);
    celltable = addCellTable("jfc_xref", id);
    celltable.add(new CellRow(id,1,1,0,0,"table","jfc_xref", ""));
    celltable.save();

    id = addPanel("jfc_watch", false, true);
    celltable = addCellTable("jfc_watch", id);
    celltable.add(new CellRow(id,2,1,6,1,"label","","Name"));
    celltable.add(new CellRow(id,9,1,2,1,"button","","New").setFunc("jfc_watch_new"));
    celltable.add(new CellRow(id,12,1,2,1,"link","","Help").setArg("watch"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_watch", ""));
    celltable.save();

    id = addPanel("jfc_watch_tags", false, true);
    celltable = addCellTable("jfc_watch_tags", id);
    celltable.add(new CellRow(id,2,1,6,1,"label","","Name"));
    celltable.add(new CellRow(id,8,1,6,1,"label","","Value"));
    celltable.add(new CellRow(id,15,1,2,1,"button","","New").setFunc("jfc_watch_tags_new"));
    celltable.add(new CellRow(id,18,1,2,1,"button","","Start").setFunc("jfc_watch_tags_start"));
    celltable.add(new CellRow(id,21,1,2,1,"link","","Help").setArg("watch_tags"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_watch_tags", ""));
    celltable.save();

    id = addPanel("jfc_udts", false, true);
    celltable = addCellTable("jfc_udts", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_udts_new"));
    celltable.add(new CellRow(id,16,1,3,1,"button","","Save").setFunc("jfc_udts_save"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("udt"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_udts", ""));
    celltable.save();

    id = addPanel("jfc_udt_editor", false, true);
    celltable = addCellTable("jfc_udt_editor", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,9,1,2,1,"label","","Type"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_udt_editor_new"));
    celltable.add(new CellRow(id,16,1,3,1,"button","","Save").setFunc("jfc_udt_editor_save"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("udt_editor"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_udt_editor", ""));
    celltable.save();

    id = addPanel("jfc_sdts", false, true);
    celltable = addCellTable("jfc_sdts", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,10,1,2,1,"link","","Help").setArg("sdt"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_sdts", ""));
    celltable.save();

    id = addPanel("jfc_sdt_editor", false, true);
    celltable = addCellTable("jfc_sdt_editor", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,9,1,2,1,"label","","Type"));
    celltable.add(new CellRow(id,12,1,2,1,"link","","Help").setArg("sdt_viewer"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_sdt_editor", ""));
    celltable.save();

    id = addPanel("jfc_panels", false, true);
    celltable = addCellTable("jfc_panels", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_panels_new"));
    celltable.add(new CellRow(id,16,1,2,1,"link","","Help").setArg("panels"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_panels", ""));
    celltable.save();

    id = addPanel("jfc_panel_editor", false, true);
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

    celltable.add(new CellRow(id,0,4,2,1,"label","", "Tag"));
    celltable.add(new CellRow(id,3,4,5,1,"textfield","tag", ""));
    celltable.add(new CellRow(id,0,5,2,1,"label","", "Press"));
    celltable.add(new CellRow(id,3,5,5,1,"textfield","press", ""));
    celltable.add(new CellRow(id,0,6,2,1,"label","", "Release"));
    celltable.add(new CellRow(id,3,6,5,1,"textfield","release", ""));
    celltable.add(new CellRow(id,0,7,2,1,"label","", "Click"));
    celltable.add(new CellRow(id,3,7,5,1,"textfield","click", ""));
    celltable.add(new CellRow(id,0,8,2,1,"button","","OK").setFunc("jfc_panel_props_ok"));
    celltable.add(new CellRow(id,3,8,2,1,"button","","Cancel").setFunc("jfc_panel_props_cancel"));
    celltable.save();

    id = addPanel("jfc_funcs", false, true);
    celltable = addCellTable("jfc_funcs", id);
    celltable.add(new CellRow(id,2,1,7,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_funcs_new"));
    celltable.add(new CellRow(id,16,1,2,1,"link","","Help").setArg("funcs"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_funcs", ""));
    celltable.save();

    id = addPanel("jfc_func_editor", false, true);
    celltable = addCellTable("jfc_func_editor", id);
    celltable.add(new CellRow(id,1,1,1,1,"button","","!image:add").setFunc("jfc_func_editor_add_rung"));
    celltable.add(new CellRow(id,3,1,1,1,"button","","!image:minus").setFunc("jfc_func_editor_del_rung"));
    celltable.add(new CellRow(id,5,1,2,1,"button","","Edit").setFunc("jfc_func_editor_edit_rung"));
    celltable.add(new CellRow(id,8,1,2,1,"button","","Compile").setFunc("jfc_func_editor_compile"));
    celltable.add(new CellRow(id,11,1,2,1,"button","jfc_debug","Debug").setFunc("jfc_func_editor_debug"));
    celltable.add(new CellRow(id,14,1,2,1,"link","","Help").setArg("func_editor"));
    celltable.add(new CellRow(id,0,0,0,0,"autoscroll","jfc_rungs_viewer", ""));
    celltable.save();

    id = addPanel("jfc_rung_editor", false, true);
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
    addFunction("init");  //1
    addFunction("main");  //2

    udts.setMinId(IDs.uid_user);
  }

  private static void create_main_menu(int id) {
    deleteCellTableById(id);
    Table<CellRow> celltable = addCellTable("jfc_menu", id);
    celltable.add(new CellRow(id,0,0,3,1,"button","","Main Panel").setFuncArg("setPanel","Main"));
    celltable.add(new CellRow(id,0,1,3,1,"button","","Controllers").setFuncArg("setPanel","jfc_controllers"));
    celltable.add(new CellRow(id,0,2,3,1,"button","","Tags").setFuncArg("jfc_ctrl_tags","1"));
    celltable.add(new CellRow(id,0,3,3,1,"button","","UserDataTypes").setFuncArg("setPanel","jfc_udts"));
    celltable.add(new CellRow(id,0,4,3,1,"button","","SysDataTypes").setFuncArg("setPanel","jfc_sdts"));
    celltable.add(new CellRow(id,0,5,3,1,"button","","Panels").setFuncArg("setPanel","jfc_panels"));
    celltable.add(new CellRow(id,0,6,3,1,"button","","Functions").setFuncArg("setPanel","jfc_funcs"));
    celltable.add(new CellRow(id,0,7,3,1,"button","","Alarms").setFuncArg("setPanel","jfc_alarm_editor"));
    celltable.add(new CellRow(id,0,8,3,1,"button","","Config").setFuncArg("setPanel","jfc_config"));
    celltable.add(new CellRow(id,0,9,3,1,"button","","Watch").setFuncArg("setPanel","jfc_watch"));
    celltable.add(new CellRow(id,0,10,3,1,"button","","Cameras").setFuncArg("setPanel","jfc_vision_cameras"));
    celltable.add(new CellRow(id,0,11,3,1,"button","","Vision").setFuncArg("setPanel","jfc_vision_programs"));
    celltable.add(new CellRow(id,0,12,3,1,"button","","Logoff").setFunc("jfc_logout"));
    celltable.save();
  }

  private static void create_card_system() {
    //addReader("name", "addr", -1);
    //addCard(123456789, -1);  //card#,user id
    addGroup("No Access");  //default access level
    addTimezone("24/7 Access");
  }

  private static void create_vision_system() {
    Table<CellRow> celltable;
    int id;

    id = addPanel("jfc_vision_cameras", false, true);
    celltable = addCellTable("jfc_vision_cameras", id);
    celltable.add(new CellRow(id,2,1,1,1,"label","","CID"));
    celltable.add(new CellRow(id,3,1,3,1,"label","","Name"));
    celltable.add(new CellRow(id,6,1,5,1,"label","","URL"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_vision_camera_new"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("vision_cameras"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_vision_cameras",""));
    celltable.save();

    id = addPanel("jfc_vision_programs", false, true);
    celltable = addCellTable("jfc_vision_programs", id);
    celltable.add(new CellRow(id,2,1,1,1,"label","","PID"));
    celltable.add(new CellRow(id,3,1,3,1,"label","","Name"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_vision_program_new"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("vision_programs"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_vision_programs",""));
    celltable.save();

    id = addPanel("jfc_vision_shots", false, true);
    celltable = addCellTable("jfc_vision_shots", id);
    celltable.add(new CellRow(id,2,1,2,1,"label","","Camera"));
    celltable.add(new CellRow(id,4,1,2,1,"label","","Offset"));
    celltable.add(new CellRow(id,12,1,3,1,"button","","New").setFunc("jfc_vision_shot_new"));
    celltable.add(new CellRow(id,20,1,2,1,"link","","Help").setArg("vision_shots"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_vision_shots",""));
    celltable.add(new CellRow(id,20,1,3,1,"button","","Load Last").setFunc("jfc_vision_shot_load_last"));
    celltable.add(new CellRow(id,23,1,3,1,"button","","Load OK").setFunc("jfc_vision_shot_load_ok"));
    celltable.add(new CellRow(id,26,1,3,1,"button","","Load NOK").setFunc("jfc_vision_shot_load_nok"));
    celltable.add(new CellRow(id,23,2,3,1,"button","","Save OK").setFunc("jfc_vision_shot_save_ok"));
    celltable.add(new CellRow(id,26,2,3,1,"button","","Save NOK").setFunc("jfc_vision_shot_save_nok"));
    celltable.add(new CellRow(id,20,4,32,18,"layers","","jfc_vision_area"));
    celltable.save();

    id = addPanel("jfc_vision_areas", false, true);
    celltable = addCellTable("jfc_vision_areas", id);
    celltable.add(new CellRow(id,2,1,3,1,"label","","Name"));
    celltable.add(new CellRow(id,7,1,2,1,"label","","x1"));
    celltable.add(new CellRow(id,9,1,2,1,"label","","y1"));
    celltable.add(new CellRow(id,11,1,2,1,"label","","x2"));
    celltable.add(new CellRow(id,13,1,2,1,"label","","y2"));
    celltable.add(new CellRow(id,15,1,3,1,"button","","New").setFunc("jfc_vision_area_new"));
    celltable.add(new CellRow(id,30,1,2,1,"link","","Help").setArg("vision_areas"));
    celltable.add(new CellRow(id,2,2,0,0,"table","jfc_vision_areas",""));
    celltable.add(new CellRow(id,20,4,32,18,"layers","","jfc_vision_area"));
    celltable.save();
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
    ArrayList<ConfigRow> rows = config.getRows();
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
    ArrayList<ConfigRow> rows = config.getRows();
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
    ArrayList<UserRow> rows = users.getRows();
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

  public static Table<ListRow> addList(String name) {
    Table<ListRow> list = new Table<>(() -> {return new ListRow();});
    list.name = name;
    lists.add(list);
    return list;
  }
  public static Table<ListRow> getList(String name) {
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
    ArrayList<ControllerRow> rows = controllers.getRows();
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
    ArrayList<ControllerRow> rows = controllers.getRows();
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
    ArrayList<ControllerRow> rows = controllers.getRows();
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
    ArrayList<Table<BlockRow>> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table<BlockRow> table = tables.get(t);
      ArrayList<BlockRow> blockRows = table.getRows();
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
    ArrayList<LogicRow> rows = logics.getRows();
    ArrayList<LogicRow> ret = new ArrayList<LogicRow>();
    for(int r=0;r<rows.size();r++) {
      LogicRow row = (LogicRow)rows.get(r);
      if (row.group.equals(group)) {
        ret.add(row);
      }
    }
    return ret.toArray(new LogicRow[0]);
  }
  public static String[] getLogicGroups() {
    ArrayList<LogicRow> rows = logics.getRows();
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
    ArrayList<TagRow> rows = tags.getRows();
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
    ArrayList<UDTMember> rows = udtmembers.getRows();
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
    ArrayList<UDTMember> rows = udtmembers.getRows();
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
    ArrayList<UDTMember> rows = udtmembers.getRows();
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
    ArrayList<UDTMember> rows = udtmembers.getRows();
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
    ArrayList<TagRow> rows = tags.getRows();
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
    ArrayList<TagRow> rows = tags.getRows();
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
    ArrayList<Table<BlockRow>> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table<BlockRow> table = tables.get(t);
      ArrayList<BlockRow> blockRows = table.getRows();
      for(int r=0;r<blockRows.size();r++) {
        BlockRow row = (BlockRow)blockRows.get(r);
        if (row.tags.contains(match1)) return true;
        if (cid == 0 && row.tags.contains(match2)) return true;
      }
    }
    return false;
  }
  public static void deleteTag(int id) {
    tags.remove(id);
    tags.save();
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

  public static PanelRow getPanelByName(String name) {
    ArrayList<PanelRow> rows = panels.getRows();
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
    ArrayList<PanelRow> rows = panels.getRows();
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
    ArrayList<Table<CellRow>> tables = cells.getTables();
    for(int t=0;t<tables.size();t++) {
      Table<CellRow> table = tables.get(t);
      ArrayList<CellRow> cellRows = table.getRows();
      for(int r=0;r<cellRows.size();r++) {
        CellRow row = (CellRow)cellRows.get(r);
        int pid = row.pid;
        if (pids.contains(pid)) break;
        if (row.tag == null) continue;
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

  public static Table<CellRow> addCellTable(String name, int pid) {
    Table<CellRow> table = new Table<>(() -> {return new CellRow();});
    table.name = name;
    table.xid = pid;
    cells.add(table);
    table.save();
    return table;
  }
  public static Table<CellRow> getCellTableById(int pid) {
    ArrayList<Table<CellRow>> tables = cells.getTables();
    for(int a=0;a<tables.size();a++) {
      Table<CellRow> table = tables.get(a);
      if (table.xid == pid) return table;
    }
    return null;
  }
  public static void saveCellTable(String name) {
    cells.get(name).save();
  }
  public static void deleteCellTableById(int pid) {
    Table table = getCellTableById(pid);
    if (table != null) {
      cells.remove(table.id);
    }
  }
  public static CellRow[] getCells(String name) {
    Table<CellRow> table = cells.get(name);
    return table.getRows().toArray(new CellRow[0]);
  }
  public static CellRow getCell(int pid, int x, int y) {
    Table<CellRow> table = getCellTableById(pid);
    ArrayList<CellRow> rows = table.getRows();
    for(int r=0;r<rows.size();r++) {
      CellRow cell = (CellRow)rows.get(r);
      if (
        (x >= cell.x) &&
        (x <= (cell.x + cell.w - 1)) &&
        (y >= cell.y) &&
        (y <= (cell.y + cell.h - 1))
      )
      {
        return cell;
      }
    }
    return null;
  }
  public static void deleteCell(int pid, int x, int y) {
    PanelRow panel = getPanelById(pid);
    String name = panel.name;
    Table<CellRow> table = cells.get(name);
    ArrayList<CellRow> rows = table.getRows();
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
    Table<RungRow> rungsTable = new Table<>(() -> {return new RungRow();});
    rungsTable.xid = func.id;
    rungs.add(rungsTable);
    rungsTable.save();
    Table<BlockRow> blocksTable = new Table<>(() -> {return new BlockRow();});
    blocksTable.xid = func.id;
    blocks.add(blocksTable);
    blocksTable.save();
  }
  public static int getFunctionIdByName(String name) {
    ArrayList<FunctionRow> rows = funcs.getRows();
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
    ArrayList<FunctionRow> rows = funcs.getRows();
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
    ArrayList<Table<BlockRow>> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table<BlockRow> table = tables.get(t);
      ArrayList<BlockRow> blockRows = table.getRows();
      for(int r=0;r<blockRows.size();r++) {
        BlockRow row = (BlockRow)blockRows.get(r);
        if (row.name.equals("CALL") && row.tags.equals(tags)) return true;
      }
    }
    return false;
  }
  public static void deleteFunctionById(int id) {
    ArrayList<FunctionRow> rows = funcs.getRows();
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
  public static FunctionRow[] getFunctions() {
    return funcs.getRows().toArray(new FunctionRow[funcs.getRows().size()]);
  }
  private static int getRungId(int fid) {
    ArrayList<Table<RungRow>> tables = rungs.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      if (table.xid == fid) return table.id;
    }
    return -1;
  }
  public static void addRung(int fid, int rid, String logic, String comment) {
    RungRow rows[] = getRungsById(fid, false);  //reverse order
    BlockRow blks[] = getBlocksById(fid);
    for(int a=0;a<rows.length;a++) {
      if (rows[a].rid >= rid) {
        rows[a].rid++;
      }
    }
    for(int b=0;b<blks.length;b++) {
      if (blks[b].rid >= rid) {
        blks[b].rid++;
      }
    }
    RungRow rung = new RungRow();
    rung.fid = fid;
    rung.rid = rid;
    rung.logic = logic;
    rung.comment = comment;
    int xid = getRungId(fid);
    Table<RungRow> table = rungs.get(xid);
    table.add(rung);
    table.save();
    saveBlocksById(fid);
  }
  public static RungRow getRungById(int fid, int rid) {
    int xid = getRungId(fid);
    Table<RungRow> table = rungs.get(xid);
    ArrayList<RungRow> rows = table.getRows();
    for(int r=0;r<rows.size();r++) {
      RungRow rung = (RungRow)rows.get(r);
      if (rung.rid == rid) return rung;
    }
    return null;
  }
  public static RungRow[] getRungsById(int fid, boolean forwardOrder) {
    int xid = getRungId(fid);
    Table<RungRow> table = rungs.get(xid);
    RungRow[] rows = table.getRows().toArray(new RungRow[0]);
    //must sort by rid
    RungRow tmp;
    if (forwardOrder) {
      for(int a=0;a<rows.length;a++) {
        for(int b=a+1;b<rows.length;b++) {
          if (rows[a].rid > rows[b].rid) {
            //swap a,b
            tmp = rows[a];
            rows[a] = rows[b];
            rows[b] = tmp;
          }
        }
      }
    } else {
      for(int a=0;a<rows.length;a++) {
        for(int b=a+1;b<rows.length;b++) {
          if (rows[a].rid < rows[b].rid) {
            //swap a,b
            tmp = rows[a];
            rows[a] = rows[b];
            rows[b] = tmp;
          }
        }
      }
    }
    return rows;
  }
  public static void saveRungsById(int fid) {
    int xid = getRungId(fid);
    Table table = rungs.get(xid);
    table.save();
  }
  public static void saveBlocksById(int fid) {
    int xid = getBlockId(fid);
    Table table = blocks.get(xid);
    table.save();
  }
  public static void deleteRungsById(int fid) {
    int xid = getRungId(fid);
    rungs.remove(xid);
  }
  public static void deleteRungById(int fid, int rid) {
    int xid = getRungId(fid);
    {
      Table<RungRow> table = rungs.get(xid);
      int id = -1;
      ArrayList<RungRow> rows = table.getRows();
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
      Table<BlockRow> table = blocks.get(xid);
      ArrayList<BlockRow> rows = table.getRows();
      for(int r=0;r<rows.size();) {
        BlockRow blk = (BlockRow)rows.get(r);
        if (blk.rid == rid) {
          rows.remove(r);
        } else {
          if (blk.rid > rid) {
            blk.rid--;
          }
          r++;
        }
      }
      table.save();
    }
  }
  private static int getBlockId(int fid) {
    ArrayList<Table<BlockRow>> tables = blocks.getTables();
    for(int t=0;t<tables.size();t++) {
      Table table = tables.get(t);
      if (table.xid == fid) return table.id;
    }
    return -1;
  }
  public static BlockRow[] getBlocksById(int fid) {
    int xid = getBlockId(fid);
    Table<BlockRow> table = blocks.get(xid);
    return table.getRows().toArray(new BlockRow[0]);
  }
  public static void addBlock(int fid, int rid, int bid, String name, String tags) {
    int xid = getBlockId(fid);
    BlockRow block = new BlockRow();
    block.fid = fid;
    block.rid = rid;
    block.bid = bid;
    block.name = name;
    block.tags = tags;
    Table<BlockRow> table = blocks.get(xid);
    table.add(block);
    table.save();
  }
  public static BlockRow[] getRungBlocksById(int fid, int rid) {
    int xid = getBlockId(fid);
    Table<BlockRow> table = blocks.get(xid);
    ArrayList<BlockRow> rows = table.getRows();
    ArrayList<BlockRow> ret = new ArrayList<>();
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
    ArrayList<Table<BlockRow>> tables = blocks.getTables();
    ArrayList<BlockRow> blocks = new ArrayList<BlockRow>();
    if (tag.cid == 0) {
      String match1 = ",t" + tag.name + ",";
      String match2 = ",t" + tag.name + "[";
      String match3 = ",t" + tag.name + ".";
      String match4 = ",tc0#" + tag.name + ",";
      String match5 = ",tc0#" + tag.name + "[";
      String match6 = ",tc0#" + tag.name + ".";
      for(int t=0;t<tables.size();t++) {
        Table<BlockRow> table = tables.get(t);
        ArrayList<BlockRow> blockRows = table.getRows();
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
        Table<BlockRow> table = tables.get(t);
        ArrayList<BlockRow> blockRows = table.getRows();
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
    ArrayList<Table<BlockRow>> tables = blocks.getTables();
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table table = tables.get(a);
      if (table.xid == fid) {
        blocks.remove(table.id);
        break;
      }
    }
  }
  public static void clearBlocksById(int fid, int rid) {
    int xid = getBlockId(fid);
    Table<BlockRow> table = blocks.get(xid);
    ArrayList<BlockRow> rows = table.getRows();
    for(int a=0;a<rows.size();) {
      BlockRow row = (BlockRow)rows.get(a);
      if (row.rid == rid) {
        rows.remove(a);
      } else {
        a++;
      }
    }
    table.save();
  }
  public static void deleteRungBlocksById(int fid, int rid) {
    int xid = getBlockId(fid);
    Table<BlockRow> table = blocks.get(xid);
    ArrayList<BlockRow> rows = table.getRows();
    ArrayList<BlockRow> ret = new ArrayList<>();
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
    Table<WatchRow> table = new Table<>(() -> {return new WatchRow();});
    table.name = name;
    watches.add(table);
  }
  public static void deleteWatchTable(int wid) {
    watches.remove(wid);
  }
  public static void addWatchTag(int wid, String tag) {
    Table<WatchRow> table = watches.get(wid);
    WatchRow row = new WatchRow();
    row.tag = tag;
    table.add(row);
    table.save();
  }
  public static WatchRow[] getWatchTagsById(int wid) {
    return (WatchRow[])watches.get(wid).getRows().toArray(new WatchRow[0]);
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

  public static void addReader(String name, String addr, int type) {
    CardReaderRow reader = new CardReaderRow();
    reader.name = name;
    reader.addr = addr;
    reader.type = type;
    readers.add(reader);
    readers.save();
  }

  public static void addCard(long cardnum, int uid) {
    CardRow card = new CardRow();
    card.card = cardnum;
    card.uid = uid;
    cards.add(card);
    cards.save();
  }

  public static void addGroup(String name) {
    GroupRow group = new GroupRow();
    group.name = name;
    groups.add(group);
    groups.save();
  }

  public static void addTimezone(String name) {
    TimezoneRow timezone = new TimezoneRow();
    timezone.name = name;
    for(int a=0;a<7;a++) {
      timezone.begin[a] = new TimezoneRow.Time();
      timezone.begin[a].hour = 0;
      timezone.begin[a].min = 0;
      timezone.end[a] = new TimezoneRow.Time();
      timezone.end[a].hour = 23;
      timezone.end[a].min = 59;
    }
    timezones.add(timezone);
    timezones.save();
  }

  //vision cameras

  public static void addVisionCamera() {
    VisionCameraRow row = new VisionCameraRow();
    int cnt = visioncameras.getCount()+1;
    //TODO : make sure cnt is unique
    row.cid = cnt;
    row.name = "Camera " + cnt;
    row.url = "";
    visioncameras.add(row);
    visioncameras.save();
  }

  public static void deleteVisionCamera(int id) {
    visioncameras.remove(id);
    visioncameras.save();
  }

  //vision programs

  public static void addVisionProgram() {
    VisionProgramRow row = new VisionProgramRow();
    row.name = "Program " + (visionprograms.getCount()+1);
    row.pid = visionprograms.getCount() + 1;
    while (getVisionProgram(row.pid) != null) {
      row.pid++;
    }
    visionprograms.add(row);
    visionprograms.save();
  }

  public static VisionProgramRow getVisionProgram(int pid) {
    int count = visionprograms.getCount();
    ArrayList<VisionProgramRow> rows = visionprograms.getRows();
    for(int a=0;a<count;a++) {
      VisionProgramRow row = (VisionProgramRow)rows.get(a);
      if (row.pid == pid) return row;
    }
    return null;
  }

  public static void deleteVisionProgram(int id) {
    int pid = 0;
    visionprograms.remove(id);
    visionprograms.save();
    deleteVisionAreas(pid);
  }

  public static int getVisionProgramPID(int id) {
    int count = visionprograms.getCount();
    ArrayList<VisionProgramRow> rows = visionprograms.getRows();
    for(int a=0;a<count;a++) {
      VisionProgramRow row = (VisionProgramRow)rows.get(a);
      if (row.id == id) return row.pid;
    }
    JFLog.log("Error:VisionProgramRow not found for id=" + id);
    return -1;
  }

  //vision shots

  public static void addVisionShot(int pid) {
    VisionShotRow row = new VisionShotRow();
    row.pid = pid;
    row.cid = 1;
    row.offset = 0;
    visionshots.add(row);
    visionshots.save();
    addVisionArea(row.pid, row.id, true);
  }

  public static void deleteVisionShot(int pid, int sid) {
    //TODO
  }

  //vision areas (ROIs)

  public static void addVisionArea(int pid, int sid, boolean locator) {
    VisionAreaRow row = new VisionAreaRow();
    if (locator) {
      row.name = "Locator";
    } else {
      row.name = "ROI-" + (getCountVisionArea(pid, sid)+1);
    }
    row.pid = pid;
    row.sid = sid;
    row.x1 = (1920 / 2) - 50;
    row.x2 = (1920 / 2) + 50;
    row.y1 = (1080 / 2) - 50;
    row.y2 = (1080 / 2) + 50;
    visionareas.add(row);
    visionareas.save();
  }

  public static void deleteVisionArea(int id) {
    visionareas.remove(id);
    visionareas.save();
  }

  public static void deleteVisionAreas(int pid) {
    int size = visionareas.getCount();
    ArrayList<VisionAreaRow> rows = visionareas.getRows();
    for(int a=0;a<size;) {
      VisionAreaRow row = (VisionAreaRow)rows.get(a);
      if (row.pid == pid) {
        visionareas.remove(row.id);
        size--;
      } else {
        a++;
      }
    }
  }

  public static void deleteVisionAreas(int pid, int sid) {
    int size = visionareas.getCount();
    ArrayList<VisionAreaRow> rows = visionareas.getRows();
    for(int a=0;a<size;) {
      VisionAreaRow row = (VisionAreaRow)rows.get(a);
      if (row.pid == pid && row.sid == sid) {
        visionareas.remove(row.id);
        size--;
      } else {
        a++;
      }
    }
  }

  public static int getCountVisionArea(int pid, int sid) {
    int size = visionareas.getCount();
    int count = 0;
    ArrayList<VisionAreaRow> rows = visionareas.getRows();
    for(int a=0;a<size;a++) {
      VisionAreaRow row = (VisionAreaRow)rows.get(a);
      if (row.pid == pid && row.sid == sid) count++;
    }
    return count;
  }

  public static VisionAreaRow[] getVisionAreas(int pid, int sid) {
    int count = getCountVisionArea(pid, sid);
    if (count == 0) return new VisionAreaRow[0];
    VisionAreaRow arearows[] = new VisionAreaRow[count];
    int size = visionareas.getCount();
    int pos = 0;
    ArrayList<VisionAreaRow> rows = visionareas.getRows();
    for(int a=0;a<size;a++) {
      VisionAreaRow row = (VisionAreaRow)rows.get(a);
      if (row.pid == pid && row.sid == sid) {
        arearows[pos++] = row;
      }
    }
    return arearows;
  }

  public static VisionAreaRow getVisionArea(int pid, int sid, int rid) {
    int count = getCountVisionArea(pid, sid);
    if (count == 0) return null;
    int size = visionareas.getCount();
    int pos = 0;
    ArrayList<VisionAreaRow> rows = visionareas.getRows();
    for(int a=0;a<size;a++) {
      VisionAreaRow row = (VisionAreaRow)rows.get(a);
      if (row.pid == pid && row.id == rid) {
        return row;
      }
    }
    return null;
  }

  public static VisionAreaRow getVisionAreaLocator(int pid) {
    int size = visionareas.getCount();
    int pos = 0;
    ArrayList<VisionAreaRow> rows = visionareas.getRows();
    for(int a=0;a<size;a++) {
      VisionAreaRow row = (VisionAreaRow)rows.get(a);
      //first area is always locator
      if (row.pid == pid) {
        return row;
      }
    }
    return null;
  }

  public static boolean update(String tableName, String id, String col, String value, String type) {
    switch (tableName) {
      case "config": {
        Table<ConfigRow> table = config;
        ArrayList<ConfigRow> rows = table.getRows();
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
        Table<ControllerRow> table = controllers;
        ArrayList<ControllerRow> rows = table.getRows();
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
        Table<TagRow> table = tags;
        ArrayList<TagRow> rows = table.getRows();
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
        Table<WatchRow> table = null;
        TableList<WatchRow> tablelist = watches;
        ArrayList<Table<WatchRow>> tables = tablelist.getTables();
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
        Table<WatchRow> table = null;
        TableList<WatchRow> tablelist = watches;
        ArrayList<Table<WatchRow>> tables = tablelist.getTables();
        int cnt = tables.size();
        String ids[] = id.split("_");
        int tableid = Integer.valueOf(ids[0]);
        int rowid = Integer.valueOf(ids[1]);
        switch (col) {
          case "tag": {
            for(int a=0;a<cnt;a++) {
              table = tables.get(a);
              if (table.id == tableid) {
                ArrayList<WatchRow> rows = table.getRows();
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
        Table<UDT> table = udts;
        ArrayList<UDT> rows = table.getRows();
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
        break;
      }
      case "udtmems": {
        Table<UDTMember> table = udtmembers;
        ArrayList<UDTMember> rows = table.getRows();
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
        Table<PanelRow> table = panels;
        ArrayList<PanelRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              PanelRow row = (PanelRow)rows.get(a);
              if (row.id == rowid) {
                String oldname = row.name;
                row.name = "usr_" + value;
                table.save();
                //must update cellTable name too
                Table<CellRow> cellTable = cells.get(oldname);
                cellTable.name = "usr_" + value;
                cellTable.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "funcs": {
        Table<FunctionRow> table = funcs;
        ArrayList<FunctionRow> rows = table.getRows();
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
        break;
      }
      case "visioncameras": {
        Table<VisionCameraRow> table = visioncameras;
        ArrayList<VisionCameraRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              VisionCameraRow row = (VisionCameraRow)rows.get(a);
              if (row.id == rowid) {
                row.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
          case "url": {
            for(int a=0;a<cnt;a++) {
              VisionCameraRow row = (VisionCameraRow)rows.get(a);
              if (row.id == rowid) {
                row.url = value;
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "visionprograms": {
        Table<VisionProgramRow> table = visionprograms;
        ArrayList<VisionProgramRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              VisionProgramRow row = (VisionProgramRow)rows.get(a);
              if (row.id == rowid) {
                row.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
          case "pid": {
            for(int a=0;a<cnt;a++) {
              VisionProgramRow row = (VisionProgramRow)rows.get(a);
              if (row.id == rowid) {
                int newpid = Integer.valueOf(value);
                VisionProgramRow row2 = getVisionProgram(newpid);
                if (row2 != null && row2 != row) {
                  //new value is not unique
                  return false;
                }
                row.pid = newpid;
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "visionshots": {
        Table<VisionShotRow> table = visionshots;
        ArrayList<VisionShotRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "camera": {
            for(int a=0;a<cnt;a++) {
              VisionShotRow row = (VisionShotRow)rows.get(a);
              if (row.id == rowid) {
                row.cid = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "offset": {
            for(int a=0;a<cnt;a++) {
              VisionShotRow row = (VisionShotRow)rows.get(a);
              if (row.id == rowid) {
                row.offset = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
      }
      case "visionareas": {
        Table<VisionAreaRow> table = visionareas;
        ArrayList<VisionAreaRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                row.name = value;
                table.save();
                return true;
              }
            }
            break;
          }
          case "x1": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                row.x1 = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "y1": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                row.y1 = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "x2": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                row.x2 = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
          case "y2": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                row.y2 = Integer.valueOf(value);
                table.save();
                return true;
              }
            }
            break;
          }
        }
        break;
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
    if (tableName.equals("null")) {
      JFLog.log("table == null");
      Main.trace();
      return null;
    }
    switch (tableName) {
      case "config": {
        Table<ConfigRow> table = Database.config;
        ArrayList<ConfigRow> rows = table.getRows();
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
        Table<ControllerRow> table = controllers;
        ArrayList<ControllerRow> rows = table.getRows();
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
        Table<TagRow> table = tags;
        ArrayList<TagRow> rows = table.getRows();
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
        Table<WatchRow> table = null;
        TableList<WatchRow> tablelist = watches;
        ArrayList<Table<WatchRow>> tables = tablelist.getTables();
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
        Table<WatchRow> table = null;
        TableList<WatchRow> tablelist = watches;
        ArrayList<Table<WatchRow>> tables = tablelist.getTables();
        int cnt = tables.size();
        String ids[] = id.split("_");
        int tableid = Integer.valueOf(ids[0]);
        int rowid = Integer.valueOf(ids[1]);
        switch (col) {
          case "tag": {
            for(int a=0;a<cnt;a++) {
              table = tables.get(a);
              if (table.id == tableid) {
                ArrayList<WatchRow> rows = table.getRows();
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
        Table<UDT> table = udts;
        ArrayList<UDT> rows = table.getRows();
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
        break;
      }
      case "udtmems": {
        Table<UDTMember> table = udtmembers;
        ArrayList<UDTMember> rows = table.getRows();
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
        Table<PanelRow> table = panels;
        ArrayList<PanelRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              PanelRow row = (PanelRow)rows.get(a);
              if (row.id == rowid) {
                return row.name.substring(4);  //remove usr_
              }
            }
            break;
          }
        }
        break;
      }
      case "funcs": {
        Table<FunctionRow> table = funcs;
        ArrayList<FunctionRow> rows = table.getRows();
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
        break;
      }
      case "visioncameras": {
        Table<VisionCameraRow> table = visioncameras;
        ArrayList<VisionCameraRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "cid": {
            for(int a=0;a<cnt;a++) {
              VisionCameraRow row = (VisionCameraRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.cid);
              }
            }
            break;
          }
          case "name": {
            for(int a=0;a<cnt;a++) {
              VisionCameraRow row = (VisionCameraRow)rows.get(a);
              if (row.id == rowid) {
                return row.name;
              }
            }
            break;
          }
          case "url": {
            for(int a=0;a<cnt;a++) {
              VisionCameraRow row = (VisionCameraRow)rows.get(a);
              if (row.id == rowid) {
                return row.url;
              }
            }
            break;
          }
        }
        break;
      }
      case "visionprograms": {
        Table<VisionProgramRow> table = visionprograms;
        ArrayList<VisionProgramRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              VisionProgramRow row = (VisionProgramRow)rows.get(a);
              if (row.id == rowid) {
                return row.name;
              }
            }
            break;
          }
          case "pid": {
            for(int a=0;a<cnt;a++) {
              VisionProgramRow row = (VisionProgramRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.pid);
              }
            }
            break;
          }
        }
        break;
      }
      case "visionshots": {
        Table<VisionShotRow> table = visionshots;
        ArrayList<VisionShotRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "cid": {
            for(int a=0;a<cnt;a++) {
              VisionShotRow row = (VisionShotRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.cid);
              }
            }
            break;
          }
          case "offset": {
            for(int a=0;a<cnt;a++) {
              VisionShotRow row = (VisionShotRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.offset);
              }
            }
            break;
          }
        }
        break;
      }
      case "visionareas": {
        Table<VisionAreaRow> table = visionareas;
        ArrayList<VisionAreaRow> rows = table.getRows();
        int cnt = rows.size();
        int rowid = Integer.valueOf(id);
        switch (col) {
          case "name": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                return row.name;
              }
            }
            break;
          }
          case "x1": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.x1);
              }
            }
            break;
          }
          case "y1": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.y1);
              }
            }
            break;
          }
          case "x2": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.x2);
              }
            }
            break;
          }
          case "y2": {
            for(int a=0;a<cnt;a++) {
              VisionAreaRow row = (VisionAreaRow)rows.get(a);
              if (row.id == rowid) {
                return Integer.toString(row.y2);
              }
            }
            break;
          }
        }
        break;
      }
      default: {
        JFLog.log("Error:Database.select():unknown table:" + tableName);
      }
    }
    JFLog.log("Error:Database.select():failed for table:" + tableName + ":col=" + col + ":id=" + id);
    return null;
  }
}

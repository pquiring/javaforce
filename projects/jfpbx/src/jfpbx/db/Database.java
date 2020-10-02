package jfpbx.db;

/** Database service.
 *
 * @author pquiring
 */

import jfpbx.core.Paths;
import java.util.*;

import javaforce.*;
import javaforce.db.*;


public class Database {

  public static String dbVersion = "1.0";

  public static Table<ConfigRow> config;
  public static Table<ExtensionRow> extensions;
  public static Table<TrunkRow> trunks;
  public static Table<RouteTableRow> outRouteTables;
  public static Table<RouteRow> inRoutes;
  public static Table<UserRow> users;

  public static void start() {
    JFLog.log("Database starting...");

    //create/load tables
    config = new Table<ConfigRow>(() -> {return new ConfigRow();});
    config.load(Paths.db + "/config.dat");
    extensions = new Table<ExtensionRow>(() -> {return new ExtensionRow();});
    extensions.load(Paths.db + "/extensions.dat");
    trunks = new Table<TrunkRow>(() -> {return new TrunkRow();});
    trunks.load(Paths.db + "/trunks.dat");
    outRouteTables = new Table<RouteTableRow>(() -> {return new RouteTableRow();});
    outRouteTables.load(Paths.db + "/outroutes.dat");
    inRoutes = new Table<RouteRow>(() -> {return new RouteRow();});
    inRoutes.load(Paths.db + "/inroutes.dat");
    users = new Table<UserRow>(() -> {return new UserRow();});
    users.load(Paths.db + "/users.dat");

    UserRow admin = getUser("admin");
    if (admin == null) {
      UserRow user = new UserRow();
      user.name = "admin";
      user.password = "21232f297a57a5a743894a0e4a801fc3";  //admin
      addUser(user);
      setConfig("version", dbVersion);
      RouteTableRow def = new RouteTableRow();
      def.name = "default";
      addOutRouteTable(def);
    }
  }

  //config

  public static String getConfig(String name) {
    for(ConfigRow config : config.getRows()) {
      if (config.name.equals(name)) {
        return config.value;
      }
    }
    return "";
  }

  public static void setConfig(String name, String value) {
    for(ConfigRow row : config.getRows()) {
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

  //users

  public static UserRow getUser(String name) {
    for(UserRow user : users.getRows()) {
      if (user.name.equals(name)) {
        return user;
      }
    }
    return null;
  }

  public static String getUserPassword(String name) {
    UserRow user = getUser(name);
    if (user == null) return null;
    return user.password;
  }

  public static void addUser(UserRow user) {
    users.add(user);
    users.save();
  }

  public static boolean setUserPassword(String name, String passmd5) {
    UserRow user = getUser(name);
    if (user == null) return false;
    user.password = passmd5;
    users.save();
    return true;
  }

  //extensions

  public static ExtensionRow getExtension(String number, int type) {
    for(ExtensionRow ext : extensions.getRows()) {
      if (ext.type == type && ext.number.equals(number)) {
        return ext;
      }
    }
    return null;
  }

  public static boolean extensionExists(String number) {
    for(ExtensionRow ext : extensions.getRows()) {
      if (ext.number.equals(number)) {
        return true;
      }
    }
    return false;
  }

  public static ExtensionRow getExtension(String number) {
    return getExtension(number, ExtensionRow.EXT);
  }

  public static ExtensionRow[] getExtensions(int type) {
    ArrayList<ExtensionRow> exts = new ArrayList<ExtensionRow>();
    for(ExtensionRow ext : extensions.getRows()) {
      if (ext.type == type) {
        exts.add(ext);
      }
    }
    return exts.toArray(new ExtensionRow[exts.size()]);
  }

  public static ExtensionRow[] getExtensions() {
    return getExtensions(ExtensionRow.EXT);
  }

  public static void addExtension(ExtensionRow ext, int type) {
    ext.type = type;
    extensions.add(ext);
    extensions.save();
  }

  public static void addExtension(ExtensionRow ext) {
    addExtension(ext, ExtensionRow.EXT);
  }

  public static void deleteExtension(String number) {
    ExtensionRow ext = getExtension(number);
    if (ext != null) {
      extensions.remove(ext.id);
      extensions.save();
    }
  }

  public static void saveExtensions() {
    extensions.save();
  }

  //inroutes

  public static RouteRow getInRoute(String name) {
    for(RouteRow row : inRoutes.getRows()) {
      if (row.name.equals(name)) {
        return row;
      }
    }
    return null;
  }

  public static RouteRow[] getInRoutes() {
    return inRoutes.getRows().toArray(new RouteRow[inRoutes.getCount()]);
  }

  public static void addInRoute(RouteRow route) {
    inRoutes.add(route);
    inRoutes.save();
  }

  public static void deleteInRoute(String number) {

  }

  //OutRouteTable

  public static RouteTableRow getOutRouteTable(String name) {
    for(RouteTableRow table : outRouteTables.getRows()) {
      if (table.name.equals(name)) {
        return table;
      }
    }
    return null;
  }

  public static RouteTableRow[] getOutRouteTables() {
    return outRouteTables.getRows().toArray(new RouteTableRow[outRouteTables.getCount()]);
  }

  public static void addOutRouteTable(RouteTableRow table) {
    outRouteTables.add(table);
    outRouteTables.save();
  }

  public static void deleteOutRouteTable(String name) {
    RouteTableRow table = getOutRouteTable(name);
    if (table != null) {
      outRouteTables.remove(table.id);
      outRouteTables.save();
    }
  }

  //OutRoute

  public static RouteRow getOutRoute(String tableName, String name) {
    RouteTableRow table = getOutRouteTable(tableName);
    if (table == null) return null;
    for(RouteRow row : table.routes) {
      if (row.name.equals(name)) {
        return row;
      }
    }
    return null;
  }

  public static RouteRow[] getOutRoutes(String tableName) {
    RouteTableRow table = getOutRouteTable(tableName);
    if (table != null) {
      return table.routes.toArray(new RouteRow[table.routes.size()]);
    }
    return null;
  }

  public static void addOutRoute(String tableName, RouteRow route) {
    RouteTableRow table = getOutRouteTable(tableName);
    if (table != null) {
      table.routes.add(route);
      outRouteTables.save();
    }
  }

  public static void saveOutRoutes(String table) {
    outRouteTables.save();
  }

  //Trunks

  public static TrunkRow getTrunk(String name) {
    for(TrunkRow trunk : trunks.getRows()) {
      if (trunk.name.equals(name)) {
        return trunk;
      }
    }
    return null;
  }

  public static TrunkRow[] getTrunks() {
    return trunks.getRows().toArray(new TrunkRow[trunks.getCount()]);
  }

  public static TrunkRow[] getTrunks(RouteRow route) {
    String[] routeTrunks = route.trunks.split(",");
    ArrayList<TrunkRow> rows = new ArrayList<TrunkRow>();
    for(TrunkRow row : trunks.getRows()) {
      for(int a=0;a<routeTrunks.length;a++) {
        if (row.name == routeTrunks[a]) {
          rows.add(row);
          break;
        }
      }
    }
    return rows.toArray(new TrunkRow[rows.size()]);
  }

  public static void addTrunk(TrunkRow trunk) {
    trunks.add(trunk);
    trunks.save();
  }

  public static void deleteTrunk(String name) {
    TrunkRow trunk = getTrunk(name);
    if (trunk != null) {
      trunks.remove(trunk.id);
      trunks.save();
    }
  }

  public static void saveTrunks() {
    trunks.save();
  }

  //IVRs

  public static ExtensionRow getIVR(String number) {
    return getExtension(number, ExtensionRow.IVR);
  }

  public static ExtensionRow[] getIVRs() {
    return getExtensions(ExtensionRow.IVR);
  }

  public static void addIVR(ExtensionRow ivr) {
    addExtension(ivr, ExtensionRow.IVR);
  }

  public static void deleteIVR(String number) {
    deleteExtension(number);
  }

  public static void saveIVRs() {
    extensions.save();
  }

  //Queues

  public static ExtensionRow getQueue(String number) {
    return getExtension(number, ExtensionRow.QUEUE);
  }

  public static ExtensionRow[] getQueues() {
    return getExtensions(ExtensionRow.QUEUE);
  }

  public static void addQueue(ExtensionRow queue) {
    addExtension(queue, ExtensionRow.QUEUE);
  }

  public static void deleteQueue(String number) {
    deleteExtension(number);
  }

  public static void saveQueues() {
    saveExtensions();
  }
}

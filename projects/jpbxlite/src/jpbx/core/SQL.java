package jpbx.core;

import java.sql.*;
import java.io.*;
import java.util.*;

import javaforce.*;

/** Provides access to database. */

public class SQL {
  private java.sql.Connection conn;
  private Exception lastException;

  public static String path;

  static {
    //setup derby.system.home
    if (JF.isWindows()) {
      path = System.getenv("ProgramData");  //Win Vista/7/8
      if (path == null) {
        path = System.getenv("AllUsersProfile");  //Win 2000/XP/2003
        if (path == null) {
          JFLog.log("Failed to find Program Data folder");
          System.exit(1);
        }
      }
      path += "/jpbxlite/";
    } else {
      path = "/var/lib";
    }
    System.setProperty("derby.system.home", path);
  }

  public static boolean initOnce() {
    try {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    SQL sql = new SQL();
    if (!sql.init()) return false;
    sql.uninit();
    return true;
  }

  public static boolean create() {
    if (new File(path + "/jpbxDB/service.properties").exists()) return true;
    JFLog.log("Creating database...");
    try {
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    SQL sql = new SQL();
    if (!sql.init("create=true")) return false;
    sql.execute("CREATE TABLE users (userid VARCHAR(16) PRIMARY KEY NOT NULL, passmd5 VARCHAR(32) NOT NULL)");
    sql.execute("INSERT INTO users (userid, passmd5) VALUES ('admin', '21232f297a57a5a743894a0e4a801fc3')");
    sql.uninit();
    return true;
  }

  public boolean init(String extra) {
    try {
      conn = DriverManager.getConnection("jdbc:derby:jpbxDB;" + extra);
      if ( conn == null ) {
        JFLog.log("Error : SQL Connection failed!");
        return false;
      }
    } catch (Exception ex2) {
      JFLog.log("Error : SQL Connection failed!");
      JFLog.log(ex2);
      return false;
    }
    return true;
  }

  public boolean init() {
    return init("");
  }

  public void uninit() {
    if (conn != null) {
      try { conn.close(); } catch (Exception e) { }
      conn = null;
    }
  }

  public void finalize() throws Throwable {
    super.finalize();
    //NOTE : sql Connections must be closed (I think they are kept in some list somewhere which prevents them from being freed by gc())
    uninit();
  }

  public static String quote(String str) {
    char strca[] = str.toCharArray();
    char strca2[] = new char[strca.length+2];
    for(int a=0;a<strca.length;a++) {
      switch (strca[a]) {
        case '"':
        case '\'':
          strca2[a+1] = ' '; break;
        default:
          strca2[a+1] = strca[a]; break;
      }
    }
    strca2[0] = '\'';
    strca2[strca.length+1] = '\'';
    return new String(strca2);
  }

  public boolean execute(String str) {
    boolean ret = true;
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    try {
      stmt = conn.createStatement();
      stmt.execute(str);  //ignore error
      stmt.close();
    } catch (Exception e) {
      return false;
    }
    return ret;
  }

  public String select1value(String str) {
    String ret;
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    try {
      stmt = conn.createStatement();
      if (!stmt.execute(str)) throw new Exception();
      rs = stmt.getResultSet();
      rsmd = rs.getMetaData();
      int colcnt = rsmd.getColumnCount();
      if (!rs.next()) {
        stmt.close();
        return null;
      }
      ret = rs.getString(1);
      stmt.close();
    } catch (Exception e) {
      return null;
    }
    return ret;
  }

  public String[] select1row(String str) {
    String ret[];
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    try {
      stmt = conn.createStatement();
      if (!stmt.execute(str)) throw new Exception();
      rs = stmt.getResultSet();
      rsmd = rs.getMetaData();
      int colcnt = rsmd.getColumnCount();
      ret = new String[colcnt];
      if (!rs.next()) return null;
      for(int a=0;a<colcnt;a++) {
        ret[a] = rs.getString(a+1);
      }
      stmt.close();
    } catch (Exception e) {
      return null;
    }
    return ret;
  }

  public String[] select1col(String str) {
    ArrayList<String> rows = new ArrayList<String>();
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    int colcnt;
    try {
      stmt = conn.createStatement();
      if (!stmt.execute(str)) throw new Exception();
      rs = stmt.getResultSet();
      rsmd = rs.getMetaData();
      colcnt = rsmd.getColumnCount();
      while (rs.next()) {
        rows.add(rs.getString(1));
      }
      stmt.close();
    } catch (Exception e) {
      return null;
    }
    String[] ret = new String[rows.size()];
    for(int r=0;r<rows.size();r++) {
      ret[r] = rows.get(r);
    }
    return ret;
  }

  public String[][] select(String str) {
    ArrayList<String[]> rows = new ArrayList<String[]>();
    String[] row;
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    int colcnt;
    try {
      stmt = conn.createStatement();
      if (!stmt.execute(str)) throw new Exception();
      rs = stmt.getResultSet();
      rsmd = rs.getMetaData();
      colcnt = rsmd.getColumnCount();
      while (rs.next()) {
        row = new String[colcnt];
        rows.add(row);
        for(int a=0;a<colcnt;a++) {
          row[a] = rs.getString(a+1);
        }
      }
      stmt.close();
    } catch (Exception e) {
      return null;
    }
    String[][] ret = new String[rows.size()][0];
    for(int r=0;r<rows.size();r++) {
      ret[r] = rows.get(r);
    }
    return ret;
  }

  public String getLastException() {
    if (lastException == null) return "No Exception";
    return lastException.toString();
  }
}

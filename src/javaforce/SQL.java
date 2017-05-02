package javaforce;

/** JDBC SQL helper. */

import java.util.*;
import java.sql.*;

public class SQL {
  /** Last exception that occured. */
  public Exception lastException;
  /** Column names from last query. */
  public String colNames[];

  private java.sql.Connection conn;

  /** Apache Derby SQL JDBC Class */
  public static String derbySQL = "org.apache.derby.jdbc.EmbeddedDriver";
  /** Microsoft SQL JDBC Class */
  public static String msSQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  /** MySQL JDBC Class */
  public static String mySQL = "com.mysql.jdbc.Driver";
  /** Oracle JDBC Class */
  public static String oracleSQL = "oracle.jdbc.driver.OracleDriver";
  /** jTDS (Microsoft SQL compatible) (jtds.sourceforge.net) */
  public static String jTDS = "net.sourceforge.jtds.jdbc.Driver";

  /** Init JDBC driver (need only call once) */
  public static boolean initClass(String jdbcClass) {
    try {
      Class.forName(jdbcClass).newInstance();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /** Connects to SQL Server. */
  public boolean connect(String connectionURL) {
    try {
      conn = DriverManager.getConnection(connectionURL);
      if ( conn == null ) {
        return false;
      }
      return true;
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return false;
    }
  }

  /** Init JDBC driver and connects to SQL Server. */
  public boolean connect(String jdbcClass, String connectionURL) {
    if (!initClass(jdbcClass)) return false;
    return connect(connectionURL);
  }

  /** Closes connection. */
  public void close() {
    if (conn != null) {
      try { conn.close(); } catch (Exception e) { }
      conn = null;
    }
  }

  public void finalize() {
    close();
  }

  /** Returns str with quotes around it (and replaces any pre-existing quotes with spaces). */
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

  /** Executes a SQL query (no return data). */
  public boolean execute(String str) {
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    try {
      stmt = conn.createStatement();
      stmt.execute(str);  //ignore return value (Exception will be thrown in an error)
      stmt.close();
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return false;
    }
    return true;
  }

  /** Executes a SQL query with one return value. */
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
      colNames = new String[colcnt];
      for(int c=0;c<colcnt;c++) colNames[c] = rsmd.getColumnName(c+1);
      if (!rs.next()) {
        stmt.close();
        return null;
      }
      ret = rs.getString(1);
      stmt.close();
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return null;
    }
    return ret;
  }

  /** Executes a SQL query with one row of data. */
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
      colNames = new String[colcnt];
      for(int c=0;c<colcnt;c++) colNames[c] = rsmd.getColumnName(c+1);
      ret = new String[colcnt];
      if (!rs.next()) {
        stmt.close();
        return null;
      }
      for(int a=0;a<colcnt;a++) {
        ret[a] = rs.getString(a+1);
      }
      stmt.close();
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return null;
    }
    return ret;
  }

  /** Executes a SQL query with one column of data. */
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
      colNames = new String[colcnt];
      for(int c=0;c<colcnt;c++) colNames[c] = rsmd.getColumnName(c+1);
      while (rs.next()) {
        rows.add(rs.getString(1));
      }
      stmt.close();
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return null;
    }
    String[] ret = new String[rows.size()];
    for(int r=0;r<rows.size();r++) {
      ret[r] = rows.get(r);
    }
    return ret;
  }

  /** Executes a SQL query and returns a table of data. */
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
      colNames = new String[colcnt];
      for(int c=0;c<colcnt;c++) colNames[c] = rsmd.getColumnName(c+1);
      while (rs.next()) {
        row = new String[colcnt];
        rows.add(row);
        for(int c=0;c<colcnt;c++) {
          row[c] = rs.getString(c+1);
        }
      }
      stmt.close();
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return null;
    }
    String[][] ret = new String[rows.size()][];
    for(int r=0;r<rows.size();r++) {
      ret[r] = rows.get(r);
    }
    return ret;
  }

  public Exception getLastException() {
    return lastException;
  }
}

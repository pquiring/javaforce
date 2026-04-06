package javaforce;

/** JDBC SQL helper. */

import java.util.*;
import java.sql.*;
import java.lang.reflect.*;

public class SQL {
  /** Last exception that occured. */
  public Exception lastException;
  /** Column names from last query. */
  public String[] colNames;

  /** Outputs all statements to stdout (default = false). */
  public static boolean debug = false;

  private java.sql.Connection conn;

  /** Apache Derby SQL JDBC Class */
  public static String derbySQL = "org.apache.derby.jdbc.EmbeddedDriver";
  /** Microsoft SQL JDBC Class */
  public static String msSQL = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
  /** MySQL JDBC Class */
  public static String mySQL = "com.mysql.jdbc.Driver";
  /** PostgreSQL JDBC Class */
  public static String postgreSQL = "org.postgresql.Driver";
  /** Oracle JDBC Class */
  public static String oracleSQL = "oracle.jdbc.driver.OracleDriver";
  /** jTDS (Microsoft SQL compatible) (jtds.sourceforge.net) */
  public static String jTDS = "net.sourceforge.jtds.jdbc.Driver";
  /** IBM UNIDATA */
  public static String UniData = "com.ibm.u2.jdbc.UniJDBCDriver";

  /** Init JDBC driver (need only call once) */
  public static boolean initClass(String jdbcClass) {
    try {
      Class<?> cls = Class.forName(jdbcClass);
      Constructor ctor = cls.getConstructor();
      ctor.newInstance();
      return true;
    } catch (Exception e) {
      JFLog.log("Error:Unable to find class:" + jdbcClass);
      return false;
    }
  }

  /** Connects to SQL Server. */
  public boolean connect(String connectionURL) {
    try {
      conn = DriverManager.getConnection(connectionURL);
      if ( conn == null ) {
        JFLog.log("Error:Unable to connect to database:" + connectionURL);
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

  /** Returns str with quotes around it (and replaces any pre-existing quotes with spaces). */
  public static String quote(String str) {
    char[] strca = str.toCharArray();
    char[] strca2 = new char[strca.length+2];
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

  /** Returns only numbers in string. */
  public static String numbers(String str) {
    return JF.filter(str, JF.filter_numeric);
  }

  /** Returns only numbers in string with single sign (- or +). */
  public static String signed_numbers(String str) {
    String sign = "";
    if (str.startsWith("-") || str.startsWith("+")) {
      sign = str.substring(0, 1);
      str = str.substring(1);
    }
    return sign + numbers(str);
  }

  /** Returns only letters in quotes in string. */
  public static String letters(String str) {
    return "\'" + JF.filter(str, JF.filter_alpha)+ "\'";
  }

  /** Returns only letters or numbers in quotes in string. */
  public static String letters_numbers(String str) {
    return "\'" + JF.filter(str, JF.filter_alpha_numeric)+ "\'";
  }

  /** Executes a SQL query (no return data). */
  public boolean execute(String str) {
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    if (debug) JFLog.log(str);
    try {
      stmt = conn.createStatement();
      stmt.execute(str);  //ignore return value (Exception will be thrown in an error)
      stmt.close();
    } catch (Exception e) {
      lastException = e;
      JFLog.log(str);
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
    if (debug) JFLog.log(str);
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
      JFLog.log(str);
      JFLog.log(e);
      return null;
    }
    return ret;
  }

  /** Executes a SQL query with one row of data. */
  public String[] select1row(String str) {
    String[] ret;
    java.sql.Statement stmt = null;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    if (debug) JFLog.log(str);
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
      JFLog.log(str);
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
    if (debug) JFLog.log(str);
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
      JFLog.log(str);
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
    if (debug) JFLog.log(str);
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
      JFLog.log(str);
      JFLog.log(e);
      return null;
    }
    String[][] ret = new String[rows.size()][];
    for(int r=0;r<rows.size();r++) {
      ret[r] = rows.get(r);
    }
    return ret;
  }

  public int getColumnIndex(String col) {
    if (colNames == null) return -1;
    for(int idx=0;idx<colNames.length;idx++) {
      if (colNames.equals(col)) return idx;
    }
    return -1;
  }

  public String[] getTables() {
    ArrayList<String> rows = new ArrayList<String>();
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    String[] types = {"TABLE"};
    int colcnt;
    try {
      rs = conn.getMetaData().getTables(null, null, "%", types);
      rsmd = rs.getMetaData();
      colcnt = rsmd.getColumnCount();
      colNames = new String[colcnt];
      for(int c=0;c<colcnt;c++) colNames[c] = rsmd.getColumnName(c+1);
      while (rs.next()) {
        rows.add(rs.getString("TABLE_NAME"));
      }
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

  /** Returns design of table
   * @see colNames for details
   */
  public String[][] getTableDesign(String table) {
    ArrayList<String[]> rows = new ArrayList<String[]>();
    String[] row;
    java.sql.ResultSet rs = null;
    java.sql.ResultSetMetaData rsmd = null;
    int colcnt;
    try {
      DatabaseMetaData metaData = conn.getMetaData();
      rs = metaData.getColumns(null, null, table, null);
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

  /** SQL CLI. */
  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println("Usage : SQL {sql-type} {connection-string} {command} [args]");
      System.out.println("  commands:");
      System.out.println("    select {select-string}");
      System.out.println("    execute {execute-string}");
      System.out.println("    gettables");
      return;
    }
    String db_type = args[0];
    String sql_class = null;
    String conn_str = args[1];
    String cmd = args[2];
    switch (db_type) {
      case "derbysql": sql_class = derbySQL; break;
      case "mysql": sql_class = mySQL; break;
      case "mssql": sql_class = msSQL; break;
      case "postgresql": sql_class = postgreSQL; break;
    }
    if (sql_class == null) {
      System.out.println("db_type unknown:" + db_type);
      return;
    }
    SQL sql = new SQL();
    if (!sql.connect(sql_class, conn_str)) {
      System.out.println("connection failed!");
      return;
    }
    switch (cmd) {
      case "select": {
        if (args.length < 4) {
          System.out.println("Usage : SQL {sql-type} {connection-string} select {query-string}");
          return;
        }
        String[][] data = sql.select(args[3]);
        for(int row=0;row<data.length;row++) {
          boolean first = true;
          for(int col=0;col<data[row].length;col++) {
            if (first)
              first = false;
            else
              System.out.print(",");
            System.out.print(data[row][col]);
          }
          System.out.println("");
        }
        break;
      }
      case "execute": {
        if (args.length < 4) {
          System.out.println("Usage : SQL {sql-type} {connection-string} execute {execute-string}");
          return;
        }
        sql.execute(args[3]);
        break;
      }
      case "gettables": {
        String[] tables = sql.getTables();
        for(String table : tables) {
          System.out.println(table);
        }
        break;
      }
      case "gettabledesign": {
        if (args.length < 4) {
          System.out.println("Usage : SQL {sql-type} {connection-string} gettabledesign {table}");
          return;
        }
        System.out.println("Column Names:");
        String[][] data = sql.getTableDesign(args[3]);
        {
          boolean first = true;
          for(String col : sql.colNames) {
            if (first) first = false; else System.out.print(",");
            System.out.print(col);
          }
        }
        System.out.println("\nTable Columns:");
        for(int row=0;row<data.length;row++) {
          boolean first = true;
          for(int col=0;col<data[row].length;col++) {
            if (first) first = false; else System.out.print(",");
            System.out.print(data[row][col]);
          }
          System.out.println("");
        }
        break;
      }
      default: {
        System.out.println("unknown cmd:" + cmd);
        break;
      }
    }
  }
}

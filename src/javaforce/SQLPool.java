package javaforce;

/** SQLPool based on Tomcat Connection Pool.
 *
 * NOTE : If using in standalone project you must include tomcat jar files.
 *   These jar files are renamed during the download:
 *     tomcat-jdbc-X,Y.Z.jar -> jdbc-api.jar
 *     tomcat-juli-X.Y.Z.jar -> juli-api.jar
 *
 * @author pquiring
 */

import java.sql.*;

import org.apache.tomcat.jdbc.pool.*;

public class SQLPool {
  private DataSource dataSource;

  /** Init SQL connection pool.
   *
   * This should be done in your servlets init() method.
   */
  public boolean init(String jdbcClass, String connectionURL) {
    // 1. Configure Pool Properties
    PoolProperties p = new PoolProperties();
    p.setUrl(connectionURL);
    p.setDriverClassName(jdbcClass);
//    p.setUsername("myuser");
//    p.setPassword("mypassword");

    // Optional: Pool tuning
    p.setMaxActive(20);
    p.setMaxIdle(10);
    p.setMinIdle(5);
    p.setInitialSize(5);
    p.setValidationQuery("SELECT 1");
    p.setTestOnBorrow(true);

    // 2. Create the DataSource
    dataSource = new DataSource();
    dataSource.setPoolProperties(p);

    return true;
  }

  /** Allocate an raw java SQL connection. */
  public Connection getConnection() throws SQLException {
    if (dataSource == null) return null;
    return dataSource.getConnection();
  }

  /** Allocate an SQL connection in a javaforce.SQL class. */
  public SQL getSQL() throws SQLException {
    Connection conn = getConnection();
    if (conn == null) return null;
    return new SQL(conn);
  }

  /** Close the connection pool.
   *
   * This should be done in your servlets destroy() method or leaks may occur.
   */
  public void close() {
    if (dataSource != null) {
      dataSource.close();
      dataSource = null;
    }
  }
}

package javaforce;

/** SQLPool based on Tomcat Connection Pool.
 *
 * @author pquiring
 */

import java.sql.*;

import org.apache.tomcat.jdbc.pool.*;

public class SQLPool {
  private DataSource dataSource;

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

  public Connection getConnection() throws SQLException {
    if (dataSource == null) return null;
    return dataSource.getConnection();
  }

  public SQL getSQL() throws SQLException {
    return new SQL(getConnection());
  }
}

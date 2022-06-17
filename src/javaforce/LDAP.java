package javaforce;

/** LDAP
 *
 * @author pquiring
 */

import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

public class LDAP {
  private DirContext ctx;

  public Exception lastException;

  /*

  LDAP Data Interchange Format
  see https://en.wikipedia.org/wiki/LDAP_Data_Interchange_Format

  dn = distinguished name (comma list of other attributes)
  dc = domain component
  ou = organizational unit
  cn = common name
  givenName = first name
  sn = surname
  mail = email address
  telephoneNumber = full phone number
  manager : manager in dn format

  */


  /** Logins into a LDAP Server.  Returns DirContext if successful (null otherwise). */
  public boolean login(String server, String domain, String username, String password) {
    try {
      if (username.length() == 0) throw new Exception("invalid username");
      if (password.length() == 0) throw new Exception("invalid password");
      // Set up the environment for creating the initial context
      Hashtable<String, String> env = new Hashtable<String, String>();
      env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      env.put(Context.PROVIDER_URL, "ldap://" + server + ":389");

      // Authenticate as user and password
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
      env.put(Context.SECURITY_PRINCIPAL, domain + "\\" + username);
      env.put(Context.SECURITY_CREDENTIALS, password);

      // Create the initial context
      ctx = new InitialDirContext(env);
      return true;
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return false;
    }
  }

  public void close() {
    if (ctx != null) {
      try {ctx.close();} catch (Exception e) {}
      ctx = null;
    }
  }

  /** Returns user object attributes.
   * @param domain = domain as dn (distinguished name) (ie: DC=example,DC=com)
   * @param username = directory user account name
   * @param attrs = list of attributes to read
   * @return attributes in order of attrs
   */
  public String[] getAttributes(String domain, String username, String[] attrs) {
    if (ctx == null) return null;
    String[] values = new String[attrs.length];
    try {
      SearchControls constraints = new SearchControls();
      constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);  //resursive
      constraints.setReturningAttributes(attrs);
      NamingEnumeration answer = ctx.search(domain, "SAMAccountName=" + username, constraints);
      if (!answer.hasMore()) {
        throw new Exception("no results");
      }
      Attributes results = ((SearchResult) answer.next()).getAttributes();
      for(int a=0;a<attrs.length;a++) {
        Object value = results.get(attrs[a]);
        if (value == null) value = "null";
        values[a] = value.toString();
      }
      return values;
    } catch (Exception e) {
      lastException = e;
      JFLog.log(e);
      return null;
    }
  }

  public static String build_dn(String domain) {
    StringBuilder dn = new StringBuilder();
    String[] p = domain.split("[.]");
    for(int a=0;a<p.length;a++) {
      if (a > 0) dn.append(",");
      dn.append("dc=");
      dn.append(p[a]);
    }
    return dn.toString();
  }

  public Exception getLastException() {
    return lastException;
  }
}

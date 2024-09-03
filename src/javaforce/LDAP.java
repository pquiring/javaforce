package javaforce;

/** LDAP
 *
 * @author pquiring
 */

import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

public class LDAP {
  private static boolean debug = false;

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


  /** Logins into a LDAP Server.
   *
   * @param server = LDAP server (FQN)
   * @param domain = domain (ie: example)
   * @param username = username
   * @param password = password
   * @return login successful
   */
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
      System.out.println(e);
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
   * Must login() first
   *
   * @param domain_dn = domain as dn (distinguished name) (ie: DC=example,DC=com)
   * @param key = search key (ie: SAMAccountName=bob)
   * @param attrs = list of attributes to read (ie: member)
   * @return attributes in order of attrs
   */
  public String[] getAttributes(String domain_dn, String key, String[] attrs) {
    if (ctx == null) {
      JFLog.log("Please login first!");
      return null;
    }
    String[] values = new String[attrs.length];
    try {
      SearchControls constraints = new SearchControls();
      constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);  //resursive
      constraints.setReturningAttributes(attrs);
      NamingEnumeration answer = ctx.search(domain_dn, key, constraints);
      if (!answer.hasMore()) {
        throw new Exception("no results");
      }
      Attributes results = ((SearchResult) answer.next()).getAttributes();
      if (debug) JFLog.log("results=" + results);
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

  /** Queries user to determine if they are a member of a group.
   * Must login() first.
   *
   * @param domain_dn = domain as dn (distinguished name) (ie: DC=example,DC=com)
   * @param username = user to check for (for computer use COMPUTERNAME plus $)
   * @param group = group name
   * @return user is a member
   */
  public boolean memberOf(String domain_dn, String username, String group) {
    String[] list = getAttributes(domain_dn, "SAMAccountName=" + username, new String[] {"memberOf"});
    if (list == null || list.length == 0) return false;
    String[] groups = list[0].replaceAll("\\\\,", "_").split("[,] ");
    for(String grp : groups) {
      int i1 = grp.indexOf('=');
      int i2 = grp.indexOf(',');
      if (i1 == -1 || i2 == -1) continue;
      grp = grp.substring(i1 + 1, i2);
      if (grp.equalsIgnoreCase(group)) return true;
    }
    return false;
  }

  /** Create distinguished name from dot domain name. */
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

  private static void usage() {
    System.out.println("Usage:ldap {command} [args]");
    System.out.println(" login server domain username password");
    System.out.println(" attrs server domain username password dname key attrs...");
    System.out.println(" memberof server domain username password dname username group");
    System.exit(1);
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      usage();
    }
    LDAP ldap = new LDAP();
    try {
      switch (args[0]) {
        case "login": {
          boolean ok = ldap.login(args[1], args[2], args[3], args[4]);
          System.out.println("login = " + ok);
          break;
        }
        case "attrs": {
          boolean ok = ldap.login(args[1], args[2], args[3], args[4]);
          if (!ok) {
            System.out.println("login failed");
            return;
          }
          int attrCount = args.length - 7;
          String[] attrs = new String[attrCount];
          for(int a=0;a<attrCount;a++) {
            attrs[a] = args[a + 7];
          }
          String[] list = ldap.getAttributes(args[5], args[6], attrs);
          if (list == null || list.length == 0) {
            System.out.println("no attrs returned");
          } else {
            for(String attr : list) {
              System.out.println("attr[]=" + attr);
            }
          }
          break;
        }
        case "memberof": {
          boolean ok = ldap.login(args[1], args[2], args[3], args[4]);
          if (!ok) {
            System.out.println("login failed");
            return;
          }
          boolean isMember = ldap.memberOf(args[5], args[6], args[7]);
          System.out.println("memberOf = " + isMember);
          break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

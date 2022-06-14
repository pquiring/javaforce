package javaforce;

/** LDAP
 *
 * @author pquiring
 */

import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

public class LDAP {
  /** Logins into a LDAP Server.  Returns DirContext if successful (null otherwise). */
  public static DirContext login(String server, String domain, String username, String password) {
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
      DirContext ctx = new InitialDirContext(env);
      return ctx;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Returns user object attributes.
   * @param ctx = DirContext from login()
   * @param domain = domain in OU style (ie: DC=example,DC=com)
   * @param username = directory user account name
   * @param attrs = list of attributes to read
   * @return attributes in order of attrs
   */
  public static String[] getAttributes(DirContext ctx, String domain, String username, String attrs[]) {
    String[] values = new String[attrs.length];
    try {
      SearchControls constraints = new SearchControls();
      constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
      String[] attrIDs = attrs;
      constraints.setReturningAttributes(attrIDs);
      NamingEnumeration answer = ctx.search("DC=YourDomain,DC=com", "SAMAccountName=" + username, constraints);
      if (answer.hasMore()) {
        Attributes results = ((SearchResult) answer.next()).getAttributes();
        for(int a=0;a<attrs.length;a++) {
          values[a] = results.get(attrs[a]).toString();
        }
      } else {
        throw new Exception("Invalid User");
      }
      return values;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
}

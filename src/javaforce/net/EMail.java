package javaforce.net;

/** EMail.
 *
 * General Format:
 * HEADER: real name <user@domain>
 *
 * Includes user defined fields : pass, flags;
 *
 * @author pquiring
 */

import java.util.*;

public class EMail {
  public String user;  //from
  public String domain;
  public String name;  //real name (optional)

  public List<EMail> to = new ArrayList<>();

  public String file;  //message file

  public String pass;  //password (optional)
  public int flags;  //user defined flags (optional)

  /** Sets email fields.
   * General format:
   *   HEADER: (real name)<user@domain>
   * real name is optional.
   */
  public boolean set(String in) {
    int cln = in.indexOf(':');
    if (cln == -1) return false;
    int i1 = in.indexOf('<');
    if (i1 == -1) return false;
    int i2 = in.indexOf('>');
    if (i2 == -1) return false;

    String email = in.substring(i1 + 1, i2);
    int at = email.indexOf('@');
    if (at == -1) return false;

    name = in.substring(cln+1, i1).trim();
    user = email.substring(0, at);
    domain = email.substring(at+1);

    return true;
  }

  public void addTo(EMail email) {
    to.add(email);
  }

  public void reset() {
    user = null;
    domain = null;
    name = null;

    to.clear();

    pass = null;
  }

  public String getUser() {return user;}
  public String getDomain() {return domain;}
  public String getRealName() {return name;}

  public String getPassword() {return pass;}

  public void setUser(String user) {this.user = user;}
  public void setDomain(String domain) {this.domain = domain;}
  public void setRealName(String name) {this.name = name;}

  public void setPassword(String pass) {this.pass = pass;}

  public List<EMail> getToList() {
    return to;
  }
}

/*
 * SiteDetails.java
 *
 * Created on August 3, 2007, 2:21 PM
 *
 * @author pquiring
 */
public class SiteDetails {

  /** Creates a new instance of SiteDetails */
  public SiteDetails() {
    sx = sy = 0;
  }

  public String name;
  public String host;
  public String protocol;
  public String port;
  public String username;
  public String password;
  public String sshKey;  //filename to PEM file
  public int sx;  //0=global
  public int sy;  //0=global
  public boolean x11;  //enable X11 forwarding
  public boolean autoSize;
  public boolean localecho;
  public boolean utf8;
}

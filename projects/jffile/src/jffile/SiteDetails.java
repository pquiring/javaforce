package jffile;

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
    name = "";
    host = "";
    protocol = "";
    port = "";
    username = "";
    password = "";
    localDir = "";
    remoteDir = "";
  }

  public String name;
  public String host;
  public String protocol;  //ftp, ftps, sftp
  public String port;
  public String username;
  public String password;
  public String localDir;
  public String remoteDir;
}

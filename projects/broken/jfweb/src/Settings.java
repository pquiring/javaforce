/*
 * Settings.java
 *
 * Created on November 21, 2007, 6:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author pquiring
 */
public class Settings {

  static {
    proxy = false;
    proxyHost = "";
    proxyPort = "";
    homePage = "http://javaforce.sf.net";
  }  

  /** Creates a new instance of Settings */
  public Settings() {
  }
  
  public static boolean proxy;
  public static String proxyHost;
  public static String proxyPort;  
  public static String homePage;
}

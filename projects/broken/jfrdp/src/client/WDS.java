package client;

import javaforce.JF;

/** Windows Desktop Sharing Native API
 *
 * @author pquiring
 */

public class WDS {
  static {
    System.loadLibrary("client64");
  }
  public static native boolean startClient(String xml, String user, String pass);
}

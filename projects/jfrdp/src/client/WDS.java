package client;

import javaforce.JF;

/** Windows Desktop Sharing Native API
 *
 * @author pquiring
 */

public class WDS {
  static {
    if (JF.is64Bit())
      System.loadLibrary("client64");
    else
      System.loadLibrary("client32");
  }
  public static native boolean startClient(String xml, String user, String pass);
}

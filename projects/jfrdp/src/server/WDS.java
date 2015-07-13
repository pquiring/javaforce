package server;

/** Windows Desktop Sharing Native API
 *
 * @author pquiring
 */

import javaforce.JF;

public class WDS {
  static {
    if (JF.is64Bit())
      System.loadLibrary("server64");
    else
      System.loadLibrary("server32");
  }
  public static native long startServer(String user, String group, String pass, int numAttend, int port);
  public static native String getConnectionString(long id);
  public static native void runServer(long id);  //process queue (does not return until stopServer is called)
  public static native void stopServer(long id);
}

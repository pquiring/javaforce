package javaforce.jni;

import javaforce.api.*;
import javaforce.ffm.*;

/** WinPipe JNI
 *
 * @author pquiring
 */

public class WinPipeJNI implements WinPipeAPI {

  private static WinPipeAPI instance;

  public static WinPipeAPI getInstance() {
    if (instance == null) {
      JFNative.load();
      instance = new WinPipeJNI();
    }
    return instance;
  }

  public native long pipeCreate(String name, boolean first);
  public native void pipeClose(long ctx);
  public native int pipeRead(long ctx, byte[] buf, int offset, int length);
  public native int pipeWrite(String name, byte[] buf, int offset, int length);
}

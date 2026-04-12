package javaforce.jni;

/** WinPipe JNI
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.ffm.*;

public class WinPipeJNI implements WinPipeAPI {

  private static WinPipeAPI instance;

  public static WinPipeAPI getInstance() {
    if (instance == null) {
      instance = new WinPipeJNI();
    }
    return instance;
  }

  public native long pipeCreate(String name, boolean first);
  public native void pipeClose(long ctx);
  public native int pipeRead(long ctx, byte[] buf, int offset, int length);
  @NoCopyBack
  public native int pipeWrite(String name, byte[] buf, int offset, int length);
}

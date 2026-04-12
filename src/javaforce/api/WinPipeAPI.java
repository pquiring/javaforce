package javaforce.api;

/** Windows Pipes API
 *
 * @author pquiring
 */

import javaforce.ffm.*;
import javaforce.jni.*;

public interface WinPipeAPI {
  public static WinPipeAPI getInstance() {
    if (FFM.enabled()) {
      return WinPipeFFM.getInstance();
    } else {
      return WinPipeJNI.getInstance();
    }
  }

  public long pipeCreate(String name, boolean first);
  public void pipeClose(long ctx);
  public int pipeRead(long ctx, byte[] buf, int offset, int length);
  public int pipeWrite(String name, byte[] buf, int offset, int length);
}

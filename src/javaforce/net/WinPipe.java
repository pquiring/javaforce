package javaforce.net;

import javaforce.api.*;
import javaforce.ffm.*;

/** Windows Pipes (JNI or FFM)
 *
 * @author pquiring
 */

public class WinPipe {
  private WinPipeAPI api;

  public WinPipe() {
    api = WinPipeFFM.getInstance();
  }

  public long pipeCreate(String name, boolean first) {
    return api.pipeCreate(name, first);
  }
  public void pipeClose(long ctx) {
    api.pipeClose(ctx);
  }
  public int pipeRead(long ctx, byte[] buf, int offset, int length) {
    return api.pipeRead(ctx, buf, offset, length);
  }
  public int pipeWrite(String name, byte[] buf, int offset, int length) {
    return api.pipeWrite(name, buf, offset, length);
  }
}

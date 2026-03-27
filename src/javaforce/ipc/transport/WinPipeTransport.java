package javaforce.ipc.transport;

/** DBus over Windows Pipes Transport
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.ipc.*;
import javaforce.jni.*;

public class WinPipeTransport extends DBusTransport {
  private long ctx;
  private String name;

  private String makePipeName(String name) {
    //network path where "." = localhost
    return "\\\\.\\pipe\\" + name.replaceAll("[.]", "\\\\");
  }

  public boolean connect(String name, DBus bus, Runnable start_reader) {
    if (ctx != 0) return false;
    if (name == null) {
      //create client name
      Random r = new Random();
      name = String.format("javaforce.client.r%x.t%x", r.nextInt(0xfffffff), System.currentTimeMillis());
    }
    ctx = WinNative.pipeCreate(makePipeName(name), true);
    if (ctx != 0) {
      this.name = name;
      if (start_reader != null) {
        start_reader.run();
      }
    }
    return ctx != 0;
  }

  public boolean disconnect() {
    if (ctx == 0) return false;
    WinNative.pipeClose(ctx);
    ctx = 0;
    return true;
  }

  public String getBusName() {
    return name;
  }

  public int read(byte[] data) {
    int read = WinNative.pipeRead(ctx, data, 0, data.length);
    if (read > 0) {
      //client connected : need to recreate server pipe
      long new_ctx = WinNative.pipeCreate(makePipeName(name), false);
      WinNative.pipeClose(ctx);
      ctx = new_ctx;
    }
    return read;
  }

  public boolean write(String name, byte[] data, int offset, int length) {
    if (name == null) return false;
    return WinNative.pipeWrite(makePipeName(name), data, offset, length) == length;
  }

  public boolean isAlive() {
    return ctx != 0;
  }
}

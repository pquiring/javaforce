package javaforce.ipc.transport;

/** DBus Transport base class.
 *
 * @author pquiring
 */

import javaforce.ipc.*;

public abstract class DBusTransport {
  public abstract boolean connect(String name, DBus bus, Runnable start_reader);
  public abstract boolean disconnect();
  public abstract int read(byte[] data);
  public abstract boolean write(String name, byte[] data, int offset, int length);
  public abstract boolean isAlive();
  public abstract String getBusName();

  private byte[] rString = new byte[1024];
  public String read_String() {
    int read = read(rString);
    if (read <= 0) return null;
    return new String(rString, 0, read);
  }

  public boolean write_String(String msg) {
    int len = msg.length();
    return write(null, msg.getBytes(), 0, len);
  }
}

package javaforce.ipc.transport;

/** DBus over Unix Socket Transport
 *
 * @author pquiring
 */

import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.ipc.*;

public class UnixSocketTransport extends DBusTransport {

  private static boolean session_bus = false;

  private static final boolean debug = true;

  private static final String DBusMessageBus = "org.freedesktop.DBus";

  private SocketChannel client;
  private String busName;

  /** Enable the use of the session bus. Default = false (use system bus) */
  public static void useSessionBus(boolean state) {
    session_bus = state;
  }

  public boolean connect(String name, DBus bus, Runnable start_reader) {
    String path = null;
    if (session_bus) {
      //unix:path=/run/user/$UID/bus
      path = System.getenv("DBUS_SESSION_BUS_ADDRESS");
    } else {
      path = "unix:path=/var/run/dbus/system_bus_socket";
      /*
      NOTE : This requires a conf file in /etc/dbus-1/system.d to own a message bus.
             Otherwise RequestName() will be denied.
      */
    }
    if (path == null) {
      JFLog.log("UnixSocketTransport:DBus socket not found");
      return false;
    }
    int idx = path.indexOf("=");
    if (idx == -1) {
      JFLog.log("DBus socket invalid");
      return false;
    }
    path = path.substring(idx + 1);
    idx = path.indexOf(';');
    if (idx != -1) {
      path = path.substring(0, idx);  //remove trailing tags (if any)
    }
    if (debug) JFLog.log("DBus.path=" + path);
    String uid = path.split("/")[3];
    if (uid.equals("dbus")) {
      //using system dbus - must be root
      uid = Integer.toString(LnxNative.getuid());
    }
    if (debug) JFLog.log("UID=" + uid);
    StringBuilder sasl = new StringBuilder();
    sasl.append((char)0);  //null byte
    sasl.append("AUTH EXTERNAL ");
    byte[] uids = uid.getBytes();
    for(byte id : uids) {
      sasl.append(String.format("%02x", id));
    }
    sasl.append("\r\n");
    try {
      UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
      client = SocketChannel.open(StandardProtocolFamily.UNIX);
      client.connect(addr);
      //SASL auth
      write_String(sasl.toString());
      String reply = read_String();
      if (debug) JFLog.log("AUTH REPLY=" + reply);
      if (!reply.startsWith("OK") || !reply.endsWith("\r\n")) throw new Exception("auth failed");
      write_String("BEGIN\r\n");
      start_reader.run();
      busName = DBus_Hello(bus);
      if (debug) {
        JFLog.log("DBus:busName=" + busName);
      }
      if (name != null) {
        if (!DBus_RequestName(name, bus)) {
          throw new Exception("DBus.RequestName() failed");
        }
        busName = name;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      try {client.close();} catch (Exception e2) {}
      client = null;
      return false;
    }
  }

  public boolean disconnect() {
    if (client == null) return false;
    try {
      client.close();
      client = null;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public String getBusName() {
    return busName;
  }

  public int read(byte[] data) {
    try {
      int read = client.read(ByteBuffer.wrap(data));
      return read;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public boolean write(String name, byte[] data, int offset, int length) {
    try {
      int write = client.write(ByteBuffer.wrap(data, offset, length));
      return write == length;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean isAlive() {
    return client != null;
  }

  private String DBus_Hello(DBus bus) throws Exception {
    Object ret = bus.invoke(DBusMessageBus, "Hello", new Object[] {});
    if (ret == null) return null;
    if (ret.getClass() != String.class) return null;
    return (String)ret;
  }

  private boolean DBus_RequestName(String name, DBus bus) throws Exception {
    //invoke org.freedesktop.DBus.RequestName(String name, int flags);
    //flags = 0x04 (do not queue)
    Object ret = bus.invoke(DBusMessageBus, "RequestName", new Object[] {name, new UInteger(0x04)});
    if (ret == null) return false;
    if (debug) {
      JFLog.log("RequestName=" + ret);
    }
    return true;
  }
}

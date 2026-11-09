package javaforce.controls.mod;

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.controls.*;

/** Modbus PLC.
 *
 * @author pquiring
 */

public class ModbusPLC implements PLC {
  private Socket socket;
  private static Object connect_lock = new Object();
  private InputStream is;
  private OutputStream os;
  private boolean connected;

  public static boolean debug;
  public Exception lastException;

  public boolean connect(String host) {
    try {
      socket = PLCFactory.connect(host, 502);
      os = socket.getOutputStream();
      is = socket.getInputStream();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    connected = true;
    return true;
  }

  public boolean disconnect() {
    try {
      if (socket != null) {
        socket.close();
        socket = null;
      }
      connected = false;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean login(String user, String pass) {
    return false;
  }

  public boolean isConnected() {
    if (socket == null) return false;
    return socket.isConnected();
  }

  public boolean isBE() {
    return true;
  }

  public boolean keepAlive() {
    return false;
  }

  public byte[] readTag(String tag) {
    ModAddr ma = ModPacket.decodeAddress(tag);
    byte[] packet = ModPacket.makeReadPacket(ma);
    try {
      os.write(packet);
    } catch (Exception e) {
      lastException = e;
      return null;
    }
    byte[] reply = new byte[1500];
    int replySize = 0;
    try {
      do {
        int read = is.read(reply, replySize, 1500 - replySize);
        if (read == -1) throw new Exception("bad read");
        replySize += read;
      } while (!ModPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return null;
    }
    ModData data = ModPacket.decodePacket(Arrays.copyOf(reply, replySize));
    return data.data;
  }

  public byte[][] readTags(String[] tags) {
    return null;
  }

  public boolean writeTag(String tag, byte[] data, int tagType) {
    ModAddr ma = ModPacket.decodeAddress(tag);
    ma.data = data;
    byte[] packet = ModPacket.makeWritePacket(ma);
    try {
      os.write(packet);
    } catch (Exception e) {
      lastException = e;
      return false;
    }
    byte[] reply = new byte[1500];
    int replySize = 0;
    try {
      do {
        int read = is.read(reply, replySize, 1500 - replySize);
        if (read == -1) throw new Exception("bad read");
        replySize += read;
      } while (!ModPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return false;
    }
    return true;
  }

  public Calendar getClock() {
    return null;
  }

  public boolean setClock(Calendar clock) {
    return false;
  }

  public Exception getException() {
    return lastException;
  }
}

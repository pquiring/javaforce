package javaforce.controls.jfc;

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.controls.*;

/** JFC PLC.
 *
 * @author pquiring
 */

public class JFCPLC implements PLC {
  private Socket socket;
  private static Object connect_lock = new Object();
  private InputStream is;
  private OutputStream os;
  private boolean connected;

  public static boolean debug;
  public Exception lastException;

  public boolean connect(String host) {
    try {
      PLCFactory.connect(host, 33100);
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
    return false;
  }

  public boolean keepAlive() {
    return false;
  }

  public byte[] readTag(String tag) {
    JFTag jftag = JFPacket.decodeAddress(tag);
    if (jftag == null) return null;
    byte[] packet = JFPacket.makeReadPacket(jftag);
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
      } while (!JFPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return null;
    }
    jftag = JFPacket.decodePacket(Arrays.copyOf(reply, replySize));
    return jftag.data;
  }

  public byte[][] readTags(String[] tags) {
    JFTag[] jftags = new JFTag[tags.length];
    for(int a=0;a<tags.length;a++) {
      jftags[a] = JFPacket.decodeAddress(tags[a]);
    }
    byte[] packet = JFPacket.makeReadPacket(jftags);
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
      } while (!JFPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return null;
    }
    jftags = JFPacket.decodeMultiPacket(Arrays.copyOf(reply, replySize), tags.length);
    byte[][] ret = new byte[tags.length][];
    for(int a=0;a<tags.length;a++) {
      ret[a] = jftags[a].data;
    }
    return ret;
  }

  public boolean writeTag(String tag, byte[] data, int tagType) {
    JFTag jftag = JFPacket.decodeAddress(tag);
    jftag.data = data;
    byte[] packet = JFPacket.makeWritePacket(jftag, data);
    try {
      os.write(packet);
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

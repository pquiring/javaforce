package javaforce.controls.ab;

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.controls.*;

/** Allen Bradley PLC.
 *
 * @author pquiring
 */

public class ABPLC implements PLC {
  private Socket socket;
  private static Object connect_lock = new Object();
  private InputStream is;
  private OutputStream os;
  private boolean connected;
  private ENIPContext ab_context = new ENIPContext();

  public static boolean debug;
  public Exception lastException;

  public boolean connect(String host) {
      ab_context = new ENIPContext();
      try {
        socket = PLCFactory.connect(host, 44818);
        os = socket.getOutputStream();
        is = socket.getInputStream();

        //connect1
        {
          byte[] packet = ABPacket.makeConnectPacket(ab_context);
          os.write(packet);

          byte[] reply = new byte[1500];
          int replySize = 0;
          do {
            int read = is.read(reply, replySize, 1500 - replySize);
            if (read == -1) throw new Exception("bad read");
            replySize += read;
          } while (!ABPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
          ENIP ip = new ENIP();
          ip.read(new Packet(reply, Endian.L));
          ab_context.session = ip.session;
        }

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
    byte[] packet = ABPacket.makeReadPacket(tag, ab_context);
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
      } while (!ABPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
      return ABPacket.decodePacket(reply);
    } catch (Exception e) {
      lastException = e;
      return null;
    }
  }

  public byte[][] readTags(String[] tags) {
    return null;
  }

  public boolean writeTag(String tag, byte[] data, int tagType) {
    if (tagType == TagType.any) return false;
    byte[] packet = ABPacket.makeWritePacket(tag, ABPacket.getType(tagType), data, ab_context);
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
      } while (!ABPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return false;
    }
    return true;
  }

  public Calendar getClock() {
    byte[] packet = ABPacket.makeReadClockPacket(ab_context);
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
      } while (!ABPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
      byte[] clock = ABPacket.decodePacket(reply);
      Calendar dt = Calendar.getInstance();
      dt.setTimeInMillis(LE.getuint64(clock, 0) / 1000L);
      return dt;
    } catch (Exception e) {
      JFLog.log(e);
      lastException = e;
      return null;
    }
  }

  public boolean setClock(Calendar clock) {
    byte[] packet = ABPacket.makeWriteClockPacket(clock, ab_context);
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
      } while (!ABPacket.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return false;
    }
    return true;
  }

  public Exception getException() {
    return lastException;
  }
}

package javaforce.controls.s7;

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.controls.*;

/** Siemens S7 PLC.
 *
 * @author pquiring
 */

public class S7PLC implements PLC {
  private Socket socket;
  private static Object connect_lock = new Object();
  private InputStream is;
  private OutputStream os;
  private boolean connected;

  public static boolean debug;
  public Exception lastException;

  public boolean connect(String host) {
    synchronized(connect_lock) {
      try {
        socket = PLCFactory.connect(host, 102);
        os = socket.getOutputStream();
        is = socket.getInputStream();

        //connect1
        {
          byte[] packet = S7Packet.makeConnectPacket1();
          os.write(packet);

          byte[] reply = new byte[1500];
          int replySize = 0;
          do {
            int read = is.read(reply, replySize, 1500 - replySize);
            if (read == -1) throw new Exception("bad read");
            replySize += read;
          } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
        }

        //connect2
        {
          byte[] packet = S7Packet.makeConnectPacket2();
          os.write(packet);

          byte[] reply = new byte[1500];
          int replySize = 0;
          do {
            int read = is.read(reply, replySize, 1500 - replySize);
            if (read == -1) throw new Exception("bad read");
            replySize += read;
          } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
        }

      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
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

  private byte[] readPartial(S7Data s7) {
    try {
      byte[] packet = S7Packet.makeReadPacket(s7);
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
      } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return null;
    }
    s7 = S7Packet.decodePacket(Arrays.copyOf(reply, replySize));
    if (s7 == null) {
      if (debug) JFLog.log("S7Packet.decodePacket returned null");
      return null;
    }
    return s7.data;
  }

  public byte[] readTag(String tag) {
    S7Data s7 = S7Packet.decodeAddress(tag);
    if (s7 == null) return null;
    byte[] data = new byte[s7.getLength()];
    int offset = 0;
    int read = 0;
    int left = data.length;
    while (read < data.length) {
      if (left > 200) {
        s7.length = (short)(200 / S7Types.getTypeSize(s7.data_type, (short)1));
      } else {
        s7.length = (short)(left / S7Types.getTypeSize(s7.data_type, (short)1));
      }
      byte[] part = readPartial(s7);
      if (part == null) {
        return null;
      }
      int part_length = part.length;
      if (part_length > left) {
        part_length = left;
      }
      System.arraycopy(part, 0, data, offset, part_length);
      left -= part_length;
      read += part_length;
      s7.offset += part_length << 3;
      offset += part_length;
    }
    return data;
  }

  public byte[][] readTags(String[] tags) {
    S7Data[] s7 = new S7Data[tags.length];
    for(int a=0;a<tags.length;a++) {
      s7[a] = S7Packet.decodeAddress(tags[a]);
    }
    try {
      byte[] packet = S7Packet.makeReadPacket(s7);
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
      } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return null;
    }
    s7 = S7Packet.decodeMultiPacket(Arrays.copyOf(reply, replySize), tags.length);
    byte[][] ret = new byte[tags.length][];
    for(int a=0;a<tags.length;a++) {
      ret[a] = s7[a].data;
    }
    return ret;
  }

  private boolean writePartial(S7Data s7) {
    try {
      byte[] packet = S7Packet.makeWritePacket(s7);
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
      } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
    } catch (Exception e) {
      lastException = e;
      return false;
    }
    return true;
  }

  public boolean writeTag(String tag, byte[] data, int tagType) {
    S7Data s7 = S7Packet.decodeAddress(tag);
    if (s7.data_type == S7Types.BIT) {
      s7.data = data;
      if (!writePartial(s7)) {
        return false;
      }
    } else {
      int left = s7.getLength();
      int dst_offset = s7.offset >> 3;
      int src_offset = 0;
      int copying = 0;
      while (left > 0) {
        if (left > 200) {
          copying = 200;
          s7.data = Arrays.copyOfRange(data, src_offset, src_offset + 200);
          s7.length = (short)(200 / S7Types.getTypeSize(s7.data_type, (short)1));
          s7.offset = dst_offset << 3;
          if (!writePartial(s7)) {
            return false;
          }
        } else {
          copying = left;
          s7.data = Arrays.copyOfRange(data, src_offset, src_offset + left);
          s7.length = (short)(left / S7Types.getTypeSize(s7.data_type, (short)1));
          s7.offset = dst_offset << 3;
          if (!writePartial(s7)) {
            return false;
          }
        }
        left -= copying;
        dst_offset += copying;
        src_offset += copying;
      }
    }
    return true;
  }

  public Calendar getClock() {
    try {
      byte[] packet = S7Packet.makeReadTimePacket();
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
      } while (!S7Packet.isPacketTimeComplete(Arrays.copyOf(reply, replySize)));
      Calendar dt = S7Packet.decodeTimePacket(Arrays.copyOf(reply, replySize));
      return dt;
    } catch (Exception e) {
      JFLog.log(e);
      lastException = e;
      return null;
    }
  }

  public boolean setClock(Calendar clock) {
    try {
      byte[] packet = S7Packet.makeWriteTimePacket(clock);
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
      } while (!S7Packet.isPacketTimeComplete(Arrays.copyOf(reply, replySize)));
      return true;
    } catch (Exception e) {
      lastException = e;
      return false;
    }
  }

  public Exception getException() {
    return lastException;
  }
}

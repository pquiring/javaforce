/**
 * Controller
 *
 * Connects to PLCs and read/write data.
 *
 * @author pquiring
 */

package javaforce.controls;

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.controls.s7.*;
import javaforce.controls.mod.*;
import javaforce.controls.ab.*;
import javaforce.controls.ni.*;
import javaforce.controls.jfc.*;
import javaforce.media.*;

public class Controller {
  private boolean connected;
  private Socket socket;
  private InputStream is;
  private OutputStream os;
  private int plc;
  private DAQmx daq;
  private Object lock = new Object();  //read/write lock
  private static Object s7_connect_lock = new Object();
  private AudioInput mic;
  private int micBufferSize;
  private short[] micBuffer;

  private ABContext ab_context;

  public static double rate;  //sample rate for all controllers (set before connecting to any controllers)

  public Exception lastException;

  public static boolean debug;

  public void setRate(float rate) {
    this.rate = rate;
  }

  /** Connects to a PLC:
   *
   * url = "S7:host"
   * url = "MODBUS:host"
   * url = "AB:host"
   * url = "NI:device/options"
   * url = "MIC:name"  (int16 data type , <default> will use default mic)
   *
   */
  public boolean connect(String url) {
    if (debug) JFLog.log("Controller.connect():" + url);
    connected = false;
    if (url == null) {
      JFLog.log("Controller:connect():url == null");
      return false;
    }
    if (url.startsWith("S7:")) {
      plc = ControllerType.S7;
      String host = url.substring(3);
      synchronized(s7_connect_lock) {
        try {
          connect(host, 102);
          socket.setSoTimeout(3000);
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
    if (url.startsWith("MODBUS:")) {
      plc = ControllerType.MB;
      String host = url.substring(7);
      try {
        connect(host, 502);
        socket.setSoTimeout(3000);
        os = socket.getOutputStream();
        is = socket.getInputStream();
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      connected = true;
      return true;
    }
    if (url.startsWith("AB:")) {
      ab_context = new ABContext();
      plc = ControllerType.AB;
      String host = url.substring(3);
      try {
        connect(host, 44818);
        socket.setSoTimeout(3000);
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
          ip.read(reply, 0);
          ab_context.session = ip.session;
        }

      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      connected = true;
      return true;
    }
    if (url.startsWith("NI:")) {
      plc = ControllerType.NI;
      daq = new DAQmx();
      connected = daq.connect(url.substring(3));
      if (!connected) {
        daq.close();
        daq = null;
      }
      return connected;
    }
    if (url.startsWith("MIC:")) {
      plc = ControllerType.MIC;
      mic = new AudioInput();
      micBufferSize = (int)(44100.0 / (1000.0 / rate));
      micBuffer = new short[micBufferSize];
      connected = mic.start(1, 44100, 16, micBufferSize, url.substring(4));
      return connected;
    }
    return false;
  }

  private String socks;

  private void connect(String host, int port) throws Exception {
    if (socks != null) {
      socket = new Socket(socks, 1080);
      if (!SOCKS.connect(socket, host, port)) {
        throw new Exception("SOCKS connection failed");
      }
    } else {
      socket = new Socket(host, port);
    }
  }

  /** Connect to PLC via a SOCKS4 server.
   * Must call this before calling connect(String url)
   *
   * @param host = IP4 address of SOCKS4 server.
   */
  public void setSOCKS(String host) {
    socks = host;
  }

  /** Disconnects from PLC. */
  public boolean disconnect() {
    if (!connected) return false;
    switch (plc) {
      case ControllerType.S7:
      case ControllerType.MB:
      case ControllerType.AB:
      case ControllerType.JF:
        try {
          if (socket != null) {
            socket.close();
            socket = null;
          }
        } catch (Exception e) {
          e.printStackTrace();
          return false;
        }
        break;
      case ControllerType.NI:
        if (daq != null) {
          daq.close();
          daq = null;
        }
        break;
      case ControllerType.MIC:
        mic.stop();
        mic = null;
        break;
    }
    connected = false;
    return true;
  }

  /** Data types for write() function.  Only AB protocol requires these. */
  public enum datatype {
    ANY, INTEGER16, INTEGER32, FLOAT, BOOLEAN
  }

  private boolean writePartial(S7Data s7) {
    byte packet[] = S7Packet.makeWritePacket(s7);
    try {
      os.write(packet);
    } catch (Exception e) {
      lastException = e;
      return false;
    }
    byte reply[] = new byte[1500];
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

  /** Writes data to PLC. */
  public boolean write(String addr, byte[] data) {
    return write(addr, data, datatype.ANY);
  }

  /** Writes data to PLC.
   *
   * datatype is required for AB controllers.
   */
  public boolean write(String addr, byte data[], datatype type) {
    addr = addr.toUpperCase();
    synchronized(lock) {
      if (!connected) return false;
      switch (plc) {
        case ControllerType.S7: {
          S7Data s7 = S7Packet.decodeAddress(addr);
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
        case ControllerType.MB: {
          ModAddr ma = ModPacket.decodeAddress(addr);
          ma.data = data;
          byte packet[] = ModPacket.makeWritePacket(ma);
          try {
            os.write(packet);
          } catch (Exception e) {
            lastException = e;
            return false;
          }
          byte reply[] = new byte[1500];
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
        case ControllerType.AB: {
          if (type == datatype.ANY) return false;
          byte packet[] = ABPacket.makeWritePacket(addr, ABPacket.getType(type), data, ab_context);
          try {
            os.write(packet);
          } catch (Exception e) {
            lastException = e;
            return false;
          }
          byte reply[] = new byte[1500];
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
        case ControllerType.JF: {
          JFTag tag = JFPacket.decodeAddress(addr);
          tag.data = data;
          byte packet[] = JFPacket.makeWritePacket(tag, data);
          try {
            os.write(packet);
          } catch (Exception e) {
            lastException = e;
            return false;
          }
          return true;
        }
        case ControllerType.NI: {
          JFLog.log("Controller:write():NI not implemented");
          return false;
        }
        case ControllerType.MIC: {
          JFLog.log("Controller:write():MIC not supported");
          return false;
        }
      }
      return false;
    }
  }

  private byte[] readPartial(S7Data s7) {
    byte packet[] = S7Packet.makeReadPacket(s7);
    try {
      os.write(packet);
    } catch (Exception e) {
      lastException = e;
      return null;
    }
    byte reply[] = new byte[1500];
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
    return s7.data;
  }

  /** Reads data from PLC. */
  public byte[] read(String addr) {
    addr = addr.toUpperCase();
    synchronized(lock) {
      if (!connected) return null;
      switch (plc) {
        case ControllerType.S7: {
          S7Data s7 = S7Packet.decodeAddress(addr);
          if (s7 == null) return null;
          byte data[] = new byte[s7.getLength()];
          int offset = 0;
          int read = 0;
          int left = data.length;
          while (read < data.length) {
            if (left > 200) {
              s7.length = (short)(200 / S7Types.getTypeSize(s7.data_type, (short)1));
            } else {
              s7.length = (short)(left / S7Types.getTypeSize(s7.data_type, (short)1));
            }
            byte part[] = readPartial(s7);
            System.arraycopy(part, 0, data, offset, part.length);
            left -= part.length;
            read += part.length;
            s7.offset += part.length << 3;
            offset += part.length;
          }
          return data;
        }
        case ControllerType.MB: {
          ModAddr ma = ModPacket.decodeAddress(addr);
          byte packet[] = ModPacket.makeReadPacket(ma);
          try {
            os.write(packet);
          } catch (Exception e) {
            lastException = e;
            return null;
          }
          byte reply[] = new byte[1500];
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
        case ControllerType.AB: {
          byte packet[] = ABPacket.makeReadPacket(addr, ab_context);
          try {
            os.write(packet);
          } catch (Exception e) {
            lastException = e;
            return null;
          }
          byte reply[] = new byte[1500];
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
        case ControllerType.JF: {
          JFTag tag = JFPacket.decodeAddress(addr);
          if (tag == null) return null;
          byte packet[] = JFPacket.makeReadPacket(tag);
          try {
            os.write(packet);
          } catch (Exception e) {
            lastException = e;
            return null;
          }
          byte reply[] = new byte[1500];
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
          tag = JFPacket.decodePacket(Arrays.copyOf(reply, replySize));
          return tag.data;
        }
        case ControllerType.NI: {
          return daq.read();
        }
        case ControllerType.MIC: {
          byte ret[] = new byte[2];
          if (!mic.read(micBuffer)) return null;
          int max = 0;
          for(int a=0;a<micBufferSize;a++) {
            short sam = micBuffer[a];
            if (sam < 0) sam *= -1;
            if (sam > max) max = sam;
          }
          LE.setuint16(ret, 0, max);
          return ret;
        }
      }
      return null;
    }
  }

 /** Reads multiple data tags from PLC. (only S7 is currently supported) */
  public byte[][] read(String[] addr) {
    if (!connected) return null;
    for(int a=0;a<addr.length;a++) {
      addr[a] = addr[a].toUpperCase();
    }
    switch (plc) {
      case ControllerType.S7: {
        S7Data[] s7 = new S7Data[addr.length];
        for(int a=0;a<addr.length;a++) {
          s7[a] = S7Packet.decodeAddress(addr[a]);
        }
        byte[] packet = S7Packet.makeReadPacket(s7);
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
          } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
        } catch (Exception e) {
          lastException = e;
          return null;
        }
        s7 = S7Packet.decodeMultiPacket(Arrays.copyOf(reply, replySize), addr.length);
        byte[][] ret = new byte[addr.length][];
        for(int a=0;a<addr.length;a++) {
          ret[a] = s7[a].data;
        }
        return ret;
      }
/*
      case ControllerType.MODBUS: {
        ModAddr ma = ModPacket.decodeAddress(addr);
        byte packet[] = ModPacket.makeReadPacket(ma);
        try {
          os.write(packet);
        } catch (Exception e) {
          lastException = e;
          return null;
        }
        byte reply[] = new byte[1500];
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
      case AB: {
        byte packet[] = ABPacket.makeReadPacket(addr, ab_context);
        try {
          os.write(packet);
        } catch (Exception e) {
          lastException = e;
          return null;
        }
        byte reply[] = new byte[1500];
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
*/
      case ControllerType.JF: {
        JFTag[] tags = new JFTag[addr.length];
        for(int a=0;a<addr.length;a++) {
          tags[a] = JFPacket.decodeAddress(addr[a]);
        }
        byte[] packet = JFPacket.makeReadPacket(tags);
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
        tags = JFPacket.decodeMultiPacket(Arrays.copyOf(reply, replySize), addr.length);
        byte[][] ret = new byte[addr.length][];
        for(int a=0;a<addr.length;a++) {
          ret[a] = tags[a].data;
        }
        return ret;
      }
    }
    return null;
  }

  public boolean isConnected() {
    if (plc == 0) return false;
    try {
      switch (plc) {
        case ControllerType.S7:
        case ControllerType.AB:
        case ControllerType.MB:
        case ControllerType.JF:
          return socket.isConnected();
        case ControllerType.NI:
        case ControllerType.MIC:
        default:
          return connected;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /** Returns true is controller is Big Endian byte order. */
  public boolean isBE() {
    switch (plc) {
      case ControllerType.JF: return false;
      case ControllerType.S7: return true;
      case ControllerType.AB: return false;
      case ControllerType.MB: return true;
      case ControllerType.NI: return true;
      case ControllerType.MIC: return false;
      default: return true;
    }
  }

  public boolean isLE() {
    return !isBE();
  }
}

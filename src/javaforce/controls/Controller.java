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

public class Controller {
  private boolean connected;
  private Socket socket;
  private InputStream is;
  private OutputStream os;
  private enum type {S7, MODBUS, AB};
  private type plc;

  private ABContext ab_context;

  public Exception lastException;

  /** Connects to a PLC:
   *
   * url = "S7:host"
   * url = "MODBUS:host"
   * url = "AB:host"
   *
   */
  public boolean connect(String url) {
    if (url.startsWith("S7:")) {
      plc = type.S7;
      String host = url.substring(3);
      try {
        socket = new Socket(host, 102);
        socket.setSoTimeout(3000);
        os = socket.getOutputStream();
        is = socket.getInputStream();

        //connect1
        {
          byte packet[] = S7Packet.makeConnectPacket1();
          os.write(packet);

          byte reply[] = new byte[1500];
          int replySize = 0;
          do {
//            System.out.println("s7.connect1:reading");
            int read = is.read(reply, replySize, 1500 - replySize);
            if (read == -1) throw new Exception("bad read 1");
//            System.out.println("s7.connect1:reply.length=" + read);
            replySize += read;
          } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
        }

        //connect2
        {
          byte packet[] = S7Packet.makeConnectPacket2();
          os.write(packet);

          byte reply[] = new byte[1500];
          int replySize = 0;
          do {
//            System.out.println("s7.connect2:reading");
            int read = is.read(reply, replySize, 1500 - replySize);
            if (read == -1) throw new Exception("bad read 1");
//            System.out.println("s7.connect2:reply.length=" + read);
            replySize += read;
          } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
        }

      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
      connected = true;
      return true;
    }
    if (url.startsWith("MODBUS:")) {
      plc = type.MODBUS;
      String host = url.substring(7);
      try {
        socket = new Socket(host, 502);
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
      plc = type.AB;
      String host = url.substring(3);
      try {
        socket = new Socket(host, 44818);
        socket.setSoTimeout(3000);
        os = socket.getOutputStream();
        is = socket.getInputStream();

        //connect1
        {
          byte packet[] = ABPacket.makeConnectPacket(ab_context);
          os.write(packet);

          byte reply[] = new byte[1500];
          int replySize = 0;
          do {
//            System.out.println("s7.connect1:reading");
            int read = is.read(reply, replySize, 1500 - replySize);
            if (read == -1) throw new Exception("bad read 1");
//            System.out.println("s7.connect1:reply.length=" + read);
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
    return false;
  }
  /** Disconnects from PLC. */
  public boolean disconnect() {
    if (!connected) return false;
    switch (plc) {
      case S7:
      case MODBUS:
      case AB:
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
    }
    connected = false;
    return true;
  }

  /** Data types for write() function.  Only AB protocol requires these. */
  public enum datatype {
    ANY, INTEGER, FLOAT, BOOLEAN
  }

  /** Writes data to PLC. */
  public boolean write(String addr, byte data[]) {
    return write(addr, data, datatype.ANY);
  }

  /** Writes data to PLC. */
  public boolean write(String addr, byte data[], datatype type) {
    if (!connected) return false;
    switch (plc) {
      case S7: {
        S7Data s7 = S7Packet.decodeAddress(addr);
        s7.data = data;
        byte packet[] = S7Packet.makeWritePacket(s7);
        try {
          os.write(packet);
        } catch (Exception e) {
          lastException = e;
          return false;
        }
        return true;
      }
      case MODBUS: {
        ModAddr ma = ModPacket.decodeAddress(addr);
        ma.state = data[0] != 0;
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
      case AB: {
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
    }
    return false;
  }

  /** Reads data from PLC. */
  public byte[] read(String addr) {
    if (!connected) return null;
    switch (plc) {
      case S7: {
        S7Data s7 = S7Packet.decodeAddress(addr);
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
    //        System.out.println("s7.read:reading");
            int read = is.read(reply, replySize, 1500 - replySize);
            if (read == -1) throw new Exception("bad read 1");
    //        System.out.println("s7.read:reply.length=" + read);
            replySize += read;
          } while (!S7Packet.isPacketComplete(Arrays.copyOf(reply, replySize)));
        } catch (Exception e) {
          lastException = e;
          return null;
        }
        s7 = S7Packet.decodePacket(Arrays.copyOf(reply, replySize));
        return s7.data;
      }
      case MODBUS: {
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
            if (read == -1) throw new Exception("bad read 1");
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
    }
    return null;
  }

  public boolean isConnected() {
    try {
      return socket.isConnected();
    } catch (Exception e) {
      return false;
    }
  }
}


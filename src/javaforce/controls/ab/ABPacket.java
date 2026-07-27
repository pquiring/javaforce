package javaforce.controls.ab;

import javaforce.net.ENIPContext;
import java.util.*;

import javaforce.*;
import javaforce.controls.*;
import javaforce.net.*;

/** Allen Bradley Packet
 *   for ControlLogix/CompactLogix (CIP)
 *
 * Captured examples using a Siemens HMI talking to AB PLC.
 *
 * Example Controller Tags:
 *   MyTag
 *   MyTag.MyArray[0]
 *   MyTag.MyArray[0].MyElement
 *   MyTag.MyArray[0,2]
 * Example Program Tags:
 *   Program:MainProgram.MyTag
 *   Program:MainProgram.MyUDT.Element1
 *   etc.
 *
 * @author pquiring
 */

public class ABPacket {
  public static byte[] makeConnectPacket(ENIPContext context) {
    Packet packet = new Packet(Endian.L);
    ENIP ip = new ENIP(ENIP.CMD_REG_SESSION);
    ip.setSizes(0);
    try {
      ip.write(packet, context);
      return packet.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeReadPacket(String tag, ENIPContext context) {
    Packet packet = new Packet(Endian.L);
    ENIP ip = new ENIP(ENIP.CMD_UNCONNECTED_SEND);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_READTAG);
    cip.setRead(tag);
    ip.setSizes(cip.getSize());
    try {
      ip.write(packet, context);
      cip.write(packet);
      return packet.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeReadClockPacket(ENIPContext context) {
    Packet packet = new Packet(Endian.L);
    ENIP ip = new ENIP(ENIP.CMD_UNCONNECTED_SEND);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_GET_ATTR_LIST);
    cip.setReadClock();
    ip.setSizes(cip.getSize());
    try {
      ip.write(packet, context);
      cip.write(packet);
      return packet.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeWritePacket(String tag, byte type, byte[] data, ENIPContext context) {
    Packet packet = new Packet(Endian.L);
    ENIP ip = new ENIP(ENIP.CMD_UNCONNECTED_SEND);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_WRITETAG);
    cip.setWrite(tag, type, data);
    ip.setSizes(cip.getSize());
    try {
      ip.write(packet, context);
      cip.write(packet);
      return packet.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeWriteClockPacket(Calendar dt, ENIPContext context) {
    Packet packet = new Packet(Endian.L);
    ENIP ip = new ENIP(ENIP.CMD_CONNECTED_SEND);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_SET_ATTR_LIST);
    cip.setWriteClock(dt.getTimeInMillis() * 1000L);
    ip.setSizes(cip.getSize());
    try {
      ip.write(packet, context);
      cip.write(packet);
      return packet.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] decodePacket(byte[] data) {
    Packet packet = new Packet(data, Endian.L);
    ENIP ip = new ENIP();
    try {
      ip.read(packet);
      if (ip.cmd == ENIP.CMD_REG_SESSION) return new byte[0];
      CIP cip = new CIP();
      cip.read(packet);
      switch (cip.cmd & 0x7f) {
        case CIP.SUB_CMD_READTAG: {
          return cip.data;
        }
        case CIP.SUB_CMD_WRITETAG: {
          return new byte[0];
        }
        case CIP.SUB_CMD_GET_ATTR_ONE:
        case CIP.SUB_CMD_GET_ATTR_LIST:
        case CIP.SUB_CMD_GET_ATTR_ALL: {
          return cip.attr_values[0];
        }
        case CIP.SUB_CMD_SET_ATTR_ONE:
        case CIP.SUB_CMD_SET_ATTR_LIST:
        case CIP.SUB_CMD_SET_ATTR_ALL: {
          return cip.attrs_success;
        }
        default: {
          JFLog.log("ABPacket:Unknown CIP cmd:0x" + Integer.toHexString(cip.cmd & 0xff));
        }
      }
      return null;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static boolean isPacketComplete(byte[] data) {
    try {
      return decodePacket(data) != null;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public static byte getType(Controller.datatype type) {
    switch (type) {
      case INTEGER8: return ABTypes.SINT;
      case INTEGER16: return ABTypes.INT;
      case INTEGER32: return ABTypes.DINT;
      case INTEGER64: return ABTypes.LINT;
      case FLOAT: return ABTypes.REAL;
      case DOUBLE: return ABTypes.LREAL;
      case BOOLEAN: return ABTypes.BOOL;
    }
    return 0;
  }
}

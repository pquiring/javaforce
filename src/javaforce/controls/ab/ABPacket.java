package javaforce.controls.ab;

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

import java.util.*;

import javaforce.controls.*;

public class ABPacket {
  public static byte[] makeConnectPacket(ABContext context) {
    byte[] packet;
    ENIP ip = new ENIP(ENIP.CMD_GET_SESSION);
    ip.setSizes(0);
    int size = ip.getSize();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.getSize();
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeReadPacket(String tag, ABContext context) {
    byte[] packet;
    ENIP ip = new ENIP(ENIP.CMD_RR_DATA);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_READTAG);
    cip.setRead(tag);
    ip.setSizes(cip.getSize());
    int size = ip.getSize() + cip.getSize();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.getSize();
      cip.write(packet, offset);
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeReadClockPacket(ABContext context) {
    byte[] packet;
    ENIP ip = new ENIP(ENIP.CMD_RR_DATA);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_GET_ATTR);
    cip.setReadClock();
    ip.setSizes(cip.getSize());
    int size = ip.getSize() + cip.getSize();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.getSize();
      cip.write(packet, offset);
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeWritePacket(String tag, byte type, byte[] data, ABContext context) {
    byte[] packet;
    ENIP ip = new ENIP(ENIP.CMD_RR_DATA);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_WRITETAG);
    cip.setWrite(tag, type, data);
    ip.setSizes(cip.getSize());
    int size = ip.getSize() + cip.getSize();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.getSize();
      cip.write(packet, offset);
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeWriteClockPacket(Calendar dt, ABContext context) {
    byte[] packet;
    ENIP ip = new ENIP(ENIP.CMD_SEND_UNIT_DATA);
    CIP cip = new CIP(CIP.CMD_UNCONNECTED_SEND, CIP.SUB_CMD_SET_ATTR);
    cip.clock = dt.getTimeInMillis() * 1000L;
    cip.setWriteClock();
    ip.setSizes(cip.getSize());
    int size = ip.getSize() + cip.getSize();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.getSize();
      cip.write(packet, offset);
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] decodePacket(byte[] data) {
    ENIP ip = new ENIP();
    int offset = 0;
    try {
      ip.read(data, offset); offset += ip.getSize();
      if (ip.cmd == ENIP.CMD_GET_SESSION) return new byte[0];
      switch (data[offset] & 0x7f) {
        case CIP.SUB_CMD_READTAG: {
          CIP cip = new CIP((byte)0, data[offset]);
          cip.readReplyReadTag(data, offset);
          return cip.data;
        }
        case CIP.SUB_CMD_WRITETAG: {
          CIP cip = new CIP((byte)0, data[offset]);
          cip.readReplyWriteTag(data, offset);
          return new byte[0];
        }
        case CIP.SUB_CMD_GET_ATTR: {
          CIP cip = new CIP((byte)0, data[offset]);
          cip.readReplyGetAttrs(data, offset);
          return cip.attrs[0];
        }
        case CIP.SUB_CMD_SET_ATTR: {
          CIP cip = new CIP((byte)0, data[offset]);
          cip.readReplySetAttrs(data, offset);
          return new byte[0];
        }
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean isPacketComplete(byte[] data) {
    try {
      return decodePacket(data) != null;
    } catch (Exception e) {
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

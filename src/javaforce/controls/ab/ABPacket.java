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

import javaforce.controls.*;

public class ABPacket {
  public static byte[] makeConnectPacket(ABContext context) {
    byte packet[];
    ENIP ip = new ENIP(ENIP.CMD_GET_SESSION);
    ip.setSizes(0);
    int size = ip.size();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.size();
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeReadPacket(String tag, ABContext context) {
    byte packet[];
    ENIP ip = new ENIP(ENIP.CMD_RR_DATA);
    CIP_Request cip = new CIP_Request();
    cip.setRead(tag);
    ip.setSizes(cip.size());
    int size = ip.size() + cip.size();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.size();
      cip.write(packet, offset);
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] makeWritePacket(String tag, byte type, byte data[], ABContext context) {
    byte packet[];
    ENIP ip = new ENIP(ENIP.CMD_RR_DATA);
    CIP_Request cip = new CIP_Request();
    cip.setWrite(tag, type, data);
    ip.setSizes(cip.size());
    int size = ip.size() + cip.size();
    packet = new byte[size];
    int offset = 0;
    try {
      ip.write(packet, offset, context); offset += ip.size();
      cip.write(packet, offset);
      return packet;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static byte[] decodePacket(byte data[]) {
    ENIP ip = new ENIP();
    int offset = 0;
    try {
      ip.read(data, offset); offset += ip.size();
      if (ip.cmd == ENIP.CMD_GET_SESSION) return new byte[0];
      switch (data[offset]) {
        case (byte)0xcc:
          CIP_Reply_Read cip_read = new CIP_Reply_Read();
          cip_read.read(data, offset);
          return cip_read.tagdata;
        case (byte)0xcd:
          CIP_Reply_Write cip_write = new CIP_Reply_Write();
          cip_write.read(data, offset);
          return new byte[0];
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean isPacketComplete(byte data[]) {
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

package javaforce.net;

import java.util.*;

/** BER Encoding.
 *
 * Encoding = TLV (tag/length/value)
 *
 * @author pquiring
 */

public class BER implements SubPacket {
  private byte[] type;
  private Packet data = new Packet();

  public int getSize() {
    return type.length + 1 + getLengthBytes(data.offset) + data.offset;
  }

  public int getDataSize() {
    return getSize();
  }

  public void read(Packet packet) throws Exception {
    //TODO
  }

  public void write(Packet packet) throws Exception {
    //write type
    packet.write(type);
    //write length
    writeLength(packet, data.offset);
    //write data
    packet.write(data.data, 0, data.offset);
  }

  private byte getLengthBytes(int length) {
    if (length < 256) {
      return 1;
    } else if (length < 65536) {
      return 2;
    } else if (length < 16777215) {
      return 3;
    } else {
      return 4;
    }
  }

  private void writeLength(Packet packet, int length) throws Exception {
    byte lenSize = getLengthBytes(length);
    packet.writeByte((byte)(0x80 + lenSize));
    switch (lenSize) {
      case 1: packet.writeByte((byte)length); break;
      case 2: packet.writeShort((short)length); break;
      case 3: packet.writeInt24(length); break;
      case 4: packet.writeInt(length); break;
    }
  }

  /** Set Type.
   *
   * @param type = BER Type
   */
  public void setType(byte[] type) {
    setType(type, 0, type.length);
  }

  /** Set Type.
   *
   * [0] = CCF11111  //CC=1 -> APPLICATION defined, F=1 -> constructed
   * [1] = 1TTTTTTT  //0x80 = more bytes (7bits Type)
   * [n] = 0TTTTTTT  //0x00 = last byte (7bits Type)
   * Type = TT..TT
   *
   * @param type = BER Type
   */
  public void setType(byte[] type, int type_offset, int type_length) {
    this.type = Arrays.copyOfRange(type, type_offset, type_offset + type_length);
  }

  /** Append 8bit String (max length = 255). */
  public void appendString(byte[] str) throws Exception {
    data.writeByte((byte)0x04);  //type
    int length = str.length;
    if (length > 255) {
      writeLength(data, length);
    } else {
      data.writeByte((byte)length);  //length
    }
    data.write(str);
  }

  /** Append boolean. */
  public void appendBoolean(boolean value) throws Exception {
    data.writeByte((byte)0x01);  //type
    data.writeByte((byte)0x01);  //length
    if (value) {
      data.writeByte((byte)(byte)0xff);  //true
    } else {
      data.writeByte((byte)0x00);  //false
    }
  }

  public void appendSequence(int length) throws Exception {
    data.writeByte((byte)0x30);  //type
    data.writeByte((byte)(byte)length);  //length
  }

  public void appendInteger(int value) throws Exception {
    data.writeByte((byte)0x02);  //type
    byte bytes = getLengthBytes(value);
    data.writeByte((byte)bytes);  //length
    switch (bytes) {
      case 1: data.writeByte((byte)value); break;
      case 2: data.writeShort((short)value); break;
      case 3: data.writeInt24(value); break;
      case 4: data.writeInt(value); break;
    }
  }
}

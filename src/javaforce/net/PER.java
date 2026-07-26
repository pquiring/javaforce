package javaforce.net;

/** PER Encoding.
 *
 * Encoding = Values "as is".
 *
 * Order of values is implied.
 *
 * @author pquiring
 */

public class PER implements SubPacket {
  private Packet data = new Packet();

  private static int _16k = 16 * 1024;
  private static int _32k = 32 * 1024;
  private static int _48k = 48 * 1024;
  private static int _64k = 64 * 1024;
  private static int _80k = 80 * 1024;

  public int getSize() {
    return data.offset;
  }

  public int getDataSize() {
    return getSize();
  }

  public void read(Packet packet) throws Exception {
  }

  public void write(Packet packet) throws Exception {
    packet.write(data.data, 0, data.offset);
  }

  public byte[] getData() {
    return data.data;
  }

  public int getLength() {
    return data.length;
  }

  public byte[] toByteArray() {
    return data.toByteArray();
  }

  /** Fragment into multiple fragments. */
  private boolean fragment(int length) {
    return length >= _16k;
  }

  /** Write length (may be fragment if > 16K) */
  private int writeLength(int length) throws Exception {
    if (length < 128) {
      data.writeByte((byte)length);
      return length;
    } else if (length < _16k) {
      data.writeShort((short)(0x8000 + length));
      return length;
    } else if (length < _32k) {
      data.writeByte((byte)(0xc1));
      return _16k;
    } else if (length < _48k) {
      data.writeByte((byte)(0xc2));
      return _32k;
    } else if (length < _64k) {
      data.writeByte((byte)(0xc3));
      return _48k;
    } else {
      data.writeByte((byte)(0xc4));
      return _64k;
    }
  }

  public void setEndian(byte value) {
    data.setEndian(value);
  }

  public void append(byte[] buf) throws Exception {
    data.write(buf);
  }

  public void appendByte(byte value) throws Exception {
    data.writeByte(value);
  }

  public void appendShort(short value) throws Exception {
    data.writeShort(value);
  }

  public void appendInt(int value) throws Exception {
    data.writeInt(value);
  }

  public void appendString16(byte[] str, int length) throws Exception {
    for(int a=0;a<str.length;a++) {
      data.writeByte(str[a]);
      data.writeByte((byte)0);
      length--;
    }
    while (length > 0) {
      data.writeShort((short)0);
      length--;
    }
  }
}

package javaforce.net;

import java.io.*;
import java.util.*;

import javaforce.*;

/** Ethernet Packet
 *
 * @author pquiring
 */

public class Packet {

  private static int mtu = 1500;

  public Packet() {
    data = new byte[mtu];
  }
  public Packet(byte endian) {
    data = new byte[mtu];
    this.endian = endian;
  }
  public Packet(byte[] data) {
    this.data = data;
    this.length = data.length;
  }
  public Packet(byte[] data, byte endian) {
    this.data = data;
    this.length = data.length;
    this.endian = endian;
  }
  public Packet(byte[] data, int offset, int length) {
    this.data = data;
    this.offset = offset;
    this.length = length;
  }
  public Packet(byte[] data, int offset, int length, byte endian) {
    this.data = data;
    this.offset = offset;
    this.length = length;
    this.endian = endian;
  }
  /** Packet data. */
  public byte[] data;
  /** Packet offset. */
  public int offset;
  /** Packet length. */
  public int length;
  /** Packet Endian (default = B)
   * @see javaforce.Endian
   */
  public byte endian = Endian.B;

  /** Packet source host. */
  public String host;
  /** Packet source port. */
  public int port;

  /** Packet media stream. */
  public int stream;
  /** Packet media timestamp. */
  public long ts;
  /** Packet media key frame. */
  public boolean keyFrame;

  /** Reset offset to start reading packet. */
  public void resetRead() {
    offset = 0;
  }

  /** Reset offset to start writing packet. */
  public void resetWrite() {
    offset = 0;
    length = 0;
  }

  /** Set Packet Endian.
   * @param value = javaforce.Endian.*
   */
  public void setEndian(byte value) {
    if (value < Endian.min || value > Endian.max) return;
    endian = value;
  }

  public byte peekByte() {
    return data[offset];
  }

  public byte readByte() throws Exception {
    if (offset >= length) throw new Exception("Buffer underflow");
    return data[offset++];
  }

  public short readShort() throws Exception {
    if (offset + 2 > length) throw new Exception("Buffer underflow");
    short value;
    if (endian == Endian.B)
      value = (short)BE.getuint16(data, offset);
    else
      value = (short)LE.getuint16(data, offset);
    offset += 2;
    return value;
  }

  public int readInt() throws Exception {
    if (offset + 4 > length) throw new Exception("Buffer underflow");
    int value;
    if (endian == Endian.B)
      value = BE.getuint32(data, offset);
    else
      value = LE.getuint32(data, offset);
    offset += 4;
    return value;
  }

  public long readLong() throws Exception {
    if (offset + 8 > length) throw new Exception("Buffer underflow");
    long value;
    if (endian == Endian.B)
      value = BE.getuint64(data, offset);
    else
      value = LE.getuint64(data, offset);
    offset += 8;
    return value;
  }

  public void read(byte[] buf) throws Exception {
    read(buf, 0, buf.length);
  }

  public void read(byte[] buf, int buf_offset, int buf_length) throws Exception {
    if (offset + buf_length > length) throw new Exception("Buffer underflow");
    System.arraycopy(data, offset, buf, buf_offset, buf_length);
    offset += buf_length;
  }

  public void writeByte(byte value) throws Exception {
    if (offset + 1 > data.length) throw new Exception("Buffer overflow");
    data[offset++] = value;
    if (offset > length) {
      length = offset;
    }
  }

  public void writeShort(short value) throws Exception {
    if (offset + 2 > data.length) throw new Exception("Buffer overflow");
    if (endian == Endian.B)
      BE.setuint16(data, offset, value);
    else
      LE.setuint16(data, offset, value);
    offset += 2;
    if (offset > length) {
      length = offset;
    }
  }

  public void writeInt(int value) throws Exception {
    if (offset + 4 > data.length) throw new Exception("Buffer overflow");
    if (endian == Endian.B)
      BE.setuint32(data, offset, value);
    else
      LE.setuint32(data, offset, value);
    offset += 4;
    if (offset > length) {
      length = offset;
    }
  }

  public void writeInt24(int value) throws Exception {
    if (offset + 4 > data.length) throw new Exception("Buffer overflow");
    if (endian == Endian.B) {
      BE.setuint32(data, offset, value);
      System.arraycopy(data, offset + 1, data, offset, 3);
    } else {
      LE.setuint32(data, offset, value);
    }
    offset += 3;
    if (offset > length) {
      length = offset;
    }
  }

  public void writeLong(long value) throws Exception {
    if (offset + 8 > data.length) throw new Exception("Buffer overflow");
    if (endian == Endian.B)
      BE.setuint64(data, offset, value);
    else
      LE.setuint64(data, offset, value);
    offset += 8;
    if (offset > length) {
      length = offset;
    }
  }

  public void write(byte[] buf) throws Exception {
    write(buf, 0, buf.length);
  }

  public void write(byte[] buf, int buf_offset, int buf_length) throws Exception {
    if (offset + buf_length > data.length) throw new Exception("Buffer overflow");
    System.arraycopy(buf, buf_offset, data, offset, buf_length);
    offset += buf_length;
    if (offset > length) {
      length = offset;
    }
  }

  public static void setDefaultMTU(int mtu) {
    if (mtu < 1500 || mtu > 65536) return;
    Packet.mtu = mtu;
  }

  public byte[] toByteArray() {
    if (length == data.length) {
      return data;
    }
    return Arrays.copyOf(data, length);
  }

  public void toFile(String filename) {
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(data, 0, length);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String toString() {
    return "Packet:{data:" + data + ",offset:" + offset + ",length:" + length + "}";
  }
}

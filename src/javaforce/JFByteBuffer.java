package javaforce;

/** Byte Buffer
 *
 * Supports read() and write() operations in Big Endian format.
 *
 * @author pquiring
 */

public class JFByteBuffer {
  private byte[] buffer;
  private int length;
  private int offset;

  private int max_length;
  private byte[] tmp = new byte[8];

  public JFByteBuffer(int max_length) {
    this.max_length = max_length;
    buffer = new byte[max_length];
  }

  public void reset() {
    length = 0;
    offset = 0;
  }
  public void setLength(int len) {
    length = len;
    offset = 0;
  }
  public byte[] getBuffer() {
    return buffer;
  }

  //read ops
  public byte readByte() {
    if (offset + 1 > length) return -1;
    return buffer[offset++];
  }
  public short readShort() {
    if (offset + 2 > length) return -1;
    short value = (short)BE.getuint16(buffer, offset);
    offset += 2;
    return value;
  }
  public int readInt() {
    if (offset + 4 > length) return -1;
    int value = BE.getuint32(buffer, offset);
    offset += 4;
    return value;
  }
  public long readLong() {
    if (offset + 8 > length) return -1;
    long value = BE.getuint64(buffer, offset);
    offset += 8;
    return value;
  }
  public boolean readBytes(byte[] out, int off, int len) {
    if (offset + len > length) return false;
    System.arraycopy(buffer, offset, out, off, len);
    offset += len;
    return true;
  }

  //write ops
  public boolean write(byte b) {
    buffer[offset++] = b;
    length++;
    return true;
  }
  public boolean write(short len) {
    BE.setuint16(tmp, 0, len);
    return write(tmp, 0, 2);
  }
  public boolean write(int len) {
    BE.setuint32(tmp, 0, len);
    return write(tmp, 0, 4);
  }
  public boolean write(long len) {
    BE.setuint64(tmp, 0, len);
    return write(tmp, 0, 8);
  }
  public boolean write(byte[] buf, int off, int len) {
    if (offset + len > max_length) return false;
    System.arraycopy(buf, off, buffer, offset, len);
    offset += len;
    length += len;
    return true;
  }
}

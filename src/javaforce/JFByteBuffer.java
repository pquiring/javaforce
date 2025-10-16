package javaforce;

/** Byte Buffer (fifo)
 *
 * Supports read() and write() operations in Big Endian format.
 *
 * read() operations remove bytes from buffer.
 * write() operations append bytes to buffer.
 *
 * Buffer size does not grow and does not wrap around.
 * Attempts to write more data than is available in the buffer will fail.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class JFByteBuffer {
  private byte[] buffer;
  private int length;
  private int read_offset;
  private int write_offset;

  private int max_length;
  private byte[] tmp = new byte[8];

  public JFByteBuffer(int max_length) {
    this.max_length = max_length;
    buffer = new byte[max_length];
  }

  /** Resets buffer length and read/write offsets. */
  public void reset() {
    length = 0;
    read_offset = 0;
    write_offset = 0;
  }

  /** Returns underlying byte buffer. */
  public byte[] getBuffer() {
    return buffer;
  }

  /** Returns read offset in buffer. */
  public int getOffset() {
    return read_offset;
  }

  /** Bytes that can read. */
  public int getLength() {
    return length;
  }

  /** Bytes that can be written (appended). */
  public int available() {
    return buffer.length - write_offset;
  }

  /** Moves any data left in the buffer to the start of the buffer. */
  public void compact() {
    if (read_offset == 0) return;
    if (length == 0) {
      read_offset = 0;
      write_offset = 0;
      return;
    }
    System.arraycopy(buffer, read_offset, buffer, 0, length);
    read_offset = 0;
    write_offset = length;
  }

  /** Searches for key and returns index.  Returns -1 if not found. */
  public int indexOf(byte[] key) {
    int key_len = key.length;
    if (length < key_len) return -1;
    int end = length - key_len;
    for(int i=0;i <= end;i++) {
      if (Arrays.compare(buffer, read_offset + i, read_offset + i + key_len, key, 0, key_len) == 0) return i;
    }
    return -1;
  }

  /** Checks if buffer starts with key bytes. */
  public boolean startsWith(byte[] key) {
    int key_len = key.length;
    if (length < key_len) return false;
    for(int i=0;i<key_len;i++) {
      if (buffer[read_offset + i] != key[i]) return false;
    }
    return true;
  }

  /** Skips bytes */
  public void skip(int bytes) {
    if (length <= bytes) {
      length = 0;
      read_offset = 0;
      write_offset = 0;
      return;
    }
    read_offset += bytes;
    length -= bytes;
  }

  //read (remove) ops
  /** Read one byte. Returns -1 if buffer is empty. */
  public byte readByte() {
    if (length < 1) return -1;
    length--;
    return buffer[read_offset++];
  }
  /** Read one short. Returns -1 if buffer is empty. */
  public short readShort() {
    if (length < 2) return -1;
    short value = (short)BE.getuint16(buffer, read_offset);
    read_offset += 2;
    length -= 2;
    return value;
  }
  /** Read one int. Returns -1 if buffer is empty. */
  public int readInt() {
    if (length < 4) return -1;
    int value = BE.getuint32(buffer, read_offset);
    read_offset += 4;
    length -= 4;
    return value;
  }
  /** Read one long. Returns -1 if buffer is empty. */
  public long readLong() {
    if (length < 8) return -1;
    long value = BE.getuint64(buffer, read_offset);
    read_offset += 8;
    length -= 8;
    return value;
  }
  /** Reads bytes placing them in out array. */
  public boolean readBytes(byte[] out, int off, int len) {
    if (length < len) return false;
    System.arraycopy(buffer, read_offset, out, off, len);
    read_offset += len;
    length -= len;
    return true;
  }
  /** Reads bytes placing them in OutputStream. */
  public int readBytes(OutputStream os, int max_bytes) {
    if (max_bytes > length) {
      max_bytes = length;
    }
    try {
      os.write(buffer, read_offset, max_bytes);
      length -= max_bytes;
      read_offset += max_bytes;
      return max_bytes;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }
  /** Reads a string of specified length. */
  public String readString(int strlen) {
    if (length < strlen) return null;
    String s = new String(buffer, read_offset, strlen);
    read_offset += strlen;
    length -= strlen;
    return s;
  }

  //write (append) ops
  /** Write (appends) one byte. */
  public boolean write(byte b) {
    if (available() < 1) return false;
    buffer[write_offset++] = b;
    length++;
    return true;
  }
  /** Writes (appends) one short. */
  public boolean write(short len) {
    BE.setuint16(tmp, 0, len);
    return write(tmp, 0, 2);
  }
  /** Writes (appends) one int. */
  public boolean write(int len) {
    BE.setuint32(tmp, 0, len);
    return write(tmp, 0, 4);
  }
  /** Writes (appends) one long. */
  public boolean write(long len) {
    BE.setuint64(tmp, 0, len);
    return write(tmp, 0, 8);
  }
  /** Appends in array to buffer. */
  public boolean write(byte[] in, int off, int len) {
    if (available() < len) return false;
    System.arraycopy(in, off, buffer, write_offset, len);
    write_offset += len;
    length += len;
    return true;
  }
  /** Appends string to buffer. */
  public boolean write(String s) {
    return write(s.getBytes(), 0, s.length());
  }
  /** Appends bytes from InputStream to buffer. */
  public int write(InputStream is, int max_bytes) {
    if (max_bytes > available()) {
      max_bytes = available();
    }
    try {
      int read = is.read(buffer, write_offset, max_bytes);
      if (read == -1) {
        throw new Exception("JFByteBuffer:read() failed");
      }
      if (read > 0) {
        write_offset += read;
        length += read;
      }
      return read;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }
}

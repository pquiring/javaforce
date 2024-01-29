package javaforce;

import java.io.*;

public class Base64 {

  private static final byte[] etable = new byte[] {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/', '='
  };
  private static final byte[] dtable = new byte[] {
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0x3e, -1, -1, -1, 0x3f,
    0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, -1, -1, -1, 0x40, -1, -1,
    -1, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e,
    0x0f, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, -1, -1, -1, -1, -1,
    -1, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28,
    0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x30, 0x31, 0x32, 0x33, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
  };

  /** Encodes raw data into Base64 format.
   * @param in = input data
   */
  public static final byte[] encode(byte[] in) {
    return encode(in, -1);
  }

  /** Encodes raw data into Base64 format.
   * @param in = input data
   * @param lineLength = max line length (-1 to ignore) */
  public static final byte[] encode(byte[] in, int lineLength) {
    //convert 3x8bit into 4x6bit
    int i3 = in.length / 3;
    int i3m = in.length % 3;
    byte e1, e2, e3, e4;  //elements
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    int inpos = 0;
    if (lineLength > 0) {
      lineLength &= (0x10000 - 3);
    }

    for (int a = 0; a < i3; a++) {
      e1 = (byte) (((in[inpos] & 0xfc) >>> 2));
      e2 = (byte) (((in[inpos + 1] & 0xf0) >>> 4) + ((in[inpos] & 0x03) << 4));
      e3 = (byte) (((in[inpos + 1] & 0x0f) << 2) + ((in[inpos + 2] & 0xc0) >>> 6));
      e4 = (byte) (in[inpos + 2] & 0x3f);
      out.write(etable[e1]);
      out.write(etable[e2]);
      out.write(etable[e3]);
      out.write(etable[e4]);
      if ((lineLength > 0) && (a % lineLength == 0) && (a > 0)) {
        out.write('\n');
      }
      inpos += 3;
    }
    if (i3m > 0) {
      //Note : 64 == '='
      e1 = (byte) ((in[inpos] & 0xfc) >>> 2);
      if (i3m == 1) {
        e2 = (byte) ((in[inpos] & 0x03) << 4);
        e3 = 64;
      } else {
        e2 = (byte) (((in[inpos + 1] & 0xf0) >>> 4) + ((in[inpos] & 0x03) << 4));
        e3 = (byte) ((in[inpos + 1] & 0x0f) << 2);
      }
      e4 = 64;
      out.write(etable[e1]);
      out.write(etable[e2]);
      out.write(etable[e3]);
      out.write(etable[e4]);
    }
    return out.toByteArray();
  }

  private static final class Buffer {
    byte[] buf;
    int pos;
  }

  private static final int getbyte(Buffer buffer) {
    int ret;
    while (true) {
      if (buffer.pos == buffer.buf.length) {
        return 65;
      }
      ret = dtable[buffer.buf[buffer.pos++]];
      if (ret != -1) {
        break;
      }
    }
    return ret;
  }

  /** Decodes Base64 data into raw data.
   * @param in = input data
   */
  public static final byte[] decode(byte[] in) {
    int e1, e2, e3, e4;
    int o32;

    Buffer buffer = new Buffer();
    buffer.buf = in;
    buffer.pos = 0;

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    while (buffer.pos != buffer.buf.length) {
      e1 = getbyte(buffer);
      if (e1 == 65) {
        break;
      }
      e2 = getbyte(buffer);
      if (e2 == 65) {
        break;
      }
      e3 = getbyte(buffer);
      if (e3 == 65) {
        break;
      }
      e4 = getbyte(buffer);
      if (e4 == 65) {
        break;
      }
      o32 = e1 << 2;
      o32 |= (((e2 & 0x30) >> 4) + ((e2 & 0x0f) << 12));
      if (e3 != 64) {
        o32 |= (((e3 & 0x3c) << 6) + ((e3 & 0x03) << 22));
      }
      if (e4 != 64) {
        o32 |= (e4 << 16);
      }
      out.write(o32 & 0xff);
      if (e3 != 64) {
        out.write((o32 & 0xff00) >> 8);
      }
      if (e4 != 64) {
        out.write((o32 & 0xff0000) >> 16);
      }
      if (e4 == 64) {
        break;
      }
    }
    return out.toByteArray();
  }
}

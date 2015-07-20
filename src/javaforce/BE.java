package javaforce;

/**
 *  Big Endian get/set functions.
 *
 *  Usage : network packets, Motorola CPU style, etc.
 *
 *  Could use java.nio.ByteBuffer but sometimes this is faster.
 *
 *  Created : Dec 5, 2013
 */

public class BE {

  public static int getuint8(byte[] data, int offset) {
    return ((int)data[offset]) & 0xff;
  }

  public static int getuint16(byte[] data, int offset) {
    int ret;
    ret = ((int) data[offset] & 0xff) << 8;
    ret += ((int)data[offset + 1]) & 0xff;
    return ret;
  }

  public static int getuint32(byte[] data, int offset) {
    int ret;
    ret = ((int) data[offset] & 0xff) << 24;
    ret += ((int) data[offset + 1] & 0xff) << 16;
    ret += ((int) data[offset + 2] & 0xff) << 8;
    ret += ((int) data[offset + 3] & 0xff);
    return ret;
  }

  public static long getuint64(byte[] data, int offset) {
    long ret;
    ret = ((long) data[offset] & 0xff) << 56;
    ret += ((long) data[offset + 1] & 0xff) << 48;
    ret += ((long) data[offset + 2] & 0xff) << 40;
    ret += ((long) data[offset + 3] & 0xff) << 32;
    ret += ((long) data[offset + 4] & 0xff) << 24;
    ret += ((long) data[offset + 5] & 0xff) << 16;
    ret += ((long) data[offset + 6] & 0xff) << 8;
    ret += ((long) data[offset + 7] & 0xff);
    return ret;
  }

  public static void setuint8(byte data[], int offset, int value) {
    data[offset+0] = (byte)(value & 0xff);
  }

  public static void setuint16(byte data[], int offset, int value) {
    data[offset+1] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+0] = (byte)(value & 0xff);
  }

  public static void setuint32(byte data[], int offset, int value) {
    data[offset+3] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+2] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+1] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+0] = (byte)(value & 0xff);
  }

  public static void setuint64(byte data[], int offset, long value) {
    data[offset+7] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+6] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+5] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+4] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+3] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+2] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+1] = (byte)(value & 0xff);
    value >>= 8;
    data[offset+0] = (byte)(value & 0xff);
  }

  //these funcs are no different (just conv functions)

  public static String getString(byte data[], int offset, int len) {
    return LE.getString(data, offset, len);
  }

  public static void setString(byte data[], int offset, int len, String str) {
    LE.setString(data, offset, len, str);
  }

  //Using arrays are 40% faster than using java.nio.ByteBuffer

  public static short[] byteArray2shortArray(byte in[], short out[]) {
    int len = in.length / 2;
    if (out == null) out = new short[len];
    int p = 0;
    short val;
    for (int a = 0; a < len; a++) {
      val  = (short)((((short)in[p++]) & 0xff) << 8);
      val += (((short)in[p++]) & 0xff);
      out[a] = val;
    }
    return out;
  }
  public static int[] byteArray2intArray(byte in[], int out[]) {
    int len = in.length / 4;
    if (out == null) out = new int[len];
    int p = 0;
    int val;
    for (int a = 0; a < len; a++) {
      val  = (((int)in[p++]) & 0xff) << 24;
      val += (((int)in[p++]) & 0xff) << 16;
      val += (((int)in[p++]) & 0xff) << 8;
      val += (((int)in[p++]) & 0xff);
      out[a] = val;
    }
    return out;
  }
  public static byte[] shortArray2byteArray(short in[], byte out[]) {
    int len = in.length;
    if (out == null) out = new byte[len * 2];
    int p = 0;
    short val;
    for (int a = 0; a < len; a++) {
      val = in[a];
      out[p++] = (byte) ((val & 0xff00) >>> 8);
      out[p++] = (byte) (val & 0xff);
    }
    return out;
  }
  public static byte[] intArray2byteArray(int in[], byte out[]) {
    int len = in.length;
    if (out == null) out = new byte[len * 4];
    int p = 0;
    int val;
    for (int a = 0; a < len; a++) {
      val = in[a];
      out[p++] = (byte) ((val & 0xff000000) >>> 8);
      out[p++] = (byte) ((val & 0xff0000) >> 8);
      out[p++] = (byte) ((val & 0xff00) >> 8);
      out[p++] = (byte) (val & 0xff);
    }
    return out;
  }

}

package javaforce;

/**
 *  Little Endian get/set functions.
 *
 *  Usage : Windows files, Intel CPU style, etc.
 *
 *  Could use java.nio.ByteBuffer but sometimes this is faster.
 *
 *  Created : Dec 5, 2013
 */

public class LE {

  public static int getuint8(byte[] data, int offset) {
    int ret;
    ret  = (int)data[offset] & 0xff;
    return ret;
  }

  public static int getuint16(byte[] data, int offset) {
    int ret;
    ret  = data[offset] & 0xff;
    ret += (data[offset+1] & 0xff) << 8;
    return ret;
  }

  public static int getuint32(byte[] data, int offset) {
    int ret;
    ret  = (int)data[offset] & 0xff;
    ret += ((int)data[offset+1] & 0xff) << 8;
    ret += ((int)data[offset+2] & 0xff) << 16;
    ret += ((int)data[offset+3] & 0xff) << 24;
    return ret;
  }

  public static float getfloat(byte[] data, int offset) {
    return Float.intBitsToFloat(getuint32(data, offset));
  }

  public static long getuint64(byte[] data, int offset) {
    long ret;
    ret  = (long)data[offset] & 0xff;
    ret += ((long)data[offset+1] & 0xff) << 8;
    ret += ((long)data[offset+2] & 0xff) << 16;
    ret += ((long)data[offset+3] & 0xff) << 24;
    ret += ((long)data[offset+4] & 0xff) << 32;
    ret += ((long)data[offset+5] & 0xff) << 40;
    ret += ((long)data[offset+6] & 0xff) << 48;
    ret += ((long)data[offset+7] & 0xff) << 56;
    return ret;
  }

  public static double getdouble(byte[] data, int offset) {
    return Double.longBitsToDouble(getuint64(data, offset));
  }

  public static String getString(byte[] data, int offset, int len) {
    String ret = "";
    while (len > 0) {
      if (data[offset]==0) break;
      ret += (char)data[offset++];
      len--;
    }
    return ret;
  }

  public static void setuint8(byte[] data, int offset, int num) {
    data[offset] = (byte)(num & 0xff);
  }

  public static void setuint16(byte[] data, int offset, int num) {
    data[offset+0] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+1] = (byte)(num & 0xff);
  }

  public static void setuint32(byte[] data, int offset, int num) {
    data[offset+0] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+1] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+2] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+3] = (byte)(num & 0xff);
  }

  public static void setfloat(byte[] data, int offset, float num) {
    setuint32(data, offset, Float.floatToIntBits(num));
  }

  public static void setuint64(byte[] data, int offset, long num) {
    data[offset+0] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+1] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+2] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+3] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+4] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+5] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+6] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+7] = (byte)(num & 0xff);
  }

  public static void setdouble(byte[] data, int offset, double num) {
    setuint64(data, offset, Double.doubleToLongBits(num));
  }

  public static void setString(byte[] data, int offset, int len, String str) {
    int pos = 0;
    while (len > 0) {
      if (pos >= str.length()) {
        data[offset++] = 0;
      } else {
        data[offset++] = (byte)str.charAt(pos++);
      }
      len--;
    }
  }

  //Using arrays are 40% faster than using java.nio.ByteBuffer

  public static short[] byteArray2shortArray(byte in[], short out[]) {
    int len = in.length / 2;
    if (out == null) out = new short[len];
    int p = 0;
    short val;
    for (int a = 0; a < len; a++) {
      val  = (short)(in[p++] & 0xff);
      val += (in[p++] & 0xff) << 8;
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
      val  = (in[p++] & 0xff);
      val += (in[p++] & 0xff) << 8;
      val += (in[p++] & 0xff) << 16;
      val += (in[p++] & 0xff) << 24;
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
      out[p++] = (byte) (val & 0xff);
      out[p++] = (byte) (val >>> 8);
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
      out[p++] = (byte) (val & 0xff);
      val >>>= 8;
      out[p++] = (byte) (val & 0xff);
      val >>>= 8;
      out[p++] = (byte) (val & 0xff);
      out[p++] = (byte) (val >>> 8);
    }
    return out;
  }
  public static short[] swap(short[] input) {
    int t1, t2;
    for(int a=0;a<input.length;a++) {
      t1 = input[a] & 0xffff;
      t2 = t1;
      t1 <<= 8;
      t1 &= 0xff00;
      t2 >>= 8;
      input[a] = (short)(t1 | t2);
    }
    return input;
  }
}

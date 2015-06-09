package javaforce.utils;

/**
 * Desc : Lists chunks in a 3DS file
 *
 * Usage : java -cp /usr/share/java/javaforce.jar javaforce.utils.list3ds
 * filein.3ds
 *
 */
import java.io.*;

public class list3ds {

  static FileInputStream fi;
  static String str;

  static void readstr() throws Exception {
    str = "";
    char ch;
    while (fi.available() > 0) {
      ch = (char) fi.read();
      if (ch == 0) {
        return;
      }
      str += ch;
    }
    str = "";
  }
  private static final int _3DS_FLG_TENSION = 0x01;
  private static final int _3DS_FLG_CONTINUITY = 0x02;
  private static final int _3DS_FLG_BIAS = 0x04;
  private static final int _3DS_FLG_EASE_TO = 0x08;
  private static final int _3DS_FLG_EASE_FROM = 0x10;

  static void readx(boolean four) throws Exception {
    int u16;
    int u32;
    float f;
    float f4[] = new float[4];
    int cnt;

    u16 = readuint16(fi);
    System.out.format("  flgs = %d\n", u16);
    u32 = readuint32(fi);
    System.out.format("  res1 = %d\n", u32);
    u32 = readuint32(fi);
    System.out.format("  res2 = %d\n", u32);
    cnt = readuint32(fi);
    System.out.format("  keys = %d\n", cnt);
    while (cnt-- > 0) {
      System.out.format("  tcb {\n");
      u32 = readuint32(fi);
      System.out.format("    frame = %d\n", u32);
      u16 = readuint16(fi);
      System.out.format("    flgs = %d\n", u16);
      if ((u16 & _3DS_FLG_TENSION) != 0) {
        f = readfloat(fi);
        System.out.format("    tension = %f\n", f);
      }
      if ((u16 & _3DS_FLG_CONTINUITY) != 0) {
        f = readfloat(fi);
        System.out.format("    continuity = %f\n", f);
      }
      if ((u16 & _3DS_FLG_BIAS) != 0) {
        f = readfloat(fi);
        System.out.format("    bias = %f\n", f);
      }
      if ((u16 & _3DS_FLG_EASE_TO) != 0) {
        f = readfloat(fi);
        System.out.format("    ease to = %f\n", f);
      }
      if ((u16 & _3DS_FLG_EASE_FROM) != 0) {
        f = readfloat(fi);
        System.out.format("    ease from = %f\n", f);
      }
      System.out.format("  };\n");
      if (four) {
        f4[0] = readfloat(fi);
        f4[1] = readfloat(fi);
        f4[2] = readfloat(fi);
        f4[3] = readfloat(fi);
        System.out.format("  angle : %f : axis : %f : %f : %f\n", f4[0], f4[1], f4[2], f4[4]);
      } else {
        f4[0] = readfloat(fi);
        f4[1] = readfloat(fi);
        f4[2] = readfloat(fi);
        System.out.format("  coords : %f : %f : %f\n", f4[0], f4[1], f4[2]);
      }
    }
  }

  public static void main(String args[]) {
    int id, u16;
    int len, u32;
    int pos = 0, skip;
    int cnt;

    float f[] = new float[4];
    int i16[] = new int[4];

    if (args.length != 1) {
      System.out.println("Usage : list3ds filein.3ds");
      System.exit(0);
    }

    try {

      fi = new FileInputStream(args[0]);

      int fs = fi.available();

      System.out.format("list3ds : %s : filesize=%08x\n", args[0], fs);

      while (fi.available() > 0) {
        id = readuint16(fi);
        len = readuint32(fi);
        skip = 0;
        switch (id) {
          case 0x0000:
            System.out.format("%08x:%08x:%04x : EOF Chunk\n", pos, len, id);
            pos += 6;
            return;
          case 0x4d4d:
            System.out.format("%08x:%08x:%04x : Main Chunk\n", pos, len, id);
            pos += 6;
            break;
          case 0x3d3d:
            System.out.format("%08x:%08x:%04x : Mesh Chunk\n", pos, len, id);
            pos += 6;
            break;
          case 0xafff:
            System.out.format("%08x:%08x:%04x : Material Chunk\n", pos, len, id);
            pos += 6;
            break;
          case 0xa000:
            System.out.format("%08x:%08x:%04x : Material : Name Chunk\n", pos, len, id);
            readstr();
            System.out.format("  Name = %s\n", str);
            pos += len;
            break;
          case 0xa200:
            System.out.format("%08x:%08x:%04x : Texture Chunk\n", pos, len, id);
            pos += 6;
            break;
          case 0xa300:
            readstr();
            System.out.format("%08x:%08x:%04x : Texture Filename Chunk\n  FileName = %s\n", pos, len, id, str);
            pos += 6 + str.length() + 1;
            break;
          case 0xb000:
            System.out.format("%08x:%08x:%04x : KeyFrame Chunk\n", pos, len, id);
            pos += 6;
            break;
          case 0xb00a:
            System.out.format("%08x:%08x:%04x : KeyFrame Header Chunk\n", pos, len, id);
            u16 = readuint16(fi);
            System.out.format("  revision = %d\n", u16);
            readstr();
            System.out.format("      name = %s\n", str);
            u32 = readuint32(fi);
            System.out.format("    frames = %d\n", u32);
            pos += 6 + 2 + str.length() + 1 + 4;
            break;
          case 0xb008:
            System.out.format("%08x:%08x:%04x : KeyFrame Segment Chunk\n", pos, len, id);
            u32 = readuint32(fi);
            System.out.format("     start = %d\n", u32);
            u32 = readuint32(fi);
            System.out.format("      stop = %d\n", u32);
            pos += 6 + 4 + 4;
            break;
          case 0xb009:
            System.out.format("%08x:%08x:%04x : KeyFrame Current Time Chunk\n", pos, len, id);
            u32 = readuint32(fi);
            System.out.format("   curtime = %d\n", u32);
            pos += 6 + 4;
            break;
          case 0xb002:
            System.out.format("%08x:%08x:%04x : KeyFrame Object Node Tag Chunk\n", pos, len, id);
            pos += 6;
            break;
          case 0xb010:
            System.out.format("%08x:%08x:%04x : KeyFrame Object Node Tag : Node Header Chunk\n", pos, len, id);
            readstr();
            System.out.format("  name = %s\n", str);
            u16 = readuint16(fi);
            System.out.format("  flgs1 = %d\n", u16);
            u16 = readuint16(fi);
            System.out.format("  flgs2 = %d\n", u16);
            u16 = readuint16(fi);
            System.out.format("  parent id = %d\n", u16);
            pos += len;
            skip = len - (6 + str.length() + 1 + 2 + 2 + 2);
            break;
          case 0xb013:
            System.out.format("%08x:%08x:%04x : KeyFrame Object Node Tag : Pivot Chunk\n", pos, len, id);
            f[0] = readfloat(fi);
            f[1] = readfloat(fi);
            f[2] = readfloat(fi);
            System.out.format("  Pivot = %f : %f : %f\n", f[0], f[1], f[2]);
            pos += 6 + 4 * 3;
            break;
          case 0xb020:
            System.out.format("%08x:%08x:%04x : KeyFrame Object Node Tag : Pos Track Chunk\n", pos, len, id);
            readx(false);
            pos += len;
            break;
          case 0xb021:
            System.out.format("%08x:%08x:%04x : KeyFrame Object Node Tag : Rotate Track Chunk\n", pos, len, id);
            readx(true);
            pos += len;
            break;
          case 0xb022:
            System.out.format("%08x:%08x:%04x : KeyFrame Object Node Tag : Scale Track Chunk\n", pos, len, id);
            readx(false);
            pos += len;
            break;
          case 0xb030:
            System.out.format("%08x:%08x:%04x : KeyFrame Object Node Tag : Node ID Chunk\n", pos, len, id);
            u16 = readuint16(fi);
            System.out.format("  node id = %d\n", u16);
            pos += len;
            skip = len - (6 + 2);
            break;
          case 0x4000:
            readstr();
            System.out.format("%08x:%08x:%04x : Object Chunk\n  Name = %s\n", pos, len, id, str);
            pos += 6 + str.length() + 1;
            break;
          case 0x4100:
            System.out.format("%08x:%08x:%04x : Triangular Chunk\n", pos, len, id);
            pos += 6;
            break;
          case 0x4110:
            u16 = readuint16(fi);
            System.out.format("%08x:%08x:%04x : Vertex Chunk\n  Count = %d\n", pos, len, id, u16);
            pos += len;
            cnt = 0;
            while (u16-- > 0) {
              f[0] = readfloat(fi);
              f[1] = readfloat(fi);
              f[2] = readfloat(fi);
              System.out.format("  %3d : %f : %f : %f\n", cnt++, f[0], f[1], f[2]);
            }
            break;
          case 0x4120:
            u16 = readuint16(fi);
            System.out.format("%08x:%08x:%04x : Points Chunk\n  Count = %d\n", pos, len, id, u16);
            pos += 6 + 2 + (u16 * 2 * 4);
            cnt = 0;
            while (u16-- > 0) {
              i16[0] = readuint16(fi);
              i16[1] = readuint16(fi);
              i16[2] = readuint16(fi);
              i16[3] = readuint16(fi);
              System.out.format("  %3d : %d : %d : %d : flgs = x%x\n", cnt++, i16[0], i16[1], i16[2], i16[3]);
            }
            break;
          case 0x4130:
            System.out.format("%08x:%08x:%04x : Object Material Name Chunk\n", pos, len, id);
            readstr();
            System.out.format("  Name = %s\n", str);
            pos += len;
            skip = len - (6 + str.length() + 1);
            break;
          case 0x4140:
            u16 = readuint16(fi);
            System.out.format("%08x:%08x:%04x : Texture Vertex Chunk\n  Count = %d\n", pos, len, id, u16);
            pos += len;
            cnt = 0;
            while (u16-- > 0) {
              f[0] = readfloat(fi);
              f[1] = readfloat(fi);
              System.out.format("  %3d : %f : %f\n", cnt++, f[0], f[1]);
            }
            break;
          default:
            System.out.format("%08x:%08x:%04x : Unknown Chunk\n", pos, len, id);
            pos += len;
            skip = len - (6);
        }
        if (skip > 0) {
          fi.skip(skip);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static int readuint16(InputStream in) {
    byte data[] = new byte[2];
    try {
      if (in.read(data) != 2) {
        return -1;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
    int ret;
    ret = (int) data[0] & 0xff;
    ret += ((int) data[1] & 0xff) << 8;
    return ret;
  }

  public static int readuint32(InputStream in) {
    byte data[] = new byte[4];
    try {
      if (in.read(data) != 4) {
        return -1;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
    int ret;
    ret = (int) data[0] & 0xff;
    ret += ((int) data[1] & 0xff) << 8;
    ret += ((int) data[2] & 0xff) << 16;
    ret += ((int) data[3] & 0xff) << 24;
    return ret;
  }

  public static float readfloat(InputStream in) {
    int bits = readuint32(in);
    return Float.intBitsToFloat(bits);
  }
}

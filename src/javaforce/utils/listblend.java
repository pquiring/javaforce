package javaforce.utils;

/**
 * Created : Sept 16, 2012
 *
 * Desc : Lists chunks in a .blend file
 *
 * NOTE : This file is largely incomplete. Only main header and sub-headers are
 * processed so far.
 *
 * Usage : java -cp /usr/share/java/javaforce.jar javaforce.utils.listblend
 * filein.blend
 *
 * @author pquiring
 */

import java.io.*;

public class listblend {

  static FileInputStream fi;
  static int bits, endian;
  static final int LITTLE = 1, BIG = 2;

  static String readstr() throws Exception {
    String str = "";
    char ch;
    while (fi.available() > 0) {
      ch = (char) fi.read();
      if (ch == 0) {
        return str;
      }
      str += ch;
    }
    return str;
  }

  static String readstr(int max) throws Exception {
    String str = "";
    char ch;
    while (fi.available() > 0 && max != 0) {
      ch = (char) fi.read();
      if (ch == 0) {
        return str;
      }
      str += ch;
      if (max > 0) {
        max--;
      }
    }
    return str;
  }

  public static void main(String args[]) {
    int id;
    int size, u16, u32;
    int pos = 0, skip, idx, cnt;
    long ptr;

    float f[] = new float[4];
    int i16[] = new int[4];

    if (args.length != 1) {
      System.out.println("Usage : listblend filein.blend");
      System.exit(0);
    }

    try {

      fi = new FileInputStream(args[0]);

      int fs = fi.available();

      System.out.format("listblend : %s : filesize=%08x\n", args[0], fs);

      //read header
      String magic = readstr(7);
      if (!magic.equals("BLENDER")) {
        System.err.println("Error:Not a Blender file, magic=" + magic);
        return;
      }
      char ch = (char) fi.read();
      switch (ch) {
        case '-':
          bits = 64;
          System.out.println("Pointer size:64bits");
          break;
        case '_':
          bits = 32;
          System.out.println("Pointer size:32bits");
          break;
        default:
          System.err.println("Pointer size:unknown");
          return;
      }
      ch = (char) fi.read();
      switch (ch) {
        case 'v':
          endian = LITTLE;
          System.out.println("Little Endianness");
          break;
        case 'V':
          endian = BIG;
          System.err.println("Big Endianness is not supported");
          return;
        default:
          System.err.println("Endianness unknown");
          return;
      }
      String ver = readstr(3);
      System.out.println("Version=" + ver);

      while (fi.available() > 0) {
        //read block header
        id = readuint32(fi);  //32bit ID of block
        size = readuint32(fi);  //size of data block excluding this header
        ptr = readuint64(fi);  //memory address ???
        idx = readuint32(fi);  //SDNA index
        cnt = readuint32(fi);  //SDNA count
        skip = size;
        switch (id) {
          case 0x31414e44:  //DNA1
            System.out.format("%04x:%04x : DNA1\n", id, size);
            break;
          case 0x41544144:  //DATA
            System.out.format("%04x:%04x : DATA\n", id, size);
            break;
          case 0x42444e45:  //ENDB  (end of file) (size is always zero)
            System.out.format("%04x:%04x : ENDB\n", id, size);
            return;
          case 0x424f4c47:  //GLOB
            System.out.format("%04x:%04x : GLOB\n", id, size);
            break;
          case 0x444e4552:  //REND
            System.out.format("%04x:%04x : REND\n", id, size);
            break;
          case 0x54534554:  //TEST
            System.out.format("%04x:%04x : TEST\n", id, size);
            break;
          case 0x00004e53:  //SN..
            System.out.format("%04x:%04x : SN..\n", id, size);
            break;
          case 0x00005242:  //BR..
            System.out.format("%04x:%04x : BR..\n", id, size);
            break;
          case 0x00004d57:  //WM..
            System.out.format("%04x:%04x : WM..\n", id, size);
            break;
          case 0x00004353:  //SC..
            System.out.format("%04x:%04x : SC..\n", id, size);
            break;
          case 0x00004143:  //CA..
            System.out.format("%04x:%04x : CA..\n", id, size);
            break;
          case 0x0000414c:  //LA..
            System.out.format("%04x:%04x : LA..\n", id, size);
            break;
          case 0x00004f57:  //WO..
            System.out.format("%04x:%04x : WO..\n", id, size);
            break;
          case 0x0000424f:  //OB..
            System.out.format("%04x:%04x : OB..\n", id, size);
            break;
          case 0x0000414d:  //MA..
            System.out.format("%04x:%04x : MA..\n", id, size);
            break;
          case 0x00004554:  //UE..
            System.out.format("%04x:%04x : UE..\n", id, size);
            break;
          case 0x0000454d:  //KE..
            System.out.format("%04x:%04x : KE..\n", id, size);
            break;
          default:
            System.out.format("%04x:%04x : Unknown Chunk\n", id, size);
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

  public static long readuint64(InputStream in) {
    byte data[] = new byte[8];
    try {
      if (in.read(data) != 8) {
        return -1;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }
    long ret;
    ret = (long) data[0] & 0xff;
    ret += ((long) data[1] & 0xff) << 8;
    ret += ((long) data[2] & 0xff) << 16;
    ret += ((long) data[3] & 0xff) << 24;
    ret += ((long) data[4] & 0xff) << 32;
    ret += ((long) data[5] & 0xff) << 40;
    ret += ((long) data[6] & 0xff) << 48;
    ret += ((long) data[7] & 0xff) << 56;
    return ret;
  }

  public static float readfloat(InputStream in) {
    int bits = readuint32(in);
    return Float.intBitsToFloat(bits);
  }
}

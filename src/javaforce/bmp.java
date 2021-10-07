package javaforce;

//BMP - M$-Windows Bitmap file format
import java.io.InputStream;
import java.io.OutputStream;

import javaforce.*;
import javaforce.ui.*;

/**
 * Internal class to provide BMP file support.
 *
 * @author Peter Quiring
 */
public class bmp {
  /*/
   struct _bmp_main {   //18 bytes
   char sig[2];       //signature = 'BM'
   uint32 filesize;   //entire file size
   uint8 junk[4];
   uint32 bitoffset;  //offset of bits
   uint32 siz;        //1st element of win/os2  (to detect which one it is)
   };   //main header  (always 1st)

   // * = ignored!
   struct _bmp_win {   //36       //Win 3.0 BMP info block (2nd part if Win 3.0)
   //  uint32 siz;    //40
   uint32 x;
   uint32 y;
   uint16 planes;  //1    //*
   uint16 bpp;
   uint32 comp;    //0=none  1=RLE8  2=RLE4
   uint32 size_image;    //*
   uint32 xpm;           //*
   uint32 ypm;           //*
   uint32 clrused;       // must be 0
   uint32 clrimp;        //*
   };

   struct _bmp_os2 {  //8        //OS/2 info block (2nd part if OS/2)
   //  uint32 siz;    //12
   uint16 x;       //DF/1.2.2 - this was uint32
   uint16 y;
   uint16 planes;  //1    *
   uint16 bpp;
   };

   struct _icon {  //6
     short reserved;
     short type;  //1=icon 2=cursor
     short imageCount;
   }

   struct _icon_entry {  //16
     byte width, height, colorsPalette, reserved;
     short planes, bpp;
     int imageDataSize, dataOffset;
   }

   /*/

  public static int[] load(InputStream in, Dimension size, int index) {
    int a;
    int clrs;
    int bmp_x, bmp_y, bmp_bpp, bmp_bypp;
    int fs;
    int bo;
    int siz;
    int ret[] = null;
    byte m1, m2;
    int type = -1, imagecount;  //icon
    int bpp;
    int planes;

    //read signature
    m1 = (byte)JF.readuint8(in);
    m2 = (byte)JF.readuint8(in);
    if (m1 == 'B' && m2 == 'M') {
      //bitmap
      fs = JF.readuint32(in);
      a = JF.readuint32(in);  //junk
      bo = JF.readuint32(in);
    } else if (m1 == 0 && m2 == 0) {
      //icon / cursor
      type = JF.readuint16(in);
      switch (type) {
        case 0:  //icon
        case 1:  //cursor
          imagecount = JF.readuint16(in);
//          JFLog.log("# images = " + imagecount);
          if (index >= imagecount) return null;
          int index_offset = -1;
          //read icon_entry headers
          for(int i=0;i<imagecount;i++) {
//            JFLog.log("index=" + i + (index == i ? "*" : ""));
            int _x = JF.readuint8(in);
            int _y = JF.readuint8(in);
//            JFLog.log("icon size=" + _x + "x" + _y);
            int _pal = JF.readuint8(in);
            int _res = JF.readuint8(in);
            int _planes = JF.readuint16(in);
            int _bpp = JF.readuint16(in);
//            JFLog.log("bpp=" + _bpp);
            int _size = JF.readuint32(in);
            int _off = JF.readuint32(in);
            if (i == index) {
              index_offset = _off;
            }
          }
          //skip to offset of image data
          int _skip = index_offset - 6 - (16 * imagecount);
          if (_skip > 0) {
            try {in.skip(_skip);} catch (Exception e) {return null;}
          }
          break;
        default:
          return null;
      }
    } else {
      //unknown image type
      return null;
    }

    siz = JF.readuint32(in);

    switch (siz) {
      case 14:    //OS/2
        bmp_x = JF.readuint16(in);
        bmp_y = JF.readuint16(in);
        planes = JF.readuint16(in);  //planes - ignore
        bpp = JF.readuint16(in);
        if (bpp != 24 && bpp != 32) {
          return null;
        }
        break;
      case 40:    //win 3.0
        bmp_x = JF.readuint32(in);
        bmp_y = JF.readuint32(in);
        planes = JF.readuint16(in);  //planes - ignore
        bpp = JF.readuint16(in);
        if (bpp != 24 && bpp != 32) {
          return null;
        }
        if (JF.readuint32(in) != 0) {
          return null;  //no compression supported
        }
        for (a = 0; a < 5; a++) {
          JF.readuint32(in);
        }
        break;
      default:
        JFLog.log("loadBMP() failed! bad image data header");
        return null;
    }

    if (type == 0 || type == 1) {
      //icon / cursor
      bmp_y /= 2;
    }

//    JFLog.log("planes=" + planes);
//    JFLog.log("image size=" + bmp_x + "x" + bmp_y);

    if (bpp == 24) {
      //load 24bit BMP
      int slsiz = (bmp_x * 3 + 3) & 0x7ffffffc;
      int sbufsiz = slsiz * bmp_y;

      byte simg[] = new byte[sbufsiz];
      try {
        if (in.read(simg) != sbufsiz) {
          return null;
        }
      } catch (Exception e) {
        return null;
      }

      ret = new int[bmp_x * bmp_y];

      int sidx = slsiz * (bmp_y - 1);
      int didx = 0;

      for (int y = 0; y < bmp_y; y++) {
        for (int x = 0; x < bmp_x; x++) {
          ret[didx++] = (((int) simg[sidx + x * 3 + 2] & 0xff) << 16 | ((int) simg[sidx + x * 3 + 1] & 0xff) << 8 | (int) simg[sidx + x * 3] & 0xff) | Color.OPAQUE;
        }
        sidx -= slsiz;
      }
    } else {
      //load 32bit BMP
      int slsiz = (bmp_x * 4 + 3) & 0x7ffffffc;
      int sbufsiz = slsiz * bmp_y;

      byte simg[] = new byte[sbufsiz];
      try {
        if (in.read(simg) != sbufsiz) {
          JFLog.log("loadBMP() failed : unable to load image data");
          return null;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }

      ret = new int[bmp_x * bmp_y];

      int sidx = slsiz * (bmp_y - 1);
      int didx = 0;

      for (int y = 0; y < bmp_y; y++) {
        for (int x = 0; x < bmp_x; x++) {
          ret[didx++] = LE.getuint32(simg, sidx + x * 4);
        }
        sidx -= slsiz;
      }
    }
    size.width = bmp_x;
    size.height = bmp_y;
    return ret;
  }

  public static boolean save24(OutputStream out, int buf[], Dimension size, boolean noheader, boolean icon) {
    int a;
    int slsiz = size.width * 3;
    int dlsiz = (slsiz + 3) & 0xfffffffc;
    int padding = dlsiz - slsiz;
    int dbufsiz = dlsiz * size.height;
    int fs;

    if (!noheader) {
      if (!icon) {
        JF.writeuint8(out, 'B');
        JF.writeuint8(out, 'M');
        fs = dbufsiz + 54;
        JF.writeuint32(out, fs);
        a = 0;
        JF.writeuint32(out, a);
        a = 54;  //offset of bits
        JF.writeuint32(out, a);
      } else {
        //_icon
        JF.writeuint16(out, 0);  //reserved
        JF.writeuint16(out, 1);  //1=ICO
        JF.writeuint16(out, 1);  //# of images
        //_icon_entry
        JF.writeuint8(out, size.width);
        JF.writeuint8(out, size.height);
        JF.writeuint8(out, 0);  //colors in palette (not used)
        JF.writeuint8(out, 0);  //reserved
        JF.writeuint16(out, 1);  //planes
        JF.writeuint16(out, 24);  //bpp
        JF.writeuint32(out, 40 + dbufsiz);  //size of image data
        JF.writeuint32(out, 0x16);  //offset to image data
      }
      a = 40;  //size of win header
      JF.writeuint32(out, a);
      JF.writeuint32(out, size.width);
      JF.writeuint32(out, size.height);
      JF.writeuint16(out, 1); //planes
      JF.writeuint16(out, 24);  //bpp
      JF.writeuint32(out, 0);  //comp
      JF.writeuint32(out, dbufsiz);
      for (a = 0; a < 4; a++) {
        JF.writeuint32(out, 0);
      }
    }

    byte dbuf[] = new byte[dbufsiz];
    int sidx = size.width * (size.height - 1);
    int didx = 0;
    //Invert BMP! (Image is stored Upside Down!!!)
    for (int y = 0; y < size.height; y++) {
      for (int x = 0; x < size.width; x++) {
        dbuf[didx++] = (byte) (buf[sidx] & 0xff);
        dbuf[didx++] = (byte) ((buf[sidx] >> 8) & 0xff);
        dbuf[didx++] = (byte) ((buf[sidx] >> 16) & 0xff);
        sidx++;
      }
      didx += padding;
      sidx -= (size.width * 2);
    }

    try {
      out.write(dbuf);
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  public static boolean save32(OutputStream out, int buf[], Dimension size, boolean noheader, boolean icon) {

    int a;
    int slsiz = size.width * 4;
    int dlsiz = (slsiz + 3) & 0xfffffffc;
    int padding = dlsiz - slsiz;
    int dbufsiz = dlsiz * size.height;
    int fs;

    if (!noheader) {
      if (!icon) {
        JF.writeuint8(out, 'B');
        JF.writeuint8(out, 'M');
        fs = dbufsiz + 54;
        JF.writeuint32(out, fs);
        a = 0;
        JF.writeuint32(out, a);
        a = 54;  //offset of bits
        JF.writeuint32(out, a);
      } else {
        //_icon
        JF.writeuint16(out, 0);  //reserved
        JF.writeuint16(out, 1);  //1=ICO
        JF.writeuint16(out, 1);  //# of images
        //_icon_entry
        JF.writeuint8(out, size.width);
        JF.writeuint8(out, size.height);
        JF.writeuint8(out, 0);  //colors in palette (not used)
        JF.writeuint8(out, 0);  //reserved
        JF.writeuint16(out, 1);  //planes
        JF.writeuint16(out, 32);  //bpp
        JF.writeuint32(out, 40 + dbufsiz);  //size of image data
        JF.writeuint32(out, 0x16);  //offset to image data
      }
      a = 40;  //size of win header
      JF.writeuint32(out, a);
      JF.writeuint32(out, size.width);
      JF.writeuint32(out, size.height * (icon ? 2 : 1));
      JF.writeuint16(out, 1); //planes
      JF.writeuint16(out, 32);  //bpp
      JF.writeuint32(out, 0);  //comp
      JF.writeuint32(out, dbufsiz);
      for (a = 0; a < 4; a++) {
        JF.writeuint32(out, 0);
      }
    }

    byte dbuf[] = new byte[dbufsiz];
    int sidx = size.width * (size.height - 1);
    int didx = 0;
    //Invert BMP! (Image is stored Upside Down!!!)
    for (int y = 0; y < size.height; y++) {
      for (int x = 0; x < size.width; x++) {
        dbuf[didx++] = (byte) (buf[sidx] & 0xff);
        dbuf[didx++] = (byte) ((buf[sidx] >> 8) & 0xff);
        dbuf[didx++] = (byte) ((buf[sidx] >> 16) & 0xff);
        dbuf[didx++] = (byte) ((buf[sidx] >> 24) & 0xff);  //alpha
        sidx++;
      }
      didx += padding;
      sidx -= (size.width * 2);
    }

    try {
      out.write(dbuf);
    } catch (Exception e) {
      return false;
    }

    return true;
  }
}

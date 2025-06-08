package javaforce.awt.priv;

/** pgm, ppm images.
 *
 * Only supports 8bit binary gray scale or color images (P5, P6).
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.ui.*;

public class pnm {

  //file format = P{type}\n{width} {height}\nmax_value\n[...pixels...]

  public static final int TYPE_BIT_ASCII = 1;
  public static final int TYPE_GRAY_ASCII = 2;
  public static final int TYPE_COLOR_ASCII = 3;

  public static final int TYPE_BIT_BIN = 4;
  public static final int TYPE_GRAY_BIN = 5;
  public static final int TYPE_COLOR_BIN = 6;

  public static int[] load(InputStream is, javaforce.ui.Dimension size) {
    byte[] data;
    int pos = 0;
    int type = -1;  //image type
    try {
      data = is.readAllBytes();
      if (data == null || data.length < 9) throw new Exception("pgm:header too small");
      if (data[0] != 'P' || data[2] != '\n') throw new Exception("pgm:invalid header");
      switch (data[1]) {
        case '1': type = TYPE_BIT_ASCII; break;
        case '2': type = TYPE_GRAY_ASCII; break;
        case '3': type = TYPE_COLOR_ASCII; break;
        case '4': type = TYPE_BIT_BIN; break;
        case '5': type = TYPE_GRAY_BIN; break;
        case '6': type = TYPE_COLOR_BIN; break;
        default: throw new Exception("pgm:unknown image type");
      }
      if (type == TYPE_BIT_ASCII || type == TYPE_BIT_BIN) throw new Exception("pmg:bit format not supported");
      if (type < TYPE_GRAY_BIN) throw new Exception("pmg:ascii format not supported");
      pos += 3;
      while (data[pos] == '#') {
        //skip comment
        while (data[pos] != '\n') {
          pos++;
        }
        pos++;  //skip \n
      }
      int start = pos;
      int strlen = 0;
      while (data[pos] != ' ') {
        pos++;
        strlen++;
      }
      pos++;  //skip space
      int width = Integer.valueOf(new String(data, start, strlen));
      start = pos;
      strlen = 0;
      while (data[pos] != '\n') {
        pos++;
        strlen++;
      }
      pos++;  //skip \n
      int height = Integer.valueOf(new String(data, start, strlen));
      start = pos;
      strlen = 0;
      while (data[pos] != '\n') {
        pos++;
        strlen++;
      }
      pos++;  //skip \n
      int max_value = Integer.valueOf(new String(data, start, strlen));
      if (max_value != 255) throw new Exception("pgm:max value not supported");
      //now read pixels
      int pxs = width * height;
      int[] px = new int[pxs];
      int off = 0;
      switch (type) {
        case TYPE_GRAY_BIN: {
          for(int a=0;a<pxs;a++) {
            int val = data[pos++];
            val = (val + (val << 8) + (val << 16)) + Color.OPAQUE;  //gray scale
            px[off++] = val;
          }
          break;
        }
        case TYPE_COLOR_BIN: {
          for(int a=0;a<pxs;a++) {
            int r = data[pos++];
            int g = data[pos++];
            int b = data[pos++];
            int val = (b + (g << 8) + (r << 16)) + Color.OPAQUE;
            px[off++] = val;
          }
          break;
        }
      }
      size.width = width;
      size.height = height;
      return px;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  public static boolean save(OutputStream os, int[] px, javaforce.ui.Dimension size, int type) {
    int pxs = size.width * size.height;
    int bytes = pxs;
    if (type == TYPE_COLOR_BIN) bytes *= 3;
    byte[] px8 = new byte[bytes];
    try {
      if (type == TYPE_BIT_ASCII || type == TYPE_BIT_BIN) throw new Exception("pgm:bit format not supported");
      if (type < TYPE_GRAY_BIN) throw new Exception("pgm:ascii format not supported");
      os.write(("P" + type + "\n").getBytes());  //header
      os.write(String.format("%d %d\n", size.width, size.height).getBytes());  //width height
      os.write("255\n".getBytes());  //max value (8 bit)
      switch (type) {
        case TYPE_GRAY_BIN:
          for(int a=0;a<pxs;a++) {
            px8[a] = (byte)(px[a] & Color.MASK_BLUE);
          }
          break;
        case TYPE_COLOR_BIN:
          int off = 0;
          for(int a=0;a<pxs;a++) {
            int val = px[a];
            px8[off++] = (byte)((val & Color.MASK_RED) >> Color.SHIFT_RED);
            px8[off++] = (byte)((val & Color.MASK_GREEN) >> Color.SHIFT_GREEN);
            px8[off++] = (byte)((val & Color.MASK_BLUE) >> Color.SHIFT_BLUE);
          }
          break;
      }
      os.write(px8);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }
}

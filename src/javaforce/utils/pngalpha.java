package javaforce.utils;

import javaforce.*;

public class pngalpha {

  public static void error(String msg) {
    System.out.println(msg);
    System.exit(0);
  }

  public static void main(String args[]) {
    JFImage bm = new JFImage();
    if (args.length < 1) {
      error("Desc : Modifies/Adds Alpha channel in PNG images.\r\nUsage : pngalpha file.png [defaultLevel [[clr=level] ...]]\r\nWhere:\r\n level = 0-255 (default=255)\r\n clr = RGB(hex)\r\n"
              + "level can also equal 'blend' to blend alpha channel\r\n");
    }
    if (!bm.load(args[0])) {
      error("Error : Unable to load file");
    }
    int deflevel = 255;
    if (args.length > 1) {
      try {
        deflevel = Integer.parseInt(args[1]);
      } catch (Exception e1) {
        error("Error : Bad defaultLevel specified\r\n" + e1.toString());
      }
    }
    if (deflevel < 0) deflevel = 0;
    if (deflevel > 255) deflevel = 255;
    bm.fillAlpha(0, 0, bm.getWidth(), bm.getHeight(), deflevel);
    try {
      for (int a = 2; a < args.length; a++) {
        String clrstr = args[a].substring(0, args[a].indexOf('='));
        String lvlstr = args[a].substring(clrstr.length() + 1, args[a].length());
        int clr = Integer.parseInt(clrstr, 16);
        if (lvlstr.equals("blend")) {
          for (int x = 0; x < bm.getWidth(); x++) {
            for (int y = 0; y < bm.getHeight(); y++) {
              if ((bm.getPixel(x, y) & 0xffffff) == clr) {
                blendPixelRect(bm, x, y, 1, 1, 0x00);
                blendPixelRect(bm, x - 1, y - 1, 3, 3, 0x40);
                blendPixelRect(bm, x - 2, y - 2, 5, 5, 0x80);
                blendPixelRect(bm, x - 3, y - 3, 7, 7, 0xc0);
              }
            }
          }
        } else {
          int lvl = Integer.parseInt(lvlstr);
          if (lvl < 0) lvl = 0;
          if (lvl > 255) lvl = 255;
          bm.fillAlphaKeyClr(0, 0, bm.getWidth(), bm.getHeight(), lvl, clr);
        }
      }
    } catch (Exception e2) {
      error("Error : Bad clr=level specified\r\n" + e2.toString());
    }
    bm.save(args[0], "png");
    error("Ok");
  }

  public static void blendPixelRect(JFImage bm, int x1, int y1, int w, int h, int lvl) {
    int clr;
    for (int x = x1; x < x1 + w; x++) {
      for (int y = y1; y < y1 + h; y++) {
        if ((x < 0) || (y < 0) || (x >= bm.getWidth()) || (y >= bm.getHeight())) {
          continue;
        }
        clr = bm.getPixel(x, y);
        clr >>>= 24;
        if (clr > lvl) {
          bm.putAlpha(x, y, lvl);
        }
      }
    }
  }
}

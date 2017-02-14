package javaforce.utils;

import javaforce.*;

public class jimgconvert {
  public static void main(String args[]) {
    if (args.length != 2) {
      System.out.println("Usage : jimgconvert filein fileout");
      System.out.println("Suppports : jpg, png, bmp, ico(output only), icns(output only)");
      return;
    }
    try {
      JFImage img = new JFImage();
      String infmt = args[0].substring(args[0].lastIndexOf(".")+1).toLowerCase();
      if (infmt.equals("jpg")) {
        img.loadJPG(args[0]);
      } else if (infmt.equals("png")) {
        img.loadPNG(args[0]);
      } else if (infmt.equals("bmp")) {
        img.loadBMP(args[0]);
      } else {
        throw new Exception("Unsupported input type:" + infmt);
      }
      String outfmt = args[1].substring(args[1].lastIndexOf(".")+1).toLowerCase();
      if (outfmt.equals("jpg")) {
        img.saveJPG(args[1]);
      } else if (outfmt.equals("png")) {
        img.savePNG(args[1]);
      } else if (outfmt.equals("bmp")) {
        img.saveBMP(args[1]);
      } else if (outfmt.equals("ico")) {
        img.saveICO(args[1]);
      } else if (outfmt.equals("icns")) {
        img.saveICNS(args[1]);
      } else {
        throw new Exception("Unsupported input type:" + infmt);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
};
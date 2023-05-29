package javaforce.utils;

import javaforce.*;
import javaforce.awt.*;

public class ImageConvert {
  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage : ImageConvert filein fileout [{index} | {width height}]");
      System.out.println("Suppports : jpg, png, bmp, ico, icns(output only), svg(input only)");
      System.out.println("    index : ico index (input only) (default = 0)");
      System.out.println("    width height : svg size (input only) (default = 256 256)");
      return;
    }
    try {
      JFImage img = new JFImage();
      String infmt = args[0].substring(args[0].lastIndexOf(".")+1).toLowerCase();
      if (infmt.equals("jpg")) {
        if (!img.loadJPG(args[0])) throw new Exception("Load failed");
      } else if (infmt.equals("png")) {
        if (!img.loadPNG(args[0])) throw new Exception("Load failed");
      } else if (infmt.equals("ico")) {
        int index = 0;
        if (args.length > 2) {
          index = JF.atoi(args[2]);
        }
        if (!img.loadBMP(args[0], index)) throw new Exception("Load failed");
      } else if (infmt.equals("bmp")) {
        if (!img.loadBMP(args[0], 0)) throw new Exception("Load failed");
      } else if (infmt.equals("svg")) {
        int width = 256;
        if (args.length > 2) {
          width = JF.atoi(args[2]);
        }
        int height = 256;
        if (args.length > 3) {
          height = JF.atoi(args[3]);
        }
        if (!img.loadSVG(args[0], width, height)) throw new Exception("Load failed");
      } else {
        throw new Exception("Unsupported input type:" + infmt);
      }
      String outfmt = args[1].substring(args[1].lastIndexOf(".")+1).toLowerCase();
      if (outfmt.equals("jpg")) {
        if (!img.saveJPG(args[1])) throw new Exception("Save failed");
      } else if (outfmt.equals("png")) {
        if (!img.savePNG(args[1])) throw new Exception("Save failed");
      } else if (outfmt.equals("bmp")) {
        if (!img.saveBMP(args[1])) throw new Exception("Save failed");
      } else if (outfmt.equals("ico")) {
        if (!img.saveICO(args[1])) throw new Exception("Save failed");
      } else if (outfmt.equals("icns")) {
        if (!img.saveICNS(args[1])) throw new Exception("Save failed");
      } else {
        throw new Exception("Unsupported output type:" + outfmt);
      }
      JFLog.log("Image Converted");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
};
package javaforce.utils;

import javaforce.*;
import javaforce.awt.*;

public class ImageConvert {
  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage : ImageConvert filein fileout [index=#] [scale=width,height] [size=width,height] [fill=aarrggbb]");
      System.out.println("Suppports : jpg, png, bmp, svg, ico, icns(output only)");
      System.out.println("    index : ico index (input only) (default = 0)");
      System.out.println("    scale : scale image by %");
      System.out.println("    size : set new image size (default svg size = 256,256)");
      System.out.println("    fill : background color (default = ff000000 [opaque black])");
      return;
    }
    int index = 0;
    int width = -1;
    int height = -1;
    float width_percent = 100.0f;
    float height_percent = 100.0f;
    for(int a=2;a<args.length;a++) {
      String arg = args[a];
      int idx = arg.indexOf('=');
      if (idx == -1) {
        System.out.println("Unknown option:" + arg);
        continue;
      }
      String key = arg.substring(0, idx);
      String value = arg.substring(idx + 1);
      switch (key) {
        case "index": {
          index = JF.atoi(value);
          break;
        }
        case "scale": {
          String[] fs = value.split("[,]");
          if (fs.length < 2) {
            System.out.println("Invalid scale:" + value);
            continue;
          }
          String width_str = fs[0];
          String height_str = fs[1];
          width_percent = JF.atof(width_str);
          height_percent = JF.atof(height_str);
          break;
        }
        case "size": {
          String[] fs = value.split("[,]");
          if (fs.length < 2) {
            System.out.println("Invalid scale:" + value);
            continue;
          }
          String width_str = fs[0];
          String height_str = fs[1];
          width = JF.atoi(width_str);
          height = JF.atoi(height_str);
          break;
        }
        case "fill": {
          int fill = Integer.parseUnsignedInt(value, 16);
          JFImage.setDefaultColor(fill);
          break;
        }
        default: System.out.println("Unknown option:" + key);
      }
    }
    try {
      JFLog.log("ImageConvert:" + args[0] + " to " + args[1]);
      JFImage img = new JFImage();
      String infmt = args[0].substring(args[0].lastIndexOf(".")+1).toLowerCase();
      JFLog.log("infmt=" + infmt);
      if (infmt.equals("jpg")) {
        if (!img.loadJPG(args[0])) throw new Exception("Load failed");
      } else if (infmt.equals("png")) {
        if (!img.loadPNG(args[0])) throw new Exception("Load failed");
      } else if (infmt.equals("ico")) {
        if (!img.loadBMP(args[0], index)) throw new Exception("Load failed");
      } else if (infmt.equals("bmp")) {
        if (!img.loadBMP(args[0], 0)) throw new Exception("Load failed");
      } else if (infmt.equals("svg")) {
        if (width == -1 || height == -1) {
          width = 256;
          height = 256;
        }
        if (!img.loadSVG(args[0], width, height)) throw new Exception("Load failed");
        width = -1;
        height = -1;
        width_percent = 100.0f;
        height_percent = 100.0f;
      } else {
        throw new Exception("Unsupported input type:" + infmt);
      }
      if (width != -1 || height != -1) {
        int org_width = img.getWidth();
        int org_height = img.getHeight();
        int new_width = width;
        int new_height = height;
        JFLog.log(String.format("Scaling:from=%dx%d,to=%dx%d", org_width, org_height, new_width, new_height));
        JFImage new_img = new JFImage(new_width, new_height);
        new_img.fill(0, 0, new_width, new_height, 0, true);
        new_img.putJFImageScaleSmooth(img, 0, 0, new_width, new_height);
        img = new_img;
      }
      if (width_percent != 100 || height_percent != 100) {
        int org_width = img.getWidth();
        int org_height = img.getHeight();
        int new_width = (int)((org_width * width_percent) / 100.0f);
        int new_height = (int)((org_height * height_percent) / 100.0f);
        JFLog.log(String.format("Scaling:from=%dx%d,to=%dx%d", org_width, org_height, new_width, new_height));
        JFImage new_img = new JFImage(new_width, new_height);
        new_img.fill(0, 0, new_width, new_height, 0, true);
        new_img.putJFImageScaleSmooth(img, 0, 0, new_width, new_height);
        img = new_img;
      }
      String outfmt = args[1].substring(args[1].lastIndexOf(".")+1).toLowerCase();
      JFLog.log("outfmt=" + outfmt);
      if (outfmt.equals("jpg")) {
        if (!img.saveJPG(args[1])) throw new Exception("Save failed");
      } else if (outfmt.equals("png")) {
        if (!img.savePNG(args[1])) throw new Exception("Save failed");
      } else if (outfmt.equals("bmp")) {
        if (!img.saveBMP(args[1])) throw new Exception("Save failed");
      } else if (outfmt.equals("svg")) {
        if (!img.saveSVG(args[1])) throw new Exception("Save failed");
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
}

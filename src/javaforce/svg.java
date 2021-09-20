package javaforce;

//SVG - scaled vector graphics
import javaforce.*;
import java.awt.Dimension;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Internal class to provide SVG file support.
 *
 * Created : Feb 27, 2012
 *
 * @author Peter Quiring
 */
public class svg {

  public static int[] load(InputStream in, Dimension size) {
    return null;  //not implemented yet - batik???
  }

  public static boolean save(OutputStream out, byte png_data[], Dimension size) {
    int w = size.width;
    int h = size.height;
    try {
      out.write("<?xml version='1.0' encoding='UTF-8' standalone='no'?>".getBytes());
      out.write(("<svg xmlns:xlink='http://www.w3.org/1999/xlink' version='1.1' width='" + w + "' height='" + h + "'>").getBytes());
      out.write(("<image width='" + w + "' height='" + h + "' x='0' y='0' ").getBytes());
      out.write("xlink:href='data:image/png;base64,".getBytes());
      out.write(Base64.encode(png_data, 76));
      out.write("'/>".getBytes());
      out.write("</svg>".getBytes());
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }
};

package javaforce.awt.priv;

import java.io.*;
import java.awt.*;
import java.awt.image.*;

import javaforce.*;

import com.github.weisj.jsvg.*;
import com.github.weisj.jsvg.attributes.*;
import com.github.weisj.jsvg.parser.*;

/**
 * Internal class to provide SVG file support.
 *
 * Created : Feb 27, 2012
 *
 * @author Peter Quiring
 */

public class svg {

  public static int[] load(InputStream in, javaforce.ui.Dimension size) {
    SVGLoader loader = new SVGLoader();
    SVGDocument svgDocument = loader.load(in, null, LoaderContext.builder().parserProvider(new DefaultParserProvider()).build());

    BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
    svgDocument.render((Component)null, (Graphics2D)image.getGraphics(), new ViewBox(0, 0, size.width, size.height));

    int[] px = new int[size.width * size.height];
    image.getRGB(0, 0, size.width, size.height, px, 0, size.width);

    return px;
  }

  public static boolean save(OutputStream out, byte[] png_data, javaforce.ui.Dimension size) {
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

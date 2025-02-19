package javaforce.awt.priv;

import java.io.*;
import java.awt.*;
import java.awt.image.*;

import javaforce.*;

//batik
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.*;
import org.apache.batik.anim.dom.*;
import org.apache.batik.util.*;

//jsvg
import com.github.weisj.jsvg.*;
import com.github.weisj.jsvg.parser.*;

/**
 * Internal class to provide SVG file support.
 *
 * Created : Feb 27, 2012
 *
 * @author Peter Quiring
 */

public class svg {

  public static boolean use_batik = false;

  private static class batik extends ImageTranscoder {
    public BufferedImage image;
    public javaforce.ui.Dimension size;

    public batik(javaforce.ui.Dimension sz) {
      size = sz;
    }
    public BufferedImage createImage(int width, int height) {
      if (width != size.width) {
        size.width = width;
      }
      if (height != size.height) {
        size.height = height;
      }
      if (image == null) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      }
      return image;
    }
    public void writeImage(BufferedImage bi, TranscoderOutput out) throws TranscoderException {
      image = bi;
    }
  }

  private static int[] load_batik(InputStream in, javaforce.ui.Dimension size) {
    TranscodingHints transcoderHints = new TranscodingHints();
//    transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
    transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
    transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
    transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
    transcoderHints.put(ImageTranscoder.KEY_WIDTH, (float)size.width);
    transcoderHints.put(ImageTranscoder.KEY_HEIGHT, (float)size.height);

    try {
      batik svg_obj = new batik(size);
      TranscoderInput input = new TranscoderInput(in);
      ImageTranscoder transcoder = svg_obj;
      transcoder.setTranscodingHints(transcoderHints);
      transcoder.transcode(input, null);
      int[] px = new int[size.width * size.height];
      svg_obj.image.getRGB(0, 0, size.width, size.height, px, 0, size.width);
      return px;
    }
    catch (TranscoderException ex) {
      ex.printStackTrace();
      return null;
    }
  }

  private static int[] load_jsvg(InputStream in, javaforce.ui.Dimension size) {
    SVGLoader loader = new SVGLoader();
    SVGDocument svgDocument = loader.load(in, null, LoaderContext.builder().parserProvider(new DefaultParserProvider()).build());

    BufferedImage image = new BufferedImage((int) size.width,(int) size.height, BufferedImage.TYPE_INT_ARGB);
    svgDocument.render(null, (Graphics2D)image.getGraphics());

    int[] px = new int[size.width * size.height];
    image.getRGB(0, 0, size.width, size.height, px, 0, size.width);

    return px;
  }

  public static int[] load(InputStream in, javaforce.ui.Dimension size) {
    if (use_batik) {
      return load_batik(in, size);
    } else {
      return load_jsvg(in, size);
    }
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

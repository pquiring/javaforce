package javaforce.awt;

/** Convert PDF to Images.
 *
 * @author pquiring
 */

import java.io.*;
import java.awt.image.*;

import javaforce.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PDF {

  /** DPI used when loading pdf files.  Default = 300. */
  public static int dpi = 300;

  /** Loads a PDF converting each page into a JFImage.
   *
   * @param pdf = filename
   */
  public static JFImage[] load(String pdf) {

    try {
      PDDocument document = Loader.loadPDF(new File(pdf));
      PDFRenderer pdfRenderer = new PDFRenderer(document);
      int pages = document.getNumberOfPages();
      JFImage[] imgs = new JFImage[pages];
      for(int pageIndex = 0; pageIndex < pages; pageIndex++) {
        BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, dpi);
        imgs[pageIndex] = new JFImage();
        imgs[pageIndex].setBufferedImage(image);
      }
      return imgs;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  /** Loads a PDF converting each page into an image that is stored in folder with name.
   *
   * @param pdf = filename
   * @param folder = folder where image files are stored (one per page)
   * @param name = name to use (without extension) (may include %d for page number else "-%d" is appended)
   * @param format = image format to save as (see JFImage.FORMAT_...)
   */
  public static int load(String pdf, String folder, String name, String format) {
    int count = 0;
    int index = 1;
    if (name.indexOf('%') == -1) {
      name = name + "-%d";
    }
    try {
      PDDocument document = Loader.loadPDF(new File(pdf));
      PDFRenderer pdfRenderer = new PDFRenderer(document);
      int pages = document.getNumberOfPages();
      for(int pageIndex = 0; pageIndex < pages; pageIndex++) {
        BufferedImage image = pdfRenderer.renderImageWithDPI(pageIndex, dpi);
        JFImage img = new JFImage();
        img.setBufferedImage(image);
        String fullname = String.format("%s/%s.%s", folder, String.format(name, index), format);
        if (img.save(fullname, format)) {
          count++;
        }
        index++;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    return count;
  }
}

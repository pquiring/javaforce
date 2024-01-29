package javaforce.awt;

import java.awt.*;
import java.awt.image.*;
import java.awt.font.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.imageio.*;
import javax.imageio.stream.*;

import javaforce.*;
import javaforce.awt.priv.*;

/**
 * Encapsules BufferedImage to provide more functions.
 * Implements javax.swing.Icon so it can also be used with JLabel.
 *
 * @author Peter Quiring
 */

public class JFImage extends JComponent implements Icon {

  private BufferedImage bi;
  private Graphics2D g2d;
  private int[] buffer;
  private ResizeOperation resizeOperation = ResizeOperation.CLEAR;
  private int imageType;  //BufferedImage.TYPE_INT_...

  public enum ResizeOperation {
    CLEAR, //reset image
    CHOP,  //copy/chop image
    SCALE  //scale image
  };

  public JFImage() {
    imageType = BufferedImage.TYPE_INT_ARGB;
  }

  public JFImage(boolean alpha) {
    if (alpha)
      imageType = BufferedImage.TYPE_INT_ARGB;
    else
      imageType = BufferedImage.TYPE_INT_RGB;
  }

  public JFImage(int x, int y) {
    imageType = BufferedImage.TYPE_INT_ARGB;
    setImageSize(x, y);
  }

  public JFImage(int x, int y, boolean alpha) {
    if (alpha)
      imageType = BufferedImage.TYPE_INT_ARGB;
    else
      imageType = BufferedImage.TYPE_INT_RGB;
    setImageSize(x, y);
  }

  private void init(boolean clear) {
    g2d = bi.createGraphics();
    buffer = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
    if (clear) fill(0,0,getWidth(),getHeight(), 0);  //fill with black opaque (the default varies by platform)
  }

  private void initImage(int x, int y) {
//    System.out.println("JFImage.initImage:" + x + "," + y);
    bi = new BufferedImage(x, y, imageType);
    init(true);
    setPreferredSize(new Dimension(x, y));
  }

  public void setImageSize(int x, int y) {
    setSize(x, y);
    initImage(x, y);
  }

  public void setBounds(int x, int y, int w, int h) {
    //usually called from LayoutManager / ScrollPane
//    System.out.println("JFImage.setBounds:" + x + "," + y + "," + w + "," + h);
    super.setBounds(x, y, w, h);
    if (bi != null) {
      BufferedImage oldbi = bi;
      int oldw = getWidth();
      int oldh = getHeight();
      Composite org;
      switch (resizeOperation) {
        case CLEAR:
          initImage(w, h);
          break;
        case CHOP:
          initImage(w, h);
          org = g2d.getComposite();
          g2d.setComposite(AlphaComposite.Src);
          g2d.drawImage(oldbi, 0, 0, null);
          g2d.setComposite(org);
          break;
        case SCALE:
          initImage(w, h);
          org = g2d.getComposite();
          g2d.setComposite(AlphaComposite.Src);
          g2d.drawImage(oldbi, 0, 0, w, h, 0, 0, oldw, oldh, null);
          g2d.setComposite(org);
          break;
      }
    } else {
      initImage(w, h);
    }
  }

  public void setSize(int x, int y) {
//    System.out.println("JFImage.setSize:" + x + "," + y);
    super.setSize(x, y);  //just calls setBounds(getLocation(),x,y);
  }

  public void setSize(Dimension d) {
//    System.out.println("JFImage.setSize:" + d);
    super.setSize(d);
  }

  /**
   * Controls how this image is resized by layout managers.
   */
  public void setResizeOperation(ResizeOperation ro) {
    resizeOperation = ro;
  }

  public Image getImage() {
    return bi;
  }

  public BufferedImage getBufferedImage() {
    return bi;
  }

  /**
   * Replaced BufferedImage.
   * Must be TYPE_INT_ARGB or TYPE_INT_RGB type.
   * @param bi - new BufferedImage
   */
  public void setBufferedImage(BufferedImage bi) {
    int newType = bi.getType();
    if (newType != BufferedImage.TYPE_INT_ARGB || newType != BufferedImage.TYPE_INT_RGB) {
      int x = bi.getWidth();
      int y = bi.getHeight();
      int[] px = bi.getRGB(0, 0, x, y, null, 0, x);  //tmp may not be int[] buffer
      setImageSize(x, y);
      putPixels(px, 0, 0, x, y, 0);
      return;
    }
    this.bi = bi;
    imageType = bi.getType();
    init(false);
  }

  public Graphics getGraphics() {
    return g2d;
  }

  public Graphics2D getGraphics2D() {
    return g2d;
  }

  /** Paint this image onto Graphics. */
  public void paint(Graphics g) {
//System.out.println("paint");
    if (bi == null) {
      return;
    }
    g.drawImage(getImage(), 0, 0, null);
  }

  public int getWidth() {
    if (bi == null) {
      return -1;
    }
    return bi.getWidth();
  }

  public int getHeight() {
    if (bi == null) {
      return -1;
    }
    return bi.getHeight();
  }

  public boolean load(InputStream in) {
    //use ImageIO (the returned Image is type TYPE_CUSTOM which is slow so copy pixels to an TYPE_INT_ARGB)
    BufferedImage tmp;
    try {
      tmp = ImageIO.read(in);
      in.close();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    if (tmp == null) {
      return false;
    }
    int x = tmp.getWidth();
    int y = tmp.getHeight();
    int[] px = tmp.getRGB(0, 0, x, y, null, 0, x);  //tmp may not be int[] buffer
    setImageSize(x, y);
    putPixels(px, 0, 0, x, y, 0);
    return true;
  }

  public boolean save(OutputStream out, String fmt) {
    boolean ret;
    if (fmt.equals("jpg")) ret = saveJPG(out);  //Must convert image type
    else if (fmt.equals("ico")) ret = saveICO(out);  //ImageIO doesn't support ICO
    else if (fmt.equals("icns")) ret = saveICNS(out);  //ImageIO doesn't support ICNS
    else if (fmt.equals("bmp")) ret = saveBMP(out);  //ImageIO doesn't support BMP (except Windows)
    else {
      try {
        ret = ImageIO.write(bi, fmt, out);
      } catch (Exception e) {
        JFLog.log(e);
        ret = false;
      }
    }
    try {out.close();} catch (Exception e) {}
    return ret;
  }

  public boolean load(String filename) {
    try {
      return load(new FileInputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean save(String filename, String fmt) {
    try {
      return save(new FileOutputStream(filename), fmt);
    } catch (Exception e) {
      return false;
    }
  }

  public static JFImage[] loadmulti(InputStream is) {
    try {
      ImageInputStream stream = ImageIO.createImageInputStream(is);
      if (stream == null) throw new Exception("reader not found");
      ImageReader reader = ImageIO.getImageReaders(stream).next();
      reader.setInput(stream);
      int cnt = reader.getNumImages(true);
      JFImage[] imgs = new JFImage[cnt];
      for(int i = 0;i<cnt;i++) {
        BufferedImage img = reader.read(i);
        imgs[i] = new JFImage();
        imgs[i].setBufferedImage(img);
      }
      return imgs;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static boolean savemulti(OutputStream os, JFImage[] imgs, String format) {
    try {
      ImageOutputStream stream = ImageIO.createImageOutputStream(os);
      if (stream == null) throw new Exception("writer not found");
      ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
      writer.setOutput(stream);
      int cnt = imgs.length;
      for(int i = 0;i<cnt;i++) {
        if (!writer.canInsertImage(i)) {
          throw new Exception("writer does not support layers, idx=" + i);
        }
        writer.writeInsert(i, new IIOImage(imgs[i].getBufferedImage(), null, null), null);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean loadJPG(InputStream in) {
    return load(in);
  }

  public boolean saveJPG(OutputStream out) {
    //Do NOT use a BufferedImage of type TYPE_INT_ARGB
    //On Windows it creates a 4 channel image which is mistake in CMYK format (which gives it a pinkish tint)
    //On Linux it throws an exception!!!
    //See http://stackoverflow.com/questions/3432388/imageio-not-able-to-write-a-jpeg-file
    //So convert BufferedImage to type TYPE_3BYTE_BGR instead and it works on all platforms
    try {
      int w = bi.getWidth();
      int h = bi.getHeight();
      int[] px = new int[w * h];
      BufferedImage tmp = new BufferedImage(bi.getWidth(),bi.getHeight(),BufferedImage.TYPE_3BYTE_BGR);
      bi.getRGB(0,0,w,h,px,0,w);
      tmp.setRGB(0,0,w,h,px,0,w);
      return ImageIO.write(tmp, "jpg", out);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public boolean loadJPG(String filename) {
    try {
      return loadJPG(new FileInputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean saveJPG(String filename) {
    try {
      return saveJPG(new FileOutputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean loadPNG(InputStream in) {
    return load(in);
  }

  public boolean savePNG(OutputStream out) {
    return save(out, "png");
  }

  public boolean loadPNG(String filename) {
    try {
      return loadPNG(new FileInputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean savePNG(String filename) {
    try {
      return savePNG(new FileOutputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  //NOTE : Not all JREs support "bmp" so I use custom code

  public boolean loadBMP(String filename, int index) {
    try {
      return loadBMP(new FileInputStream(filename), index);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean saveBMP(String filename) {
    try {
      return saveBMP(new FileOutputStream(filename));
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public boolean loadBMP(InputStream in, int index) {
    int[] buf;
    javaforce.ui.Dimension size = new javaforce.ui.Dimension(0, 0);
    buf = bmp.load(in, size, index);
    try {in.close();} catch (Exception e) {e.printStackTrace();}
    if (buf == null) {
      JFLog.log("loadBMP() failed! null returned");
      return false;
    }
    if (size.width == 0 || size.height == 0) {
      JFLog.log("loadBMP() failed! image size zero");
      return false;
    }
    setImageSize(size.width, size.height);
    putPixels(buf, 0, 0, size.width, size.height, 0);
    return true;
  }

  public boolean saveBMP(OutputStream out) {
    int[] pixels;
    pixels = getPixels(0, 0, getWidth(), getHeight());
    javaforce.ui.Dimension size = new javaforce.ui.Dimension(getWidth(), getHeight());
    return bmp.save24(out, pixels, size, false, false);
  }

  //save ICO (no loading supported)

  public boolean saveICO(String filename) {
    try {
      return saveICO(new FileOutputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean saveICO(OutputStream out) {
    int[] pixels;
    pixels = getPixels(0, 0, getWidth(), getHeight());
    javaforce.ui.Dimension size = new javaforce.ui.Dimension(getWidth(), getHeight());
    return bmp.save32(out, pixels, size, false, true);
  }

  /** save icns (Mac icon) */
  public boolean saveICNS(String filename) {
    try {
      return saveICNS(new FileOutputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  /** save icns (Mac icon) */
  public boolean saveICNS(OutputStream out) {
    //see http://en.wikipedia.org/wiki/Apple_Icon_Image_format
    //use one of the PNG formats
    byte[] header = new byte[4*4];
    //scale image to one of supported sizes
    int w = getWidth();
    int h = getHeight();
    if (w > h) h = w;
    if (h > w) w = h;
    int newSize = 16;
    String OSType = "icp4";
/*
    if (w > 512) {
      newSize = 1024;  //also retina 512x512@2x ???
      OSType = "ic10";
    }
    else
*/
    if (w > 256) {
      newSize = 512;
      OSType = "ic09";
    }
    else if (w > 128) {
      newSize = 256;
      OSType = "ic08";
    }
    else if (w > 64) {
      newSize = 128;
      OSType = "ic07";
    }
    else if (w > 32) {
      newSize = 64;
      OSType = "icp6";
    }
    else if (w > 16) {
      newSize = 32;
      OSType = "icp5";
    }
    if (w != newSize || h != newSize) {
//      System.out.println("Scaling icns to:" + newSize + "x" + newSize);
      setResizeOperation(ResizeOperation.SCALE);
      setSize(newSize, newSize);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    savePNG(baos);
//    System.out.println("png.size=" + baos.size());
    System.arraycopy("icns".getBytes(), 0, header, 0, 4);  //file header "icns"
    BE.setuint32(header, 4, 4*4 + baos.size());  //file size
    System.arraycopy(OSType.getBytes(), 0, header, 8, 4);  //image type
    BE.setuint32(header, 12, 4*2 + baos.size());  //image size

    try {
      out.write(header);
      out.write(baos.toByteArray());
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  public boolean loadSVG(String filename, int width, int height) {
    try {
      return loadSVG(new FileInputStream(filename), width, height);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean saveSVG(String filename) {
    try {
      return saveSVG(new FileOutputStream(filename));
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean loadSVG(InputStream in, int width, int height) {
    int[] buf;
    javaforce.ui.Dimension size = new javaforce.ui.Dimension(width, height);
    buf = svg.load(in, size);
    try {in.close();} catch (Exception e) {}
    if (buf == null) {
      JFLog.log("error:buf==null");
      return false;
    }
    if (size.width == 0 || size.height == 0) {
      JFLog.log("error:width,height=" + size.width + "," + size.height);
      return false;
    }
    setImageSize(size.width, size.height);
    putPixels(buf, 0, 0, size.width, size.height, 0);
    return true;
  }

  public boolean saveSVG(OutputStream out) {
    ByteArrayOutputStream png_data = new ByteArrayOutputStream();
    savePNG(png_data);
    javaforce.ui.Dimension size = new javaforce.ui.Dimension(getWidth(), getHeight());
    boolean ret = svg.save(out, png_data.toByteArray(), size);
    try {out.close();} catch (Exception e) {}
    return ret;
  }

  public boolean loadXPM(String filename) {
    try {
      return loadXPM(new FileInputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean loadXPM(InputStream in) {
    int[] buf;
    javaforce.ui.Dimension size = new javaforce.ui.Dimension(0, 0);
    buf = new xpm().load(in, size);
    if (buf == null) {
      return false;
    }
    if (size.width == 0 || size.height == 0) {
      return false;
    }
    setImageSize(size.width, size.height);
    putPixels(buf, 0, 0, size.width, size.height, 0);
    return true;
  }

  //tiff support (requires twelvemonkeys)
  public boolean loadTIFF(InputStream in) {
    return load(in);
  }

  public boolean saveTIFF(OutputStream out) {
    return save(out, "tiff");
  }

  public boolean loadTIFF(String filename) {
    try {
      return loadTIFF(new FileInputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean saveTIFF(String filename) {
    try {
      return saveTIFF(new FileOutputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  //support for layered tiffs
  public static JFImage[] loadTIFFs(InputStream in) {
    return loadmulti(in);
  }

  public static boolean saveTIFFs(OutputStream out, JFImage[] imgs) {
    return savemulti(out, imgs, "tiff");
  }

  /** Puts pixels . */
  public void putJFImage(JFImage img, int x, int y) {
    int[] px = img.getPixels();
    putPixels(px, x, y, img.getWidth(), img.getHeight(), 0);
  }

  /** Puts pixels unless src pixel == keyclr */
  public void putJFImageKeyClr(JFImage img, int x, int y, int keyClr) {
    int[] px = img.getPixels();
    putPixelsKeyClr(px, x, y, img.getWidth(), img.getHeight(), 0, keyClr);
  }

  /** Puts pixels blending using img alpha (dest alpha is ignored) */
  public void putJFImageBlend(JFImage img, int x, int y, boolean keepAlpha) {
    int[] px = img.getPixels(0, 0, img.getWidth(), img.getHeight());
    putPixelsBlend(px, x, y, img.getWidth(), img.getHeight(), 0, img.getWidth(), keepAlpha);
  }

  /** Puts pixels scaling image to fit */
  public void putJFImageScale(JFImage img, int x, int y, int width, int height) {
    g2d.drawImage(img.getImage(), x, y, x+width-1, y+height-1, 0, 0, img.getWidth()-1, img.getHeight()-1, null);
  }

  /** Returns an area of this image as a new JFImage. */
  public JFImage getJFImage(int x, int y, int w, int h) {
    JFImage ret = new JFImage(w,h);
    int[] px = getPixels(x,y,w,h);
    ret.putPixels(px, 0, 0, w, h, 0);
    return ret;
  }

  public static final int ALPHA_MASK = 0xff000000;  //Alpha
  public static final int OPAQUE = 0xff000000;
  public static final int TRANSPARENT = 0x00000000;
  public static final int RED_MASK = 0x00ff0000;
  public static final int GREEN_MASK = 0x0000ff00;
  public static final int BLUE_MASK = 0x000000ff;
  public static final int RGB_MASK = 0x00ffffff;

  /** Draws one pixel using r,g,b values (alpha assumed opaque) */
  public void putPixel(int x, int y, int r, int g, int b) {
    buffer[y * getWidth() + x] = OPAQUE | r << 16 | g << 8 | b;
  }

  /** Draws one pixel using rgb value (alpha assumed opaque) */
  public void putPixel(int x, int y, int c) {
    buffer[y * getWidth() + x] = OPAQUE | c;
  }

  /** Returns rgb value of pixel at x,y */
  public int getPixel(int x, int y) {
    return buffer[y * getWidth() + x] & RGB_MASK;
  }

  /** Returns alpha value of pixel at x,y */
  public int getAlpha(int x, int y) {
    return (buffer[y * getWidth() + x] & ALPHA_MASK) >>> 24;
  }

  /** Sets alpha value at x,y */
  public void putAlpha(int x, int y, int lvl) {
    int offset = y * getWidth() + x;
    int px = buffer[offset] & RGB_MASK;
    px |= (lvl << 24);
    buffer[offset] = px;
  }

  /** Puts pixels */
  public void putPixels(int[] src, int x, int y, int w, int h, int srcOffset) {
    putPixels(src, x, y, w, h, srcOffset, w);
  }

  /** Puts pixels (supports padding at end of each scan line) */
  public void putPixels(int[] src, int x, int y, int w, int h, int srcOffset, int srcScanSize) {
    //do clipping
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      srcOffset += y * srcScanSize;
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return;
      }
      srcOffset += x;
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return;
      }
    }
    int dstOffset = y * bw + x;
    if (w == bw && srcScanSize == w) {
      System.arraycopy(src, srcOffset, buffer, dstOffset, w * h);
    } else {
      for(int i=0;i<h;i++) {
        System.arraycopy(src, srcOffset, buffer, dstOffset, w);
        srcOffset += srcScanSize;
        dstOffset += bw;
      }
    }
  }

  /** Put Pixels unless src pixel == keyclr */
  public void putPixelsKeyClr(int[] px, int x, int y, int w, int h, int offset, int keyclr) {
    //do clipping
    int scansize = w;
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      offset += y * scansize;
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return;
      }
      offset += x;
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return;
      }
    }
    int dst = y * bw + x;
    for(int i=0;i<h;i++) {
      for(int j=0;j<w;j++) {
        if ((px[offset + j] & RGB_MASK) != keyclr) {
          buffer[dst + j] = px[offset + j];
        }
      }
      offset += scansize;
      dst += bw;
    }
  }

  /** Puts pixels blending using src alpha (dest alpha is ignored).
   * if keepAlpha is true then dest alpha is preserved.
   * if keepAlpha is false then src alpha is copied to dest.
   */
  public void putPixelsBlend(int[] px, int x, int y, int w, int h, int srcOffset, int srcScansize, boolean keepAlpha) {
    //do clipping
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      srcOffset += y * srcScansize;
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return;
      }
      srcOffset += x;
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return;
      }
    }
    int dst = y * bw + x;
    int sp, slvl, dp, dlvl, sa, da;
    for(int i=0;i<h;i++) {
      for(int j=0;j<w;j++) {
        sp = px[srcOffset + j];
        sa = sp & ALPHA_MASK;
        sp &= RGB_MASK;
        slvl = sa >>> 24;
        if (slvl > 0) {
          slvl++;
        }
        dlvl = 0x100 - slvl;
        dp = buffer[dst + j];
        da = dp & ALPHA_MASK;
        dp &= RGB_MASK;
        buffer[dst + j] = (keepAlpha ? da : sa)
          + ((((dp & RED_MASK) * dlvl) >> 8) & RED_MASK)
          + ((((dp & GREEN_MASK) * dlvl) >> 8) & GREEN_MASK)
          + ((((dp & BLUE_MASK) * dlvl) >> 8) & BLUE_MASK)
          + ((((sp & RED_MASK) * slvl) >> 8) & RED_MASK)
          + ((((sp & GREEN_MASK) * slvl) >> 8) & GREEN_MASK)
          + ((((sp & BLUE_MASK) * slvl) >> 8) & BLUE_MASK);
      }
      srcOffset += srcScansize;
      dst += bw;
    }
  }

  /** Puts pixels blending using src alpha (dest alpha is ignored) unless src pixel == keyclr.
   * if keepAlpha is true then dest alpha is preserved.
   * if keepAlpha is false then src alpha is copied to dest.
   */
  public void putPixelsBlendKeyClr(int[] px, int x, int y, int w, int h, int srcOffset, int srcScansize, boolean keepAlpha, int keyclr) {
    //do clipping
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      srcOffset += y * srcScansize;
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return;
      }
      srcOffset += x;
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return;
      }
    }
    int dst = y * bw + x;
    int sp, slvl, dp, dlvl, sa, da;
    for(int i=0;i<h;i++) {
      for(int j=0;j<w;j++) {
        sp = px[srcOffset + j];
        sa = sp & ALPHA_MASK;
        sp &= RGB_MASK;
        if (sp == keyclr) continue;
        slvl = sa >>> 24;
        if (slvl > 0) {
          slvl++;
        }
        dlvl = 0x100 - slvl;
        dp = buffer[dst + j];
        da = dp & ALPHA_MASK;
        dp &= RGB_MASK;
        buffer[dst + j] = (keepAlpha ? da : sa)
          + ((((dp & RED_MASK) * dlvl) >> 8) & RED_MASK)
          + ((((dp & GREEN_MASK) * dlvl) >> 8) & GREEN_MASK)
          + ((((dp & BLUE_MASK) * dlvl) >> 8) & BLUE_MASK)
          + ((((sp & RED_MASK) * slvl) >> 8) & RED_MASK)
          + ((((sp & GREEN_MASK) * slvl) >> 8) & GREEN_MASK)
          + ((((sp & BLUE_MASK) * slvl) >> 8) & BLUE_MASK);
      }
      srcOffset += srcScansize;
      dst += bw;
    }
  }

  /** Gets a rectangle of pixels (including alpha) */
  public int[] getPixels(int x, int y, int w, int h) {
    int[] dst = new int[w * h];
    int dstScanSize = w;
    int dstOffset = 0;
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return null;
      }
      dstOffset += y * dstScanSize;
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return null;
      }
      dstOffset += x;
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return null;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return null;
      }
    }
    int srcOffset = y * bw + x;
    if (w == bw && dstScanSize == w) {
      System.arraycopy(buffer, srcOffset, dst, dstOffset, w * h);
    } else {
      for(int i=0;i<h;i++) {
        System.arraycopy(buffer, srcOffset, dst, dstOffset, w);
        dstOffset += dstScanSize;
        srcOffset += bw;
      }
    }
    return dst;
  }

  /** Returns a copy of the buffer */
  public int[] getPixels() {
    int[] px = new int[buffer.length];
    System.arraycopy(buffer, 0, px, 0, buffer.length);
    return px;
  }

  /** Returns the data buffer */
  public int[] getBuffer() {
    return buffer;
  }

  /** Clears the image to black with opaque alpha. */
  public void clear() {
    fill(0,0,getWidth(),getHeight(),0);
  }

  /** Fills a rectangle with clr (assumes OPAQUE alpha) */
  public void fill(int x, int y, int w, int h, int clr) {
    fill(x,y,w,h,clr,false);
  }

  /** Fills a rectangle with clr */
  public void fill(int x, int y, int w, int h, int clr, boolean hasAlpha) {
    if (!hasAlpha) clr |= OPAQUE;
    int scansize = w;
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return;
      }
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return;
      }
    }
    int dst = y * bw + x;
    if (w == bw && scansize == w) {
      Arrays.fill(buffer, dst, dst + w * h, clr);
    } else {
      for(int i=0;i<h;i++) {
        Arrays.fill(buffer, dst, dst + w, clr);
        dst += bw;
      }
    }
  }

  /** Fills the alpha channel with lvl (doesn't touch rgb values) */
  public void fillAlpha(int x, int y, int w, int h, int lvl) {
    lvl <<= 24;
    int scansize = w;
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return;
      }
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return;
      }
    }
    int dst = y * bw + x;
    if (w == bw && scansize == w) {
      int cnt = w * h;
      for(int i=0;i<cnt;i++) {
        buffer[dst] = (buffer[dst] & RGB_MASK) | lvl;
        dst++;
      }
    } else {
      int odst;
      for(int i=0;i<h;i++) {
        odst = dst;
        for(int j=0;j<w;j++) {
          buffer[dst] = (buffer[dst] & RGB_MASK) | lvl;
          dst++;
        }
        dst = odst + bw;
      }
    }
  }

  /** Fills alpha channel with lvl ONLY where dest pixel == keyClr */
  public void fillAlphaKeyClr(int x, int y, int w, int h, int lvl, int keyClr) {
    lvl <<= 24;
    //do clipping
    int scansize = w;
    int bw = getWidth();
    int bh = getHeight();
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      y = 0;
    }
    if (x < 0) {
      x *= -1;
      w -= x;
      if (w <= 0) {
        return;
      }
      x = 0;
    }
    if (x + w > bw) {
      w = bw - x;
      if (w <= 0) {
        return;
      }
    }
    if (y + h > bh) {
      h = bh - y;
      if (h <= 0) {
        return;
      }
    }
    int dst = y * bw + x;
    if (w == bw && scansize == w) {
      int cnt = w * h;
      for(int i=0;i<cnt;i++) {
        if ((buffer[dst] & RGB_MASK) == keyClr) {
          buffer[dst] &= RGB_MASK;
          buffer[dst] |= lvl;
        }
        dst++;
      }
    } else {
      for(int i=0;i<h;i++) {
        int odst = dst;
        for(int j=0;j<w;j++) {
          if ((buffer[dst] & RGB_MASK) == keyClr) {
            buffer[dst] &= RGB_MASK;
            buffer[dst] |= lvl;
          }
          dst++;
        }
        dst = odst + bw;
      }
    }
  }

  /** Draw a rectangle outline in clr (assumes opaque). */
  public void box(int x, int y, int w, int h, int clr) {
    clr |= OPAQUE;
    g2d.setColor(new Color(clr));
    g2d.drawRect(x,y,w,h);
  }

  /** Draw a horizontal line */
  public void hline(int x1, int x2, int y, int clr) {
    line(x1, y, x2, y, clr);
  }

  /** Draw a vertical line */
  public void vline(int x, int y1, int y2, int clr) {
    line(x, y1, x, y2, clr);
  }

  /** Draw a line */
  public void line(int x1, int y1, int x2, int y2, int clr) {
    clr |= OPAQUE;
    g2d.setColor(new Color(clr));
    g2d.drawLine(x1, y1, x2, y2);
  }

  /** Draw a circle/oval */
  public void oval(int x, int y, int w, int h, int clr) {
    clr |= OPAQUE;
    g2d.setColor(new Color(clr));
    g2d.drawOval(x, y, w, h);
  }

  /** Draw an arc */
  public void arc(int x,int y, int w, int h, int startAngle, int arcAngle, int clr) {
    clr |= OPAQUE;
    g2d.setColor(new Color(clr));
    g2d.drawArc(x, y, w, h, startAngle, arcAngle);
  }

  /** Draw a filled circle/oval */
  public void fillOval(int x, int y, int w, int h, int clr) {
    clr |= OPAQUE;
    g2d.setColor(new Color(clr));
    g2d.fillOval(x, y, w, h);
  }

  /** Draw a filled arc */
  public void fillArc(int x,int y, int w, int h, int startAngle, int arcAngle, int clr) {
    clr |= OPAQUE;
    g2d.setColor(new Color(clr));
    g2d.fillArc(x, y, w, h, startAngle, arcAngle);
  }

  /** Returns font metrics for a given font/text.
   *
   * @param fnt - Font
   * @param txt - sample text
   * @return
   *   [0] = width required to draw text in font
   *   [1] = ascent
   *   [2] = descent
   *  NOTE : total height required = ascent + descent
   *  Note : add ascent to y coordinate of print() to start printing below your coordinates
   *  For a static version see JF.java
   */
  public int[] getFontMetrics(Font fnt, String txt) {
    FontRenderContext frc = g2d.getFontRenderContext();
    TextLayout tl = new TextLayout(txt, fnt, frc);
    int[] ret = new int[3];
    ret[0] = (int) tl.getBounds().getWidth();
    ret[1] = (int) tl.getAscent();
    ret[2] = (int) tl.getDescent();
    return ret;
  }

  /** Draws text in font at x,y in clr */
  public void print(Font fnt, int x, int y, String txt, int clr) {
    g2d.setColor(new Color(clr));
    g2d.setFont(fnt);
    g2d.drawString(txt, x, y);  //x,y = baseline of font
  }

  /** Sets the font in the Graphics object (if this JFImage is used as a JComponent) */
  public void setFont(Font font) {
    //this is a little confusing because setFont() is inherited from JComponent which is what you probably don't want
    getGraphics().setFont(font);
  }
//interface Icon

  public int getIconHeight() {
    return getHeight();
  }

  public int getIconWidth() {
    return getWidth();
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    g.drawImage(getImage(), x, y, null);
  }

  /** Uses java.awt.Robot to capture and return a screen image. */
  public static JFImage createScreenCapture() {
    return createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
  }

  /** Uses java.awt.Robot to capture and return a screen image. */
  public static JFImage createScreenCapture(GraphicsDevice device) {
    DisplayMode mode = device.getDisplayMode();
    return createScreenCapture(device, device.getDefaultConfiguration().getBounds());
  }

  /** Uses java.awt.Robot to capture and return a screen image of specified rectangle. */
  public static JFImage createScreenCapture(Rectangle rect) {
    return createScreenCapture(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice(), rect);
  }

  /** Uses java.awt.Robot to capture and return a screen image of specified rectangle. */
  public static JFImage createScreenCapture(GraphicsDevice device, Rectangle rect) {
    try {
      JFImage img = new JFImage();
      BufferedImage cap = new Robot(device).createScreenCapture(rect);
      int width = cap.getWidth();
      int height = cap.getHeight();
      if (cap.getType() == BufferedImage.TYPE_INT_ARGB) {
        img.bi = cap;
        img.init(false);
        return img;
      } else if (cap.getType() == BufferedImage.TYPE_INT_RGB) {
        img.setSize(width, height);
        int[] src = ((DataBufferInt) cap.getRaster().getDataBuffer()).getData();
        int cnt = width * height;
        for(int a=0;a<cnt;a++) {
          img.buffer[a] = src[a] | 0xff000000;
        }
        return img;
      } else {
        //slow performance
        JFLog.log("JFImage.createScreenCapture() unknown type=" + cap.getType());
        int[] px = cap.getRGB(0, 0, width, height, null, 0, width);
        img.setSize(width, height);
        img.putPixels(px, 0, 0, width, height, 0);
        return img;
      }
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Gets color Layer (RGB) : pixels AND bits */
  public int[] getLayer(int bits) {
    int[] px = getPixels();
    for(int a=0;a<px.length;a++) {
      px[a] &= bits;
      px[a] |= OPAQUE;
    }
    return px;
  }

  /** Puts color Layer (RGB) : (pixels OR bits) */
  public void putLayer(int[] px, int bits) {
    if (px.length != buffer.length) return;
    int mask = 0xffffffff - bits;
    for(int a=0;a<px.length;a++) {
      buffer[a] &= mask;
      buffer[a] |= (px[a] & RGB_MASK);
    }
  }

  /** Gets alpha Layer as grey scale */
  public int[] getAlphaLayer() {
    int[] px = getPixels();
    int p1, p2;
    for(int a=0;a<px.length;a++) {
      p1 = px[a] & 0xff000000;
      p1 >>>= 8;
      p2 = OPAQUE | p1 | (p1 >> 8) | (p1 >> 16);
      px[a] = p2;
    }
    return px;
  }

  /** Puts alpha Layer from grey scale */
  public void putAlphaLayer(int[] px) {
    //just use red color
    if (px.length != buffer.length) return;
    for(int a=0;a<px.length;a++) {
      buffer[a] &= RGB_MASK;
      buffer[a] |= (px[a] & 0x00ff0000) << 8;
    }
  }
};

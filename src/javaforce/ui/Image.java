package javaforce.ui;

/** Image
 *
 * Contains an image that can be load()ed or save()ed.
 * Image is also a Component that can be embedded in Windows.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Image extends FontComponent {
  private int[] buffer;
  private int lineStyle = LineStyle.SOLID;
  private int resizeOperation = ResizeOperation.CLEAR;

  public Image() {
    setSize(1, 1);
  }

  public Image(int width, int height) {
    setSize(width, height);
  }

  private native int[] nloadPNG(byte[] data, int[] dim);

  public boolean loadPNG(InputStream is) {
    byte[] data;
    try {
      data = is.readAllBytes();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    int[] dim = new int[2];
    buffer = nloadPNG(data, dim);
    if (buffer != null) {
      super.setSize(dim[0], dim[1]);
    }
    return buffer != null;
  }

  public boolean loadPNG(String fn) {
    try {
      FileInputStream fis = new FileInputStream(fn);
      boolean loaded = loadPNG(fis);
      fis.close();
      return loaded;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private native byte[] nsavePNG(int[] pixels, int width, int height);

  public boolean savePNG(OutputStream os) {
    byte[] data = nsavePNG(buffer, size.width, size.height);
    if (data == null) return false;
    try {
      os.write(data);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean savePNG(String fn) {
    try {
      FileOutputStream fis = new FileOutputStream(fn);
      boolean saved = savePNG(fis);
      fis.close();
      return saved;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private native int[] nloadJPG(byte[] data, int[] dim);

  public boolean loadJPG(InputStream is) {
    byte[] data;
    try {
      data = is.readAllBytes();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    int[] dim = new int[2];
    buffer = nloadJPG(data, dim);
    return buffer != null;
  }

  public boolean loadJPG(String fn) {
    try {
      FileInputStream fis = new FileInputStream(fn);
      boolean loaded = loadJPG(fis);
      fis.close();
      return loaded;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private native byte[] nsaveJPG(int[] pixels, int width, int height, int quality);

  /** JPEG Image Quality when saving. (0-100) Default = 90 */
  public int jpeg_quality = 90;

  public boolean saveJPG(OutputStream os) {
    byte[] data = nsaveJPG(buffer, size.width, size.height, jpeg_quality);
    if (data == null) return false;
    try {
      os.write(data);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean saveJPG(String fn) {
    try {
      FileOutputStream fis = new FileOutputStream(fn);
      boolean saved = saveJPG(fis);
      fis.close();
      return saved;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

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
    Dimension size = new Dimension(0, 0);
    buffer = bmp.load(in, size, index);
    try {in.close();} catch (Exception e) {e.printStackTrace();}
    if (buffer == null) {
      JFLog.log("loadBMP() failed! null returned");
      return false;
    }
    if (size.width == 0 || size.height == 0) {
      JFLog.log("loadBMP() failed! image size zero");
      return false;
    }
    super.setSize(size.width, size.height);
    return true;
  }

  public boolean saveBMP(OutputStream out) {
    Dimension size = new Dimension(getWidth(), getHeight());
    return bmp.save24(out, buffer, size, false, false);
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
    Dimension size = new Dimension(getWidth(), getHeight());
    return bmp.save32(out, buffer, size, false, true);
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
    byte header[] = new byte[4*4];
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
//      setResizeOperation(ResizeOperation.SCALE);
//      setSize(newSize, newSize);
      JFLog.log("Image.saveICNS() failed:Image must be square");
      return false;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    savePNG(baos);
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

  public boolean loadSVG(String filename) {
    try {
      return loadSVG(new FileInputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean saveSVG(String filename) {
    try {
      return saveSVG(new FileOutputStream(filename));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean loadSVG(InputStream in) {
    Dimension size = new Dimension(0, 0);
    buffer = svg.load(in, size);
    try {in.close();} catch (Exception e) {}
    if (buffer == null) {
      return false;
    }
    if (size.width == 0 || size.height == 0) {
      return false;
    }
    super.setSize(size.width, size.height);
    return true;
  }

  public boolean saveSVG(OutputStream out) {
    ByteArrayOutputStream png_data = new ByteArrayOutputStream();
    savePNG(png_data);
    Dimension size = new Dimension(getWidth(), getHeight());
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
    Dimension size = new Dimension(0, 0);
    buffer = new xpm().load(in, size);
    if (buffer == null) {
      return false;
    }
    if (size.width == 0 || size.height == 0) {
      return false;
    }
    setSize(size.width, size.height);
    return true;
  }

  public void setSize(int width, int height) {
    int[] org_buffer = buffer;
    int org_w = getWidth();
    int org_h = getHeight();
    super.setSize(width, height);
    buffer = new int[width * height];
    if (org_buffer != null) {
      switch (resizeOperation) {
        case ResizeOperation.CLEAR:
          break;
        case ResizeOperation.CHOP:
          putPixels(org_buffer, 0, 0, org_w, org_h, 0, org_w);
          break;
        case ResizeOperation.SCALE:
          //TODO
          break;
      }
    }
  }

  public int getResizeOperation() {
    return resizeOperation;
  }

  public void setResizeOperation(int op) {
    resizeOperation = op;
  }

  public int getPixel(int x, int y) {
    if (x < 0 || x >= getWidth()) return 0;
    if (y < 0 || y >= getHeight()) return 0;
    return buffer[y * getWidth() + x] & Color.MASK_RGB;
  }

  public void putPixel(int x, int y, int c) {
    if (x < 0 || x >= getWidth()) return;
    if (y < 0 || y >= getHeight()) return;
    buffer[y * getWidth() + x] = Color.OPAQUE | c;
  }

  public int getPixelA(int x, int y) {
    if (x < 0 || x >= getWidth()) return 0;
    if (y < 0 || y >= getHeight()) return 0;
    return buffer[y * getWidth() + x];
  }

  public void putPixelA(int x, int y, int c) {
    if (x < 0 || x >= getWidth()) return;
    if (y < 0 || y >= getHeight()) return;
    buffer[y * getWidth() + x] = c;
  }

  /** Returns a copy of the pixels buffer. */
  public int[] getPixels() {
    int[] px = new int[buffer.length];
    System.arraycopy(buffer, 0, px, 0, buffer.length);
    return px;
  }

  /** Returns the pixels buffer. */
  public int[] getBuffer() {
    return buffer;
  }

  /** Puts pixels. */
  public void putPixels(int[] src, int x, int y, int w, int h, int srcOffset) {
    putPixels(src, x, y, w, h, srcOffset, w);
  }

  /** Puts pixels (supports padding at end of each scan line) */
  public void putPixels(int[] src, int x, int y, int w, int h, int srcOffset, int srcWidth) {
    //do clipping
    int bw = getWidth();
    int bh = getHeight();
    if (w <= 0) {
      return;
    }
    if (h <= 0) {
      return;
    }
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      srcOffset += y * srcWidth;
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
    if (w == bw && srcWidth == w) {
      System.arraycopy(src, srcOffset, buffer, dstOffset, w * h);
    } else {
      for(int i=0;i<h;i++) {
        System.arraycopy(src, srcOffset, buffer, dstOffset, w);
        srcOffset += srcWidth;
        dstOffset += bw;
      }
    }
  }

  /** Put Pixels unless src pixel == keyclr */
  public void putPixelsKeyClr(int[] px, int x, int y, int w, int h, int offset, int keyclr) {
    //do clipping
    int srcWidth = w;
    int bw = getWidth();
    int bh = getHeight();
    if (w <= 0) {
      return;
    }
    if (h <= 0) {
      return;
    }
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      offset += y * srcWidth;
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
        if ((px[offset + j] & Color.MASK_RGB) != keyclr) {
          buffer[dst + j] = px[offset + j];
        }
      }
      offset += srcWidth;
      dst += bw;
    }
  }

  /** Puts pixels blending using src alpha (dest alpha is ignored).
   * if keepAlpha is true then dest alpha is preserved.
   * if keepAlpha is false then src alpha is copied to dest.
   */
  public void putPixelsBlend(int[] px, int x, int y, int w, int h, int srcOffset, int srcWidth, boolean keepAlpha) {
    //do clipping
    int bw = getWidth();
    int bh = getHeight();
    if (w <= 0) {
      return;
    }
    if (h <= 0) {
      return;
    }
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      srcOffset += y * srcWidth;
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
        sa = sp & Color.MASK_ALPHA;
        sp &= Color.MASK_RGB;
        slvl = sa >>> 24;
        if (slvl > 0) {
          slvl++;
        }
        dlvl = 0x100 - slvl;
        dp = buffer[dst + j];
        da = dp & Color.MASK_ALPHA;
        dp &= Color.MASK_RGB;
        buffer[dst + j] = (keepAlpha ? da : sa)
          + ((((dp & Color.MASK_RED) * dlvl) >> 8) & Color.MASK_RED)
          + ((((dp & Color.MASK_GREEN) * dlvl) >> 8) & Color.MASK_GREEN)
          + ((((dp & Color.MASK_BLUE) * dlvl) >> 8) & Color.MASK_BLUE)
          + ((((sp & Color.MASK_RED) * slvl) >> 8) & Color.MASK_RED)
          + ((((sp & Color.MASK_GREEN) * slvl) >> 8) & Color.MASK_GREEN)
          + ((((sp & Color.MASK_BLUE) * slvl) >> 8) & Color.MASK_BLUE);
      }
      srcOffset += srcWidth;
      dst += bw;
    }
  }

  /** Puts pixels blending using src alpha (dest alpha is ignored) unless src pixel == keyclr.
   * if keepAlpha is true then dest alpha is preserved.
   * if keepAlpha is false then src alpha is copied to dest.
   */
  public void putPixelsBlendKeyClr(int[] px, int x, int y, int w, int h, int srcOffset, int srcWidth, boolean keepAlpha, int keyclr) {
    //do clipping
    int bw = getWidth();
    int bh = getHeight();
    if (w <= 0) {
      return;
    }
    if (h <= 0) {
      return;
    }
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      srcOffset += y * srcWidth;
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
        sa = sp & Color.MASK_ALPHA;
        sp &= Color.MASK_RGB;
        if (sp == keyclr) continue;
        slvl = sa >>> 24;
        if (slvl > 0) {
          slvl++;
        }
        dlvl = 0x100 - slvl;
        dp = buffer[dst + j];
        da = dp & Color.MASK_ALPHA;
        dp &= Color.MASK_RGB;
        buffer[dst + j] = (keepAlpha ? da : sa)
          + ((((dp & Color.MASK_RED) * dlvl) >> 8) & Color.MASK_RED)
          + ((((dp & Color.MASK_GREEN) * dlvl) >> 8) & Color.MASK_GREEN)
          + ((((dp & Color.MASK_BLUE) * dlvl) >> 8) & Color.MASK_BLUE)
          + ((((sp & Color.MASK_RED) * slvl) >> 8) & Color.MASK_RED)
          + ((((sp & Color.MASK_GREEN) * slvl) >> 8) & Color.MASK_GREEN)
          + ((((sp & Color.MASK_BLUE) * slvl) >> 8) & Color.MASK_BLUE);
      }
      srcOffset += srcWidth;
      dst += bw;
    }
  }

  /** Puts pixels using src as a stencil. */
  public void putPixelsStencil(int[] px, int x, int y, int w, int h, int srcOffset, int srcWidth, boolean keepAlpha, int clr) {
    //do clipping
    int bw = getWidth();
    int bh = getHeight();
    if (w <= 0) {
      return;
    }
    if (h <= 0) {
      return;
    }
    if (y < 0) {
      y *= -1;
      h -= y;
      if (h <= 0) {
        return;
      }
      srcOffset += y * srcWidth;
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
        sa = sp & Color.MASK_ALPHA;
        sp = clr;
        slvl = sa >>> 24;
        if (slvl > 0) {
          slvl++;
        }
        dlvl = 0x100 - slvl;
        dp = buffer[dst + j];
        da = dp & Color.MASK_ALPHA;
        dp &= Color.MASK_RGB;
        buffer[dst + j] = (keepAlpha ? da : sa)
          + ((((dp & Color.MASK_RED) * dlvl) >> 8) & Color.MASK_RED)
          + ((((dp & Color.MASK_GREEN) * dlvl) >> 8) & Color.MASK_GREEN)
          + ((((dp & Color.MASK_BLUE) * dlvl) >> 8) & Color.MASK_BLUE)
          + ((((sp & Color.MASK_RED) * slvl) >> 8) & Color.MASK_RED)
          + ((((sp & Color.MASK_GREEN) * slvl) >> 8) & Color.MASK_GREEN)
          + ((((sp & Color.MASK_BLUE) * slvl) >> 8) & Color.MASK_BLUE);
      }
      srcOffset += srcWidth;
      dst += bw;
    }
  }

  /** Fills a rectangle with clr */
  public void fill(int x, int y, int w, int h, int clr) {
    fill(x, y, w, h, clr, false);
  }

  /** Fills a rectangle with clr */
  public void fill(int x, int y, int w, int h, int clr, boolean hasAlpha) {
    if (!hasAlpha) clr |= Color.OPAQUE;
    int srcWidth = w;
    int bw = getWidth();
    int bh = getHeight();
    if (w <= 0) {
      return;
    }
    if (h <= 0) {
      return;
    }
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
    if (w == bw && srcWidth == w) {
      Arrays.fill(buffer, dst, dst + w * h, clr);
    } else {
      for(int i=0;i<h;i++) {
        Arrays.fill(buffer, dst, dst + w, clr);
        dst += bw;
      }
    }
  }

  /** Fills a rectangle with current fore color. */
  public void fill(int x, int y, int w, int h) {
    fill(x, y, w, h, getForeColor().getColor(), false);
  }

  public void drawText(int x, int y, String txt) {
    getFont().drawText(x, y, txt, this, getForeColor().getColor());
  }

  public void drawPixel(int x, int y) {
    switch (lineStyle) {
      case LineStyle.DASH:
        if ((x + y) % 2 == 0) return;
        break;
    }
    putPixel(x, y, getForeColor().getColor());
  }

  public void setLineStyle(int style) {
    lineStyle = style;
  }

  public void drawLine(int x1, int y1, int x2, int y2) {
    int rise = y2 - y1;
    int run = x2 - x1;
    if (rise == 0 && run == 0) {
      //dot
      drawPixel(x1, y1);
      return;
    }
    if (rise == 0) {
      //vertical line
      int dx = (x2 > x1) ? 1 : -1;
      while (x1 != x2) {
        drawPixel(x1, y1);
        x1 += dx;
      }
      drawPixel(x1, y1);
      return;
    }
    if (run == 0) {
      //horizontal line
      int dy = (y2 > y1) ? 1 : -1;
      while (y1 != y2) {
        drawPixel(x1, y1);
        y1 += dy;
      }
      drawPixel(x1, y1);
      return;
    }
    if (Math.abs(rise) == Math.abs(run)) {
      //true diag line
      int dx = (x2 > x1) ? 1 : -1;
      int dy = (y2 > y1) ? 1 : -1;
      while (y1 != y2) {
        drawPixel(x1, y1);
        x1 += dx;
        y1 += dy;
      }
      drawPixel(x1, y1);
      return;
    }
    //odd diag line
    if (rise > run) {
      //up/down diag line
      float fx = x1;
      float dx = (float)run / (float)rise;
      int dy = (y2 > y1) ? 1 : -1;
      while (y1 != y2) {
        drawPixel((int)fx, y1);
        fx += dx;
        y1 += dy;
      }
      drawPixel((int)fx, y1);
    } else {
      //left/right diag line
      float fy = y1;
      float dy = (float)rise / (float)run;
      int dx = (x2 > x1) ? 1 : -1;
      while (x1 != x2) {
        drawPixel(x1, (int)fy);
        x1 += dx;
        fy += dy;
      }
      drawPixel(x1, (int)fy);
    }
  }

  public void drawBox(int x, int y, int width, int height) {
    int x1 = x;
    int y1 = y;
    int x2 = x + width - 1;
    int y2 = y + height - 1;
    drawLine(x1, y1, x2, y1);
    drawLine(x2, y1, x2, y2);
    drawLine(x2, y2, x1, y2);
    drawLine(x1, y2, x1, y1);
  }

  public void drawImage(Image img, int x, int y) {
    putPixels(img.getBuffer(), x, y, img.getWidth(), img.getHeight(), 0);
  }

  public void drawImageKeyClr(Image img, int x, int y, int keyclr) {
    putPixelsKeyClr(img.getBuffer(), x, y, img.getWidth(), img.getHeight(), 0, keyclr);
  }

  public void drawImageBlend(Image img, int x, int y, boolean keepAlpha) {
    putPixelsBlend(img.getBuffer(), x, y, img.getWidth(), img.getHeight(), 0, img.getWidth(), keepAlpha);
  }
}

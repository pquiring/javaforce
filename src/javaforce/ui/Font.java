package javaforce.ui;

/** Font
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.gl.*;
import javaforce.jni.*;

public class Font {

  public static boolean debug = false;

  private static final int SystemFontSize = 20;

  private int width, height;

  private int[] fontinfo;
  private int[] coords;
  private int[] glyphinfo;
  private byte[] px;
  private int[] codes;  //U16 code points

  private int avg_ascent, avg_descent;
  private int linegap;
  private int max_ascent, max_descent;

  private int size = 512;

  private native static int loadFont(byte[] font, int ptSize, int[] fontinfo, int[] coords, int[] adv, int[] cps, byte[] pixels, int px, int py);
  public boolean load(byte[] font, int ptSize) {
    fontinfo = new int[5];  //avg ascent(baseline), avg descent, linegap, max-ascent, max-descent
    coords = new int[256 * 4];  //x1,y1,x2,y2
    glyphinfo = new int[256 * 2];  //baseline, advance
    codes = new int[256];  //code points
    for(int a=0;a<256;a++) {
      codes[a] = ASCII8.convert(a);
    }
    width = size;
    height = size;
    px = new byte[width*height];  //pixel data
    boolean loaded = loadFont(font, ptSize, fontinfo, coords, glyphinfo, codes, px, width, height) == 256;
    if (loaded) {
      avg_ascent = fontinfo[0];
      avg_descent = fontinfo[1];
      linegap = fontinfo[2];
      max_ascent = fontinfo[3];
      max_descent = fontinfo[4];
    }
    return loaded;
  }
  public boolean load(String name, int ptSize) {
    try {
      FileInputStream fis = new FileInputStream(name);
      byte[] data = fis.readAllBytes();
      fis.close();
      return load(data, ptSize);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  private Texture tex;
  public Texture getTexture() {
    if (tex == null) {
      Image img = new Image(size, size);
      int pos = 0;
      int a, rgb = 0xffffff;
      int[] buffer = img.getBuffer();
      for(int y=0;y<height;y++) {
        for(int x=0;x<width;x++) {
          a = px[pos];
          buffer[pos] = (a << 24) + rgb;
          pos++;
        }
      }
      if (debug) {
        for(int cp=0;cp<256;cp++) {
          int x1 = coords[cp * 4 + 0];
          if (x1 < 0) continue;
          int y1 = coords[cp * 4 + 1];
          if (y1 < 0) continue;
          int x2 = coords[cp * 4 + 2];
          if (x2 < 0) continue;
          int y2 = coords[cp * 4 + 3];
          if (y2 < 0) continue;
          //draw dots on corners
          img.putPixel(x1, y1, 0xff0000);
          img.putPixel(x1, y2, 0x770000);
          img.putPixel(x2, y1, 0x0000ff);
          img.putPixel(x2, y2, 0x000077);
          int baseline = glyphinfo[cp * 2 + 0];
          int advance = glyphinfo[cp * 2 + 1];
          if (y1 + baseline < 0) continue;
          if (y1 + baseline >= size) continue;
          //draw dots on baseline
          img.putPixel(x1, y1 - baseline, 0x00ff00);
          img.putPixel(x2, y1 - baseline, 0x00ff00);
        }
        img.savePNG("font.png");
      }
      tex = new Texture(0);
      tex.setImage(img);
      tex.load();
    }
    return tex;
  }

  public Image getImage() {
    return getTexture().getImage();
  }

  public void bind() {
    getTexture().bind();
  }

  private static Font systemFont;

  public static Font getSystemFont() {
    if (systemFont == null) {
      systemFont = new Font();
      try {
        InputStream is = systemFont.getClass().getResourceAsStream("/javaforce/ui/system.ttf");
        if (is == null) throw new Exception("Resource not found:/javaforce/ui/system.ttf");
        systemFont.load(is.readAllBytes(), SystemFontSize);
        is.close();
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }
    return systemFont;
  }

  public int getMaxAscent() {
    return max_ascent;
  }

  public int getMaxDescent() {
    return max_descent;
  }

  public int getAdvance(char ch) {
    int cp = ASCII8.convert(ch);
    if (cp >= 256) return 0;
    return glyphinfo[cp * 2 + 1];
  }

  /** Get font ascent.  Note : The ascent is usually negative relative to baseline. */
  public int getAscent(char ch) {
    int cp = ASCII8.convert(ch);
    if (cp >= 256) return 0;
    return glyphinfo[cp * 2 + 0];
  }

  public void drawChar(int x, int y, char ch, Image image, int clr) {
    int cp = ASCII8.convert(ch);
    if (cp >= 256) return;
    int ascent = getAscent(ch);
    y += ascent;
    int x1 = coords[cp * 4 + 0];
    int y1 = coords[cp * 4 + 1];
    int x2 = coords[cp * 4 + 2];
    int y2 = coords[cp * 4 + 3];
    int w = x2 - x1 + 1;
    int h = y2 - y1 + 1;
    if (debug) {
      JFLog.log("drawChar:" + x + "," + y + ":" + ch + "@" + x1 + "," + y1 + ":" + x2 + "," + y2 + ":" + ascent);
    }
    Image fontImage = getImage();
    int fontWidth = fontImage.getWidth();
    image.putPixelsStencil(fontImage.getBuffer(), x, y, w, h, y1 * fontWidth + x1, fontWidth, true, clr);
  }

  public void drawText(int x, int y, String txt, Image image, int clr) {
    char[] ca = txt.toCharArray();
    for(char ch : ca) {
      drawChar(x,y,ch,image,clr);
      x += getAdvance(ch);
    }
  }

  public FontMetrics getMetrics(String txt) {
    FontMetrics m = new FontMetrics();
    char[] ca = txt.toCharArray();
    m.ascent = max_ascent;
    m.descent = max_descent;
    for(int pos=0;pos<ca.length;pos++) {
      char ch = ca[pos];
      boolean found = false;
      for(int code=0;code<codes.length;code++) {
        if (codes[code] == ch) {
          //ascent = glythinfo[code * 2 + 0];  //ascent (baseline)
          m.advance += glyphinfo[code * 2 + 1];  //advance
          found = true;
          break;
        }
      }
      if (!found) {
        ch = 0;
        int code = 0;
        //ascent = glythinfo[code * 2 + 0];  //ascent (baseline)
        m.advance += glyphinfo[code * 2 + 1];  //advance
      }
    }
    return m;
  }

  /** Font test. */
  public static void main(String[] args) {
    JFNative.load();
    Font font = Font.getSystemFont();
    System.out.println("Font test complete!");
  }
}

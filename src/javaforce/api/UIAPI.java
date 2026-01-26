package javaforce.api;

/** UI Native API
 *
 * @author pquiring
 */

import javaforce.jni.*;
import javaforce.ui.*;

public interface UIAPI {
  public static UIAPI getInstance() {
    return new UIJNI();
  }

  public static final int KEY_TYPED = 1;
  public static final int KEY_PRESS = 2;
  public static final int KEY_RELEASE = 3;
  public static final int MOUSE_MOVE = 4;
  public static final int MOUSE_DOWN = 5;
  public static final int MOUSE_UP = 6;
  public static final int MOUSE_SCROLL = 7;
  public static final int WIN_RESIZE = 8;
  public static final int WIN_CLOSING = 9;

  public static final int STYLE_VISIBLE = 1;
  public static final int STYLE_RESIZABLE = 2;
  public static final int STYLE_TITLEBAR = 4;
  public static final int STYLE_FULLSCREEN = 8;

  //Font
  public int loadFont(byte[] font, int ptSize, int[] fontinfo, int[] coords, int[] adv, int[] cps, byte[] pixels, int px, int py);

  //Image
  public int[] loadPNG(byte[] data, int[] dim);
  public byte[] savePNG(int[] pixels, int width, int height);
  public int[] loadJPG(byte[] data, int[] dim);
  public byte[] saveJPG(int[] pixels, int width, int height, int quality);

  //Window
  public boolean init();
  public long create(int style, String title, int width, int height, Window eventMgr, long shared);
  public void destroy(long id);
  public void setcurrent(long id);
  public void seticon(long id, String icon, int x, int y);
  public void pollEvents(int wait);
  public void postEvent();
  public void show(long id);
  public void hide(long id);
  public void swap(long id);
  public void hidecursor(long id);
  public void showcursor(long id);
  public void lockcursor(long id);
  public void getpos(long id, int[] pos);
  public void setpos(long id, int x, int y);
}

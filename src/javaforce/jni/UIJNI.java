package javaforce.jni;

/** UI JNI
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.ui.*;

public class UIJNI implements UIAPI {

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
  public native int loadFont(byte[] font, int ptSize, int[] fontinfo, int[] coords, int[] adv, int[] cps, byte[] pixels, int px, int py);

  //Image
  public native int[] loadPNG(byte[] data, int[] dim);
  public native byte[] savePNG(int[] pixels, int width, int height);
  public native int[] loadJPG(byte[] data, int[] dim);
  public native byte[] saveJPG(int[] pixels, int width, int height, int quality);

  //Window
  public native boolean init();
  public native long create(int style, String title, int width, int height, Window eventMgr, long shared);
  public native void destroy(long id);
  public native void setcurrent(long id);
  public native void seticon(long id, String icon, int x, int y);
  public native void pollEvents(int wait);
  public native void postEvent();
  public native void show(long id);
  public native void hide(long id);
  public native void swap(long id);
  public native void hidecursor(long id);
  public native void showcursor(long id);
  public native void lockcursor(long id);
  public native void getpos(long id, int[] pos);
  public native void setpos(long id, int x, int y);
}

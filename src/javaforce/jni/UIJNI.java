package javaforce.jni;

/** UI JNI
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.ui.*;

public class UIJNI implements UIAPI {

  private static UIAPI api;
  public static UIAPI getInstance() {
    if (api == null) {
      api = new UIJNI();
    }
    return api;
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
  public native int uiLoadFont(byte[] font, int ptSize, int[] fontinfo, int[] coords, int[] adv, int[] cps, byte[] pixels, int px, int py);

  //Image
  public native int[] uiLoadPNG(byte[] data, int length, int[] dim);
  public native byte[] uiSavePNG(int[] pixels, int width, int height);
  public native int[] uiLoadJPG(byte[] data, int length, int[] dim);
  public native byte[] uiSaveJPG(int[] pixels, int width, int height, int quality);

  //Window
  public native boolean uiInit();
  public native long uiWindowCreate(int style, String title, int width, int height, UIEvents events, long shared);
  public native void uiWindowDestroy(long id);
  public native void uiWindowSetCurrent(long id);
  public native void uiWindowSetIcon(long id, String icon, int x, int y);
  public native void uiPollEvents(long id, int wait);
  public native void uiPostEvent();
  public native void uiWindowShow(long id);
  public native void uiWindowHide(long id);
  public native void uiWindowSwap(long id);
  public native void uiWindowHideCursor(long id);
  public native void uiWindowShowCursor(long id);
  public native void uiWindowLockCursor(long id);
  public native void uiWindowGetPos(long id, int[] pos);
  public native void uiWindowSetPos(long id, int x, int y);
}

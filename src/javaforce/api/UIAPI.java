package javaforce.api;

/** UI Native API
 *
 * @author pquiring
 */

import javaforce.jni.*;
import javaforce.ffm.*;
import javaforce.ui.*;

public interface UIAPI {
  public static UIAPI getInstance() {
    if (FFM.enabled()) {
      return UIFFM.getInstance();
    } else {
      return UIJNI.getInstance();
    }
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
  public int uiLoadFont(byte[] font, int ptSize, int[] fontinfo, int[] coords, int[] adv, int[] cps, byte[] pixels, int px, int py);

  //Image
  public int[] uiLoadPNG(byte[] data, int length, int[] dim);
  public byte[] uiSavePNG(int[] pixels, int width, int height);
  public int[] uiLoadJPG(byte[] data, int length, int[] dim);
  public byte[] uiSaveJPG(int[] pixels, int width, int height, int quality);

  //Window
  public boolean uiInit();
  public long uiWindowCreate(int style, String title, int width, int height, UIEvents events, long shared);
  public void uiWindowDestroy(long id);
  public void uiWindowSetCurrent(long id);
  public void uiWindowSetIcon(long id, String icon, int x, int y);
  public void uiPollEvents(long id, int wait);  //TODO : move UIEvents here - then the upcall doesn't need to be stored
  public void uiPostEvent();
  public void uiWindowShow(long id);
  public void uiWindowHide(long id);
  public void uiWindowSwap(long id);
  public void uiWindowHideCursor(long id);
  public void uiWindowShowCursor(long id);
  public void uiWindowLockCursor(long id);
  public void uiWindowGetPos(long id, int[] pos);
  public void uiWindowSetPos(long id, int x, int y);
}

package javaforce.gl;

import javaforce.jni.JFNative;

/** OpenGL Window.
 *
 * @author pquiring
 */

public class GLWindow {
  public static final int MOUSE_BUTTON_LEFT = 1;
  public static final int MOUSE_BUTTON_RIGHT = 2;
  public static final int MOUSE_BUTTON_MIDDLE = 3;

  public static interface KeyEvents {
    public void keyTyped(char ch);
    public void keyPressed(int key);
    public void keyReleased(int key);
  }

  public static interface MouseEvents {
    public void mouseMove(int x,int y);
    public void mouseDown(int button);
    public void mouseUp(int button);
    public void mouseScroll(int x,int y);
  }

  public static interface WindowEvents {
    public void windowResize(int x,int y);
    public void windowClosing();
  }

  private static native boolean ninit();
  public static boolean init() {
    return ninit();
  }

  private long id;

  private KeyEvents keys;
  private MouseEvents mouse;
  private WindowEvents window;

  private static final int KEY_TYPED = 1;
  private static final int KEY_PRESS = 2;
  private static final int KEY_RELEASE = 3;
  private static final int MOUSE_MOVE = 4;
  private static final int MOUSE_DOWN = 5;
  private static final int MOUSE_UP = 6;
  private static final int MOUSE_SCROLL = 7;
  private static final int WIN_RESIZE = 8;
  private static final int WIN_CLOSING = 9;

  /** This is called from native code to dispatch events. */
  private void dispatchEvent(int type, int v1, int v2) {
    switch (type) {
      case KEY_TYPED: if (keys != null) keys.keyTyped((char)v1); break;
      case KEY_PRESS: if (keys != null) keys.keyPressed((char)v1); break;
      case KEY_RELEASE: if (keys != null) keys.keyReleased((char)v1); break;
      case MOUSE_MOVE: if (mouse != null) mouse.mouseMove(v1, v2); break;
      case MOUSE_DOWN: if (mouse != null) mouse.mouseDown(v1); break;
      case MOUSE_UP: if (mouse != null) mouse.mouseUp(v1); break;
      case MOUSE_SCROLL: if (mouse != null) mouse.mouseScroll(v1, v2); break;
      case WIN_RESIZE: if (window != null) window.windowResize(v1, v2); break;
      case WIN_CLOSING: if (window != null) window.windowClosing(); break;
    }
  }

  public static final int STYLE_VISIBLE = 1;
  public static final int STYLE_RESIZABLE = 2;
  public static final int STYLE_TITLEBAR = 4;
  public static final int STYLE_FULLSCREEN = 8;

  private static native long ncreate(int style, String title, int width, int height, GLWindow eventMgr, long shared);
  public boolean create(int style, String title, int width, int height, GLWindow shared) {
    id = ncreate(style, title, width, height, this, shared == null ? 0 : shared.id);
    return id != 0;
  }

  private static native void ndestroy(long id);
  /** Show the window. */
  public void destroy() {
    if (id == 0) return;
    ndestroy(id);
    id = 0;
  }

  private static native void nsetcurrent(long id);
  /** Set the OpenGL Context current for this window. */
  public void setCurrent() {
    nsetcurrent(id);
  }

  private static native void nseticon(long id, String icon, int x, int y);
  /** Set an icon to the window.
   * This function is somewhat platform dependant.
   * Only windows is supported currently.
   * @param filename = file (.ico for windows)
   */
  public void setIcon(String filename, int x, int y) {
    nseticon(id, filename, x, y);
  }

  public void setKeyListener(KeyEvents keys) {
    this.keys = keys;
  }

  public void setMouseListener(MouseEvents mouse) {
    this.mouse = mouse;
  }

  public void setWindowListener(WindowEvents window) {
    this.window = window;
  }

  /** Polls for events. */
  public static native void pollEvents();

  private static native void nshow(long id);
  /** Show the window. */
  public void show() {
    nshow(id);
  }

  private static native void nhide(long id);
  /** Hide the window. */
  public void hide() {
    nhide(id);
  }

  private static native void nswap(long id);
  /** Swaps the OpenGL Buffers. */
  public void swap() {
    nswap(id);
  }

  private static native void nhidecursor(long id);
  /** Hide the cursor. */
  public void hideCursor() {
    nhidecursor(id);
  }

  private static native void nshowcursor(long id);
  /** Show the cursor (default). */
  public void showCursor() {
    nshowcursor(id);
  }

  private static native void nlockcursor(long id);
  /** Hide the cursor and lock to this window.
   * Use showCursor() to unlock.
   */
  public void lockCursor() {
    nlockcursor(id);
  }

  private static native void ngetpos(long id, int pos[]);
  /** Get window position.
   * @return int[0] = x, int[1] = y
   */
  public int[] getPosition() {
    int ret[] = new int[2];
    ngetpos(id, ret);
    return ret;
  }

  private static native void nsetpos(long id, int x, int y);
  /** set window position.
   * @return int[0] = x, int[1] = y
   */
  public void setPosition(int x,int y) {
    nsetpos(id, x, y);
  }
}

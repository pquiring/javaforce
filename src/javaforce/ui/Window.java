package javaforce.ui;

/** OpenGL Window.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.jni.*;
import javaforce.gl.*;
import javaforce.ui.theme.*;
import static javaforce.gl.GL.*;

public class Window {
  private static native boolean ninit();
  public static boolean init() {
    JFNative.load();  //ensure native library is loaded
    return ninit();
  }

  private long id;
  private int width, height;
  private int scale = 1;

  private KeyEvents keys;
  private MouseEvents mouse;
  private WindowEvents window;
  private boolean visible;
  private Texture texture;
  private Object3 object;
  private boolean active = true;
  private Component content;

  private static ArrayList<Canvas> canvasList = new ArrayList<>();
  private static ArrayList<Window> windows = new ArrayList<>();

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
      case MOUSE_MOVE: if (mouse != null) mouse.mouseMove(v1 / scale, v2 / scale); break;
      case MOUSE_DOWN: if (mouse != null) mouse.mouseDown(v1); break;
      case MOUSE_UP: if (mouse != null) mouse.mouseUp(v1); break;
      case MOUSE_SCROLL: if (mouse != null) mouse.mouseScroll(v1, v2); break;
      case WIN_RESIZE: resize(v1, v2); if (window != null) window.windowResize(v1 / scale, v2 / scale); break;
      case WIN_CLOSING: if (window != null) window.windowClosing(); break;
    }
  }

  public static final int STYLE_VISIBLE = 1;
  public static final int STYLE_RESIZABLE = 2;
  public static final int STYLE_TITLEBAR = 4;
  public static final int STYLE_FULLSCREEN = 8;

  private static native long ncreate(int style, String title, int width, int height, Window eventMgr, long shared);
  public boolean create(int style, String title, int width, int height, Window shared) {
    this.width = width;
    this.height = height;
    id = ncreate(style, title, width, height, this, shared == null ? 0 : shared.id);
    if (id != 0) {
      synchronized(windows) {
        windows.add(this);
      }
    }
    return id != 0;
  }

  private static native void ndestroy(long id);
  /** Show the window. */
  public void destroy() {
    if (id == 0) return;
    active = false;
    ndestroy(id);
    id = 0;
    synchronized(windows) {
      windows.remove(this);
    }
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

  /** Polls for events.
   * @param wait = time to wait for event to occur
   *   -1 = wait forever
   *    0 = do not wait
   *    x = wait x milliseconds
   */
  public static native void pollEvents(int wait);

  /** Polls for events. Does not wait for an event.  Same as pollEvents(0); */
  public static void pollEvents() {
    pollEvents(0);
  }

  private static native void nshow(long id);
  /** Show the window. */
  public void show() {
    nshow(id);
    visible = true;
  }

  private static native void nhide(long id);
  /** Hide the window. */
  public void hide() {
    nhide(id);
    visible = false;
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

  public int getWidth() {
    return width;
  }

  public int getWidthScaled() {
    return width / scale;
  }

  public int getHeight() {
    return height;
  }

  public int getHeightScaled() {
    return height / scale;
  }

  public static Window[] getWindows() {
    synchronized(windows) {
      return windows.toArray(new Window[windows.size()]);
    }
  }

  public Component getContent() {
    return content;
  }

  public void setContent(Component content) {
    this.content = content;
    keys = content;
    mouse = content;
    layout();
  }

  public int getScale() {
    return scale;
  }

  public void setScale(int scale) {
    if (scale < 1) scale = 1;
    if (scale > 4) scale = 4;
    this.scale = scale;
    layout();
  }

  public void layout() {
    if (content != null) {
      content.layout(new LayoutMetrics(getWidthScaled(), getHeightScaled()));
    }
  }

  public void resize(int w, int h) {
    width = w;
    height = h;
    if (content != null) {
      layout();
    }
  }

  public static void registerCanvas(Canvas canvas) {
    canvasList.add(canvas);
  }

  public void render(Scene scene) {
    if (debug) System.out.println("Window.render()");
    if (texture == null || texture.getWidth() != getWidthScaled() || texture.getHeight() != getHeightScaled()) {
      if (texture != null) {
        texture.unload();
        texture = null;
      }
      if (object != null) {
        object.freeBuffers();
        object = null;
      }
      texture = new Texture(0, getWidthScaled(), getHeightScaled());
    }
    if (object == null) {
      object = new Object3();
      object.createUVMap();
      //add vertex ccw
      int z = -1;
      object.addVertex(new float[] {0,0,z}, new float[] {0,1});
      object.addVertex(new float[] {1,0,z}, new float[] {1,1});
      object.addVertex(new float[] {1,1,z}, new float[] {1,0});
      object.addVertex(new float[] {0,1,z}, new float[] {0,0});
      //add triangle ccw
      object.addPoly(new int[] {0,1,3});
      object.addPoly(new int[] {1,2,3});
      object.copyBuffers();
    }
    canvasList.clear();
    texture.getImage().fill(0, 0, width, height, Color.OPAQUE + Theme.getTheme().getBackColor().getColor());
    content.render(texture.getImage());
    setCurrent();
    clear(Color.black, getWidth(), getHeight());
    texture.loaded = false;
    texture.load();
    object.bindBuffers(scene);
    texture.bind();
    object.render(scene);
    glFlush();
    swap();
  }

  public Canvas[] getCanvasList() {
    return canvasList.toArray(new Canvas[0]);
  }
}

/*
 * BufferViewer.java
 *
 * Created on May 10, 2019, 1:34 PM
 *
 * @author pquiring
 *
 */

package javaforce.ansi.client;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.jni.*;
import javaforce.jni.lnx.*;
import javaforce.jni.win.*;

public class BufferViewer extends JComponent implements KeyListener, MouseListener, MouseMotionListener {

  public Buffer buffer;
  public Settings settings;

  public BufferViewer(Buffer buffer, Settings settings) {
    this.buffer = buffer;
    changeFont();
  }

  private void init() {
    //now runs in the EDT
    JFLog.log("BufferViewer.init start");
    buffer.init();
    try {
      lock = new Object();
      setFocusable(true);
      setRequestFocusEnabled(true);
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
      timer = new java.util.Timer();
      timer.schedule(new TimerTask() {
        public void run() {
          timer();
        }
      }, 500, 500);
      pane = (JScrollPane)getClientProperty("pane");
      pane.getVerticalScrollBar().setUnitIncrement(8);  //faster!
      if (settings.autoSize) {
        Dimension d;
        d = pane.getViewport().getExtentSize();
        if (d.width < fx) d.width = fx;
        if (d.height < fy) d.height = fy;
        settings.sx = d.width / fx;
        settings.sy = d.height / fy;
      }
      init = true;
      if (settings.autoSize)
        changeSize();
      else
        reSize();
      setVisible(true);
      requestFocus();
      ready = true;  //ready to paint
      render = new Render();
      render.setPriority(RenderPriority);
      render.start();
    } catch (Exception e) {
      JFLog.log(e);
    }
    JFLog.log("Buffer.init done");
  }

  //public data
  public Script script = null;

  //private static data
  private static int fx, fy;  //font size x/y
  private static int descent;

  //private data
  private volatile boolean ready = false;
  private Object lock;
  private Render render;
  private final int RenderPriority = Thread.MAX_PRIORITY-1;
  /** A timer to blink the cursor, the blinky text. */
  private java.util.Timer timer;
  private boolean cursorShown = false;
  private int selectStart = -1, selectEnd = -1;
  private FileOutputStream fos;  //log file
  private boolean blinker = false;
  private boolean blinkerShown = false;
  //connection info
  private boolean connected = false, connecting = false;
  private boolean failed = false;
  private boolean closed = false;
  private boolean init = false;
  private JScrollPane pane;
  private LnxPty pty;

  private long profile_last = 0;
  private void profile(boolean show, String msg) {
    long current = System.nanoTime();
    if (show) System.out.println(msg + "Diff=" + (current-profile_last));
    profile_last = current;
  }

  public byte[] char2byte(char[] buf, int buflen) {
    byte[] tmp = new byte[buflen];
    for(int a=0;a<buflen;a++) tmp[a] = (byte)buf[a];
    return tmp;
  }

  public char[] byte2char(byte[] buf, int buflen) {
    char[] tmp = new char[buflen];
    for(int a=0;a<buflen;a++) {
      tmp[a] = (char)buf[a];
      tmp[a] &= 0xff;
    }
    return tmp;
  }

  public void changeSize() {
    buffer.changeSize(pane.getViewport().getExtentSize());
  }

  public void changeFont() {
    {
      int[] metrics = JFAWT.getFontMetrics(settings.fnt);
      //[0] = width
      //[1] = ascent
      //[2] = descent
//      JFLog.log("metrics=" + metrics[0] + "," + metrics[1] + "," + metrics[2]);
      fx = metrics[0] + settings.fontWidth;
      fy = metrics[1] + metrics[2] + settings.fontHeight;
      descent = metrics[2] + settings.fontDescent;
    }
  }

  private void setPreferredSize() {
    setPreferredSize(new Dimension(fx * buffer.sx, fy * (buffer.sy + buffer.scrollBack)));
  }

  public void reSize() {
    signalRepaint(true, true);
  }

  public void repaint(boolean showCursor) {
    if (showCursor) scrollRectToVisible(new Rectangle(0,fy * buffer.scrollBack,fx * buffer.sx, fy * (buffer.scrollBack + buffer.sy)));
    repaint();
  }

  public void paintComponent(Graphics g) {
    if (!ready) {
      if (!init) init();
      return;
    }
    synchronized(lock) {
      paintComponentLocked(g);
    }
  }

  private JFImage img = new JFImage();  //double buffering image for paint

  public void paintComponentLocked(Graphics gc) {
    Rectangle r = gc.getClipBounds();
    if (img.getWidth() != r.width || img.getHeight() != r.height) {
      img.setImageSize(r.width, r.height);
    }
    img.fill(0,0,r.width,r.height,settings.backColor.getRGB());
    Graphics g = img.getGraphics();
    g.setFont(settings.fnt);
    int startx, starty;  //char
    int endx, endy;  //char
    int offx, offy;  //offset
    startx = r.x / fx;
    offx = r.x % fx;
    endx = (r.x + r.width) / fx + 1;
    if (endx > buffer.sx) endx = buffer.sx;
    starty = r.y / fy;
    offy = r.y % fy;
    endy = (r.y + r.height) / fy + 1;
    if (endy > buffer.sy + buffer.scrollBack) endy = buffer.sy + buffer.scrollBack;
    char ch;
    for(int y = starty;y < endy;y++) {
      for(int x = startx;x < endx;x++) {
        int p = y * buffer.sx + x;
        if ((x == buffer.cx) && (y == buffer.cy + buffer.scrollBack) && (cursorShown)) {
          //draw Cursor
          g.setColor(settings.cursorColor);
        } else {
          //normal background
          if (((p >= selectStart) && (p <= selectEnd)) || ((p >= selectEnd) && (p <= selectStart) && (selectEnd > 0))) {
            g.setColor(settings.selectColor);
          } else {
            g.setColor(buffer.chars[p].bc);
          }
        }
        g.fillRect((x - startx) * fx - offx,(y - starty) * fy - offy,fx,fy);
        if ((blinkerShown) && (buffer.chars[p].blink))
          g.setColor(buffer.chars[p].bc);
        else
          g.setColor(buffer.chars[p].fc);
        ch = buffer.chars[p].ch;
        if (ch != 0) g.drawString("" + ch, (x - startx) * fx - offx,(y+1-starty) * fy - descent - offy);
      }
      if ((y == buffer.scrollBack) && (buffer.scrollBack > 0)) {
        g.setColor(Color.RED);
        g.drawLine(0 - offx, (y - starty) * fy - 2 - offy, (endx - startx) * fx - 1 - offx, (y - starty) * fy - 2 - offy);
      }
    }
    gc.drawImage(img.getImage(), r.x, r.y, null);
  }

  public void setAutoWrap(boolean state) {
    settings.autowrap = state;
  }

  //these methods are overloaded to allow such functionality
  public void nextTab() {System.out.println("nextTab");}
  public void prevTab() {System.out.println("prevTab");}
  public void setTab(int idx) {System.out.println("setTab" + idx);}
  public void setName(String str) {}

  /** This thread handles requesting screen repaints.*/
  private class Render extends Thread {
    public volatile boolean findCursor = true;
    public volatile boolean draw = false;
    public void run() {
      while (!closed) {
        draw = false;
        repaint(findCursor);
        synchronized(this) {
          if (draw) continue;
          try{wait();} catch(Exception e) {}
        }
      }
      JFLog.log("Render thread done");
    }
  }

  public void signalRepaint(boolean findScreen, boolean revalidate) {
    if (render == null) return;
    if (revalidate) {
      setPreferredSize();
      revalidate();  //must call this after resizing
    }
    synchronized(render) {
      render.findCursor = findScreen;
      render.draw = true;
      render.notify();
    }
  }

  public void logFile() {
    if (fos == null) {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      chooser.setMultiSelectionEnabled(false);
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        fos = JF.filecreate(chooser.getSelectedFile().getAbsolutePath());
        if (fos != null) setName(settings.name + "*");
      }
    } else {
      fos = null;
      setName(settings.name);
    }
  }

  public String toString() {
    return "BufferViewer";
  }

//interface KeyListener
  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    int keyMods = e.getModifiersEx() & JFAWT.KEY_MASKS;
    if (keyMods == KeyEvent.CTRL_DOWN_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_A: selectStart = 0; selectEnd = buffer.sx * (buffer.sy + buffer.scrollBack) - 1; break;
        case KeyEvent.VK_W: buffer.close(); break;
        case KeyEvent.VK_C:
        case KeyEvent.VK_INSERT: buffer.copy(); break;
        case KeyEvent.VK_V: buffer.paste(); break;
        case KeyEvent.VK_TAB: nextTab(); break;
        case KeyEvent.VK_HOME:
        case KeyEvent.VK_END: e.consume(); break;
      }
    }
    if (keyMods == KeyEvent.SHIFT_DOWN_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_INSERT: buffer.paste(); break;
      }
    }
    if (keyMods == (KeyEvent.SHIFT_DOWN_MASK & KeyEvent.CTRL_DOWN_MASK)) {
      switch (keyCode) {
        case KeyEvent.VK_TAB: prevTab(); break;
      }
    }
    if (keyMods == KeyEvent.ALT_DOWN_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_HOME: buffer.clrscr(); break;
        case KeyEvent.VK_1: setTab(0); break;
        case KeyEvent.VK_2: setTab(1); break;
        case KeyEvent.VK_3: setTab(2); break;
        case KeyEvent.VK_4: setTab(3); break;
        case KeyEvent.VK_5: setTab(4); break;
        case KeyEvent.VK_6: setTab(5); break;
        case KeyEvent.VK_7: setTab(6); break;
        case KeyEvent.VK_8: setTab(7); break;
        case KeyEvent.VK_9: setTab(8); break;
        case KeyEvent.VK_0: setTab(9); break;
      }
    }
    if (keyMods == 0) {
      switch (keyCode) {
        case KeyEvent.VK_UP:  //arrow keys cause JScrollPane to move
        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_LEFT:
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_PAGE_UP:
        case KeyEvent.VK_PAGE_DOWN:
        case KeyEvent.VK_F10:  //F10 would open menu
          e.consume();
          break;
      }
    }
    if (!connected) return;
//    System.out.println("keyPressed=" + keyCode);  //test
    buffer.ansi.keyPressed(keyCode, keyMods, buffer);
  }
  public void keyReleased(KeyEvent e) {
  }
  public void keyTyped(KeyEvent e) {
    if (!connected) {
      if (failed) {
        failed = false;
        buffer.signalReconnect();
      }
      return;
    }
    char key = e.getKeyChar();
    int mods = e.getModifiersEx() & JFAWT.KEY_MASKS;
    if (mods == KeyEvent.CTRL_DOWN_MASK) {
      if ((key == 10) || (key == 13)) {buffer.output('\r'); buffer.output('\n');}
      return;
    }
    if (mods == KeyEvent.ALT_DOWN_MASK) return;
    if (mods == (KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) return;
    if (key == 10) key = 13;
    if (key == KeyEvent.VK_DELETE) return;  //handled in keyPressed
//    System.out.println("keyTyped=" + ((int)key));  //test
    buffer.output(key);
  }
//interface MouseListener
  public void mouseClicked(MouseEvent e) {
    if (e.getButton() == e.BUTTON2) buffer.paste();
    if (e.getButton() == e.BUTTON1) requestFocus();
  }
  public void mousePressed(MouseEvent e) {
    //start selection process
    if (e.getButton() != e.BUTTON1) return;
    int x = e.getX();
    int y = e.getY();
    selectStart = ((y  / fy) * buffer.sx + (x / fx));
    selectEnd = -1;
  }
  public void mouseReleased(MouseEvent e) {
    if (e.getButton() != e.BUTTON1) return;
    if (selectStart == -1) return;
    int x = e.getX();
    int y = e.getY();
    selectEnd = ((y / fy) * buffer.sx + (x / fx));
    if (selectEnd == selectStart) {selectStart = selectEnd = -1;}
    signalRepaint(false, false);
    if (selectStart != -1) buffer.copy();
  }
  public void mouseEntered(MouseEvent e) {
  }
  public void mouseExited(MouseEvent e) {
  }
//interface MouseMotionListener
  public void mouseDragged(MouseEvent e) {
    if (selectStart == -1) return;
    int x = e.getX();
    int y = e.getY();
    if (x < 0) x = 0;
    if (x > buffer.sx * fx) x = (buffer.sx * fx)-1;
    if (y < 0) y = 0;
    if (y > (buffer.sy + buffer.scrollBack) * fy) y = (buffer.sy + buffer.scrollBack) * fy - 1;
    selectEnd = ((y / fy) * buffer.sx + (x / fx));
    signalRepaint(false, false);
    scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));
  }
  public void mouseMoved(MouseEvent e) {
  }
  public void timer() {
    if (cursorShown) {
      if (blinkerShown) blinkerShown = false; else blinkerShown = true;
      cursorShown = false;
    } else {
      cursorShown = true;
    }
    if (script != null) {
      if (script.process(buffer)) script = null;
    }
    signalRepaint(false, false);
  }
}

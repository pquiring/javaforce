/*
 * BufferViewer.java
 *
 * Created on May 10, 2019, 1:34 PM
 *
 * @author pquiring
 *
 */

package javaforce.ansi.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class BufferViewer extends JComponent implements KeyListener, MouseListener, MouseMotionListener, Buffer.UI {

  public static boolean debug = false;

  public Buffer buffer;
  public Profile profile;

  public BufferViewer(Profile profile) {
    buffer = new Buffer(profile, this);
    this.profile = profile;
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
      pane = (JScrollPane)getClientProperty("pane");
      pane.getVerticalScrollBar().setUnitIncrement(8);  //faster!
      if (profile.autoSize) {
        Dimension d;
        d = pane.getViewport().getExtentSize();
        if (d.width < profile.fontWidth) d.width = profile.fontWidth;
        if (d.height < profile.fontHeight) d.height = profile.fontHeight;
        profile.sx = d.width / profile.fontWidth;
        profile.sy = d.height / profile.fontHeight;
      }
      init = true;
      if (profile.autoSize)
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
    JFLog.log("BufferViewer.init done");
  }

  //private data
  private volatile boolean ready = false;
  private Object lock;
  private Render render;
  private final int RenderPriority = Thread.MAX_PRIORITY-1;
  //connection info
  private boolean init = false;
  private JScrollPane pane;

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
    signalRepaint(true, true);
  }

  public void changeFont() {
    {
      int[] metrics = JFAWT.getFontMetrics(profile.fnt);
      //[0] = width
      //[1] = ascent
      //[2] = descent
      JFLog.log("metrics=" + metrics[0] + "," + metrics[1] + "," + metrics[2]);
      profile.fontWidth = metrics[0];
      profile.fontHeight = metrics[1] + metrics[2];
      profile.fontDescent = metrics[2];
    }
  }

  private void setPreferredSize() {
    setPreferredSize(new Dimension(profile.fontWidth * buffer.sx, profile.fontHeight * (buffer.sy + buffer.scrollBack)));
  }

  public void reSize() {
    signalRepaint(true, true);
  }

  public void repaint(boolean showCursor) {
    if (showCursor) scrollRectToVisible(new Rectangle(0,profile.fontHeight * buffer.scrollBack,profile.fontWidth * buffer.sx, profile.fontHeight * (buffer.scrollBack + buffer.sy)));
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
    img.fill(0,0,r.width,r.height,profile.backColor.getRGB());
    Graphics g = img.getGraphics();
    g.setFont(profile.fnt);
    int startx, starty;  //char
    int endx, endy;  //char
    int offx, offy;  //offset
    startx = r.x / profile.fontWidth;
    offx = r.x % profile.fontWidth;
    endx = (r.x + r.width) / profile.fontWidth + 1;
    if (endx > buffer.sx) endx = buffer.sx;
    starty = r.y / profile.fontHeight;
    offy = r.y % profile.fontHeight;
    endy = (r.y + r.height) / profile.fontHeight + 1;
    if (endy > buffer.sy + buffer.scrollBack) endy = buffer.sy + buffer.scrollBack;
    char ch;
    for(int y = starty;y < endy;y++) {
      for(int x = startx;x < endx;x++) {
        int p = y * buffer.sx + x;
        if ((x == buffer.cx) && (y == buffer.cy + buffer.scrollBack) && (buffer.cursorShown)) {
          //draw Cursor
          g.setColor(profile.cursorColor);
        } else {
          //normal background
          if (((p >= buffer.selectStart) && (p <= buffer.selectEnd)) || ((p >= buffer.selectEnd) && (p <= buffer.selectStart) && (buffer.selectEnd > 0))) {
            g.setColor(profile.selectColor);
          } else {
            g.setColor(buffer.chars[p].bc);
          }
        }
        g.fillRect((x - startx) * profile.fontWidth - offx,(y - starty) * profile.fontHeight - offy,profile.fontWidth,profile.fontHeight);
        if ((buffer.blinkerShown) && (buffer.chars[p].blink))
          g.setColor(buffer.chars[p].bc);
        else
          g.setColor(buffer.chars[p].fc);
        ch = buffer.chars[p].ch;
        if (ch != 0) {
          g.drawString(Character.toString(ch), (x - startx) * profile.fontWidth - offx,(y+1-starty) * profile.fontHeight - profile.fontDescent - offy);
        }
      }
      if ((y == buffer.scrollBack) && (buffer.scrollBack > 0)) {
        g.setColor(Color.RED);
        g.drawLine(0 - offx, (y - starty) * profile.fontHeight - 2 - offy, (endx - startx) * profile.fontWidth - 1 - offx, (y - starty) * profile.fontHeight - 2 - offy);
      }
    }
    gc.drawImage(img.getImage(), r.x, r.y, null);
  }

  public void setAutoWrap(boolean state) {
    profile.autowrap = state;
  }

  public void close() {
    buffer.close();
  }

  public void copy() {
    buffer.copy();
  }

  public void paste() {
    buffer.paste();
  }

  public void output(char[] buf) {
    buffer.output(buf);
  }

  public void changeScrollBack(int newSize) {
    buffer.changeScrollBack(newSize);
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
      while (!buffer.closed) {
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

  public void signalRepaint(boolean findCursor, boolean revalidate) {
    if (revalidate) {
      setPreferredSize();
      revalidate();  //must call this after resizing
    }
    if (render == null) {
      JFLog.log("Warning:render not ready");
      return;
    }
    synchronized(render) {
      render.findCursor = findCursor;
      render.draw = true;
      render.notify();
    }
  }

  public JComponent getComponent() {
    return this;
  }

  public void logFile() {
    if (buffer.fos == null) {
      JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      chooser.setMultiSelectionEnabled(false);
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        buffer.fos = JF.filecreate(chooser.getSelectedFile().getAbsolutePath());
        if (buffer.fos != null) setName(profile.name + "*");
      }
    } else {
      buffer.fos = null;
      setName(profile.name);
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
        case KeyEvent.VK_A: buffer.selectStart = 0; buffer.selectEnd = buffer.sx * (buffer.sy + buffer.scrollBack) - 1; break;
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
    if (!buffer.connected) return;
    if (debug) JFLog.log("keyPressed=" + keyCode);
    buffer.ansi.keyPressed(keyCode, keyMods, buffer);
  }
  public void keyReleased(KeyEvent e) {
  }
  public void keyTyped(KeyEvent e) {
    if (!buffer.connected) {
      if (buffer.failed) {
        buffer.failed = false;
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
    if (debug) JFLog.log("keyTyped=" + ((int)key));
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
    buffer.selectStart = ((y  / profile.fontHeight) * buffer.sx + (x / profile.fontWidth));
    buffer.selectEnd = -1;
  }
  public void mouseReleased(MouseEvent e) {
    if (e.getButton() != e.BUTTON1) return;
    if (buffer.selectStart == -1) return;
    int x = e.getX();
    int y = e.getY();
    buffer.selectEnd = ((y / profile.fontHeight) * buffer.sx + (x / profile.fontWidth));
    if (buffer.selectEnd == buffer.selectStart) {buffer.selectStart = buffer.selectEnd = -1;}
    signalRepaint(false, false);
    if (buffer.selectStart != -1) buffer.copy();
  }
  public void mouseEntered(MouseEvent e) {
  }
  public void mouseExited(MouseEvent e) {
  }
//interface MouseMotionListener
  public void mouseDragged(MouseEvent e) {
    if (buffer.selectStart == -1) return;
    int x = e.getX();
    int y = e.getY();
    if (x < 0) x = 0;
    if (x > buffer.sx * profile.fontWidth) x = (buffer.sx * profile.fontWidth)-1;
    if (y < 0) y = 0;
    if (y > (buffer.sy + buffer.scrollBack) * profile.fontHeight) y = (buffer.sy + buffer.scrollBack) * profile.fontHeight - 1;
    buffer.selectEnd = ((y / profile.fontHeight) * buffer.sx + (x / profile.fontWidth));
    signalRepaint(false, false);
    scrollRectToVisible(new Rectangle(e.getX(), e.getY(), 1, 1));
  }
  public void mouseMoved(MouseEvent e) {
  }
}

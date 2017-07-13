/*
 * Buffer.java
 *
 * Created on August 2, 2007, 7:48 PM
 *
 * @author pquiring
 *
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.jni.lnx.*;
import javaforce.jni.win.*;

import com.jcraft.jsch.*;

public class Buffer extends JComponent implements KeyListener, MouseListener, MouseMotionListener {

  public Buffer() {
    changeFont();
  }

  public void finalize() {
    JFLog.log("Buffer.finalize()");
  }

  private void init() {
    //now runs in the EDT
    JFLog.log("Buffer.init start");
    try {
      lock = new Object();
      setFocusable(true);
      setRequestFocusEnabled(true);
      addKeyListener(this);
      addMouseListener(this);
      addMouseMotionListener(this);
      scrollBack = Settings.settings.scrollBack;
      foreColor = Settings.settings.foreColor;
      backColor = Settings.settings.backColor;
      gotoPos(1,1);
      ansi = new ANSI(this, sd.protocol.equals("telnet"));
      telnet = new Telnet();
      utf8 = new UTF8();
      timer = new java.util.Timer();
      timer.schedule(new TimerTask() {
        public void run() {
          timer();
        }
      }, 500, 500);
      pane = (JScrollPane)getClientProperty("pane");
      pane.getVerticalScrollBar().setUnitIncrement(8);  //faster!
      if (sd.autoSize) {
        Dimension d;
        d = pane.getViewport().getExtentSize();
        if (d.width < fx) d.width = fx;
        if (d.height < fy) d.height = fy;
        sd.sx = d.width / fx;
        sd.sy = d.height / fy;
      }
      sx = sd.sx;
      sy = sd.sy;
      y1 = 0;
      y2 = sy-1;
      chars = new Char[sx*(sy+scrollBack)];
      for(int a=0;a<sx*(sy+scrollBack);a++) chars[a] = new Char();
      init = true;
      if (sd.autoSize)
        changeSize();
      else
        reSize();
      setVisible(true);
      requestFocus();
      ready = true;  //ready to paint
      render = new Render();
      render.setPriority(RenderPriority);
      render.start();
      reader = new Reader();
      reader.setPriority(ReaderPriority);
      reader.start();
    } catch (Exception e) {
      JFLog.log(e);
    }
    JFLog.log("Buffer.init done");
  }

  //public data
  public int sx, sy;  //screen size x/y (80x24)
  public boolean applet = false;  //non-signed applet
  private int y1,y2;  //scroll range
  public Script script = null;

  //private static data
  private static int fx, fy;  //font size x/y
  private static int descent;

  //private data
  private volatile boolean ready = false;
  private Object lock;
  private Render render;
  private final int RenderPriority = Thread.MAX_PRIORITY-1;
  private Reader reader;
  private final int ReaderPriority = Thread.MAX_PRIORITY-1;
  private InputStream in;
  private OutputStream out;
  private Socket s;  //telnet
  private Session session;  //ssh
  private Channel channel;  //ssh
  private SSLSocket ssl;  //ssl
  /** Number of lines that user can scroll back to. */
  private int scrollBack;
  private Telnet telnet;
  private ANSI ansi;
  private UTF8 utf8;
  /** Current Telnet/ANSI code */
  private char code[] = new char[64];  //Telnet/ANSI code
  private int codelen = 0;
  private final char IAC = 255;
  private final char ESC = 27;  //0x1b
  /** The actual screen buffer.*/
  private Char chars[] = null;
  private int cx, cy;  //cursor position (0,0 = top left)
  /** A timer to blink the cursor, the blinky text. */
  private java.util.Timer timer;
  private Color foreColor, backColor;  //current foreColor, backColor
  private boolean cursorShown = false;
  private int selectStart = -1, selectEnd = -1;
  private FileOutputStream fos;  //log file
  private boolean blinker = false;
  private boolean reverse = false;
  private boolean blinkerShown = false;
  //connection info
  private boolean connected = false, connecting = false;
  private boolean failed = false;
  private Frame parent;
  private SiteDetails sd;
  /** eol means we are 1 char beyond the end of a line but haven't moved down to the next line yet */
  private boolean eol = false;
  private boolean closed = false;
  private boolean init = false;
  private JScrollPane pane;
  private LnxPty pty;
  private boolean autowrap = true;

  private long profile_last = 0;
  private void profile(boolean show, String msg) {
    long current = System.nanoTime();
    if (show) System.out.println(msg + "Diff=" + (current-profile_last));
    profile_last = current;
  }

  public byte[] char2byte(char buf[], int buflen) {
    byte tmp[] = new byte[buflen];
    for(int a=0;a<buflen;a++) tmp[a] = (byte)buf[a];
    return tmp;
  }

  public char[] byte2char(byte buf[], int buflen) {
    char tmp[] = new char[buflen];
    for(int a=0;a<buflen;a++) {
      tmp[a] = (char)buf[a];
      tmp[a] &= 0xff;
    }
    return tmp;
  }

  public void output(char buf[]) {
    if (sd.localecho) input(buf, buf.length);
    byte tmp[] = char2byte(buf, buf.length);
    try {
      out.write(tmp);
      out.flush();
    } catch (Exception e) {
      JFLog.log(e);
      if (!closed) {
        close();
      }
    }
  }
  public void output(char ch) {
    if (sd.localecho) input(ch);
    try {
      out.write(new byte[] {(byte)ch});
      out.flush();
    } catch (Exception e) {
      JFLog.log(e);
      if (!closed) {
        close();
      }
    }
  }

  private void input(String str) {
    input(str.toCharArray(), str.length());
  }

  private void input(char buf[], int buflen) {
    //process Telnet/ANSI code
    if (fos != null) {
      byte tmp[] = char2byte(buf, buflen);
      JF.write(fos, tmp, 0, tmp.length);
    }
    char newbuf[] = new char[buflen];
    int newbuflen = 0;
    for(int a=0;a<buflen;a++) {
      if (codelen == 0) {
        if ((buf[a] == IAC) || (buf[a] == ESC) || (sd.utf8 && utf8.isUTF8(buf[a]))) {
          if (newbuflen > 0) {print(newbuf, newbuflen); newbuflen = 0;}
          codelen = 1;
          code[0] = buf[a];
          continue;
        }
        newbuf[newbuflen++] = ansi.encodeChar(buf[a]);
      } else {
        code[codelen++] = buf[a];  //TODO: check overflow
        //some systems generate two ESC in a row (ignore the 1st ESC)
        if ((codelen == 2) && (code[0] == ESC) && (code[1] == ESC)) {codelen = 1;}
        if (code[0] == IAC) {
          if (telnet.decode(code, codelen, this)) codelen = 0;
        } else if (code[0] == ESC) {
          if (ansi.decode(code, codelen, this)) codelen = 0;
        } else /*if (utf8.isUTF8(code[0]))*/ {
          if (utf8.decode(code, codelen, this)) {
            codelen = 0;
            newbuf[newbuflen++] = utf8.char16;
          }
        }
      }
    }
    if (newbuflen > 0) print(newbuf, newbuflen);
  }

  private void input(char ch) {
    char tmp[] = new char[1];
    tmp[0] = ch;
    input(tmp, 1);
  }

  public static void changeFont() {
    {
      int metrics[] = JFAWT.getFontMetrics(Settings.fnt);
      //[0] = width
      //[1] = ascent
      //[2] = descent
//      JFLog.log("metrics=" + metrics[0] + "," + metrics[1] + "," + metrics[2]);
      fx = metrics[0] + Settings.settings.fontWidth;
      fy = metrics[1] + metrics[2] + Settings.settings.fontHeight;
      descent = metrics[2] + Settings.settings.fontDescent;
    }
  }

  private void setPreferredSize() {
    setPreferredSize(new Dimension(fx * sx, fy * (sy + scrollBack)));
  }

  public void reSize() {
    setPreferredSize();
    revalidate();  //must call this after resizing
    signalRepaint(true);
  }

  public synchronized void changeSize() {
    if (!init) return;
    if (!sd.autoSize) return;

    Dimension d;
    d = pane.getViewport().getExtentSize();
    if (d.width < fx) d.width = fx;
    if (d.height < fy) d.height = fy;
    int newsx = d.width / fx;
    int newsy = d.height / fy;
    y1 = 0;
    y2 = newsy-1;

    Char newChars[] = new Char[newsx*(newsy+scrollBack)];
    int sy2 = sy + scrollBack;
    int newsy2 = newsy + scrollBack;
    if (newsy2 > sy2) {
      for(int y=0;y<sy2;y++) {
        if (newsx > sx) {
          for(int x=0;x<sx;x++) newChars[y * newsx + x] = chars[y * sx + x];
          for(int x=sx;x<newsx;x++) newChars[y * newsx + x] = new Char();
        } else {
          for(int x=0;x<newsx;x++) newChars[y * newsx + x] = chars[y * sx + x];
        }
      }
      for(int y=sy2;y<newsy2;y++) {
        for(int x=0;x<newsx;x++) newChars[y * newsx + x] = new Char();
      }
    } else {
      for(int y=0;y<newsy2;y++) {
        if (newsx > sx) {
          for(int x=0;x<sx;x++) newChars[y * newsx + x] = chars[y * sx + x];
          for(int x=sx;x<newsx;x++) newChars[y * newsx + x] = new Char();
        } else {
          for(int x=0;x<newsx;x++) newChars[y * newsx + x] = chars[y * sx + x];
        }
      }
    }
    synchronized(lock) {
      sx = newsx;
      sy = newsy;
      if (channel != null) {
        ((ChannelShell)channel).setPtyType(Settings.settings.termType, sx, sy, sx * 8, sy * 8);
      }
      if (sd.protocol.equals("local")) {
        pty_setsize();
      }
      if (cx >= sx) cx = sx-1;
      if (cy >= sy) cy = sy-1;
      chars = newChars;
      System.gc();
    }
    setPreferredSize();
    revalidate();  //must call this after resizing
    signalRepaint(true);
  }

  public void changeScrollBack(int newSize) {
    Char newChars[] = new Char[sx*(sy+newSize)];
    int diff, pos;
    if (newSize > scrollBack) {
      diff = newSize - scrollBack;
      for(int y=0;y<diff;y++) {for(int x=0;x<sx;x++) newChars[y * sx + x] = new Char();}
      pos = 0;
      for(int y=diff;y<(sy + newSize);y++) {
        for(int x=0;x<sx;x++) {
          newChars[y * sx + x] = chars[pos * sx + x];
        }
        pos++;
        if (pos == sy + scrollBack) pos = 0;
      }
    } else {
      diff = scrollBack - newSize;
      pos = diff;
      if (pos >= sy + scrollBack) pos -= sy + scrollBack;
      for(int y=0;y<(sy + newSize);y++) {
        for(int x=0;x<sx;x++) {
          newChars[y * sx + x] = chars[pos * sx + x];
        }
        pos++;
        if (pos == sy + scrollBack) pos = 0;
      }
    }
    synchronized(lock) {
      scrollBack = newSize;
      chars = newChars;
    }
    setPreferredSize();
    revalidate();  //must call this after resizing
    signalRepaint(true);
  }

  public void setForeColor(int newClr) {
    foreColor = new Color(newClr);
  }
  public void setBackColor(int newClr) {
    backColor = new Color(newClr);
  }
  public void setForeColor(Color newClr) {
    foreColor = newClr;
  }
  public void setBackColor(Color newClr) {
    backColor = newClr;
  }
  public void setBlinker(boolean state) {
    blinker = state;
  }
  public void setReverse(boolean state) {
    reverse = state;
  }
  public Color getForeColor() { return foreColor; }
  public Color getBackColor() { return backColor; }

  public void repaint(boolean showCursor) {
    if (showCursor) scrollRectToVisible(new Rectangle(0,fy * scrollBack,fx * sx, fy * (scrollBack + sy)));
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
    img.fill(0,0,r.width,r.height,Settings.settings.backColor.getRGB());
    Graphics g = img.getGraphics();
    g.setFont(Settings.settings.fnt);
    int startx, starty;  //char
    int endx, endy;  //char
    int offx, offy;  //offset
    startx = r.x / fx;
    offx = r.x % fx;
    endx = (r.x + r.width) / fx + 1;
    if (endx > sx) endx = sx;
    starty = r.y / fy;
    offy = r.y % fy;
    endy = (r.y + r.height) / fy + 1;
    if (endy > sy + scrollBack) endy = sy + scrollBack;
    char ch;
    for(int y = starty;y < endy;y++) {
      for(int x = startx;x < endx;x++) {
        int p = y * sx + x;
        if ((x == cx) && (y == cy + scrollBack) && (cursorShown)) {
          //draw Cursor
          g.setColor(Settings.settings.cursorColor);
        } else {
          //normal background
          if (((p >= selectStart) && (p <= selectEnd)) || ((p >= selectEnd) && (p <= selectStart) && (selectEnd > 0))) {
            g.setColor(Settings.settings.selectColor);
          } else {
            g.setColor(chars[p].bc);
          }
        }
        g.fillRect((x - startx) * fx - offx,(y - starty) * fy - offy,fx,fy);
        if ((blinkerShown) && (chars[p].blink))
          g.setColor(chars[p].bc);
        else
          g.setColor(chars[p].fc);
        ch = chars[p].ch;
        if (ch != 0) g.drawString("" + ch, (x - startx) * fx - offx,(y+1-starty) * fy - descent - offy);
      }
      if ((y == scrollBack) && (scrollBack > 0)) {
        g.setColor(Color.RED);
        g.drawLine(0 - offx, (y - starty) * fy - 2 - offy, (endx - startx) * fx - 1 - offx, (y - starty) * fy - 2 - offy);
      }
    }
    gc.drawImage(img.getImage(), r.x, r.y, null);
  }

  public void clrscr() {
    for(int pos=0;pos<sx * (sy+scrollBack);pos++) {
      chars[pos].ch = 0;
      chars[pos].fc = foreColor;
      chars[pos].bc = backColor;
      chars[pos].blink = false;
    }
    cx = 0;
    cy = 0;
    signalRepaint(true);
    if (sd.protocol.equals("local")) {
      pty_setsize();  //test
    }
  }

  public void print(String txt) {
    print(txt.toCharArray(), txt.length());
  }

  public void print(char buf[], int buflen) {
    for(int a=0;a<buflen;a++) {
      if (script != null) script.input(buf[a], this);
      switch (buf[a]) {
        case 127:
        case 8:
          decPosX();
          break;
        case 9:
          int ts = (getx()-1) % Settings.settings.tabStops;
          for(int t=0;t<Settings.settings.tabStops - ts;t++) {
            if (eol) incPosX();
//            setChar(cx+1, cy+1, ' ');  //don't do that
            incPosX();
          }
          break;
        case 10:  //LF
          incPosY();
          break;
        case 13:  //CR
          gotoPos(1, gety());
          break;
        default:
          if ((buf[a] < 32) && (buf[a] >= 0)) break;
          if (eol) incPosX();
          setChar(cx+1, cy+1, buf[a]);
          incPosX();
          break;
      }
    }
    signalRepaint(true);
  }

  public void print(char ch) {
    char x[] = new char[1];
    x[0] = ch;
    print(x, 1);
  }

  /** Sets a char in buffer.  Uses 1,1 based coords. */
  public void setChar(int cx, int cy, char ch) {
    cx--;
    cy--;
    int pos = (cy+scrollBack) * sx + cx;
    chars[pos].ch = ch;
    if (reverse) {
      chars[pos].fc = backColor;
      chars[pos].bc = foreColor;
    } else {
      chars[pos].fc = foreColor;
      chars[pos].bc = backColor;
    }
    chars[pos].blink = blinker;
  }

  private void decPosX() {
    if (cx > 0) {
      eol = false;
      cx--;
    } else {
      cx = sx-1;
      decPosY();
      eol = true;
    }
  }
  private void decPosY() {
    if (cy > 0) cy--;
  }
  private void incPosX() {
    if (eol) {
      if (!autowrap) return;
      cx = 0;
      incPosY();
      eol = false;
    } else {
      if (cx < (sx-1)) {
        cx++;
      } else {
        eol = true;
      }
    }
  }

  private void incPosY() {
    if (cy < y2)
      cy++;
    else
      scrollUp(1);
  }

  public void setAutoWrap(boolean state) {
    autowrap = state;
  }

  public int getx() {return cx + 1;}
  public int gety() {return cy + 1;}
  public void gotoPos(int x,int y) {
    cx = x-1;
    if (cx < 0) cx = 0;
    if (cx >= sx) cx = sx-1;
    cy = y-1;
    if (cy < 0) cy = 0;
    if (cy >= sy) cy = sy-1;
    eol = false;
  }

  public int gety1() {return y1 + 1;}
  public int gety2() {return y2 + 1;}

  public void sety1(int v) {y1 = v-1;}
  public void sety2(int v) {y2 = v-1;}

  public void scrollUp(int cnt) {
    while (cnt > 0) {
      if (y1==0)
        for(int p=0;p<sx * (y2+1 + scrollBack-1);p++) chars[p] = chars[p + sx];
      else
        for(int p=sx * (y1 + scrollBack);p<sx * (y2+1 + scrollBack-1);p++) chars[p] = chars[p + sx];
      for(int p=0;p<sx;p++) chars[p + (sx * (y2+1 + scrollBack - 1))] = new Char(foreColor, backColor, blinker);
      selectStart = selectEnd = -1;
      cnt--;
    }
  }
  public void scrollDown(int cnt) {
    while (cnt > 0) {
      for(int p=sx * (y2+1 + scrollBack)-1;p>=sx * (y1 + scrollBack+1);p--) chars[p] = chars[p - sx];
      for(int p=0;p<sx;p++) chars[p + (sx * (y1 + scrollBack))] = new Char(foreColor, backColor, blinker);
      selectStart = selectEnd = -1;
      cnt--;
    }
  }

  public void delete() {
    for(int p=cx;p<sx-1;p++) chars[(cy+scrollBack) * sx + p] = chars[(cy+scrollBack) * sx + p + 1];
    chars[(cy+scrollBack) * sx + sx-1] = new Char(foreColor, backColor, blinker);
  }

  public void insert() {
    for(int p=sx-2;p>=cx;p--) chars[(cy+scrollBack) * sx + p + 1] = chars[(cy+scrollBack) * sx + p];
    chars[(cy+scrollBack) * sx + cx] = new Char(foreColor, backColor, blinker);
  }

  public void close() {
    //this method is overloaded in TermApp to also close the tab
    synchronized(reader) {
      closed = true;
      connected = false;
      connecting = false;
    }
    signalRepaint(false);  //allow render thread to exit
    signalReconnect();  //allow reader thread to exit (if connection failed)
    try {if (pty != null) {pty.close(); pty = null;}} catch(Exception e) {}
    try {if (wincom != null) {wincom.close(); wincom = null;}} catch(Exception e) {}
    try {if (lnxcom != null) {lnxcom.close(); lnxcom = null;}} catch(Exception e) {}
    try {if (session != null) {session.disconnect(); session = null;}} catch(Exception e) {}
    try {if (out != null) {out.close(); out = null;}} catch(Exception e) {}
    try {if (in != null) {in.close(); in = null;}} catch(Exception e) {}
    try {if (s != null) {s.close(); s = null;}} catch(Exception e) {}
    if (timer != null) {timer.cancel(); timer = null;}
  }

  //these methods are overloaded to allow such functionality
  public void nextTab() {System.out.println("nextTab");}
  public void prevTab() {System.out.println("prevTab");}
  public void setTab(int idx) {System.out.println("setTab" + idx);}
  public void setName(String str) {}

  /** This thread handles the actual setSiteDetailsing and reading the input.*/
  private class Reader extends Thread {
    public void run() {
      while (!closed) {
        try {
          connecting = true;
          input("Connecting...");
          signalRepaint(true);  //show cursor
          if (doConnect() == false) {
            input("\r\nConnection failed! Press any key to retry...");
            signalRepaint(true);  //show cursor
            failed = true;
            synchronized(reader) {
              if (closed) break;
              try {reader.wait();} catch (Exception e) {}
            }
            input("\r\n");
            continue;
          }
          connected = true;
          connecting = false;
          input("" + ESC + "[2J");  //clrscr
          //keep reading in until it's disconnected
          byte buf[] = new byte[1024];
          int buflen;
          while (connected) {
            buflen = in.read(buf);
            if (buflen == -1) throw new Exception("read error");
            if (buflen > 0) input(byte2char(buf, buflen), buflen);
          }
        } catch (Exception e) {
          JFLog.log(e);
          if (!closed) {
            close();
          }
        }
      }
      JFLog.log("Reader thread done");
    }
  }

  public void signalReconnect() {
    synchronized(reader) { reader.notify(); }
  }

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

  public void signalRepaint(boolean findScreen) {
    if (render == null) return;
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
        if (fos != null) setName(sd.name + "*");
      }
    } else {
      fos = null;
      setName(sd.name);
    }
  }

  public void copy() {
    if ((selectStart == -1) || (selectEnd == -1)) return;  //nothing to copy
    try {
      StringBuilder str = new StringBuilder();
      if (selectStart > selectEnd) {
        int tmp = selectStart;
        selectStart = selectEnd;
        selectEnd = tmp;
      }
      boolean eol = false;
      for(int a=selectStart;a<=selectEnd;a++) {
        if (chars[a].ch != 0) {
          str.append(chars[a].ch);
          if (eol) eol = false;
          if (a % sx == sx-1) str.append("\n");
        } else {
          if (!eol) {
            eol = true;
            str.append("\n");
          }
        }
      }
      StringSelection ss = new StringSelection(str.toString());
      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      if (cb == null) return;
      cb.setContents(ss, ss);
    } catch (Exception e) {}
  }

  public void paste() {
    if (!connected) return;
    try {
      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      String str = (String)cb.getContents(null).getTransferData(DataFlavor.stringFlavor);
      if (str == null) return;
      output(str.replaceAll("\r", "\n").toCharArray());
    } catch (Exception e) {
      JFLog.log(e);
    }
    signalRepaint(true);
  }

  public void setSiteDetails(SiteDetails sd) {
    this.sd = sd;
  }

  private boolean doConnect() {
    if (sd.protocol.equals("com")) return connectCom(sd);
    if (sd.protocol.equals("local")) return connectLocal(sd);
    if (sd.protocol.equals("telnet")) return connectTelnet(sd);
    if (sd.protocol.equals("ssh")) return connectSSH(parent, sd);
    if (sd.protocol.equals("ssl")) return connectSSL(sd);
    return false;
  }

  private void sendTT() {
    //trigger a request for TT ???
    input(new char[] {Telnet.IAC, Telnet.SB, Telnet.TO_TT, Telnet.IAC, Telnet.SE}, 5);
  }

  private void pty_setsize() {
    if (pty == null) return;
    pty.setSize(sx, sy);
  }

  private WinCom wincom;
  private LnxCom lnxcom;
  private boolean connectCom(SiteDetails sd) {
    try {
      if (!Settings.hasComm) throw new Exception("no com support");
      String f[] = sd.host.split(",");  //com1,56000
      if (JF.isWindows()) {
        wincom = WinCom.open(f[0], JF.atoi(f[1]));
        if (wincom == null) return false;
        in = new InputStream() {
          public int read() throws IOException {
            byte data[] = new byte[1];
            int read = 0;
            do {
              read = wincom.read(data);
            } while (read != 1);
            return data[0];
          }
          public int read(byte buf[]) throws IOException {
            int read;
            do {
              read = wincom.read(buf);
            } while (read <= 0);
            return read;
          }
        };
        out = new OutputStream() {
          public void write(int b) throws IOException {
            wincom.write(new byte[] {(byte)b});
          }
          public void write(byte buf[]) throws IOException {
            wincom.write(buf);
          }
        };
      } else {
        lnxcom = LnxCom.open(f[0], JF.atoi(f[1]));
        if (lnxcom == null) return false;
        in = new InputStream() {
          public int read() throws IOException {
            byte data[] = new byte[1];
            int read;
            do {
              read = lnxcom.read(data);
            } while (read <= 0);
            return data[0];
          }
          public int read(byte buf[]) throws IOException {
            int read;
            do {
              read = lnxcom.read(buf);
            } while (read <= 0);
            return read;
          }
        };
        out = new OutputStream() {
          public void write(int b) throws IOException {
            lnxcom.write(new byte[] {(byte)b});
          }
          public void write(byte buf[]) throws IOException {
            lnxcom.write(buf);
          }
        };
      }
      return true;
    } catch (Throwable t) {
      if (!closed) input(t.toString());
      return false;
    }
  }

  private boolean connectLocal(SiteDetails sd) {
    try {
      pty = LnxPty.exec(Settings.settings.termApp
        , new String[] {Settings.settings.termApp, "-i", "-l", null}
        , LnxPty.makeEnvironment(new String[] {"TERM=" + Settings.settings.termType}));
      if (pty == null) throw new Exception("pty failed");
      in = new InputStream() {
        public int read() {return -1;}
        public int read(byte buf[]) {
          return pty.read(buf);
        }
      };
      out = new OutputStream() {
        public void write(int x) {};
        public void write(byte buf[]) {
          pty.write(buf);
        }
      };
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      if (!closed) input(e.toString());
    }
    return false;
  }

  private boolean connectTelnet(SiteDetails sd) {
    try {
      s = new Socket(sd.host, JF.atoi(sd.port));
      in = s.getInputStream();
      out = s.getOutputStream();
      return true;
    } catch (Exception e) {
      if (!closed) input(e.toString());
    }
    return false;
  }

  private int detectX11port() {
    int port = 6000;
/*    Socket tmp;
    for(int a=6000;a<6009;a++) {
      try {
        tmp = new Socket("localhost", a);
        port = a;
        tmp.close();
        break;
      } catch (Exception e) {}
    }*/
    return port;
  }

  private boolean connectSSH(Frame parent, SiteDetails sd) {
    try{
      if (sd.sshKey.length() == 0) {
        if (sd.password.length() == 0) sd.password = GetPassword.getPassword(parent);
        if (sd.password == null) return false;
      }
      Object pipes[];
      JSch jsch=new JSch();
      if (!applet) jsch.setKnownHosts(JF.getUserPath() + "/.jfterm.ssh");
      session=jsch.getSession(sd.username, sd.host, JF.atoi(sd.port));
      if (sd.x11) {
        session.setX11Host("127.0.0.1");
        session.setX11Port(detectX11port());
      }
      if (sd.sshKey.length() == 0) {
        session.setPassword(sd.password);
        UserInfo ui=new SSHUserInfo(sd.password);
        session.setUserInfo(ui);
      } else {
        JFLog.log("using key:" + sd.sshKey);
        jsch.addIdentity(sd.sshKey);
        java.util.Properties config = new java.util.Properties ();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
      }
      session.connect(30000);
      channel=session.openChannel("shell");
      if (sd.x11) {
        channel.setXForwarding(true);
      } else {
        // Enable agent-forwarding.
        ((ChannelShell)channel).setAgentForwarding(true);
      }
      pipes = createPipes();
      if (pipes == null) return false;
      out = (OutputStream)pipes[1];
      channel.setInputStream((InputStream)pipes[0]);
      pipes = createPipes();
      if (pipes == null) return false;
      in = (InputStream)pipes[0];
      channel.setOutputStream((OutputStream)pipes[1]);
      ((ChannelShell)channel).setPtyType(Settings.settings.termType, sd.sx, sd.sy, sd.sx * 8, sd.sy * 8);
//      ((ChannelShell)channel).setEnv(hashMap);  //???
      /*
      java.util.Hashtable env=new java.util.Hashtable();
      env.put("LANG", "ja_JP.eucJP");
      ((ChannelShell)channel).setEnv(env);
      */
      channel.connect(30000);
      return true;
    } catch (Exception e) {
      if (!closed) input(e.toString());
    }
    return false;
  }

  private Object[] createPipes() {
    Object[] ret = new Object[2];
    try {
      ret[0] = new PipedInputStream();
      ret[1] = new PipedOutputStream((PipedInputStream)ret[0]);
      return ret;
    } catch (Exception e) {
      return null;
    }
  }

  private static class SSHUserInfo implements UserInfo {
    public String password;
    public SSHUserInfo(String password) {this.password = password;}
    public String getPassword(){
      return password;
    }
    public boolean promptYesNo(String str){
      Object[] options={ "yes", "no" };
      int foo=JOptionPane.showOptionDialog(null,
        str,
        "Warning",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.WARNING_MESSAGE,
        null, options, options[0]);
      return foo==0;
    }
    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ return true; }
    public boolean promptPassword(String message){ return true; }
    public void showMessage(String message){
      JOptionPane.showMessageDialog(null, message);
    }
  }

  private boolean connectSSL(SiteDetails sd) {
    try {
      TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }
          public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
          public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        }
      };
      // Let us create the factory where we can set some parameters for the connection
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
//      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();  //this method will only work with trusted certs
      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) sc.getSocketFactory();  //this method will work with untrusted certs
      ssl = (SSLSocket) sslsocketfactory.createSocket(sd.host, JF.atoi(sd.port));
      s = (Socket)ssl;
      in = s.getInputStream();
      out = s.getOutputStream();
      return true;
    } catch (Exception e) {
      if (!closed) input(e.toString());
    }
    return false;
  }

  public String toString() {
    return "Buffer";
  }

//interface KeyListener
  public void keyPressed(KeyEvent e) {
    int keyCode = e.getKeyCode();
    int keyMods = e.getModifiers();
    if (keyMods == KeyEvent.CTRL_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_A: selectStart = 0; selectEnd = sx * (sy + scrollBack) - 1; break;
        case KeyEvent.VK_W: close(); break;
        case KeyEvent.VK_C:
        case KeyEvent.VK_INSERT: copy(); break;
        case KeyEvent.VK_V: paste(); break;
        case KeyEvent.VK_TAB: nextTab(); break;
        case KeyEvent.VK_HOME:
        case KeyEvent.VK_END: e.consume(); break;
      }
    }
    if (keyMods == KeyEvent.SHIFT_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_INSERT: paste(); break;
      }
    }
    if (keyMods == (KeyEvent.SHIFT_MASK & KeyEvent.CTRL_MASK)) {
      switch (keyCode) {
        case KeyEvent.VK_TAB: prevTab(); break;
      }
    }
    if (keyMods == KeyEvent.ALT_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_HOME: clrscr(); break;
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
    ansi.keyPressed(keyCode, keyMods, this);
  }
  public void keyReleased(KeyEvent e) {
  }
  public void keyTyped(KeyEvent e) {
    if (!connected) {
      if (failed) {
        failed = false;
        signalReconnect();
      }
      return;
    }
    char key = e.getKeyChar();
    if (e.getModifiers() == KeyEvent.CTRL_MASK) {
      if ((key == 10) || (key == 13)) {output('\r'); output('\n');}
      return;
    }
    if (e.getModifiers() == KeyEvent.ALT_MASK) return;
    if (e.getModifiers() == (KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK)) return;
    if (key == 10) key = 13;
    if (key == KeyEvent.VK_DELETE) return;  //handled in keyPressed
//    System.out.println("keyTyped=" + ((int)key));  //test
    output(key);
  }
//interface MouseListener
  public void mouseClicked(MouseEvent e) {
    if (e.getButton() == e.BUTTON2) paste();
    if (e.getButton() == e.BUTTON1) requestFocus();
  }
  public void mousePressed(MouseEvent e) {
    //start selection process
    if (e.getButton() != e.BUTTON1) return;
    int x = e.getX();
    int y = e.getY();
    selectStart = ((y  / fy) * sx + (x / fx));
    selectEnd = -1;
  }
  public void mouseReleased(MouseEvent e) {
    if (e.getButton() != e.BUTTON1) return;
    if (selectStart == -1) return;
    int x = e.getX();
    int y = e.getY();
    selectEnd = ((y / fy) * sx + (x / fx));
    if (selectEnd == selectStart) {selectStart = selectEnd = -1;}
    signalRepaint(false);
    if (selectStart != -1) copy();
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
    if (x > sx * fx) x = (sx * fx)-1;
    if (y < 0) y = 0;
    if (y > (sy + scrollBack) * fy) y = (sy + scrollBack) * fy - 1;
    selectEnd = ((y / fy) * sx + (x / fx));
    signalRepaint(false);
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
      if (script.process(this)) script = null;
    }
    signalRepaint(false);
  }
}

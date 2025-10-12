package javaforce.webui;

/** Linux Terminal
 *
 * Executes linux console app and displays output in a terminal.
 *
 * Default size : 80x25
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.ansi.client.*;
import javaforce.webui.event.*;
import javaforce.jni.lnx.*;
import javaforce.service.*;

public class Terminal extends Container implements Screen, Resized, KeyDown, MouseDown {

  private class Line extends Component {

    public boolean dirty;
    public boolean blink;

    private int len;
    private char[] chs;
    private int[] fcs;
    private int[] bcs;
    public int y;

    public Line(int len, int fc, int bc) {
      this.len = len;
      chs = new char[len];
      fcs = new int[len];
      bcs = new int[len];
      for(int i=0;i<len;i++) {
        fcs[i] = fc;
        bcs[i] = bc;
        chs[i] = ' ';
      }
      dirty = true;
    }

    public String html() {
      int fc = -1;
      int bc = -1;
      char ch;
      StringBuilder html = new StringBuilder();
      html.append("<div");
      html.append(getAttrs());
      html.append(">");
      for(int x=0;x<len;x++) {
        int nfc = fcs[x];
        int nbc = bcs[x];
        if (cursorShown && y == cy && x == cx) {
          nbc ^= 0xffffff;
        }
        if (fc != nfc || bc != nbc) {
          fc = nfc;
          bc = nbc;
          if (x > 0) {
            html.append("</pre>");
          }
          html.append("<pre style='");
          html.append("color: #" + String.format("%06x", fc) + ";");
          html.append("background-color: #" + String.format("%06x", bc) + ";");
          html.append("'>");
        }
        ch = chs[x];
        switch (ch) {
          case '<': html.append("&lt;"); break;
          case '>': html.append("&gt;"); break;
          case '&': html.append("&amp;"); break;
          default: html.append(ch); break;
        }
      }
      html.append("</pre>");
      html.append("</div>");
      return html.toString();
    }

    public void setlen(int len, int fc, int bc) {
      int oldlen = this.len;
      if (oldlen == len) return;
      chs = Arrays.copyOf(chs, len);
      fcs = Arrays.copyOf(fcs, len);
      bcs = Arrays.copyOf(bcs, len);
      for(int i = oldlen;i<len;i++) {
        chs[i] = ' ';
        fcs[i] = fc;
        bcs[i] = bc;
      }
      this.len = len;
      dirty = true;
    }

    public void clear(int fc, int bc) {
      for(int i=0;i<len;i++) {
        chs[i] = ' ';
        fcs[i] = fc;
        bcs[i] = bc;
      }
      dirty = true;
    }

    public String toString() {
      StringBuilder txt = new StringBuilder();
      for(int i=0;i<len;i++) {
        txt.append(chs[i]);
      }
      return txt.toString();
    }
  }

  public static boolean debug = false;

  private LnxPty pty;
  private String[] cmd;
  private ANSI ansi;
  private TelnetDecoder telnet;
  private UTF8 utf8;
  private InputStream in;
  private OutputStream out;
  private Reader reader;
  private Timer timer;
  private boolean active;

  private static final int bufSize = 256;
  private static final int fontSizeX = 9;
  private static final int fontSizeY = 18;

  /** Terminal.
   * @param cmd = command with arguments to execute
   */
  public Terminal(String[] cmd) {
    int cmdlen = cmd.length;
    //add null terminator required by LnxPty.exec()
    this.cmd = new String[cmdlen + 1];
    System.arraycopy(cmd, 0, this.cmd, 0, cmdlen);
    for(int i=0;i<sy;i++) {
      Line line = new Line(sx, fc, bc);
      add(line);
    }
    setMaxWidth();
    setMaxHeight();
    setFocusable();
    if (debug) JFLog.log("Terminal:" + sx + "x" + sy);
    ANSI.debug = debug;
  }

  /** Terminal.
   * Connect to existing Linux pty.
   * @param pty = existing pty interface.
   */
  public Terminal(LnxPty pty) {
    for(int i=0;i<sy;i++) {
      Line line = new Line(sx, fc, bc);
      add(line);
    }
    setMaxWidth();
    setMaxHeight();
    setFocusable();
    if (debug) JFLog.log("Terminal:" + sx + "x" + sy);
    ANSI.debug = debug;
    this.pty = pty;
  }

  public void init() {
    super.init();
    addKeyDownListenerPreventDefault(this);
    addMouseDownListener(this);
    addResizedListener(this);
    active = true;
    ansi = new ANSI(this, true);
    telnet = new TelnetDecoder();
    utf8 = new UTF8();
    if (connect()) {
      reader = new Reader();
      reader.start();
      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {public void run() {flashCursor();}}, 100, 100);
    } else {
      print("connect() failed!".toCharArray());
    }
  }

  private boolean connect() {
    try {
      if (pty == null) {
        pty = LnxPty.exec(
          cmd[0],
          cmd,
          LnxPty.makeEnvironment(new String[] {"TERM=xterm"})
        );
        if (pty == null) throw new Exception("pty failed");
      }
      in = new InputStream() {
        public int read() {return -1;}
        public int read(byte[] buf) {
          return pty.read(buf);
        }
      };
      out = new OutputStream() {
        public void write(int x) {};
        public void write(byte[] buf) {
          pty.write(buf);
        }
      };
      pty.setSize(sx, sy);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      print(e.toString().toCharArray(), e.toString().length());
    }
    return false;
  }

  public void disconnect() {
    active = false;
    clrscr();
    if (pty != null) {
      pty.close();
      pty = null;
    }
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  public void onResized(Component comp, int width, int height) {
    if (debug) JFLog.log("onresized:" + width + "x" + height);
    sx = width / fontSizeX;
    if (sx < 1) sx = 1;
    sy = height / fontSizeY;
    if (sy < 1) sy = 1;
    y1 = 0;
    y2 = sy - 1;
    if (pty != null) {
      pty.setSize(sx, sy);
    }
    clampCursor();
    setLines();
  }

  private void clampCursor() {
    if (cx >= sx) cx = sx - 1;
    if (cy >= sy) cy = sy - 1;
  }

  private void setLines() {
    int cnt = count();
    if (cnt > sy) {
      //remove lines
      while (cnt > sy) {
        remove(cnt - 1);
        cnt--;
      }
    } else {
      //add lines
      while (cnt < sy) {
        add(new Line(sx, fc, bc));
        cnt++;
      }
    }
    for(int i=0;i<cnt;i++) {
      Line line = (Line)get(i);
      line.setlen(sx, fc, bc);
    }
    update();
  }

  public void onKeyDown(KeyEvent event, Component comp) {
    if (event.keyChar == 0) {
      int code = event.keyCode;
      int mods = 0;
      if (event.altKey) {
        mods |= java.awt.event.KeyEvent.ALT_DOWN_MASK;
      }
      if (event.ctrlKey) {
        mods |= java.awt.event.KeyEvent.CTRL_DOWN_MASK;
      }
      if (event.shiftKey) {
        mods |= java.awt.event.KeyEvent.SHIFT_DOWN_MASK;
      }
      if (debug) JFLog.log("key code=" + code + ",mods=" + mods);
      switch (code) {
        case 8: break;  //backspace
        case 10: break;  //enter
        case KeyEvent.VK_ARROW_LEFT: ansi.keyPressed(java.awt.event.KeyEvent.VK_LEFT, mods, this); return;
        case KeyEvent.VK_ARROW_RIGHT: ansi.keyPressed(java.awt.event.KeyEvent.VK_RIGHT, mods, this); return;
        case KeyEvent.VK_ARROW_UP: ansi.keyPressed(java.awt.event.KeyEvent.VK_UP, mods, this); return;
        case KeyEvent.VK_ARROW_DOWN: ansi.keyPressed(java.awt.event.KeyEvent.VK_DOWN, mods, this); return;
        case KeyEvent.VK_ESCAPE: break;
        case KeyEvent.VK_TAB: break;
        case KeyEvent.VK_HOME: ansi.keyPressed(java.awt.event.KeyEvent.VK_HOME, mods, this); return;
        case KeyEvent.VK_END: ansi.keyPressed(java.awt.event.KeyEvent.VK_END, mods, this); return;
        case KeyEvent.VK_PAGE_UP: ansi.keyPressed(java.awt.event.KeyEvent.VK_PAGE_UP, mods, this); return;
        case KeyEvent.VK_PAGE_DOWN: ansi.keyPressed(java.awt.event.KeyEvent.VK_PAGE_DOWN, mods, this); return;
        case KeyEvent.VK_DELETE: ansi.keyPressed(java.awt.event.KeyEvent.VK_DELETE, mods, this); return;
        case KeyEvent.VK_PAUSE: ansi.keyPressed(java.awt.event.KeyEvent.VK_PAUSE, mods, this); return;
        case KeyEvent.VK_F1: ansi.keyPressed(java.awt.event.KeyEvent.VK_F1, mods, this); return;
        case KeyEvent.VK_F2: ansi.keyPressed(java.awt.event.KeyEvent.VK_F2, mods, this); return;
        case KeyEvent.VK_F3: ansi.keyPressed(java.awt.event.KeyEvent.VK_F3, mods, this); return;
        case KeyEvent.VK_F4: ansi.keyPressed(java.awt.event.KeyEvent.VK_F4, mods, this); return;
        case KeyEvent.VK_F5: ansi.keyPressed(java.awt.event.KeyEvent.VK_F5, mods, this); return;
        case KeyEvent.VK_F6: ansi.keyPressed(java.awt.event.KeyEvent.VK_F6, mods, this); return;
        case KeyEvent.VK_F7: ansi.keyPressed(java.awt.event.KeyEvent.VK_F7, mods, this); return;
        case KeyEvent.VK_F8: ansi.keyPressed(java.awt.event.KeyEvent.VK_F8, mods, this); return;
        case KeyEvent.VK_F9: ansi.keyPressed(java.awt.event.KeyEvent.VK_F9, mods, this); return;
        case KeyEvent.VK_F10: ansi.keyPressed(java.awt.event.KeyEvent.VK_F10, mods, this); return;
        case KeyEvent.VK_F11: ansi.keyPressed(java.awt.event.KeyEvent.VK_F11, mods, this); return;
        case KeyEvent.VK_F12: ansi.keyPressed(java.awt.event.KeyEvent.VK_F12, mods, this); return;
        default: if (debug) JFLog.log("unknown key code:" + code); return;
      }
      char[] buf = new char[1];
      buf[0] = (char)code;
      output(buf);
    } else {
      if (debug) JFLog.log("key char=" + event.keyChar);
      char[] buf = new char[1];
      buf[0] = event.keyChar;
      output(buf);
    }
  }

  public void onMouseDown(MouseEvent me, Component comp) {
    setFocus();
  }

  byte[] char2byte = new byte[bufSize];
  private byte[] char2byte(char[] buf, int buflen) {
    if (char2byte.length != buflen) {
      char2byte = new byte[buflen];
    }
    for(int a=0;a<buflen;a++) {
      char2byte[a] = (byte)buf[a];
    }
    return char2byte;
  }

  char[] byte2char = new char[bufSize];
  private char[] byte2char(byte[] buf, int buflen) {
    for(int a=0;a<buflen;a++) {
      byte2char[a] = (char)(buf[a] & 0xff);
    }
    return byte2char;
  }

  private class Reader extends Thread {
    public void run() {
      byte[] buf = new byte[bufSize];
      try {
        while (active) {
          int buflen = in.read(buf);
          if (buflen == -1) throw new Exception("read error");
          if (buflen > 0) {
            if (debug) JFLog.log("read=" + buflen);
            input(byte2char(buf, buflen), buflen);
          }
          if (buflen == 0) JF.sleep(100);
        }
      } catch (Exception e) {
        JFLog.log(e);
        disconnect();
      }
    }
  }

  private boolean requested_size;

  public void flashCursor() {
    if (!requested_size) {
      if (isLoaded()) {
        requestSize();
        requested_size = true;
      }
    }
    synchronized (cursorLock) {
      cursorCounter -= 100;
      if (cursorCounter <= 0) {
        cursorShown = !cursorShown;
        updateCursor();
        cursorCounter = 500;
        update();
      }
    }
  }

  public void updateCursor() {
    synchronized (cursorLock) {
      Line line = getLine(cy);
      line.dirty = true;
      cursorCounter = 500;
    }
  }

  private int codelen;
  private final char IAC = 255;
  private final char ESC = 27;  //0x1b
  private char[] code = new char[bufSize];  //Telnet/ANSI code
  private int sx = 80;
  private int sy = 25;
  private int fc = 0x00000000;  //black
  private int bc = 0x00ffffff;  //white
  private int cx, cy;  //cursor position (0,0 = top left)
  private Object cursorLock = new Object();
  private boolean cursorShown;
  private int cursorCounter = 500;
  //y1-y2 = scroll area
  private int y1 = 0;
  private int y2 = 25 - 1;
  private boolean eol;
  private boolean autowrap = true;
  private boolean blinker;
  private boolean reverse;
  private int tabStops = 8;

  public void input(char[] buf, int buflen) {
    //process Telnet/ANSI code
    if (debug) writeArray(" input", buf, 0, buflen);

    char[] newbuf = new char[buflen];
    int newbuflen = 0;
    for(int a=0;a<buflen;a++) {
      if (codelen == 0) {
        if ((buf[a] == IAC) || (buf[a] == ESC) || (utf8.isUTF8(buf[a]))) {
          if (newbuflen > 0) {print(newbuf, newbuflen); newbuflen = 0;}
          codelen = 1;
          code[0] = buf[a];
          continue;
        }
        newbuf[newbuflen++] = ansi.encodeChar(buf[a]);
      } else {
        if (codelen == code.length) {
          if (debug) writeArray("  code overflow", code, 0, code.length);
          codelen = 0;
          continue;
        }
        code[codelen++] = buf[a];  //TODO: check overflow
        //some systems generate two ESC in a row (ignore the 1st ESC)
        if ((codelen == 2) && (code[0] == ESC) && (code[1] == ESC)) {codelen = 1;}
        if (code[0] == IAC) {
          if (telnet.decode(code, codelen, this)) {
            codelen = 0;
          }
        } else if (code[0] == ESC) {
          if (ansi.decode(code, codelen, this)) {
            codelen = 0;
          }
        } else /*if (utf8.isUTF8(code[0]))*/ {
          if (utf8.decode(code, codelen, this)) {
            codelen = 0;
            newbuf[newbuflen++] = utf8.char16;
          }
        }
      }
    }
    if (newbuflen > 0) print(newbuf, newbuflen);
    update();
  }

  public void print(char[] buf) {
    print(buf, buf.length);
  }

  public void print(char[] buf, int buflen) {
    for(int i=0;i<buflen;i++) {
      switch (buf[i]) {
        case 127:
        case 8:
          decPosX();
          break;
        case 9:
          int ts = (getx()-1) % tabStops;
          for(int t=0;t<tabStops - ts;t++) {
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
          if ((buf[i] < 32) && (buf[i] >= 0)) break;
          if (eol) incPosX();
          setChar(cx+1, cy+1, buf[i]);
          incPosX();
      }
    }
  }

  private void update() {
    int cnt = count();
    for(int y=0;y<cnt;y++) {
      Line line = (Line)get(y);
      if (line == null) continue;
      line.y = y;
      if (line.dirty) {
        line.dirty = false;
        line.sendEvent("replace", new String[] {"html=" + line.html()});
      }
    }
  }

  private Line getLine(int y) {
    int cnt = count();
    int off = cnt - sy + y;
    if (off < 0) off = 0;
    if (off >= cnt) off = cnt - 1;
    return (Line)get(off);
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
    if (cy > 0) {
      updateCursor();
      cy--;
      updateCursor();
    }
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
    if (cy < y2) {
      updateCursor();
      cy++;
      updateCursor();
    } else {
      scrollUp(1);
    }
  }

  public void writeArray(byte[] tmp) {
    StringBuilder msg = new StringBuilder();
    msg.append("output:" + tmp.length + ":");
    for(int i=0;i<tmp.length;i++) {
      if (tmp[i] < 32) {
        msg.append("{");
        msg.append(Integer.toString(tmp[i] & 0xff));
        msg.append("}");
      } else {
        msg.append(tmp[i]);
      }
    }
    JFLog.log(msg.toString());
  }

  public void writeArray(String txt, char[] buf, int off, int buflen) {
    StringBuilder msg = new StringBuilder();
    msg.append(txt + ":" + buflen + ":");
    for(int i=0;i<buflen;i++) {
      if (buf[i] < 32) {
        msg.append("{");
        msg.append(Integer.toString(buf[i]));
        msg.append("}");
      } else {
        msg.append(buf[i]);
      }
    }
    JFLog.log(msg.toString());
  }

  //Screen interface
  public void output(char[] buf) {
    byte[] tmp = char2byte(buf, buf.length);
    if (debug) writeArray(tmp);
    if (!active) return;
    try {
      out.write(tmp);
      out.flush();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public int getForeColor() {return fc;}
  public int getBackColor() {return bc;}
  public int getsx() {return sx;}
  public int getsy() {return sy;}
  public int getx() {return cx + 1;}
  public int gety() {return cy + 1;}
  public int gety1() {return y1 + 1;}
  public int gety2() {return y2 + 1;}
  public void sety1(int v) {y1 = v - 1;}
  public void sety2(int v) {y2 = v - 1;}
  public void scrollUp(int cnt) {
    updateCursor();
    for(int i=0;i<cnt;i++) {
      remove(y1);
      add(y2, new Line(sx, fc, bc));
    }
    updateCursor();
  }
  public void scrollDown(int cnt) {
    updateCursor();
    for(int i=0;i<cnt;i++) {
      remove(y2);
      add(y1, new Line(sx, fc, bc));
    }
    updateCursor();
  }
  public void delete() {
    Line line = getLine(cy);
    for(int p=cx;p<sx-1;p++) {
      line.chs[p] = line.chs[p + 1];
      line.fcs[p] = line.fcs[p + 1];
      line.bcs[p] = line.bcs[p + 1];
    }
    line.chs[sx-1] = ' ';
    line.fcs[sx-1] = fc;
    line.bcs[sx-1] = bc;
  }
  public void insert() {
    Line line = getLine(cy);
    for(int p=sx-2;p>=cx;p--) {
      line.chs[p + 1] = line.chs[p];
      line.fcs[p + 1] = line.fcs[p];
      line.bcs[p + 1] = line.bcs[p];
    }
    line.chs[cx] = ' ';
    line.fcs[cx] = fc;
    line.bcs[cx] = bc;
  }
  public void gotoPos(int x,int y) {
    if (debug) JFLog.log("gotoPos:" + x + "," + y);
    updateCursor();
    cx = x-1;
    if (cx < 0) cx = 0;
    if (cx >= sx) cx = sx-1;
    cy = y-1;
    if (cy < 0) cy = 0;
    if (cy >= sy) cy = sy-1;
    eol = false;
    updateCursor();
  }
  public void setChar(int x, int y, char ch) {
    if (debug) JFLog.log("setChar:" + x + "," + y + ":" + ch + ":" + (int)ch);
    x--;
    y--;
    Line line = getLine(y);
    if (line == null) {
      return;
    }
    if (x < 0 || x >= line.len) {
      return;
    }
    line.chs[x] = ch;
    if (reverse) {
      line.fcs[x] = bc;
      line.bcs[x] = fc;
    } else {
      line.fcs[x] = fc;
      line.bcs[x] = bc;
    }
    line.blink = blinker;
    line.dirty = true;
  }
  public void setAutoWrap(boolean state) {autowrap = state;}
  public void clrscr() {
    updateCursor();
    int cnt = count();
    for(int i=0;i<cnt;i++) {
      Line line = (Line)get(i);
      line.clear(fc, bc);
    }
    cx = 0;
    cy = 0;
    updateCursor();
  }
  public void setBlinker(boolean state) {blinker = state;}
  public void setReverse(boolean state) {reverse = state;}
  public void setForeColor(int newClr) {
    if (debug) JFLog.log("fc=" + newClr);
    fc = newClr & 0xffffff;
  }
  public void setBackColor(int newClr) {
    if (debug) JFLog.log("bc=" + newClr);
    bc = newClr & 0xffffff;
  }
  public String getTermType() {return "vt100";}

  private static class Test implements WebUIHandler {

    public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
      Panel panel = new Panel();
      Terminal terminal = new Terminal(new String[] {"/usr/bin/bash", "-i", "-l"});
      panel.add(terminal);
      return panel;
    }

    public byte[] getResource(String url, HTTP.Parameters params, WebRequest request, WebResponse response) {
      return null;
    }

    public void clientConnected(WebUIClient client) {
    }

    public void clientDisconnected(WebUIClient client) {
    }

  }

  public static void main(String[] args) {
    new WebUIServer().start(new Test(), 8080);
  }

}

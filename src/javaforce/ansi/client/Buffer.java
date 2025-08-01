/*
 * Buffer.java
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
import java.awt.datatransfer.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.jni.lnx.*;
import javaforce.jni.win.*;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;

public class Buffer implements Screen {

  public static interface UI {
    public void signalRepaint(boolean findCursor, boolean revalidate);
    public JComponent getComponent();
    public void close();
  }

  public Buffer(Profile profile, UI ui) {
    this.profile = profile;
    this.ui = ui;
  }

  public Profile profile;
  public UI ui;

  public void init() {
    JFLog.log("Buffer.init start");
    try {
      lock = new Object();
      scrollBack = profile.scrollBack;
      foreColor = profile.foreColor;
      backColor = profile.backColor;
      ansi = new ANSI(this, profile.protocol.equals("telnet"));
      telnet = new TelnetDecoder();
      utf8 = new UTF8();
      JFLog.log("Screen Size:" + profile.sx + "," + profile.sy);
      sx = profile.sx;
      sy = profile.sy;
      if (sx < 80) {
        sx = 80;
      }
      if (sy < 25) {
        sy = 25;
      }
      y1 = 0;
      y2 = sy-1;
      chars = new Char[sx*(sy+scrollBack)];
      for(int a=0;a<sx*(sy+scrollBack);a++) chars[a] = new Char(profile.foreColor, profile.backColor);
      gotoPos(1,1);
      init = true;
      ready = true;  //ready to paint
      timer = new java.util.Timer();
      timer.schedule(new TimerTask() {
        public void run() {
          timer();
        }
      }, 500, 500);
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
  private int y1,y2;  //scroll range
  public Script script = null;

  //private data
  private volatile boolean ready = false;
  private Object lock;
  private final int RenderPriority = Thread.MAX_PRIORITY-1;
  private Reader reader;
  private final int ReaderPriority = Thread.MAX_PRIORITY-1;
  private InputStream in;
  private OutputStream out;
  private Socket s;  //telnet
  private SshClient client;  //ssh
  private ClientSession session;  //ssh
  private ClientChannel channel;  //ssh
  private SSLSocket ssl;  //ssl
  /** Number of lines that user can scroll back to. */
  public int scrollBack;
  private TelnetDecoder telnet;
  public ANSI ansi;
  private UTF8 utf8;
  /** Current Telnet/ANSI code */
  private char[] code = new char[64];  //Telnet/ANSI code
  private int codelen = 0;
  private final char IAC = 255;
  private final char ESC = 27;  //0x1b
  /** The actual screen buffer.*/
  public Char[] chars = null;
  public int cx, cy;  //cursor position (0,0 = top left)
  /** A timer to blink the cursor, the blinky text. */
  private java.util.Timer timer;
  private Color foreColor, backColor;  //current foreColor, backColor
  public boolean cursorShown = false;
  public int selectStart = -1, selectEnd = -1;
  public FileOutputStream fos;  //log file
  private boolean blinker = false;
  private boolean reverse = false;
  public boolean blinkerShown = false;
  //connection info
  public boolean connected = false;
  public boolean failed = false;
  private Frame parent;
  /** eol means we are 1 char beyond the end of a line but haven't moved down to the next line yet */
  private boolean eol = false;
  public boolean closed = false;
  private boolean init = false;
  private LnxPty pty;
  private boolean autowrap = true;

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

  public void output(char[] buf) {
    if (profile.localEcho) input(buf, buf.length);
    byte[] tmp = char2byte(buf, buf.length);
    try {
      out.write(tmp);
      out.flush();
    } catch (Exception e) {
      JFLog.log(e);
      if (!closed) {
        ui.close();
      }
    }
  }
  public void output(char ch) {
    if (profile.localEcho) input(ch);
    try {
      out.write(new byte[] {(byte)ch});
      out.flush();
    } catch (Exception e) {
      JFLog.log(e);
      if (!closed) {
        ui.close();
      }
    }
  }

  private void input(String str) {
    input(str.toCharArray(), str.length());
  }

  private void input(char[] buf, int buflen) {
    //process Telnet/ANSI code
    if (fos != null) {
      byte[] tmp = char2byte(buf, buflen);
      JF.write(fos, tmp, 0, tmp.length);
    }
    char[] newbuf = new char[buflen];
    int newbuflen = 0;
    for(int a=0;a<buflen;a++) {
      if (codelen == 0) {
        if ((buf[a] == IAC) || (buf[a] == ESC) || (profile.utf8 && utf8.isUTF8(buf[a]))) {
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
    char[] tmp = new char[1];
    tmp[0] = ch;
    input(tmp, 1);
  }

  public synchronized void changeSize(Dimension extent) {
    if (!init) return;
    if (!profile.autoSize) return;

    if (extent.width < profile.fontWidth) extent.width = profile.fontWidth;
    if (extent.height < profile.fontHeight) extent.height = profile.fontHeight;
    int newsx = extent.width / profile.fontWidth;
    int newsy = extent.height / profile.fontHeight;
    y1 = 0;
    y2 = newsy-1;

    Char[] newChars = new Char[newsx*(newsy+scrollBack)];
    int sy2 = sy + scrollBack;
    int newsy2 = newsy + scrollBack;
    if (newsy2 > sy2) {
      for(int y=0;y<sy2;y++) {
        if (newsx > sx) {
          for(int x=0;x<sx;x++) newChars[y * newsx + x] = chars[y * sx + x];
          for(int x=sx;x<newsx;x++) newChars[y * newsx + x] = new Char(profile.foreColor, profile.backColor);
        } else {
          for(int x=0;x<newsx;x++) newChars[y * newsx + x] = chars[y * sx + x];
        }
      }
      for(int y=sy2;y<newsy2;y++) {
        for(int x=0;x<newsx;x++) newChars[y * newsx + x] = new Char(profile.foreColor, profile.backColor);
      }
    } else {
      for(int y=0;y<newsy2;y++) {
        if (newsx > sx) {
          for(int x=0;x<sx;x++) newChars[y * newsx + x] = chars[y * sx + x];
          for(int x=sx;x<newsx;x++) newChars[y * newsx + x] = new Char(profile.foreColor, profile.backColor);
        } else {
          for(int x=0;x<newsx;x++) newChars[y * newsx + x] = chars[y * sx + x];
        }
      }
    }
    synchronized(lock) {
      sx = newsx;
      sy = newsy;
      if (channel != null) {
        ssh_setPtyType();
      }
      if (profile.protocol.equals("local")) {
        pty_setsize();
      }
      if (cx >= sx) cx = sx-1;
      if (cy >= sy) cy = sy-1;
      chars = newChars;
      System.gc();
    }
    signalRepaint(true, true);
  }

  public void changeScrollBack(int newSize) {
    Char[] newChars = new Char[sx*(sy+newSize)];
    int diff, pos;
    if (newSize > scrollBack) {
      diff = newSize - scrollBack;
      for(int y=0;y<diff;y++) {for(int x=0;x<sx;x++) newChars[y * sx + x] = new Char(profile.foreColor, profile.backColor);}
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
    signalRepaint(true, true);
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
  public int getForeColor() { return foreColor.getRGB(); }
  public int getBackColor() { return backColor.getRGB(); }

  public void clrscr() {
    for(int pos=0;pos<sx * (sy+scrollBack);pos++) {
      chars[pos].ch = 0;
      chars[pos].fc = foreColor;
      chars[pos].bc = backColor;
      chars[pos].blink = false;
    }
    cx = 0;
    cy = 0;
    signalRepaint(true, false);
    if (profile.protocol.equals("local")) {
      pty_setsize();  //test
    }
  }

  public void print(String txt) {
    print(txt.toCharArray(), txt.length());
  }

  public void print(char[] buf, int buflen) {
    for(int a=0;a<buflen;a++) {
      if (script != null) {
        script.input(buf[a], this);
      }
      switch (buf[a]) {
        case 127:
        case 8:
          decPosX();
          break;
        case 9:
          int ts = (getx()-1) % profile.tabStops;
          for(int t=0;t<profile.tabStops - ts;t++) {
            if (eol) incPosX();
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
          if (buf[a] < 32) break;
          if (eol) incPosX();
          setChar(cx + 1, cy + 1, buf[a]);
          incPosX();
          break;
      }
    }
    signalRepaint(true, false);
  }

  public void print(char ch) {
    char[] x = new char[1];
    x[0] = ch;
    print(x, 1);
  }

  /** Sets a char in buffer.  (1,1 = top left) */
  public void setChar(int x, int y, char ch) {
    if (x < 1 || y < 1) {
      JFLog.logTrace("ERROR:invalid chords!");
    }
    x--;
    y--;
    int pos = ((y + scrollBack) * sx) + x;
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

  public int getsx() {return sx;}
  public int getsy() {return sy;}

  /** Get cursor x (1,1 = top left) */
  public int getx() {return cx + 1;}
  /** Get cursor y (1,1 = top left) */
  public int gety() {return cy + 1;}
  /** Set cursor pos (1,1 = top left) */
  public void gotoPos(int x, int y) {
    x--;
    y--;
    cx = x;
    if (cx < 0) cx = 0;
    if (cx >= sx) cx = sx-1;
    cy = y;
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
    synchronized(reader) {
      closed = true;
      connected = false;
    }
    signalRepaint(false, false);  //allow render thread to exit
    signalReconnect();  //allow reader thread to exit (if connection failed)
    try {if (pty != null) {pty.close(); pty = null;}} catch(Exception e) {}
    try {if (wincom != null) {wincom.close(); wincom = null;}} catch(Exception e) {}
    try {if (lnxcom != null) {lnxcom.close(); lnxcom = null;}} catch(Exception e) {}
    try {if (client != null) {client.close(); client = null;}} catch(Exception e) {}
    try {if (out != null) {out.close(); out = null;}} catch(Exception e) {}
    try {if (in != null) {in.close(); in = null;}} catch(Exception e) {}
    try {if (s != null) {s.close(); s = null;}} catch(Exception e) {}
    if (timer != null) {timer.cancel(); timer = null;}
  }

  public String getTermType() {
    return profile.termType;
  }

  //these methods are overloaded to allow such functionality
  public void nextTab() {System.out.println("nextTab");}
  public void prevTab() {System.out.println("prevTab");}
  public void setTab(int idx) {System.out.println("setTab" + idx);}
  public void setName(String str) {}

  /** This thread handles reading the input.*/
  private class Reader extends Thread {
    public void run() {
      while (!closed) {
        try {
          input("Connecting...");
          signalRepaint(true, false);  //show cursor
          if (doConnect() == false) {
            input("\r\nConnection failed! Press any key to retry...");
            signalRepaint(true, false);  //show cursor
            failed = true;
            synchronized(reader) {
              if (closed) break;
              try {reader.wait();} catch (Exception e) {}
            }
            input("\r\n");
            continue;
          }
          connected = true;
          input("" + ESC + "[2J");  //clrscr
          //keep reading in until it's disconnected
          byte[] buf = new byte[1024];
          int buflen;
          while (connected) {
            buflen = in.read(buf);
            if (buflen == -1) throw new Exception("read error");
            if (buflen > 0) input(byte2char(buf, buflen), buflen);
          }
        } catch (SocketException e) {
          //no log
          if (!closed) {
            ui.close();
          }
        } catch (Exception e) {
          JFLog.log(e);
          if (!closed) {
            ui.close();
          }
        }
      }
      JFLog.log("Reader thread done");
    }
  }

  public void signalReconnect() {
    synchronized(reader) { reader.notify(); }
  }

  public void signalRepaint(boolean findScreen, boolean revalidate) {
    if (ui != null) {
      ui.signalRepaint(findScreen, revalidate);
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
    signalRepaint(true, false);
  }

  private boolean doConnect() {
    if (profile.protocol.equals("com")) return connectCom();
    if (profile.protocol.equals("local")) return connectLocal();
    if (profile.protocol.equals("telnet")) return connectTelnet();
    if (profile.protocol.equals("ssh")) return connectSSH(parent);
    if (profile.protocol.equals("ssl")) return connectSSL();
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
  private boolean connectCom() {
    try {
//      if (!profile.hasComm) throw new Exception("no com support");
      String[] f = profile.host.split(",");  //com1,56000
      if (JF.isWindows()) {
        wincom = WinCom.open(f[0], JF.atoi(f[1]));
        if (wincom == null) return false;
        in = new InputStream() {
          public int read() throws IOException {
            byte[] data = new byte[1];
            int read = 0;
            do {
              read = wincom.read(data);
            } while (read != 1);
            return data[0];
          }
          public int read(byte[] buf) throws IOException {
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
          public void write(byte[] buf) throws IOException {
            wincom.write(buf);
          }
        };
      } else {
        lnxcom = LnxCom.open(f[0], JF.atoi(f[1]));
        if (lnxcom == null) return false;
        in = new InputStream() {
          public int read() throws IOException {
            byte[] data = new byte[1];
            int read;
            do {
              read = lnxcom.read(data);
            } while (read <= 0);
            return data[0];
          }
          public int read(byte[] buf) throws IOException {
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
          public void write(byte[] buf) throws IOException {
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

  private boolean connectLocal() {
    JFLog.log("connectLocal:" + profile.termApp);
    try {
      profile.termArgs[0] = profile.termApp;
      pty = LnxPty.exec(profile.termApp
        , profile.termArgs
        , LnxPty.makeEnvironment(new String[] {"TERM=" + profile.termType}));
      if (pty == null) throw new Exception("pty failed");
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
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      if (!closed) input(e.toString());
    }
    return false;
  }

  private boolean connectTelnet() {
    try {
      s = new Socket(profile.host, profile.port);
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

  private void ssh_setPtyType() {
    JFLog.log("TODO : set Pty Type");
    //channel.setPtyType(settings.termType, sx, sy, sx * 8, sy * 8);
  }

  private boolean connectSSH(Frame parent) {
    try{
      if (profile.sshKey.length() == 0) {
        if (profile.password.length() == 0) profile.password = GetPassword.getPassword(parent);
        if (profile.password == null) return false;
      }
      Object[] pipes;
      client = SshClient.setUpDefaultClient();
      client.start();
      JFLog.log("TODO : set knownhosts");
      //setKnownHosts(JF.getUserPath() + "/.jfterm-knownhosts");
      ConnectFuture cf = client.connect(profile.username, profile.host, profile.port);
      session = cf.verify().getSession();
      if (profile.x11) {
        JFLog.log("TODO : set X11 host / port");
        //session.setX11Host("127.0.0.1");
        //session.setX11Port(detectX11port());
      }
      if (profile.sshKey.length() == 0) {
        session.addPasswordIdentity(profile.password);
      } else {
        JFLog.log("SSH:using key:" + profile.sshKey);
        JFLog.log("TODO : set ssh key");
        //session.addPublicKeyIdentity(sd.sshKey);
      }
      session.auth().verify(30000);
      channel = session.createChannel(ClientChannel.CHANNEL_SHELL);
      if (profile.x11) {
        // Enable X11 forwarding
        //channel.setXForwarding(true);
        JFLog.log("TODO : enable x11 forwarding");
      } else {
        // Enable agent-forwarding
        //channel.setAgentForwarding(true);
        JFLog.log("TODO : enable agent forwarding");
      }
      pipes = createPipes();
      if (pipes == null) return false;
      out = (OutputStream)pipes[1];
      channel.setIn((InputStream)pipes[0]);
      pipes = createPipes();
      if (pipes == null) return false;
      in = (InputStream)pipes[0];
      channel.setOut((OutputStream)pipes[1]);
      channel.open();
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

  private boolean connectSSL() {
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
      ssl = (SSLSocket) sslsocketfactory.createSocket(profile.host, profile.port);
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
    return "Buffer:" + sx + "," + sy + "+" + scrollBack;
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
    signalRepaint(false, false);
  }
}

package javaforce.ansi.client;

/*
 * Buffer.java
 *
 * Created on May 10, 2019, 1:34 PM
 *
 * @author pquiring
 *
 */

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.io.*;
import javaforce.awt.*;
import javaforce.jni.lnx.*;

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
      sx = profile.sx;
      sy = profile.sy;
      if (sx < 80) {
        sx = 80;
      }
      if (sy < 25) {
        sy = 25;
      }
      JFLog.log("Screen Size:" + sx + "," + sy);
      y1 = 0;
      y2 = sy-1;
      int ty = sy+scrollBack;
      lines = new Line[ty];
      for(int y=0;y<ty;y++) {
        lines[y] = new Line(sx, profile.foreColor, profile.backColor);
      }
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
  public Line[] lines = null;
  public int cx, cy;  //cursor position (0,0 = top left)
  /** A timer to blink the cursor, the blinky text. */
  private java.util.Timer timer;
  private int foreColor;
  private int backColor;
  public boolean cursorShown = false;
  public int selectStartY = -1, selectEndY = -1;
  public int selectStartX = -1, selectEndX = -1;
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

  private void input(char ch) {
    char[] tmp = new char[1];
    tmp[0] = ch;
    input(tmp, 1);
  }

  private void input(char[] buf, int buflen) {
    if (fos != null) {
      byte[] tmp = char2byte(buf, buflen);
      JF.write(fos, tmp, 0, tmp.length);
    }
    synchronized(lock) {
      inputLocked(buf, buflen);
    }
  }

  private void inputLocked(char[] buf, int buflen) {
    //process Telnet/ANSI code
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

  private void setLines() {
    int oldLen = lines.length;
    int ty = sy + scrollBack;
    if (oldLen > ty) {
      //remove lines
      Line[] newLines = new Line[ty];
      for(int y=0;y<ty;y++) {
        newLines[y] = lines[y];
      }
      lines = newLines;
    } else {
      //add lines
      Line[] newLines = new Line[ty];
      for(int y=0;y<oldLen;y++) {
        newLines[y] = lines[y];
      }
      for(int y=oldLen;y<ty;y++) {
        newLines[y] = new Line(sx, foreColor, backColor);
      }
      lines = newLines;
    }
    for(int i=0;i<ty;i++) {
      Line line = lines[i];
      line.setlen(sx, foreColor, backColor);
    }
  }

  public synchronized void changeSize(Dimension extent) {
    if (!init) return;
    if (!profile.autoSize) return;

    if (extent.width < profile.fontWidth) extent.width = profile.fontWidth;
    if (extent.height < profile.fontHeight) extent.height = profile.fontHeight;
    int newsx = extent.width / profile.fontWidth;
    int newsy = extent.height / profile.fontHeight;

    synchronized(lock) {
      y1 = 0;
      y2 = sy-1;
      sx = newsx;
      sy = newsy;
      if (cx >= sx) cx = sx-1;
      if (cy >= sy) cy = sy-1;
      setLines();
      if (channel != null) {
        ssh_setPtyType();
      }
      if (profile.protocol.equals("local")) {
        pty_setsize();
      }
      System.gc();
    }
    clear_selection();
    signalRepaint(true, true);
  }

  public void changeScrollBack(int newSize) {
    synchronized(lock) {
      scrollBack = newSize;
      setLines();
    }
    clear_selection();
    signalRepaint(true, true);
  }

  public void setForeColor(int newClr) {
    foreColor = newClr & 0xffffff;
  }
  public void setBackColor(int newClr) {
    backColor = newClr & 0xffffff;
  }
  public void setBlinker(boolean state) {
    blinker = state;
  }
  public void setReverse(boolean state) {
    reverse = state;
  }
  public int getForeColor() { return foreColor; }
  public int getBackColor() { return backColor; }

  public void clrscr() {
    int ty = sy+scrollBack;
    for(int y=0;y<ty;y++) {
      lines[y].clear(foreColor, backColor);
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
      return;
    }
    x--;
    y--;
    Line line = lines[y + scrollBack];
    line.chs[x] = ch;
    if (reverse) {
      line.fcs[x] = backColor;
      line.bcs[x] = foreColor;
    } else {
      line.fcs[x] = foreColor;
      line.bcs[x] = backColor;
    }
    line.blinks[x] = blinker;
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
    for(int i=0;i<cnt;i++) {
      for(int y=y1 + scrollBack;y<y2 + scrollBack;y++) {
        lines[y] = lines[y+1];
      }
      lines[y2 + scrollBack] = new Line(sx, foreColor, backColor);
    }
    selectStartY = selectEndY = -1;
    selectStartX = selectEndX = -1;
  }
  public void scrollDown(int cnt) {
    for(int i=0;i<cnt;i++) {
      for(int y=y2 + scrollBack;y>y1 + scrollBack;y--) {
        lines[y] = lines[y-1];
      }
      lines[y1 + scrollBack] = new Line(sx, foreColor, backColor);
    }
    selectStartY = selectEndY = -1;
    selectStartX = selectEndX = -1;
  }

  public void delete() {
    Line line = lines[cy + scrollBack];
    for(int x=cx;x<sx-1;x++) {
      line.copy(x, x+1);
    }
    line.set(sx-1, foreColor, backColor);
  }

  public void insert() {
    Line line = lines[cy + scrollBack];
    for(int x=sx-2;x>=cx;x--) {
      line.copy(x-1, x);
    }
    line.set(cx, foreColor, backColor);
  }

  public void close() {
    synchronized(reader) {
      closed = true;
      connected = false;
    }
    signalRepaint(false, false);  //allow render thread to exit
    signalReconnect();  //allow reader thread to exit (if connection failed)
    try {if (pty != null) {pty.close(); pty = null;}} catch(Exception e) {}
    try {if (com != null) {com.close(); com = null;}} catch(Exception e) {}
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

  public boolean have_selection() {
    return selectStartY != -1 && selectEndY != -1;
  }

  public void clear_selection() {
    selectStartY = -1;
    selectStartX = -1;
    selectEndY = -1;
    selectEndX = -1;
  }

  public void swap_selection() {
    //swap start / end
    int tmpY = selectStartY;
    selectStartY = selectEndY;
    selectEndY = tmpY;
    int tmpX = selectStartX;
    selectStartX = selectEndX;
    selectEndX = tmpX;
  }

  public void copy() {
    if ((!have_selection())) return;
    try {
      StringBuilder str = new StringBuilder();
      if (selectStartY > selectEndY) {
        swap_selection();
      }
      for(int y=selectStartY;y<=selectEndY;y++) {
        Line line = lines[y];
        if (y == selectStartY) {
          int x1 = 0;
          int x2 = sx;
          if (y == selectEndY) {
            //partial line
            x1 = selectEndX;
          }
          for(int x=x1;x<x2;x++) {
            str.append(line.chs[x]);
          }
        } else if (y == selectEndY) {
          int x1 = 0;
          int x2 = sx;
          if (y == selectEndY) {
            //partial line
            x2 = selectEndX;
          }
          for(int x=x1;x<x2;x++) {
            str.append(line.chs[x]);
          }
        } else {
          //full line
          for(int x=0;x<sx;x++) {
            str.append(line.chs[x]);
          }
        }
        str.append("\n");
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

  private ComPort com;
  private boolean connectCom() {
    try {
      com = ComPort.open(profile.com, profile.baud);
      in = new InputStream() {
        public int read() throws IOException {
          byte[] data = new byte[1];
          int read = 0;
          do {
            read = com.read(data);
          } while (read != 1);
          return data[0];
        }
        public int read(byte[] buf) throws IOException {
          int read;
          do {
            read = com.read(buf);
          } while (read <= 0);
          return read;
        }
      };
      out = new OutputStream() {
        public void write(int b) throws IOException {
          com.write(new byte[] {(byte)b});
        }
        public void write(byte[] buf) throws IOException {
          com.write(buf);
        }
      };
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

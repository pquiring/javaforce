package javaforce.awt;

/** RFB Protocol (VNC) server/client.
 *
 * RFC : 6143
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import javaforce.*;

public class RFB {
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private boolean connected;

  private int width;
  private int height;
  private String name;
  private JFImage image;
  private int[] buffer;
  private PixelFormat pf;
  private int bytesPixel;
  private int clr;
  private int colors[] = new int[256];
  private int mx, my;  //mouse x,y

  private static int log;
  private static final int MAX_TEXT_LENGTH = 1024 * 1024;

  public static boolean debug;

  //pixel formats
  public static int PF_RGB = 1;  //Java
  public static int PF_BGR = 2;  //tightVNC

  public static class Rectangle {
    public Rectangle() {}
    public Rectangle(java.awt.Rectangle r) {
      this.x = r.x;
      this.y = r.y;
      this.width = r.width;
      this.height = r.height;
    }
    public Rectangle(int x, int y, int width, int height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
    }
    public Rectangle(Rectangle o) {
      this.x = o.x;
      this.y = o.y;
      this.width = o.width;
      this.height = o.height;
    }
    public int x;
    public int y;
    public int width;
    public int height;
    public boolean newSize;
    public boolean ptrPos;

    public void add(Rectangle rect) {
      if (width == 0 || height == 0) {
        x = rect.x;
        y = rect.y;
        width = rect.width;
        height = rect.height;
        return;
      }
      int rx1 = rect.x;
      int rx2 = rx1 + rect.width - 1;
      int ry1 = rect.y;
      int ry2 = ry1 + rect.height - 1;

      int tx1 = this.x;
      int tx2 = tx1 + this.width - 1;
      int ty1 = this.y;
      int ty2 = ty1 + this.height - 1;

      if (rx1 < tx1) {
        tx1 = rx1;
      }
      if (ry1 < ty1) {
        ty1 = ry1;
      }
      if (rx2 > tx2) {
        tx2 = rx2;
      }
      if (ry2 > ty2) {
        ty2 = ry2;
      }

      this.x = tx1;
      this.y = ty1;
      this.width = tx2 - tx1 + 1;
      this.height = ty2 - ty1 + 1;
    }

    public String toString() {
      return "Rectangle:" + x + "," + y + ":" + width + "x" + height;
    }
  }

  public static class PixelFormat {
    public int bpp;  //bits/pixel (8, 16, 32)
    public int depth;  //24
    public boolean be;  //big endian
    public boolean tc;  //true color
    public int r_max;
    public int g_max;
    public int b_max;
    public int r_shift;
    public int g_shift;
    public int b_shift;

    public int bytesPixel() {
      switch (bpp) {
        case 8: return 1;
        case 16: return 2;
        case 32: return 4;
      }
      return 0;
    }

    public void decode(byte[] pkt, int offset) {
      bpp = pkt[offset++] & 0xff;
      depth = pkt[offset++];
      be = pkt[offset++] == 1;
      tc = pkt[offset++] == 1;
      r_max = BE.getuint16(pkt, offset); offset += 2;
      g_max = BE.getuint16(pkt, offset); offset += 2;
      b_max = BE.getuint16(pkt, offset); offset += 2;
      r_shift = pkt[offset++];
      g_shift = pkt[offset++];
      b_shift = pkt[offset++];
      //13-15 = padding
    }

    public void encode(byte[] pkt, int offset) {
      pkt[offset++] = (byte)bpp;
      pkt[offset++] = (byte)depth;
      pkt[offset++] = (byte)(be ? 1 : 0);
      pkt[offset++] = (byte)(tc ? 1 : 0);
      BE.setuint16(pkt, offset, r_max); offset += 2;
      BE.setuint16(pkt, offset, g_max); offset += 2;
      BE.setuint16(pkt, offset, b_max); offset += 2;
      pkt[offset++] = (byte)r_shift;
      pkt[offset++] = (byte)g_shift;
      pkt[offset++] = (byte)b_shift;
    }

    public static PixelFormat create_24bpp() {
      PixelFormat pf = new PixelFormat();
      pf.bpp = 32;
      pf.depth = 24;
      pf.be = false;
      pf.tc = true;
      pf.r_max = 0xff;
      pf.g_max = 0xff;
      pf.b_max = 0xff;
      pf.r_shift = 16;
      pf.g_shift = 8;
      pf.b_shift = 0;
      return pf;
    }

    public int getFormat() {
      if (be) return PF_BGR;  //default
      return PF_RGB;
    }

    public String toString() {
      return "PixelFormat:" + bpp + "," + depth + ",be=" + be + ",tc=" + tc + ",r=" + r_shift + ",g=" + g_shift + ",b=" + b_shift;
    }
  }

  /** Convert RGB to BGR. */
  public static int[] swapPixelFormat(int[] rgb) {
    int len = rgb.length;
    int[] bgr = new int[len];
    int px, aa_gg, rr, bb;
    for(int i=0;i<len;i++) {
      px = rgb[i];
      //AA RR GG BB  ->  AA BB GG RR
      aa_gg = px & 0xff00ff00;
      rr = (px & 0xff0000) >> 16;
      bb = (px & 0xff) << 16;
      px = aa_gg | rr | bb;
      bgr[i] = px;
    }
    return bgr;
  }

  public static class RFBMouseEvent {
    public int x, y;
    public int buttons;
  }

  public static class RFBKeyEvent {
    public int code;
    public boolean down;
  }

  public static final int VK_BACK_SPACE = 0xff08;
  public static final int VK_TAB = 0xff09;
  public static final int VK_ENTER = 0xff0d;
  public static final int VK_ESCAPE = 0xff1b;
  public static final int VK_HOME = 0xff50;
  public static final int VK_LEFT = 0xff51;
  public static final int VK_UP = 0xff52;
  public static final int VK_RIGHT = 0xff53;
  public static final int VK_DOWN = 0xff54;
  public static final int VK_PAGE_UP = 0xff55;
  public static final int VK_PAGE_DOWN = 0xff56;
  public static final int VK_END = 0xff57;
  public static final int VK_INSERT = 0xff63;
  public static final int VK_CONTEXT_MENU = 0xff67;
  //numpad
  public static final int VK_NUMPAD_ENTER = 0xff8d;
  public static final int VK_NUMPAD_ASTERISK = 0xffaa;
  public static final int VK_NUMPAD_PLUS = 0xffab;
  public static final int VK_NUMPAD_PERIOD = 0xffae;
  public static final int VK_NUMPAD_MINUS = 0xffad;
  public static final int VK_NUMPAD_DIVIDE = 0xffaf;
  public static final int VK_NUMPAD0 = 0xffb0;
  public static final int VK_NUMPAD1 = 0xffb1;
  public static final int VK_NUMPAD2 = 0xffb2;
  public static final int VK_NUMPAD3 = 0xffb3;
  public static final int VK_NUMPAD4 = 0xffb4;
  public static final int VK_NUMPAD5 = 0xffb5;
  public static final int VK_NUMPAD6 = 0xffb6;
  public static final int VK_NUMPAD7 = 0xffb7;
  public static final int VK_NUMPAD8 = 0xffb8;
  public static final int VK_NUMPAD9 = 0xffb9;

  public static final int VK_F1 = 0xffbe;
  public static final int VK_F2 = 0xffbf;
  public static final int VK_F3 = 0xffc0;
  public static final int VK_F4 = 0xffc1;
  public static final int VK_F5 = 0xffc2;
  public static final int VK_F6 = 0xffc3;
  public static final int VK_F7 = 0xffc4;
  public static final int VK_F8 = 0xffc5;
  public static final int VK_F9 = 0xffc6;
  public static final int VK_F10 = 0xffc7;
  public static final int VK_F11 = 0xffc8;
  public static final int VK_F12 = 0xffc9;
  public static final int VK_SHIFT = 0xffe1;
  public static final int VK_SHIFT_R = 0xffe2;
  public static final int VK_CONTROL = 0xffe3;
  public static final int VK_CONTROL_R = 0xffe4;
  public static final int VK_META = 0xffe7;
  public static final int VK_META_R = 0xffe8;
  public static final int VK_ALT = 0xffe9;
  public static final int VK_ALT_R = 0xffea;
  public static final int VK_WIN_KEY = 0xffeb;
  public static final int VK_WIN_KEY_R = 0xffec;
  public static final int VK_DELETE = 0xffff;

  public static final int VK_EXCLAMATION_MASK = 0x0021;
  public static final int VK_AT = 0x0040;
  public static final int VK_NUMBER_SIGN = 0x0023;
  public static final int VK_DOLLAR_SIGN = 0x0024;
  public static final int VK_PERCENT = 0x0025;
  public static final int VK_CIRCUMFLEX = 0x005e;
  public static final int VK_AMPERSAND = 0x0026;
  public static final int VK_ASTERISK = 0x002a;
  public static final int VK_LEFT_PARENTHSIS = 0x0028;
  public static final int VK_RIGHT_PARENTHSIS = 0x0029;
  public static final int VK_UNDERSCORE = 0x005f;
  public static final int VK_PLUS = 0x002b;
  public static final int VK_QUOTE_LEFT = 0x0060;
  public static final int VK_TILDE = 0x007e;
  public static final int VK_OPEN_BRACKET = 0x007b;
  public static final int VK_CLOSE_BRACKET = 0x007d;
  public static final int VK_PIPE = 0x007c;
  public static final int VK_SEMICOLON = 0x003a;
  public static final int VK_DOUBLE_QUOTE = 0x0022;
  public static final int VK_LESS = 0x003c;
  public static final int VK_GREATER = 0x003e;
  public static final int VK_QUESTION_MARK = 0x003f;
  public static final int VK_QUOTE = 0x0027;

  public boolean connect(String host, int port) {
    try {
      s = new Socket(host, port);
      is = s.getInputStream();
      os = s.getOutputStream();
      connected = true;
    } catch (Exception e) {
      connected = false;
      JFLog.log(log, e);
      return false;
    }
    return true;
  }

  public boolean connect(Socket s) {
    try {
      this.s = s;
      is = s.getInputStream();
      os = s.getOutputStream();
      connected = true;
    } catch (Exception e) {
      connected = false;
      JFLog.log(log, e);
      return false;
    }
    return true;
  }

  public boolean isConnected() {
    return connected;
  }

  public void disconnect() {
    try {
      if ( s!= null) {
        s.close();
        s = null;
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  public static void setLog(int log) {
    RFB.log = log;
  }

  private void setSize() {
    if (debug) {
      JFLog.log("RFB:setSize:" + width + "x" + height);
    }
    image = new JFImage(width, height);
    buffer = image.getBuffer();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int[] getBuffer() {
    return buffer;
  }

  public void setBuffer(int[] px) {
    if (px.length != buffer.length) {
      JFLog.log("Error:RFB.setBuffer():px.length wrong size:" + px.length + "!=" + buffer.length);
      return;
    }
    System.arraycopy(px, 0, buffer, 0, px.length);
  }

  public JFImage getImage(Rectangle area) {
    JFImage image = new JFImage(area.width, area.height);
    image.putPixels(getBuffer(), 0, 0, area.width, area.height, area.y * width + area.x, width);
    return image;
  }

  public int getMouseX() {
    return mx;
  }

  public int getMouseY() {
    return my;
  }

  public String getDesktopName() {
    return name;
  }

  private static int getPixel(byte[] data, int offset) {
    int ret;
    ret = ((int) data[offset + 2] & 0xff) << 16;
    ret += ((int) data[offset + 1] & 0xff) << 8;
    ret += ((int) data[offset + 0] & 0xff);
    return ret;
  }

  private void fill(Rectangle rect, int clr) {
    if (debug) {
      JFLog.log(log, "RFB:fill:" + rect + ",clr=" + Integer.toString(clr, 16));
    }
    int x1 = rect.x;
    int x2 = x1 + rect.width - 1;
    int y1 = rect.y;
    int y2 = y1 + rect.height - 1;
    //clip rectangle
    if (x2 >= width) {
      x2 = width - 1;
    }
    if (y2 >= height) {
      y2 = height - 1;
    }
    clr |= JFImage.OPAQUE;
    for(int y = y1;y <= y2;y++) {
      for(int x = x1;x <= x2;x++) {
        buffer[y * width + x] = clr;
      }
    }
  }

  private int readByte() {
    try {
      int val = is.read();
      if (val == -1) throw new Exception("EOF");
      return val;
    } catch (Exception e) {
      connected = false;
      JFLog.log(log, e);
      return -1;
    }
  }

  private int readShort() {
    byte[] data = read(2);
    if (data == null) return -1;
    return BE.getuint16(data, 0);
  }

  private int readInt() {
    byte[] data = read(4);
    if (data == null) return -1;
    return BE.getuint32(data, 0);
  }

  private byte[] read(int len) {
    try {
      return is.readNBytes(len);
    } catch (Exception e) {
      connected = false;
      JFLog.log(log, e);
      return null;
    }
  }

  private int readCompactLen() {
    int[] portion = new int[3];
    portion[0] = readByte();
    int byteCount = 1;
    int len = portion[0] & 0x7f;
    if ((portion[0] & 0x80) != 0) {
      portion[1] = readByte();
      byteCount++;
      len |= (portion[1] & 0x7f) << 7;
      if ((portion[1] & 0x80) != 0) {
        portion[2] = readByte();
        byteCount++;
        len |= (portion[2] & 0xff) << 14;
      }
    }
    return len;
  }

  private void writeCompactLen(int length) {
    while (length > 0) {
      if (length >= 0x80) {
        writeByte((length & 0x7f) + 0x80);
      } else {
        writeByte(length & 0x7f);
      }
      length >>= 7;
    }
  }

  private void writeByte(int data) {
    try {
      os.write(data);
    } catch (Exception e) {
      connected = false;
      JFLog.log(log, e);
    }
  }

  private void writeShort(int data) {
    byte[] pkt = new byte[2];
    BE.setuint16(pkt, 0, data);
    write(pkt);
  }

  private void writeInt(int data) {
    byte[] pkt = new byte[4];
    BE.setuint32(pkt, 0, data);
    write(pkt);
  }

  private void write(byte[] data) {
    try {
      os.write(data);
    } catch (Exception e) {
      connected = false;
      JFLog.log(log, e);
    }
  }

  private void write(byte[] data, int offset, int length) {
    try {
      os.write(data, offset, length);
    } catch (Exception e) {
      connected = false;
      JFLog.log(log, e);
    }
  }

  public float readVersion() {
    //12 bytes : "RFB xxx.yyy\n"
    byte[] data = read(12);
    String major = new String(data, 4, 3);
    String minor = new String(data, 8, 3);
    float ver = Float.valueOf(major) + (Float.valueOf(minor) / 10f);
    if (debug) {
      JFLog.log(log, "RFB:read server version=" + ver);
    }
    return ver;
  }

  public static float VERSION_3_3 = 3.3f;  //1998
  public static float VERSION_3_7 = 3.7f;  //2003
  public static float VERSION_3_8 = 3.8f;  //2007

  public void writeVersion(float ver) {
    int major = (int)ver;
    int minor = (int)((ver - major + 0.01f) * 10f);
    String str = String.format("RFB %03d.%03d\n", major, minor);
    if (debug) {
      JFLog.log(log, "RFB:write client version=" + str);
    }
    byte[] pkt = str.getBytes();
    write(pkt);
  }

  public static final int AUTH_FAIL = 0;
  public static final int AUTH_NONE = 1;
  public static final int AUTH_VNC = 2;

  public byte[] readAuthTypes() {
    int cnt = readByte();
    if (debug) {
      JFLog.log(log, "RFB:read AuthType Count=" + cnt);
    }
    byte[] types = read(cnt);
    if (debug) {
      for(int a=0;a<types.length;a++) {
        JFLog.log(log, "RFB:read AuthType[]=" + types[a]);
      }
    }
    return types;
  }

  public void writeAuthTypes() {
    byte[] pkt = new byte[2];
    pkt[0] = 1;
    pkt[1] = AUTH_VNC;
    write(pkt);
  }

  public byte readAuthType() {
    return (byte)readByte();
  }

  public byte[] readAuthChallenge() {
    byte[] data = read(16);
    if (debug) {
      JFLog.log(log, "RFB:read challenge=" + data);
    }
    return data;
  }

  public byte[] writeAuthChallenge() {
    byte[] pkt = new byte[16];
    Random r = new Random();
    for(int a=0;a<16;a++) {
      pkt[a] = (byte)r.nextInt(256);
    }
    if (debug) {
      JFLog.log(log, "RFB:read challenge=" + pkt);
    }
    write(pkt);
    return pkt;
  }

  private static byte[] reverseBits(byte[] input) {
    byte[] output = new byte[input.length];
    for(int a=0;a<input.length;a++) {
      byte in = input[a];
      byte out = 0;
      for(int b=0;b<8;b++) {
        out <<= 1;
        if ((in & 0x01) == 0x01) {
          out |= 1;
        }
        in >>= 1;
      }
      output[a] = out;
    }
    return output;
  }

  /** Ensures password is the required 8 chars long. */
  public static String checkPassword(String password) {
    int len = password.length();
    if (len == 8) return password;
    if (len > 8) return password.substring(0, 8);
    byte[] buf = new byte[8];
    System.arraycopy(password.getBytes(), 0, buf, 0, len);
    //fill remainder with zeros
    for(int i = len;i<8;i++) {
      buf[i] = '0';
    }
    return new String(buf);
  }

  public static byte[] encodeResponse(byte[] challenge, byte[] password) {
    //VNC password must be 8 chars long
    byte[] password8 = checkPassword(new String(password)).getBytes();
    //VNC password key must be reversed
    byte[] r_password = reverseBits(password8);

    try {
      // Create DES Cipher key
      DESKeySpec desKeySpec = new DESKeySpec(r_password);
      SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
      SecretKey secretKey = keyFactory.generateSecret(desKeySpec);

      // Create DES Cipher instance (ECB, no padding)
      Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);

      // Encrypt response
      return cipher.doFinal(challenge);
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    return null;
  }

  public void writeAuthType(int type) {
    if (debug) {
      JFLog.log(log, "RFB:write AuthType=" + type);
    }
    writeByte(type);
  }

  public void writeAuthResponse(byte[] res) {
    if (debug) {
      JFLog.log(log, "RFB:write AuthResponse=" + res);
    }
    write(res);
  }

  public void writeAuthResult(boolean state) {
    int value = state == true ? 0 : 1;
    writeInt(value);
    if (value == 1) {
      writeInt(0);
    }
  }

  public boolean readAuthResult() {
    int res = readInt();
    if (debug) {
      JFLog.log(log, "RFB:read AuthResult=" + res);
    }
    if (res == 1) {
      int strlen = readInt();
      if (strlen > MAX_TEXT_LENGTH) {
        JFLog.log(log, "RFB:error strlen=" + strlen);
        connected = false;
        return false;
      }
      byte[] errmsg_bytes = read(strlen);
      String errmsg = new String(errmsg_bytes);
      connected = false;
      JFLog.log("RFB:errmsg=" + errmsg);
    }
    return res == 0;
  }

  public byte readClientInit() {
    return (byte)readByte();
  }

  public void writeClientInit(boolean shared) {
    if (debug) {
      JFLog.log(log, "RFB:write ClientInit:shared=" + shared);
    }
    writeByte((byte)(shared ? 1 : 0));
  }

  public boolean readServerInit() {
    byte[] pkt = read(20);
    width = BE.getuint16(pkt, 0);
    height = BE.getuint16(pkt, 2);
    setSize();
    {
      pf = new PixelFormat();
      pf.decode(pkt, 4);
      bytesPixel = pf.bytesPixel();
      if (debug) {
        JFLog.log(log, "RFB:read server pixel format=" + pf);
      }
    }
    int strlen = readInt();
    if (strlen > MAX_TEXT_LENGTH) {
      JFLog.log(log, "RFB:error strlen=" + strlen);
      connected = false;
      return false;
    }
    byte[] name_bytes = read(strlen);
    name = new String(name_bytes);
    if (debug) {
      JFLog.log("RFB:desktop name=" + name);
    }
    return true;
  }

  public boolean writeServerInit(int width, int height) {
    String name = JF.getHostname();
    if (name == null || name.length() == 0) name = "computer";
    int len = name.length();
    byte[] pkt = new byte[20 + 4 + len];
    {
      this.width = width;
      this.height = height;
      setSize();
      BE.setuint16(pkt, 0, width);
      BE.setuint16(pkt, 2, height);
      pf = PixelFormat.create_24bpp();
      pf.encode(pkt, 4);
    }
    {
      BE.setuint32(pkt, 20, len);
      System.arraycopy(name.getBytes(), 0, pkt, 24, len);
    }
    write(pkt);
    return true;
  }

  //client to server

  public static final int C_MSG_SET_PIXEL_FORMAT = 0;
  public static final int C_MSG_SET_ENCODING = 2;
  public static final int C_MSG_BUFFER_REQUEST = 3;
  public static final int C_MSG_KEY_EVENT = 4;
  public static final int C_MSG_MOUSE_EVENT = 5;
  public static final int C_MSG_CUT_TEXT = 6;

  public PixelFormat readPixelFormat() {
    PixelFormat pf = new PixelFormat();
    byte[] pkt = read(19);
    pf.decode(pkt, 3);
    return pf;
  }

  public void writePixelFormat() {
    if (debug) {
      JFLog.log(log, "RFB:write PixelFormat");
    }
    byte[] pkt = new byte[20];
    pkt[0] = C_MSG_SET_PIXEL_FORMAT;
    //1-3 = padding
    PixelFormat pf = PixelFormat.create_24bpp();
    pf.encode(pkt, 4);
    write(pkt);
  }

  private int[] encodings;

  public int[] readEncodings() {
    byte[] header = read(3);  //byte padding; short count;
    int count = BE.getuint16(header, 1);
    if (count < 0 || count > 128) {
      JFLog.log("RFB:Error:# encodings=" + count);
      return null;
    }
    encodings = new int[count];
    byte[] data = read(count * 4);
    for(int idx=0;idx<count;idx++) {
      encodings[idx] = BE.getuint32(data, idx * 4);
    }
    return encodings;
  }

  public boolean haveEncodings() {
    return encodings != null;
  }

  private boolean haveEncoding(int encoding) {
    for(int a=0;a<encodings.length;a++) {
      if (encodings[a] == encoding) return true;
    }
    return false;
  }

  private int bestEncoding() {
    if (haveEncoding(TYPE_TIGHT)) return TYPE_TIGHT;
    if (haveEncoding(TYPE_ZLIB)) return TYPE_ZLIB;
    //all clients MUST support TYPE_RAW
    return TYPE_RAW;
  }

  public void writeEncodings(int[] encodings) {
    if (debug) {
      JFLog.log(log, "RFB:write Encodings");
    }
    int count = encodings.length;
    byte[] pkt = new byte[4 + count * 4];
    pkt[0] = C_MSG_SET_ENCODING;
    BE.setuint16(pkt, 2, count);
    for(int a=0;a<count;a++) {
      BE.setuint32(pkt, 4 + a * 4, encodings[a]);
    }
    write(pkt);
  }

  /** Writes encoding with speed preference. */
  public void writeEncodingsFast() {
    //order in preference
    writeEncodings(new int[] {
      TYPE_HEXTILE,
      TYPE_COPY_RECT,
      TYPE_CORRE,
      TYPE_RRE,
      TYPE_RAW,
      TYPE_ZLIB,
      TYPE_TIGHT,
      //pseudo encodings (params)
      TYPE_JPEG_9,  //tight encoding jpeg level
      TYPE_DESKTOP_SIZE,
      TYPE_LAST_RECT,
      TYPE_POINTER_POS,
      TYPE_CURSOR,
    });
  }

  /** Writes encoding with low bandwidth preference. */
  public void writeEncodingsLean() {
    //order in preference
    writeEncodings(new int[] {
      TYPE_TIGHT,
      TYPE_ZLIB,
      TYPE_HEXTILE,
      TYPE_COPY_RECT,
      TYPE_CORRE,
      TYPE_RRE,
      TYPE_RAW,
      //pseudo encodings (params)
      TYPE_JPEG_9,  //tight encoding jpeg level
      TYPE_DESKTOP_SIZE,
      TYPE_LAST_RECT,
      TYPE_POINTER_POS,
      TYPE_CURSOR,
    });
  }

  public Rectangle readBufferUpdateRequest() {
    Rectangle rect = new Rectangle();
    byte[] pkt = read(9);
    //pkt[0] = flags (0=incremental)
    rect.x = BE.getuint16(pkt, 1);
    rect.y = BE.getuint16(pkt, 3);
    rect.width = BE.getuint16(pkt, 5);
    rect.height = BE.getuint16(pkt, 7);
    return rect;
  }

  public void writeBufferUpdateRequest(int x, int y, int width, int height, boolean incremental) {
    if (debug) {
      JFLog.log(log, "RFB:write BufferUpdateRequest");
    }
    byte[] pkt = new byte[10];
    pkt[0] = C_MSG_BUFFER_REQUEST;
    pkt[1] = (byte)(incremental ? 1 : 0);
    BE.setuint16(pkt, 2, x);
    BE.setuint16(pkt, 4, y);
    BE.setuint16(pkt, 6, width);
    BE.setuint16(pkt, 8, height);
    write(pkt);
  }

  public void writeKeyEvent(int code, boolean down) {
    if (debug) {
      JFLog.log(log, "RFB:write KeyEvent");
    }
    byte[] pkt = new byte[8];
    pkt[0] = C_MSG_KEY_EVENT;
    pkt[1] = (byte)(down ? 1 : 0);
    //pkt[2,3] = padding
    BE.setuint32(pkt, 4, code);
    write(pkt);
  }

  public RFBKeyEvent readKeyEvent() {
    RFBKeyEvent event = new RFBKeyEvent();
    byte[] pkt = read(7);
    event.down = (pkt[0] & 0x1) == 0x1;
    event.code = BE.getuint32(pkt, 3);
    return event;
  }

  public void writeMouseEvent(int x, int y, int buttons) {
    if (debug) {
      JFLog.log(log, "RFB:write MouseEvent");
    }
    byte[] pkt = new byte[6];
    pkt[0] = C_MSG_MOUSE_EVENT;
    pkt[1] = (byte)(buttons);
    BE.setuint16(pkt, 2, x);
    BE.setuint16(pkt, 4, y);
    write(pkt);
  }

  public RFBMouseEvent readMouseEvent() {
    RFBMouseEvent event = new RFBMouseEvent();
    byte[] pkt = read(5);
    event.buttons = pkt[0];
    event.x = BE.getuint16(pkt, 1);
    event.y = BE.getuint16(pkt, 3);
    return event;
  }

  public void writeCutText(String txt) {
    if (debug) {
      JFLog.log(log, "RFB:write CutText");
    }
    byte[] bytes = txt.getBytes();
    int len = 8 + bytes.length;
    byte[] pkt = new byte[len];
    pkt[0] = C_MSG_CUT_TEXT;
    BE.setuint32(pkt, 4, len);
    System.arraycopy(bytes, 0, pkt, 4, bytes.length);
    write(pkt);
  }

  //server to client

  public static final int S_MSG_CLOSE = -1;
  public static final int S_MSG_BUFFER_UPDATE = 0;
  public static final int S_MSG_COLOR_MAP = 1;
  public static final int S_MSG_BELL = 2;
  public static final int S_MSG_CUT_TEXT = 3;

  public int readMessageType() {
    if (!connected) {
      JFLog.log(log, "RFB:connection is closed");
      return S_MSG_CLOSE;
    }
    if (debug) {
      JFLog.log(log, "Reading Message Type");
    }
    int msg = readByte();
    if (debug) {
      JFLog.log(log, "RFB:read msg=" + msg);
    }
    return msg;
  }

  public Rectangle readBufferUpdate() {
    byte[] pkt = read(3);
    //pkt[0] = padding
    int count = BE.getuint16(pkt, 1);
    if (debug) {
      JFLog.log("RFB:read UpdateBufferCount=" + count);
    }
    Rectangle rect = new Rectangle();
    for(int a=0;a<count;a++) {
      readRectangle(rect);
    }
    return rect;
  }

  public void writeBufferUpdate(Rectangle rect, int encoding) {
    byte[] pkt = new byte[4];
    pkt[0] = S_MSG_BUFFER_UPDATE;
    pkt[1] = 0;  //padding
    BE.setuint16(pkt, 2, 1);  //count
    write(pkt);
    if (encoding == -1) encoding = bestEncoding();
    if (debug) {
      JFLog.log("encoding=" + encoding);
    }
    writeRectangle(rect, encoding);
  }

  //vnc encoding types                            // decode | encode
  public static final int TYPE_RAW = 0;           // yes    | yes
  public static final int TYPE_COPY_RECT = 1;     // yes    | no
  public static final int TYPE_RRE = 2;           // yes    | no
  public static final int TYPE_CORRE = 4;         // yes    | no
  public static final int TYPE_HEXTILE = 5;       // yes    | no
  public static final int TYPE_ZLIB = 6;          // yes    | yes
  public static final int TYPE_TIGHT = 7;         // yes    | yes
  public static final int TYPE_HEXTILE_ZLIB = 8;  // no     | no
  public static final int TYPE_ULTRA = 9;         // no     | no
  public static final int TYPE_ULTRA2 = 10;       // no     | no
  public static final int TYPE_TRLE = 15;         // no     | no
  public static final int TYPE_ZRLE = 16;         // no     | no
  public static final int TYPE_ZYWRLE = 17;       // no     | no

  //psuedo types
  public static final int TYPE_DESKTOP_SIZE = -223;
  public static final int TYPE_LAST_RECT = -224;
  public static final int TYPE_POINTER_POS = -232;
  public static final int TYPE_CURSOR = -239;

  //tight jpeg levels
  public static final int TYPE_JPEG_9 = -23;
  public static final int TYPE_JPEG_8 = -24;
  public static final int TYPE_JPEG_7 = -25;
  public static final int TYPE_JPEG_6 = -26;
  public static final int TYPE_JPEG_5 = -27;
  public static final int TYPE_JPEG_4 = -28;
  public static final int TYPE_JPEG_3 = -29;
  public static final int TYPE_JPEG_2 = -30;
  public static final int TYPE_JPEG_1 = -31;
  public static final int TYPE_JPEG_0 = -32;

  private void readRectangle(Rectangle full) {
    byte[] pkt = read(12);
    Rectangle rect = new Rectangle();
    rect.x = BE.getuint16(pkt, 0);
    rect.y = BE.getuint16(pkt, 2);
    rect.width = BE.getuint16(pkt, 4);
    rect.height = BE.getuint16(pkt, 6);
    full.add(rect);
    int encoding = BE.getuint32(pkt, 8);
    if (debug) {
      JFLog.log(log, "RFB:read RectType[]=" + encoding + ":rect=" + rect);
    }
    switch (encoding) {
      case TYPE_RAW: readRectRaw(rect); break;
      case TYPE_COPY_RECT: readRectRect(rect); break;
      case TYPE_RRE: readRectRRE(rect); break;
      case TYPE_CORRE: readRectCoRRE(rect); break;
      case TYPE_HEXTILE: readRectHexTile(rect); break;
      case TYPE_ZLIB: readRectZlib(rect); break;
      case TYPE_TIGHT: readRectTight(rect); break;
      case TYPE_HEXTILE_ZLIB: unsupported(encoding); break;
      case TYPE_TRLE: unsupported(encoding); break;
      case TYPE_ZRLE: unsupported(encoding); break;
      case TYPE_ZYWRLE: unsupported(encoding); break;
      case TYPE_CURSOR: readRectCursor(rect); break;
      case TYPE_DESKTOP_SIZE: readRectDesktopSize(rect); full.newSize = true; break;
      case TYPE_POINTER_POS: readPointerPos(rect); full.ptrPos = true; break;
      default: unsupported(encoding); break;
    }
  }

  private void unsupported(int type) {
    JFLog.log("Unsupported encoding:" + type);
  }

  private void readRectRaw(Rectangle rect) {
    byte[] data = read(rect.width * rect.height * 4);
    int src = 0;
    int px;
    int x1 = rect.x;
    int x2 = x1 + rect.width - 1;
    int y1 = rect.y;
    int y2 = y1 + rect.height - 1;
    int dst = y1 * width + x1;
    int stride = width - rect.width;
    for(int y = y1;y <= y2;y++) {
      for(int x = x1;x <= x2;x++) {
        px = getPixel(data, src) | JFImage.OPAQUE;
        buffer[dst] = px;
        src += 4;
        dst++;
      }
      dst += stride;
    }
  }

  private void readRectRect(Rectangle rect) {
    byte[] pkt = read(4);
    int srcx = BE.getuint16(pkt, 0);
    int srcy = BE.getuint16(pkt, 2);
    int x1 = rect.x;
    int x2 = x1 + rect.width - 1;
    int y1 = rect.y;
    int y2 = y1 + rect.height - 1;
    int sx = srcx;
    int sy = srcy;
    for(int y = y1;y <= y2;y++) {
      sx = srcx;
      for(int x = x1;x <= x2;x++) {
        buffer[y * width + x] = buffer[sy * width + sx];
        sx++;
      }
      sy++;
    }
  }

  private void readRectRRE(Rectangle rect) {
    byte[] pkt = read(8);
    int cnt = BE.getuint32(pkt, 0);
    int clr = getPixel(pkt, 4);
    fill(rect, clr);
    Rectangle subrect = new Rectangle();
    for(int a=0;a<cnt;a++) {
      pkt = read(12);
      clr = getPixel(pkt, 0);
      subrect.x = rect.x + BE.getuint16(pkt, 4);
      subrect.y = rect.y + BE.getuint16(pkt, 6);
      subrect.width = BE.getuint16(pkt, 8);
      subrect.height = BE.getuint16(pkt, 10);
      fill(subrect, clr);
    }
  }

  private void readRectCoRRE(Rectangle rect) {
    byte[] pkt = read(8);
    int cnt = BE.getuint32(pkt, 0);
    int clr = getPixel(pkt, 4);
    fill(rect, clr);
    Rectangle subrect = new Rectangle();
    for(int a=0;a<cnt;a++) {
      pkt = read(8);
      clr = getPixel(pkt, 0);
      subrect.x = rect.x + pkt[4] & 0xff;
      subrect.y = rect.y + pkt[5] & 0xff;
      subrect.width = pkt[6] & 0xff;
      subrect.height = pkt[7] & 0xff;
      fill(subrect, clr);
    }
  }

  private int hextile_bg = 0;
  private int hextile_fg = 0;

  private void readRectHexTile(Rectangle r) {
    hextile_bg = 0;
    hextile_fg = 0;
    for (int ty = r.y; ty < r.y + r.height; ty += 16) {
      int th = 16;
      if (r.y + r.height - ty < 16) {
        th = r.y + r.height - ty;
      }

      for (int tx = r.x; tx < r.x + r.width; tx += 16) {
        int tw = 16;
        if (r.x + r.width - tx < 16) {
          tw = r.x + r.width - tx;
        }
        hextileSubrect(new Rectangle(tx, ty, tw, th));
      }
    }
  }

  private final int HEXTILE_RAW = (1 << 0);  //1
  private final int HEXTILE_BACKGROUND_SPECIFIED = (1 << 1);  //2
  private final int HEXTILE_FOREGROUND_SPECIFIED = (1 << 2);  //4
  private final int HEXTILE_ANY_SUBRECTS = (1 << 3);  //8
  private final int HEXTILE_SUBRECTS_COLOURED = (1 << 4);  //16
  private final int HEXTILE_ZLIB_RAW = (1 << 5);  //32
  private final int HEXTILE_ZLIB = (1 << 6);  //64

  private void hextileSubrect(Rectangle sr) {
    int subencoding = readByte();

    if (debug) {
      JFLog.log(log, "RFB:HexTile:subType=0x" + Integer.toString(subencoding, 16));
    }

    if ((subencoding & HEXTILE_RAW) != 0) {
      readRectRaw(sr);
      return;
    }

    // Read and draw the background if specified.
    byte[] cbuf;
    if ((subencoding & HEXTILE_BACKGROUND_SPECIFIED) != 0) {
      if (bytesPixel == 1) {
        cbuf = read(1);
        hextile_bg = colors[cbuf[0] & 0xff];
      } else {
        cbuf = read(4);
        hextile_bg = getPixel(cbuf, 0);
      }
      if (debug) {
        JFLog.log("RFB:HexTile:bg=0x" + Integer.toString(hextile_bg, 16));
      }
    }
    fill(sr, hextile_bg);

    // Read the foreground color if specified.
    if ((subencoding & HEXTILE_FOREGROUND_SPECIFIED) != 0) {
      if (bytesPixel == 1) {
        cbuf = read(1);
        hextile_fg = colors[cbuf[0] & 0xff];
      } else {
        cbuf = read(4);
        hextile_fg = getPixel(cbuf, 0);
      }
      if (debug) {
        JFLog.log("RFB:HexTile:fg=0x" + Integer.toString(hextile_bg, 16));
      }
    }

    // Done with this tile if there is no sub-rectangles.
    if ((subencoding & HEXTILE_ANY_SUBRECTS) == 0) {
      return;
    }

    int nSubRects = readByte();

    if (debug) {
      JFLog.log("RFB:HexTile:SubRects=" + nSubRects);
    }

    int bufsize = nSubRects * 2;
    if ((subencoding & HEXTILE_SUBRECTS_COLOURED) != 0) {
      bufsize += nSubRects * bytesPixel;
    }
    byte[] buf = read(bufsize);

    int b1, b2, sx, sy, sw, sh;
    int i = 0;

    if ((subencoding & HEXTILE_SUBRECTS_COLOURED) == 0) {
      // Sub-rectangles are all of the same color.
      for (int j = 0; j < nSubRects; j++) {
        b1 = buf[i++] & 0xff;
        b2 = buf[i++] & 0xff;
        sx = sr.x + (b1 >> 4);
        sy = sr.y + (b1 & 0xf);
        sw = (b2 >> 4) + 1;
        sh = (b2 & 0xf) + 1;
        fill(new Rectangle(sx, sy, sw, sh), hextile_fg);
      }
    } else if (bytesPixel == 1) {
      // BGR233 (8-bit color) version for colored sub-rectangles.
      for (int j = 0; j < nSubRects; j++) {
        hextile_fg = colors[buf[i++] & 0xff];
        b1 = buf[i++] & 0xff;
        b2 = buf[i++] & 0xff;
        sx = sr.x + (b1 >> 4);
        sy = sr.y + (b1 & 0xf);
        sw = (b2 >> 4) + 1;
        sh = (b2 & 0xf) + 1;
        fill(new Rectangle(sx, sy, sw, sh), hextile_fg);
      }
    } else {
      // Full-color (24-bit) version for colored sub-rectangles.
      for (int j = 0; j < nSubRects; j++) {
        hextile_fg = getPixel(buf, i);
        i += 4;
        b1 = buf[i++] & 0xff;
        b2 = buf[i++] & 0xff;
        sx = sr.x + (b1 >> 4);
        sy = sr.y + (b1 & 0xf);
        sw = (b2 >> 4) + 1;
        sh = (b2 & 0xf) + 1;
        fill(new Rectangle(sx, sy, sw, sh), hextile_fg);
      }
    }
  }

  private Inflater zlibInflater;

  void readRectZlib(Rectangle r) {
    int nBytes = readInt();
    byte[] input = read(nBytes);

    if (zlibInflater == null) {
      zlibInflater = new Inflater();
    }
    zlibInflater.setInput(input);

    int x1 = r.x;
    int y1 = r.y;
    int x2 = x1 + r.width - 1;
    int y2 = y1 + r.height - 1;

    if (bytesPixel == 1) {
      //TODO : support 8bpp zlib
      byte[] pixels8 = null;
      for (int dy = y1; dy <= y2; dy++) {
        try {
          zlibInflater.inflate(pixels8, dy * width + x1, r.width);
        } catch (Exception e) {
          JFLog.log(log, e);
        }
      }
    } else {
      byte[] buf = new byte[r.width * 4];
      int offset;
      for (int dy = y1; dy <= y2; dy++) {
        try {
          int length = zlibInflater.inflate(buf);
          if (debug) {
            JFLog.log("inflate.length=" + length);
          }
        } catch (Exception e) {
          JFLog.log(log, e);
        }
        offset = dy * width + r.x;
        for (int i = 0; i < r.width; i++) {
          buffer[offset + i] = getPixel(buf, i*4) | JFImage.OPAQUE;
        }
      }
    }
  }

  private void readRectCursor(Rectangle r) {
    int cursorLen = r.width * r.height * bytesPixel;
    byte[] cursor = read(cursorLen);
    int bitmaskLen = ((r.width + 7) / 8) * r.height;
    byte[] bitmask = read(bitmaskLen);
  }

  private Inflater[] tightInflaters = new Inflater[4];

  //tight encoding sub-types
  private final static int TIGHT_FILTER_COPY = 0x00;
  private final static int TIGHT_FILTER_PALETTE = 0x01;
  private final static int TIGHT_FILTER_GRADIENT = 0x02;
  private final static int TIGHT_EXPLICIT_FILTER = 0x04;
  private final static int TIGHT_FILL = 0x08;
  private final static int TIGHT_JPEG = 0x09;
  private final static int TIGHT_MAXSUBENCODING = 0x09;

  private final static int TIGHT_MIN_TO_COMPRESS = 12;

  private void readRectTight(Rectangle r) {
    int comp_ctl = readByte();

    // Flush zlib streams if we are told to do so.
    for (int stream_id = 0; stream_id < 4; stream_id++) {
      if ((comp_ctl & 1) != 0 && tightInflaters[stream_id] != null) {
        tightInflaters[stream_id] = null;
      }
      comp_ctl >>= 1;
    }

    // Check correctness of subencoding value.
    if (comp_ctl > TIGHT_MAXSUBENCODING) {
      JFLog.log(log, "RFB:Incorrect tight subencoding: " + comp_ctl);
      connected = false;
      return;
    }

    if (debug) {
      JFLog.log(log, "RFB:Tight:SubType=" + comp_ctl);
    }

    // Handle solid-color rectangles.
    if (comp_ctl == TIGHT_FILL) {
      if (bytesPixel == 1) {
        int idx = readByte();
        clr = colors[idx];
      } else {
        byte[] buf = read(4);
        clr = getPixel(buf, 0);
      }
      fill(r, clr);
      return;
    }

    if (comp_ctl == TIGHT_JPEG) {
      byte[] jpegData = read(readCompactLen());
      JFImage tmp = new JFImage();
      if (!tmp.loadJPG(new ByteArrayInputStream(jpegData))) {
        JFLog.log(log, "RFB:Error:Unable to load JPEG image");
        return;
      }
      image.putJFImage(tmp, r.x, r.y);
      return;
    }

    // Read filter id and parameters.
    int numColors = 0, rowSize = r.width;
    byte[] palette8 = new byte[2];
    int[] palette24 = new int[256];
    boolean useGradient = false;
    if ((comp_ctl & TIGHT_EXPLICIT_FILTER) != 0) {
      int filter_id = readByte();
      if (filter_id == TIGHT_FILTER_PALETTE) {
        numColors = readByte() + 1;
        if (bytesPixel == 1) {
          if (numColors != 2) {
            JFLog.log(log, "RFB:Incorrect tight palette size: " + numColors);
            return;
          }
          palette8 = read(2);
        } else {
          byte[] buf = read(numColors * 3);  //TPIXEL
          for (int i = 0; i < numColors; i++) {
            palette24[i] = getPixel(buf, i * 3) | JFImage.OPAQUE;
          }
        }
        if (numColors == 2) {
          rowSize = (r.width + 7) / 8;
        }
      } else if (filter_id == TIGHT_FILTER_GRADIENT) {
        useGradient = true;
      } else if (filter_id != TIGHT_FILTER_COPY) {
        JFLog.log(log, "RFB:Incorrect tight filter id: " + filter_id);
        return;
      }
    }
    if (numColors == 0 && bytesPixel == 4) {
      rowSize *= 3;  //TPIXEL
    }

    // Read, optionally uncompress and decode data.
    int dataSize = r.height * rowSize;
    if (dataSize < TIGHT_MIN_TO_COMPRESS) {
      // Data size is small - not compressed with zlib.
      if (numColors != 0) {
        // Indexed colors.
        byte[] indexedData = read(dataSize);
        if (numColors == 2) {
          // Two colors.
          if (bytesPixel == 1) {
            decodeMonoData(r, indexedData, palette8);
          } else {
            decodeMonoData(r, indexedData, palette24);
          }
        } else {
          // 3..255 colors (assuming bytesPixel == 4).
          int i = 0;
          for (int dy = r.y; dy < r.y + r.height; dy++) {
            for (int dx = r.x; dx < r.x + r.width; dx++) {
              buffer[dy * width + dx] = palette24[indexedData[i++] & 0xff];
            }
          }
        }
      } else if (useGradient) {
        // "Gradient"-processed data
        byte[] buf = read(r.width * r.height * 3);  //TPIXEL
        decodeGradientData(r, buf);
      } else {
        // Raw truecolor data.
        if (bytesPixel == 1) {
          for (int dy = r.y; dy < r.y + r.height; dy++) {
            byte[] pixels8 = read(r.width);
            for(int a=0;a<pixels8.length;a++) {
              buffer[a] = pixels8[dy * width + r.x] | JFImage.OPAQUE;
            }
          }
        } else {
          byte[] buf;
          int i, offset;
          for (int dy = r.y; dy < r.y + r.height; dy++) {
            buf = read(r.width * 3);  //TPIXEL
            offset = dy * width + r.x;
            for (i = 0; i < r.width; i++) {
              buffer[offset + i] = getPixel(buf, i * 3) | JFImage.OPAQUE;
            }
          }
        }
      }
    } else {
      // Data was compressed with zlib.
      int zlibDataLen = readCompactLen();
      byte[] zlibData = read(zlibDataLen);
      int stream_id = comp_ctl & 0x03;
      if (tightInflaters[stream_id] == null) {
        tightInflaters[stream_id] = new Inflater();
      }
      Inflater myInflater = tightInflaters[stream_id];
      myInflater.setInput(zlibData);
      byte[] buf = new byte[dataSize];
      try {
        int length = myInflater.inflate(buf);
        if (debug) {
          JFLog.log("Tight.length=" + length);
        }
      } catch (Exception e) {
        JFLog.log(log, e);
        return;
      }

      if (numColors != 0) {
        // Indexed colors.
        if (numColors == 2) {
          // Two colors.
          if (bytesPixel == 1) {
            decodeMonoData(r, buf, palette8);
          } else {
            decodeMonoData(r, buf, palette24);
          }
        } else {
          // More than two colors (assuming bytesPixel == 4).
          int i = 0;
          for (int dy = r.y; dy < r.y + r.height; dy++) {
            for (int dx = r.x; dx < r.x + r.width; dx++) {
              buffer[dy * width + dx] = palette24[buf[i++] & 0xff];
            }
          }
        }
      } else if (useGradient) {
        // Compressed "Gradient"-filtered data (assuming bytesPixel == 4).
        decodeGradientData(r, buf);
      } else {
        // Compressed truecolor data.
        if (bytesPixel == 1) {
          int destOffset = r.y * width + r.x;
          for (int dy = 0; dy < r.height; dy++) {
            for(int x = 0;x<r.width; x++) {
              buffer[destOffset] = buf[dy * r.width + x] | JFImage.OPAQUE;
            }
            destOffset += width;
          }
        } else {
          int srcOffset = 0;
          int destOffset, i;
          for (int dy = 0; dy < r.height; dy++) {
            destOffset = (r.y + dy) * width + r.x;
            for (i = 0; i < r.width; i++) {
              buffer[destOffset + i] = getPixel(buf, srcOffset) | JFImage.OPAQUE;
              srcOffset += 3;  //TPIXEL
            }
          }
        }
      }
    }
  }

  void decodeMonoData(Rectangle r, byte[] src, byte[] palette) {
    int dx, dy, n;
    int i = r.y * width + r.x;
    int rowBytes = (r.width + 7) / 8;
    byte b;

    for (dy = 0; dy < r.height; dy++) {
      for (dx = 0; dx < r.width / 8; dx++) {
        b = src[dy * rowBytes + dx];
        for (n = 7; n >= 0; n--) {
          buffer[i++] = palette[b >> n & 1] | JFImage.OPAQUE;
        }
      }
      for (n = 7; n >= 8 - r.width % 8; n--) {
        buffer[i++] = palette[src[dy * rowBytes + dx] >> n & 1] | JFImage.OPAQUE;
      }
      i += (width - r.width);
    }
  }

  void decodeMonoData(Rectangle r, byte[] src, int[] palette) {
    int dx, dy, n;
    int i = r.y * width + r.x;
    int rowBytes = (r.width + 7) / 8;
    byte b;

    for (dy = 0; dy < r.height; dy++) {
      for (dx = 0; dx < r.width / 8; dx++) {
        b = src[dy * rowBytes + dx];
        for (n = 7; n >= 0; n--) {
          buffer[i++] = palette[b >> n & 1];
        }
      }
      for (n = 7; n >= 8 - r.width % 8; n--) {
        buffer[i++] = palette[src[dy * rowBytes + dx] >> n & 1];
      }
      i += (width - r.width);
    }
  }

  //
  // Decode data processed with the "Gradient" filter.
  //
  void decodeGradientData(Rectangle r, byte[] buf) {
    int dx, dy, c;
    byte[] prevRow = new byte[r.width * 3];
    byte[] thisRow = new byte[r.width * 3];
    byte[] pix = new byte[3];
    int[] est = new int[3];

    int offset = r.y * width + r.x;

    for (dy = 0; dy < r.height; dy++) {

      /* First pixel in a row */
      for (c = 0; c < 3; c++) {
        pix[c] = (byte) (prevRow[c] + buf[dy * r.width * 3 + c]);
        thisRow[c] = pix[c];
      }
      buffer[offset++] = getPixel(pix, 0);

      /* Remaining pixels of a row */
      for (dx = 1; dx < r.width; dx++) {
        for (c = 0; c < 3; c++) {
          est[c] = ((prevRow[dx * 3 + c] & 0xff) + (pix[c] & 0xff) - (prevRow[(dx - 1) * 3 + c] & 0xff));
          if (est[c] > 0xff) {
            est[c] = 0xff;
          } else if (est[c] < 0x00) {
            est[c] = 0x00;
          }
          pix[c] = (byte) (est[c] + buf[(dy * r.width + dx) * 3 + c]);
          thisRow[dx * 3 + c] = pix[c];
        }
        buffer[offset++] = getPixel(pix, 0);
      }

      System.arraycopy(thisRow, 0, prevRow, 0, r.width * 3);
      offset += (width - r.width);
    }
  }

  private void readRectDesktopSize(Rectangle r) {
    //framebuffer size changed
    width = r.width;
    height = r.height;
    setSize();
  }

  private void readPointerPos(Rectangle r) {
    mx = r.x;
    my = r.y;
    if (debug) {
      JFLog.log("Pointer Pos:" + mx + "," + my);
    }
  }

  private void writeRectangle(Rectangle rect, int encoding) {
    byte[] pkt = new byte[12];
    BE.setuint16(pkt, 0, rect.x);
    BE.setuint16(pkt, 2, rect.y);
    BE.setuint16(pkt, 4, rect.width);
    BE.setuint16(pkt, 6, rect.height);
    BE.setuint32(pkt, 8, encoding);
    write(pkt);
    switch (encoding) {
      case TYPE_RAW: writeRectRaw(rect); break;
      case TYPE_ZLIB: writeRectZlib(rect); break;
      case TYPE_TIGHT: writeRectTight(rect); break;
      case TYPE_DESKTOP_SIZE: writeRectDesktopSize(rect); break;
    }
  }

  private void writeRectDesktopSize(Rectangle rect) {
    this.width = rect.width;
    this.height = rect.height;
    setSize();
  }

  private void readPixel(int src, byte[] out, int dst) {
    int px = buffer[src];
    out[dst + 0] = (byte)(px & 0xff);
    px >>>= 8;
    out[dst + 1] = (byte)(px & 0xff);
    px >>>= 8;
    out[dst + 2] = (byte)(px & 0xff);
  }

  private void writeRectRaw(Rectangle rect) {
    byte[] data = new byte[rect.width * rect.height * 4];
    int src = 0;
    int dst = 0;
    int x1 = rect.x;
    int x2 = x1 + rect.width - 1;
    int y1 = rect.y;
    int y2 = y1 + rect.height - 1;
    int stride = width - rect.width;
    for(int y = y1;y <= y2;y++) {
      for(int x = x1;x <= x2;x++) {
        readPixel(src, data, dst);
        src++;
        dst += 4;
      }
      src += stride;
    }
    write(data);
  }

  private Deflater zlibDeflater;

  private void writeRectZlib(Rectangle rect) {
    if (zlibDeflater == null) {
      zlibDeflater = new Deflater();
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    byte[] input = new byte[rect.width * 4];
    byte[] output = new byte[input.length + 128];

    int x1 = rect.x;
    int y1 = rect.y;
    int x2 = x1 + rect.width - 1;
    int y2 = y1 + rect.height - 1;
    int src_idx = y1 * width + x1;
    int dst_idx = 0;
    int stride = width - rect.width;
    for(int y=y1;y<=y2;y++) {
      dst_idx = 0;
      for(int x=x1;x<=x2;x++) {
        readPixel(src_idx, input, dst_idx);
        src_idx++;
        dst_idx += 4;
      }
      src_idx += stride;
      zlibDeflater.setInput(input);
      int length = zlibDeflater.deflate(output, 0, output.length, Deflater.SYNC_FLUSH);
      if (debug) {
        JFLog.log("length=" + length);
      }
      baos.write(output, 0, length);
    }

    byte[] out = baos.toByteArray();

    writeInt(out.length);
    write(out);
  }

  private void writeRectTight(Rectangle r) {
    int x1 = r.x;
    int x2 = x1 + r.width - 1;
    int y1 = r.y;
    int y2 = y1 + r.height - 1;
    int size = r.width * r.height * 3;
    writeByte(0);  //comp_ctl
    if (size < TIGHT_MIN_TO_COMPRESS) {
      //just write pixels raw
      byte[] raw = new byte[size];
      int src = y1 * width + x1;
      int dst = 0;
      int stride = width - r.width;
      for(int y=y1;y<=y2;y++) {
        for(int x=x1;x<=x2;x++) {
          readPixel(src, raw, dst);
          src++;
          dst += 3;
        }
        src += stride;
      }
      write(raw);
    } else {
      if (zlibDeflater == null) {
        zlibDeflater = new Deflater();
      }
      byte[] raw = new byte[size];
      int src = y1 * width + x1;
      int dst = 0;
      int stride = width - r.width;
      for(int y=y1;y<=y2;y++) {
        for(int x=x1;x<=x2;x++) {
          readPixel(src, raw, dst);
          src++;
          dst += 3;
        }
        src += stride;
      }
      byte[] output = new byte[size * 2];
      zlibDeflater.setInput(raw);
      int length = zlibDeflater.deflate(output, 0, output.length, Deflater.SYNC_FLUSH);
      writeCompactLen(length);
      write(output, 0, length);
    }
  }

  /** Reads color map. */
  public byte[] readColorMap() {
    byte[] pkt = read(5);
    int first = BE.getuint16(pkt, 1);
    int count = BE.getuint16(pkt, 3);
    if (debug) {
      JFLog.log(log, "RFB:read ColorMap:first=" + first + ",count=" + count);
    }
    byte[] clrs = read(count * 6);  //RGB array
    if (debug) {
      JFLog.log(log, "RFB:read ColorMap:array=" + clrs);
    }
    return clrs;
  }

  public void readBell() {
    //there is no data
  }

  public String readCutText() {
    byte[] pkt = read(7);
    int strlen = BE.getuint32(pkt, 3);
    if (strlen > MAX_TEXT_LENGTH) {
      connected = false;
      return null;
    }
    byte[] bytes = read(strlen);
    return new String(bytes);
  }
}

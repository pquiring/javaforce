package javaforce;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

/**
 * A collection of static methods. Many methods help reduce try/catch clutter by
 * returning error codes.
 *
 * @author Peter Quiring
 */

public class JF {

  public static String getVersion() {
    return "9.0.0";
  }

  public static void sleep(int milli) {
    try {
      Thread.sleep(milli);
    } catch (InterruptedException e) {
      JFLog.log(e);
    }
  }

  public static void msg(String msg) {
    java.lang.System.out.println(msg);
  }

  public static URL createURL(String url) {
    try {
      return new URI(url).toURL();
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static String readURL(String url) {
    String str = "";
    byte data[] = new byte[1024];
    int read;
    String purl = "";  //percent URL
    char ch;
    for (int p = 0; p < url.length(); p++) {
      ch = url.charAt(p);
      switch (ch) {
        case ' ':
          purl += "%20";
          break;
        case '%':
          purl += "%25";
          break;
        case '<':
          purl += "%3c";
          break;
        case '>':
          purl += "%3e";
          break;
        default:
          purl += ch;
          break;
      }
    }
    try {
      URL u = new URI(purl).toURL();
      HttpURLConnection huc = (HttpURLConnection) u.openConnection();
      huc.setRequestMethod("GET");
      huc.connect();
      if (huc.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return "huc error : " + huc.getResponseCode();  //ohoh
      }
      InputStream is = huc.getInputStream();
      while (true) {
        read = is.read(data);
        if (read <= 0) {
          break;
        }
        str += new String(data, 0, read);
      }
      is.close();
      huc.disconnect();
      return str;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static int atoi(String str) {
    if (str.length() == 0) {
      return 0;
    }
    try {
      if (str.charAt(0) == '+') {
        return Integer.parseInt(str.substring(1));
      }
      return Integer.parseInt(str);
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public static int atox(String str) {
    if (str.length() == 0) {
      return 0;
    }
    try {
      return Integer.parseInt(str, 16);
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public static boolean isWindows() {
    return (File.separatorChar == '\\');
  }

  public static boolean isUnix() {
    return (File.separatorChar == '/');
  }

  public static String getUserPath() {
    /*
     if (isWindows()) return System.getenv("USERPROFILE");
     return "~";
     */
    return System.getProperty("user.home");
  }

  public static String getCurrentUser() {
    return System.getenv("USER");
  }
  private static String user_dir = null;

  public static synchronized String getCurrentPath() {
    if (user_dir == null) {
      user_dir = System.getProperty("user.dir");
    }
    return user_dir;
  }

  /**
   * This just changes the internal value returned from getCurrentPath()
   */
  public static void setCurrentPath(String new_dir) {
    user_dir = new_dir;
  }
//file IO helper functions (these are little-endian format!!!)

  public static boolean eof(InputStream f) {
    try {
      return (!(f.available() > 0));
    } catch (Exception e) {
      JFLog.log(e);
      return true;
    }
  }

  public static int filelength(InputStream is) {
    try {
      return is.available();
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public static boolean fileskip(InputStream is, int length) {
    try {
      is.skip(length);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean fileflush(BufferedOutputStream bos) {
    try {
      bos.flush();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static FileInputStream fileopen(String name) {
    try {
      return new FileInputStream(name);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static FileOutputStream filecreate(String name) {
    try {
      return new FileOutputStream(name);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static FileOutputStream filecreateappend(String name) {
    try {
      return new FileOutputStream(name, true);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static boolean fileclose(InputStream is) {
    try {
      is.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean fileclose(OutputStream os) {
    try {
      os.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static byte[] read(InputStream in, int max) {
    byte ret[] = new byte[max];
    try {
      int read = in.read(ret);
      if (read == max) {
        return ret;
      }
      if (read == 0) {
        return null;
      }
      byte ret2[] = new byte[read];
      System.arraycopy(ret, 0, ret2, 0, read);
      return ret2;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static byte[] readAll(InputStream in) {
    try {
      int len = in.available();
      byte ret[] = new byte[len];
      int pos = 0;
      while (pos < len) {
        int read = in.read(ret, pos, len - pos);
        if (read <= 0) {
          return null;
        }
        pos += read;
      }
      return ret;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static byte[] readAll(InputStream in, int len) {
    try {
      byte ret[] = new byte[len];
      int pos = 0;
      while (pos < len) {
        int read = in.read(ret, pos, len - pos);
        if (read <= 0) {
          return null;
        }
        pos += read;
      }
      return ret;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static boolean readAll(InputStream in, byte buf[], int pos, int len) {
    int end = pos + len;
    try {
      while (pos < end) {
        int read = in.read(buf, pos, len);
        if (read <= 0) {
          return false;
        }
        pos += read;
        len -= read;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean readAll(RandomAccessFile in, byte buf[], int pos, int len) {
    int end = pos + len;
    try {
      while (pos < end) {
        int read = in.read(buf, pos, len);
        if (read <= 0) {
          return false;
        }
        pos += read;
        len -= read;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static byte[] readAll(InputStream in, Socket s) {
    try {
      byte tmp[] = new byte[1500];
      byte ret[] = new byte[0];
      while (in.available() > 0 || s.isConnected()) {
        int read = in.read(tmp);
        if (read <= 0) {
          return ret;
        }
        int pos = ret.length;
        ret = copyOf(ret, pos + read);
        System.arraycopy(tmp, 0, ret, pos, read);
      }
      return ret;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static int read(InputStream in, byte[] buf, int pos, int len) {
    try {
      return in.read(buf, pos, len);
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  public static int read(InputStream in) {
    try {
      return in.read();
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  public static char readchar(InputStream in) {
    try {
      return (char) in.read();
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  public static int readuint8(InputStream in) {
    byte data[] = new byte[1];
    try {
      if (in.read(data) != 1) {
        return -1;
      }
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
    return (int) data[0] & 0xff;
  }

  public static int readuint16(InputStream in) {
    byte data[] = new byte[2];
    try {
      if (in.read(data) != 2) {
        return -1;
      }
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
    int ret;
    ret = (int) data[0] & 0xff;
    ret += ((int) data[1] & 0xff) << 8;
    return ret;
  }

  public static int readuint32(InputStream in) {
    byte data[] = new byte[4];
    try {
      if (in.read(data) != 4) {
        return -1;
      }
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
    int ret;
    ret = (int) data[0] & 0xff;
    ret += ((int) data[1] & 0xff) << 8;
    ret += ((int) data[2] & 0xff) << 16;
    ret += ((int) data[3] & 0xff) << 24;
    return ret;
  }

  public static long readuint64(InputStream in) {
    byte data[] = new byte[8];
    try {
      if (in.read(data) != 8) {
        return -1;
      }
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
    long ret;
    ret = (long) data[0] & 0xff;
    ret += ((long) data[1] & 0xff) << 8;
    ret += ((long) data[2] & 0xff) << 16;
    ret += ((long) data[3] & 0xff) << 24;
    ret += ((long) data[4] & 0xff) << 32;
    ret += ((long) data[5] & 0xff) << 40;
    ret += ((long) data[6] & 0xff) << 48;
    ret += ((long) data[7] & 0xff) << 56;
    return ret;
  }

  public static float readfloat(InputStream in) {
    int bits = JF.readuint32(in);
    return Float.intBitsToFloat(bits);
  }

  public static double readdouble(InputStream in) {
    long bits = JF.readuint64(in);
    return Double.longBitsToDouble(bits);
  }

  public static boolean write(OutputStream out, byte data[]) {
    try {
      out.write(data);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean write(OutputStream out, String str) {
    return write(out, str.getBytes());
  }

  public static boolean write(OutputStream out, byte data[], int offset, int length) {
    try {
      out.write(data, offset, length);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean writeuint8(OutputStream out, int data) {
    try {
      out.write(data);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public static boolean writeuint16(OutputStream out, int data) {
    try {
      out.write(data & 0xff);
      data >>= 8;
      out.write(data & 0xff);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public static boolean writeuint32(OutputStream out, int data) {
    try {
      out.write(data & 0xff);
      data >>= 8;
      out.write(data & 0xff);
      data >>= 8;
      out.write(data & 0xff);
      data >>= 8;
      out.write(data & 0xff);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public static boolean writeuint64(OutputStream out, long data) {
    try {
      for (int a = 0; a < 8; a++) {
        out.write((int) (data & 0xff));
        data >>= 8;
      }
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public static boolean writefloat(OutputStream out, float data) {
    int bits = Float.floatToIntBits(data);
    return writeuint32(out, bits);
  }

  public static boolean writedouble(OutputStream out, double data) {
    long bits = Double.doubleToLongBits(data);
    return writeuint64(out, bits);
  }

  public static boolean copyAll(InputStream is, OutputStream os) {
    try {
      int len = is.available();
      byte buf[] = new byte[1024];
      int copied = 0;
      while (copied < len) {
        int read = is.read(buf);
        if (read == 0) {
          continue;
        }
        if (read == -1) {
          return false;
        }
        os.write(buf, 0, read);
        copied += read;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean copyAll(String src, String dst) {
    try {
      FileInputStream fis = new FileInputStream(src);
      FileOutputStream fos = new FileOutputStream(dst);
      boolean success = copyAll(fis, fos);
      fis.close();
      fos.close();
      return success;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean copyAll(InputStream is, OutputStream os, long length) {
    try {
      byte buf[] = new byte[1024];
      int copied = 0;
      while (copied < length) {
        int read = is.read(buf);
        if (read == 0) {
          continue;
        }
        if (read == -1) {
          return false;
        }
        os.write(buf, 0, read);
        copied += read;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean wildcardCompare(String fnstr, String wcstr, boolean caseSensitive) {
    //wc = "*.txt"
    //fn = "filename.txt"
    char fn[] = fnstr.toCharArray();
    char wc[] = wcstr.toCharArray();
    int fnp = 0, wcp = 0;
    char wcc, fnc;
    int sl, a;
    while ((fnp < fn.length) && (wcp < wc.length)) {
      if (wc[wcp] == '*') {
        sl = fn.length - fnp;
        //try 0-sl chars of fn
        wcp++;
        for (a = 0; a <= sl; a++) {
          if (wildcardCompare(
                  new String(fn, fnp, fn.length - fnp),
                  new String(fn, fnp, a) + new String(wc, wcp, wc.length - wcp),
                  caseSensitive)) {
            return true;
          }
        }
        return false;
      }
      if (wc[wcp] == '?') {
        //try 0-1 chars of fn
        wcp++;
        for (a = 0; a <= 1; a++) {
          if (wildcardCompare(
                  new String(fn, fnp, fn.length - fnp),
                  new String(fn, fnp, a) + new String(wc, wcp, wc.length - wcp),
                  caseSensitive)) {
            return true;
          }
        }
        return false;
      }
      if (caseSensitive) {
        fnc = fn[fnp];
        wcc = wc[wcp];
      } else {
        fnc = Character.toUpperCase(fn[fnp]);
        wcc = Character.toUpperCase(wc[wcp]);
      }
      if (fnc != wcc) {
        break; //no match
      }
      fnp++;
      wcp++;
    };
    while ((wcp < wc.length) && ((wc[wcp] == '*') || (wc[wcp] == '?'))) {
      wcp++;
    }
    if (wcp < wc.length) {
      return false;  //wc not used up
    }
    if (fnp < fn.length) {
      return false;  //fn not used up
    }
    return true;
  }
//String char[] functions (StringBuffer is awkward) (these are NOT null terminating)

  public static char[] createstr(String str) {
    return str.toCharArray();
  }

  public static char[] truncstr(char str[], int newlength) {
    if (newlength == 0) {
      return null;
    }
    char ret[] = new char[newlength];
    if (newlength <= str.length) {
      System.arraycopy(str, 0, ret, 0, ret.length);
    } else {
      System.arraycopy(str, 0, ret, 0, str.length);
      for (int a = str.length; a < ret.length; a++) {
        ret[a] = 0;
      }
    }
    return ret;
  }

  public static char[] strcpy(char str[]) {
    char ret[] = new char[str.length];
    System.arraycopy(str, 0, ret, 0, str.length);
    return ret;
  }

  public static char[] strcat(char s1[], char s2[]) {
    if (s1 == null) {
      s1 = new char[0];
    }
    char ret[] = new char[s1.length + s2.length];
    System.arraycopy(s1, 0, ret, 0, s1.length);
    System.arraycopy(s2, 0, ret, s1.length, s2.length);
    return ret;
  }

  public static char[] strcat(char str[], char ch) {
    char ret[];
    if (str == null) {
      ret = new char[1];
      ret[0] = ch;
      return ret;
    }
    ret = new char[str.length + 1];
    System.arraycopy(str, 0, ret, 0, str.length);
    ret[str.length] = ch;
    return ret;
  }

  public static String createString(char str[]) {
    return new String(str);
  }

  public static boolean strcmp(String s1, String s2) {
    return s1.equals(s2);
  }

  public static boolean stricmp(String s1, String s2) {
    return s1.equalsIgnoreCase(s2);
  }

  public static boolean strcmp(char s1[], char s2[]) {
    return strcmp(new String(s1), new String(s2));
  }

  public static boolean stricmp(char s1[], char s2[]) {
    return stricmp(new String(s1), new String(s2));
  }

  public static void memcpy(Object src[], int srcpos, Object dest[], int destpos, int len) {
    System.arraycopy(src, srcpos, dest, destpos, len);
  }

  public static boolean memcmp(byte m1[], int m1pos, byte[] m2, int m2pos, int len) {
    for (int a = 0; a < len; a++) {
      if (m1[m1pos + a] != m2[m2pos + a]) {
        return false;
      }
    }
    return true;
  }

  public static boolean memicmp(byte m1[], int m1pos, byte[] m2, int m2pos, int len) {
    char c1, c2;
    for (int a = 0; a < len; a++) {
      c1 = Character.toUpperCase((char) m1[m1pos + a]);
      c2 = Character.toUpperCase((char) m2[m2pos + a]);
      if (c1 != c2) {
        return false;
      }
    }
    return true;
  }
//executable JAR functions

  public static String getJARPath() {
    //this will equal your executable JAR filename (like argv[0] in C++)
    return System.getProperty("java.class.path");
  }
//misc functions

  public static void randomize(Random random) {
    random.setSeed(System.currentTimeMillis());
  }
//endian functions

  public static short endian(short x) {
    short ret = (short) (x << 8);
    ret += x >>> 8;
    return ret;
  }

  public static int endian(int x) {
    return (x << 24)
            | ((x << 8) & 0xff0000)
            | ((x >> 8) & 0xff00)
            | (x >>> 24);
  }

  public static long endian(long x) {
    return (x << 56)
            | ((x << 40) & 0xff000000000000L)
            | ((x << 24) & 0xff0000000000L)
            | ((x << 8) & 0xff00000000L)
            | ((x >> 8) & 0xff000000L)
            | ((x >> 24) & 0xff0000L)
            | ((x >> 40) & 0xff00L)
            | (x >>> 56);
  }

  //java5 doesn't provide this function

  public static byte[] copyOf(byte data[], int newLength) {
    byte array[] = new byte[newLength];
    System.arraycopy(data, 0, array, 0, newLength < data.length ? newLength : data.length);
    return array;
  }

  //copies arrays excluding one element (something Arrays is missing)

  public static Object[] copyOfExcluding(Object[] array, int idx) {
    Class cls = array.getClass().getComponentType();
    Object newArray[] = (Object[]) Array.newInstance(cls, array.length - 1);
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static boolean[] copyOfExcluding(boolean[] array, int idx) {
    boolean newArray[] = new boolean[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static byte[] copyOfExcluding(byte[] array, int idx) {
    byte newArray[] = new byte[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static short[] copyOfExcluding(short[] array, int idx) {
    short newArray[] = new short[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static int[] copyOfExcluding(int[] array, int idx) {
    int newArray[] = new int[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static float[] copyOfExcluding(float[] array, int idx) {
    float newArray[] = new float[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  public static double[] copyOfExcluding(double[] array, int idx) {
    double newArray[] = new double[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }
}

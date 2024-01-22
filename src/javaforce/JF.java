package javaforce;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.lang.reflect.*;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.nio.file.Files;
import java.nio.file.*;

/**
 * A collection of useful static methods.
 *
 * @author Peter Quiring
 */
public class JF {

  public static String getVersion() {
    return "50.1";
  }

  public static void main(String[] args) {
    System.out.println("javaforce/" + getVersion());
  }

  public static final boolean isGraal = Boolean.getBoolean("java.graal");

  public static final boolean isGraal() {
    return isGraal;
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

  public static String encoderURL(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (Exception e) {
      return null;
    }
  }

  public static String decodeURL(String url) {
    try {
      return URLDecoder.decode(url, "UTF-8");
    } catch (Exception e) {
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

  public static float atof(String str) {
    if (str.length() == 0) {
      return 0;
    }
    try {
      return Float.parseFloat(str);
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

  /** Checks if system is Windows only. */
  public static boolean isWindows() {
    return (File.separatorChar == '\\');
  }

  /** Checks if system is Unix based (includes : Unix, Linux, Mac, jfLinux) */
  public static boolean isUnix() {
    return (File.separatorChar == '/');
  }

  /** Checks if system is Mac only. */
  public static boolean isMac() {
    if (!isUnix()) return false;
    String osName = System.getProperty("os.name");
    return osName.startsWith("Mac") || osName.startsWith("Darwin");
  }

  /** Checks if system is jfLinux only. */
  public static boolean isJFLinux() {
    if (!isUnix()) return false;
    return new File("/usr/sbin/jlogon").exists();
  }

  public static String getUserPath() {
    return System.getProperty("user.home");
  }

  public static String getCurrentUser() {
    return System.getProperty("user.name");
  }

  public static String getTempPath() {
    if (JF.isWindows()) {
      return System.getenv("TEMP");
    } else {
      return "/tmp";
    }
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

  /** Returns system config path for system services. */
  public static String getConfigPath() {
    if (JF.isWindows()) {
      String path = System.getenv("PROGRAMDATA");  //Win Vista/7/8/10
      if (path == null) {
        path = System.getenv("ALLUSERSPROFILE");  //WinXP
      }
      return path;
    } else {
      return "/etc";
    }
  }

  public static String getLogPath() {
    if (JF.isWindows()) {
      return System.getenv("windir") + "/Logs";
    } else {
      return "/var/log";
    }
  }

  public static void printEnvironment() {
    Map<String, String> env = System.getenv();

    if (true) {
      env.forEach((k, v) -> System.out.println(k + ":" + v));
    } else {
      for (Map.Entry<String, String> entry : env.entrySet()) {
        System.out.println(entry.getKey() + " : " + entry.getValue());
      }
    }
  }

  public static void printProperties() {
    Properties p = System.getProperties();
    Enumeration keys = p.keys();
    while (keys.hasMoreElements()) {
      String key = (String)keys.nextElement();
      String value = (String)p.get(key);
      System.out.println(key + ": " + value);
    }
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
        ret = Arrays.copyOf(ret, pos + read);
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
      return -1;
    }
  }

  public static int read(InputStream in) {
    try {
      return in.read();
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
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
      byte buf[] = new byte[1024];
      while (is.available() > 0) {
        int read = is.read(buf);
        if (read == 0) {
          continue;
        }
        if (read == -1) {
          return false;
        }
        os.write(buf, 0, read);
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

  public static boolean copyAllAppend(String src, String dst) {
    try {
      FileInputStream fis = new FileInputStream(src);
      FileOutputStream fos = new FileOutputStream(dst, true);
      boolean success = copyAll(fis, fos);
      fis.close();
      fos.close();
      return success;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean echoAppend(String str, String dst) {
    try {
      FileOutputStream fos = new FileOutputStream(dst, true);
      fos.write(str.getBytes());
      fos.close();
      return true;
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

  public static boolean isWildcard(String wcstr) {
    if (wcstr.indexOf("*") != -1) return true;
    if (wcstr.indexOf("?") != -1) return true;
    return false;
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
//flip endian functions

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

  public static int getPID() {
    return (int)ProcessHandle.current().pid();
  }

  //copies arrays excluding one element (something Arrays is missing)

  @SuppressWarnings("unchecked")
  public static <T> T[] copyOfExcluding(T[] array, int idx) {
    Class<?> cls = array.getClass().getComponentType();
    T newArray[] = (T[]) Array.newInstance(cls, array.length - 1);
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

  public static char[] copyOfExcluding(char[] array, int idx) {
    char newArray[] = new char[array.length - 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx + 1, newArray, idx, array.length - idx - 1);
    return newArray;
  }

  //copyOfInsert

  @SuppressWarnings("unchecked")
  public static <T> T[] copyOfInsert(T[] array, int idx, T insert) {
    Class<?> cls = array.getClass().getComponentType();
    T newArray[] = (T[]) Array.newInstance(cls, array.length + 1);
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  public static boolean[] copyOfInsert(boolean[] array, int idx, boolean insert) {
    boolean newArray[] = new boolean[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  public static byte[] copyOfInsert(byte[] array, int idx, byte insert) {
    byte newArray[] = new byte[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  public static short[] copyOfInsert(short[] array, int idx, short insert) {
    short newArray[] = new short[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  public static int[] copyOfInsert(int[] array, int idx, int insert) {
    int newArray[] = new int[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  public static long[] copyOfInsert(long[] array, int idx, long insert) {
    long newArray[] = new long[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  public static float[] copyOfInsert(float[] array, int idx, float insert) {
    float newArray[] = new float[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  public static double[] copyOfInsert(double[] array, int idx, double insert) {
    double newArray[] = new double[array.length + 1];
    System.arraycopy(array, 0, newArray, 0, idx);
    System.arraycopy(array, idx, newArray, idx + 1, array.length - idx);
    newArray[idx] = insert;
    return newArray;
  }

  private static boolean initedHttps = false;

  /** This allows connections to untrusted hosts when using https:// with URLConnection. */
  public static void initHttps() {
    if (initedHttps) return;
    initedHttps = true;
    TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    try {
      SSLContext ctx = SSLContext.getInstance("SSL");
      ctx.init(null, trustAllCerts, new SecureRandom());
      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) ctx.getSocketFactory();  //this method will work with untrusted certs
      HttpsURLConnection.setDefaultSSLSocketFactory(sslsocketfactory);
    } catch (Exception e) {
      JFLog.log(e);
    }
    //trust any hostname
    HostnameVerifier hv = new HostnameVerifier() {
      public boolean verify(String urlHostName, SSLSession session) {
        if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
          System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
        }
        return true;
      }
    };
    HttpsURLConnection.setDefaultHostnameVerifier(hv);
  }

  private static final String[] protocols = new String[] {"TLSv1.3"};
  private static final String[] cipher_suites = new String[] {"TLS_AES_128_GCM_SHA256"};

  public static Socket connectSSL(String host, int port, KeyMgmt keys) {
    TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    try {
      SSLContext ctx = SSLContext.getInstance("TLSv1.3");
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      KeyStore ks = keys.getKeyStore();
      kmf.init(ks, keys.getPassword());
      ctx.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) ctx.getSocketFactory();  //this method will work with untrusted certs
      Socket raw = new Socket(host, port);
      SSLSocket ssl = (SSLSocket)sslsocketfactory.createSocket(raw, raw.getInetAddress().getHostAddress(), raw.getPort(), true);
//      ssl.setEnabledProtocols(protocols);
//      ssl.setEnabledCipherSuites(cipher_suites);
      ssl.startHandshake();
      return ssl;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  //creates SSL socket bound to port
  public static ServerSocket createServerSocketSSL(int port, KeyMgmt keys) {
    TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    try {
      SSLContext ctx = SSLContext.getInstance("TLSv1.3");
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      KeyStore ks = keys.getKeyStore();
      kmf.init(ks, keys.getPassword());
      ctx.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
      SSLServerSocketFactory sslfactory = ctx.getServerSocketFactory();  //this method will work with untrusted certs
      SSLServerSocket ssl = (SSLServerSocket) sslfactory.createServerSocket(port);
//      ssl.setEnabledProtocols(protocols);
//      ssl.setEnabledCipherSuites(cipher_suites);
      return ssl;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  //creates unbound SSL socket
  public static ServerSocket createServerSocketSSL(KeyMgmt keys) {
    TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    try {
      SSLContext ctx = SSLContext.getInstance("TLSv1.3");
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      KeyStore ks = keys.getKeyStore();
      kmf.init(ks, keys.getPassword());
      ctx.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
      SSLServerSocketFactory sslfactory = ctx.getServerSocketFactory();  //this method will work with untrusted certs
      SSLServerSocket ssl = (SSLServerSocket) sslfactory.createServerSocket();
//      ssl.setEnabledProtocols(protocols);
//      ssl.setEnabledCipherSuites(cipher_suites);
      return ssl;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static Socket connectSSL(String host, int port) {
    TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    try {
      SSLContext ctx = SSLContext.getInstance("TLSv1.3");
      ctx.init(null, trustAllCerts, new SecureRandom());
      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) ctx.getSocketFactory();  //this method will work with untrusted certs
      Socket raw = new Socket(host, port);
      SSLSocket ssl = (SSLSocket)sslsocketfactory.createSocket(raw, raw.getInetAddress().getHostAddress(), raw.getPort(), true);
//      ssl.setEnabledProtocols(protocols);
//      ssl.setEnabledCipherSuites(cipher_suites);
      ssl.startHandshake();
      return ssl;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static ServerSocket createServerSocketSSL(int port) {
    TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    try {
      SSLContext ctx = SSLContext.getInstance("TLSv1.3");
      ctx.init(null, trustAllCerts, new SecureRandom());
      SSLServerSocketFactory sslfactory = ctx.getServerSocketFactory();  //this method will work with untrusted certs
      SSLServerSocket ssl = (SSLServerSocket) sslfactory.createServerSocket(port);
//      ssl.setEnabledProtocols(protocols);
//      ssl.setEnabledCipherSuites(cipher_suites);
      return ssl;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Upgrades existing socket to SSL. */
  public static Socket connectSSL(Socket socket, boolean server) {
    TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    try {
      SSLContext ctx = SSLContext.getInstance("TLSv1.3");
      ctx.init(null, trustAllCerts, new SecureRandom());
      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) ctx.getSocketFactory();  //this method will work with untrusted certs
      SSLSocket ssl = (SSLSocket)sslsocketfactory.createSocket(socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
      if (!server) {
        ssl.setUseClientMode(true);
      }
//      ssl.setEnabledProtocols(protocols);
//      ssl.setEnabledCipherSuites(cipher_suites);
      ssl.startHandshake();
      return ssl;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Upgrades existing socket to SSL (client side). */
  public static Socket connectSSL(Socket socket) {
    return connectSSL(socket, false);
  }

  /** Returns line # of calling method.
   * See http://stackoverflow.com/questions/115008/how-can-we-print-line-numbers-to-the-log-in-java
   */
  public static int getLineNumber() {
    return Thread.currentThread().getStackTrace()[2].getLineNumber();
  }

  /** Joins array of strings placing a delimit in between each string.
   */
  public static String join(String delimit, String strings[]) {
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<strings.length;a++) {
      if (a > 0) sb.append(delimit);
      sb.append(strings[a]);
    }
    return sb.toString();
  }

  /** Joins array of strings placing a delimit in between each string.
   */
  public static String join(String delimit, String strings[], int startIdx) {
    StringBuilder sb = new StringBuilder();
    for(int a=startIdx;a<strings.length;a++) {
      if (a > startIdx) sb.append(delimit);
      sb.append(strings[a]);
    }
    return sb.toString();
  }

  /** Expand arguments.
   *  Performs argument globbing (wildcards).
   *  Used by native launcher.
   * (same as sun.launcher.LauncherHelper) */
  public static String[] expandArgs(String args[]) {
    ArrayList<String> list = new ArrayList<String>();

    boolean caseSensitive = !isWindows();

    for(int a=0;a<args.length;a++) {
      if (args[a].length() > 0 && args[a].charAt(0) != '\"' && (args[a].indexOf('*') != -1 || args[a].indexOf('?') != -1)) {
        File x = new File(args[a]);
        File parent = x.getParentFile();
        String glob = x.getName();
        if (parent == null) {
          parent = new File(".");
        }
        String parentPath = args[a].substring(0, args[a].length() - glob.length());
        if (parentPath.indexOf('*') != -1 || parentPath.indexOf('?') != -1) {
          list.add(args[a]);
          continue;
        }
        File files[] = parent.listFiles();
        if (files == null) {
          list.add(args[a]);
          continue;
        }
        int cnt = 0;
        for(int f=0;f<files.length;f++) {
          String name = files[f].getName();
          if (wildcardCompare(name, glob, caseSensitive)) {
            list.add(parentPath + name);
            cnt++;
          }
        }
        if (cnt == 0) {
          list.add(args[a]);
        }
      } else {
        list.add(args[a]);
      }
    }

    return list.toArray(new String[list.size()]);
  }

  public static String java_app_home = ".";

  public static void setJavaAppHome(String value) {
    System.out.println("setting java_app_home to " + value);
    java_app_home = value;
  }


  public static boolean is64Bit() {
    return true;
  }

  /** Returns path component of a filename */
  public static String getPath(String filename) {
    File file = new File(filename);
    return file.getParent();
  }

  /** Deletes a folder and all sub-files and folders. */
  public static void deletePathEx(String path) {
    try {
      File folder = new File(path);
      if (!folder.isDirectory()) return;
      File files[] = new File(path).listFiles();
      if (files == null) return;
      for(int a=0;a<files.length;a++) {
        File file = files[a];
        if (file.isDirectory()) {
          deletePathEx(file.getAbsolutePath());
        } else {
          file.delete();
        }
      }
      folder.delete();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static void zipPath(ZipOutputStream zos, String path, String base) throws Exception {
    File files[] = new File(path).listFiles();
    for(int a=0;a<files.length;a++) {
      File file = files[a];
      String name = file.getName();
      String full;
      if (base.length() == 0) {
        full = name;
      } else {
        full = base + "/" + name;
      }
      if (file.isDirectory()) {
        zipPath(zos, file.getAbsolutePath(), full);
      } else {
        ZipEntry ze = new ZipEntry(full);
        zos.putNextEntry(ze);
        FileInputStream fis = new FileInputStream(file);
        copyAll(fis, zos);
        fis.close();
        zos.closeEntry();
      }
    }
  }

  /** Zips path into zip file. */
  public static void zipPath(String path, String zip) {
    try {
      File folder = new File(path);
      if (!folder.isDirectory()) {
        JFLog.log("Error:path to be zipped not found");
        return;
      }
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
      zipPath(zos, path, "");
      zos.finish();
      zos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Unzips zip file into path. */
  public static void unzip(String zip, String path) {
    JFLog.log("unzip:" + zip + " to " + path);
    try {
      File folder = new File(path);
      if (folder.exists() && !folder.isDirectory()) {
        JFLog.log("Error:target path is a file");
        return;
      }
      if (!folder.exists()) {
        folder.mkdirs();
      }
      ZipInputStream zis = new ZipInputStream(new FileInputStream(zip));
      ZipEntry ze;
      while ((ze = zis.getNextEntry()) != null) {
        String name = ze.getName();
        if (ze.isDirectory()) {
          new File(path + "/" + name).mkdirs();
          continue;
        }
        String full = path + "/" + name;
        int idx = full.lastIndexOf("/");
        if (idx != -1) {
          String epath = full.substring(0, idx);
          new File(epath).mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(full);
        copyAll(zis, fos);
        fos.close();
      }
      zis.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static String[] splitQuoted(String in, char token) {
    ArrayList<String> s = new ArrayList<String>();
    char ca[] = in.toCharArray();
    int off = 0;
    boolean quote1 = false;
    boolean quote2 = false;
    int cnt = 0;
    for(int a=0;a<ca.length;a++) {
      char ch = ca[a];
      if (ch == '\'' && !quote2) quote1 = !quote1;
      if (ch == '\"' && !quote1) quote2 = !quote2;
      if (!quote1 && !quote2 && ch == token) {
        String str = new String(ca, off, cnt);
        s.add(str);
        off = a + 1;
        cnt = 0;
      } else {
        cnt++;
      }
    }
    if (cnt > 0) {
      String str = new String(ca, off, cnt);
      s.add(str);
    }
    return s.toArray(new String[0]);
  }

  public static String readLineQuoted(InputStream is) {
    StringBuilder sb = new StringBuilder();
    boolean quote1 = false;
    boolean quote2 = false;
    try {
      while (is.available() > 0) {
        char ch = (char)is.read();
        if (ch == '\'' && !quote2) quote1 = !quote1;
        if (ch == '\"' && !quote1) quote2 = !quote2;
        if (!quote1 && !quote2) {
          if (ch == '\r') continue;
          if (ch == '\n') break;
        }
        sb.append(ch);
      }
    } catch (Exception e) {}
    return sb.toString();
  }

  public static Object readObject(InputStream is) {
    try {
      ObjectInputStream ois = new ObjectInputStream(is);
      Object obj = ois.readObject();
      return obj;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static Object readObject(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      Object obj = readObject(fis);
      fis.close();
      return obj;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static void writeObject(Object obj, OutputStream os) {
    try {
      ObjectOutputStream oos = new ObjectOutputStream(os);
      oos.writeObject(obj);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void writeObject(Object obj, String file) {
    try {
      FileOutputStream fos = new FileOutputStream(file);
      writeObject(obj, fos);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Determines local IP address for an inbound DatagramPacket */
  public static InetAddress getLocalAddress(DatagramPacket packet) {
    try {
      SocketAddress remoteAddress = packet.getSocketAddress();
      DatagramSocket sock = new DatagramSocket();
      //DatagramSocket.connect() will send an ICMP packet to determine if port is open
      sock.connect(remoteAddress);
      InetAddress localAddress = sock.getLocalAddress();
      sock.disconnect();
      sock.close();
      return localAddress;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Returns MAC address for a given IP4 address.
   * Supports only clients on same LAN.
   * Uses the 'arp' command.
   */
  public static String getRemoteMAC(String ip) {
    ShellProcess shell = new ShellProcess();
    String output;
    int macidx;
    char macdelimit;
    if (isWindows()) {
      output = shell.run(new String[] {"arp", "-a"}, false);
      //Internet Address      Physical Address      Type
      macidx = 1;
      macdelimit = '-';
    } else {
      output = shell.run(new String[] {"arp", "-n"}, false);
      //Address                  HWtype  HWaddress           Flags Mask            Iface
      macidx = 2;
      macdelimit = ':';
    }
    String[] lns = output.split("\n");
    for(String ln : lns) {
      String[] f = ln.split(" +");
      if (f[0].equals(ip)) {
        return f[macidx].replaceAll("[" + macdelimit + "]", "");
      }
    }
    JFLog.log("JF.getRemoteMac():Error:IP not found:" + ip);
    return null;
  }

  /** Filter regex for any letter. */
  public static String filter_alpha = "[a-zA-Z]";
  /** Filter regex for any number. */
  public static String filter_numeric = "[0-9]";
  /** Filter regex for any hex number. */
  public static String filter_hex = "[0-9a-fA-F]";
  /** Filter regex for any letter or number. */
  public static String filter_alpha_numeric = "[a-zA-Z0-9]";

  /** Filters a string using a regex that is matched against each character.
   * See filter_alpha, filter_alpha_number, etc.
   */
  public static String filter(String str, String regex) {
    StringBuilder sb = new StringBuilder();
    int len = str.length();
    for(int idx = 0; idx < len; idx++) {
      String ch = str.substring(idx, idx+1);
      if (ch.matches(regex)) {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  /** Executes a command and returns the exit code. */
  public static int exec(String[] cmd) {
    try {
      Process p = Runtime.getRuntime().exec(cmd);
      return p.waitFor();
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  private final static long KB = 1024;
  private final static long MB = 1024 * 1024;
  private final static long GB = 1024 * 1024 * 1024;
  private final static long TB = 1024L * 1024L * 1024L * 1024L;

  /** Converts long to engineering notation (KB, MB, GB, TB). */
  public static String toEng(long size) {
    if (size < MB) {
      return String.format("%dKB", size / KB);
    } else if (size < GB) {
      return String.format("%dMB", size / MB);
    } else if (size < TB) {
      return String.format("%dGB", size / GB);
    } else {
      return String.format("%dTB", size / TB);
    }
  }

  /** Converts engineering notation (KB, MB, GB, TB) value to long. */
  public static long fromEng(String value) {
    String cap = value.toUpperCase();
    if (!cap.endsWith("B")) {
      cap = cap + "B";
    }
    if (cap.endsWith("KB")) {
      String val = cap.substring(0, cap.length() - 2);
      return Long.valueOf(val) * KB;
    }
    if (cap.endsWith("MB")) {
      String val = cap.substring(0, cap.length() - 2);
      return Long.valueOf(val) * MB;
    }
    if (cap.endsWith("GB")) {
      String val = cap.substring(0, cap.length() - 2);
      return Long.valueOf(val) * GB;
    }
    if (cap.endsWith("TB")) {
      String val = cap.substring(0, cap.length() - 2);
      return Long.valueOf(val) * TB;
    }
    return Long.valueOf(value);
  }

  public static boolean moveFile(String src, String dest) {
    try {
      Path temp = Files.move(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
      return temp != null;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean copyFile(String src, String dest) {
    try {
      Path temp = Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
      return temp != null;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean grep(InputStream is, OutputStream os, String regex) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String ln;
      String regexln = ".*(" + regex + ").*";
      while ((ln = br.readLine()) != null) {
        try {
          if (ln.matches(regexln)) {
            os.write(ln.getBytes());
            if (JF.isWindows()) {
              os.write("\r\n".getBytes());
            } else {
              os.write("\n".getBytes());
            }
          }
        } catch (Exception e) {}
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean grep(String filein, String fileout, String regex) {
    try {
      InputStream is = new FileInputStream(filein);
      OutputStream os = new FileOutputStream(fileout);
      boolean res = grep(is, os, regex);
      is.close();
      os.close();
      return res;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Copy of Integer.digits */
  public static final char[] digits = {
      '0' , '1' , '2' , '3' , '4' , '5' ,
      '6' , '7' , '8' , '9' , 'a' , 'b' ,
      'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
      'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
      'o' , 'p' , 'q' , 'r' , 's' , 't' ,
      'u' , 'v' , 'w' , 'x' , 'y' , 'z'
  };
}

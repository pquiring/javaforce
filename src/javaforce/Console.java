package javaforce;

/** Console
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.jni.*;

public class Console {
  private static boolean isWindows = JF.isWindows();
  public static InputStream getInputStream() {
    return new InputStream() {
      public int read() throws IOException {
        int ch;
        if (isWindows) {
          ch = WinNative.readConsole();
        } else {
          ch = LnxNative.readConsole();
        }
        return ch;
      }

      public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
      }
      public int read(byte[] buf, int offset, int length) throws IOException {
        int start = offset;
        int end = offset + length;
        int count = 0;
        for(int i=start;i<end;) {
          int ch = read();
          if (ch == 0 || ch == -1) break;
          buf[i] = (byte)ch;
          i++;
          count++;
        }
        return count;
      }
    };
  }

  public static OutputStream getOutputStream() {
    return new OutputStream() {
      public void write(int utf16) throws IOException {
        char ch = ASCII8.convertUTF16(utf16);
        if (isWindows) {
          WinNative.writeConsole(ch);
        } else {
          LnxNative.writeConsole(ch);
        }
      }
    };
  }

  public static void enableConsoleMode() {
    if (JF.isWindows())
      WinNative.enableConsoleMode();
    else
      LnxNative.enableConsoleMode();
  }

  public static void disableConsoleMode() {
    if (JF.isWindows())
      WinNative.disableConsoleMode();
    else
      LnxNative.disableConsoleMode();
  }

  public static boolean kbhit() {
    if (JF.isWindows()) {
      return WinNative.peekConsole();
    } else {
      return LnxNative.peekConsole();
    }
  }

  public static <T> void printArray(T[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    int size = a.length;
    for(int i=0;i<size;i++) {
      if (i > 0) sb.append(",");
      sb.append(a[i].toString());
    }
    sb.append("}");
    System.out.println(sb.toString());
  }

  public static void printArray(byte[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    int size = a.length;
    for(int i=0;i<size;i++) {
      if (i > 0) sb.append(",");
      sb.append(Byte.toString(a[i]));
    }
    sb.append("}");
    System.out.println(sb.toString());
  }

  public static void printArray(short[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    int size = a.length;
    for(int i=0;i<size;i++) {
      if (i > 0) sb.append(",");
      sb.append(Short.toString(a[i]));
    }
    sb.append("}");
    System.out.println(sb.toString());
  }

  public static void printArray(int[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    int size = a.length;
    for(int i=0;i<size;i++) {
      if (i > 0) sb.append(",");
      sb.append(Integer.toString(a[i]));
    }
    sb.append("}");
    System.out.println(sb.toString());
  }

  public static void printArray(long[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    int size = a.length;
    for(int i=0;i<size;i++) {
      if (i > 0) sb.append(",");
      sb.append(Long.toString(a[i]));
    }
    sb.append("}");
    System.out.println(sb.toString());
  }

  public static void printArray(float[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    int size = a.length;
    for(int i=0;i<size;i++) {
      if (i > 0) sb.append(",");
      sb.append(Float.toString(a[i]));
    }
    sb.append("}");
    System.out.println(sb.toString());
  }

  public static void printArray(double[] a) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    int size = a.length;
    for(int i=0;i<size;i++) {
      if (i > 0) sb.append(",");
      sb.append(Double.toString(a[i]));
    }
    sb.append("}");
    System.out.println(sb.toString());
  }

  public static void main(String[] args) {
    enableConsoleMode();
    InputStream is = getInputStream();
    JFLog.log("Press q to quit");
    int ch = 0;
    while (true) {
      try {
        if (kbhit()) {
          ch = is.read();
          JFLog.log("ch=" + ch);
        } else {
          JF.sleep(100);
        }
        if (ch=='q') break;
      } catch (Exception e) {
        break;
      }
    }
    disableConsoleMode();
  }
}

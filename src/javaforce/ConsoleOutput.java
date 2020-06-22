package javaforce;

/** Console Output
 *
 * Alternative to System.out which has issues in Graal.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.jni.*;

public class ConsoleOutput extends OutputStream {

  private int low = -1;
  public void write(int b) throws IOException {
    b &= 0xff;
    if (low == -1) {
      low = b;
    } else {
      b <<= 8;
      b |= low;
      char c = ASCII8.convertUTF16(b);
      if (JF.isWindows()) {
        WinNative.writeConsole(c);
      } else if (JF.isUnix()) {
        LnxNative.writeConsole(c);
      }
      low = -1;
    }
  }

  public static void install() {
    try {
      System.setOut(new PrintStream(new ConsoleOutput(), true, "UTF-16LE"));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

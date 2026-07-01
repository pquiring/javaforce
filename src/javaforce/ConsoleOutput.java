package javaforce;

import java.io.*;

import javaforce.api.*;

/** Console Output.
 *
 * Alternative to System.out which has issues in Graal.
 *
 * @author pquiring
 */

public class ConsoleOutput extends OutputStream {
  private int low = -1;
  private boolean isWindows;
  public ConsoleOutput() {
    isWindows = JF.isWindows();
  }
  public void write(int b) throws IOException {
    b &= 0xff;
    if (low == -1) {
      low = b;
    } else {
      b <<= 8;
      b |= low;
      char c = ASCII8.convertUTF16(b);
      if (isWindows) {
        WindowsAPI.getInstance().writeConsole(c);
      } else {
        LinuxAPI.getInstance().writeConsole(c);
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

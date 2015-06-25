package javaforce;

/** Init JFLog for Android
 *
 * @author pquiring
 */

import java.io.*;

import android.util.*;

public class JFLogAndroid {
  public static void init(int id, final String tag) {
    PrintStream ps = new PrintStream(new OutputStream() {
      private StringBuffer sb = new StringBuffer();
      private void add(byte b[]) {
        for(int a=0;a<b.length;a++) {
          if (b[a] == '\n') {
            Log.i(tag, sb.toString());
            sb.setLength(0);
          } else {
            sb.append((char)b[a]);
          }
        }
      }
      public void write(int b) throws IOException {
        add(new byte[] {(byte)b});
      }
      public void write(byte b[]) throws IOException {
        add(b);
      }
    });
    JFLog.init(id, null, true, ps);
  }
}

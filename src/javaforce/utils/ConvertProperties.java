package javaforce.utils;

/** Convert '-' properties to '_' properties
 *   for bash scripts
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class ConvertProperties {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("ConvertProperties filein fileout");
      return;
    }
    try {
      FileInputStream fis = new FileInputStream(args[0]);
      String[] lns = new String(fis.readAllBytes()).split(JF.eol);
      StringBuilder out = new StringBuilder();
      for(String ln : lns) {
        out.append(process(ln));
        out.append(JF.eol);
      }
      FileOutputStream fos = new FileOutputStream(args[1]);
      fos.write(out.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  private static String process(String ln) {
    char[] ca = ln.toCharArray();
    int pos = 0;
    while (pos < ca.length) {
      if (ca[pos] == '=') break;
      if (ca[pos] == '#') break;
      if (ca[pos] == '-') ca[pos] = '_';
      pos++;
    }
    return new String(ca);
  }
}

package javaforce.utils;

import javaforce.*;

/** grep CLI
 *
 * @author pquiring
 */

public class grep {
  public static void main(String[] args) {
    if (args.length != 3) {
      JFLog.log("Usage: grep filein fileout regex");
      return;
    }
    JF.grep(args[0], args[1], args[2]);
  }
}

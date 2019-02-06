/** PLC Restore command line tool.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.controls.*;

public class Restore {
  private static void usage() {
    System.out.println("jfplcrestore PLC_URL PLC_TAG filein");
    System.out.println("  URL = S7:IP  //Siemens");
    System.out.println("  URL = AB:IP  //Allen Bradley");
    System.out.println("  TAG = \"DB5.DBX0.0 BYTE 1000\"  //Siemens");
    System.out.println("  TAG = \"MyDataTag\"  //Allen Bradley");
    System.exit(1);
  }
  public static void main(String args[]) {
    if (args.length != 3) {
      usage();
    }
    try {
      Controller c = new Controller();
      if (!c.connect(args[0])) {
        throw new Exception("Error:Controller.connect() failed");
      }
      FileInputStream fis = new FileInputStream(args[2]);
      byte data[] = JF.readAll(fis);
      if (data == null) {
        throw new Exception("Error:null data");
      }
      c.write(args[1], data);
      c.disconnect();
      System.out.println("Restore complete!");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

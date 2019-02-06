/** PLC Backup command line tool.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.controls.*;

public class Backup {
  private static void usage() {
    System.out.println("jfplcbackup PLC_URL PLC_TAG fileout");
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
      byte data[] = c.read(args[1]);
      if (data == null) {
        throw new Exception("Error:null data");
      }
      c.disconnect();
      FileOutputStream fis = new FileOutputStream(args[2]);
      fis.write(data);
      fis.close();
      System.out.println("Backup complete!");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

/** PLC Backup command line tool.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.controls.*;

public class Backup {
  private static void usage() {
    System.out.println("jfplcbackup PLC_URL PLC_TAG fileout [debug]");
    System.out.println("  URL = S7:IP  //Siemens");
    System.out.println("  URL = AB:IP  //Allen Bradley");
    System.out.println("  TAG = \"DB5.DBX0.0 BYTE 1000\"  //Siemens");
    System.out.println("  TAG = \"MyDataTag\"  //Allen Bradley");
    System.exit(1);
  }
  public static void main(String args[]) {
    if (args.length < 3) {
      usage();
    }
    if (args.length > 3) {
      if (args[3].equals("debug")) {
        PLCFactory.debug = true;
      }
    }
    try {
      String url = args[0];
      int idx = url.indexOf(':');
      String plc_type = url.substring(0, idx);
      String plc_host = url.substring(idx + 1);
      PLC plc = PLCFactory.create(plc_type);
      if (!plc.connect(plc_host)) {
        throw new Exception("Error:Controller.connect() failed");
      }
      byte data[] = plc.readTag(args[1]);
      if (data == null) {
        Exception e = plc.getException();
        if (e != null) {
          e.printStackTrace();
        }
        throw new Exception("Error:null data");
      }
      plc.disconnect();
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

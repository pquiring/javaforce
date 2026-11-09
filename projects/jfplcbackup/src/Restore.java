/** PLC Restore command line tool.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.controls.*;

public class Restore {
  private static void usage() {
    System.out.println("jfplcrestore PLC_URL PLC_TAG filein [datatype]");
    System.out.println("  URL = S7:IP  //Siemens");
    System.out.println("  URL = AB:IP  //Allen Bradley");
    System.out.println("  TAG = \"DB5.DBX0.0 BYTE 1000\"  //Siemens");
    System.out.println("  TAG = \"MyDataTag\"  //Allen Bradley");
    System.out.println("  datatype = INT8 INT16 INT32 INT64 FLOAT DOUBLE BOOLEAN  //Allen Bradley only");
    System.exit(1);
  }
  public static void main(String args[]) {
    if (args.length < 3) {
      usage();
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
      FileInputStream fis = new FileInputStream(args[2]);
      byte data[] = JF.readAll(fis);
      if (data == null) {
        throw new Exception("Error:null data");
      }
      boolean ok = false;
      if (args.length == 4) {
        int dt = PLCFactory.getDataType(args[3]);
        ok = plc.writeTag(args[1], data, dt);
      } else {
        ok = plc.writeTag(args[1], data);
      }
      plc.disconnect();
      if (ok) {
        System.out.println("Restore complete!");
      } else {
        System.out.println("Restore failed!");
      }
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

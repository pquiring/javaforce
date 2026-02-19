package javaforce.tests;

/** Test PLC (Controller)
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.controls.*;

public class TestPLC {
  private static void usage() {
    JFLog.log("TestPLC ACTION HOST [data]");
    JFLog.log(" ACTION=readtag writetag readtime writetime");
  }
  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
      return;
    }
    switch (args[0].toLowerCase()) {
      case "readtag": readtag(args); break;
      case "writetag": writetag(args); break;
      case "readtime": readtime(args); break;
      case "writetime": writetime(args); break;
    }
  }
  public static void readtag(String[] args) {
    if (args.length != 3) {
      System.out.println("Usage: ReadTag HOST TAG");
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP NI:device/opts etc.");
      return;
    }
    try {
      String action = args[0];
      String url = args[1];
      String tag = args[2];
      Controller c = new Controller();
      if (!c.connect(url)) {
        System.out.println("Connection failed");
        return;
      }
      byte[] data = c.read(tag);
      if (data == null) {
        System.out.println("read failed");
        return;
      }
      System.out.println("Size:" + data.length);
      for(int a=0;a<data.length;a++) {
        System.out.print(String.format("%02d,", data[a] & 0xff));
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
  public static void writetag(String[] args) {
    if (args.length != 5) {
      System.out.println("Usage: WriteTag HOST TAG TagType HexBytes,...");
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP NI:device/opts etc.");
      System.out.println(" Type: ANY INT8 INT16 INT32 INT64 FLOAT DOUBLE (required for AllenBradley only, all others use ANY)");
      return;
    }
    try {
      String action = args[0];
      String url = args[1];
      String tag = args[2];
      String type = args[3];
      String[] bytes = args[4].split("[,]");
      Controller c = new Controller();
      if (!c.connect(url)) {
        System.out.println("Connection failed");
        return;
      }
      byte[] data = new byte[bytes.length];
      for(int a=0;a<bytes.length;a++) {
        data[a] = (byte)(int)Integer.valueOf(bytes[a], 16);
      }
      Controller.datatype datatype = Controller.datatype.ANY;
      switch (type) {
        case "ANY": datatype = Controller.datatype.ANY; break;
        case "INT8": datatype = Controller.datatype.INTEGER8; break;
        case "INT16": datatype = Controller.datatype.INTEGER16; break;
        case "INT32": datatype = Controller.datatype.INTEGER32; break;
        case "INT64": datatype = Controller.datatype.INTEGER64; break;
        case "FLOAT": datatype = Controller.datatype.FLOAT; break;
        case "DOUBLE": datatype = Controller.datatype.DOUBLE; break;
        default: System.out.println("Error:unknown data type"); System.exit(1); break;
      }
      if (!c.write(tag, data, datatype)) {
        System.out.println("write failed");
      } else {
        System.out.println("write ok");
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
  public static void readtime(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: ReadTag HOST");
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP etc.");
      return;
    }
    try {
      String action = args[0];
      String url = args[1];
      Controller c = new Controller();
      if (!c.connect(url)) {
        System.out.println("Connection failed");
        return;
      }
      Calendar dt = c.readTime();
      if (dt == null) {
        System.out.println("read failed");
        return;
      }
      JFLog.log("time=" + JF.Date2String(dt) + " " + JF.Time2String(dt));
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
  public static void writetime(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: WriteTime HOST");
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP etc.");
      return;
    }
    try {
      String action = args[0];
      String url = args[1];

      Calendar dt = Calendar.getInstance();
      Controller c = new Controller();
      if (!c.connect(url)) {
        System.out.println("Connection failed");
        return;
      }
      if (!c.writeTime(dt)) {
        System.out.println("update failed");
      } else {
        System.out.println("update success");
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}

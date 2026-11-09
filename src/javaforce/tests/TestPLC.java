package javaforce.tests;

import java.util.*;

import javaforce.*;
import javaforce.controls.*;

/** Test PLC (Controller)
 *
 * @author pquiring
 */

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
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP etc.");
      return;
    }
    try {
      String action = args[0];
      String url = args[1];
      int idx = url.indexOf(":");
      String plc_type = url.substring(0, idx);
      String plc_host = url.substring(idx + 1);
      String tag = args[2];
      PLC plc = PLCFactory.create(plc_host);
      if (!plc.connect(plc_host)) {
        System.out.println("Connection failed");
        return;
      }
      byte[] data = plc.readTag(tag);
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
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP etc.");
      System.out.println(" Type: ANY INT8 INT16 INT32 INT64 FLOAT DOUBLE (required for AllenBradley only, all others use ANY)");
      return;
    }
    try {
      String action = args[0];
      String url = args[1];
      int idx = url.indexOf(":");
      String plc_type = url.substring(0, idx);
      String plc_host = url.substring(idx + 1);
      String tag = args[2];
      String type = args[3];
      String[] bytes = args[4].split("[,]");
      PLC plc = PLCFactory.create(plc_host);
      if (!plc.connect(plc_host)) {
        System.out.println("Connection failed");
        return;
      }
      byte[] data = new byte[bytes.length];
      for(int a=0;a<bytes.length;a++) {
        data[a] = (byte)(int)Integer.valueOf(bytes[a], 16);
      }
      int dt = PLCFactory.getDataType(type);
      if (!plc.writeTag(tag, data, dt)) {
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
      System.out.println("Usage: ReadTime HOST");
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP etc.");
      return;
    }
    try {
      String action = args[0];
      String url = args[1];
      int idx = url.indexOf(":");
      String plc_type = url.substring(0, idx);
      String plc_host = url.substring(idx + 1);
      PLC plc = PLCFactory.create(plc_host);
      if (!plc.connect(plc_host)) {
        System.out.println("Connection failed");
        return;
      }
      Calendar dt = plc.getClock();
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
      int idx = url.indexOf(":");
      String plc_type = url.substring(0, idx);
      String plc_host = url.substring(idx + 1);

      Calendar dt = Calendar.getInstance();
      PLC plc = PLCFactory.create(plc_host);
      if (!plc.connect(plc_host)) {
        System.out.println("Connection failed");
        return;
      }
      if (!plc.setClock(dt)) {
        System.out.println("update failed");
      } else {
        System.out.println("update success");
      }
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }
}

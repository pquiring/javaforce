package javaforce.controls;

/** Controller Write Tag test
 *
 * @author pquiring
 */

import javaforce.*;

public class WriteTag {
  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("Usage: WriteTag HOST TAG TagType HexBytes,...");
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP NI:device/opts etc.");
      System.out.println(" Type: ANY INT8 INT16 INT32 INT64 FLOAT DOUBLE (required for AllenBradley only, all others use ANY)");
      return;
    }
    try {
      String url = args[0];
      String tag = args[1];
      String type = args[2];
      String[] bytes = args[3].split("[,]");
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
}

package javaforce.controls;

/** Controller Read Tag test
 *
 * @author pquiring
 */

public class ReadTag {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: ReadTag HOST TAG");
      System.out.println(" Host: S7:IP AB:IP MODBUS:IP NI:device/opts etc.");
      return;
    }
    String url = args[0];
    String tag = args[1];
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
  }
}

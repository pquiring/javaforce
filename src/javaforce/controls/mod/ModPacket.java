package javaforce.controls.mod;

/** ModBus packet
 *
 * @author pquiring
 */

import javaforce.BE;

public class ModPacket {

  private static short tid = 0x1000;  //next transaction id

  private static byte readFunc(byte io_type) {
    switch (io_type) {
      case ModTypes.C: return 0x01;
      case ModTypes.DI: return 0x02;
    }
    return 0;
  }

  /** Creates a packet to read data from ModBus. */
  public static byte[] makeReadPacket(ModAddr addr) {
    byte data[] = new byte[12];
    BE.setuint16(data, 0, tid++);  //transaction id
//    BE.setuint16(data, 2, 0);  //protocol
    BE.setuint16(data, 4, 6);  //length
//    data[6] = 0;  //unit ID

    data[7] = readFunc(addr.io_type);
    BE.setuint16(data, 8, addr.io_number - 1);  //starting addr
    BE.setuint16(data, 10, addr.length);  //bit count

    return data;
  }

  /** Creates a packet to write data to ModBus. */
  public static byte[] makeWritePacket(ModAddr addr) {
    byte data[] = new byte[12];
    BE.setuint16(data, 0, tid++);  //transaction id
//    BE.setuint16(data, 2, 0);  //protocol
    BE.setuint16(data, 4, 6);  //length
//    data[6] = 0;  //unit ID

    data[7] = 5;  //set single coil
    BE.setuint16(data, 8, addr.io_number - 1);
    BE.setuint16(data, 10, addr.state ? 0xff00 : 0x0000);
    return data;
  }

  public static ModAddr decodeAddress(String addr) {
    //C# - coil output
    //DI# = discrete input
    ModAddr ma = new ModAddr();
    ma.length = 1;
    switch (addr.charAt(0)) {
      case 'C':
        ma.io_type = ModTypes.C;
        ma.io_number = Short.valueOf(addr.substring(1));
        break;
      case 'D':
        ma.io_type = ModTypes.DI;
        ma.io_number = Short.valueOf(addr.substring(2));
        break;
      default:
        return null;
    }
    return ma;
  }

  public static ModData decodePacket(byte data[]) {
    ModData out = new ModData();

    short tid = (short)BE.getuint16(data, 0);
    short proto = (short)BE.getuint16(data, 2);
    short len = (short)BE.getuint16(data, 4);
    byte uid = data[6];

    if (data.length != 6 + len) return null;

    byte func = data[7];
    switch (func) {
      case 0x01:  //returning coils
      case 0x02:  //returning inputs
        byte blen = data[8];
        out.type = func;
        out.data = new byte[blen];
        System.arraycopy(data, 9, out.data, 0, blen);
        break;
    }
    return out;
  }

  public static boolean isPacketComplete(byte data[]) {
    try {
      return decodePacket(data) != null;
    } catch (Exception e) {
      return false;
    }
  }
}

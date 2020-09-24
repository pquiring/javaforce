package javaforce.controls.mod;

/** ModBus packet
 *
 * Addressing:
 *  C1-65536
 *  DI1-65536
 *  IR0-65535 or IRX0-FFFF
 *  HR0-65535 or HRX0-FFFF
 *
 * @author pquiring
 */

import javaforce.*;

public class ModPacket {

  private static short tid = 0x1000;  //next transaction id

  private static byte readFunc(byte io_type) {
    switch (io_type) {
      case ModTypes.C: return 0x01;  //read coil
      case ModTypes.DI: return 0x02;  //read direct input
      case ModTypes.HR: return 0x03;  //read holding register
      case ModTypes.IR: return 0x04;  //read input register
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
    BE.setuint16(data, 8, addr.io_number);  //starting addr
    BE.setuint16(data, 10, addr.length);  //bit count / register count

    return data;
  }

  private static byte writeFunc(byte io_type) {
    switch (io_type) {
      case ModTypes.C: return 0x5;  //write single coil
      case ModTypes.HR: return 0x6;  //write output register
    }
    return 0;
  }

  /** Creates a packet to write data to ModBus. */
  public static byte[] makeWritePacket(ModAddr addr) {
    byte data[] = new byte[12];
    BE.setuint16(data, 0, tid++);  //transaction id
//    BE.setuint16(data, 2, 0);  //protocol
    BE.setuint16(data, 4, 6);  //length
//    data[6] = 0;  //unit ID

    data[7] = writeFunc(addr.io_type);
    BE.setuint16(data, 8, addr.io_number);
    if (addr.io_type == ModTypes.C) {
      BE.setuint16(data, 10, addr.data[0] != 0 ? 0xff00 : 0x0000);
    } else {
      data[10] = addr.data[0];
      data[11] = addr.data[1];
    }
    return data;
  }

  public static ModAddr decodeAddress(String addr) {
    //C# - coil output
    //DI# = discrete input
    addr = addr.toUpperCase();
    ModAddr ma = new ModAddr();
    ma.length = 1;
    switch (addr.charAt(0)) {
      case 'C':
        //1-65536
        ma.io_type = ModTypes.C;
        ma.io_number = Short.valueOf(addr.substring(1));
        ma.io_number--;
        break;
      case 'D':
        //1-65536
        if (addr.charAt(1) != 'I') {
          JFLog.log("MODBUS:Invalid addr:" + addr);
          return null;
        }
        ma.io_type = ModTypes.DI;
        ma.io_number = Integer.valueOf(addr.substring(2));
        ma.io_number--;
        break;
      case 'I':
        //0-65535
        if (addr.charAt(1) != 'R') {
          JFLog.log("MODBUS:Invalid addr:" + addr);
          return null;
        }
        ma.io_type = ModTypes.IR;
        if (addr.charAt(2) == 'X') {
          ma.io_number = Integer.valueOf(addr.substring(3), 16);
        } else {
          ma.io_number = Integer.valueOf(addr.substring(2));
        }
        break;
      case 'H':
        //0-65535
        if (addr.charAt(1) != 'R') {
          JFLog.log("MODBUS:Invalid addr:" + addr);
          return null;
        }
        ma.io_type = ModTypes.HR;
        if (addr.charAt(2) == 'X') {
          ma.io_number = Integer.valueOf(addr.substring(3), 16);
        } else {
          ma.io_number = Integer.valueOf(addr.substring(2));
        }
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
      case 0x02:  //returning discrete inputs
      case 0x03:  //returning input registers
      case 0x04:  //returning holding registers
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

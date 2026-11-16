package javaforce;

import java.util.*;

/** GUID/UUID.
 *
 * 128 bits / 16 bytes (32 hex chars)
 *
 * Hex format : {12345678-1234-1234-1234-123456781234}
 *
 * @author pquiring
 */

public class GUID {
  public int data1;  //32 bits
  public short data2;  //16 bits
  public short data3;  //16 bits
  public long data4;  //split into 16 bits / 48 bits

  /** Generate a random GUID. */
  public static GUID generate() {
    Random r = new Random();
    GUID guid = new GUID();
    guid.data1 = r.nextInt();
    guid.data2 = (short)(r.nextInt() & 0xffff);
    guid.data3 = (short)(r.nextInt() & 0xffff);
    guid.data4 = r.nextLong();
    return guid;
  }

  public String toString(boolean curly_brackets, boolean upper_case) {
    String str = String.format("%08x-%04x-%04x-%04x-%012x", data1, data2, data3, data4 >>> 48, data4 & 0xffff_ffff_ffffL);
    if (upper_case) {
      str = str.toUpperCase();
    }
    if (curly_brackets) {
      return "{" + str + "}";
    } else {
      return str;
    }
  }

  public String toString() {
    return toString(true, true);
  }

  /** Unit test. */
  public static void main(String[] args) {
    int cnt = 1;
    try {
      if (args.length > 0) {
        cnt = Integer.valueOf(args[0]);
      }
      for(int a=0;a<cnt;a++) {
        GUID guid = generate();
        System.out.println("GUID[" + a + "]=" + guid.toString());
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

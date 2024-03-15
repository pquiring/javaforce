package javaforce.vm;

/** UUID
 *
 * @author pquiring
 */

import java.util.*;

public class UUID {
  /** Generate a random UUID String. */
  public static String generate() {
    //format : 36 chars : 8-4-4-4-12
    Random rand = new Random();
    StringBuilder uuid = new StringBuilder();
    uuid.append(String.format("%08x", rand.nextInt(0x6fffffff) + 0x10000000));
    uuid.append('-');
    uuid.append(String.format("%04x", rand.nextInt(0x6fff) + 0x1000));
    uuid.append('-');
    uuid.append(String.format("%04x", rand.nextInt(0x6fff) + 0x1000));
    uuid.append('-');
    uuid.append(String.format("%04x", rand.nextInt(0x6fff) + 0x1000));
    uuid.append('-');
    uuid.append(String.format("%08x", rand.nextInt(0x6fffffff) + 0x10000000));
    uuid.append(String.format("%04x", rand.nextInt(0x7fff)));
    if (uuid.length() != 36) {
      System.out.println("UUID.generate() != 36 chars");
      uuid.setLength(36);
    }
    return uuid.toString();
  }
}

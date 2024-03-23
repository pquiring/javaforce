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
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    uuid.append('-');
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    uuid.append('-');
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    uuid.append('-');
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    uuid.append('-');
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    uuid.append(String.format("%04x", rand.nextInt(0xffff)));
    if (uuid.length() != 36) {
      System.out.println("UUID.generate() != 36 chars");
      uuid.setLength(36);
    }
    return uuid.toString();
  }
}

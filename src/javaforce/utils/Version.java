package javaforce.utils;

/** Prints JavaForce version info.
 *
 * @author pquiring
 */

public class Version {
  public static void main(String args[]) {
    System.out.println("jfLinux=" + javaforce.linux.Linux.getVersion());
    System.out.println("JavaForce=" + javaforce.JF.getVersion());
    System.out.println("Java=" + System.getProperty("java.version"));
  }
}

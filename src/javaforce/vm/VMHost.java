package javaforce.vm;

/** Provides some statistics of the VM Host.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class VMHost {
  public static native long total_memory();
  public static native long free_memory();
  public static native long cpu_load();

  /** Tests connecting to remote host.
   *
   * @param remote = remote host
   *
   */
  public static native boolean connect(String remote);

  public static String getHostname() {
    try {
      FileInputStream fis = new FileInputStream("/etc/hostname");
      String hostname = new String(fis.readAllBytes()).trim();
      fis.close();
      return hostname;
    } catch (Exception e) {
      JFLog.log(e);
      return "localhost";
    }
  }
}

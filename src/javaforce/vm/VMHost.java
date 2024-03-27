package javaforce.vm;

/** Provides some statistics of the VM Host.
 *
 * @author pquiring
 */

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
}

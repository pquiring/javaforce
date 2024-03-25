package javaforce.vm;

/** Provides some statistics of the VM Host.
 *
 * @author pquiring
 */

public class VMHost {
  public static native long total_memory();
  public static native long free_memory();
  public static native long cpu_load();
}

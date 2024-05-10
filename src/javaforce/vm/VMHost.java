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

  /** This function will collect stats for all running VMs and save to local file system.
   * Interval should be every 20 seconds (4320 per day) (180 per hour)
   * Stats are saved to /var/jfkvm/stats/{vm-uuid}/{cpu,memory,disk,network}-yyyy-mm-dd-hh.stat
   *
   * @param year = year
   * @param month = month (1-12)
   * @param day = day of month
   * @param hour = hour (24)
   * @param sample = (0-180)
   */
  public static native boolean get_all_stats(int year, int month, int day, int hour, int sample);

  private static final long ms_day = 1000L * 60L * 60L * 24L;

  /** Delete old stats from all VMs.
   * Call once a day.
   * @param days = # of days to keep stats
   */
  public static void clean_stats(int days) {
    long ts = System.currentTimeMillis() - (days * ms_day);
    File[] folders = new File("/var/jfkvm/stats").listFiles();
    if (folders == null || folders.length == 0) return;
    for(File folder : folders) {
      File[] files = folder.listFiles();
      int deleted = 0;
      for(File file : files) {
        if (file.lastModified() < ts) {
          file.delete();
          deleted++;
        }
      }
      if (files.length == deleted) {
        folder.delete();
      }
    }
  }

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

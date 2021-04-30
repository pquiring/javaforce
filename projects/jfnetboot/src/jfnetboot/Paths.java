package jfnetboot;

/** Paths
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Paths {
  public static void init() {
    filesystems = "/var/netboot/filesystems";
    clients = "/var/netboot/clients";
    config = "/etc/netboot";
    logs = JF.getLogPath() + "/netboot";
    new File(filesystems).mkdirs();
    new File(clients).mkdirs();
    new File(config).mkdirs();
    new File(logs).mkdirs();
  }

  public static String filesystems;
  public static String clients;
  public static String config;
  public static String logs;
}

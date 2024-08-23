package javaforce.utils;

/** WinService
 *
 * Windows Service API
 *
 * @author peter.quiring
 */

import javaforce.*;

public class WinService {
  public static boolean create(String name, String exe) {
    ShellProcess sp = new ShellProcess();
    String windir = System.getenv("windir");
    String output = sp.run(new String[] {windir + "/system32/sc.exe", "create", name, "binPath=", exe, "start=", "auto"}, true);
    return sp.getErrorLevel() == 0;
  }

  public static boolean delete(String name) {
    ShellProcess sp = new ShellProcess();
    String windir = System.getenv("windir");
    String output = sp.run(new String[] {windir + "/system32/sc.exe", "delete", name}, true);
    return sp.getErrorLevel() == 0;
  }

  public static boolean start(String name) {
    ShellProcess sp = new ShellProcess();
    String windir = System.getenv("windir");
    String output = sp.run(new String[] {windir + "/system32/sc.exe", "start", name}, true);
    return sp.getErrorLevel() == 0;
  }

  public static boolean stop(String name) {
    ShellProcess sp = new ShellProcess();
    String windir = System.getenv("windir");
    String output = sp.run(new String[] {windir + "/system32/sc.exe", "stop", name}, true);
    return sp.getErrorLevel() == 0;
  }
}

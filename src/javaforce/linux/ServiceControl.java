package javaforce.linux;

/** Service Control (systemctl)
 *
 * @author pquiring
 */

import javaforce.*;

public class ServiceControl {
  public static int logid = 0;
  public static void setLog(int id) {
    logid = id;
  }
  public static boolean start(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "start", name}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean stop(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "stop", name}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean restart(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "restart", name}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean enable(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "enable", name}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean disable(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "disable", name}, true);
    JFLog.log(logid, output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean isEnabled(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "status", name}, true);
    JFLog.log(logid, output);
    boolean enabled = false;
    boolean active = false;
    String[] lns = output.split("\n");
    for(String ln : lns) {
      ln = ln.trim();
      if (ln.startsWith("Loaded:")) {
        int i1 = ln.indexOf(';');
        int i2 = ln.indexOf(';', i1 + 1);
        String state = ln.substring(i1 + 1, i2);
        enabled = state.trim().equals("enabled");
        continue;
      }
      if (ln.startsWith("Active:")) {
        ln = ln.substring(7).trim();
        active = ln.startsWith("active");
        continue;
      }
    }
    return enabled;
  }
  public static boolean isActive(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "status", name}, true);
    JFLog.log(logid, output);
    boolean enabled = false;
    boolean active = false;
    String[] lns = output.split("\n");
    for(String ln : lns) {
      ln = ln.trim();
      if (ln.startsWith("Loaded:")) {
        int i1 = ln.indexOf(';');
        int i2 = ln.indexOf(';', i1 + 1);
        String state = ln.substring(i1 + 1, i2);
        enabled = state.trim().equals("enabled");
        continue;
      }
      if (ln.startsWith("Active:")) {
        ln = ln.substring(7).trim();
        active = ln.startsWith("active");
        continue;
      }
    }
    return active;
  }
  /** return = [name, enabled, active] */
  public static String[] getStates(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "status", name}, true);
    JFLog.log(logid, output);
    String enabled = "n/a";
    String active = "n/a";
    String[] lns = output.split("\n");
    for(String ln : lns) {
      ln = ln.trim();
      if (ln.startsWith("Loaded:")) {
        int i1 = ln.indexOf(';');
        int i2 = ln.indexOf(';', i1 + 1);
        if (i2 == -1) {
          i2 = ln.indexOf(')', i1 + 1);
        }
        String state = ln.substring(i1 + 1, i2).trim();
        switch (state) {
          case "enabled": enabled = "true"; break;
          case "disabled": enabled = "false"; break;
          default: enabled = state; break;
        }
        continue;
      }
      if (ln.startsWith("Active:")) {
        ln = ln.substring(7).trim();
        if (ln.startsWith("active")) {
          active = "true";
        } else {
          active = "false";
        }
        continue;
      }
    }
    String[] states = new String[3];
    states[0] = name;
    states[1] = enabled;
    states[2] = active;
    return states;
  }
}

package javaforce.linux;

/** Service Control (systemctl)
 *
 * @author pquiring
 */

import javaforce.*;

public class ServiceControl {
  public static boolean start(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "start", name}, true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean stop(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "stop", name}, true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean restart(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "restart", name}, true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean enable(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "enable", name}, true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean disable(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "disable", name}, true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }
  public static boolean isEnabled(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "status", name}, true);
    JFLog.log(output);
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
        ln = ln.substring(7);
        active = ln.startsWith("active");
      }
    }
    return enabled;
  }
  public static boolean isActive(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "status", name}, true);
    JFLog.log(output);
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
        ln = ln.substring(7);
        active = ln.startsWith("active");
      }
    }
    return active;
  }
  /** return = [name, enabled, active] */
  public static String[] getStates(String name) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/systemctl", "status", name}, true);
    JFLog.log(output);
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
        ln = ln.substring(7);
        active = ln.startsWith("active");
      }
    }
    String[] state = new String[3];
    state[0] = name;
    state[1] = Boolean.toString(enabled);
    state[2] = Boolean.toString(active);
    return state;
  }
}

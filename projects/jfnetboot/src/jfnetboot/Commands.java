package jfnetboot;

/** Commands
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Commands {
  public static HashMap<String, Command> cmds = new HashMap();

  public static String getConfigFile() {
    return Paths.config + "/commands.cfg";
  }

  public static void init() {
    File file = new File(getConfigFile());
    if (file.exists()) {
      try {
        FileInputStream fis = new FileInputStream(getConfigFile());
        byte[] data = fis.readAllBytes();
        String[] lns = new String(data).split("\n");
        for(String ln : lns) {
          if (ln.length() == 0) continue;
          if (ln.startsWith("#")) continue;
          int idx = ln.indexOf('=');
          if (idx == -1) continue;
          String key = ln.substring(0, idx);
          String value = ln.substring(idx + 1);
          cmds.put(key, new Command(key, value));
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    if (cmds.get("default") == null) {
      cmds.put("default", new Command("default", "chromium --kiosk file:///netboot/default.html"));
    }
  }

  public static void save() {
    StringBuilder sb = new StringBuilder();
    Command[] list = cmds.values().toArray(new Command[0]);
    for(Command cmd : list) {
      sb.append(cmd.name);
      sb.append('=');
      sb.append(cmd.exec);
      sb.append('\n');
    }
    try {
      FileOutputStream fos = new FileOutputStream(getConfigFile());
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static String getCmd(String name) {
    Command cmd = cmds.get(name);
    if (cmd != null) return cmd.exec;
    cmd = cmds.get("default");
    return cmd.exec;
  }

  public static Command[] getCommands() {
    return cmds.values().toArray(new Command[0]);
  }

  public static void add(String name, String exec) {
    cmds.put(name, new Command(name, exec));
  }

  public static void delete(Command cmd) {
    cmds.remove(cmd.name);
  }
}

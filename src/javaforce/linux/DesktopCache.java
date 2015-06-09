package javaforce.linux;

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.utils.*;

/**
 * Cache of .desktop files
 *
 * @author pquiring
 *
 * Created : Jan 9, 2014
 */

public class DesktopCache implements monitordir.Listener {
  public static class Desktop {
    public String icon, file, name, exec, all;
  }
  private static Vector<Desktop> desktops = new Vector<Desktop>();  //synchronized

  private static void buildCache(File folder) {
    File files[] = folder.listFiles();
    for(int a=0;a<files.length;a++) {
      try {
        if (files[a].isDirectory()) {
          buildCache(files[a]);
          continue;
        }
        String file = files[a].getAbsolutePath();
        if (!file.endsWith(".desktop")) continue;
        FileInputStream fis = new FileInputStream(files[a]);
        byte data[] = JF.readAll(fis);
        String str = new String(data);
        String lns[] = str.split("\n");
        String name = null, icon = null, type = null, exec = null;
        boolean desktopEntry = false;
        for(int b=0;b<lns.length;b++) {
          if (lns[b].startsWith("[Desktop Entry]")) {
            desktopEntry = true;
            continue;
          }
          if (lns[b].startsWith("[")) desktopEntry = false;
          if (!desktopEntry) continue;
          if (lns[b].startsWith("Name=")) {name = lns[b].substring(5); continue;}
          if (lns[b].startsWith("Icon=")) {icon = lns[b].substring(5); continue;}
          if (lns[b].startsWith("Type=")) {type = lns[b].substring(5); continue;}
          if (lns[b].startsWith("Exec=")) {exec = lns[b].substring(5); continue;}
        }
        if ((name == null) || (icon == null) || (type == null)) continue;
        if (!type.equals("Application")) continue;
        boolean exists = false;
        for(int b=0;b<desktops.size();b++) {
          if (desktops.get(b).name.equals(name)) {exists = true; break;}
        }
        if (exists) continue;
        Desktop app = new Desktop();
        app.icon = icon;
        app.name = name;
        app.file = file;
        app.exec = exec;
        app.all = str.toLowerCase();
        desktops.add(app);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public synchronized static void buildCache() {
    File file1 = new File("/usr/share/applications");
    if (file1.exists() && file1.isDirectory()) {
      buildCache(file1);
      monitorDir(file1);
    }
    File file2 = new File(JF.getUserPath() + "/.local/share/applications");
    if (file2.exists() && file2.isDirectory()) {
      buildCache(file2);
      monitorDir(file2);
    }
  }

  private static ArrayList<String> monitors = new ArrayList<String>();

  private static synchronized void monitorDir(File folder) {
    String fullpath = folder.getAbsolutePath();
    for(int a=0;a<monitors.size();a++) {
      if (monitors.get(a).equals(fullpath)) return;
    }
    if (!monitordir.init()) {
      JFLog.log("DesktopCache:Error:Could not init monitordir");
    }
    int wd = monitordir.add(fullpath);
    monitordir.setListener(wd, new DesktopCache());
    monitors.add(fullpath);
  }

  private static String readFile(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      //NOTE: fis.available() is ALWAYS zero
      byte data[] = new byte[1024];
      int pos = 0;
      do {
        int read = fis.read(data, pos, 1024 - pos);
        if (read <= 0) break;
        pos += read;
      } while (pos < 1024);
      fis.close();
      return new String(data, 0, pos);
    } catch (Exception e) {
      return null;
    }
  }

  private static boolean isInterpreter(String cmd) {
    if (cmd.equals("bash")) return true;
    if (cmd.equals("wine")) return true;
    if (cmd.equals("python")) return true;
    return false;
  }

  public static String getDesktopFromPID(int pid) {
    if (pid == -1) return null;
    try {
      String cmdline = readFile("/proc/" + pid + "/cmdline");
      if (cmdline == null) return null;
      String args[] = cmdline.split(new String(new char[] {0}));
      String stat = readFile("/proc/" + pid + "/stat");
      if (stat == null) return null;
      String stats[] = stat.split(" ");
      int ppid = JF.atoi(stats[3]);
      String cmd = args[0];
      int idx = cmd.lastIndexOf('/');
      if (idx != -1) cmd = cmd.substring(idx+1);
      if (isInterpreter(cmd)) {
        cmd = args[1];
        idx = cmd.lastIndexOf('/');
        if (idx != -1) cmd = cmd.substring(idx+1);
      }
      if (cmd.equals("java")) {
        //try parent process
        if (ppid > 0) return getDesktopFromPID(ppid);
        return null;
      }
//      JFLog.log("cmd for " + pid + "=" + cmd);  //test
      return DesktopCache.getDesktopFromExec(cmd);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  public static String getDesktopFromExec(String exec) {
    for(int a=0;a<desktops.size();a++) {
      Desktop app = desktops.get(a);
      String appExec = app.exec;
      int idx = appExec.lastIndexOf('/');
      if (idx != -1) appExec = appExec.substring(idx + 1);
      if (appExec.equalsIgnoreCase(exec)) return app.file;
    }
    return null;
  }

  public static String getDesktopFromText(String text) {
    text = text.toLowerCase();
    int idx;
    idx = text.indexOf(' ');
    if (idx != -1) text = text.substring(0, idx);
    idx = text.indexOf('-');
    if (idx != -1) text = text.substring(0, idx);
    idx = text.indexOf('/');
    if (idx != -1) text = text.substring(0, idx);
    idx = text.indexOf('~');
    if (idx != -1) text = text.substring(0, idx);
    for(int a=0;a<desktops.size();a++) {
      Desktop app = desktops.get(a);
      if (app.all.indexOf(text) != -1) return app.file;
    }
    return null;
  }

  public static java.util.List<Desktop> getList() {
    return desktops;
  }

  public void folderChangeEvent(String event, String path) {
    buildCache();
  }
}

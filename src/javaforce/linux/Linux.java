package javaforce.linux;

/**
 * Created : Mar 12, 2012
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.jni.*;

/**
 * Common functions for Linux administration.
 */

public class Linux {

  /** Returns jfLinux ISO version. */
  public static String getVersion() {
    return JF.getVersion();
  }

  public static enum DistroTypes {
    Unknown, Ubuntu, Fedora
  };
  public static DistroTypes distro = DistroTypes.Unknown;

  /**
   * Detects Linux distribution type. (Support Ubuntu, Fedora currently)
   */
  public static boolean detectDistro() {
    if (distro != DistroTypes.Unknown) {
      return true;
    }
    try {
      //Debian/Ubuntu : /etc/os-release
      File lsb = new File("/etc/os-release");
      if (lsb.exists()) {
        FileInputStream fis = new FileInputStream(lsb);
        Properties props = new Properties();
        props.load(fis);
        fis.close();
        String id = props.getProperty("ID");
        if (id.equals("debian") || id.equals("ubuntu")) {
          distro = DistroTypes.Ubuntu;
          JFLog.log("Detected Linux:debian");
          return true;
        }
        if (id.equals("fedora")) {
          distro = DistroTypes.Fedora;
          JFLog.log("Detected Linux:fedora");
          return true;
        }
      }
      JFLog.log("Error:Unknown distro");
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public static boolean ubuntuAddRepo(String ppa) {
    ShellProcess sp = new ShellProcess();
    sp.removeEnvironmentVariable("TERM");
    sp.addEnvironmentVariable("DEBIAN_FRONTEND", "noninteractive");
    String output = sp.run(new String[] {"sudo", "-E", "add-apt-repository", ppa}, true);
    if (output == null) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Creates folder as root
   */
  public static boolean mkdir(String folder) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("mkdir");
    cmd.add("-p");
    cmd.add(folder);
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Failed to exec mkdir");
      return false;
    }
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /**
   * Copies src to dst as root
   */
  public static boolean copyFile(String src, String dst) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("cp");
    cmd.add(src);
    cmd.add(dst);
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Failed to exec cp");
      return false;
    }
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /**
   * Creates Link to Target as root
   */
  public static boolean createLink(String target, String link) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("ln");
    cmd.add("-s");
    cmd.add(target);
    cmd.add(link);
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Failed to exec ln");
      return false;
    }
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /**
   * Deletes file as root
   */
  public static boolean deleteFile(String file) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("rm");
    cmd.add("-f");
    cmd.add(file);
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Failed to exec ln");
      return false;
    }
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /**
   * Restarts a service
   */
  public static boolean restartService(String name) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("service");
    cmd.add(name);
    cmd.add("restart");
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Failed to exec service");
      return false;
    }
    if (sp.getErrorLevel() != 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /**
   * Restarts a JF service
   */
  public static boolean restartJFService(String name) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("jservice");
    cmd.add(name);
    cmd.add("restart");
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Failed to exec jservice");
      return false;
    }
    if (sp.getErrorLevel() != 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  public static boolean ubuntuUpdate() {
    ShellProcess sp = new ShellProcess();
    sp.removeEnvironmentVariable("TERM");
    sp.addEnvironmentVariable("DEBIAN_FRONTEND", "noninteractive");
    String output = sp.run(new String[] {"sudo", "-E", "apt", "--yes", "update"}, true);
    if (output == null) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Work with Ubuntu packages.
   */
  private static boolean apt(String action, String pkg, String desc) {
    JFTask task = new JFTask() {
      public boolean work() {
        this.setProgress(-1);
        String action = (String)this.getProperty("action");
        String pkg = (String)this.getProperty("pkg");
        String desc = (String)this.getProperty("desc");
        setLabel((action.equals("install") ? "Installing " : "Removing ") + desc);
        ShellProcess sp = new ShellProcess();
        sp.removeEnvironmentVariable("TERM");
        sp.addEnvironmentVariable("DEBIAN_FRONTEND", "noninteractive");
        String output = sp.run(new String[]{"sudo", "-E", "apt", "--yes", action, pkg}, true);
        if (output == null) {
          setLabel("Failed to exec apt");
          JFLog.log("Failed to exec apt");
          return false;
        }
        if (output.indexOf("Unable to locate package") != -1) {
          setLabel("Package not found");
          JFLog.log("Package not found");
          return false;
        }
        setLabel("Complete");
        setProgress(100);
        return true;
      }
    };
    task.setProperty("action", action);
    task.setProperty("pkg", pkg);
    task.setProperty("desc", desc);
    new ProgressDialog(null, true, task).setVisible(true);
    return task.getStatus();
  }

  /**
   * Work with Fedora packages.
   */
  private static boolean yum(String action, String pkg, String desc) {
    JFTask task = new JFTask() {
      public boolean work() {
        this.setProgress(-1);
        String action = (String)this.getProperty("action");
        String pkg = (String)this.getProperty("pkg");
        String desc = (String)this.getProperty("desc");
        setLabel((action.equals("install") ? "Installing " : "Removing ") + desc);
        ShellProcess sp = new ShellProcess();
        sp.removeEnvironmentVariable("TERM");  //prevent config dialogs
        String output = sp.run(new String[]{"sudo", "-E", "yum", "-y", action, pkg}, false);
        if (output == null) {
          setLabel("Failed to exec yum");
          JFLog.log("Failed to exec yum");
          return false;
        }
        setLabel("Complete");
        setProgress(100);
        return true;
      }
    };
    task.setProperty("action", action);
    task.setProperty("pkg", pkg);
    task.setProperty("desc", desc);
    new ProgressDialog(null, true, task).setVisible(true);
    return task.getStatus();
  }

  public static boolean installPackage(String pkg, String desc) {
    detectDistro();
    switch (distro) {
      case Ubuntu:
        return apt("install", pkg, desc);
      case Fedora:
        return yum("install", pkg, desc);
    }
    return false;
  }

  public static boolean removePackage(String pkg, String desc) {
    detectDistro();
    switch (distro) {
      case Ubuntu:
        return apt("autoremove", pkg, desc);
      case Fedora:
        return yum("remove", pkg, desc);
    }
    return false;
  }

  /**
   * Sets file as executable as root
   */
  public static boolean setExec(String file) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("chmod");
    cmd.add("+x");
    cmd.add(file);
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Failed to exec chmod");
      return false;
    }
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }
  private static String[][] pkgList;

  /*  public static String[][] getPackages() {
   if (dpkg == null) updateInstalled();
   return dpkg;
   }*/
  private static String[][] ubuntu_searchPackages(String regex) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("apt-cache");
    cmd.add("search");
    cmd.add(regex);
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Error:unable to execute apt-cache");
      return null;
    }
    String[] lns = output.split("\n");
    String[][] ret = new String[lns.length][2];
    String[] f;
    for (int a = 0; a < lns.length; a++) {
      f = lns[a].split(" - ");
      if (f.length != 2) {
        continue;
      }
      ret[a][0] = f[0];  //package name
      ret[a][1] = f[1];  //package desc
    }
    return ret;
  }

  private static String[][] fedora_searchPackages(String regex) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("yum");
    cmd.add("search");
    cmd.add(regex);
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Error:unable to execute yum");
      return null;
    }
    String[] lns = output.split("\n");
    String[][] ret = new String[lns.length][2];
    String[] f;
    for (int a = 0; a < lns.length; a++) {
      f = lns[a].split(" : ");
      if (f.length != 2) {
        continue;
      }
      ret[a][0] = f[0];  //package name
      ret[a][1] = f[1];  //package desc
    }
    return ret;
  }

  /**
   * Searches for available packages (NOTE:There may be nulls in the output)
   */
  public static String[][] searchPackages(String regex) {
    detectDistro();
    switch (distro) {
      case Ubuntu:
        return ubuntu_searchPackages(regex);
      case Fedora:
        return fedora_searchPackages(regex);
    }
    return null;
  }

  public static void ubuntu_updateInstalled() {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("dpkg");
    cmd.add("-l");
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Error:unable to execute dpkg");
      return;
    }
    String[] lns = output.split("\n");
    pkgList = new String[lns.length - 5][3];
    String[] f;
    for (int a = 5; a < lns.length; a++) {
      f = lns[a].split(" +", 4);  //greedy spaces
      pkgList[a - 5][0] = f[1];  //package name
      pkgList[a - 5][1] = f[3];  //package desc
      pkgList[a - 5][2] = (f[0].charAt(0) == 'i' ? "true" : "false");  //package installed?
    }
  }

  public static void fedora_updateInstalled() {
    //NOTE:can't use "rpm -qa" because the version # is mangled in the name
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("yum");
    cmd.add("list");
    cmd.add("installed");
    String output = sp.run(cmd, false);
    if (output == null) {
      JFLog.log("Error:unable to execute yum");
      return;
    }
    String lns[] = output.split("\n");
    pkgList = new String[lns.length - 2][3];
    for (int a = 2; a < lns.length; a++) {
      String f[] = lns[a].split(" +");  //greedy spaces
      if (f.length != 3) {
        pkgList[a-2][0] = "";
        pkgList[a-2][1] = "";
        pkgList[a-2][2] = "";
        continue;
      }
      int idx = f[0].lastIndexOf(".");  //strip arch
      if (idx == -1) {
        pkgList[a-2][0] = f[0];  //package name
      } else {
        pkgList[a-2][0] = f[0].substring(0, idx);  //package name
      }
      pkgList[a-2][1] = f[1];  //package desc
      pkgList[a-2][2] = "true";  //package installed?
    }
  }

  public static void updateInstalled() {
    detectDistro();
    switch (distro) {
      case Ubuntu:
        ubuntu_updateInstalled();
        break;
      case Fedora:
        fedora_updateInstalled();
        break;
    }
  }

  public static boolean isInstalled(String pkg) {
    if (pkg == null) {
      return true;
    }
    if (pkgList == null) {
      updateInstalled();
    }
    for (int a = 0; a < pkgList.length; a++) {
      if (pkgList[a][0].equals(pkg)) {
        return pkgList[a][2].equals("true");
      }
    }
    return false;
  }

  public static String getPackageDesc(String pkg) {
    if (pkg == null) {
      return "";
    }
    if (pkgList == null) {
      updateInstalled();
      if (pkgList == null) {
        return "";
      }
    }
    for (int a = 0; a < pkgList.length; a++) {
      if (pkgList[a][0].equals(pkg)) {
        return pkgList[a][1];
      }
    }
    return "";
  }

  public static boolean isMemberOf(String user, String group) {
    try {
      ShellProcess sp = new ShellProcess();
      String output = sp.run(new String[]{"groups", user}, false).replaceAll("\n", "");
      //output = "user : group1 group2 ..."
      String groups[] = output.split(" ");
      for (int a = 2; a < groups.length; a++) {
        if (groups[a].equals(group)) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private static String expandQuotes(String inString) {
    while (true) {
      int i1 = inString.indexOf('\"');
      if (i1 == -1) {
        return inString;
      }
      int i2 = inString.indexOf('\"', i1 + 1);
      if (i2 == -1) {
        return inString;
      }
      inString = inString.substring(0, i1) + inString.substring(i1 + 1, i2).replace(' ', '\u1234') + inString.substring(i2 + 1);
    }
  }

  private static String[] expandBackslash(String inString) {
    StringBuilder out = new StringBuilder();
    char inCA[] = inString.toCharArray();
    for (int a = 0; a < inCA.length; a++) {
      if (inCA[a] == '\\') {
        if (inCA[a + 1] == '\\') {
          if (inCA[a + 2] == '\\') {
            if (inCA[a + 3] == '\\') {
              out.append('\\');
              out.append('\\');
              a += 3;
              continue;
            }
          } else if (inCA[a + 2] == ' ') {
            out.append('\u1234');
            a += 2;
            continue;
          } else {
            out.append('\\');
            a++;
            continue;
          }
        }
      }
      out.append(inCA[a]);
    }
    String cmd[] = out.toString().split(" ");
    for (int a = 0; a < cmd.length; a++) {
      if (cmd[a].indexOf('\u1234') != -1) {
        cmd[a] = cmd[a].replace('\u1234', ' ');
      }
    }
    return cmd;
  }

  /**
   * Currently only supports %f,%F,%u,%U
   */
  public static String[] expandDesktopExec(String exec, String file) {
    if (file == null) {
      file = "";
    }
    file = '\"' + file + '\"';
    exec = exec.replaceAll("%f", file);
    exec = exec.replaceAll("%F", file);
    exec = exec.replaceAll("%u", file);
    exec = exec.replaceAll("%U", file);
    exec = expandQuotes(exec);
    return expandBackslash(exec);
  }

  /**
   * Currently only supports %f,%F,%u,%U
   */
  public static String[] expandDesktopExec(String exec, String file[]) {
    String files = "";
    if (file != null) {
      for (int a = 0; a < file.length; a++) {
        if (a > 0) {
          files += " ";
        }
        files += '\"' + file[a] + '\"';
      }
    } else {
      file = new String[1];
      file[0] = "";
    }
    exec = exec.replaceAll("%f", '\"' + file[0] + '\"');
    exec = exec.replaceAll("%F", files);
    exec = exec.replaceAll("%u", '\"' + file[0] + '\"');
    exec = exec.replaceAll("%U", files);
    exec = expandQuotes(exec);
    return expandBackslash(exec);
  }

  public static boolean executeDesktop(String desktop, String file[]) {
    try {
      Properties props = new Properties();
      FileInputStream fis = new FileInputStream(desktop);
      props.load(fis);
      fis.close();
      String exec = props.getProperty("Exec");
      String path = props.getProperty("Path");
      if (path == null || path.length() == 0) {
        path = System.getenv("HOME");
        if (path == null || path.length() == 0) {
          path = "/";
        }
      }
      ProcessBuilder pb = new ProcessBuilder();
      pb.directory(new File(path));
      String expand[] = expandDesktopExec(exec, file);
      pb.command(expand);
      pb.start();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static int detectBits() {
    if (new File("/usr/lib64").exists()) {
      return 64;
    }
    return 32;
  }

  /**
   * Runs a bash script as root
   */
  public static boolean runScript(String lns[]) {
    try {
      File tmpFile = File.createTempFile("script", ".sh", new File("/tmp"));
      FileOutputStream fos = new FileOutputStream(tmpFile);
      fos.write("#!/bin/bash\n".getBytes());
      for (int a = 0; a < lns.length; a++) {
        fos.write((lns[a] + "\n").getBytes());
      }
      fos.close();
      tmpFile.setExecutable(true);
      ShellProcess sp = new ShellProcess();
      String output = sp.run(new String[]{"sudo", tmpFile.getAbsolutePath()}, true);
      tmpFile.delete();
      return sp.getErrorLevel() == 0;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private static final int None = 0;
  private static final int CurrentTime = 0;
  private static final int ShiftMask = 1;

  private static final int True = 1;
  private static final int False = 0;

  private static final int KeyPress = 2;
  private static final int KeyRelease = 3;
  private static final int CreateNotify = 16;
  private static final int DestroyNotify = 17;
  private static final int UnmapNotify = 18;
  private static final int MapNotify = 19;
  private static final int ReparentNotify = 21;
  private static final int ConfigureNotify = 22;
  private static final int PropertyNotify = 28;
  private static final int ClientMessage = 33;

  private static final int KeyPressMask = 1;
  private static final int KeyReleaseMask = (1 << 1);
  private static final int StructureNotifyMask = (1 << 17);
  private static final int SubstructureNotifyMask = (1 << 19);
  private static final int PropertyChangeMask = (1 << 22);

  public static boolean init() {
    return true;
  }

  public static long x11_get_id(java.awt.Window w) {
    return LnxNative.x11_get_id(w);
  }

  public static void x11_set_desktop(long xid) {
    LnxNative.x11_set_desktop(xid);
  }

  public static void x11_set_dock(long xid) {
    LnxNative.x11_set_dock(xid);
  }

  public static void x11_set_strut(long xid, int panelHeight, int x, int y, int width, int height) {
    LnxNative.x11_set_strut(xid, panelHeight,x,y,width,height);
  }

//tray functions

  public static void x11_set_listener(X11Listener cb) {
    LnxNative.x11_set_listener(cb);
  }

  /** Polls and dispatches tray events.  Does not return until x11_tray_stop() is called. */
  public static void x11_tray_main(long pid, int screenWidth, int trayPos, int trayHeight) {
    LnxNative.x11_tray_main(pid, screenWidth, trayPos, trayHeight);
  }

  /** Repositions tray icons and the tray window itself.
   *
   * @param screenWidth = new screen width (-1 = has not changed)
   */
  public static void x11_tray_reposition(int screenWidth, int trayPos, int trayHeight) {
    LnxNative.x11_tray_reposition(screenWidth, trayPos, trayHeight);
  }

  /** Stops x11_tray_main() */
  public static void x11_tray_stop() {
    LnxNative.x11_tray_stop();
  }

  public static int x11_tray_width() {
    return LnxNative.x11_tray_width();
  }

//top-level x11 windows monitor

  /** Polls and dispatches top-level windows events.  Does not return until x11_window_list_stop() is called. */
  public static void x11_window_list_main() {
    LnxNative.x11_window_list_main();
  }

  public static void x11_window_list_stop() {
    LnxNative.x11_window_list_stop();
    //TODO : send a message to ??? to cause main() loop to abort
  }

  public static class Window {
    public long xid;
    public int pid;
    public String title;  //_NET_WM_NAME
    public String name;  //XFetchName
    public String res_name, res_class;
    public String file;  //user defined
    public long org_event_mask;
    public Window(long xid, int pid, String title, String name, String res_name, String res_class) {
      this.xid = xid;
      this.pid = pid;
      this.title = title;
      this.name = name;
      this.res_name = res_name;
      this.res_class = res_class;
    }
  }

  private static ArrayList<Window> currentList = new ArrayList<Window>();
  private static Object currentListLock = new Object();

  /** Returns list of all top-level windows.
   * NOTE : x11_window_list_main() must be running.
   */
  public static Window[] x11_get_window_list() {
    //create a "copy" of currentList
    synchronized(currentListLock) {
      return currentList.toArray(new Window[0]);
    }
  }

  //called from native code to add/update a window
  private static void x11_window_add(long xid, int pid, String title, String name, String res_name, String res_class) {
    Window window = new Window(xid, pid, title, name, res_name, res_class);
    synchronized(currentListLock) {
      int cnt = currentList.size();
      for(int a=0;a<cnt;a++) {
        if (currentList.get(a).xid == xid) {
          currentList.set(a, window);
          return;
        }
      }
      currentList.add(window);
    }
  }

  //called from native code to delete a window
  private static void x11_window_del(long xid) {
    synchronized(currentListLock) {
      int cnt = currentList.size();
      for(int a=0;a<cnt;a++) {
        if (currentList.get(a).xid == xid) {
          currentList.remove(a);
          return;
        }
      }
    }
  }

  public static void x11_minimize_all() {
    LnxNative.x11_minimize_all();
  }

  public static void x11_raise_window(long xid) {
    LnxNative.x11_raise_window(xid);
  }

  public static void x11_map_window(long xid) {
    LnxNative.x11_map_window(xid);
  }

  public static void x11_unmap_window(long xid) {
    LnxNative.x11_unmap_window(xid);
  }

//x11 send event functions

  public static int x11_keysym_to_keycode(char keysym) {
    return LnxNative.x11_keysym_to_keycode(keysym);
  }

  /** Send keyboard event to window with focus. */
  public static boolean x11_send_event(int keycode, boolean down) {
    return LnxNative.x11_send_event(keycode, down);
  }

  /** Send keyboard event to specific window. */
  public static boolean x11_send_event(long id, int keycode, boolean down) {
    return LnxNative.x11_send_event(id, keycode, down);
  }

  //X11 : RandR support

  public static class Size {
    public String size;
    public boolean active;
    public boolean preferred;
  }

  public static class Port {
    public String name;
    public boolean connected;
    public boolean hasActiveSize;
    public boolean used;  //mapped to Monitor
    public ArrayList<Size> sizes = new ArrayList<Size>();
  }

  public static class Screen {
    public int idx;
    public ArrayList<Port> ports = new ArrayList<Port>();
  }

  public static class Monitor {
    public String name;
    public String res = "";  //resolution
    public int rotate;  //NORMAL, RIGHT, LEFT, INVERTED
    public boolean mirror;  //relpos ignored
    public int relpos;  //NONE, LEFT, RIGHT, BELOW, ABOVE, SAME
    public String relName = "";  //relative to this monitor
  }

  public static final int P_NONE = 0;  //only primary uses P_NONE
  public static final int P_LEFT = 1;
  public static final int P_ABOVE = 2;
  public static final int P_RIGHT = 3;
  public static final int P_BELOW = 4;
  public static final int P_SAME = 5;

  public static final int R_NORMAL = 0;
  public static final int R_RIGHT = 1;  //CW
  public static final int R_LEFT = 2;  //CCW
  public static final int R_INVERTED = 3;

  public static ArrayList<Screen> screens = new ArrayList<Screen>();

  /** X11 : RandR : Detects current state.
   * @param config = current settings (optional)
   * @return new settings
   */
  public static Monitor[] x11_rr_get_setup(Monitor config[]) {
    //xrandr output:
    //--------------
    //Screen 0: minimum 1 x 1, current X x Y, maximum 8192 x 8192
    //MonitorName [dis]connected XxY+0+0 [rotation] (normal left inverted right x axis y axis) 0mm x 0mm
    //  XxY         freq*+    [freq2]    (where *=current +=preferred)
    //--------------
    //(where rotation = right, left, inverted)

    screens.clear();
    Screen screen = null;
    Port port = null;
    Size size = null;
    ArrayList<Monitor> newConfig = new ArrayList<Monitor>();

    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"xrandr"}, false);
    String lns[] = output.split("\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].length() == 0) continue;
      if (lns[a].startsWith("Screen ")) {
        screen = new Screen();
        int i1 = lns[a].indexOf(" ");
        int i2 = lns[a].indexOf(":");
        screen.idx = JF.atoi(lns[a].substring(i1+1, i2));
        screens.add(screen);
        continue;
      }
      if (lns[a].startsWith(" ")) {
        //size / rate*+  (*=in use +=preferred)
        if (port == null) {
          JFLog.log("Error:XRandR size line without monitor");
          continue;
        }
        if (!port.connected) continue;  //junk info
        size = new Size();
        String f[] = lns[a].trim().split(" +");  //greedy spaces
        size.size = f[0];
        size.active = f[1].indexOf("*") != -1;
        if (size.active) {
          port.hasActiveSize = true;
        }
        size.preferred = f[1].indexOf("+") != -1 || (f.length > 2 && f[2].equals("+"));
        port.sizes.add(size);
      } else {
        //port
        port = new Port();
        int i1 = lns[a].indexOf(" ");
        port.name = lns[a].substring(0, i1);
        port.connected = lns[a].substring(i1+1).startsWith("connected");
        screen.ports.add(port);
      }
    }
    if (config == null) config = new Monitor[0];
    for(int b=0;b<screens.size();b++) {
      screen = screens.get(b);
      for(int c=0;c<screen.ports.size();c++) {
        port = screen.ports.get(c);
        if (!port.connected) continue;  //ignore disconnected monitors
        Monitor monitor = new Monitor();
        monitor.name = port.name;
        for(int a=0;a<config.length;a++) {
          Monitor other = config[a];
          if (other.name.equals(port.name)) {
            monitor.res = other.res;
            monitor.mirror = other.mirror;
            monitor.relpos = other.relpos;
            monitor.relName = other.relName;
            monitor.rotate = other.rotate;
            break;
          }
        }
        if (monitor.res.length() == 0) {
          if (port.hasActiveSize) {
            //use active size
            for(int s=0;s<port.sizes.size();s++) {
              size = port.sizes.get(s);
              if (size.active) {
                monitor.res = size.size;
                break;
              }
            }
          } else {
            //use prefered size
            for(int s=0;s<port.sizes.size();s++) {
              size = port.sizes.get(s);
              if (size.preferred) {
                monitor.res = size.size;
                break;
              }
            }
          }
          if (monitor.res.length() == 0) {
            //no active or preferred size
            //use first size listed
            if (port.sizes.size() > 0) {
              monitor.res = port.sizes.get(0).size;
            } else {
              //no sizes???
              JFLog.log("Warning:Monitor " + monitor.name + " has no sizes, trying 800x600");
              monitor.res = "800x600";  //must use something or xrandr options will be corrupt
            }
          }
        }
        newConfig.add(monitor);
      }
    }
    return x11_rr_arrangeMonitors(newConfig.toArray(new Monitor[0]));
  }

  /** Arranges monitors ensuring they all have valid positions.
   * @return : the same list of monitors
   */
  public static Monitor[] x11_rr_arrangeMonitors(Monitor monitors[]) {
    for(int m=1;m<monitors.length;m++) {
      Monitor monitor = monitors[m];
      if (monitor.mirror) {
        monitor.relpos = P_SAME;
        //ensure mirrored monitor exists and is not a mirror itself
        String mirrorName = monitor.relName;
        boolean ok = false;
        for(int c=0;c<monitors.length;c++) {
          if (monitors[c].name.equals(mirrorName) && !monitors[c].mirror) {
            ok = true;
            break;
          }
        }
        if (ok) continue;
        //mirror is not valid
        monitor.relpos = P_NONE;
        monitor.relName = "";
      }
      if (monitor.relpos != P_NONE && monitor.relpos != P_SAME) {
        //make sure path exists and is not circular
        Monitor path = monitor;
        boolean ok = true;
        while (ok && path != monitors[0]) {
          String parent = path.relName;
          Monitor thisMonitor = path;
          for(int c=0;c<monitors.length;c++) {
            if (monitors[c].name.equals(parent)) {
              path = monitors[c];
              break;
            }
          }
          if (thisMonitor == path) {
            //parent not found
            ok = false;
          }
          if (path == monitor) {
            //circular
            ok = false;
          }
        }
        if (ok) continue;
      }
      Monitor rightMonitor = null;
      String relName = monitors[0].name;
      boolean left = false, right = false, above = false, below = false;
      for(int c=1;c<monitors.length;c++) {
        if (!monitors[c].name.equals(relName)) continue;
        if (monitors[c].relpos == P_LEFT) left = true;
        else if (monitors[c].relpos == P_RIGHT) {right = true; rightMonitor = monitors[c];}
        else if (monitors[c].relpos == P_ABOVE) above = true;
        else if (monitors[c].relpos == P_BELOW) below = true;
      }
      monitor.relName = monitors[0].name;
      if (!right) monitor.relpos = P_RIGHT;
      else if (!below) monitor.relpos = P_BELOW;
      else if (!left) monitor.relpos = P_LEFT;
      else if (!above) monitor.relpos = P_ABOVE;
      else {
        //need to make adj to another monitor (just keep moving right)
        relName = rightMonitor.name;
        boolean cont = false;
        do {
          cont = false;
          for(int c=1;c<monitors.length;c++) {
            if (!monitors[c].relName.equals(relName)) continue;
            if (monitors[c].relpos == P_RIGHT) {
              cont = true;
              rightMonitor = monitors[c];
              relName = rightMonitor.name;
              break;
            }
          }
        } while (cont);
        monitor.relName = rightMonitor.name;
        monitor.relpos = P_RIGHT;
      }
    }
    return monitors;
  }


  /** X11 : RandR : Reconnects disconnected monitors. */
  public static void x11_rr_auto() {
    try {
      Process p = Runtime.getRuntime().exec(new String[] {"xrandr", "--auto"});
      p.waitFor();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** X11 : RandR : Applies config. */
  public static void x11_rr_set(Monitor config[]) {
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("xrandr");
    for(int a=0;a<config.length;a++) {
      cmd.add("--output");
      cmd.add(config[a].name);
      cmd.add("--mode");
      cmd.add(config[a].res);
      cmd.add("--rotate");
      switch (config[a].rotate) {
        case R_NORMAL: cmd.add("normal"); break;
        case R_LEFT: cmd.add("left"); break;
        case R_RIGHT: cmd.add("right"); break;
        case R_INVERTED: cmd.add("inverted"); break;
      }
      if (config[a].relpos != P_NONE) {
        switch (config[a].relpos) {
          case P_LEFT: cmd.add("--left-of"); break;
          case P_RIGHT: cmd.add("--right-of"); break;
          case P_ABOVE: cmd.add("--above"); break;
          case P_BELOW: cmd.add("--below"); break;
          case P_SAME: cmd.add("--same-as"); break;
        }
        cmd.add(config[a].relName);
      }
    }
/*
    for(int a=0;a<cmd.size();a++) {
      JFLog.log("cmd[]=" + cmd.get(a));
    }
*/
    try {
      Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
      p.waitFor();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** X11 : RandR : Sets only primary display to res and disable all other (for Logon Screen)
   * @param res = resolution (ie: 800x600)
   */
  public static void x11_rr_reset(String res) {
    Monitor config[] = x11_rr_get_setup(null);
    if (config == null || config.length == 0) return;
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("xrandr");
    cmd.add("--output");
    cmd.add(config[0].name);
    cmd.add("--mode");
    cmd.add(res);
    cmd.add("--rotate");
    cmd.add("normal");
    for(int a=1;a<config.length;a++) {
      cmd.add("--output");
      cmd.add(config[a].name);
      cmd.add("--off");
    }
    try {
      Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
      p.waitFor();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static class Config {
    public Monitor monitor[];
  }

  public static Monitor[] x11_rr_load_user() {
    Config config = new Config();
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(JF.getUserPath() + "/.xrandr.xml");
      xml.read(fis);
      xml.writeClass(config);
      fis.close();
      return config.monitor;
    } catch (FileNotFoundException e1) {
      return new Monitor[0];
    } catch (Exception e2) {
      JFLog.log(e2);
      return new Monitor[0];
    }
  }

  public static void x11_rr_save_user(Monitor monitor[]) {
    Config config = new Config();
    config.monitor = monitor;
    try {
      XML xml = new XML();
      FileOutputStream fos = new FileOutputStream(JF.getUserPath() + "/.xrandr.xml");
      xml.readClass("display", config);
      xml.write(fos);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //PAM functions

  private static final int PAM_SILENT = 0x8000;
  private static final int PAM_PROMPT_ECHO_ON = 2;
  private static final int PAM_PROMPT_ECHO_OFF = 1;

  private static String pam_user, pam_pass;
  private static long pam_responses;

  public static synchronized boolean authUser(String user, String pass) {
    return LnxNative.authUser(user, pass);
  }

  public static final int SIGKILL = 9;
  public static final int SIGTERM = 15;

  public static void kill(int pid, int signal) {
    try {
      Runtime.getRuntime().exec(new String[] {"kill", "-" + signal, "" + pid});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void setenv(String name, String value) {
    LnxNative.setenv(name, value);
  }

  /* Test */
  public static void main(String args[]) {
    x11_set_listener(new X11Listener() {
      public void trayIconAdded(int count) {
        System.out.println("tragIconAdded:" + count);
      }

      public void trayIconRemoved(int count) {
        System.out.println("tragIconRemoved:" + count);
      }

      public void windowsChanged() {
        System.out.println("windowsChanged");
      }
    });
    x11_window_list_main();
  }
}

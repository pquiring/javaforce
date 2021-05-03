/**
 * jfLinux Startup
 *  - includes org.jflinux.jfsystemmgr
 *
 * Created : Mar 31, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;
import javaforce.linux.Linux;
import javaforce.utils.monitordir;

public class Startup implements ShellProcessListener{
  private static ShellProcess display_mgr;
  private static boolean rebootFlag, shutdownFlag;
  private static boolean wayland = false;

  public static AutoMounter autoMounter;
  public static JBusClient jbusClient;

  /** Main entry point for jfLinux system.*/
  public static void main(String args[]) {
    JFLog.init("/var/log/jflogon.log", true);
    JFLog.log("jLogon:Startup");
    try {
      fixSudoers();
      Linux.init();
      monitordir.init();
      //start jfsystemmgr
      jbusClient = new JBusClient("org.jflinux.jfsystemmgr", new JBusMethods());
      jbusClient.start();
      //start automounter
      autoMounter = new AutoMounter();
      autoMounter.start();
      //start device monitor
      new DeviceMonitor().start();
      //stop plymouth
      hidePlymouth();
      if (!wayland) {
        create_server_xauth();
      }
      boolean retry;
      do {
        retry = false;
        try {
          if (wayland) {
            start(new String[] {"/usr/bin/weston" , "--config=/etc/weston-logon.ini", "--xwayland"});
          } else {
            start(new String[] {"/usr/bin/X"});
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
        JF.sleep(1500);  //wait for display manager to start

        JF.exec(new String[] {"numlockx", "on"});
        try {
          if (new File("/etc/.live").exists()) {
            doLiveLogon();
          } else {
            createLogon();
          }
        } catch (java.awt.HeadlessException he) {
          JFLog.log(he);
          JF.sleep(500);
          File xorgconf = new File("/etc/X11/xorg.conf");
          if (xorgconf.exists()) {
            JFLog.log("X Failed : Attempting to delete /etc/X11/xorg.conf and try again");
            xorgconf.delete();
          }
          stop();
          retry = true;
        } catch (Exception e) {
          JFLog.log(e);
          stop();
          retry = true;
        }
      } while (retry);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static void start(final String[] cmds) throws Exception {
    new Thread() {
      public void run() {
        display_mgr = new ShellProcess();
        display_mgr.keepOutput(false);
        display_mgr.addListener(new Startup());
        display_mgr.addEnvironmentVariable("XDG_RUNNING_DIR", "/run/weston");
        new File("/run/weston").mkdir();
        JFLog.log("Starting Display Server...");
        display_mgr.run(cmds, true);
      }
    }.start();
  }

  public static void stop() throws Exception {
    if (display_mgr != null) {
      JFLog.log("Stopping Display Manager...");
      display_mgr.destroy();
      JF.sleep(500);
      for(int a=0;a<3;a++) {
        if (!display_mgr.isAlive()) break;
        JF.sleep(1000);
      }
      if (display_mgr.isAlive()) {
        display_mgr.destroyForcibly();
        JF.sleep(500);
      }
      display_mgr = null;
      JFLog.log("Display Manager stopped...");
    }
  }

  public static byte mcookie[] = new byte[16];

  private static void create_server_xauth() throws Exception {
    //write auth data to /root/.Xauthority
    Random r = new Random();
    for(int a=0;a<16;a++) {
      mcookie[a] = (byte)('a' + (Math.abs(r.nextInt()) % 26));
    }
    write_xauth("/root/.Xauthority");
  }

  private static void write_xauth(String fn) throws Exception {
    FileOutputStream fos = new FileOutputStream(fn);
    fos.write(new byte[] { (byte)0xfc, 0x00 });  //uint16 = 252
    fos.write(new byte[] { 0x00, 0x00 });  //uint16 = 0 (string length)
    fos.write(new byte[] { 0x00, 0x00 });  //uint16 = 0 (string length)
    fos.write(new byte[] { 0x12, 0x00 });  //uint16 = 0x12 (string length)
    fos.write("MIT-MAGIC-COOKIE-1".getBytes());  //magic string
    fos.write(new byte[] { 0x10, 0x00 });  //uint16 = 0x10 (data length)
    fos.write(mcookie);  //cookie
    fos.close();
  }

  private static void chown_xauth(String fn, String user) throws Exception {
    try {
      JF.exec(new String[] {"chown", user+":"+user, fn});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static void doLiveLogon() {
    boolean casper = true;
    try {
      FileInputStream fis = new FileInputStream("/etc/.live");
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      String user = props.getProperty("user");
      if (user == null) user = "jflive";
      String casperFlag = props.getProperty("casper");
      if (casperFlag != null) casper = casperFlag.trim().equals("true");
      //run session as live user
      runSession(user, "/usr/bin/jfdesktop", null, null, false);
      stop();
      JF.sleep(1000);
      System.out.println("" + (char)0x1b + "[2J");  //clear screen
      System.out.println("\n\n\n\n\n\t\tPlease remove installation media and reboot\n\n\n\n\n");
//      shutdown("-H");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void runSession(String user, String session, String env_names[], String env_values[], boolean domainLogon) {
    try {
      getUserDetails(user);
      if (!wayland) {
        String xauthFile = homePath + "/.Xauthority";
        write_xauth(xauthFile);
        chown_xauth(xauthFile, user);
      }
      if (!Linux.isMemberOf(user, "audio")) {
        //pulseaudio requires user to be member of 'audio' group
        JF.exec(new String[] {"usermod", "-aG", "audio", user});
      }
      if (!Linux.isMemberOf(user, "sambashare")) {
        //net usershare requires user to be member of 'sambashare' group
        JF.exec(new String[] {"usermod", "-aG", "sambashare", user});
      }
      if (!Linux.isMemberOf(user, "video")) {
        //video4linux requires user to be a member of 'video' group
        JF.exec(new String[] {"usermod", "-aG", "video", user});
      }
      String jid = "j" + Math.abs(new Random().nextInt());
      String cmd[] = new String[] {"/usr/bin/sudo", "-E", "-u", user,
        domainLogon ? "/usr/sbin/jflogon-rundomain" : "/usr/sbin/jflogon-runsession",
        session};
      ProcessBuilder pb = new ProcessBuilder(cmd);
      //sudo doesn't pass environment variables unless -E is used
      //but leaving these next lines out causes problems
      Map<String,String> env = pb.environment();
      env.put("USER", user);
      env.put("LOGNAME", user);
      env.put("SHELL", shellPath);
      env.put("HOME", homePath);
      if (!wayland) {
        env.put("XAUTHORITY", homePath + "/.Xauthority");
      }
      env.put("JID", jid);
      if (env_names != null) {
        for(int a=0;a<env_names.length;a++) {
          env.put(env_names[a], env_values[a]);
        }
      }
      JFLog.log("Starting session:" + session + " for user " + user);
      Process p = pb.start();
      p.waitFor();
      JFLog.log("Session has terminated");
      JFLog.log("Killing all processes for user " + user);
      JF.exec(new String[] {"killall", "-u", user});  //ensure session ended
      JF.sleep(1500);  //wait for windows to close
      if (!globalConfig.disableSleep) {
        if (rebootFlag) {
          JFLog.log("Rebooting...");
          rebootFlag = false;
          Startup.reboot();
          return;
        }
        if (shutdownFlag) {
          JFLog.log("Shutting down...");
          shutdownFlag = false;
          Startup.shutdown("-P");
          return;
        }
      } else {
        JFLog.log("Power functions disabled by security policy.");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static String homePath, shellPath;

  public static void getUserDetails(String user) throws Exception {
    //find path from /etc/passwd
    //passwd = user_name:x:uid:gid:full_name:home_dir:shell
    FileInputStream fis = new FileInputStream("/etc/passwd");
    int len = fis.available();
    byte passwd[] = new byte[len];
    fis.read(passwd);
    String text = new String(passwd);
    String lns[] = text.split("\n");
    for(int ln=0;ln<lns.length;ln++) {
      String fs[] = lns[ln].split(":");
      if (!fs[0].equals(user)) continue;
      homePath = fs[5];
      shellPath = fs[6];
      fis.close();
      return;
    }
    fis.close();
    throw new Exception("user not found");
  }

  /** Reboots PC */
  public static void reboot() {
    try {
      stop();
      showPlymouth();
      JFLog.log("Rebooting...");
      JF.exec(new String[] {"reboot"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Shuts down PC
   * @param type "-P" = powerdown, "-H"=halt
   */
  public static void shutdown(String type) {
    try {
      stop();
      showPlymouth();
      JFLog.log("Shutting down...,type=" + type);
      JF.exec(new String[] {"shutdown " + type + " now"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void createLogon() {
    Linux.x11_rr_reset("800x600");
    //execute greeter
    try {
      Logon logon = new Logon();
      logon.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void hidePlymouth() {
    if (new File("/bin/plymouth").exists()) {
      try {
        JF.exec(new String[] {"/bin/plymouth","--quit"});
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  //this is interferring with reboot/shutdown
  private static void showPlymouth() {
    if (new File("/bin/plymouth").exists()) {
      try {
//        JF.exec(new String[] {"/bin/plymouth","--show-splash"});  //causes reboot to fail
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public void shellProcessOutput(String string) {
  }

  /** sudoers can NOT require tty or jflogon (and other apps that use sudo) fail. */
  public static void fixSudoers() {
    try {
      FileInputStream fis = new FileInputStream("/etc/sudoers");
      byte data[] = JF.readAll(fis);
      fis.close();
      String sudoers = new String(data);
      String lns[] = sudoers.split("\n");
      boolean patched = false;
      StringBuilder sb = new StringBuilder();
      for(int a=0;a<lns.length;a++) {
        if (lns[a].indexOf("requiretty") != -1 && !lns[a].startsWith("#")) {
          lns[a] = "#" + lns[a];
          patched = true;
        }
        sb.append(lns[a]);
        sb.append("\n");
      }
      if (!patched) return;
      FileOutputStream fos = new FileOutputStream("/etc/sudoers");
      fos.write(sb.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static class GlobalConfig {
    public boolean disableSleep;
  }
  private static GlobalConfig globalConfig = new GlobalConfig();
  private String globalConfigFolder = "/etc/jfconfig.d/";
  private String globalConfigFile = "global.xml";

  public void loadGlobalConfig() {
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(globalConfigFolder + "/" + globalConfigFile);
      xml.read(fis);
      xml.writeClass(globalConfig);
    } catch (FileNotFoundException fnfe) {
      defaultGlobalConfig();
    } catch (Exception e) {
      defaultGlobalConfig();
      JFLog.log(e);
    }
  }

  private void defaultGlobalConfig() {
    globalConfig.disableSleep = false;
  }

  private static String quote(String str) {
    return "\"" + str + "\"";
  }

  public static class JBusMethods {
    public void sleep() {
      if (globalConfig.disableSleep) return;
      try {
        JF.exec(new String[] {"systemctl", "suspend"});
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void reboot() {
      if (globalConfig.disableSleep) return;
      JFLog.log("Reboot requested on Session stop");
      rebootFlag = true;
    }
    public void shutdown() {
      if (globalConfig.disableSleep) return;
      JFLog.log("Shutdown requested on Session stop");
      shutdownFlag = true;
    }
    public void upgradesAvailable(int upgrades) {
      jbusClient.broadcast("org.jflinux.jfdesktop", "updatesAvailable", "" + upgrades);
    }
    public void mount(String dev) {
      JFLog.log("mount:" + dev);
      Startup.autoMounter.mount(dev);
    }
    public void umount(String path) {
      JFLog.log("umount:" + path);
      AutoMounter.Mount mount = Startup.autoMounter.getMount(path);
      if (mount == null) {
        JFLog.log("umount:" + path + ":Error:never mounted by AutoMounter");
        //try to fix this
        mount = new AutoMounter.Mount();
        mount.media = path;
      }
      Startup.autoMounter.umount(mount);
    }
    private String cleanName(String name) {
      //filter out bad chars in volume names
      StringBuilder sb = new StringBuilder();
      char in[] = name.toCharArray();
      for(int a=0;a<in.length;a++) {
        switch (in[a]) {
          case ':':
          case ';':
          case '*':
          case '?':
            break;
          default:
            sb.append(in[a]);
            break;
        }
      }
      return sb.toString();
    }
    public void renameDevice(String media, String newName) {
      newName = cleanName(newName);
      if (newName.length() == 0) return;
      //get device name
      AutoMounter.Mount mount = Startup.autoMounter.getMount("/media/" + media);
      if (mount == null) return;
      if (mount.fs.equals("iso9660")) return;
      //umount it
      Startup.autoMounter.umount(mount.dev);
      JF.sleep(500);  //just in case
      //change name
      String cmd[] = {mount.fs + "fslabel", mount.dev, newName};
      try {JF.exec(cmd);} catch (Exception e) {JFLog.log(e);}
      JF.sleep(500);  //this is needed
      //mount it back
      Startup.autoMounter.mount(mount.dev);
    }
    public void getStorageInfo(String pack, String dev) {
      AutoMounter.Mount tmp = new AutoMounter.Mount();
      String volName = Startup.autoMounter.getVolumeName(dev, tmp);
      if (volName == null) volName = "";
      if (tmp.fs == null) tmp.fs = "unknown";
      String mountPt = Startup.autoMounter.getMountPoint(dev);
      if (mountPt == null) mountPt = "";
      jbusClient.call(pack, "storageInfo", quote(dev) + "," + quote(volName) + "," + quote(tmp.fs) + ","
        + quote(mountPt));
    }
    public void stopAutoMounter() {
      AutoMounter.paused--;
    }
    public void startAutoMounter() {
      AutoMounter.paused++;
    }
    public void broadcastWAPList(String list) {
      jbusClient.broadcast("org.jflinux.jfdesktop.", "setWAPList", quote(list));
    }
    public void broadcastVideoChanged(String reason) {
      jbusClient.broadcast("org.jflinux.jfdesktop.", "videoChanged", quote(reason));
      jbusClient.broadcast("org.jflinux.jfconfig.", "videoChanged", quote(reason));
    }
  }
}

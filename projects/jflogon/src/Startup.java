/**
 * jfLinux Startup
 *  - includes org.jflinux.jsystemmgr
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
  private static ShellProcess x11process;
  private static boolean rebootFlag, shutdownFlag;

  public static JBusServer jbusServer;
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
      //start JBus
      jbusServer = new JBusServer();
      jbusServer.start();
      while (!JBusServer.ready) JF.sleep(50);
      //start jnetworkmgr
      new jnetworkmgr.Server().start();
      //start jsystemmgr
      jbusClient = new JBusClient("org.jflinux.jsystemmgr", new JBusMethods());
      jbusClient.start();
      //start automounter
      autoMounter = new AutoMounter();
      autoMounter.start();
      //start JF services
      startJFServices();
      //start device monitor
      new DeviceMonitor().start();
      //stop plymouth
      hidePlymouth();
      //start X
      create_server_xauth();
      boolean retry;
      do {
        retry = false;
        try {
          startx();
        } catch (Exception e) {
          JFLog.log(e);
          return;
        }
        JF.sleep(1500);  //wait for X to start
        Runtime.getRuntime().exec(new String[] {"numlockx", "on"});
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
          stopx();
          retry = true;
        } catch (Exception e) {
          JFLog.log(e);
          stopx();
          retry = true;
        }
      } while (retry);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static void startx() throws Exception {
    new Thread() {
      public void run() {
        x11process = new ShellProcess();
        x11process.keepOutput(false);
        x11process.addListener(new Startup());
        JFLog.log("Starting X Server...");
        x11process.run(new String[] {"/usr/bin/X"}, true);
        //some options lightdm uses
        // -core :0
        // -seat seat0
        // -nolisten tcp
        // vt7
        // -novtswitch
        // -auth /var/run/lightdm/root/:0
      }
    }.start();
  }

  public static void stopx() throws Exception {
    if (x11process != null) {
      JFLog.log("Stopping X Server...");
//      x11process.destroy();  //doesn't work
      int pid = x11process.getpid();
      JFLog.log("X11 pid=" + pid);
      Linux.kill(pid, Linux.SIGTERM);
      JF.sleep(500);
      for(int a=0;a<3;a++) {
        if (!x11process.isAlive()) break;
        JF.sleep(1000);
      }
      if (x11process.isAlive()) {
        Linux.kill(pid, Linux.SIGKILL);
        JF.sleep(500);
      }
      x11process = null;
      JFLog.log("X Server stopped...");
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
      Runtime.getRuntime().exec(new String[] {"chown", user+":"+user, fn});
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
      runSession(user, "/usr/bin/jdesktop", null, null, false);
      stopx();
      JF.sleep(1000);
      System.out.println("" + (char)0x1b + "[2J");  //clear screen
      System.out.println("\n\n\n\n\n\t\tPlease remove installation media and reboot\n\n\n\n\n");
//      shutdown("-H");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static ArrayList<String> services = new ArrayList<String>();

  public static void startJFServices() {
    //open all .conf files in /etc/jinit
    File etcjinit = new File("/etc/jinit");
    if (!etcjinit.exists()) return;
    File confs[] = etcjinit.listFiles();
    if (confs == null) return;
    for(int a=0;a<confs.length;a++) {
      String svc = confs[a].getName();
      if (!svc.endsWith(".conf")) continue;
      int idx1 = svc.lastIndexOf("/");
      int idx2 = svc.indexOf(".conf");
      svc = svc.substring(idx1+1, idx2);
      startJFService(svc);
    }
  }

  public static void startJFService(String svc) {
    String conf = "/etc/jinit/" + svc + ".conf";
    if (services.contains(svc)) return;  //already loaded
    try {
      Properties props = new Properties();
      FileInputStream fis = new FileInputStream(conf);
      props.load(fis);
      fis.close();
      String cp = props.getProperty("CLASSPATH");
      String cls = props.getProperty("CLASS");
      if ((cp == null) || (cls == null)) throw new Exception("Invalid conf:" + conf);
      Runtime.getRuntime().exec(new String[] {"java", "-cp", cp, cls});
      services.add(svc);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void stopJFService(String svc) {
    //this just removes it from the list
    for(int a=0;a<services.size();a++) {
      if (services.get(a).equals(svc)) {
        services.remove(a);
        return;
      }
    }
  }

  public static String serviceStatusAll() {
    StringBuilder sb = new StringBuilder();
    for(int a=0;a<services.size();a++) {
      sb.append("Service:" + services.get(a) + " running.|");
    }
    return sb.toString();
  }

  public static void runSession(String user, String session, String env_names[], String env_values[], boolean domainLogon) {
    try {
      getUserDetails(user);
      String xauthFile = homePath + "/.Xauthority";
      write_xauth(xauthFile);
      chown_xauth(xauthFile, user);
      if (!Linux.isMemberOf(user, "audio")) {
        //pulseaudio requires user to be member of 'audio' group
        Runtime.getRuntime().exec(new String[] {"usermod", "-aG", "audio", user});
      }
      if (!Linux.isMemberOf(user, "sambashare")) {
        //net usershare requires user to be member of 'sambashare' group
        Runtime.getRuntime().exec(new String[] {"usermod", "-aG", "sambashare", user});
      }
      if (!Linux.isMemberOf(user, "video")) {
        //video4linux requires user to be a member of 'video' group
        Runtime.getRuntime().exec(new String[] {"usermod", "-aG", "video", user});
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
      env.put("XAUTHORITY", homePath + "/.Xauthority");
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
      Runtime.getRuntime().exec(new String[] {"killall", "-u", user});  //ensure session ended
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
      stopx();
      showPlymouth();
      JFLog.log("Rebooting...");
      Runtime.getRuntime().exec("reboot");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Shuts down PC
   * @param type "-P" = powerdown, "-H"=halt
   */
  public static void shutdown(String type) {
    try {
      stopx();
      showPlymouth();
      JFLog.log("Shutting down...,type=" + type);
      Runtime.getRuntime().exec("shutdown " + type + " now");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void createLogon() {
    Linux.x11_rr_reset("800x600");
    //execute greeter
    try {
      Runtime.getRuntime().exec("jflogon");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void hidePlymouth() {
    if (new File("/bin/plymouth").exists()) {
      try {
        Runtime.getRuntime().exec(new String[] {"/bin/plymouth","--quit"});
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  //this is interferring with reboot/shutdown
  private static void showPlymouth() {
    if (new File("/bin/plymouth").exists()) {
      try {
//        Runtime.getRuntime().exec(new String[] {"/bin/plymouth","--show-splash"});  //causes reboot to fail
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
  private String globalConfigFolder = "/etc/jconfig.d/";
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
        Runtime.getRuntime().exec("pm-suspend");
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
      Startup.jbusServer.broadcast("org.jflinux.jdesktop", "updatesAvailable", "" + upgrades);
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
      try {Runtime.getRuntime().exec(cmd);} catch (Exception e) {JFLog.log(e);}
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
    public void startService(String svc) {
      Startup.startJFService(svc);
    }
    public void stopService(String svc) {
      Startup.stopJFService(svc);
      jbusClient.call("org.jflinux.service." + svc, "stop", "");
    }
    public void serviceStatusAll(String pack) {
      String ret = Startup.serviceStatusAll();
      jbusClient.call(pack, "serviceStatusAll", quote(ret));
    }
    public void stopAutoMounter() {
      AutoMounter.paused--;
    }
    public void startAutoMounter() {
      AutoMounter.paused++;
    }
    public void broadcastWAPList(String list) {
      Startup.jbusServer.broadcast("org.jflinux.jdesktop.", "setWAPList", quote(list));
    }
    public void broadcastVideoChanged(String reason) {
      Startup.jbusServer.broadcast("org.jflinux.jdesktop.", "videoChanged", quote(reason));
      Startup.jbusServer.broadcast("org.jflinux.jconfig.", "videoChanged", quote(reason));
    }
  }
}

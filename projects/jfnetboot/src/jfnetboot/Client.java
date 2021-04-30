package jfnetboot;

/** Client device.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Client {
  public String serial;  //mac address
  public String filesystem;  //file system name
  public String arch;  //arm or x86
  public String cmd;  //command name to execute on startup (see Commands) (inserted into /command.sh)
  public String opts;  //kernel options (ie: "quiet") (default = "")
  public String hostname;  //hostname (default = serial)
  public String videomode;  //force video mode (raspberry pi only)
  public String touchscreen;  //touch screen calibration (inserted into command.sh)

  private FileSystem fs;  //file system instance
  private String ip;

  public Client(String serial, String arch) {
    this.serial = serial;
    this.arch = arch;
    filesystem = Settings.current.defaultFileSystem;
    cmd = "";
    opts = "";
    hostname = getSerial();
    videomode = "";
    touchscreen = "";
  }

  public static String getConfigFile(String serial, String arch) {
    return Paths.clients + "/" + getSerial(serial) + "-" + arch + ".cfg";
  }

  private static String getProperty(Properties props, String key, String def) {
    String value = props.getProperty(key);
    if (value == null) return def;
    return value;
  }

  public static Client load(String serial, String arch) {
    try {
      FileInputStream fis = new FileInputStream(getConfigFile(serial, arch));
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      Client client = new Client(serial, arch);
      client.filesystem = getProperty(props, "file-system", Settings.current.defaultFileSystem);
      client.arch = getProperty(props, "arch", arch);
      client.cmd = getProperty(props, "cmd", "");
      client.opts = getProperty(props, "kernel-opts", "");
      client.hostname = getProperty(props, "hostname", client.getSerial());
      client.videomode = getProperty(props, "videomode", "");
      client.touchscreen = getProperty(props, "touch-screen", "");
      return client;
    } catch (Exception e) {
      return null;
    }
  }

  public boolean save() {
    if (arch == null) {
      JFLog.log("Client.save() arch == null");
      return false;
    }
    try {
      Properties props = new Properties();
      props.setProperty("arch", arch);
      props.setProperty("file-system", filesystem);
      props.setProperty("cmd", cmd);
      props.setProperty("kernel-opts", opts);
      props.setProperty("hostname", hostname);
      props.setProperty("videomode", videomode);
      props.setProperty("touch-screen", touchscreen);
      FileOutputStream fos = new FileOutputStream(getConfigFile(serial, arch));
      props.store(fos, "Client Settings");
      fos.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void delete() {
    new File(getConfigFile(serial, arch)).delete();
    Clients.remove(this);
    fs.delete();
  }

  public void purge() {
    fs.purge();
  }

  public void clone(String newName, Runnable notify) {
    fs.clone(newName, notify);
  }

  public boolean mount() {
    FileSystem lower = FileSystems.get(filesystem, arch);
    if (lower == null) {
      JFLog.log("Client:Error:Lower FileSystem not found:" + filesystem + "-" + arch);
      return false;
    }
    fs = new FileSystem(getSerial(), arch, this);
    if (!new File(fs.getRootPath() + "/etc/passwd").exists()) {
      new File(fs.local + "/upper").mkdir();
      new File(fs.local + "/work").mkdir();
      // mount -t overlay overlay -o lowerdir=/lower,upperdir=/upper,workdir=/work /merged
      JF.exec(new String[] {"mount", "-t", "overlay", "overlay", "-o", "lowerdir=" + lower.getRootPath() + ",upperdir=" + fs.local + "/upper" + ",workdir=" + fs.local + "/work",
        "-o", "nfs_export=on",
        "-o", "index=on",
        fs.getRootPath()});
      if (!new File(fs.getRootPath() + "/etc/passwd").exists()) {
        JFLog.log("Client:Failed to mount overlay filesystem");
        return false;
      }
    }
    replaceFiles();  //must replace files before index() but after mount()
    if (Service.nfs_server) {
      JF.exec(new String[] {"exportfs", "-o", "rw,sync,no_root_squash,insecure", "*:" + fs.getRootPath()});
    } else {
      fs.index();
    }
    return true;
  }

  public boolean umount() {
    fs.closeAllFiles();
    int max = 10;
    while (new File(fs.getRootPath() + "/etc/passwd").exists()) {
      JF.sleep(500);
      JF.exec(new String[] {"umount", fs.getRootPath()});
      JF.sleep(500);
      max--;
      if (max == 0) {
        JFLog.log("Error:Client.umount() failed!");
        break;
      }
    }
    if (Service.nfs_server) {
      JF.exec(new String[] {"exportfs", "-u", "*:" + fs.getRootPath()});
    }
    fs = null;
    return true;
  }

  /** Returns default message displayed on screen. */
  private String getHTML() {
    StringBuilder sb = new StringBuilder();
    sb.append("<center>");
    sb.append("<h1>jfNetBoot Thin Client</h1>");
    sb.append("<br>");
    sb.append("<h1>Serial:" + getSerial() + "</h1>");
    sb.append("<br>");
    sb.append("<h2>This client is not configured yet!</h2>");
    sb.append("<h2>Please log into jfNetBoot console, configure and then reboot.</h2>");
    sb.append("</center>");
    return sb.toString();
  }

  private void mkdirs(String path) {
    new File(fs.getRootPath() + path).mkdirs();
  }

  private void replaceFile(String path, String content) {
    try {
      FileOutputStream fos = new FileOutputStream(fs.getRootPath() + path);
      fos.write(content.getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void deleteFileRecursive(String regex, NFolder pfolder) {
    for(int idx = 0;idx < pfolder.cfiles.size();) {
      NFile cfile = pfolder.cfiles.get(idx);
      if (cfile.name.matches(regex)) {
        fs.remove(pfolder.handle, cfile.name);
      } else {
        idx++;
      }
    }
    for(NFolder cfolder : pfolder.cfolders) {
      deleteFileRecursive(regex, cfolder);
    }
  }

  private void deleteFileRecursive(String regex) {
    deleteFileRecursive(regex, fs.getRootFolder());
  }

  private void patchFile(String path, String regex, String replace) {
    String local = fs.getRootPath() + path;
    try {
      FileInputStream fis = new FileInputStream(local);
      byte[] data = fis.readAllBytes();
      fis.close();
      data = new String(data).replaceAll(regex, replace).getBytes();
      FileOutputStream fos = new FileOutputStream(local);
      fos.write(data);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void chmod(String mode, String path) {
    JF.exec(new String[] {"chmod", mode, getFileSystem().getRootPath() + path});
  }

  private void replaceFiles() {
    JFLog.log("Client.FileSystem.local=" + fs.getRootPath());
    //create replacement files before indexing
    mkdirs("/etc");
    if (arch.equals("arm")) {
      mkdirs("/boot/firmware");
    }
    mkdirs("/netboot");
    //replace /etc/fstab
    replaceFile("/etc/fstab", getfstab());
    //replace /etc/hostname
    replaceFile("/etc/hostname", hostname + "\n");
    if (arch.equals("arm")) {
      replaceFile("/boot/firmware/config.txt", getFirmwareConfig());
    }
    //SERIAL environment variable for scripts (might not need this)
    replaceFile("/etc/environment", "SERIAL=" + getSerial() + "\n");
    //create netboot files
    replaceFile("/netboot/command.sh", getCommand());
    replaceFile("/netboot/default.html", getHTML());
    replaceFile("/netboot/config.sh", getConfigScript());
    replaceFile("/netboot/autostart", getAutostartScript());
    chmod("+x", "/netboot/*.sh");
  }

  private String getCommand() {
    StringBuilder sb = new StringBuilder();
    if (touchscreen.length() > 0) {
      int x = -1;
      int y = -1;
      String[] ps = touchscreen.split(",");
      for(String p : ps) {
        int idx = p.indexOf('=');
        if (idx == -1) continue;
        String key = p.substring(0, idx);
        String value = p.substring(idx + 1);
        switch (key) {
          case "x": x = Integer.valueOf(value); break;
          case "y": y = Integer.valueOf(value); break;
        }
      }
      if (x != -1 && y != -1) {
        sb.append("for i in {4..15}\n");
        sb.append("do\n");
        //matrix = [hscale] [vskew] [hoffset] [hskew] [vscale] [voffset] 0 0 1
        //matrix = 1.x 0.0 -0.x/2 0.0 1.y -0.y/2 0.0 0.0 1.0
        int x_2 = x / 2;
        int y_2 = y / 2;
        sb.append("  xinput set-prop $i \"libinput Calibration Matrix\" 1." + x +" 0.0 -0." + x_2 + " 0.0 1." + y + " -0." + y_2 + " 0.0 0.0 1.0\n");
        sb.append("done\n");
      }
    }
    sb.append(Commands.getCmd(cmd));
    sb.append("\n");
    return sb.toString();
  }

  public void reinitCommand() {
    replaceFile("/netboot/command.sh", getCommand());
  }

  private String getConfigScript() {
    StringBuilder sb = new StringBuilder();
    sb.append(
      "#!/bin/bash\n" +
      "if [ -f /usr/bin/apt ]; then\n" +
      "  apt update\n" +
      "  apt install --no-install-recommends xorg xinput openbox lightdm chromium chromium-sandbox pulseaudio openssh-server sudo\n" +
      "fi\n" +
      "if [ -f /usr/bin/dnf ]; then\n" +
      "  dnf install --setopt=install_weak_deps=False xorg-x11-server-Xorg xinput openbox lightdm chromium pulseaudio openssh-server sudo\n" +
      "fi\n" +
      "if [ -f /usr/bin/pacman ]; then\n" +
      "  pacman -S xorg-server xorg-xinput openbox lightdm chromium pulseaudio openssh sudo\n" +
      "fi\n" +
      "cp autostart /etc/xdg/openbox\n" +
      "#change OSKey + E = open terminal\n" +
      "sed -i -- 's/kfmclient openProfile filemanagement/x-terminal-emulator/g' /etc/xdg/openbox/rc.xml\n" +
      "#enable auto login : username=user\n" +
      "sed -i -- 's/#autologin-user=/autologin-user=user/g' /etc/lightdm/lightdm.conf\n" +
      "#enable auto login : timeout = 0\n" +
      "sed -i -- 's/#autologin-user-timeout=/autologin-user-timeout=0/g' /etc/lightdm/lightdm.conf\n" +
      "mkdir ~/.ssh\n" +
      "ssh-keygen -t dsa\n" +
      "cp ~/.ssh/id_dsa.pub ~/.ssh/authorized_keys2\n" +
      "echo PubkeyAcceptedKeyTypes +ssh-dss* >> /etc/ssh/sshd_config\n" +
      "echo PermitRootLogin prohibit-password >> /etc/ssh/sshd_config\n" +
      "systemctl enable ssh\n" +
      "systemctl start ssh\n" +
      "echo Follow prompts to create default user, use any password you like:\n" +
      "adduser user\n" +
      "echo Thin Client setup complete!\n"
    );
    return sb.toString();
  }

  private String getAutostartScript() {
    StringBuilder sb = new StringBuilder();
    sb.append(
      "#OpenBox autostart /etc/xdg/openbox/autostart\n" +
      "\n" +
      "#disable sleep mode\n" +
      "xset -dpms\n" +
      "xset s off\n" +
      "\n" +
      "#turn numlock on\n" +
      "setleds -D +num\n" +
      "\n" +
      "#delete preferences in case of a crash\n" +
      "rm ~/.config/chromium/Default/Preferences\n" +
      "\n" +
      "#remap keypad keys to numbers if numlock is off\n" +
      "xmodmap -e 'keycode 87 = 1'\n" +
      "xmodmap -e 'keycode 88 = 2'\n" +
      "xmodmap -e 'keycode 89 = 3'\n" +
      "xmodmap -e 'keycode 83 = 4'\n" +
      "xmodmap -e 'keycode 84 = 5'\n" +
      "xmodmap -e 'keycode 85 = 6'\n" +
      "xmodmap -e 'keycode 79 = 7'\n" +
      "xmodmap -e 'keycode 80 = 8'\n" +
      "xmodmap -e 'keycode 81 = 9'\n" +
      "xmodmap -e 'keycode 90 = 0'\n" +
      "\n" +
      "#if dhcp takes a long time increase this sleep\n" +
      "sleep 7\n" +
      "\n" +
      "#execute command configured for this client\n" +
      ". /netboot/command.sh\n"
    );
    return sb.toString();
  }

  public String getFirmwareConfig() {
    StringBuilder sb = new StringBuilder();
    String[] ps = videomode.split("[,]");
    for(String p : ps) {
      sb.append(p);
      sb.append("\n");
    }
    return sb.toString();
  }

  public void setIP(String ip) {
    this.ip = ip;
  }

  public FileSystem getFileSystem() {
    if (fs == null) {
      JFLog.log("Error:client not init?");
    }
    return fs;
  }

  private String getfstab() {
    switch (arch) {
      case "arm": {
        StringBuilder sb = new StringBuilder();
        sb.append("#jfNetBoot : DO NOT EDIT\n");
        sb.append("/dev/nfs   /          nfs    tcp,nolock  0   0\n");
        sb.append("proc       /proc      proc   defaults    0   0\n");
        sb.append("sysfs      /sys       sysfs  defaults    0   0\n");
        sb.append("tmpfs      /tmp       tmpfs  defaults    0   0\n");
        sb.append("tmpfs      /var/log   tmpfs  defaults    0   0\n");
        sb.append("tmpfs      /var/tmp   tmpfs  defaults    0   0\n");
        return sb.toString();
      }
      case "x86": {
        StringBuilder sb = new StringBuilder();
        sb.append("#jfNetBoot : DO NOT EDIT\n");
        sb.append("/dev/nfs   /          nfs    tcp,nolock  0   0\n");
        sb.append("proc       /proc      proc   defaults    0   0\n");
        sb.append("sysfs      /sys       sysfs  defaults    0   0\n");
        sb.append("tmpfs      /tmp       tmpfs  defaults    0   0\n");
        sb.append("tmpfs      /var/log   tmpfs  defaults    0   0\n");
        sb.append("tmpfs      /var/tmp   tmpfs  defaults    0   0\n");
        return sb.toString();
      }
    }
    return null;
  }

  public String getSerial() {
    return serial;
  }

  public static String getSerial(String serial) {
    return serial;
  }

  public String getHostname() {
    return hostname;
  }

  public void shutdown() {
    //ssh -i id_dsa root@$HOST "shutdown -h now"
    new Thread() {
      public void run() {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("ssh");
        cmd.add("-i");
        cmd.add(fs.getRootPath() + "/root/.ssh/id_dsa");
        cmd.add("root@" + ip);
        cmd.add("\"shutdown -h now\"");
        try {
          Runtime.getRuntime().exec(cmd.toArray(new String[0]));
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }.start();
  }

  public void reboot() {
    //ssh -i id_dsa root@$HOST "reboot"
    new Thread() {
      public void run() {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("ssh");
        cmd.add("-i");
        cmd.add(fs.getRootPath() + "/root/.ssh/id_dsa");
        cmd.add("root@" + ip);
        cmd.add("\"reboot\"");
        try {
          Runtime.getRuntime().exec(cmd.toArray(new String[0]));
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }.start();
  }
}

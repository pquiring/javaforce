/**
 * Created : Mar 30, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.utils.*;

/** Monitors /dev for new devices and mounts them (such as USB drives). */

public class AutoMounter extends Thread implements monitordir.Listener {
  public boolean active = true;
  public static int paused = 0;
  private int monitor;

  public static class Mount {
    public String dev, media, fs;
  }

  private Vector<Mount> mounts = new Vector<Mount>();
  private Object lock = new Object();

  public void run() {
    cleanMedia();
    bootMount();
    monitor = monitordir.add("/dev");
    monitordir.setListener(monitor, this);
  }

  //mounts any partitions that are not already mounted
  private void bootMount() {
    File dev = new File("/dev");
    File devs[] = dev.listFiles();
    for(int a=0;a<devs.length;a++) {
      String name = devs[a].getAbsolutePath();
      if (name.startsWith("/dev/sd")) mountIf(name);
      if (name.startsWith("/dev/sr")) mountIf(name);
    }
  }

  //mount if not already mounted
  private void mountIf(String dev) {
    if (getMountPoint(dev) != null) return;
    mount(dev);
  }

  public Mount getMount(String mediaMointPoint) {
    synchronized(lock) {
      for(int a=0;a<mounts.size();a++) {
        if (mounts.get(a).media.equals(mediaMointPoint)) {
          return mounts.get(a);
        }
      }
    }
    return null;
  }

  public String getMountPoint(String dev) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"mount"}, true);
    String lns[] = output.split("\n");
    for(int a=0;a<lns.length;a++) {
      String f[] = lns[a].split(" ");
      //dev on path ...
      if (!f[0].equals(dev)) continue;
      return f[2];
    }
    return null;
  }

  public synchronized void mount(String dev) {
    JFLog.log("AutoMount:Attempting to mount:" + dev);
    if (paused < 0) return;
    //mount in /media
    if (!dev.startsWith("/dev/sd") && !dev.startsWith("/dev/sr")) {
      //not a mountable volume (Storage Device/ROM)
      JFLog.log("AutoMount:Not a mountable device:" + dev);
      return;
    }
    if (dev.startsWith("/dev/sd")) {
      char lastChar = dev.charAt(dev.length()-1);
      if (!((lastChar >= '0') && (lastChar <= '9'))) {
        //not a partition
        JFLog.log("AutoMount:Not a mountable partition:" + dev);
        return;
      }
    }
    Mount mount = new Mount();
    String volName = getVolumeName(dev, mount);
    JFLog.log("AutoMount:Volume Label:" + dev + "=" + volName);
    if (volName == null) {
      JFLog.log("AutoMount:No volume label found:" + dev);
      return;
    }
    volName = volName.replaceAll(" ", "");
    if (volName.length() == 0) volName = "Unknown";
    String volPath = "/media/" + volName;
    String fullPath = volPath;
    File file = new File(fullPath);
    int cnt = 1;
    while (file.exists()) {
      fullPath = volPath + "(" + cnt++ + ")";
      file = new File(fullPath);
    }
    file.mkdir();
    mount.dev = dev;
    mount.media = fullPath;
    if (dev.startsWith("/dev/sr")) {
      //do not automount cdda - user can use gvfs (gnome-disk-image-mounter)
      return;
    }
    String cmd[] = {"mount", dev, fullPath, "-o", "user"};
    ShellProcess sp = new ShellProcess();
    String output = sp.run(cmd, true);
    if (sp.getErrorLevel() != 0) {
      file.delete();
      JFLog.log("AutoMount:Failed to mount:" + dev + ":error=" + output);
    } else {
      JFLog.log("AutoMount:Success:" + dev + " mounted to " + fullPath);
      synchronized(lock) {
        mounts.add(mount);
      }
      //broadcast /media change
      Startup.jbusClient.broadcast("org.jflinux.jffile", "rescanMedia", "");
    }
  }

  public void umount(String dev) {
    if (paused < 0) return;
    //Normally user umount's from jffile - this should just clean up the /media folder
    boolean ok = false;
    Mount mount = null;
    synchronized(lock) {
      for(int a=0;a<mounts.size();a++) {
        mount = mounts.get(a);
        if (mount.dev.equals(dev)) {
          mounts.remove(a);
          ok = true;
          break;
        }
      }
    }
    if (ok) {
      umount(mount);
    } else {
      JFLog.log("AutoMount:umount unknown device:" + dev);
    }
  }

  public void umount(Mount mount) {
    if (mount.dev != null) {
      String cmd[] = {"umount", mount.dev};
      try {JF.exec(cmd);} catch (Exception e) {JFLog.log(e);}
    }
    JF.sleep(500);  //give kernel some time
    File folder = new File(mount.media);
    folder.delete();
    //broadcast /media change
    Startup.jbusClient.broadcast("org.jflinux.jffile", "rescanMedia", "");
  }

  public String getVolumeName(String dev, Mount mount) {
    //TODO : is there a better way to do this?
    if (dev.startsWith("/dev/sr")) {
      //CDROM - dosfslabel will not work
      mount.fs = "iso9660";
      return "CDROM" + dev.substring(7);
    }
    ShellProcess sp = new ShellProcess();
    //try dosfslabel
    String output = sp.run(new String[] {"dosfslabel", dev}, false);
    if (sp.getErrorLevel() == 0) {
      mount.fs = "dos";
      return output.replaceAll("\n", "");
    }
    //try ntfslabel
    output = sp.run(new String[] {"ntfslabel", dev}, false);
    if (sp.getErrorLevel() == 0) {
      mount.fs = "nt";
      return output.replaceAll("\n", "");
    }
    //try e2label
    output = sp.run(new String[] {"e2label", dev}, false);
    if (sp.getErrorLevel() == 0) {
      mount.fs = "e2";
      return output.replaceAll("\n", "");
    }
    //unknown filesystem
    return null;
  }

  public void folderChangeEvent(String event, String file) {
    if (event.equals("CREATED")) {
      mount("/dev/" + file);
    }
    if (event.equals("DELETED")) {
      //BUG : this doesn't make any sense - you should umount before it's deleted?
      umount("/dev/" + file);
    }
  }

  private void cleanMedia() {
    try {
      File files[] = new File("/media").listFiles();
      if (files == null || files.length == 0) return;
      for(int a=0;a<files.length;a++) {
        if (files[a].isDirectory()) {
          files[a].delete();
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

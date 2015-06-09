package jfparted;

import java.util.*;

import javaforce.*;

/** Contains all static data and partition related functions
 *
 *  NOTE : This code is shared with jinstall
 *
 *  "Humor, I Love it, weeeeee" - Data
 *
 */

public class Data {
  public static enum installTypes { LINUX, ALL, CUSTOM };
  public static installTypes installType = installTypes.LINUX;
  public static ArrayList<Device> devices;
  public static Device guidedTarget;
  public static ArrayList<String> ops1, ops2;  //guided parted operations
  public static Partition swap, root;
  public static ArrayList<String> fstab; //filesys table /etc/fstab
  public static String fullName, loginName, passwd, localhost, localdomain, timezone;

  public static class Device implements Cloneable {
    //parted fields
    public String dev;
    public String size;
    public String model;
    public boolean uninit;
    public ArrayList<Partition> parts = new ArrayList<Partition>();
    public Device clone() {
      Device clone = new Device();
      clone.dev = dev;
      clone.size = size;
      clone.model = model;
      clone.uninit = uninit;
      clone.parts = new ArrayList<Partition>();
      for(int a=0;a<parts.size();a++) {
        clone.parts.add(parts.get(a).clone());
        clone.parts.get(a).device = clone;  //make sure to update owner
      }
      return clone;
    }
  }
  public static class Partition implements Cloneable {
    //parted fields
    public Device device;  //owner
    public int number;
    public String start, end;
    public String size;
    public String type;
    public String filesys;
    public String label;  //used to format() only
    public String flags;
    //extra fields
    public String mount;
    public boolean delete;
    public Partition clone() {
      Partition n = new Partition();
      n.device = device;
      n.number = number;
      n.start = start;
      n.end = end;
      n.size = size;
      n.type = type;
      n.filesys = filesys;
      n.flags = flags;
      n.mount = mount;
      n.delete = delete;
      return n;
    }
  }

  private static void clearDevices() {
    devices = new ArrayList<Device>();
  }

  private static void addDevice(String dev, String size, boolean uninit) {
    Device device = new Device();
    device.dev = dev;
    device.size = size;
    device.uninit = uninit;
    devices.add(device);
  }

  public static void getDevices() {
    JFLog.log("getDevices()");
    clearDevices();
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("parted");
    sp.addResponse("(parted) ", "print devices\n", false);
    sp.addResponse("(parted) ", "quit\n", false);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec parted"); return;}
    String lns[] = output.split("\n");
    int i1,i2,i3;
    String str;
    for(int idx=0;idx < lns.length;idx++) {
      str = lns[idx];
      if (str.indexOf("Error") != -1 && str.indexOf("unrecognised disk label") != -1) {
        //str=Error: /dev/sda: unrecognised disk label
        JFLog.log("Warning:Device has no partition table!");
        i1 = str.indexOf("/dev");
        i2 = str.substring(i1).indexOf(":");
        addDevice(str.substring(i1, i1+i2), "unknown", true);
        continue;
      }
      if (str.startsWith("/dev/fd")) continue;  //floppies
      if (str.startsWith("/dev/sr")) continue;  //CD-ROMs
      if (str.startsWith("/dev/zram")) continue;  //RAM drives
      if (str.startsWith("/dev/mapper")) continue;  //mapper drives
      if (str.startsWith("/dev")) {
        //str=/dev/sda (300GB)
        i1 = str.indexOf(" ");
        i2 = str.indexOf("(");
        i3 = str.indexOf(")");
        addDevice(str.substring(0, i1), str.substring(i2+1,i3-1) + "B", false);
      }
    }
  }

  private static void getPartitions(Device device) {
    JFLog.log("getPartitions() for " + device.dev);
    device.parts.clear();
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("parted");
    sp.addResponse("(parted) ", "select " + device.dev + "\n", false);
    sp.addResponse("(parted) ", "print free\n", false);
    sp.addResponse("(parted) ", "quit\n", false);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec parted"); return;}
    String lns[] = output.split("\n");
    int idx = 0, iModel, iNumber, iStart, iEnd, iSize, iType, iFileSys, iFlags;
    String str;
    String number;
    while (idx < lns.length) {
      str = lns[idx];
      if (str.indexOf("Error") != -1 && str.indexOf("unrecognised disk label") != -1) {
        //parted/3.x started placing this warning here instead of 'print devices' (which is better)
        JFLog.log("Warning:Device has no partition table!");
        device.uninit = true;
        return;
      }
      if (str.startsWith("Model")) {
        iModel = str.lastIndexOf(" ");
//        i2 = str.indexOf("(");  //(scsi)
//        i3 = str.indexOf(")");
        device.model = str.substring(7, iModel);
        while (lns[idx].length() > 0) idx++;  //skip Model, Disk, Sector size, Partition Table, [Disk Flags]
        idx++;  //skip blank line
        String cols = lns[idx++];  //Number Start End Size Type File system Flags
        iNumber = cols.indexOf("Number");
        iStart = cols.indexOf("Start");
        iEnd = cols.indexOf("End");
        iSize = cols.indexOf("Size");
        iType = cols.indexOf("Type");
        iFileSys = cols.indexOf("File system");
        if (iType == -1) iType = iFileSys;
        iFlags = cols.indexOf("Flags");
        while (lns[idx].length() > 0) {
          str = lns[idx] + " ";
          int len = str.length();
          Partition part = new Partition();
          part.device = device;
          device.parts.add(part);
          number = str.substring(Math.min(iNumber+1, len-1),Math.min(8, len)).trim();
          if (number.length() == 0) part.number = -1; else part.number = Integer.valueOf(number);
          part.start = str.substring(Math.min(iStart, len-1),Math.min(iEnd, len)).trim();
          part.end = str.substring(Math.min(iEnd, len-1),Math.min(iSize, len)).trim();
          part.size = str.substring(Math.min(iSize, len-1),Math.min(iType, len)).trim();
          if (iType == iFileSys)
            part.type = "";
          else
            part.type = str.substring(Math.min(iType, len-1),Math.min(iFileSys, len)).trim();
          part.filesys = str.substring(Math.min(iFileSys, len-1),Math.min(iFlags, len)).trim();
          if (part.filesys.startsWith("linux-swap")) part.filesys = "swap";
          part.flags = str.substring(Math.min(iFlags, len-1)).trim();
          part.mount = "";
          idx++;
        }
      } else {
        idx++;
      }
    }
  }

  public static void getPartitions() {
    for(int a=0;a<devices.size();a++) {
      getPartitions(devices.get(a));
    }
  }

  public static int getDeviceCount() {
    return devices.size();
  }

  public static Device getDevice(String dev) {
    for(int a=0;a<devices.size();a++) {
      if (devices.get(a).dev.equals(dev)) return devices.get(a);
    }
    return null;
  }

  public static double getSize(String size) {
    // ##.# k/M/G/T B
    if (size == null) return -1.0;
    int len = size.length();
    String number = size.substring(0, len-2).trim();
    char exp = size.charAt(len-2);
    switch (exp) {
      case 'k':
      case 'K':
        return Double.valueOf(number) * 1024.0;
      case 'm':
      case 'M':
        return Double.valueOf(number) * 1024.0 * 1024.0;
      case 'g':
      case 'G':
        return Double.valueOf(number) * 1024.0 * 1024.0 * 1024.0;
      case 't':
      case 'T':
        return Double.valueOf(number) * 1024.0 * 1024.0 * 1024.0 * 1024.0;
    }
    return 0.0;
  }

  public static String makeSize(double size) {
    if (size < 1024.0 * 1024.0) {
      return String.format("%4.1fkB", size / 1024.0).trim();
    }
    if (size < 1024.0 * 1024.0 * 1024.0) {
      return String.format("%4.1fMB", size / (1024.0 * 1024.0)).trim();
    }
    if (size < 1024.0 * 1024.0 * 1024.0 * 1024.0) {
      return String.format("%4.1fGB", size / (1024.0 * 1024.0 * 1024.0)).trim();
    }
    return String.format("%4.1fTB", size / (1024.0 * 1024.0 * 1024.0 * 1024.0)).trim();
  }

  public static boolean isLogical(Partition part) {
    //determines if a block is logical (including Free Space)
    if (part.type.equals("primary")) return false;
    if (part.type.equals("extended")) return false;
    if (part.type.equals("logical")) return true;
    //check if Free Space is within extended part
    Partition ext = null;
    for(int a=0;a<part.device.parts.size();a++) {
      if (part.device.parts.get(a).type.equals("extended")) {ext = part.device.parts.get(a); break;}
    }
    if (ext == null) return false;  //no extended partition found
    if (getSize(part.start) >= getSize(ext.end)) return false;
    if (getSize(part.end) <= getSize(ext.start)) return false;
    return true;
  }

  /** Deletes partitions in list that are marked for deletion (virtually). */
  public static void deletePartitions(Device device) {
    int idx = 0;
    Partition prev, next;
    logDevice(device, "deletePartitions() start (virtually)");
    while (idx < device.parts.size()) {
      Partition part = device.parts.get(idx);
      if (!part.delete) {idx++; continue;}
      JFLog.log("Deleting part:" + part.device.dev + part.number);
      if (part.type.equals("extended")) {
        JFLog.log("Deleting logical parts of extended part");
        //delete all logical partitions (which will always follow)
        int extidx = idx+1;
        while (extidx < device.parts.size()) {
          if (isLogical(device.parts.get(extidx))) {
            device.parts.remove(extidx);
          } else {
            extidx++;
          }
        }
      }
      boolean partLogical = isLogical(part);
      boolean merged = false;
      if (idx > 0) {
        prev = device.parts.get(idx-1);
        boolean prevLogical = isLogical(prev);
        if (prev.filesys.equals("Free Space") && (partLogical == prevLogical)) {
          //join with previous block
          JFLog.log("Merging part with prev:" + prev.device.dev + prev.number);
          prev.end = part.end;
          JFLog.log("prev.size=" + prev.size + ",part.size=" + part.size);
          prev.size = makeSize(getSize(prev.size) + getSize(part.size));
          JFLog.log("merged.size=" + prev.size);
          device.parts.remove(idx);
          merged = true;
          part = prev;
          partLogical = prevLogical;
          idx--;
        }
      }
      if (idx+1 < device.parts.size()) {
        next = device.parts.get(idx+1);
        boolean nextLogical = isLogical(next);
        if (next.filesys.equals("Free Space") && (partLogical == nextLogical)) {
          //join with next block
          JFLog.log("Merging part with next:" + next.device.dev + next.number);
          next.start = part.start;
          JFLog.log("next.size=" + next.size + ",part.size=" + part.size);
          next.size = makeSize(getSize(next.size) + getSize(part.size));
          JFLog.log("merged.size=" + next.size);
          device.parts.remove(idx);
          merged = true;
        }
      }
      if (!merged) {
        JFLog.log("Setting part as free");
        part.number = -1;
        part.type = "";
        part.filesys = "Free Space";
        idx++;
      }
    }
    logDevice(device, "deletePartitions() done");
  }

  /** Find a place to create a new primary partition. */
  public static Partition findPartition(Device device, String reqsizeStr, String filesys) {
    double reqsize = getSize(reqsizeStr);  //NOTE : if sizeStr == null reqsize = -1.0
    logDevice(device, "findPartition() start");
    //check if there are already 4 pri/ext partitions defined
    int pecnt = 0;
    for(int idx=0;idx<device.parts.size();idx++) {
      if (device.parts.get(idx).type.equals("primary")) pecnt++;
      if (device.parts.get(idx).type.equals("extended")) pecnt++;
    }
    if (pecnt >= 4) {
      JFLog.log("findPartition() : Device already has 4 primary partitions.");
      return null;
    }  //NOTE:should NEVER be > 4
    int bestidx = -1;
    double excess = -1.0;
    double bestsize = 0.0;
    double partsize;
    Partition part;
    for(int idx=0;idx<device.parts.size();idx++) {
      part = device.parts.get(idx);
      JFLog.log("part:" + part.device.dev + part.number + "," + part.filesys);
      if (part.device != device) JFLog.log("Warning:device has changed in partition");
      if (!part.filesys.equals("Free Space")) {JFLog.log("not free"); continue;}
      if (isLogical(part)) {JFLog.log("logical"); continue;}
      JFLog.log("checking:" + idx);
      partsize = getSize(part.size);
      if (reqsize != -1.0) {
        if (partsize < reqsize) { JFLog.log("too small"); continue; }  //too small
        if (excess == -1.0) {
          //found a match
          excess = partsize - reqsize;
          bestidx = idx;
          continue;
        }
        if (partsize - reqsize < excess) {
          //found a better match
          excess = partsize - reqsize;
          bestidx = idx;
          continue;
        }
      } else {
        if (partsize > bestsize) {
          //found a [better] match
          bestsize = partsize;
          bestidx = idx;
        }
      }
    }
    if (bestidx == -1) {
      JFLog.log("findPartition() : Could not find a match.");
      return null;
    }
    part = device.parts.get(bestidx);
    if ((reqsize == -1) || (reqsize == getSize(part.size))) {
      part.number = nextNumber(device);
      part.filesys = filesys;
      part.type = "primary";
      logDevice(device, "findPartition() done : full");
      return part;
    }
    //split part into two
    int number = nextNumber(device);
    if (number == -1) return null;
    part.type = "";
    part.filesys = "Free Space";
    part.size = makeSize(getSize(part.size) - reqsize);
    Partition newpart = new Partition();
    newpart.device = part.device;
    newpart.end = part.end;
    part.end = makeSize(getSize(part.end) - reqsize);
    newpart.number = number;
    newpart.filesys = filesys;
    newpart.type = "primary";
    newpart.start = part.end;
    newpart.size = reqsizeStr;
    device.parts.add(newpart);
    logDevice(device, "findPartition() done : split");
    return newpart;
  }

  private static int nextNumber(Device device) {
    //calc next partition # that would be given by parted
    boolean used[] = new boolean[4];
    Partition part;
    for(int idx=0;idx<device.parts.size();idx++) {
      part = device.parts.get(idx);
      if (part.type.equals("primary")) {used[part.number-1] = true; continue;}
      if (part.type.equals("extended")) {used[part.number-1] = true; continue;}
    }
    for(int idx=0;idx<4;idx++) {
      if (!used[idx]) return idx+1;
    }
    return -1;  //ohOh
  }

  public static boolean deviceHasExtPart(Device device) {
    for(int idx=0;idx<device.parts.size();idx++) {
      if (device.parts.get(idx).type.equals("extended")) return true;
    }
    return false;
  }

  public static boolean createPartTable(String dev) {
    //exec "sudo parted"
    //commands : "select 'dev'" and "mktable msdos"
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("parted");
    sp.addResponse("(parted) ", "select " + dev + "\n", false);
    sp.addResponse("(parted) ", "mktable msdos\n", false);
    sp.addResponse("(parted) ", "quit\n", false);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec parted"); return false;}
    return true;
  }

  public static boolean createPart(Device device, String type, String filesys, String start, String end) {
    //exec "sudo parted"
    //commands : "select $dev" and "mkpart $type [$filesys] start end"
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("parted");
    if ((filesys != null) && (filesys.equals("swap"))) filesys = "linux-swap(v1)";
    sp.addResponse("(parted) ", "select " + device.dev + "\n", false);
    sp.addResponse("(parted) ", "mkpart " + type + " " + (filesys != null ? filesys : "") + " " + start + " " + end + "\n", false);
    sp.addResponse("(parted) ", "quit\n", false);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec parted"); return false;}
    return true;
  }

  public static boolean deletePart(Partition part) {
    //exec "sudo parted"
    //commands : "select $part.device.dev" and "rm $part.number"
    if (part.number == -1) return false;
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("parted");
    sp.addResponse("(parted) ", "select " + part.device.dev + "\n", false);
    sp.addResponse("(parted) ", "rm " + part.number + "\n", false);
    sp.addResponse("(parted) ", "quit\n", false);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec parted"); return false;}
    return true;
  }

  public static boolean format(Partition part) {
    return format(part, null);
  }

  public static boolean format(Partition part, ShellProcessListener listener) {
    //exec "sudo mkfs.* $part.device.dev+$part.number"
    if (part.number == -1) return false;
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    if (part.filesys.equals("ext4")) {cmd.add("mkfs.ext4"); /*cmd.add("-O"); cmd.add("extend");*/}
    else if (part.filesys.equals("ext3")) {cmd.add("mkfs.ext3"); /*cmd.add("-O"); cmd.add("extend");*/}
    else if (part.filesys.equals("ext3")) {cmd.add("mkfs.ext2"); /*cmd.add("-O"); cmd.add("extend");*/}
    else if (part.filesys.equals("ntfs")) {cmd.add("mkfs.ntfs"); cmd.add("-Q");}
    else if (part.filesys.equals("swap")) cmd.add("mkswap");
    else if (part.filesys.equals("fat32")) {cmd.add("mkfs.vfat"); cmd.add("-F"); cmd.add("32");}
    else if (part.filesys.equals("fat16")) {cmd.add("mkfs.vfat"); cmd.add("-F"); cmd.add("16");}
    else {
      JFLog.log("Error:Unsupported filesys:" + part.filesys);
      return false;
    }
    cmd.add(part.device.dev + part.number);
    cmd.add("-L");
    cmd.add(part.label);
    if (listener != null) sp.addListener(listener);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec " + cmd.get(1)); return false;}
    return true;
  }

  /** Creates a folder with sudo. */
  public static boolean mkdir(String folder) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("mkdir");
    cmd.add("-p");
    cmd.add(folder);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec mkdir"); return false;}
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /** Changes a folder mode bits */
  public static boolean chmod(String folder, String mod) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("chmod");
    cmd.add(mod);
    cmd.add(folder);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec chmod"); return false;}
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /** Mount --bind oldFolder newFolder */
  public static boolean mount_bind(String oldFolder, String newFolder) {
    //exec "sudo mkdir -p $folder"
    //exec "sudo mount --bind $oldFolder $newFolder"
    if (!mkdir(newFolder)) return false;
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    String output;
    cmd.add("sudo");
    cmd.add("mount");
    cmd.add("--bind");
    cmd.add(newFolder);
    cmd.add(oldFolder);
    output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec mount"); return false;}
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /** Mount a part to a folder.  The folder will be created if needed. */
  public static boolean mount(Partition part, String folder) {
    //exec "sudo mkdir -p $folder"
    //exec "sudo mount $device+$part.number $folder"
    if (part.number == -1) return false;
    if (!mkdir(folder)) return false;
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    String output;
    cmd.add("sudo");
    cmd.add("mount");
    cmd.add(part.device.dev + part.number);
    cmd.add(folder);
    output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec mount"); return false;}
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  /** Mount dev to folder */
  public static boolean mount(String dev, String folder) {
    //exec "sudo mkdir -p $folder"
    //exec "sudo mount --bind $oldFolder $newFolder"
    if (!mkdir(folder)) return false;
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    String output;
    cmd.add("sudo");
    cmd.add("mount");
    cmd.add(dev);
    cmd.add(folder);
    output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec mount"); return false;}
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  public static boolean umount(String dev) {
    //exec "sudo mount /mnt/install"
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("sudo");
    cmd.add("umount");
    cmd.add(dev);
    String output = sp.run(cmd, true);
    if (output == null) {JFLog.log("Failed to exec umount"); return false;}
    if (output.length() > 0) {
      JFLog.log("Error:" + output);
      return false;
    }
    return true;
  }

  public static void clearfstab() {
    fstab = new ArrayList<String>();
  }

  public static void addfstab(String dev, String mount, String filesys, String opts, int dump, int pass) {
    //<file system> <mount point>   <filesys>  <options>       <dump>  <pass>
    if (mount.equals("swap")) mount = "none";
    fstab.add(dev + "\t" + mount + "\t" + filesys + "\t" + opts + "\t" + dump + "\t" + pass);
  }

  /** Outputs current partition table to log. */
  public static void logDevice(Device device, String msg) {
    if (msg != null) JFLog.log(msg);
    JFLog.log("Device:" + device.dev + ":" + device.model);
    JFLog.log("Part:  Number\t   Start\t     End\t    Type\t File system\t    Size\t   Mount");
    for(int p=0;p<device.parts.size();p++) {
      Partition part = device.parts.get(p);
      String log = String.format("Part:%8d\t%8s\t%8s\t%8s\t%12s\t%8s\t%8s"
        , part.number, part.start, part.end, part.type, part.filesys, part.size, part.mount);
      JFLog.log(log);
    }
  }
}

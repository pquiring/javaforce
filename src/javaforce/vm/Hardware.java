package javaforce.vm;

/** Hardware setup for a Virtual Machine.
 *
 * Supported hardware:
 *   machine : kvm -machine ?
 *     pc alias = pc-i440fx-7.2
 *     q35 alias = pc-q35-7.2
 *   usb : kvm -device ? | grep usb
 *     piix3-uhci
 *     piix4-uhci
 *     ich9-uhci6
 *
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Hardware implements Serializable {
  private static final long serialVersionUID = 1L;

  public String pool;
  public String name;
  public String genid;
  public int os;
  public int cores;
  public int tpm;
  public Size memory;
  public String machine = "pc";
  public String video = "vga";
  public int vram = 16384;
  public ArrayList<Disk> disks;
  public ArrayList<Network> networks;
  public ArrayList<Device> devices;
  public ArrayList<Controller> controllers;

  public boolean bios_efi;
  public boolean bios_secure;
  public boolean video_3d_accel;

  public static final int OS_LINUX = 0;
  public static final int OS_WINDOWS = 1;

  public static final int TPM_NONE = 0;
  public static final int TPM_1_2 = 1;
  public static final int TPM_2_0 = 2;

  public Hardware() {
    pool = "default";
    name = "default";
    genid = JF.generateUUID();
    os = OS_LINUX;
    cores = 2;
    memory = new Size(1, Size.GB);
    disks = new ArrayList<>();
    networks = new ArrayList<>();
    devices = new ArrayList<>();
    controllers = new ArrayList<>();
  }

  public Hardware(String pool, String name, int os, int cores, Size memory) {
    this.pool = pool;
    this.name = name;
    this.genid = JF.generateUUID();
    this.os = os;
    this.cores = cores;
    this.memory = memory;
    disks = new ArrayList<>();
    networks = new ArrayList<>();
    devices = new ArrayList<>();
    controllers = new ArrayList<>();
  }

  public static Hardware load(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      Hardware hardware = (Hardware)Compression.deserialize(fis, new File(file).length());
      fis.close();
      return hardware;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public synchronized boolean save(String file) {
    try {
      FileOutputStream fos = new FileOutputStream(file);
      boolean res = Compression.serialize(fos, this);
      fos.close();
      return res;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public String getPath() {
    return "/volumes/" + pool + "/" + name;
  }

  public void addNetwork(Network network) {
    networks.add(network);
  }

  public void removeNetwork(Network network) {
    networks.remove(network);
  }

  public void addDisk(Disk disk) {
    disks.add(disk);
  }

  public void removeDisk(Disk disk) {
    disks.remove(disk);
  }

  public String getNextDiskName() {
    int num = disks.size();
    do {
      String name = "disk" + num;
      boolean ok = true;
      for(Disk disk : disks) {
        if (disk.name.equals(name)) {
          ok = false;
        }
      }
      if (ok) return name;
      num++;
    } while (true);
  }

  public boolean hasDisk(String name) {
    for(Disk disk : disks) {
      if (disk.name.equals(name)) return true;
    }
    return false;
  }

  public void addDevice(Device device) {
    devices.add(device);
  }

  public void removeDevice(Device device) {
    devices.remove(device);
  }

  public void addController(Controller ctrl) {
    controllers.add(ctrl);
  }

  public void removeController(Controller ctrl) {
    controllers.remove(ctrl);
  }

  public String getTPMVersion() {
    switch (tpm) {
      case TPM_1_2: return "1.2";
      case TPM_2_0: return "2.0";
    }
    return "0.0";
  }

  public void validate() {
    if (genid == null) {
      genid = JF.generateUUID();
    }
    if (cores == 0) {
      cores = 2;
    }
    if (machine == null) {
      machine = "pc";
    }
    if (video == null) {
      video = "vga";
    }
    if (vram <= 0) {
      vram = 16384;
    }
    if (memory == null) {
      memory = new Size(1, Size.GB);
    }
    if (disks == null) {
      disks = new ArrayList<>();
    }
    if (networks == null) {
      networks = new ArrayList<>();
    }
    if (devices == null) {
      devices = new ArrayList<>();
    }
    if (controllers == null) {
      controllers = new ArrayList<>();
    }
    int idx = 0;
    for(Disk disk : disks) {
      disk.target_dev = String.format("sd%c", 'a' + idx);
      idx++;
    }
  }
}

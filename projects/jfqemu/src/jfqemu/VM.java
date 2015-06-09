package jfqemu;

/**
 * Created : Apr 21, 2012
 *
 * @author pquiring
 */

import java.util.ArrayList;

import javaforce.*;

public class VM {
  public String name;
  public String folder;
  public String os;
  public String hda,hdb,hdc,hdd;  //if ends in .iso or starts with "/dev/" it's a CD-ROM
  public boolean hdacd, hdbcd, hdccd, hddcd;
  public String hdaif, hdbif, hdcif, hddif;  //interface : ide, sata, scsi
  public String boot;
  public int memory = 256;  //in MBs
  public int cpuCount = 1;
  public String cpuType;  //i386 | x86_64
  public int serviceID;  //1-99 (-1 = none)
  public String video, sound;
  public String netModel, netCount;  //obsolete fields (v0.1)
  public String net1model, net1type, net2model, net2type, net3model, net3type;
  public boolean usb;
  public String chipset;  //I440FX(PIIX3) or Q35(ICH9)

  private boolean sata;
  private void addDrive(ArrayList<String> cmd, String hd, boolean cd, String hdif, int idx) {
    String iface = null, id = null;
    if (hdaif.equals("ide")) {
      iface = "ide";
    } else if (hdaif.equals("scsi")) {
      iface = "scsi";
    } else if (hdaif.equals("sata")) {
      sata = true;
      iface = "none";
      cmd.add("-device");
      cmd.add("ide-drive,drive=sata" + idx + ",bus=achi." + idx);
      id = "sata" + idx;
    } else {
      //unknown option
      iface = "ide";
    }
    cmd.add("-drive");
    cmd.add("file=" + hd + ",if=" + iface + ",media=" + (cd ? "cdrom" : "disk") + (id != null ? ",id=" + id : ""));
  }
  public String[] getCMD(boolean asService) {
    ArrayList<String> cmd = new ArrayList<String>();
    if (net1model == null) net1model = "disabled";
    if (net2model == null) net2model = "disabled";
    if (net3model == null) net3model = "disabled";
    if (hdaif == null) hdaif = "ide";
    if (hdbif == null) hdbif = "ide";
    if (hdcif == null) hdcif = "ide";
    if (hddif == null) hddif = "ide";
    sata = false;
    if (JF.isWindows()) {
      cmd.add(System.getenv("ProgramFiles") + "\\QEMU\\" + "qemu-system-" + cpuType);
    } else {
      cmd.add("qemu-system-" + cpuType);
    }
    if (hda.length() > 0) {
      addDrive(cmd, hda, hdacd, hdaif, 0);
    }
    if (hdb.length() > 0) {
      addDrive(cmd, hdb, hdbcd, hdbif, 1);
    }
    if (hdc.length() > 0) {
      addDrive(cmd, hdc, hdccd, hdcif, 2);
    }
    if (hdd.length() > 0) {
      addDrive(cmd, hdd, hddcd, hddif, 3);
    }
    if (boot.equals("d")) {
      cmd.add("-boot");
      cmd.add("once=d");
    }
    if (sata) {
      cmd.add("-device");
      cmd.add("ich9-ahci,id=sata");
    }
    if (chipset != null && chipset.equals("ICH9")) {
      cmd.add("-machine");
      cmd.add("q35");
    }
    cmd.add("-m");  //memory (megs)
    cmd.add("" + memory);
    if (!sound.equals("none")) {
      cmd.add("-soundhw");
      cmd.add(sound);
    }
    cmd.add("-vga");
    cmd.add(video);
    if (!net1model.equals("disabled")) {
      if (net1type.equals("user")) {
        cmd.add("-net");
        cmd.add("nic,model=" + net1model);
        cmd.add("-net");
        cmd.add("user");
      } else {
        cmd.add("-net");
        cmd.add("nic,model=" + net1model);
        cmd.add("-net");
        cmd.add("tap,ifname=" + net1type + ",script=no,downscript=no");
      }
    }
    if (!net2model.equals("disabled")) {
      if (net2type.equals("user")) {
        cmd.add("-net");
        cmd.add("nic,model=" + net2model);
        cmd.add("-net");
        cmd.add("user");
      } else {
        cmd.add("-net");
        cmd.add("nic,model=" + net2model);
        cmd.add("-net");
        cmd.add("tap,ifname=" + net2type + ",script=no,downscript=no");
      }
    }
    if (!net3model.equals("disabled")) {
      if (net3type.equals("user")) {
        cmd.add("-net");
        cmd.add("nic,model=" + net3model);
        cmd.add("-net");
        cmd.add("user");
      } else {
        cmd.add("-net");
        cmd.add("nic,model=" + net3model);
        cmd.add("-net");
        cmd.add("tap,ifname=" + net3type + ",script=no,downscript=no");
      }
    }
    if (usb) cmd.add("-usb");
    if (cpuCount > 1) {
      cmd.add("-smp");
      cmd.add("" + cpuCount);
    }
    if (asService) {
      cmd.add("-vnc");
      cmd.add("127.0.0.1:" + serviceID);
    }
    return (String[])cmd.toArray(new String[0]);
  }
}

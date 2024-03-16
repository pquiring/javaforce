package javaforce.vm;

/** Virtual Machine registered with libvirt. */

import java.io.*;

public class VirtualMachine implements Serializable {
  private static final long serialVersionUID = 1L;

  private VirtualMachine(String pool, String name) {
    this.pool = pool;
    this.name = name;
  }

  private String pool;
  private String name;

  public static final int STATE_OFF = 0;
  public static final int STATE_ON = 1;
  public static final int STATE_SUSPEND = 2;
  public static final int STATE_ERROR = 3;

  public String getPool() {return pool;}
  public String getName() {return name;}

  public String getConfigFile() {
    return "/volumes/" + pool + "/" + name + "/" + name + ".jfvm";
  }

  //virDomainCreate
  private native static boolean nstart(String name);
  public boolean start() {
    return nstart(name);
  }

  //virDomainShutdown()
  private native static boolean nstop(String name);
  public boolean stop() {
    return nstop(name);
  }

  //virDomainShutdown()
  private native static boolean npoweroff(String name);
  public boolean poweroff() {
    return npoweroff(name);
  }

  //virDomainRestart
  private native static boolean nrestart(String name);
  public boolean restart() {
    return nrestart(name);
  }

  //virDomainSave
  private native static boolean nsuspend(String name);
  public boolean suspend() {
    return nsuspend(name);
  }

  //virDomainRestore
  private native static boolean nrestore(String name);
  public boolean restore() {
    return nrestore(name);
  }

  private native static int ngetState(String name);
  public int getState() {
    return ngetState(name);
  }

  private static VirtualMachine getByDesc(String desc) {
    String[] fs = desc.split(";");
    String name = null, pool = null;
    for(int a=0;a<fs.length;a++) {
      String f = fs[a];
      int i = f.indexOf('=');
      if (i == -1) continue;
      String key = f.substring(0, i).trim();
      String value = f.substring(i + 1).trim();
      switch (key) {
        case "pool": pool = value; break;
        case "name": name = value; break;
      }
    }
    return new VirtualMachine(pool, name);
  }

  //virConnectListAllDomains & virDomainGetUUID & virDomainGetName & virDomainGetDesc
  private native static String[] nlist();
  public static VirtualMachine[] list() {
    String[] list = nlist();
    if (list == null) list = new String[0];
    VirtualMachine[] vms = new VirtualMachine[list.length];
    for(int idx = 0;idx<list.length;idx++) {
      vms[idx] = getByDesc(list[idx]);
    }
    return vms;
  }

  //returns vm desc
  private native static String nget(String name);
  public static VirtualMachine get(String name) {
    String vm = nget(name);
    if (vm == null) return null;
    return getByDesc(vm);
  }

  //virDomainDefineXML
  private native static boolean nregister(String xml);
  public static VirtualMachine register(Hardware hardware, NetworkProvider provider) {
    if (!nregister(createXML(hardware, provider))) return null;
    return new VirtualMachine(hardware.pool, hardware.name);
  }

  //virDomainUndefine
  private native static boolean nunregister(String name);
  public boolean unregister() {
    return nunregister(name);
  }

  private native static boolean nmigrate(String name, String desthost, Status status);
  /** Live/offline VM migration. */
  public boolean migrateCompute(String desthost, Status status) {
    return nmigrate(name, desthost, status);
  }

  /** Offline only VM storage migration. */
  public boolean migrateData(String vmname, String destpool, Status status) {
    //TODO : move vm folder to new pool
    return false;
  }

  /** Generate XML for a new VM.
   *
   * @param hardware = VM hardware setup
   * @param provider = network provider to lookup network details
   *
   * @return XML
   */
  private static String createXML(Hardware hardware, NetworkProvider provider) {
    StringBuilder xml = new StringBuilder();
    xml.append("<domain type='kvm'>");
    xml.append("<name>" + hardware.name + "</name>");
    xml.append("<uuid>" + UUID.generate() + "</uuid>");
    xml.append("<genid>" + hardware.genid + "</genid>");
    xml.append("<title>" + hardware.name + "</title>");
    xml.append("<description>");  //desc is used for metadata
      xml.append("pool=" + hardware.pool);
      xml.append(";name=" + hardware.name);
    xml.append("</description>");
    if (hardware.os == Hardware.OS_WINDOWS) {
      xml.append("<clock offset='localtime'/>");
    } else {
      xml.append("<clock offset='utc'/>");
    }
    xml.append("<os");
      if (hardware.bios_efi) {
        xml.append(" firmware='efi'");
      }
      xml.append(">");
      xml.append("<type arch='x86_64'>hvm</type>");
      if (hardware.bios_secure) {
        xml.append("<loader secure='yes'/>");
      }
      if (hardware.bios_efi) {
        xml.append("<nvram type='file'>");
        xml.append("<source file='/volumes/" + hardware.pool + "/" + hardware.name + "/" + hardware.name + ".nvram'/>");
        xml.append("</nvram>");
      }
      xml.append("<boot dev='hd'/>");
      xml.append("<bootmenu enable='yes' timeout='3000'/>");
      xml.append("<smbios mode='emulate'/>");
    xml.append("</os>");
    xml.append(hardware.memory.toMemoryXML());
    xml.append("<features>");
      xml.append("<acpi/>");
      xml.append("<apic/>");
      xml.append("<pae/>");
    xml.append("</features>");
/*
  <on_poweroff>destroy</on_poweroff>
  <on_reboot>restart</on_reboot>
  <on_crash>restart</on_crash>
*/
    xml.append("<devices>");
      //keyboard
      xml.append("<input type='keyboard' bus='usb'/>");
      //mouse
      xml.append("<input type='mouse' bus='usb'/>");
      //video card
      xml.append("<video>");
        xml.append("<model type='vmvga' vram='16384' heads='1'/>");
  //      xml.append("<driver name='qemu'/>");
      xml.append("</video>");
      //remote viewing
      xml.append("<graphics type='vnc' port='-1' autoport='yes' sharePolicy='allow-exclusive'/>");
  //    xml.append("<acceleration accel3d='no' accel2d='yes'/>");
      if (hardware.disks != null) {
        for(Disk drive : hardware.disks) {
          xml.append(drive.toXML());
        }
      }
      if (hardware.networks != null) {
        for(Network nic : hardware.networks) {
          int vlan = provider.getVLAN(nic.network);
          String bridge = provider.getBridge(nic.network);
          xml.append(nic.toXML(bridge, vlan));
        }
      }
      if (hardware.devices != null) {
        for(Device device : hardware.devices) {
          xml.append(device.toXML());
        }
      }
    xml.append("</devices>");
    xml.append("</domain>");
    return xml.toString();
  }

  public static void main(String[] args) {
    VirtualMachine vm = new VirtualMachine("pool", "example");
    Disk disk = new Disk();
    disk.pool = "pool";
    disk.vmname = "example";
    disk.name = "disk";
    disk.type = Disk.TYPE_VMDK;
    disk.target_dev = "sda";
    disk.target_bus = "scsi";
    //disk...
    Network nw = new Network();
    nw.network = "servers";
    nw.model = "vmxnet3";
    nw.mac = "00:11:22:33:44:55";
    Hardware hw = new Hardware();
    hw.pool = "pool";
    hw.name = "example";
    hw.genid = UUID.generate();
    hw.os = Hardware.OS_WINDOWS;
    hw.cores = 4;
    hw.memory = new Size(4, Size.GB);
    hw.disks = new Disk[] {disk};
    hw.networks = new Network[] {nw};
    hw.devices = new Device[0];
    System.out.println(createXML(hw, new NetworkProvider() {
      public int getVLAN(String name) {
        return 1;
      }
      public String getBridge(String name) {
        return "virbr0";
      }
    }));
  }
}

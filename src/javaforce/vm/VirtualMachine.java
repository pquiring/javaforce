package javaforce.vm;

/** Virtual Machine registered with libvirt. */

import java.io.*;
import java.util.*;
import java.nio.file.*;

import javaforce.*;
import javaforce.webui.tasks.*;

public class VirtualMachine implements Serializable {
  private static final long serialVersionUID = 1L;

  private VirtualMachine(String pool, String name, String uuid, int vnc) {
    //existing vm
    this.pool = pool;
    this.name = name;
    this.uuid = uuid;
    this.vnc = vnc;
  }

  public VirtualMachine(Hardware hardware) {
    //new vm
    pool = hardware.pool;
    name = hardware.name;
    uuid = JF.generateUUID();
    vnc = -1;  //update during register
  }

  public static native boolean init();

  public String pool;
  public String name;
  private String uuid;  //only valid while registered
  private int vnc;

  public static final int STATE_OFF = 0;
  public static final int STATE_ON = 1;
  public static final int STATE_SUSPEND = 2;
  public static final int STATE_ERROR = 3;

  public String getPool() {return pool;}
  public String getName() {return name;}
  public int getVNC() {return vnc;}
  public String getUUID() {return uuid;}

  public String getPath() {
    return "/volumes/" + pool + "/" + name;
  }

  public String getConfigFile() {
    return getPath() + "/" + name + ".jfvm";
  }

  public Hardware loadHardware() {
    return Hardware.load(getConfigFile());
  }

  public boolean saveHardware(Hardware hardware) {
    new File(getPath()).mkdirs();
    return hardware.save(getConfigFile());
  }

  public void create_stats_folder() {
    new File("/var/jfkvm/stats/" + uuid).mkdir();
  }

  public boolean check_write_access() {
    File file = new File(getPath() + "/jfkvm-test.tmp");
    byte[] data = Long.toString(System.currentTimeMillis()).getBytes();
    try {
      if (file.exists()) {
        file.delete();
      }
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(data);
      fos.close();
      FileInputStream fis = new FileInputStream(file);
      byte[] verify = fis.readAllBytes();
      fis.close();
      if (verify == null || verify.length != data.length) {
        throw new Exception("check_write_access:failed");
      }
      for(int a=0;a<data.length;a++) {
        if (verify[a] != data[a]) {
          throw new Exception("check_write_access:failed");
        }
      }
      file.delete();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      try { file.delete(); } catch (Exception e2) {}
      return false;
    }
  }

  //virDomainCreate
  private native static boolean nstart(String name);
  public boolean start() {
    if (!check_write_access()) return false;
    create_stats_folder();
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

  public String getStateString() {
    int state = getState();
    switch (state) {
      case STATE_OFF: return "off";
      case STATE_ON: return "on";
      case STATE_SUSPEND: return "suspended";
      case STATE_ERROR: return "error";
    }
    return "???";
  }

  public String[] getStates() {
    return new String[] {name, getStateString(), pool};
  }

  private static VirtualMachine getByDesc(String desc) {
    String[] fs = desc.split(";");
    String pool = null;
    String name = null;
    String uuid = null;
    int vnc = 0;
    for(int a=0;a<fs.length;a++) {
      String f = fs[a];
      int i = f.indexOf('=');
      if (i == -1) continue;
      String key = f.substring(0, i).trim();
      String value = f.substring(i + 1).trim();
      switch (key) {
        case "pool": pool = value; break;
        case "name": name = value; break;
        case "uuid": uuid = value; break;
        case "vnc": vnc = Integer.valueOf(value); break;
      }
    }
    return new VirtualMachine(pool, name, uuid, vnc);
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
  public static boolean register(VirtualMachine vm, Hardware hardware, VMProvider provider) {
    String xml = createXML(vm, hardware, provider);
    JFLog.log("VirtualMachine.xml=" + xml);
    return nregister(xml);
  }

  public boolean reregister(Hardware hardware, VMProvider provider) {
    String xml = createXML(this, hardware, provider);
    JFLog.log("VirtualMachine.xml=" + xml);
    return nregister(xml);
  }

  //virDomainUndefine
  private native static boolean nunregister(String name);
  public boolean unregister() {
    return nunregister(name);
  }

  private native static boolean nmigrate(String name, String desthost, boolean live, Status status);
  /** Live/offline VM migration. */
  public boolean migrateCompute(String desthost, boolean live, Status status) {
    return nmigrate(name, desthost, live, status);
  }

  /** Offline only VM storage migration. */
  public boolean migrateData(Storage dest_pool, Hardware hw, Status status, VMProvider provider) {
    if (status == null) {
      status = Status.null_status;
    }
    String _src_folder = "/volumes/" + pool + "/" + name;
    String _dest_folder = "/volumes/" + dest_pool.name + "/" + name;
    File src_folder = new File(_src_folder);
    if (!src_folder.exists()) {
      status.setStatus("Source folder not found");
      status.setResult(false);
      return false;
    }
    File dest_folder = new File(_dest_folder);
    if (dest_folder.exists()) {
      status.setStatus("Dest folder already exists");
      status.setResult(false);
      return false;
    }
    dest_folder.mkdir();
    if (!dest_folder.exists()) {
      status.setStatus("Unable to create Dest folder");
      status.setResult(false);
      return false;
    }
    File[] files = src_folder.listFiles();
    if (files == null || files.length == 0) {
      status.setStatus("No files found");
      status.setResult(false);
      return false;
    }
    int done = 0;
    int todo = files.length;
    status.setPercent(0);
    status.setStatus("Moving files...");
    for(File file : files) {
      if (file.isDirectory()) continue;
      String name = file.getName();
      Path src_path = file.toPath();
      Path dest_path = new File(_dest_folder + "/" + name).toPath();
      try {
        Files.move(src_path, dest_path);
      } catch (Exception e) {
        JFLog.log(e);
        status.setStatus("Move failed, see logs.");
        status.setResult(false);
        return false;
      }
      status.setPercent((done * 100) / todo);
    }
    src_folder.delete();

    String src_pool = pool;

    //update pool
    pool = dest_pool.name;
    hw.pool = dest_pool.name;
    //update disks that were moved
    for(Disk disk : hw.disks) {
      if (disk.pool.equals(src_pool)) {
        disk.pool = dest_pool.name;
      }
    }
    if (!saveHardware(hw)) {
      status.setStatus("Move failed, see logs.");
      status.setResult(false);
      return false;
    }
    if (!register(this, hw, provider)) {
      status.setStatus("Clone failed, see logs.");
      status.setResult(false);
      return false;
    }

    status.setPercent(100);
    status.setStatus("Completed");
    status.setResult(true);
    return true;
  }

  /** Offline only VM storage clone. */
  public boolean cloneData(Storage dest_pool, String new_name, Status status, VMProvider provider) {
    if (status == null) {
      status = Status.null_status;
    }
    String _src_folder = "/volumes/" + pool + "/" + name;
    String _dest_folder = "/volumes/" + dest_pool.name + "/" + new_name;
    File src_folder = new File(_src_folder);
    if (!src_folder.exists()) {
      status.setStatus("Source folder not found");
      status.setResult(false);
      return false;
    }
    File dest_folder = new File(_dest_folder);
    if (dest_folder.exists()) {
      status.setStatus("Dest folder already exists");
      status.setResult(false);
      return false;
    }
    dest_folder.mkdir();
    if (!dest_folder.exists()) {
      status.setStatus("Unable to create Dest folder");
      status.setResult(false);
      return false;
    }
    File[] files = src_folder.listFiles();
    if (files == null || files.length == 0) {
      status.setStatus("No files found");
      status.setResult(false);
      return false;
    }
    int done = 0;
    int todo = files.length;
    status.setPercent(0);
    status.setStatus("Copying files...");
    for(File file : files) {
      if (file.isDirectory()) continue;
      String name = file.getName();
      Path src_path = file.toPath();
      Path dest_path = new File(_dest_folder + "/" + name).toPath();
      try {
        Files.copy(src_path, dest_path);
      } catch (Exception e) {
        JFLog.log(e);
        status.setStatus("Clone failed, see logs.");
        status.setResult(false);
        return false;
      }
      status.setPercent((done * 100) / todo);
    }
    VirtualMachine clone = new VirtualMachine(dest_pool.name, new_name, null, -1);
    Hardware hw = clone.loadHardware();
    if (hw == null) {
      status.setStatus("Clone failed, see logs.");
      status.setResult(false);
      return false;
    }
    //update name
    hw.name = new_name;
    if (!pool.equals(dest_pool.name)) {
      //also moved to a new storage pool - update pool and disks
      hw.pool = dest_pool.name;
      //update disks that were copied
      for(Disk disk : hw.disks) {
        if (disk.pool.equals(pool)) {
          disk.pool = dest_pool.name;
        }
      }
    }
    if (!clone.saveHardware(hw)) {
      status.setStatus("Clone failed, see logs.");
      status.setResult(false);
      return false;
    }
    if (!register(clone, hw, provider)) {
      status.setStatus("Clone failed, see logs.");
      status.setResult(false);
      return false;
    }
    status.setPercent(100);
    status.setStatus("Completed");
    status.setResult(true);
    return true;
  }

  /** Generate XML to register a new VM or replace existing one.
   *
   * @param vm = VirtualMachine
   * @param hardware = VM hardware setup
   * @param provider = network provider to lookup network details
   *
   * @return XML
   */
  private static String createXML(VirtualMachine vm, Hardware hardware, VMProvider provider) {
    vm.vnc = provider.getVNCPort(hardware.name);
    String hostname = provider.getServerHostname();
    StringBuilder xml = new StringBuilder();
    xml.append("<domain type='kvm'>");
    xml.append("<name>" + hardware.name + "</name>");
    xml.append("<uuid>" + vm.uuid + "</uuid>");
    xml.append("<genid>" + hardware.genid + "</genid>");
    xml.append("<title>" + hardware.name + "</title>");
    xml.append("<description>");  //desc is used for metadata
      xml.append("pool=" + hardware.pool);
      xml.append(";name=" + hardware.name);
      xml.append(";uuid=" + vm.uuid);
      xml.append(";vnc=" + vm.vnc);
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
      xml.append("<type arch='x86_64' machine='" + hardware.machine + "'>hvm</type>");
      if (hardware.bios_secure) {
        xml.append("<loader secure='yes'/>");
      }
      if (hardware.bios_efi) {
        xml.append("<nvram type='file'>");
        xml.append("<source file='/volumes/" + hardware.pool + "/" + hardware.name + "/" + hardware.name + ".nvram'/>");
        xml.append("</nvram>");
      }
      xml.append("<bootmenu enable='yes' timeout='3000'/>");
      xml.append("<smbios mode='emulate'/>");
    xml.append("</os>");
    xml.append("<vcpu>" + hardware.cores + "</vcpu>");
    xml.append("<cpu>");
    xml.append(" <topology sockets='1' dies='1' clusters='1' cores='" + hardware.cores + "' threads='1'/>");
    xml.append("</cpu>");
    xml.append(hardware.memory.toMemoryXML());
    xml.append("<features>");
      xml.append("<acpi/>");
      xml.append("<apic/>");
      xml.append("<pae/>");
    xml.append("</features>");
/*
    xml.append("<on_poweroff>destroy</on_poweroff>");
    xml.append("<on_reboot>restart</on_reboot>");
    xml.append("<on_crash>destroy</on_crash>");
*/
    xml.append("<devices>");
      if (hardware.tpm != Hardware.TPM_NONE) {
        xml.append("<tpm model='tpm-tis'>");
        xml.append(" <backend type='emulator' version='" + hardware.getTPMVersion() + "'/>");
        xml.append("</tpm>");
      }
      for(Controller c : hardware.controllers) {
        xml.append(c.toXML());
      }
      //keyboard
      xml.append("<input type='keyboard' bus='usb'/>");
      //mouse
      xml.append("<input type='mouse' bus='usb'/>");
      //audio
      xml.append("<audio id='1' type='none'/>");
      //video card
      xml.append("<video>");
      xml.append("<model type='" + hardware.video + "' vram='" + hardware.vram + "' heads='1'>");
      xml.append("</model>");
      if (hardware.video_3d_accel) {
        xml.append("<acceleration accel3d='yes'/>");
      }
      xml.append("</video>");
      //remote viewing
      if (vm.vnc != -1) {
        xml.append("<graphics type='vnc' port='" + vm.vnc + "' autoport='no' sharePolicy='allow-exclusive' passwd='" + provider.getVNCPassword() + "'>");
        xml.append("<listen type='address' address='127.0.0.1'/>");
        xml.append("</graphics>");
      }
      if (hardware.disks != null) {
        for(Disk drive : hardware.disks) {
          xml.append(drive.getHardwareXML(hardware.os));
        }
      }
      if (hardware.networks != null) {
        for(Network nic : hardware.networks) {
          int vlan = provider.getVLAN(nic.network);
          NetworkBridge bridge = provider.getBridge(nic.network);
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
    VirtualMachine vm = new VirtualMachine("pool", "example", JF.generateUUID(), 5901);
    Disk disk = new Disk();
    disk.pool = "pool";
    disk.folder = "example";
    disk.name = "disk";
    disk.type = Disk.TYPE_VMDK;
    disk.target_dev = "sda";
    disk.target_bus = "scsi";
    //disk...
    Network nw = new Network("servers", "vmxnet3", MAC.generate());
    Hardware hw = new Hardware("pool", "example", Hardware.OS_WINDOWS, 4, new Size(4, Size.GB));
    hw.disks.add(disk);
    hw.networks.add(nw);
    System.out.println(createXML(vm, hw, new VMProvider() {
      public int getVLAN(String name) {
        return 1;
      }
      public NetworkBridge getBridge(String name) {
        return new NetworkBridge("virbr0", "os", "eth0");
      }
      public int getVNCPort(String name) {
        return 5901;
      }
      public String getVNCPassword() {
        return "password";
      }
      public String getServerHostname() {
        return "127.0.0.1";
      }
      public boolean addsshkey(String host, String key) {
        return false;
      }
    }));
  }
}

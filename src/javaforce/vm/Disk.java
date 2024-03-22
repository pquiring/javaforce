package javaforce.vm;

/** Disk.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Disk implements Serializable {
  private static final long serialVersionUID = 1L;

  public String pool;  //storage pool
  public String folder;  //usually vm name
  public String name;  //filename
  public int type;
  public Size size;
  public int boot_order;  //0=none 1=first etc.
  public String target_dev = "sd%c";
  public String target_bus = "scsi";

  public static final int TYPE_VMDK = 0;
  public static final int TYPE_QCOW2 = 1;
  public static final int TYPE_ISO = 2;

  public static final int PROVISION_THICK = 0;
  public static final int PROVISION_THIN = 1;

  public String getType() {
    switch (type) {
      case TYPE_QCOW2: return "qcow2";
      case TYPE_VMDK: return "vmdk";
      case TYPE_ISO: return "iso";
    }
    return "";
  }

  public static int getType(String ext) {
    switch (ext) {
      case "qcow2": return TYPE_QCOW2;
      case "vmdk": return TYPE_VMDK;
      case "iso": return TYPE_ISO;
    }
    return -1;
  }

  public String getPath() {
    return "/volumes/" + pool + "/" + folder + "/" + name + '.' + getType();
  }

  private String getPath2() {
    if (type == TYPE_VMDK) {
      String flat = "/volumes/" + pool + "/" + folder + "/" + name + "-flat." + getType();
      if (new File(flat).exists()) {
        return flat;
      }
    }
    return "/volumes/" + pool + "/" + folder + "/" + name + '.' + getType();
  }

  private native static boolean ncreate(String pool_name, String xml);
  /** Provision virtual disk for a VirtualMachine. */
  public boolean create(Storage pool, int provision) {
    if (false) {
      //use libvirt (not working per docs)
      String xml = getCreateXML(provision);
      JFLog.log("Disk.xml=" + xml);
      return ncreate(pool.name, xml);
    } else {
      //use qemu-img
      ShellProcess sp = new ShellProcess();
      switch (type) {
        case TYPE_VMDK: {
          String subformat = "";
          switch (provision) {
            case PROVISION_THICK:
              subformat = "monolithicFlat";
              break;
            default:
            case PROVISION_THIN:
              subformat = "monolithicSparse";
              break;
          }
          sp.run(new String[] {"/usr/bin/qemu-img", "create", "-f", getType(), "-o", "subformat=" + subformat, getPath(), size.getSize()}, true);
          break;
        }
        case TYPE_QCOW2: {
          String preallocation = "";
          switch (provision) {
            case PROVISION_THICK:
              preallocation = "metadata";  //or "full" which is slower
              break;
            default:
            case PROVISION_THIN:
              preallocation = "off";
              break;
          }
          sp.run(new String[] {"/usr/bin/qemu-img", "create", "-f", getType(), "-o", "preallocation=" + preallocation, getPath(), size.getSize()}, true);
          break;
        }
      }
      return sp.getErrorLevel() == 0;
    }
  }

  public boolean resize(Storage pool) {
    if (false) {
      //use libvirt (not working per docs)
      String xml = getCreateXML(0);
      JFLog.log("Disk.xml=" + xml);
      return ncreate(pool.name, xml);
    } else {
      //use qemu-img
      ShellProcess sp = new ShellProcess();
      switch (type) {
        case TYPE_VMDK:
          sp.run(new String[] {"/usr/bin/qemu-img", "resize", getPath(), size.getSize()}, true);
          break;
        case TYPE_QCOW2:
          sp.run(new String[] {"/usr/bin/qemu-img", "resize", getPath(), size.getSize()}, true);
          break;
      }
      return sp.getErrorLevel() == 0;
    }
  }

  public String getHardwareXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("<disk type='file' device='disk'>");
    xml.append("<source file='" + getPath2() + "'>");
    xml.append("</source>");
    xml.append("<target dev='" + target_dev + "' bus='" + target_bus + "'/>");
    if (boot_order > 0) {
      xml.append("<boot order='" + boot_order + "'/>");
    }
    xml.append("</disk>");
    return xml.toString();
  }

  private String getCreateXML(int provision) {
    StringBuilder xml = new StringBuilder();
    xml.append("<volume type='file'>");
    xml.append("<name>" + name + "</name>");
    xml.append("<allocation unit='" + size.getUnitChar() + "iB'>");
    switch (provision) {
      case PROVISION_THICK:
        xml.append(size.size);
        break;
      case PROVISION_THIN:
        xml.append("0");
        break;
    }
    xml.append("</allocation>");
    xml.append("<capacity unit='" + size.getUnitChar() + "iB'>");
    xml.append(size.size);
    xml.append("</capacity>");
    xml.append("<target>");
    xml.append("<path>" + getPath() + "</path>");
    xml.append("<format type='" + getType() + "'/>");
    xml.append("</target>");
    xml.append("</volume>");
    return xml.toString();
  }
}

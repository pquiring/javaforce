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
  public String name;  //filename
  public int type;
  public Size size;
  public int boot_order;  //0=none 1=first etc.
  public String target_dev = "sda";
  public String target_bus = "scsi";

  public static final int TYPE_VMDK = 0;
  public static final int TYPE_QCOW2 = 1;

  public static final int PROVISION_THICK = 0;
  public static final int PROVISION_THIN = 1;

  public String getType() {
    switch (type) {
      case TYPE_QCOW2: return "qcow2";
      case TYPE_VMDK: return "vmdk";
    }
    return "";
  }

  public String getPath(Hardware hardware) {
    return "/volumes/" + pool + "/" + hardware.name + "/" + name + '.' + getType();
  }

  private native static boolean ncreate(String pool_name, String xml);
  /** Provision virtual disk for a VirtualMachine. */
  public boolean create(Hardware hardware, Storage pool, int provision) {
    if (false) {
      //use libvirt (not working per docs)
      String xml = getCreateXML(hardware, provision);
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
          sp.run(new String[] {"/usr/bin/qemu-img", "create", "-f", getType(), "-o", "subformat=" + subformat, getPath(hardware), size.getSize()}, true);
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
          sp.run(new String[] {"/usr/bin/qemu-img", "create", "-f", getType(), "-o", "preallocation=" + preallocation, getPath(hardware), size.getSize()}, true);
          break;
        }
      }
      return sp.getErrorLevel() == 0;
    }
  }

  public boolean resize(Hardware hardware, Storage pool) {
    if (false) {
      //use libvirt (not working per docs)
      String xml = getCreateXML(hardware, 0);
      JFLog.log("Disk.xml=" + xml);
      return ncreate(pool.name, xml);
    } else {
      //use qemu-img
      ShellProcess sp = new ShellProcess();
      switch (type) {
        case TYPE_VMDK:
          sp.run(new String[] {"/usr/bin/qemu-img", "resize", getPath(hardware), size.getSize()}, true);
          break;
        case TYPE_QCOW2:
          sp.run(new String[] {"/usr/bin/qemu-img", "resize", getPath(hardware), size.getSize()}, true);
          break;
      }
      return sp.getErrorLevel() == 0;
    }
  }

  public String getHardwareXML(Hardware hardware) {
    StringBuilder xml = new StringBuilder();
    xml.append("<disk type='file' device='disk'>");
    xml.append("<source file='" + getPath(hardware) + "'>");
    xml.append("</source>");
    xml.append("<target dev='" + target_dev + "' bus='" + target_bus + "'/>");
    if (boot_order > 0) {
      xml.append("<boot order='" + boot_order + "'/>");
    }
    xml.append("</disk>");
    return xml.toString();
  }

  private String getCreateXML(Hardware hardware, int provision) {
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
    xml.append("<path>" + getPath(hardware) + "</path>");
    xml.append("<format type='" + getType() + "'/>");
    xml.append("</target>");
    xml.append("</volume>");
    return xml.toString();
  }
}

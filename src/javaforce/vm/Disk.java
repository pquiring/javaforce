package javaforce.vm;

/** Disk.
 *
 * @author pquiring
 */

import java.io.*;

public class Disk implements Serializable {
  private static final long serialVersionUID = 1L;

  public String pool;  //storage pool
  public String vmname;  //virtual machine name
  public String name;  //filename
  public int type;
  public int provision;
  public Size size;
  public int boot_order;  //0=none 1=first etc.
  public String target_dev, target_bus;

  public static final int TYPE_QCOW2 = 1;
  public static final int TYPE_VMDK = 2;

  public static final int PROVISION_THIN = 1;
  public static final int PROVISION_THICK = 2;

  public String getExt() {
    switch (type) {
      case TYPE_QCOW2: return ".qcow2";
      case TYPE_VMDK: return ".vmdk";
    }
    return "";
  }

  public String getPath() {
    return "/volumes/" + pool + "/" + vmname + "/" + name + getExt();
  }

  private native static boolean ncreate(int type, int provision, long size, String fullpath);
  /** Provision virtual disk for a VirtualMachine. */
  protected boolean create() {
    return ncreate(type, provision, size.toLong(), getPath());
  }

  public String toXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("<disk type='file' device='disk'>");
    xml.append("<source file='" + getPath() + "'>");
    xml.append("</source");
    xml.append("<target dev='" + target_dev + "' bus='" + target_bus + "'/>");
    if (boot_order > 0) {
      xml.append("<boot order='" + boot_order + "'/>");
    }
    xml.append("</disk>");
    return xml.toString();
  }
}

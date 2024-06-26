package javaforce.vm;

/** Controller (optional).
 *
 * libvirt will usually automatically add required controllers.
 *
 * @author pquiring
 */

import java.io.*;

public class Controller extends Address implements Serializable {
  private static final long serialVersionUID = 1L;

  //type
  public String type;
  public String model;

  public Controller(String type, String model) {
    this.type = type;
    this.model = model;
  }

  public Controller(String type, String model, String addr_type, String domain, String bus, String slot, String function) {
    this.type = type;
    this.model = model;
    this.addr_type = addr_type;
    this.domain = domain;
    this.bus = bus;
    this.slot = slot;
    this.function = function;
  }

  public static String[] get_scsi_models() {
    return new String[] {
      "auto",
      "buslogic",
      "ibmvscsi",
      "lsilogic",
      "lsisas1068",
      "lsisas1078",
      "virtio-scsi",
      "vmpvscsi",
      "virtio-transitional",
      "virtio-non-transitional",
      "ncr53c90",
      "am53c974",
      "dc390",
    };
  }

  public static String[] get_usb_models() {
    return new String[] {
      "auto",
      "piix3-uhci",
      "piix4-uhci",
      "ehci",
      "ich9-ehci1",
      "ich9-uhci1",
      "ich9-uhci2",
      "ich9-uhci3",
      "vt82c686b-uhci",
      "pci-ohci",
      "nec-xhci",
      "qusb1",
      "qusb2",
      "qemu-xhci",
    };
  }

  public static String[] get_ide_models() {
    return new String[] {
      "auto",
      "piix3",
      "piix4",
      "ich6"
    };
  }

  public String toString() {
    return type + ":" + model;
  }

  public String toXML() {
    StringBuilder xml = new StringBuilder();
    if (type == null || type.equals("auto")) return "";
    xml.append("<controller type='" + type + "' model='" + model + "'>");
    if (!type.endsWith("-root")) {
      xml.append(getAddressXML());
    }
    xml.append("</controller>");
    return xml.toString();
  }
}

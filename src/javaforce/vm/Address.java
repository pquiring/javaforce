package javaforce.vm;

/** Address {PCI, USB}
 *
 * @author pquiring
*/

import java.io.*;

import javaforce.*;

public class Address implements Serializable {
  private static final long serialVersionUID = 1L;

  public String addr_type;
  public String domain;
  public String bus;
  public String slot;
  public String function;
  public String port;

  public Address() {
  }

  /** PCI Address
   * @param domain = 0x0000
   * @param bus = 0x00
   * @param slot = 0x01
   * @param function = 0x0
   */
  public Address(String domain, String bus, String slot, String function) {
    setPCIAddress(domain, bus, slot, function);
  }

  public void setPCIAddress(String domain, String bus, String slot, String function) {
    addr_type = "pci";
    this.domain = domain;
    this.bus = bus;
    this.slot = slot;
    this.function = function;
  }

  /** USB Address
   * @param bus = 1
   * @param port = 1
   */
  public Address(String bus, String port) {
    setUSBAddress(bus, port);
  }

  public void setUSBAddress(String bus, String port) {
    addr_type = "usb";
    this.bus = bus;
    this.port = port;
  }

  public void setAutoAddress() {
    addr_type = "auto";
    domain = null;
    bus = null;
    slot = null;
    function = null;
    port = null;
  }

  public String getType() {
    if (addr_type == null) addr_type = "auto";
    return addr_type;
  }

  public String getDomain() {
    if (domain == null) return "0x0000";
    return domain;
  }

  public String getBus(boolean pci) {
    if (bus == null) {
      if (pci) {
        return "0x00";
      } else {
        return "0";
      }
    }
    return bus;
  }

  public String getSlot() {
    if (slot == null) return "0x00";
    return slot;
  }

  public String getFunction() {
    if (function == null) return "0x0";
    return function;
  }

  public String getPort() {
    if (port == null) return "0";
    return port;
  }

  public static String cleanHex(String str, int max) {
    if (str.startsWith("0x")) {
      str = str.substring(2);
    }
    str = str.toLowerCase();
    str = JF.filter(str, JF.filter_hex);
    if (str.length() == 0 || Integer.valueOf(str, 16) > max) {
      switch (max) {
        case 0xf: return "0x0";
        case 0xff: return "0x00";
        case 0xffff: return "0x0000";
      }
    }
    return "0x" + str;
  }

  public static String cleanDec(String str, int max) {
    str = JF.filter(str, JF.filter_numeric);
    if (str.length() == 0 || Integer.valueOf(str) > max) {
      return "0";
    }
    return str;
  }

  public String getAddressXML() {
    if (addr_type == null || addr_type.equals("auto")) return "";
    StringBuilder xml = new StringBuilder();
    switch (addr_type) {
      case "pci":
        xml.append("<address type='" + addr_type + "' domain='" + domain + "' bus='" + bus + "' slot='" + slot + "' function='" + function + "'/>");
        break;
      case "usb":
        xml.append("<address type='" + addr_type + "' bus='" + bus + "' slot='" + slot + "'/>");
        break;
    }
    return xml.toString();
  }
}

package javaforce.vm;

/** Address {PCI, USB}
 *
 * @author pquiring
*/

public class Address {
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
    addr_type = "usb";
    this.bus = bus;
    this.port = port;
  }

  public String getAddressXML() {
    if (addr_type == null) return "";
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

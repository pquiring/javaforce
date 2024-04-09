package javaforce.vm;

/** Address {PCI}
 *
 * @author pquiring
*/

public class Address {
  //address
  public String addr_type;
  public String domain;
  public String bus;
  public String slot;
  public String function;

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

  public String getAddressXML() {
    StringBuilder xml = new StringBuilder();
    if (domain != null) {
      xml.append("<address type='" + addr_type + "' domain='" + domain + "' bus='" + bus + "' slot='" + slot + "' function='" + function + "'/>");
    }
    return xml.toString();
  }
}

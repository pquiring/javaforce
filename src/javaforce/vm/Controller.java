package javaforce.vm;

/** Controller (optional).
 *
 * libvirt will usually automatically add required controllers.
 *
 * @author pquiring
 */

import java.io.*;

public class Controller implements Serializable {
  private static final long serialVersionUID = 1L;

  //type
  public String type;
  public String model;
  //address
  public String addr_type = "pci";
  public String domain = "0x0000";
  public String bus = "0x00";
  public String slot = "0x01";
  public String function = "0x0";

  public Controller(String type, String model) {
    this.type = type;
    this.model = model;
    addr_type = null;
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

  public String toXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("<controller type='" + type + "' index='0' model='" + model + "'>");
    if (!type.endsWith("-root")) {
      xml.append("<address type='pci' domain='" + domain + "' bus='" + bus + "' slot='" + slot + "' function='" + function + "'/>");
    }
    xml.append("</controller>");
    return xml.toString();
  }
}

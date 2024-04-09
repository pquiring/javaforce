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

  public String toXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("<controller type='" + type + "' index='0' model='" + model + "'>");
    if (!type.endsWith("-root")) {
      xml.append(getAddressXML());
    }
    xml.append("</controller>");
    return xml.toString();
  }
}

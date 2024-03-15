package javaforce.vm;

/** Network guest NIC.
 *
 * @author pquiring
 */

import java.io.*;

public class Network implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String port;
  public String model;
  public String mac;

  /** Create XML for VirtualMachine XML. */
  public String toXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("<network>");
    xml.append("<name>" + name + "</name>");
    xml.append("<networkport>" + port + "</networkport>");
    xml.append("<model>" + model + "</model>");
    xml.append("<mac>" + model + "</mac>");
    xml.append("</network>");
    return xml.toString();
  }
}

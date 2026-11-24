package javaforce.controls.opcua;

import javaforce.controls.*;

/** OPC UA Driver.
 *
 * @author pquiring
 */

public class OPCUADriver implements PLCDriver {
  public final static String name = "PLC4J";

  public String getName() {
    return name;
  }

  public PLC create() {
    return new OPCUAPLC();
  }
}

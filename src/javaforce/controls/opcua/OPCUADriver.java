package javaforce.controls.opcua;

import javaforce.controls.*;

/** OPC UA Driver.
 *
 * @author pquiring
 */

public class OPCUADriver implements PLCDriver {

  public String getName() {
    return "OPCUA";
  }

  public PLC create() {
    return new OPCUAPLC();
  }
}

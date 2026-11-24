package javaforce.controls.opcua;

import java.util.*;

import javaforce.controls.*;

/** OPC UA Server (WIP).
 *
 * @author pquiring
 */

public class OPCUAServer extends OPCUA {

  private HashMap<String, PLC> plcs = new HashMap<>();

  public void registerPLC(String name, PLC plc) {
    plcs.put(name, plc);
  }

  public void start() {
    start(4840);
  }

  public void start(int port) {

  }
}

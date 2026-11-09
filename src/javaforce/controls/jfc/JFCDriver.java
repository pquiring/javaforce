package javaforce.controls.jfc;

import javaforce.controls.*;

/** JFC Driver.
 *
 * JFC is a JavaForce protocol implemented by jfControls.
 *
 * @author pquiring
 */

public class JFCDriver implements PLCDriver {
  public final static String name = "JFC";
  public String getName() {return name;}
  public PLC create() {
    return new JFCPLC();
  }
}

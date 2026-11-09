package javaforce.controls.plc4j;

import javaforce.controls.*;

/** PLC4J Driver.
 *
 * @author pquiring
 */

public class PLC4JDriver implements PLCDriver {
  public static String name = "PLC4J";
  public String getName() {return name;}
  public PLC create() {
    return new PLC4JPLC();
  }
}

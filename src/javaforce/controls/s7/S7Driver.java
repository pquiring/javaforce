package javaforce.controls.s7;

import javaforce.controls.*;

/** Siemens S7 PLC Driver.
 *
 * @author pquiring
 */

public class S7Driver implements PLCDriver {
  public static String name = "S7";
  public String getName() {return name;}
  public PLC create() {
    return new S7PLC();
  }
}

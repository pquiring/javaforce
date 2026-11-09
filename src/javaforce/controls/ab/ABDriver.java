package javaforce.controls.ab;

import javaforce.controls.*;

/** Allen Bradley PLC Driver.
 *
 * @author pquiring
 */

public class ABDriver implements PLCDriver {
  public static String name = "AB";
  public String getName() {return name;}
  public PLC create() {
    return new ABPLC();
  }
}

package javaforce.controls.jfc;

import javaforce.controls.*;

/** JFC Driver.
 *
 * @author pquiring
 */

public class JFCDriver implements PLCDriver {
  public static String name = "JFC";
  public String getName() {return name;}
  public PLC create() {
    return new JFCPLC();
  }
}

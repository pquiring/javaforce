package javaforce.controls.mod;

import javaforce.controls.*;

/** Modbus PLC Driver.
 *
 * Modbus devices are typically remote I/O without any "logic".
 *
 * @author pquiring
 */

public class ModbusDriver {
  public final static String name = "MB";
  public String getName() {return name;}
  public PLC create() {
    return new ModbusPLC();
  }
}

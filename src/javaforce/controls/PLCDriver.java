package javaforce.controls;

/** PLC Driver Interface.
 *
 * @author pquiring
 */

public interface PLCDriver {
  public String getName();
  public PLC create();
}

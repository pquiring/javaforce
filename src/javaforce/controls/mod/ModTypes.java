package javaforce.controls.mod;

/**
 *
 * @author pquiring
 */

public class ModTypes {
  public final static byte C = 0x1;  //coils
  public final static byte DI = 0x2;  //discrete inputs
  public final static byte IR = 0x3;  //input registers (16bit) (read-only)
  public final static byte HR = 0x4;  //holding registers (16bit) (read-write)
}

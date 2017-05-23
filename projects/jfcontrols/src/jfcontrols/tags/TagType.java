package jfcontrols.tags;

/** Tag Data Types
 *
 * @author pquiring
 */

public class TagType {
  public static final int BIT = 1;
  public static final int BYTE = 8;
  public static final int SHORT = 16;
  public static final int INT = 32;
  public static final int LONG = 64;

  public static final int CHAR = 2;  //implied unsigned
  public static final int FLOAT = 3;
  public static final int DOUBLE = 4;
  public static final int FUNCTION = 5;

  public static final int UNSIGNED = 0x8000;
  public static final int ARRAY = 0x4000;
}

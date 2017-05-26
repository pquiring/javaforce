package jfcontrols.tags;

/** Tag Data Types
 *
 * @author pquiring
 */

public class TagType {
  public static final int NOTFOUND = 0;

  public static final int BIT = 1;
  public static final int INT8 = 2;
  public static final int INT16 = 3;
  public static final int INT32 = 4;
  public static final int INT64 = 5;

  public static final int FLOAT32 = 6;
  public static final int FLOAT64 = 7;

  public static final int CHAR8 = 8;
  public static final int CHAR16 = 9;
  public static final int FUNCTION = 10;

  public static final int ARRAY = 0x0100;
  public static final int UNSIGNED = 0x0200;
}

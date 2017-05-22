package jfcontrols.tags;

/** Tag
 *
 * @author pquiring
 */

public class Tag {
  private String name;
  private int type;
  private String value;  //cached value

  public Tag() {
    value = "0";
  }

  public String getName() {
    return name;
  }

  public boolean getBoolean() {
    return !value.equals("0");
  }

  public void setBoolean(boolean value) {
    this.value = value ? "1" : "0";
  }

  public int getInt() {
    return Integer.valueOf(value);
  }

  public void setInt(int value) {
    this.value = Integer.toString(value);
  }
}

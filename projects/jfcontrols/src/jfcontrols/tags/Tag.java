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

  public boolean isset() {
    return !value.equals("0");
  }
}

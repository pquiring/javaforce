package jfcontrols.tags;

/** Tag Value for arrays
 *
 * @author pquiring
 */

public class TagValue {
  public TagValue(TagID id) {
    this.id = id;
  }
  public TagID id;
  public boolean dirty;
  public String value;
  public String oldValue;
  public boolean insert;
}

package jfcontrols.tags;

/** Array Tag Base
 *
 * @author pquiring
 */

public interface TagBaseArray {
  public abstract String getValue(TagAddr ta);
  public abstract void setValue(TagAddr ta, String value);
}

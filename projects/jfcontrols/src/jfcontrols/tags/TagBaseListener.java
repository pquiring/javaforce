package jfcontrols.tags;

/** Tag Listener
 *
 * @author pquiring
 */

public interface TagBaseListener {
  /** Invoked when a tag's value has changed. */
  public void tagChanged(TagBase tag, TagID id, String oldValue, String newValue);
}

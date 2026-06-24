package javaforce.controls;

/** Tag Listener interface.
 *
 * @author pquiring
 */

public interface TagListener {
  /** Invoked when a tag's value has changed. */
  public void tagChanged(Tag tag, String value);
}

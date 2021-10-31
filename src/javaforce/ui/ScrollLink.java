package javaforce.ui;

/** Scroll Link
 *
 * Links a Component that semi scrolls with a Scroll Box with scroll bars.
 *
 * @author pquiring
 */

public interface ScrollLink {
  public int getClientX();
  public void setClientX(int value);
  public int getClientY();
  public void setClientY(int value);
  public void setLink(ScrollBox box);
}

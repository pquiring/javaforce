package javaforce.webui;

/** Scroll Panel
 *
 * @author pquiring
 */

public class ScrollPanel extends Panel {
  private boolean scroll = true;
  public ScrollPanel() {
    setFlexDirection(NONE);
    setOverflow(AUTO);
    addClass("flex");
  }
  /** Set scroll state. */
  public void setScroll(boolean scroll) {
    if (this.scroll == scroll) return;
    if (scroll) {
      setOverflow(AUTO);
    } else {
      setOverflow(HIDDEN);
    }
    this.scroll = scroll;
  }
}

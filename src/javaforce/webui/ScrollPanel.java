package javaforce.webui;

/** Scroll Panel
 *
 * The height MUST be specified in order for scroll bars to appear.
 *
 * @author pquiring
 */

public class ScrollPanel extends Panel {
  public ScrollPanel() {
    setClass("block");
    setOverflow(AUTO);
    setResizeChild(false);
  }
}

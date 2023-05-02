package javaforce.webui;

/** Scroll Panel
 *
 * @author pquiring
 */

public class ScrollPanel extends Panel {
  public ScrollPanel() {
    setClass("block");
    setOverflow(AUTO);
    setResizeChild(false);
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<div class='scrollpanel' style='overflow: auto;'>");
    sb.append("<div class='scrollpanelcell' style='overflow: auto;'>");
    sb.append(super.html());
    sb.append("</div>");
    sb.append("</div>");
    return sb.toString();
  }
}

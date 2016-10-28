package javaforce.webui;

/** Popup Panel (or Window)
 *
 * @author pquiring
 */

public class PopupPanel extends Container {
  private TitleBar titleBar;
  public PopupPanel(String title) {
    titleBar = new TitleBar(title, this);
    add(titleBar);
    setClass("popuppanel");
    display = "inline-flex";
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + "'>");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}

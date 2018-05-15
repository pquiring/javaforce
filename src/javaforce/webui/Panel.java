package javaforce.webui;

/** Panel to display components.
 *
 * @author pquiring
 */

public class Panel extends Container {
  public Panel() {
    addClass("panel");
    addClass("column");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("<div class='column-end'></div>");
    sb.append("</div>");
    return sb.toString();
  }
  public void onLoaded(String args[]) {
    super.onLoaded(args);
    if (parent == null) invokeOnLoaded(this);
  }
  private static void invokeOnLoaded(Container c) {
    int cnt = c.count();
    for(int a=0;a<cnt;a++) {
      Component c2 = c.get(a);
      c2.onLoaded(null);
      if (c2 instanceof Container) {
        invokeOnLoaded((Container)c2);
      }
    }
  }
}

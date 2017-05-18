package javaforce.webui;

/** Centered Panel to display components.
 *
 * @author pquiring
 */

public class CenteredPanel extends Panel {
  public CenteredPanel() {
    addClass("panel");
    addClass("column");
    addClass("height100");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + "'>");
    sb.append("<div class='pad'></div>");
    sb.append("<div class='row'>");
    sb.append("<div class='pad'></div>");
    sb.append("<div class='column'>");  //content
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");  //content
    sb.append("<div class='pad'></div>");
    sb.append("</div>");  //row
    sb.append("<div class='pad'></div>");
    sb.append("</div>");  //panel
    return sb.toString();
  }
}

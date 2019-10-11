package javaforce.webui;

/** Inner Panel to display components with a border.
 *
 * @author pquiring
 */

public class InnerPanel extends Container {
  public String header;
  public InnerPanel(String header) {
    this.header = header;
    addClass("innerpanel");
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<fieldset" + getAttrs() + "'>");
    if (header != null && header.length() > 0) {
      sb.append("<legend>" + header + "</legend>");
    }
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</fieldset>");
    return sb.toString();
  }
}

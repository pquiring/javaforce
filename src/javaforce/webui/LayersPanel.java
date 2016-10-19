package javaforce.webui;

/** Layers Panel.
 *
 * All added Components will overlap.
 *
 * @author pquiring
 */

public class LayersPanel extends Container {
  public LayersPanel() {
    addClass("layersPanel");
    addClass("pad");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
  public void add(Component c) {
    super.add(c);
    c.addClass("layer");
  }
}

package javaforce.webui;

/** Inner Panel to display components with a border.
 *
 * @author pquiring
 */

public class InnerPanel extends Panel {
  private static class Legend extends Component {
    private String text;
    public Legend(String text) {
      this.text = text;
    }
    public String html() {
      StringBuilder html = new StringBuilder();
      html.append("<legend");
      html.append(getAttrs());
      html.append(">");
      html.append(text);
      html.append("</legend>");
      return html.toString();
    }
  }
  private Legend legend;
  public InnerPanel(String text) {
    legend = new Legend(text);
    removeClass("panel");
    addClass("innerpanel");
  }
  public String html() {
    StringBuilder html = new StringBuilder();
    html.append("<fieldset" + getAttrs() + "'>");
    html.append(legend.html());
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      html.append(get(a).html());
    }
    html.append("</fieldset>");
    return html.toString();
  }
  public void setAlign(int value) {
    super.setAlign(value);
    legend.setAlign(value);
  }
}

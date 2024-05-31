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
      StringBuilder sb = new StringBuilder();
      sb.append("<legend");
      sb.append(getAttrs());
      sb.append(">");
      sb.append(text);
      sb.append("</legend>");
      return sb.toString();
    }
  }
  private Legend legend;
  public InnerPanel(String text) {
    legend = new Legend(text);
    removeClass("panel");
    addClass("innerpanel");
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<fieldset" + getAttrs() + "'>");
    sb.append(legend.html());
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</fieldset>");
    return sb.toString();
  }
  public void setAlign(int value) {
    super.setAlign(value);
    legend.setAlign(value);
  }
}

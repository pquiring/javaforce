package javaforce.webui;

/** FlexBox.
 *
 * Fills up space or gap between other components like a spring.
 *
 * @author pquiring
 */

public class FlexBox extends Component {

  public FlexBox() {
    addClass("flex");
    addClass("inlineblock");
  }

  public String html() {
    StringBuilder html = new StringBuilder();
    html.append("<div" + getAttrs() + ">");
    html.append("</div>");
    return html.toString();
  }
}

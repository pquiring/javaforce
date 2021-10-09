package javaforce.webui;

/** FlexBox
 *
 * Fills up space or gap between other components like a spring.
 *
 * @author pquiring
 */

public class FlexBox extends Component {

  public FlexBox() {
    addClass("flexitem");
  }

  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<div" + getAttrs() + ">");
    sb.append("</div>");
    return sb.toString();
  }
}

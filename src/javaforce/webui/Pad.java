package javaforce.webui;

/** Pad - to add padding
 *
 * @author pquiring
 */

public class Pad extends Component {
  public Pad() {
    addClass("pad");
  }
  public String html() {
    return "<div" + getAttrs() + "></div>";
  }
}

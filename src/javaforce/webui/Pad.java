package javaforce.webui;

/** Pad - to add padding
 *
 * @author pquiring
 */

public class Pad extends Component {
  public String html() {
    return "<div id='" + name + "' class='pad'></div>";
  }
}

package javaforce.webui;

/** IFrame to display another webpage.
 *
 * @author pquiring
 */

public class IFrame extends Component {
  private String url;

  public IFrame(String url) {
    this.url = url;
  }

  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<iframe" + getAttrs() + " src='" + url + "'>");
    sb.append("</iframe>");
    return sb.toString();
  }
}

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
    StringBuilder html = new StringBuilder();
    html.append("<iframe" + getAttrs() + " src='" + url + "'>");
    html.append("</iframe>");
    return html.toString();
  }
}

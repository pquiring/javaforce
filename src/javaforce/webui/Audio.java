package javaforce.webui;

/** Audio
 *
 * @author pquiring
 */

public class Audio extends Component {
  private String src;
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<audio");
    sb.append(getAttrs());
    if (width != 0) {
      sb.append(" width='" + width + "'");
    }
    if (height != 0) {
      sb.append(" height='" + height + "'");
    }
    if (src != null) {
      sb.append(" src='" + src + "'");
    }
    sb.append("></audio>");
    return sb.toString();
  }
  public void setSource(String url) {
    src = url;
  }
}

package javaforce.webui;

/** Video
 *
 * @author pquiring
 */

public class Video extends Component {
  private String src;
  public Video() {
    addEvent("onplay", "console.log('Video.play:readyState=' + this.readyState);");
    addEvent("onstop", "console.log('Video.stop:readyState=' + this.readyState);");
    addEvent("onpause", "console.log('Video.paused:readyState=' + this.readyState);");
    addEvent("onseeking", "console.log('Video.seeking:readyState=' + this.readyState);");
    addEvent("onstaled", "console.log('Video.staled:readyState=' + this.readyState);");
    addEvent("onsuspend", "console.log('Video.suspend:readyState=' + this.readyState);");
    addEvent("onwaiting", "console.log('Video.waiting:readyState=' + this.readyState);");
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<video");
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
    sb.append(" controls autplay></video>");
    return sb.toString();
  }
  public void setSource(String url) {
    src = url;
  }
}

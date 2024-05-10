package javaforce.webui;

/** Image.
 *
 * @author pquiring
 */

public class Image extends Component {
  private Resource img;
  private String api;
  private int count;

  public Image(Resource img) {
    this.img = img;
  }

  public Image(String api_url) {
    this.api = api_url;
  }

  private String getsrc() {
    if (img != null) {
      return "/static/" + img.id;
    }
    if (api != null) {
      return "/api/" + api;
    }
    return "/user/" + getClient().hash + "/" + id + "/" + count;
  }

  public String html() {
    return "<img" + getAttrs() +  " src='" + getsrc() + "'>";
  }

  public void setImage(Resource img) {
    this.img = img;
    sendEvent("setsrc", new String[] {"src=" + getsrc()});
  }

  public void setImage(String api_url) {
    this.api = api_url;
    sendEvent("setsrc", new String[] {"src=" + getsrc()});
  }

  public void refresh() {
    count++;
    sendEvent("setsrc", new String[] {"src=" + getsrc()});
  }
}

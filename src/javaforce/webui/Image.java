package javaforce.webui;

/** Image.
 *
 * @author pquiring
 */

public class Image extends Component {
  private Resource img;
  private int count;

  public Image(Resource img) {
    this.img = img;
  }

  private String getsrc() {
    if (img == null)
      return "/user/" + getClient().hash + "/" + id + "/" + count;
    else
      return "/static/" + img.id;
  }

  public String html() {
    return "<img" + getAttrs() +  " src='" + getsrc() + "'>";
  }

  public void setImage(Resource img) {
    this.img = img;
    sendEvent("setsrc", new String[] {"src=" + getsrc()});
  }

  public void refresh() {
    count++;
    sendEvent("setsrc", new String[] {"src=" + getsrc()});
  }
}

package javaforce.webui;

/** Image.
 *
 * @author pquiring
 */

public class Image extends Component {
  private Resource img;
  public Image(Resource img) {
    this.img = img;
  }

  public String html() {
    return "<img" + getAttrs() +  " src='/static/" + img.id + "'>";
  }

  public void setImage(Resource img) {
    this.img = img;
    sendEvent("setsrc", new String[] {"src=/static/" + img.id});
  }
}

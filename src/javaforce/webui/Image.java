package javaforce.webui;

/** Image.
 *
 * @author pquiring
 */

public class Image extends Component {
  public Resource img;
  public Image(Resource img) {
    this.img = img;
  }

  public String html() {
    return "<img src='/static/" + img.id + "'>";
  }

  public void setImage(Resource img) {
    this.img = img;
    peer.sendEvent(id, "setsrc", new String[] {"src=/static/" + img.id});
  }
}

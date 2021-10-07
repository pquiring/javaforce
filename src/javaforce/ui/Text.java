package javaforce.ui;

/** Text rendered directly.
 *
 * @author pquiring
 */

public class Text extends Component {
  private Font font;

  public void render() {
    font.getTexture().bind();
  }
}

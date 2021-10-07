package javaforce.ui;

/** Canvas - allows drawing custom OpenGL objects.
 *
 * @author pquiring
 */

import javaforce.gl.*;

public class Canvas extends Component {
  public final void render(Image image) {
    //register canvas to render after Window
    GLWindow.registerCanvas(this);
  }
  public void render() {

  }
}

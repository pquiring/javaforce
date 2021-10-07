package javaforce.ui;

/** TestUI
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.gl.*;

public class TestUI implements GLWindow.WindowEvents {
  public static void main(String[] args) {
    GLWindow.init();
    UIRender render = new UIRender();
    GLWindow window = new GLWindow();
    window.create(GLWindow.STYLE_TITLEBAR | GLWindow.STYLE_RESIZABLE, "TestUI", 512, 512, window);
    window.show();
    window.setWindowListener(new TestUI());
    GL.glInit();
    Label label = new Label("TestUI");
    window.setContent(label);
    render.run();
  }

  public void windowResize(int x, int y) {
    System.out.println("resize:" + x + "," + y);
  }

  public void windowClosing() {
    System.exit(0);
  }
}

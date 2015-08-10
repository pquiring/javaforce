/**
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.gl.*;

public class Main implements GLWindow.WindowEvents {
  public static GLWindow gl;
  public static GLCode code;
  public static void main(String args[]) {
    GLWindow.init();
    gl = new GLWindow();
    gl.create(GLWindow.STYLE_VISIBLE | GLWindow.STYLE_TITLEBAR | GLWindow.STYLE_RESIZABLE, "Test", 512, 512, null);
    gl.show();
    gl.setWindowListener(new Main());
    GL.glInit();  //must call AFTER window is created
    code = new GLCode(true);
    code.init();
    while (true) {
      code.render();
      gl.pollEvents();
    }
  }

  public static void swap() {
    gl.swap();
  }

  public void windowResize(int x, int y) {
    code.resize(x, y);
  }

  public void windowClosing() {
    System.exit(0);
  }
}

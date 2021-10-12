/**
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.ui.*;
import javaforce.gl.*;

public class Main implements WindowEvents {
  public static Window gl;
  public static GLCode code;
  public static void main(String args[]) {
    Window.init();
    gl = new Window();
    gl.create(Window.STYLE_VISIBLE | Window.STYLE_TITLEBAR | Window.STYLE_RESIZABLE, "Test", 512, 512, null);
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

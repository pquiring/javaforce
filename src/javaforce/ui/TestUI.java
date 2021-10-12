package javaforce.ui;

/** TestUI
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.gl.*;

public class TestUI implements WindowEvents {
  public static void main(String[] args) {
    Window.init();
    UIRender render = new UIRender();
    Window window = new Window();
    window.create(Window.STYLE_TITLEBAR | Window.STYLE_RESIZABLE, "TestUI", 512, 512, window);
    window.show();
    window.setWindowListener(new TestUI());
    GL.glInit();
    window.setContent(createUI());
    render.run();
  }

  public static Component createUI() {
    Column c = new Column();
    Row r = new Row();
    r.add(new Label("TopLeft"));
    r.add(new FlexBox());
    r.add(new Label("TopRight"));
    c.add(r);
    c.add(new FlexBox());
    r = new Row();
    r.add(new Label("BottomLeft"));
    r.add(new FlexBox());
    r.add(new Label("BottomRight"));
    c.add(r);
    return c;
  }

  public void windowResize(int x, int y) {
    System.out.println("resize:" + x + "," + y);
  }

  public void windowClosing() {
    System.exit(0);
  }
}

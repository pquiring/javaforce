package javaforce.ui;

/** TestUI
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.gl.*;
import javaforce.ui.theme.Theme;

public class TestUI implements WindowEvents {
  public static UIRender render;
  public static Window window;
  public static void main(String[] args) {
    Window.init();
    render = new UIRender();
    window = new Window();
    window.create(Window.STYLE_TITLEBAR | Window.STYLE_RESIZABLE, "TestUI", 512, 512, window);
    window.show();
    window.setWindowListener(new TestUI());
    GL.glInit();
    window.setContent(createUI());
    render.run();
  }

  public static Component createUI() {
    Theme.getTheme().setForeColor(Color.GREEN);
    Column c = new Column();
    Row r = new Row();
    r.add(new Label("TopLeft"));
    Button b = new Button("Button");
    b.setActionListner(new ActionListener() {
      public void actionPerformed(Component cmp) {
        b.setText("OK");
        window.layout();
      }
    });
    r.add(b);
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
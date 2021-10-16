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
    Button b1 = new Button("Button1");
    b1.setActionListner(new ActionListener() {
      public void actionPerformed(Component cmp) {
        b1.setText("OK1");
        window.layout();
      }
    });
    r.add(b1);
    Button b2 = new Button("Button2");
    b2.setEnabled(false);
    b2.setActionListner(new ActionListener() {
      public void actionPerformed(Component cmp) {
        b2.setText("OK2");
        window.layout();
      }
    });
    r.add(b2);
    ToggleButton b3 = new ToggleButton("ToggleButton");
    r.add(b3);
    r.add(new FlexBox());
    r.add(new Label("TopRight"));
    c.add(r);

    r = new Row();
    CheckBox b4 = new CheckBox("CheckBox");
    r.add(b4);
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

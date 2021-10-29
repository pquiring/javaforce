package javaforce.ui;

/** TestUI
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.gl.*;
import javaforce.ui.theme.*;

public class TestUI implements WindowEvents {
  public static UIRender render;
  public static Window window;
  public static void main(String[] args) {
    Window.init();
    render = new UIRender();
    window = new Window();
    window.create(Window.STYLE_TITLEBAR | Window.STYLE_RESIZABLE, "TestUI", 1024, 512, window);
    window.show();
    window.setWindowListener(new TestUI());
    GL.glInit();
    window.setContent(createUI());
    window.setScale(2);
    render.run();
  }

  public static Component createUI() {
    Theme.getTheme().setForeColor(Color.GREEN);

    Column c = new Column();

    Row r = new Row();
    c.add(r);

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

    r = new Row();
    c.add(r);

    CheckBox b4 = new CheckBox("CheckBox");
    r.add(b4);

    ListBox list = new ListBox();
    list.addItem("Item 1");
    list.addItem("Item 2");
    list.addItem("Item 3");
    r.add(list);

    ListBox list2 = new ListBox();
    list2.addItem("Item 1");
    list2.addItem("Item 2");
    list2.addItem("Item 3");
    list2.addItem("Item 4");
    ScrollBox scroll = new ScrollBox(list2, Direction.VERTICAL);
    scroll.setSize(list2.getMinWidth() + 16, list2.getItemHeight() * 2);
    scroll.setStepsize(list2.getItemHeight());
    r.add(scroll);

    TextField tf = new TextField("Test");
    tf.setSize(100, tf.getMinHeight());
    r.add(tf);

    TextField ptf = new TextField("");
    ptf.setPassword(true);
    ptf.setSize(100, ptf.getMinHeight());
    r.add(ptf);

    r = new Row();
    c.add(r);

    TextBox tb = new TextBox("Test123\nTest456");
    tb.setSize(100, 100);
    r.add(tb);

    ComboBox cb = new ComboBox("");
    cb.addItem("Item 1");
    cb.addItem("Item 2");
    cb.addItem("Item 3");
    cb.setForeColor(Color.RED);
    cb.setSize(100, 16);
    cb.setEditable(true);
    r.add(cb);

    c.add(new FlexBox());

    r = new Row();
    c.add(r);

    r.add(new Label("BottomLeft"));
    r.add(new FlexBox());
    r.add(new Label("BottomRight"));

    return c;
  }

  public void windowResize(int x, int y) {
    System.out.println("resize:" + x + "," + y);
  }

  public void windowClosing() {
    System.exit(0);
  }
}

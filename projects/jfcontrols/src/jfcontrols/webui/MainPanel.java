package jfcontrols.webui;

/** MainPanel
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class MainPanel extends Panel {
  public MainPanel() {
    Column c = new Column();
    c.setHeight("100%");
    c.add(new Pad());
    Row r = new Row();
    r.add(new Pad());
    r.add(new LoginPanel());
    r.add(new Pad());
    c.add(r);
    c.add(new Pad());
    add(c);
  }
}

package jfcontrols.webui;

/** MainMenu
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class MainMenu extends Panel {
  public MainMenu() {
    Row row = new Row();
    row.setFlex(true);
    add(row);
    Column col = new Column();
    col.setFlex(true);
    row.add(col);
    panels = new Button("Panels");
    panels.setFontSize(56);
    col.add(panels);
    logic = new Button("Logic");
    logic.setFontSize(56);
    col.add(logic);
    col = new Column();
    col.setFlex(true);
    row.add(col);
    config = new Button("Config");
    config.setFontSize(56);
    col.add(config);
    logout = new Button("Logout");
    logout.setFontSize(56);
    col.add(logout);
    exit = new Button("Shutdown");  //TODO : testing only! remove before going live!
    exit.setFontSize(56);
    col.add(exit);
    logout.addClickListener((me, c) -> {
      c.getClient().redirect(new MainPanel());
    });
    exit.addClickListener((me, c) -> {
      System.exit(0);
    });
  }
  Button panels, logic;
  Button config, logout;
  Button exit;
}

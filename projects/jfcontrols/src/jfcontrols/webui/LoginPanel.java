package jfcontrols.webui;

/** LoginPanel
 *
 * @author pquiring
 */

import javaforce.webui.*;
import javaforce.webui.event.*;

public class LoginPanel extends InnerPanel {
  public LoginPanel() {
    super("Login");
    Row row = new Row();
    add(row);
    Column col = new Column();
    row.add(col);

    Label label = new Label("Username:");
    label.setFontSize(24);
    col.add(label);
    label = new Label("Password:");
    label.setFontSize(24);
    col.add(label);

    col = new Column();
    row.add(col);

    username = new TextField("");
    username.setFontSize(24);
    col.add(username);
    password = new TextField("");
    password.setFontSize(24);
    col.add(password);

    row = new Row();
    add(row);
    login = new Button("Login");
    login.setFontSize(24);
    login.setFlex(true);
    row.add(login);

    row = new Row();
    add(row);

    msg = new Label("");
    msg.setFontSize(24);
    row.add(msg);

    login.addClickListener((MouseEvent me, Component c) -> {
      if (login()) {
        c.getClient().redirect(new MainMenu());
      } else {
        if (cnt > 0) {
          msg.setText("Access denied (" + cnt + ")");
        } else {
          msg.setText("Access denied");
        }
        cnt++;
      }
    });
  }
  public boolean login() {
    return true;
  }
  public TextField username;
  public TextField password;
  public Button login;
  public Label msg;
  public int cnt;
}

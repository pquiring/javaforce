package javaforce.webui;

/** Login Panel
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class LoginPanel extends Panel {
  public static interface Login {
    public boolean login(String user, String pass);
  }
  public LoginPanel(String appName, boolean show_username, Login login) {
    this.removeClass("column");
    InnerPanel inner = new InnerPanel(appName + " Login");
    inner.setAutoWidth();
    inner.setAutoHeight();
    setAlign(Component.CENTER);
    Row row;
    Label msg = new Label("");
    msg.setColor(Color.red);
    inner.add(msg);

    TextField username = new TextField("");
    TextField password = new TextField("");
    password.setPassword(true);

    row = new Row();

    if (show_username) {
      row.add(new Label("Username:"));
      row.add(username);
    }

    row.add(new Label("Password:"));
    row.add(password);

    Button button_login = new Button("Login");
    row.add(button_login);
    inner.add(row);
    button_login.addClickListener( (MouseEvent m, Component c) -> {
      msg.setText("");
      if (!login.login(username.getText(), password.getText())) {
        msg.setText("Wrong password");
      }
    });
    add(inner);
  }
}

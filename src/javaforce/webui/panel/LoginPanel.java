package javaforce.webui.panel;

/** Login Panel
 *
 * @author pquiring
 */

import javaforce.webui.Button;
import javaforce.webui.Color;
import javaforce.webui.Component;
import javaforce.webui.GridLayout;
import javaforce.webui.InnerPanel;
import javaforce.webui.Label;
import javaforce.webui.Panel;
import javaforce.webui.TextField;
import javaforce.webui.event.*;
import static javaforce.webui.event.KeyEvent.*;

public class LoginPanel extends Panel {
  public static interface Login {
    public boolean login(String user, String pass);
  }
  public LoginPanel(String appName, boolean show_username, Login login) {
    this.removeClass("column");
    InnerPanel inner = new InnerPanel(appName + " Login");
    inner.setMaxWidth();
    inner.setMaxHeight();
    setAlign(Component.CENTER);
    Label msg = new Label("");
    msg.setColor(Color.red);
    inner.add(msg);

    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    grid.setAlign(CENTER);
    inner.add(grid);

    TextField username = new TextField("");
    if (show_username) {
      grid.addRow(new Component[] {new Label("Username"), username});
    }

    TextField password = new TextField("");
    password.setPassword(true);
    grid.addRow(new Component[] {new Label("Password"), password});

    Button button_login = new Button("Login");
    inner.add(button_login);

    username.addKeyDownListener((ke, cmp) -> {
      if (ke.keyCode == VK_ENTER) {
        password.setFocus();
      }
    });

    password.addKeyDownListener((ke, cmp) -> {
      if (ke.keyCode == VK_ENTER) {
        button_login.click();
      }
    });

    button_login.addClickListener( (MouseEvent m, Component c) -> {
      msg.setText("");
      if (!login.login(username.getText(), password.getText())) {
        msg.setText("Wrong password");
      }
    });
    add(inner);
  }
}

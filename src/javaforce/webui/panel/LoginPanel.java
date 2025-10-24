package javaforce.webui.panel;

/** Login Panel
 *
 * @author pquiring
 */

import javaforce.access.*;
import javaforce.webui.*;
import javaforce.webui.event.*;
import static javaforce.webui.event.KeyEvent.*;

public class LoginPanel extends Panel {
  /** Callback interface to validate provided password. */
  public static interface Login {
    /** Validates user/pass credentials.
     * @return access granted
     */
    public boolean login(String user, String pass);
  }

  /** Creates new LoginPanel.
   *
   * @param appName = application name to display
   * @param login = interface to check password
   */
  public LoginPanel(String appName, boolean show_username, Login login, WebUIClient client) {
    init(appName, show_username, login, client);
  }

  /** Creates new LoginPanel.
   *
   * Authorization will use AccessControl
   *
   * @param appName = application name to display
   */
  public LoginPanel(String appName, WebUIClient client) {
    init(appName, true, null, client);
  }

  private void init(String appName, boolean show_username, Login login, WebUIClient client) {
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
      String user = username.getText();
      String pass = password.getText();
      boolean success = false;
      if (login == null) {
        //use WebUIClient
        success = client.getAccessControl().login(user, pass);
      } else {
        success = login.login(user, pass);
      }
      if (success) {
        client.setProperty("user", user);
        client.setProperty("groups", client.getAccessControl().getGroups(user));
        client.refresh();
      } else {
        msg.setText("Wrong password");
      }
    });
    add(inner);
  }
}

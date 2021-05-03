package jfnetboot;

/**
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class LoginPanel extends Panel {
  public LoginPanel() {
    this.removeClass("column");
    InnerPanel inner = new InnerPanel("jfNetBoot Login");
    inner.setDisplay("inline");
    inner.setAutoWidth();
    inner.setAutoHeight();
    this.setAlign(Component.CENTER);
    Row row;
    Label msg = new Label("");
    inner.add(msg);
    row = new Row();
    row.add(new Label("Password:"));
    TextField password = new TextField("");
    row.add(password);
    Button login = new Button("Login");
    row.add(login);
    inner.add(row);
    login.addClickListener( (MouseEvent m, Component c) -> {
      String passTxt = password.getText();
      WebUIClient webclient = c.getClient();
      String passEncoded = Settings.encodePassword(passTxt);
      if (passEncoded.equals(Settings.current.password)) {
        webclient.setPanel(new ConfigPanel());
      } else {
        msg.setText("Wrong password");
        msg.setColor(Color.red);
      }
    });
    this.add(inner);
  }
}

package javaforce.service;

import java.io.*;

import javaforce.*;
import javaforce.bus.*;
import javaforce.webui.*;
import javaforce.webui.panel.*;

/** Common Config Servlet.
 *
 * Service must implement IPC methods:
 *   String getConfig()
 *   boolean setConfig(String cfg)
 *   String getLogFile()
 *
 * @author pquiring
 */

public abstract class ConfigServlet implements WebUIServlet {

  private String appName;
  private JBusClient busClient;
  private String busName;

  public abstract String getAppName();
  public abstract String getBusName();

  private class UI {
    TextArea view_log_textarea;
  }

  public void init() {
    try {
      appName = getAppName();
      busName = getBusName();
      JFLog.init(JF.getLogPath() + "/" + appName + "-servlet.log", true);
      busClient = new JBusClient(null);
      busClient.connect();
    } catch (Throwable t) {
      JFLog.log(t);
    }
  }

  public void destroy() {
  }

  public String getName() {
    return appName;
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    String user = client.getUser();
    if (user == null) {
      return new LoginPanel(appName, client);
    }
    Panel panel = new Panel();

    UI ui = new UI();

    Panel viewLogPanel = getViewLogPanel(ui);
    panel.add(viewLogPanel);

    ToolBar tools = new ToolBar();
    panel.add(tools);

    Label title = new Label(appName + " Configuration");
    tools.add(title);

    Button load = new Button("Load Config");
    tools.add(load);

    Button save = new Button("Save Config");
    tools.add(save);

    Button view_log = new Button("View Log");
    tools.add(view_log);

    TextArea config = new TextArea("");
    config.setMaxWidth();
    config.setFlex(true);
    panel.add(config);

    view_log.addClickListener((me, cmp) -> {
      viewLogRefresh(ui);
      viewLogPanel.setVisible(true);
    });

    load.addClickListener((me, cmp) -> {
      String cfg = (String)busClient.invoke(busName, "getConfig");
      config.setText(cfg);
    });

    save.addClickListener((me, cmp) -> {
      String cfg = (String)config.getText();
      if (cfg.length() == 0) return;
      busClient.invoke(busName, "setConfig", cfg);
    });

    return panel;
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebRequest request, WebResponse response) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }

  private void viewLogRefresh(UI ui) {
    String logfile = (String)busClient.invoke(busName, "getLogFile");
    String log = "";
    try {
      FileInputStream fis = new FileInputStream(logfile);
      byte[] data = fis.readAllBytes();
      fis.close();
      log = new String(data);
    } catch (Exception e) {
      log = e.toString();
    }
    ui.view_log_textarea.setText(log);
  }

  private Panel getViewLogPanel(UI ui) {
    Panel panel = new PopupPanel("View Log");

    TextArea text = new TextArea("");
    text.setWidth(1024);
    text.setHeight(512);
    ui.view_log_textarea = text;
    panel.add(ui.view_log_textarea);

    ToolBar tools = new ToolBar();
    panel.add(tools);

    Button refresh = new Button("Refresh");
    tools.add(refresh);

    refresh.addClickListener((me, cmp) -> {
      viewLogRefresh(ui);
    });

    return panel;
  }
}

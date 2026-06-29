package service;

/** Config Service : jf Admin
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.access.*;
import javaforce.io.*;
import javaforce.service.*;
import javaforce.webui.*;
import javaforce.webui.event.*;
import javaforce.webui.panel.*;
import javaforce.webui.tasks.*;

public class ConfigService implements WebUIHandler {
  public static String version = "0.2";
  public static String appname = "jfAdmin";
  public static boolean debug = false;
  public static boolean debug_api = false;
  public static boolean show_tasklog = false;  //TODO : share TaskLog with Servlets
  public WebUIServer server;
  public AccessControl access;
  private KeyMgmt keys;

  public void start() {
    initSecureWebKeys();
    server = new WebUIServer();
    server.start(this, 443, keys);
    server.setUploadFolder("/var/upload");
    server.setUploadLimit(-1);
    access = new AccessControl();
    access.setConfigFolder(Paths.accessPath);
    server.setAccessControl(access);
    server.startServlets();
    loadServlets();
  }

  public void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  private void initSecureWebKeys() {
    String keyfile = Paths.dataPath + "/jfadmin.key";
    String password = "password";
    KeyParams params = new KeyParams();
    params.dname = "CN=jfkvm.sourceforge.net, O=server, OU=webserver, C=CA";
    if (new File(keyfile).exists()) {
      //load existing keys
      keys = new KeyMgmt();
      try {
        FileInputStream fis = new FileInputStream(keyfile);
        keys.open(fis, password);
        fis.close();
      } catch (Exception e) {
        if (!keys.isValid()) {
          //generate random keys
          keys = KeyMgmt.create(keyfile, password, "webserver", params, password);
        }
        JFLog.log(e);
      }
    } else {
      //generate random keys
      keys = KeyMgmt.create(keyfile, password, "webserver", params, password);
    }
  }

  /** This class holds UI elements to be passed down to sub-panels. */
  private static class UI {
    public WebUIClient client;
    public String user;

    public SplitPanel top_bottom_split;
    public SplitPanel left_right_split;
    public Panel tasks;
    public Panel right_panel;

    public PopupPanel message_popup;
    public Label message_message;

    public PopupPanel confirm_popup;
    public Label confirm_message;
    public Button confirm_button;
    public Runnable confirm_action;

    public void setRightPanel(Panel panel) {
      if (panel == null) return;
      right_panel = panel;
      left_right_split.setRightComponent(panel);
    }
  }

  //WebUIHandler interface

  public void clientConnected(WebUIClient client) {
    client.setProperty("ui", new UI());
    client.setAccessControl(access);
  }

  public void clientDisconnected(WebUIClient client) {
    UI ui = (UI)client.getProperty("ui");
    if (ui == null) return;
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebRequest request, WebResponse res) {
    return null;
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    String user = (String)client.getProperty("user");
    if (user == null) {
      return loginPanel(client);
    }
    Panel panel = new Panel();
    UI ui = (UI)client.getProperty("ui");
    ui.client = client;
    ui.user = user;

    //add AccessControl related popup panels
    client.addPopupPanels(panel);

    ui.message_popup = messagePopupPanel(ui);
    panel.add(ui.message_popup);

    ui.confirm_popup = confirmPopupPanel(ui);
    panel.add(ui.confirm_popup);

    int bottomSize = 128;
    ui.top_bottom_split = new SplitPanel(SplitPanel.HORIZONTAL, SplitPanel.TOP);
    panel.add(ui.top_bottom_split);
    ui.top_bottom_split.setDividerPosition(bottomSize);

    int leftSize = 96;
    ui.left_right_split = new SplitPanel(SplitPanel.VERTICAL);
    ui.left_right_split.setDividerPosition(leftSize);
    ui.left_right_split.setLeftComponent(leftPanel(ui, leftSize));
    ui.left_right_split.setRightComponent(configPanel(ui));

    Panel tasks = tasksPanel(ui);

    ui.top_bottom_split.setTopComponent(ui.left_right_split);
    ui.top_bottom_split.setBottomComponent(tasks);

    if (show_tasklog) {
      return panel;
    } else {
      return ui.left_right_split;
    }
  }

  private Panel tasksPanel(UI ui) {
    Panel panel = new Panel();
    panel.add(new Label("Tasks"));

    Panel tasks = new Panel();
    panel.add(tasks);

    ui.tasks = tasks;

    return panel;
  }

  private PopupPanel messagePopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Message");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label popup_msg = new Label("Message");
    row.add(popup_msg);

    row = new Row();
    panel.add(row);
    Button popup_b_action = new Button("Okay");
    row.add(popup_b_action);

    popup_b_action.addClickListener((MouseEvent e, Component button) -> {
      ui.message_popup.setVisible(false);
    });
    ui.message_message = popup_msg;
    panel.setOnClose( () -> {
      popup_b_action.click();
    });
    return panel;
  }

  private PopupPanel confirmPopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Confirm");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label popup_msg = new Label("Message");
    row.add(popup_msg);

    row = new Row();
    panel.add(row);
    Label popup_label = new Label("Are you sure?");
    row.add(popup_label);

    row = new Row();
    panel.add(row);
    Button popup_b_action = new Button("Action");
    row.add(popup_b_action);
    Button popup_b_cancel = new Button("Cancel");
    row.add(popup_b_cancel);

    popup_b_action.addClickListener((MouseEvent e, Component button) -> {
      if (ui.confirm_action != null) {
        ui.confirm_action.run();
      }
      ui.confirm_popup.setVisible(false);
    });
    popup_b_cancel.addClickListener((MouseEvent e, Component button) -> {
      panel.setVisible(false);
    });
    ui.confirm_message = popup_msg;
    ui.confirm_button = popup_b_action;
    panel.setOnClose( () -> {
      popup_b_cancel.click();
    });
    return panel;
  }

  private Panel loginPanel(WebUIClient client) {
    return new LoginPanel(appname, client);
  }

  private Panel leftPanel(UI ui, int size) {
    Panel panel = new Panel();
    ListBox list = new ListBox();
    panel.add(list);
    Button config = new Button("System");
    config.setWidth(size);
    list.add(config);
    //add WebUIServlets
    addServlets(list, ui, size);
    Button tasks_log = new Button("Tasks Log");
    tasks_log.setWidth(size);
    if (show_tasklog) {
      list.add(tasks_log);
    }

    config.addClickListener((me, cmp) -> {
      ui.setRightPanel(configPanel(ui));
    });
    tasks_log.addClickListener((me, cmp) -> {
      ui.setRightPanel(new TaskLogUI(Tasks.tasks.getTaskLog()));
    });
    return panel;
  }

  private void addServlets(ListBox list, UI ui, int size) {
    WebUIServletContext[] servlets = server.getServlets();
    for(WebUIServletContext servlet : servlets) {
      Button button = new Button(servlet.getName());
      button.setWidth(size);
      list.add(button);
      button.addClickListener((me, cmp) -> {
        ui.setRightPanel(new IFramePanel("https://" + ui.client.getHost() + "/" + servlet.getName()));
      });
    }
  }

  private void loadServlets() {
    File[] files = new File(Paths.adminPath).listFiles();
    if (files == null) return;
    for(File file : files) {
      String name = file.getName();
      if (!name.endsWith(".cfg")) continue;
      loadServlet(file.getAbsolutePath());
    }
  }

  private void loadServlet(String file) {
    Properties cfg = new Properties();
    try {
      FileInputStream fis = new FileInputStream(file);
      cfg.load(fis);
      fis.close();
      String classpath = cfg.getProperty("CLASSPATH");
      if (classpath == null) {
        JFLog.log("Servlet missing CLASSPATH:" + file);
        return;
      }
      String cls = cfg.getProperty("WEBUISERVLET");
      if (cls == null) {
        JFLog.log("Servlet missing WEBUISERVLET:" + file);
        return;
      }
      String[] cp = classpath.split(";");
      int i1 = file.lastIndexOf(File.separatorChar);
      int i2 = file.lastIndexOf('.');
      String name = file.substring(i1 + 1, i2);
      JFLog.log("Loading Servlet:" + file);
      WebUIServletContext servlet = server.createServlet(JF.getClassFolder(name), cp, cls);
      if (servlet == null) {
        JFLog.log("Servlet failed to load:" + file);
        return;
      }
      server.addServlet(servlet);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private Panel configPanel(UI ui) {
    TabPanel panel = new TabPanel();

    panel.addTab(hostsPanel(ui), "Host");
    panel.addTab(new UsersPanel(ui.client), "Users");
    panel.addTab(new GroupsPanel(ui.client), "Groups");

    return panel;
  }

  private Panel hostsPanel(UI ui) {
    Panel panel = new Panel();

    ToolBar tools = new ToolBar();
    panel.add(tools);

    Label msg = new Label("System Settings (WIP)");
    panel.add(msg);

    return panel;
  }

  public Panel getPanelResourceNotFound() {
    Panel panel = new Panel();
    Label errmsg = new Label("Error:Resource not found");
    errmsg.setColor(Color.red);
    panel.add(errmsg);
    return panel;
  }
}

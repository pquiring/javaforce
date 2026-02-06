package service;

/** Config Service : jf Term Server
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
  public static String version = "0.5";
  public static String appname = "jfTerminalServer";
  public static boolean debug = false;
  public static boolean debug_api = false;
  public WebUIServer server;
  public AccessControl access;
  private KeyMgmt keys;
  private ArrayList<ComPort> com_sessions = new ArrayList<>();
  private Object com_lock = new Object();

  public void start() {
    initSecureWebKeys();
    server = new WebUIServer();
    server.start(this, 443, keys);
    server.setUploadFolder("/volumes");
    server.setUploadLimit(-1);
    access = new AccessControl();
    access.setConfigFolder(Paths.accessPath);
    server.setAccessControl(access);
  }

  public void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  private void initSecureWebKeys() {
    String keyfile = Paths.dataPath + "/jftermserver.key";
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
    public Calendar now;

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

    public PopupPanel browse_popup;
    public Runnable browse_init;
    public Label browse_errmsg;
    public String browse_path;
    public Button browse_button_select;
    public Button browse_button_edit;
    public String browse_file;
    public String[] browse_filters;
    public Runnable browse_complete_select;
    public Runnable browse_complete_edit;
    public Task browse_upload_task;
    public Object browse_upload_wait;

    public ComPort com;

    public void setRightPanel(Panel panel) {
      if (panel == null) return;
      right_panel = panel;
      left_right_split.setRightComponent(panel);
    }
  }

  private String getUser(WebUIClient client) {
    return (String)client.getProperty("user");
  }

  private String getIP(WebUIClient client) {
    return client.getHost();
  }

  private TaskEvent createEvent(String action, UI ui) {
    return TaskEvent.create(action, getUser(ui.client), getIP(ui.client));
  }

  private TaskEvent createEvent(String action, String user, String ip) {
    return TaskEvent.create(action, user, ip);
  }

  //WebUIHandler interface

  public void clientConnected(WebUIClient client) {
    client.setProperty("ui", new UI());
    client.setAccessControl(access);
  }

  public void clientDisconnected(WebUIClient client) {
    UI ui = (UI)client.getProperty("ui");
    if (ui == null) return;
    try {
      if (ui.com != null) {
        synchronized (com_lock) {
          com_sessions.remove(ui.com);
        }
        ui.com.close();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebRequest request, WebResponse res) {
    return null;
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    if (name.equals("terminal")) {
      return getWebTerminal(params, client);
    }
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

    int leftSize = 128;
    ui.left_right_split = new SplitPanel(SplitPanel.VERTICAL);
    ui.left_right_split = ui.left_right_split;
    ui.left_right_split.setDividerPosition(leftSize);
    ui.left_right_split.setLeftComponent(leftPanel(ui, leftSize));
    ui.left_right_split.setRightComponent(terminalPanel(ui));

    Panel tasks = tasksPanel(ui);

    ui.top_bottom_split.setTopComponent(ui.left_right_split);
    ui.top_bottom_split.setBottomComponent(tasks);

    return panel;
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
    Button terminal = new Button("Terminal");
    terminal.setWidth(size);
    list.add(terminal);
    Button config = new Button("Settings");
    config.setWidth(size);
    list.add(config);
    Button tasks_log = new Button("Tasks Log");
    tasks_log.setWidth(size);
    list.add(tasks_log);

    terminal.addClickListener((me, cmp) -> {
      ui.setRightPanel(terminalPanel(ui));
    });
    config.addClickListener((me, cmp) -> {
      ui.setRightPanel(configPanel(ui));
    });
    tasks_log.addClickListener((me, cmp) -> {
      ui.setRightPanel(new TaskLogUI(Tasks.tasks.getTaskLog()));
    });
    return panel;
  }

  private static String[] bauds = new String[] {"9600", "19200", "38400", "57600", "115200"};

  private void loadName(String port, TextField name, ComboBox sel_baud) {
    PortSettings cfg = Config.current.getPortSettings(port);
    if (cfg == null) {
      name.setText("");
      sel_baud.setSelectedIndex(0);
      return;
    }
    name.setText(cfg.name);
    int idx = 0;
    for(String baud : bauds) {
      if (baud.equals(cfg.baud)) {
        sel_baud.setSelectedIndex(idx);
        break;
      }
      idx++;
    }
  }

  private Panel terminalPanel(UI ui) {
    Panel panel = new Panel();

    panel.add(new Label(appname + "/" + version));

    InnerPanel inner = new InnerPanel("Terminal");

    panel.add(inner);

    inner.add(new Label("Ports:"));

    ComboBox sel_port = new ComboBox();
    String[] ports = ComPort.list();
    for(String port : ports) {
      sel_port.add(port, port);
    }
    inner.add(sel_port);

    Row row = new Row();
    row.add(new Label("Name:"));
    TextField name = new TextField("");
    row.add(name);

    inner.add(row);

    inner.add(new Label("Baud:"));
    ComboBox sel_baud = new ComboBox();
    for(String baud : bauds) {
      sel_baud.add(baud, baud);
    }
    inner.add(sel_baud);

    ToolBar tools = new ToolBar();
    Button apply = new Button("Apply");
    tools.add(apply);
    Button open = new Button("Open Terminal");
    tools.add(open);
    inner.add(tools);

    Label msg = new Label("");
    inner.add(msg);

    {
      String port = sel_port.getSelectedValue();
      if (port != null) {
        loadName(port, name, sel_baud);
      }
    }

    sel_port.addChangedListener((cmp) -> {
      //load name, baud from Config
      msg.setText("");
      String port = sel_port.getSelectedValue();
      if (port == null) return;
      loadName(port, name, sel_baud);
    });

    apply.addClickListener((me, cmp) -> {
      msg.setText("Applying...");
      String port = sel_port.getSelectedValue();
      if (port == null) return;
      String baud = sel_baud.getSelectedValue();
      if (baud == null) return;
      //save name, baud to Config
      Config.current.setPortSettings(port, name.getText(), baud);
      msg.setText("Settings updated");
    });

    open.addClickListener((me, cmp) -> {
      msg.setText("Opening...");
      String port = sel_port.getSelectedValue();
      if (port == null) return;
      String baud = sel_baud.getSelectedValue();
      if (baud == null) return;
      apply.click();
      int int_baud = JF.atoi(baud);
      ComPort com;
      synchronized (com_lock) {
        com = ComPort.open(port, int_baud);
        if (com == null) {
          msg.setText("Unable to open COM port");
          return;
        }
        com_sessions.add(com);
      }
      TerminalSession sess = new TerminalSession();
      sess.id = JF.generateUUID();
      sess.com = com;
      sess.ts = System.currentTimeMillis();
      sess.put();
      cmp.getClient().openURL("/terminal?id=" + sess.id);
      msg.setText("Starting session on port " + port);
    });

    return panel;
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

    Button close_all_ports = new Button("Close All Ports");

    tools.add(close_all_ports);

    Label msg = new Label("");
    panel.add(msg);

    close_all_ports.addClickListener((me, cmp) -> {
      synchronized (com_lock) {
        msg.setText("");
        int cnt = 0;
        while (com_sessions.size() > 0) {
          ComPort port = com_sessions.remove(0);
          port.close();
          cnt++;
        }
        msg.setText("Ports closed : " + cnt);
      }
    });

    return panel;
  }

  public Panel getPanelResourceNotFound() {
    Panel panel = new Panel();
    Label errmsg = new Label("Error:Resource not found");
    errmsg.setColor(Color.red);
    panel.add(errmsg);
    return panel;
  }

  public Panel getWebTerminal(HTTP.Parameters params, WebUIClient client) {
    String id = params.get("id");
    if (id == null) {
      JFLog.log("TERM:id==null");
      return getPanelResourceNotFound();
    }
    TerminalSession sess = TerminalSession.get(id);
    if (sess == null) {
      JFLog.log("TERM:sess==null");
      return getPanelResourceNotFound();
    }
    UI ui = (UI)client.getProperty("ui");
    if (ui == null) {
      ui = new UI();
      client.setProperty("ui", ui);
    }
    return createTerminalPanel(sess.com, ui);
  }

  private Panel createTerminalPanel(ComPort com, UI ui) {
    Panel panel = new Panel();
    Row row = new Row();
    panel.add(row);
    Button close = new Button("Close");
    row.add(close);
    row.add(new Label("Com Port:" + com.getPort()));
    TerminalPanel term = new TerminalPanel();
    term.setup(com);
    term.connect();
    panel.add(term);
    ui.com = com;
    close.addClickListener((me, cmp) -> {
      synchronized (com_lock) {
        term.disconnect();  //invokes com.close()
        com_sessions.remove(com);
      }
      panel.remove(row);
      panel.remove(term);
      panel.add(new Label("Session ended! Please close this tab."));
    });
    return panel;
  }
}

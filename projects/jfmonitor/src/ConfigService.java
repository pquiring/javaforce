/** ConfigService
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.service.*;
import javaforce.webui.*;
import javaforce.webui.event.*;
import javaforce.webui.tasks.*;

public class ConfigService implements WebUIHandler {
  public WebUIServer server;
  public KeyMgmt keys;
  public Client client;

  public void start() {
    initSecureWebKeys();
    server = new WebUIServer();
    server.start(this, 443, keys);
  }

  public void stop() {
    if (server == null) return;
    server.stop();
    server = null;
  }

  private void initSecureWebKeys() {
    String keyfile = Paths.dataPath + "/jfmonitor.key";
    String password = "password";
    KeyParams params = new KeyParams();
    params.dname = "CN=jfmonitor.sourceforge.net, O=server, OU=webserver, C=CA";;
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

  private static class Selection {
    public HashMap<Port, Component> ports = new HashMap<>();

    public int size() {
      return ports.size();
    }

    public boolean containsGroup() {
      for(Port port : getPorts()) {
        if (port.isGroup) return true;
      }
      return false;
    }

    public boolean alreadyGrouped() {
      for(Port port : getPorts()) {
        if (port.group != null && port.group.length() > 0) {
          return true;
        }
      }
      return false;
    }

    public boolean notEquals() {
      Port[] ports = getPorts();
      Port first = ports[0];
      for(Port port : ports) {
        if (!port.equals(first)) {
          return true;
        }
      }
      return false;
    }

    private Port[] getPorts() {
      return ports.keySet().toArray(Port.ArrayType);
    }

    private Component[] getComponents() {
      return ports.values().toArray(Component.ArrayType);
    }

    public Port getPort(int idx) {
      if (size() == 0) return null;
      return getPorts()[idx];
    }

    public void setSelection(Port port, Component cell) {
      for(Port other : getPorts()) {
        ports.get(other).setBorder(false);
        ports.remove(other);
      }
      ports.put(port, cell);
      cell.setBorder(true);
    }

    public boolean isSelected(Port port) {
      return ports.containsKey(port);
    }

    public void invertSelection(Port port, Component cell) {
      if (ports.containsKey(port)) {
        ports.get(port).setBorder(false);
        ports.remove(port);
      } else {
        cell.setBorder(true);
        ports.put(port, cell);
      }
    }
  }

  private static class UI {
    public SplitPanel split;
    public Panel tasks;

    public PopupPanel message_popup;
    public Label message_message;

    public PopupPanel confirm_popup;
    public Label confirm_message;
    public Button confirm_button;
    public Runnable confirm_action;

    public Device device;
    public HashMap<Device, Selection> selection = new HashMap<>();

    public PopupPanel port_popup;
    public Runnable port_init;
    public TextField port_vlans;
    public TextField port_vlan;

    public PopupPanel vlan_popup;
    public Runnable vlan_init;
    public VLAN vlan_vlan;

    public PopupPanel vlans_popup;
    public Runnable vlans_init;
    public VLAN[] vlans_vlans;
  }

  private Panel tasksPanel(UI ui) {
    Panel panel = new Panel();
    panel.add(new Label("Tasks"));

    Panel tasks = new Panel();
    panel.add(tasks);

    ui.tasks = tasks;

    return panel;
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    switch (Config.current.mode) {
      case "install": return installPanel();
    }
    String password = (String)client.getProperty("password");
    if (password == null) {
      return loginPanel();
    }
    switch (Config.current.mode) {
      case "server": return serverPanel(client);
      case "client": return clientPanel();
    }
    return null;
  }

  //keep chars that are valid file names and avoid html chars
  private String cleanHost(String input) {
    StringBuilder sb = new StringBuilder();
    for(char ch : input.toCharArray()) {
      if (Character.isLetterOrDigit(ch)) {
        sb.append(ch);
      } else {
        switch (ch) {
          case '-': sb.append(ch); break;
          case '.': sb.append(ch); break;
        }
      }
    }
    return sb.toString();
  }

  private String cleanPath(String input) {
    StringBuilder sb = new StringBuilder();
    for(char ch : input.toCharArray()) {
      if (Character.isLetterOrDigit(ch)) {
        sb.append(ch);
      } else {
        switch (ch) {
          case '-': sb.append(ch); break;
          case '+': sb.append(ch); break;
          case '.': sb.append(ch); break;
          case ',': sb.append(ch); break;
          case '\\': sb.append(ch); break;
          case '/': sb.append('\\'); break;
          case ' ': sb.append(ch); break;
          case '!': sb.append(ch); break;
          case '@': sb.append(ch); break;
          case '#': sb.append(ch); break;
          case '$': sb.append(ch); break;
//          case '%': sb.append(ch); break;  //HTML character
          case '^': sb.append(ch); break;
//          case '&': sb.append(ch); break;  //HTML character
          case '(': sb.append(ch); break;
          case ')': sb.append(ch); break;
          case '[': sb.append(ch); break;
          case ']': sb.append(ch); break;
          case '~': sb.append(ch); break;  //not valid for unix
        }
      }
    }
    return sb.toString();
  }

  private String cleanNumber(String input) {
    StringBuilder sb = new StringBuilder();
    for(char ch : input.toCharArray()) {
      if (Character.isDigit(ch)) {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  public Panel loginPanel() {
    Panel panel = new Panel();
    Container ctr = new Container();
    InnerPanel inner = new InnerPanel("jfMonitor Login");
    panel.setAlign(Component.CENTER);
    Row row;
    Label msg = new Label("");
    inner.add(msg);
    row = new Row();
    row.add(new Label("Password:"));
    TextField password = new TextField("");
    password.setPassword(true);
    row.add(password);
    Button login = new Button("Login");
    row.add(login);
    inner.add(row);
    login.addClickListener( (MouseEvent m, Component c) -> {
      String passTxt = password.getText();
      WebUIClient webclient = c.getClient();
      if (passTxt.equals(Config.current.password)) {
        webclient.setProperty("password", passTxt);
        webclient.setPanel(getPanel("root", null, webclient));
      } else {
        msg.setText("Wrong password");
        msg.setColor(Color.red);
      }
    });
    ctr.add(inner);
    panel.add(ctr);
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
      panel.setVisible(false);
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

  private PopupPanel editPortPopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Edit Port");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label port_desc = new Label("Port:");
    row.add(port_desc);

    GridLayout grid = new GridLayout(2, 0, new int[] {GridLayout.RIGHT, GridLayout.LEFT});
    panel.add(grid);

    ComboBox mode = new ComboBox();
    mode.add("access", "L2:Access");
    mode.add("trunk", "L2:Trunk");
    mode.add("ip", "L3:IP");

    grid.addRow(new Component[] {new Label("Mode"), mode});

    TextField name = new TextField("");
    grid.addRow(new Component[] {new Label("Name"), name});

    TextField vlans = new TextField("");
    grid.addRow(new Component[] {new Label("VLANs"), vlans});

    TextField vlan = new TextField("");
    grid.addRow(new Component[] {new Label("VLAN"), vlan});

    TextField group = new TextField("");
    grid.addRow(new Component[] {new Label("Group"), group});

    TextField ip = new TextField("");
    grid.addRow(new Component[] {new Label("IP"), ip});

    TextField mask = new TextField("");
    grid.addRow(new Component[] {new Label("Mask"), mask});

    row = new Row();
    panel.add(row);
    Button b_save = new Button("Save");
    row.add(b_save);
    Button b_cancel = new Button("Cancel");
    row.add(b_cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.port_init = () -> {
      errmsg.setText("");
      Port port = ui.selection.get(ui.device).getPort(0);
      port_desc.setText("Port:" + port.id);
      mode.setSelectedIndex(Cisco.getSwitchMode(port.mode));
      name.setText(port.getName());
      vlans.setText(port.getVLANs());
      vlan.setText(port.getVLAN());
      group.setText(port.getGroup());
      ip.setText(port.ip);
      mask.setText(port.mask);
    };

    b_save.addClickListener((MouseEvent e, Component button) -> {
      errmsg.setText("");
      int _mode = mode.getSelectedIndex();
      String _name = name.getText();
      String _vlans = vlans.getText();
      String _vlan = vlan.getText();
      String _group = group.getText();
      String _ip = ip.getText();
      String _mask = mask.getText();
      Port port = ui.selection.get(ui.device).getPort(0);
      if (port == null) return;
      if (_ip.length() > 0 || _mask.length() > 0) {
        if (_ip.length() == 0 || !IP4.isIP(_ip)) {
          errmsg.setText("Invalid IP Address");
          return;
        }
        if (_mask.length() == 0 || !Subnet4.isSubnet(_mask)) {
          errmsg.setText("Invalid Subnet Mask");
          return;
        }
      }
      if (port.getMode() != _mode) {
        //change mode
        Task task = new Task("Set Port Mode") {
          public void doTask() {
            try {
              if (ui.device.configSetSwitchMode(port, _mode)) {
                setStatus("Completed");
              } else {
                setStatus("Failed");
              }
            } catch (Exception e) {
              setStatus("Error:" + action + " failed, check logs.");
              JFLog.log(e);
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      }
      if (!port.name.equals(_name)) {
        //change name
        Task task = new Task("Set Port Name") {
          public void doTask() {
            try {
              if (ui.device.configSetPortName(port, _name)) {
                setStatus("Completed");
              } else {
                setStatus("Failed");
              }
            } catch (Exception e) {
              setStatus("Error:" + action + " failed, check logs.");
              JFLog.log(e);
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      }
      if (_mode == Cisco.MODE_TRUNK && !port.getVLANs().equals(_vlans)) {
        //change vlans
        if (!VLAN.validVLANs(_vlans)) {
          errmsg.setText("Invalid VLANs");
          return;
        }
        String[] _vlan_list = VLAN.splitVLANs(_vlans);
        port.setVLANs(_vlan_list);
        Task task = new Task("Set Port VLANs") {
          public void doTask() {
            try {
              if (ui.device.configSetVLANs(port, _vlans)) {
                setStatus("Completed");
              } else {
                setStatus("Failed");
              }
            } catch (Exception e) {
              setStatus("Error:" + action + " failed, check logs.");
              JFLog.log(e);
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      }
      if (!port.getVLAN().equals(_vlan)) {
        //change vlan
        if (!VLAN.validVLAN(_vlan)) {
          errmsg.setText("Invalid VLAN");
          return;
        }
        Task task = new Task("Set Port VLAN") {
          public void doTask() {
            try {
              if (ui.device.configSetVLAN(port, _vlan)) {
                setStatus("Completed");
              } else {
                setStatus("Failed");
              }
            } catch (Exception e) {
              setStatus("Error:" + action + " failed, check logs.");
              JFLog.log(e);
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      }
      if (!port.getGroup().equals(_group)) {
        //change group membership
        if (_group.length() > 0 && (!Port.validGroup(_group) || !ui.device.groupExists(_group))) {
          errmsg.setText("Invalid Group");
          return;
        }
          Task task = new Task("Set Port Group") {
            public void doTask() {
              try {
                if (ui.device.configSetGroup(_group, port)) {
                  setStatus("Completed");
                } else {
                  setStatus("Failed");
                }
              } catch (Exception e) {
                setStatus("Error:" + action + " failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
      }
      if (_mode == Cisco.MODE_IP) {
        if (!_ip.equals(port.ip) || !_mask.equals(port.mask)) {
          if (_ip.length() > 0) {
            Task task = new Task("Set Port IP") {
              public void doTask() {
                try {
                  if (ui.device.configAddPort_IP(port, _ip, _mask)) {
                    setStatus("Completed");
                  } else {
                    setStatus("Failed");
                  }
                } catch (Exception e) {
                  setStatus("Error:" + action + " failed, check logs.");
                  JFLog.log(e);
                }
              }
            };
            Tasks.tasks.addTask(ui.tasks, task);
          } else {
            //remove ip
            Task task = new Task("Remove Port IP") {
              public void doTask() {
                try {
                  if (ui.device.configRemovePort_IP(port)) {
                    setStatus("Completed");
                  } else {
                    setStatus("Failed");
                  }
                } catch (Exception e) {
                  setStatus("Error:" + action + " failed, check logs.");
                  JFLog.log(e);
                }
              }
            };
            Tasks.tasks.addTask(ui.tasks, task);
          }
        }
      }
      QueryHardware.scan_now = true;
      panel.setVisible(false);
    });
    b_cancel.addClickListener((MouseEvent e, Component button) -> {
      panel.setVisible(false);
    });
    panel.setOnClose( () -> {
      b_cancel.click();
    });
    return panel;
  }

  private PopupPanel editVLANPopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Edit VLAN");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;
    GridLayout grid = new GridLayout(2, 0, new int[] {GridLayout.RIGHT, GridLayout.LEFT});
    panel.add(grid);

    TextField id = new TextField("");
    grid.addRow(new Component[] {new Label("ID"), id});

    TextField name = new TextField("");
    grid.addRow(new Component[] {new Label("Name"), name});

    TextField ip = new TextField("");
    grid.addRow(new Component[] {new Label("IP"), ip});

    TextField mask = new TextField("");
    grid.addRow(new Component[] {new Label("Mask"), mask});

    row = new Row();
    panel.add(row);
    Button b_save = new Button("Save");
    row.add(b_save);
    Button b_cancel = new Button("Cancel");
    row.add(b_cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.vlan_init = () -> {
      if (ui.vlan_vlan == null) {
        id.setText("");
        id.setReadonly(false);
        name.setText("");
        ip.setText("");
        mask.setText("");
      } else {
        id.setText(ui.vlan_vlan.getNumber());
        id.setReadonly(true);
        name.setText(ui.vlan_vlan.name);
        ip.setText(ui.vlan_vlan.ip);
        mask.setText(ui.vlan_vlan.mask);
      }
    };

    b_save.addClickListener((MouseEvent e, Component button) -> {
      errmsg.setText("");
      String _id = id.getText();
      String _name = name.getText();
      String _ip = ip.getText();
      String _mask = mask.getText();
      if (!VLAN.validVLAN(_id)) {
        errmsg.setText("Invalid VLAN ID");
        return;
      }
      if (_ip.length() > 0 || _mask.length() > 0) {
        if (_ip.length() == 0 || !IP4.isIP(_ip)) {
          errmsg.setText("Invalid IP Address");
          return;
        }
        if (_mask.length() == 0 || !Subnet4.isSubnet(_mask)) {
          errmsg.setText("Invalid Subnet Mask");
          return;
        }
      }
      if (ui.vlan_vlan == null) {
        //create vlan
        if (true) {
          Task task = new Task("Create VLAN") {
            public void doTask() {
              try {
                if (ui.device.configCreateVLAN(_id, _name)) {
                  setStatus("Completed");
                } else {
                  setStatus("Failed");
                }
              } catch (Exception e) {
                setStatus("Error:" + action + " failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
        }
        if (_ip.length() > 0) {
          Task task = new Task("Create VLAN IP") {
            public void doTask() {
              try {
                if (ui.device.configAddVLAN_IP(ui.vlan_vlan, _ip, _mask)) {
                  setStatus("Completed");
                } else {
                  setStatus("Failed");
                }
              } catch (Exception e) {
                setStatus("Error:" + action + " failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
        }
      } else {
        //edit vlan
        if (true) {
          Task task = new Task("Set VLAN Name") {
            public void doTask() {
              try {
                if (ui.device.configEditVLAN(ui.vlan_vlan, _name)) {
                  setStatus("Completed");
                } else {
                  setStatus("Failed");
                }
              } catch (Exception e) {
                setStatus("Error:" + action + " failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
        }
        if (_ip.length() > 0) {
          Task task = new Task("Create VLAN IP") {
            public void doTask() {
              try {
                if (ui.device.configAddVLAN_IP(ui.vlan_vlan, _ip, _mask)) {
                  setStatus("Completed");
                } else {
                  setStatus("Failed");
                }
              } catch (Exception e) {
                setStatus("Error:" + action + " failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
        } else {
          if (ui.vlan_vlan.ip.length() > 0) {
            Task task = new Task("Remove VLAN IP") {
              public void doTask() {
                try {
                  if (ui.device.configRemoveVLAN_IP(ui.vlan_vlan)) {
                    setStatus("Completed");
                  } else {
                    setStatus("Failed");
                  }
                } catch (Exception e) {
                  setStatus("Error:" + action + " failed, check logs.");
                  JFLog.log(e);
                }
              }
            };
            Tasks.tasks.addTask(ui.tasks, task);
          }
        }
      }
      ui.vlans_init.run();
      QueryHardware.scan_now = true;
      panel.setVisible(false);
    });
    b_cancel.addClickListener((MouseEvent e, Component button) -> {
      panel.setVisible(false);
    });
    panel.setOnClose( () -> {
      b_cancel.click();
    });
    return panel;
  }

  private PopupPanel viewVLANsPopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Edit VLAN");
    panel.setPosition(256, 128);
    panel.setModal(true);

    ToolBar tools = new ToolBar();
    panel.add(tools);

    Button add = new Button("Add");
    tools.add(add);
    Button edit = new Button("Edit");
    tools.add(edit);
    Button delete = new Button("Delete");
    tools.add(delete);

    Table table = new Table(new int[] {64, 128, 128, 128}, 32, 4, 0);
    table.setBorder(true);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setHeader(true);
    panel.add(table);

    ui.vlans_init = () -> {
      if (ui.device == null) return;
      ui.vlans_vlans = ui.device.hardware.vlans.toArray(VLAN.ArrayType);
      Arrays.sort(ui.vlans_vlans);
      table.removeAll();
      table.addRow(new Component[] {new Label("ID"), new Label("Name"), new Label("IP"), new Label("Mask")});
      for(VLAN vlan : ui.vlans_vlans) {
        table.addRow(new Component[] {new Label(vlan.getNumber()), new Label(vlan.name), new Label(vlan.ip), new Label(vlan.mask)});
      }
    };

    Button close = new Button("Close");
    close.setAlign(Component.RIGHT);
    panel.add(close);

    add.addClickListener((me, cmp) -> {
      ui.vlan_vlan = null;
      ui.vlan_init.run();
      ui.vlan_popup.setVisible(true);
    });

    edit.addClickListener((me, cmp) -> {
      int idx = table.getSelectedRow();
      if (idx == -1) return;
      ui.vlan_vlan = ui.vlans_vlans[idx];
      ui.vlan_init.run();
      ui.vlan_popup.setVisible(true);
    });

    delete.addClickListener((me, cmp) -> {
      int idx = table.getSelectedRow();
      if (idx == -1) return;
      VLAN vlan = ui.vlans_vlans[idx];
      ui.confirm_action = () -> {
        Task task = new Task("Delete VLAN") {
          public void doTask() {
            try {
              if (ui.device.configRemoveVLAN(vlan)) {
                setStatus("Completed");
              } else {
                setStatus("Failed");
              }
            } catch (Exception e) {
              setStatus("Error:" + action + " failed, check logs.");
              JFLog.log(e);
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_message.setText("Delete VLAN : Are you sure?");
      ui.confirm_button.setText("Delete");
      ui.confirm_popup.setVisible(true);
    });

    close.addClickListener((me, cmp) -> {
      panel.setVisible(false);
    });

    panel.setOnClose( () -> {
      close.click();
    });

    return panel;
  }

  public Panel serverPanel(WebUIClient client) {
    Panel panel = new Panel();

    UI ui = new UI();

    ui.message_popup = messagePopupPanel(ui);
    panel.add(ui.message_popup);

    ui.confirm_popup = confirmPopupPanel(ui);
    panel.add(ui.confirm_popup);

    ui.port_popup = editPortPopupPanel(ui);
    panel.add(ui.port_popup);

    ui.vlans_popup = viewVLANsPopupPanel(ui);
    panel.add(ui.vlans_popup);

    ui.vlan_popup = editVLANPopupPanel(ui);
    panel.add(ui.vlan_popup);

    int topSize = client.getHeight() - 128;
    SplitPanel top_bot = new SplitPanel(SplitPanel.HORIZONTAL);
    panel.add(top_bot);
    top_bot.setDividerPosition(topSize);

    SplitPanel left_right = new SplitPanel(SplitPanel.VERTICAL);
    ui.split = left_right;
    left_right.setName("split");
    int leftSize = 128;
    left_right.setDividerPosition(leftSize);
    Panel left = serverLeftPanel(leftSize);
    Panel right = null;
    String screen = (String)client.getProperty("screen");
    if (screen == null) screen = "";
    switch (screen) {
      case "": right = serverHome(); break;
      case "status": right = serverStatus(); break;
      case "monitor_network": right = serverMonitorNetwork(); break;
      case "monitor_hardware": right = serverMonitorHardware(ui); break;
      case "monitor_storage": right = serverMonitorStorage(); break;
      case "config_network": right = serverConfigNetwork(); break;
      case "config_hardware": right = serverConfigHardware(); break;
      case "config_storage": right = serverConfigStorage(); break;
      case "config": right = serverConfig(); break;
      case "logs": right = serverLogs(null); break;
      default: JFLog.log("Unknown screen:" + screen); right = new Panel(); break;
    }
    left_right.setLeftComponent(left);
    right.setHeight(topSize);
    left_right.setRightComponent(right);

    Panel tasks = tasksPanel(ui);

    top_bot.setTopComponent(left_right);
    top_bot.setBottomComponent(tasks);

    return panel;
  }

  public Panel serverLeftPanel(int leftSize) {
    Panel panel = new Panel();
    //left side
    ListBox list = new ListBox();
    list.setName("list");
    //add menu options
    Button opt_status = new Button("Status");
    list.add(opt_status);
    opt_status.setWidth(leftSize);
    opt_status.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "status");
      webclient.refresh();
    });
    Button opt_net_mon = new Button("Network Monitor");
    list.add(opt_net_mon);
    opt_net_mon.setWidth(leftSize);
    opt_net_mon.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "monitor_network");
      webclient.refresh();
    });
    Button opt_hw_mon = new Button("Monitor Hardware");
    list.add(opt_hw_mon);
    opt_hw_mon.setWidth(leftSize);
    opt_hw_mon.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "monitor_hardware");
      webclient.refresh();
    });
    Button opt_storage_mon = new Button("Storage Monitor");
    list.add(opt_storage_mon);
    opt_storage_mon.setWidth(leftSize);
    opt_storage_mon.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "monitor_storage");
      webclient.refresh();
    });
    Button opt_setup_net = new Button("Setup Network");
    list.add(opt_setup_net);
    opt_setup_net.setWidth(leftSize);
    opt_setup_net.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "config_network");
      webclient.refresh();
    });
    Button opt_setup_hw = new Button("Setup Hardware");
    list.add(opt_setup_hw);
    opt_setup_hw.setWidth(leftSize);
    opt_setup_hw.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "config_hardware");
      webclient.refresh();
    });
    Button opt_cfg = new Button("Configure");
    list.add(opt_cfg);
    opt_cfg.setWidth(leftSize);
    opt_cfg.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "config");
      webclient.refresh();
    });
    Button opt_logs = new Button("Logs");
    list.add(opt_logs);
    opt_logs.setWidth(leftSize);
    opt_logs.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "logs");
      webclient.refresh();
    });

    panel.add(list);
    return panel;
  }

  public Panel serverHome() {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    panel.add(new Label("Select an option on the left."));

    return panel;
  }

  public Panel serverStatus() {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    row = new Row();
    row.add(new Label("Status:"));
    panel.add(row);

    TextArea text = new TextArea("Loading...");
    text.setReadonly(true);
    text.setMaxWidth();
    text.setMaxHeight();

    panel.add(text);

    new Thread() {
      public void run() {
        StringBuilder sb = new StringBuilder();
        sb.append("Online Clients:\r\n\r\n");
        for(String host : Config.current.hosts) {
          sb.append(host + "\r\n");
        }
        text.setText(sb.toString());
      }
    }.start();

    return panel;
  }

  private static String memoryUsage() {
    long total = Runtime.getRuntime().totalMemory();
    long free = Runtime.getRuntime().freeMemory();
    long max = Runtime.getRuntime().maxMemory();
    long used = total - free;
    return " Memory:Used:" + JF.toEng(used) + " Max:" + JF.toEng(max);
  }

  private boolean valid(String nw_host, String nw_ip, String nw_first, String nw_last, String nw_dhcp_first, String nw_dhcp_last, Label msg) {
    //validate form
    if (nw_host.length() == 0) {
      msg.setText("Invalid client");
      return false;
    }
    if (!PacketCapture.valid_ip(nw_ip)) {
      msg.setText("Invalid network interface IP");
      return false;
    }
    if (!PacketCapture.valid_ip(nw_first)) {
      msg.setText("Invalid first IP");
      return false;
    }
    if (!PacketCapture.valid_ip(nw_last)) {
      msg.setText("Invalid last IP");
      return false;
    }
    byte[] b_first_ip = PacketCapture.decode_ip(nw_first);
    int i_first_ip = BE.getuint32(b_first_ip, 0);
    byte[] b_last_ip = PacketCapture.decode_ip(nw_last);
    int i_last_ip = BE.getuint32(b_last_ip, 0);
    int nw_length = PacketCapture.get_ip_range_length(b_first_ip, b_last_ip);
    if (nw_length < 0) {
      msg.setText("Invalid IP range");
      return false;
    }
    if (nw_dhcp_first.length() > 0 && !PacketCapture.valid_ip(nw_dhcp_first)) {
      msg.setText("Invalid first dhcp IP");
      return false;
    }
    if (nw_dhcp_last.length() > 0 && !PacketCapture.valid_ip(nw_dhcp_last)) {
      msg.setText("Invalid last dhcp IP");
      return false;
    }
    if (nw_dhcp_first.length() == 0 && nw_dhcp_last.length() != 0) {
      msg.setText("Invalid dhcp range");
      return false;
    }
    if (nw_dhcp_first.length() != 0 && nw_dhcp_last.length() == 0) {
      msg.setText("Invalid dhcp range");
      return false;
    }
    if (nw_dhcp_first.length() > 0) {
      byte[] b_dhcp_first_ip = PacketCapture.decode_ip(nw_dhcp_first);
      int i_dhcp_first_ip = BE.getuint32(b_dhcp_first_ip, 0);
      byte[] b_dhcp_last_ip = PacketCapture.decode_ip(nw_dhcp_last);
      int i_dhcp_last_ip = BE.getuint32(b_dhcp_last_ip, 0);
      int dhcp_length = PacketCapture.get_ip_range_length(b_dhcp_first_ip, b_dhcp_last_ip);
      if (dhcp_length < 0 ||
        i_dhcp_first_ip < i_first_ip || i_dhcp_first_ip > i_last_ip ||
        i_dhcp_last_ip < i_first_ip || i_dhcp_last_ip > i_last_ip
      ) {
        msg.setText("Invalid dhcp range");
        return false;
      }
    }
    return true;
  }

  public Panel serverConfigNetwork() {
    Panel panel = new ScrollPanel();
    Row row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    row = new Row();
    row.add(new Label("Client:"));
    ComboBox client = new ComboBox();
    for(String host : Config.current.hosts) {
      client.add(host, host);
    }
    row.add(client);
    panel.add(row);

    row = new Row();
    row.add(new Label("Network Interface IP:"));
    TextField host_ip = new TextField("");
    row.add(host_ip);
    panel.add(row);

    row = new Row();
    row.add(new Label("First IP:"));
    TextField first_ip = new TextField("");
    row.add(first_ip);
    row.add(new Label("Last IP:"));
    TextField last_ip = new TextField("");
    row.add(last_ip);
    panel.add(row);

    row = new Row();
    row.add(new Label("DHCP First IP:"));
    TextField first_dhcp_ip = new TextField("");
    row.add(first_ip);
    row.add(new Label("Last IP:"));
    TextField last_dhcp_ip = new TextField("");
    row.add(last_ip);
    row.add(new Label("(optional)"));
    panel.add(row);

    row = new Row();
    row.add(new Label("Description:"));
    TextField desc = new TextField("");
    row.add(desc);
    panel.add(row);

    row = new Row();
    Button add = new Button("Add");
    row.add(add);
    Label msg = new Label("");
    msg.setColor(Color.red);
    row.add(msg);
    panel.add(row);
    add.addClickListener( (MouseEvent me, Component c) -> {
      String nw_host = client.getSelectedText();
      String nw_ip = host_ip.getText();
      String nw_first = first_ip.getText();
      String nw_last = last_ip.getText();
      String nw_dhcp_first = first_dhcp_ip.getText();
      String nw_dhcp_last = last_dhcp_ip.getText();
      String nw_desc = desc.getText();
      if (!valid(nw_host, nw_ip, nw_first, nw_last, nw_dhcp_first, nw_dhcp_last, msg)) {
        return;
      }

      Network nw = new Network();
      nw.host = nw_host;
      nw.ip_nic = nw_ip;
      nw.ip_first = nw_first;
      nw.ip_last = nw_last;
      nw.ip_dhcp_first = nw_dhcp_first;
      nw.ip_dhcp_last = nw_dhcp_last;
      nw.desc = nw_desc;
      nw.init();
      Config.current.addNetwork(nw);
      Config.save();
      WebUIClient webclient = c.getClient();
      webclient.refresh();
    });

    for(Network nw : Config.current.getNetworks()) {
      row = new Row();
      row.setBackColor(Color.blue);
      row.setHeight(5);
      panel.add(row);

      row = new Row();
      row.add(new Label("Client:" + nw.host));
      panel.add(row);

      row = new Row();
      row.add(new Label("Network Interface IP:"));
      TextField edit_host_ip = new TextField(nw.ip_nic);
      row.add(edit_host_ip);
      panel.add(row);

      row = new Row();
      row.add(new Label("First IP:"));
      TextField edit_first_ip = new TextField(nw.ip_first);
      row.add(edit_first_ip);
      row.add(new Label("Last IP:"));
      TextField edit_last_ip = new TextField(nw.ip_last);
      row.add(edit_last_ip);
      panel.add(row);

      row = new Row();
      row.add(new Label("DHCP First IP:"));
      TextField edit_first_dhcp_ip = new TextField(nw.ip_dhcp_first);
      row.add(edit_first_dhcp_ip);
      row.add(new Label("Last IP:"));
      TextField edit_last_dhcp_ip = new TextField(nw.ip_dhcp_last);
      row.add(edit_last_dhcp_ip);
      row.add(new Label("(optional)"));
      panel.add(row);

      row = new Row();
      row.add(new Label("Description:"));
      TextField edit_desc = new TextField(nw.desc);
      row.add(edit_desc);
      panel.add(row);

      row = new Row();
      Button edit_save = new Button("Save");
      row.add(edit_save);
      Label edit_msg = new Label("");
      msg.setColor(Color.red);
      row.add(edit_msg);
      edit_save.addClickListener( (MouseEvent me, Component c) -> {
        String nw_ip = edit_host_ip.getText();
        String nw_first = edit_first_ip.getText();
        String nw_last = edit_last_ip.getText();
        String nw_dhcp_first = edit_first_dhcp_ip.getText();
        String nw_dhcp_last = edit_last_dhcp_ip.getText();
        String nw_desc = edit_desc.getText();
        if (!valid(nw.host, nw_ip, nw_first, nw_last, nw_dhcp_first, nw_dhcp_last, msg)) {
          return;
        }
        nw.ip_nic = nw_ip;
        nw.ip_first = nw_first;
        nw.ip_last = nw_last;
        nw.ip_dhcp_first = nw_dhcp_first;
        nw.ip_dhcp_last = nw_dhcp_last;
        nw.desc = nw_desc;
        nw.validate();
        Config.save();
        WebUIClient webclient = c.getClient();
        webclient.refresh();
      });
      Button edit_delete = new Button("Delete");
      row.add(edit_delete);
      Label edit_msg_delete = new Label("");
      msg.setColor(Color.red);
      row.add(edit_msg_delete);
      edit_delete.addClickListener( (MouseEvent me, Component c) -> {
        Config.current.removeNetwork(nw);
        WebUIClient webclient = c.getClient();
        webclient.refresh();
      });
      panel.add(row);
    }

    return panel;
  }

  public Panel serverConfigHardware() {
    Panel panel = new ScrollPanel();
    Row row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    row = new Row();
    row.add(new Label("Type:"));
    ComboBox type = new ComboBox();
    type.add("Cisco", "Cisco");
    row.add(type);
    panel.add(row);

    row = new Row();
    row.add(new Label("Device:"));
    TextField device = new TextField("");
    row.add(device);
    panel.add(row);

    row = new Row();
    row.add(new Label("Username:"));
    TextField user = new TextField("");
    row.add(user);
    panel.add(row);

    row = new Row();
    row.add(new Label("Password:"));
    TextField pass = new TextField("");
    pass.setPassword(true);
    row.add(pass);
    panel.add(row);

    row = new Row();
    Button add = new Button("Add");
    row.add(add);
    panel.add(row);

    row = new Row();
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);
    panel.add(row);

    row = new Row();
    Label msg = new Label("");
    row.add(msg);
    panel.add(row);

    add.addClickListener((me, cmp) -> {
      errmsg.setText("");
      msg.setText("Connecting...");
      String _host = device.getText();
      if (!IP4.isIP(_host)) {
        errmsg.setText("Invalid IP address");
        msg.setText("");
        return;
      }
      String _mac = Config.current.getmac(_host);
      if (_mac == null) {
        errmsg.setText("Device mac not found");
        msg.setText("");
        return;
      }
      Device _device = Config.current.getDevice(_mac);
      if (_device == null) {
        errmsg.setText("Device not found");
        msg.setText("");
        return;
      }
      if (_device.hardware != null) {
        errmsg.setText("Device already added");
        msg.setText("");
        return;
      }
      String _user = user.getText();
      String _pass = pass.getText();
      SSH ssh = new SSH();
      SSH.Options opts = new SSH.Options();
      opts.username = _user;
      opts.password = _pass;
      if (!ssh.connect(_host, 22, opts)) {
        errmsg.setText("Connection failed");
        msg.setText("");
        return;
      }
      ssh.disconnect();
      _device.hardware = new Hardware();
      _device.type = Device.TYPE_CISCO;
      _device.hardware.user = _user;
      _device.hardware.pass = _pass;
      msg.setText("Device added");
      device.setText("");
      user.setText("");
      pass.setText("");
      Config.save();
      QueryHardware.scan_now = true;
    });

    return panel;
  }

  public Panel serverConfigStorage() {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    //TODO : list current storage

    return panel;
  }

  public String validString(String str) {
    if (str == null) return "";
    return str;
  }

  public Panel serverConfig() {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("Config Settings");
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    Label msg = new Label("");
    panel.add(msg);

    row = new Row();
    row.add(new Label("EMail Notification:"));
    panel.add(row);
    row = new Row();
    row.add(new Label("SMTP Server:"));
    TextField email_server = new TextField(validString(Config.current.email_server));
    row.add(email_server);
    CheckBox email_secure = new CheckBox("Secure");
    email_secure.setSelected(Config.current.email_secure);
    row.add(email_secure);
    panel.add(row);
    row = new Row();
    row.add(new Label(" Email To:"));
    TextField emails = new TextField(validString(Config.current.emails));
    row.add(emails);
    row.add(new Label("(seperate multiple emails with commas)"));
    panel.add(row);
    row = new Row();
    row.add(new Label("SMTP Auth (optional): User:"));
    TextField email_user = new TextField(validString(Config.current.email_user));
    row.add(email_user);
    row.add(new Label("Pass:"));
    TextField email_pass = new TextField(validString(Config.current.email_pass));
    row.add(email_pass);
    row.add(new Label("Type:"));
    ComboBox email_type = new ComboBox();
    email_type.add("LOGIN", "LOGIN");
    email_type.add("NTLM", "NTLM");
    switch (Config.current.email_type) {
      case SMTP.AUTH_LOGIN: email_type.setSelectedIndex(0); break;
      case SMTP.AUTH_NTLM: email_type.setSelectedIndex(1); break;
    }
    row.add(email_type);
    panel.add(row);

    row = new Row();
    row.add(new Label("Unknown Devices:"));
    CheckBox unknowns = new CheckBox("Daily Notification Report");
    unknowns.setSelected(Config.current.notify_unknown_device);
    row.add(unknowns);
    panel.add(row);

    row = new Row();
    Button email_save = new Button("Save");
    email_save.addClickListener((MouseEvent me, Component c) -> {
      try {
        String _email_server = email_server.getText();
        boolean _email_secure = email_secure.isSelected();
        String _emails = emails.getText();
        String _email_user = email_user.getText();
        String _email_pass = email_pass.getText();
        Config.current.email_server = _email_server;
        Config.current.email_secure = _email_secure;
        Config.current.email_user = _email_user;
        Config.current.email_pass = _email_pass;
        int _email_type = email_type.getSelectedIndex();
        switch (_email_type) {
          case 0: Config.current.email_type = SMTP.AUTH_LOGIN; break;
          case 1: Config.current.email_type = SMTP.AUTH_NTLM; break;
        }
        Config.current.emails = _emails;
        Config.current.notify_unknown_device = unknowns.isSelected();
        Config.save();
        msg.setText("Updated Notification Settings");
        msg.setColor(Color.green);
      } catch (Exception e) {
        e.printStackTrace();
        msg.setText("Failed to save Notification Settings");
        msg.setColor(Color.red);
      }
    });
    row.add(email_save);
    panel.add(row);

    return panel;
  }

  public Panel serverLogs(String file) {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("Logs");
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    if (file == null) {
      ScrollPanel scroll = new ScrollPanel();
      panel.setMaxWidth();
      row.setMaxWidth();
      row.setMaxHeight();
      panel.add(scroll);
      //list log files (limit 1 year)
      File folder = new File(Paths.logsPath);
      File files[] = folder.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.startsWith("backup") || name.startsWith("restore");
        }
      });
      Arrays.sort(files, new Comparator<File>() {
        public int compare(File f1, File f2) {
          long t1 = f1.lastModified();
          long t2 = f2.lastModified();
          if (t1 == t2) return 0;
          if (t1 < t2) return 1;
          return -1;
        }
      });
      long oneyear = System.currentTimeMillis();
      oneyear -= 365 * 24 * 60 * 60 * 1000;
      for(File log : files) {
        if (!log.isFile()) continue;
        String name = log.getName();
        long lastMod = log.lastModified();
        if (lastMod < oneyear) continue;
        row = new Row();
        Button view = new Button(name);
        view.addClickListener((MouseEvent me, Component c) -> {
          SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
          split.setRightComponent(serverLogs(name));
        });
        row.add(view);
        row.add(new Label(" at " + toDateTime(lastMod)));
        scroll.add(row);
      }
      if (files.length == 0) {
        row = new Row();
        row.add(new Label("No logs found"));
        scroll.add(row);
      }
    } else {
      TextArea text = new TextArea("Loading...");
      text.setReadonly(true);
      text.setMaxWidth();
      text.setMaxHeight();
      panel.setMaxWidth();
      row.setMaxWidth();
      try {
        FileInputStream fis = new FileInputStream(Paths.logsPath + "\\" + file);
        byte data[] = fis.readAllBytes();
        fis.close();
        text.setText(new String(data));
      } catch (Exception e) {
        text.setText("Error:" + e.toString());
      }
      panel.add(text);
    }

    return panel;
  }

  public Panel serverMonitorNetwork() {
    Panel panel = new ScrollPanel();
    Row row;

    row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    row = new Row();
    row.add(new Label("Network Monitor:"));
    Label progress = new Label("");
    row.add(progress);
    panel.add(row);

    for(Network nw : Config.current.getNetworks()) {
      InnerPanel inner = new InnerPanel("Network:" + nw.desc);
      for(IP ip : nw.ips) {
        Block block = new Block();
        row = new Row();
        Label icon = new Label("");
        icon.setBorder(true);
        icon.setStyle("padding", "5px");
        if (ip.online) {
          icon.setBackColor(Color.green);
        } else {
          if (ip.notify) {
            icon.setBackColor(Color.red);
          } else {
            icon.setBackColor(Color.grey);
          }
        }
        row.add(icon);
        row.add(new Label(ip.host));
        if (ip.mac != null && !ip.mac.equals("null")) {
          row.add(new Label(":"));
          TextField mac = new TextField(ip.mac);
          mac.setReadonly(true);
          row.add(mac);
          Device dev = Config.current.getDevice(ip.mac);
          row.add(new Label(":"));
          TextField desc = new TextField(dev == null ? "Unknown device" : dev.desc);
          row.add(desc);
          CheckBox notify = new CheckBox("notify");
          notify.setSelected(ip.notify);
          row.add(notify);
          Button update = new Button(dev == null ? "Add" : "Save");
          update.addClickListener( (me, c) -> {
            Device _dev = Config.current.getDevice(ip.mac);
            if (_dev == null) {
              ip.notify = notify.isSelected();
              Device newdev = new Device();
              newdev.mac = ip.mac;
              newdev.desc = desc.getText();
              Config.current.addDevice(newdev);
              update.setText("Save");
            } else {
              ip.notify = notify.isSelected();
              Device newdev = Config.current.getDevice(ip.mac);
              newdev.desc = desc.getText();
            }
            Config.save();
          });
          row.add(update);
        }
        block.add(row);
        inner.add(block);
      }
      panel.add(inner);
    }

/*
    //TODO : update buttons ??? refresh for now
    new Thread() {
      public void run() {
        WebUIClient webclient = panel.getClient();
        int id = webclient.getCurrentID();
        if (id == -1) return;
        while (webclient.getCurrentID() == id) {
          //TODO : update lights
          JF.sleep(1000);
        }
      }
    }.start();
*/

    return panel;
  }

  private void setupPortCell(Component cell, Device device, Port port, Label msg, UI ui) {
    cell.setSize(CELL_SIZE_X, CELL_SIZE_Y);
    if (port.link) {
      cell.setBackColor(Color.green);
    } else {
      cell.setBackColor(Color.grey);
    }
    if (!ui.selection.containsKey(device)) {
      ui.selection.put(device, new Selection());
    }
    cell.addClickListener((me, cmp) -> {
      if (me.ctrlKey)
        ui.selection.get(device).invertSelection(port, cell);
      else
        ui.selection.get(device).setSelection(port, cell);
      msg.setText("Port:" + port.info(device.hardware.ports.toArray(Port.ArrayType)));
    });
  }

  private static final int CELL_SIZE_X = 48;
  private static final int CELL_SIZE_Y = 32;

  public Panel serverMonitorHardware(UI ui) {
    Panel panel = new ScrollPanel();
    Row row;

    row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    row = new Row();
    row.add(new Label("Hardware Monitor:"));
    Button refresh = new Button("Refresh");
    row.add(refresh);
    panel.add(row);

    Device[] list = Config.current.getDevices();
    for(Device device : list) {
      if (device.hardware == null) continue;

      row = new Row();
      row.setBackColor(Color.blue);
      row.setHeight(5);
      panel.add(row);

      Hardware hw = device.hardware;

      ToolBar tools = new ToolBar();
      panel.add(tools);
      Button editPort = new Button("Edit Port");
      tools.add(editPort);
      Button viewVLAN = new Button("View VLANs");
      tools.add(viewVLAN);
      Button addGroup = new Button("Create Group");
      tools.add(addGroup);
      Button removeGroup = new Button("Remove Group");
      tools.add(removeGroup);
      Button save = new Button("Save");
      tools.add(save);
      Button delete = new Button("Delete");
      tools.add(delete);

      row = new Row();
      panel.add(row);
      Label desc = new Label("Device:" + Config.current.getip(device.mac));
      row.add(desc);

      row = new Row();
      panel.add(row);
      Label msg = new Label("");
      row.add(msg);

      row = new Row();
      panel.add(row);
      Label errmsg = new Label("");
      errmsg.setColor(Color.red);
      row.add(errmsg);

      Table table = new Table(CELL_SIZE_X, CELL_SIZE_Y, 0, 3);
      table.setBorder(true);
      panel.add(table);

      int gidx = 0;
      int gcnt = hw.groups.size();
      int pcnt = hw.ports.size();
      for(int pidx = 0;pidx < pcnt;) {
        int idx2 = pidx + 1;
        Port p1 = hw.ports.get(pidx);
        Port p2 = idx2 < pcnt ? hw.ports.get(pidx + 1) : null;

        Component c1 = new Label(p1.toString());
        setupPortCell(c1, device, p1, msg, ui);
        Component c2 = p2 != null ? new Label(p2.toString()) : null;
        if (c2 != null) {
          setupPortCell(c2, device, p2, msg, ui);
        } else {
          c2 = new Label("X");
        }

        if (gidx < gcnt) {
          Port p3 = hw.groups.get(gidx++);
          Component c3 = new Label(p3.toString());
          setupPortCell(c3, device, p3, msg, ui);
          table.addColumn(new Component[] {c1, c2, c3});
        } else {
          table.addColumn(new Component[] {c1, c2});
        }

        pidx += 2;
      }
      if (pcnt == 0) {
        table.addColumn(new Component[] {new Label("?"), new Label("?")});
      }

      editPort.addClickListener((me, cmp) -> {
        if (ui.selection.get(device).size() != 1) {
          errmsg.setText("Must select only one port to edit");
          return;
        }
        ui.device = device;
        ui.port_init.run();
        ui.port_popup.setVisible(true);
      });

      viewVLAN.addClickListener((me, cmp) -> {
        ui.device = device;
        ui.vlans_init.run();
        ui.vlans_popup.setVisible(true);
      });

      addGroup.addClickListener((me, cmp) -> {
        if (ui.selection.get(device).size() < 1) {
          errmsg.setText("Must select more than one port to make a group");
          return;
        }
        if (ui.selection.get(device).containsGroup()) {
          errmsg.setText("Do not select a group port to create a group");
          return;
        }
        if (ui.selection.get(device).alreadyGrouped()) {
          errmsg.setText("One or more ports is already in a group");
          return;
        }
        if (ui.selection.get(device).notEquals()) {
          errmsg.setText("Ports must be all the same");
          return;
        }
        ui.confirm_action = () -> {
          Port[] ports = ui.selection.get(device).getPorts();
          Task task = new Task("Create Group") {
            public void doTask() {
              try {
                if (device.configCreateGroup(device.nextGroupID(), ports)) {
                  setStatus("Completed");
                } else {
                  setStatus("Failed");
                }
              } catch (Exception e) {
                setStatus("Error:" + action + " failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
        };
        ui.confirm_message.setText("Create Group : Are you sure?");
        ui.confirm_button.setText("Create");
        ui.confirm_popup.setVisible(true);
      });

      removeGroup.addClickListener((me, cmp) -> {
        errmsg.setText("");
        if (ui.selection.get(device).size() != 1) {
          errmsg.setText("Must select one group port");
          return;
        }
        Port group = ui.selection.get(device).getPorts()[0];
        if (!group.isGroup) {
          errmsg.setText("Must select one group port");
          return;
        }
        ui.confirm_action = () -> {
          Task task = new Task("Delete Group") {
            public void doTask() {
              try {
                if (ui.device.configRemoveGroup(group.getGroupID())) {
                  setStatus("Completed");
                } else {
                  setStatus("Failed");
                }
              } catch (Exception e) {
                setStatus("Error:" + action + " failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
        };
        ui.confirm_message.setText("Delete Group : Are you sure?");
        ui.confirm_button.setText("Delete");
        ui.confirm_popup.setVisible(true);
      });

      save.addClickListener((me, cmp) -> {
        Task task = new Task("Save Config") {
          public void doTask() {
            try {
              if (device.saveConfig()) {
                setStatus("Completed");
              } else {
                setStatus("Failed");
              }
            } catch (Exception e) {
              setStatus("Error:" + action + " failed, check logs.");
              JFLog.log(e);
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      });

      delete.addClickListener((me, cmp) -> {
        ui.confirm_action = () -> {
          device.hardware = null;
          Config.save();
          WebUIClient webclient = cmp.getClient();
          webclient.refresh();
        };
        ui.confirm_message.setText("Delete Device : Are you sure?");
        ui.confirm_button.setText("Delete");
        ui.confirm_popup.setVisible(true);
      });
    }

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    refresh.addClickListener((me, cmp) -> {
      cmp.getClient().refresh();
    });

    return panel;
  }

  public Panel serverMonitorStorage() {
    Panel panel = new ScrollPanel();
    Row row = new Row();
    Label label = new Label("jfMonitor/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    row = new Row();
    row.add(new Label("Storage Monitor:"));
    Label progress = new Label("");
    row.add(progress);
    panel.add(row);

    for(ServerClient client : Server.clients) {
      row = new Row();
      row.setBackColor(Color.blue);
      row.setHeight(5);
      panel.add(row);

      row = new Row();
      row.add(new Label("System:" + client.getHost()));
      panel.add(row);

      for(Storage store : client.stores) {
        row = new Row();
        row.add(new Label("File System:" + store.name));
        panel.add(row);

        row = new Row();
        row.add(new Label("Size:" + JF.toEng(store.size)));

        Label sp1 = new Label("");
        sp1.setStyle("padding", "5px");
        row.add(sp1);

        row.add(new Label("Used:" + JF.toEng(store.size - store.free)));

        Label sp2 = new Label("");
        sp2.setStyle("padding", "5px");
        row.add(sp2);

        row.add(new Label("Free:" + JF.toEng(store.free)));
        panel.add(row);

        row = new Row();
        ProgressBar bar = new ProgressBar(Component.HORIZONTAL, 100.0f, 16);
        bar.setLevels(80, 90, 100);
        bar.setColors(Color.green, Color.yellow, Color.red);
        bar.setValue(store.percent);
        row.add(bar);
        panel.add(row);

        row = new Row();
        row.setBackColor(Color.blue);
        row.setHeight(3);
        panel.add(row);

      }
    }

    return panel;
  }

  public Panel clientPanel() {
    Panel panel = new Panel();
    //TODO : add options to reset config, disconnect from server, etc.
    panel.add(new Label("jfMonitor/" + Config.AppVersion + " is running in client mode."));
    panel.add(new Label("There are no options to configure here."));
    panel.add(new Label("Use the server to add resources to monitor."));
    panel.add(new Label("This Host=" + Config.current.this_host));
    return panel;
  }

  private static Object lockInstall = new Object();

  public Panel installPanel() {
    Row row;
    Column col;
    Panel panel = new Panel();
    panel.setAlign(Component.CENTER);
    Button server_next = new Button("Save");
    Button client_next = new Button("Connect");
    col = new Column();
    panel.add(col);

    col.add(new Label("jfMonitor has not been setup yet, please select client or server setup."));
    {
      InnerPanel client_panel = new InnerPanel("Client Setup");
      col.add(client_panel);
      Label msg = new Label("");
      client_panel.add(msg);
      row = new Row();
      row.add(new Label("Client Host/IP:"));
      TextField name = new TextField("");
      name.setName("name");
      row.add(name);
      client_panel.add(row);
      row = new Row();
      row.add(new Label("Server Host/IP:"));
      TextField host = new TextField("");
      host.setName("host");
      row.add(host);
      client_panel.add(row);
      row = new Row();
      row.add(new Label("Server Password:"));
      TextField pass = new TextField("");
      pass.setPassword(true);
      pass.setName("password");
//      password.setPassword(true);  //login may be blocked - need to create HTTPS server instead
      row.add(pass);
      client_panel.add(row);
      row = new Row();
      row.add(client_next);
      client_panel.add(row);
      client_next.addClickListener( (MouseEvent e, Component c) -> {
        //connect to server
        WebUIClient webclient = c.getClient();
        String nameTxt = cleanHost(name.getText());
        String hostTxt = cleanHost(host.getText());
        String passTxt = pass.getText();
        //TODO : validate fields better
        if (nameTxt.length() < 1) {
          name.setText(nameTxt);
          msg.setColor(Color.red);
          msg.setText("Name invalid");
          return;
        }
        if (hostTxt.length() < 1) {
          host.setText(hostTxt);
          msg.setColor(Color.red);
          msg.setText("Host invalid");
          return;
        }
        if (passTxt.length() < 8) {
          msg.setColor(Color.red);
          msg.setText("Password too short (8 chars min)");
          return;
        }
        msg.setColor(Color.black);
        msg.setText("Connecting...");
        client_next.setVisible(false);
        server_next.setVisible(false);
        new Thread() {
          public void run() {
            synchronized(lockInstall) {
              if (MonitorService.client != null) {
                msg.setColor(Color.red);
                msg.setText("Already connected!  Please refresh your browser!");
                return;
              }
              if (client != null) {
                msg.setColor(Color.black);
                msg.setText("Already connecting!  Please refresh your browser.");
                return;
              }
              msg.setColor(Color.black);
              msg.setText("Connecting...");
              clientSaveConfig(nameTxt, hostTxt, passTxt);
              client = new Client();
              if (!client.test()) {
                client = null;
                msg.setColor(Color.red);
                msg.setText("Connection failed!");
                client_next.setVisible(true);
                server_next.setVisible(true);
              } else {
                saveConfigMode("client");
                client = null;
                webclient.setProperty("password", passTxt);
                webclient.setPanel(clientPanel());
              }
            }
          }
        }.start();
      });
    }
    col.add(new HTMLContainer("br"));
    {
      InnerPanel server_panel = new InnerPanel("Server Setup");
      col.add(server_panel);
      Label msg = new Label("");
      msg.setColor(Color.red);
      server_panel.add(msg);

      row = new Row();
      row.add(new Label("Server Host/IP:"));
      TextField name = new TextField("");
      row.add(name);
      server_panel.add(row);

      row = new Row();
      row.add(new Label("Server Password:"));
      TextField pass = new TextField("");
      pass.setPassword(true);
      pass.setName("password");
//      password.setPassword(true);  //login may be blocked - need to create HTTPS server
      row.add(pass);
      server_panel.add(row);

      row = new Row();
      row.add(new Label("Confirm Password:"));
      TextField confirm = new TextField("");
      confirm.setPassword(true);
      confirm.setName("password");
//      password.setPassword(true);  //login may be blocked - need to create HTTPS server
      row.add(confirm);
      server_panel.add(row);

      row = new Row();
      row.add(server_next);
      server_panel.add(row);
      server_next.addClickListener( (MouseEvent e, Component c) -> {
        //save server config
        WebUIClient webclient = c.getClient();
        String nameTxt = cleanHost(name.getText());
        if (nameTxt.length() == 0) {
          name.setText(nameTxt);
          msg.setColor(Color.red);
          msg.setText("Invalid name");
          return;
        }
        String passTxt = pass.getText();
        String confirmTxt = confirm.getText();
        //TODO : validate fields better
        if (passTxt.length() < 8) {
          msg.setColor(Color.red);
          msg.setText("Password too short (8 chars min)");
          return;
        }
        if (confirmTxt.length() < 8) {
          msg.setColor(Color.red);
          msg.setText("Password too short (8 chars min)");
          return;
        }
        if (!passTxt.equals(confirmTxt)) {
          msg.setColor(Color.red);
          msg.setText("Password doesn't match");
          return;
        }
        msg.setColor(Color.black);
        msg.setText("Saving config...");
        serverSaveConfig(nameTxt, passTxt);
        saveConfigMode("server");
        webclient.setProperty("password", passTxt);
        webclient.setPanel(serverPanel(webclient));
      } );
    }
    return panel;
  }

  public void clientSaveConfig(String this_host, String server_host, String password) {
    Config.current.this_host = this_host;
    Config.current.server_host = server_host;
    Config.current.password = password;
    Config.save();
  }

  public void serverSaveConfig(String name, String password) {
    Config.current.this_host = name;
    Config.current.server_host = name;  //to allow local Client to connect
    Config.current.password = password;
    Config.save();
  }

  public void saveConfigMode(String mode) {
    Config.current.mode = mode;
    Config.save();
    switch (mode) {
      case "client": MonitorService.startClient(); break;
      case "server": MonitorService.startServer(); break;
    }
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }

  public static String toDateTime(long ts) {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(ts);
    return String.format("%d/%02d/%02d %02d:%02d:%02d",
      c.get(Calendar.YEAR),
      c.get(Calendar.MONTH) + 1,
      c.get(Calendar.DAY_OF_MONTH),
      c.get(Calendar.HOUR_OF_DAY),
      c.get(Calendar.MINUTE),
      c.get(Calendar.SECOND));
  }

  public static String numbers(String str) {
    return JF.filter(str, JF.filter_numeric);
  }
}

package service;

/** Config Service
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.vm.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class ConfigService implements WebUIHandler {
  public static String version = "0.1";
  public WebUIServer server;
  private KeyMgmt keys;
  private VMM vmm;

  public void start() {
    initSecureWebKeys();
    server = new WebUIServer();
    server.start(this, 443, keys);
    vmm = new VMM();
  }

  public void stop() {
    if (server == null) return;
    server.stop();
    server = null;
  }

  private void initSecureWebKeys() {
    String dname = "CN=jfkvm.sourceforge.net, O=server, OU=webserver, C=CA";
    String keyfile = Paths.dataPath + "/jfkvm.key";
    String password = "password";
    if (new File(keyfile).exists()) {
      //load existing keys
      keys = new KeyMgmt();
      try {
        FileInputStream fis = new FileInputStream(keyfile);
        keys.open(fis, password.toCharArray());
        fis.close();
      } catch (Exception e) {
        if (!keys.isValid()) {
          //generate random keys
          keys = KeyMgmt.create(keyfile, "webserver", dname, password);
        }
        JFLog.log(e);
      }
    } else {
      //generate random keys
      keys = KeyMgmt.create(keyfile, "webserver", dname, password);
    }
  }

  /** This class holds UI elements to be passed down to sub-panels. */
  private static class UI {
    public String user;

    public SplitPanel split;
    public Panel tasks;

    public PopupPanel confirm_popup;
    public Label confirm_message;
    public Button confirm_button;
    public Runnable confirm_action;

    public PopupPanel browse_popup;
    public Runnable browse_load;
    public Storage browse_store;
    public String browse_path;

    public PopupPanel disk_popup;

    public PopupPanel vm_network_popup;

    public PopupPanel networkvlan_popup;
    public Runnable networkvlan_init;
    public NetworkVLAN networkvlan;

    public PopupPanel device_usb_popup;
    public PopupPanel device_pci_popup;

    public Hardware hardware;  //editing VM hardware

    public void setRightPanel(Panel panel) {
      split.setRightComponent(panel);
    }
  }

  public Panel getRootPanel(WebUIClient client) {
    if (Config.current.password == null) {
      return installPanel(client);
    }
    String user = (String)client.getProperty("user");
    if (user == null) {
      return loginPanel();
    }
    Panel panel = new Panel();
    UI ui = new UI();
    ui.user = user;

    ui.confirm_popup = confirmPopupPanel(ui);
    panel.add(ui.confirm_popup);

    ui.browse_popup = browseStoragePopupPanel(ui);
    panel.add(ui.browse_popup);

    ui.disk_popup = diskPopupPanel(ui);
    panel.add(ui.disk_popup);

    ui.vm_network_popup = vm_network_PopupPanel(ui);
    panel.add(ui.vm_network_popup);

    ui.networkvlan_popup = network_networkvlan_PopupPanel(ui);
    panel.add(ui.networkvlan_popup);

    ui.device_usb_popup = device_usb_PopupPanel(ui);
    panel.add(ui.device_usb_popup);

    ui.device_pci_popup = device_pci_PopupPanel(ui);
    panel.add(ui.device_pci_popup);

    int topSize = client.getHeight() - 128;
    SplitPanel top_bot = new SplitPanel(SplitPanel.HORIZONTAL);
    panel.add(top_bot);
    top_bot.setDividerPosition(topSize);

    int leftSize = 128;
    SplitPanel left_right = new SplitPanel(SplitPanel.VERTICAL);
    ui.split = left_right;
    left_right.setDividerPosition(leftSize);
    left_right.setLeftComponent(leftPanel(ui, leftSize));
    left_right.setRightComponent(rightPanel());

    ui.tasks = tasksPanel(ui);

    top_bot.setTopComponent(left_right);
    top_bot.setBottomComponent(ui.tasks);

/*
    b_help.addClickListener((MouseEvent e, Component button) -> {
        client.openURL("http://jfkvm.sourceforge.net/help.html");
      });
*/

    return top_bot;
  }

  private Panel tasksPanel(UI ui) {
    Panel panel = new Panel();
    panel.add(new Label("Tasks panel"));

    return panel;
  }

  private PopupPanel confirmPopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Confirm");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Label popup_msg = new Label("Message");
    panel.add(popup_msg);
    Label popup_label = new Label("Are you sure?");
    panel.add(popup_label);
    Button popup_b_action = new Button("Action");
    panel.add(popup_b_action);
    popup_b_action.addClickListener((MouseEvent e, Component button) -> {
      if (ui == null || ui.user == null) return;
      if (ui.confirm_action != null) {
        ui.confirm_action.run();
      }
    });
    Button popup_b_cancel = new Button("Cancel");
    panel.add(popup_b_cancel);
    popup_b_cancel.addClickListener((MouseEvent e, Component button) -> {
      if (ui == null || ui.user == null) return;
      panel.setVisible(false);
    });
    ui.confirm_message = popup_msg;
    ui.confirm_button = popup_b_action;
    return panel;
  }

  private PopupPanel diskPopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Disk");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Name"));
    TextField name = new TextField("sda");
    row.add(name);

    row = new Row();
    panel.add(row);
    row.add(new Label("Format"));
    ComboBox type = new ComboBox();
    row.add(type);
    type.add("qcow2", "qcow2");
    type.add("vmdk", "vmdk");

    row = new Row();
    panel.add(row);
    row.add(new Label("Size"));
    TextField size = new TextField("100");
    row.add(size);
    ComboBox size_units = new ComboBox();
    size_units.add("MB", "MB");
    size_units.add("GB", "GB");
    row.add(size_units);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //TODO : add button methods

    return panel;
  }

  private PopupPanel vm_network_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Network");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Model"));
    ComboBox type = new ComboBox();
    row.add(type);
    type.add("vmxnet3", "vmxnet3");
    type.add("virtio", "virtio");
    type.add("e1000", "e1000");
    type.add("e1000e", "e1000e");

    row = new Row();
    panel.add(row);
    row.add(new Label("Network"));
    ComboBox networks = new ComboBox();
    row.add(networks);
    ArrayList<NetworkVLAN> nics = Config.current.vlans;
    for(NetworkVLAN nic : nics) {
      networks.add(nic.name, nic.name);
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //TODO : add button methods

    return panel;
  }

  private PopupPanel network_networkvlan_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Network VLAN");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Name"));
    TextField name = new TextField("");
    row.add(name);

    row = new Row();
    panel.add(row);
    row.add(new Label("Switch"));
    ComboBox bridge = new ComboBox();
    row.add(bridge);

    row = new Row();
    panel.add(row);
    row.add(new Label("VLAN"));
    TextField vlan = new TextField("");
    row.add(vlan);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.networkvlan_init = new Runnable() {
      public void run() {
        bridge.clear();
        NetworkBridge[] nics = NetworkBridge.list(NetworkBridge.TYPE_OS);
        for(NetworkBridge nic : nics) {
          bridge.add(nic.name, nic.name);
        }
        if (ui.networkvlan == null) {
          name.setText("");
          vlan.setText("0");
        } else {
          name.setText(ui.networkvlan.name);
          vlan.setText(Integer.toString(ui.networkvlan.vlan));
          int idx = 0;
          for(NetworkBridge nic : nics) {
            if (nic.name.equals(ui.networkvlan.bridge)) {
              bridge.setSelectedIndex(idx);
              break;
            }
          }
        }
      }
    };

    accept.addClickListener((me, cmp) -> {
      String _name = vmm.cleanName(vlan.getText());
      if (_name.length() == 0) {
        name.setText(_name);
        name.setBackColor(Color.red);
        return;
      }
      String _bridge = bridge.getSelectedText();
      if (_bridge == null || _bridge.length() == 0) {
        bridge.setBackColor(Color.red);
        return;
      }
      int _vlan = JF.atoi(vlan.getText());
      if (_vlan < 0 || _vlan > 4095) {
        vlan.setBackColor(Color.red);
        return;
      }
      Config.current.addNetworkVLAN(new NetworkVLAN(_name, _bridge, _vlan));
      ui.networkvlan_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.networkvlan_popup.setVisible(false);
    });

    return panel;
  }

  private PopupPanel device_usb_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("USB Device");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Device"));
    ComboBox type = new ComboBox();
    row.add(type);
    String[] groups = vmm.listDevices(Device.TYPE_USB);
    for(String group : groups) {
      type.add(group, group);
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //TODO : add button methods

    return panel;
  }

  private PopupPanel device_pci_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("PCI Device");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Device"));
    ComboBox type = new ComboBox();
    row.add(type);
    String[] groups = vmm.listDevices(Device.TYPE_PCI);
    for(String group : groups) {
      type.add(group, group);
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //TODO : add button methods

    return panel;
  }

  public Panel installPanel(WebUIClient client) {
    Panel panel = new Panel();
    panel.removeClass("column");
    InnerPanel inner = new InnerPanel("jfKVM Setup");
    inner.setAutoWidth();
    inner.setAutoHeight();
    panel.setAlign(Component.CENTER);
    Row row;
    Label header = new Label("jfKVM has not been setup yet, please supply the admin password.");
    inner.add(header);
    Label msg = new Label("");
    inner.add(msg);

    row = new Row();
    inner.add(row);
    row.add(new Label("Password:"));
    TextField password = new TextField("");
    row.add(password);

    row = new Row();
    inner.add(row);
    row.add(new Label("Confirm:"));
    TextField confirm = new TextField("");
    row.add(confirm);

    row = new Row();
    inner.add(row);
    Button login = new Button("Save");
    row.add(login);

    login.addClickListener( (MouseEvent m, Component c) -> {
      if (Config.current.password != null) {
        msg.setText("Already configured, please refresh!");
        msg.setColor(Color.red);
        return;
      }
      String passTxt1 = password.getText();
      String passTxt2 = confirm.getText();
      if (passTxt1.length() >= 8 && passTxt1.equals(passTxt2)) {
        Config.current.password = passTxt1;
        Config.current.save();
        client.setPanel(getRootPanel(client));
      } else {
        msg.setText("Password too short (min 8)");
        msg.setColor(Color.red);
      }
    });
    panel.add(inner);
    return panel;
  }

  private Panel loginPanel() {
    Panel panel = new Panel();
    panel.removeClass("column");
    InnerPanel inner = new InnerPanel("jfKVM Login");
    inner.setAutoWidth();
    inner.setAutoHeight();
    panel.setAlign(Component.CENTER);
    Row row;
    Label msg = new Label("");
    inner.add(msg);

    row = new Row();
    inner.add(row);
    row.add(new Label("Username:"));
    TextField username = new TextField("");
    row.add(username);

    row = new Row();
    inner.add(row);
    row.add(new Label("Password:"));
    TextField password = new TextField("");
    row.add(password);

    row = new Row();
    inner.add(row);
    Button login = new Button("Login");
    row.add(login);

    login.addClickListener( (MouseEvent m, Component c) -> {
      String userTxt = username.getText();
      String passTxt = password.getText();
      WebUIClient webclient = c.getClient();
      if (passTxt.equals(Config.current.password)) {
        webclient.setProperty("user", userTxt);
        webclient.setPanel(getRootPanel(webclient));
      } else {
        msg.setText("Wrong password");
        msg.setColor(Color.red);
      }
    });
    panel.add(inner);
    return panel;
  }

  private Panel leftPanel(UI ui, int size) {
    Panel panel = new Panel();
    ListBox list = new ListBox();
    panel.add(list);
    Button host = new Button("Host");
    host.setWidth(size);
    list.add(host);
    Button vms = new Button("Virtual Machines");
    vms.setWidth(size);
    list.add(vms);
    Button stores = new Button("Storage");
    stores.setWidth(size);
    list.add(stores);
    Button networks = new Button("Network");
    networks.setWidth(size);
    list.add(networks);
    host.addClickListener((me, cmp) -> {
      ui.split.setRightComponent(hostPanel());
    });
    vms.addClickListener((me, cmp) -> {
      ui.split.setRightComponent(vmsPanel(ui));
    });
    stores.addClickListener((me, cmp) -> {
      ui.split.setRightComponent(storagePanel(ui));
    });
    networks.addClickListener((me, cmp) -> {
      ui.split.setRightComponent(networkPanel(ui));
    });
    return panel;
  }

  private Panel rightPanel() {
    Panel panel = new Panel();
    panel.add(new Label("jfKVM/" + version));
    return panel;
  }

  private Panel hostPanel() {
    TabPanel panel = new TabPanel();
    panel.addTab(hostInfoPanel(), "Info");
    panel.addTab(hostConfigPanel(), "Settings");
    return panel;
  }

  private Panel hostInfoPanel() {
    Panel panel = new Panel();
    panel.add(new Label("jfKVM/" + version));
    //see : https://libvirt.org/html/libvirt-libvirt-host.html
    //TODO : add more details (cpu load, memory load, etc.)
    return panel;
  }

  private Panel hostConfigPanel() {
    Panel panel = new Panel();
    panel.add(new Label("jfKVM/" + version));
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Host FQN"));
    TextField host = new TextField(Config.current.fqn);
    row.add(host);

    row = new Row();
    panel.add(row);
    row.add(new Label("Default iSCSI Initiator IQN"));
    TextField client_iqn = new TextField(Config.current.iqn);
    row.add(client_iqn);
    Button client_iqn_generate = new Button("Generate");
    row.add(client_iqn_generate);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button save = new Button("Save");
    tools.add(save);

    row = new Row();
    panel.add(row);
    Label msg = new Label("");
    row.add(msg);

    save.addClickListener((me, cmp) -> {
      Config.current.fqn = client_iqn.getText();
      Config.current.iqn = client_iqn.getText();
      Config.current.save();
      msg.setText("Settings saved!");
    });

    client_iqn_generate.addClickListener((me, cmp) -> {
      Config.current.fqn = client_iqn.getText();
      client_iqn.setText(IQN.generate(Config.current.fqn));
      msg.setText("Client IQN regenerated");
    });

    return panel;
  }

  private Panel vmsPanel(UI ui) {
    TabPanel panel = new TabPanel();
    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button create = new Button("Create");
    tools.add(create);
    Button edit = new Button("Edit");
    tools.add(edit);
    Button start = new Button("Start");
    tools.add(start);
    Button stop = new Button("Stop");
    tools.add(stop);
    Button poweroff = new Button("PowerOff");
    tools.add(poweroff);
    Button unreg = new Button("Unregister");
    tools.add(unreg);

    ListBox list = new ListBox();
    //TODO : add more details
    String[] vms = vmm.listVMs();
    for(String vm : vms) {
      list.add(vm);
    }

    create.addClickListener((me, cmp) -> {
      ui.split.setRightComponent(vmEditPanel(new Hardware(), true, ui));
    });

    edit.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String vmname = list.getSelectedItem();
      String pool = vmm.getVMPool(vmname);
      Hardware hardware = vmm.loadHardware(pool, vmname);
      if (hardware == null) {
        JFLog.log("Error:Failed to load config for vm:" + vmname);
        return;
      }
      ui.split.setRightComponent(vmEditPanel(hardware, false, ui));
    });

    start.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String name = list.getSelectedItem();
      ui.confirm_button.setText("Start");
      ui.confirm_message.setText("Start VM : " + name);
      ui.confirm_action = new Runnable() {
        public void run() {
          ui.confirm_popup.setVisible(false);
          Task task = new Task("Start") {
            public void doTask() {
              VirtualMachine vm = VirtualMachine.get(name);
              if (vm == null) {
                setResult("Error:VM not found:" + name);
                return;
              }
              if (vm.start()) {
                setResult("Completed");
              } else {
                setResult("Error occured, see logs.");
              }
            }
          };
          KVMService.tasks.addTask(task);
        }
      };
      ui.confirm_popup.setVisible(true);
    });

    //stop
    stop.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String name = list.getSelectedItem();
      ui.confirm_button.setText("Stop");
      ui.confirm_message.setText("Stop VM : " + name);
      ui.confirm_action = new Runnable() {
        public void run() {
          ui.confirm_popup.setVisible(false);
          Task task = new Task("Stop") {
            public void doTask() {
              VirtualMachine vm = VirtualMachine.get(name);
              if (vm == null) {
                setResult("Error:VM not found:" + name);
                return;
              }
              if (vm.stop()) {
                setResult("Completed");
              } else {
                setResult("Error occured, see logs.");
              }
            }
          };
          KVMService.tasks.addTask(task);
        }
      };
      ui.confirm_popup.setVisible(true);
    });

    //poweroff
    poweroff.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String name = list.getSelectedItem();
      ui.confirm_button.setText("Power Off");
      ui.confirm_message.setText("Power Off VM : " + name);
      ui.confirm_action = new Runnable() {
        public void run() {
          ui.confirm_popup.setVisible(false);
          Task task = new Task("Power Off") {
            public void doTask() {
              VirtualMachine vm = VirtualMachine.get(name);
              if (vm == null) {
                setResult("Error:VM not found:" + name);
                return;
              }
              if (vm.poweroff()) {
                setResult("Completed");
              } else {
                setResult("Error occured, see logs.");
              }
            }
          };
          KVMService.tasks.addTask(task);
        }
      };
      ui.confirm_popup.setVisible(true);
    });

    unreg.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String name = list.getSelectedItem();
      ui.confirm_button.setText("Unregister");
      ui.confirm_message.setText("Unregister VM : " + name);
      ui.confirm_action = new Runnable() {
        public void run() {
          ui.confirm_popup.setVisible(false);
          Task task = new Task("Unregister") {
            public void doTask() {
              VirtualMachine vm = VirtualMachine.get(name);
              if (vm == null) {
                setResult("Error:VM not found:" + name);
                return;
              }
              if (vm.unregister()) {
                setResult("Completed");
              } else {
                setResult("Error occured, see logs.");
              }
            }
          };
          KVMService.tasks.addTask(task);
        }
      };
      ui.confirm_popup.setVisible(true);
    });

    return panel;
  }

  private Panel vmEditPanel(Hardware hardware, boolean create, UI ui) {
    Panel panel = new Panel();
    ui.hardware = hardware;
    Row row;
    //name [   ]
    row = new Row();
    panel.add(row);
    row.add(new Label("Name"));
    TextField name = new TextField(hardware.name);
    row.add(name);
    //pool [  v]
    row = new Row();
    panel.add(row);
    row.add(new Label("Storage Pool"));
    ComboBox pool = new ComboBox();
    row.add(pool);
    String[] pools = vmm.listPools();
    for(String p : pools) {
      pool.add(p, p);
    }
    //memory [   ] [MB/GB]
    row = new Row();
    panel.add(row);
    row.add(new Label("Memory"));
    TextField memory = new TextField("4");
    row.add(memory);
    ComboBox memory_units = new ComboBox();
    memory_units.add("MB", "MB");
    memory_units.add("GB", "GB");
    row.add(memory_units);
    //cpus [   ]
    row = new Row();
    panel.add(row);
    row.add(new Label("CPU Cores"));
    TextField cpus = new TextField("2");
    row.add(cpus);
    //firmware [BIOS/UEFI]
    row = new Row();
    panel.add(row);
    row.add(new Label("Firmware"));
    ComboBox firmware = new ComboBox();
    firmware.add("BIOS", "BIOS");
    firmware.add("UEFI", "UEFI");
    row.add(firmware);
    //disks
    InnerPanel disks = new InnerPanel("Disks");
    panel.add(disks);
    ToolBar disk_ops = new ToolBar();
    disks.add(disk_ops);
    Button b_disk_create = new Button("Create");
    disk_ops.add(b_disk_create);
    Button b_disk_add = new Button("Add");
    disk_ops.add(b_disk_add);
    Button b_disk_delete = new Button("Delete");
    disk_ops.add(b_disk_delete);
    ListBox disk_list = new ListBox();
    disks.add(disk_list);
    for(Disk disk : hardware.disks) {
      //TODO : add more details
      disk_list.add(disk.name);
    }
    //networks
    InnerPanel networks = new InnerPanel("Network");
    panel.add(networks);
    ToolBar net_ops = new ToolBar();
    networks.add(net_ops);
    Button b_net_add = new Button("Add");
    net_ops.add(b_net_add);
    Button b_net_delete = new Button("Delete");
    net_ops.add(b_net_delete);
    ListBox net_list = new ListBox();
    networks.add(net_list);
    int idx = 0;
    for(Network nic : hardware.networks) {
      //TODO : add more details
      net_list.add("eth" + idx);
      idx++;
    }
    //devices
    InnerPanel devices = new InnerPanel("Devices");
    panel.add(devices);
    ToolBar dev_ops = new ToolBar();
    devices.add(dev_ops);
    Button b_dev_add_usb = new Button("Add USB");
    dev_ops.add(b_dev_add_usb);
    Button b_dev_add_pci = new Button("Add PCI");
    dev_ops.add(b_dev_add_pci);
    Button b_dev_delete = new Button("Delete");
    dev_ops.add(b_dev_delete);
    ListBox dev_list = new ListBox();
    devices.add(dev_list);
    for(Device dev : hardware.devices) {
      //TODO : add more details
      dev_list.add(dev.name);
    }
    //save / cancel
    row = new Row();
    panel.add(row);
    Button b_save = new Button("Save");
    row.add(b_save);
    Button b_cancel = new Button("Cancel");
    row.add(b_cancel);

    b_disk_create.addClickListener((me, cmp) -> {
      //TODO : setup panel
      ui.disk_popup.setVisible(true);
    });
    b_disk_add.addClickListener((me, cmp) -> {
      //TODO : setup panel
      ui.browse_popup.setVisible(true);
    });
    b_disk_delete.addClickListener((me, cmp) -> {
      String disk_name = "";  //TODO
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Disk : " + disk_name);
      ui.confirm_action = new Runnable() {
        public void run() {
          //TODO
          ui.confirm_popup.setVisible(false);
        }
      };
      ui.confirm_popup.setVisible(true);
    });
    b_net_add.addClickListener((me, cmp) -> {
      //TODO : setup panel
      ui.vm_network_popup.setVisible(true);
    });
    b_net_delete.addClickListener((me, cmp) -> {
      String network_name = "";  //TODO
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Disk : " + network_name);
      ui.confirm_action = new Runnable() {
        public void run() {
          //TODO
          ui.confirm_popup.setVisible(false);
        }
      };
      ui.confirm_popup.setVisible(true);
    });
    b_dev_add_usb.addClickListener((me, cmp) -> {
      //TODO : setup panel
      ui.device_usb_popup.setVisible(true);
    });
    b_dev_add_pci.addClickListener((me, cmp) -> {
      //TODO : setup panel
      ui.device_pci_popup.setVisible(true);
    });
    b_dev_delete.addClickListener((me, cmp) -> {
      String device_name = "";  //TODO
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Device : " + device_name);
      ui.confirm_action = new Runnable() {
        public void run() {
          //TODO
          ui.confirm_popup.setVisible(false);
        }
      };
      ui.confirm_popup.setVisible(true);
    });

    return panel;
  }

  private Panel storagePanel(UI ui) {
    Panel panel = new Panel();

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button add = new Button("Add");
    tools.add(add);
    Button edit = new Button("Edit");
    tools.add(edit);
    Button browse = new Button("Edit");
    tools.add(browse);
    Button format = new Button("Format");
    tools.add(format);
    Button delete = new Button("Delete");
    tools.add(delete);
    ListBox list = new ListBox();
    String[] pools = vmm.listPools();
    for(String pool : pools) {
      list.add(pool);
    }

    add.addClickListener((me, cmp) -> {
      ui.split.setRightComponent(addStoragePanel(ui));
    });
    edit.addClickListener((me, cmp) -> {
      String name = list.getSelectedItem();
      if (name == null) return;
      Storage pool = vmm.getPoolByName(name);
      ui.split.setRightComponent(editStoragePanel(pool, ui));
    });
    browse.addClickListener((me, cmp) -> {
      String name = list.getSelectedItem();
      if (name == null) return;
      Storage pool = vmm.getPoolByName(name);
      ui.browse_store = pool;
      ui.browse_load.run();
      ui.browse_popup.setVisible(true);
    });
    format.addClickListener((me, cmp) -> {
      //TODO : format storage pool (iscsi/local only)
      //start task
    });
    delete.addClickListener((me, cmp) -> {
      //TODO : delete storage
      //start task
    });

    return panel;
  }

  private Panel addStoragePanel(UI ui) {
    Panel panel = new Panel();
    Label desc = new Label("New Storage Pool");
    panel.add(desc);
    Row row;

    //name
    row = new Row();
    panel.add(row);
    row.add(new Label("Name:"));
    TextField name = new TextField("");
    row.add(name);

    //type
    row = new Row();
    panel.add(row);
    row.add(new Label("Type:"));
    ComboBox type = new ComboBox();
    type.add("nfs", "NFS");
    type.add("iscsi", "iSCSI");
    type.add("local", "Local");

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button next = new Button("Next");
    tools.add(next);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    next.addClickListener((me, cmp) -> {
      String storagename = vmm.cleanName(name.getText());
      if (storagename.length() == 0) {
        name.setText(storagename);
        name.setBackColor(Color.red);
        return;
      }
      switch (type.getSelectedValue()) {
        case "nfs":
          ui.setRightPanel(nfs_StoragePanel(new Storage(Storage.TYPE_NFS, storagename, null), ui));
          break;
        case "iscsi":
          ui.setRightPanel(iscsi_StoragePanel(new Storage(Storage.TYPE_ISCSI, storagename, null), ui));
          break;
        case "local":
          ui.setRightPanel(local_StoragePanel(new Storage(Storage.TYPE_LOCAL, storagename, null), ui));
          break;
      }
    });
    cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });

    return panel;
  }

  private Panel editStoragePanel(Storage store, UI ui) {
    switch (store.type) {
      case Storage.TYPE_NFS: return nfs_StoragePanel(store, ui);
      case Storage.TYPE_ISCSI: return iscsi_StoragePanel(store, ui);
      case Storage.TYPE_LOCAL: return local_StoragePanel(store, ui);
    }
    return null;
  }

  private Panel nfs_StoragePanel(Storage store, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + store.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Host:"));
    TextField host = new TextField("");
    row.add(host);

    row = new Row();
    panel.add(row);
    row.add(new Label("Source Path:"));
    TextField path = new TextField("");
    row.add(path);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //TODO : button methods

    return panel;
  }

  private Panel iscsi_StoragePanel(Storage store, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + store.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Host:"));
    TextField host = new TextField("");
    row.add(host);

    row = new Row();
    panel.add(row);
    row.add(new Label("Target IQN:"));
    TextField target = new TextField("");
    row.add(target);

    row = new Row();
    panel.add(row);
    row.add(new Label("Initiator IQN:"));
    TextField initiator = new TextField("");
    row.add(initiator);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //TODO : button methods

    return panel;
  }

  private Panel local_StoragePanel(Storage store, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + store.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Device:"));
    TextField dev = new TextField("");
    row.add(dev);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //TODO : button methods

    return panel;
  }

  private PopupPanel browseStoragePopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Browse");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;
    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button cdup = new Button("UP");  //TODO : icons
    tools.add(cdup);
    Button delete = new Button("Delete");
    tools.add(delete);

    row = new Row();
    panel.add(row);
    TextField path = new TextField("");
    row.add(path);
    path.setReadonly(true);

    ListBox list = new ListBox();
    panel.add(list);

    ui.browse_load = new Runnable() { public void run() {
      //TODO : list folder
      path.setText(ui.browse_path);
      list.removeAll();
      File[] files = new File(ui.browse_path).listFiles();
      for(File file : files) {
        String name = file.getName();
        if (file.isDirectory()) {
          list.add("/" + name);
        } else {
          list.add(name);
        }
      }
    }};
    ui.browse_path = "/volumes";

    refresh.addClickListener((me, cmp) -> {
      ui.browse_load.run();
    });
    cdup.addClickListener((me, cmp) -> {
      int idx = ui.browse_path.lastIndexOf('/');
      if (idx == -1 || idx == 0) return;
      ui.browse_path = ui.browse_path.substring(0, idx);
      ui.browse_load.run();
    });
    delete.addClickListener((me, cmp) -> {
      String item = list.getSelectedItem();
      if (item == null) return;
      if (item.startsWith("/")) {
        ui.confirm_message.setText("Delete folder:" + item);
        ui.confirm_action = new Runnable() {public void run() {
          new File(ui.browse_path + item).delete();
          ui.browse_load.run();
        }};
      } else {
        ui.confirm_message.setText("Delete file:" + item);
        ui.confirm_action = new Runnable() {public void run() {
          new File(ui.browse_path + "/" + item).delete();
          ui.browse_load.run();
        }};
      }
    });
    list.addClickListener((me, cmp) -> {
      String item = list.getSelectedItem();
      if (item == null) return;
      if (item.startsWith("/")) {
        ui.browse_path += item;
        ui.browse_load.run();
      }
    });

    return panel;
  }

  private Panel networkPanel(UI ui) {
    TabPanel panel = new TabPanel();
    networkPanel_networkvlans(panel, ui);
    networkPanel_bridges(panel, ui);
    networkPanel_nics(panel, ui);
    networkPanel_phys(panel, ui);
    return panel;
  }

  private void networkPanel_phys(TabPanel panel, UI ui) {
    {
      Panel phys = new Panel();
      panel.addTab(phys, "Physical NICs");
      ToolBar tools = new ToolBar();
      phys.add(tools);
      Button phys_view = new Button("View");
      tools.add(phys_view);
      ListBox phys_list = new ListBox();
      phys.add(phys_list);
      NetworkInterface[] nics = vmm.listNetworkInterface();
      for(NetworkInterface nic : nics) {
        if (nic.name.equals("lo")) continue;
        phys_list.add(nic.name);
      }
      //TODO : button methods
    }
  }

  private void networkPanel_bridges(TabPanel panel, UI ui) {
    //bridges (virtual switches)
    {
      Panel virt = new Panel();
      panel.addTab(virt, "Virtual Switches");
      ToolBar tools = new ToolBar();
      virt.add(tools);
      Button create = new Button("Create");
      tools.add(create);
      Button edit = new Button("Edit");
      tools.add(edit);
      Button delete = new Button("Delete");
      tools.add(delete);
      virt.add(new Label("NOTE : 'os' bridges are required for VLAN tagging guest networks. Please convert 'br' bridges if present."));
      ListBox list = new ListBox();
      virt.add(list);
      NetworkBridge[] nics = NetworkBridge.list();
      for(NetworkBridge nic : nics) {
        list.add(nic.name + ":" + nic.type + ":" + nic.iface);
      }
      //TODO : button methods
    }
  }

  private void networkPanel_networkvlans(TabPanel panel, UI ui) {
    //network VLANs
    {
      Panel ports = new Panel();
      panel.addTab(ports, "Networks");
      ToolBar tools = new ToolBar();
      ports.add(tools);
      Button create = new Button("Create");
      tools.add(create);
      Button edit = new Button("Edit");
      tools.add(edit);
      Button delete = new Button("Delete");
      tools.add(delete);
      ListBox list = new ListBox();
      ports.add(list);
      ArrayList<NetworkVLAN> nics = Config.current.vlans;
      for(NetworkVLAN nic : nics) {
        list.add(nic.name + ":" + nic.vlan);
      }
      //TODO : button methods
      create.addClickListener((me, cmp) -> {
        ui.networkvlan_popup.setVisible(true);
      });
    }
  }

  private void networkPanel_nics(TabPanel panel, UI ui) {
    //vm host/server nics
    {
      Panel vmnics = new Panel();
      panel.addTab(vmnics, "Server Virtual NICs");
      ToolBar tools = new ToolBar();
      vmnics.add(tools);
      Button create = new Button("Create");
      tools.add(create);
      Button edit = new Button("Edit");
      tools.add(edit);
      Button delete = new Button("Delete");
      tools.add(delete);
      ListBox list = new ListBox();
      vmnics.add(list);
      ArrayList<NetworkVirtual> nics = Config.current.nics;
      for(NetworkVirtual nic : nics) {
        list.add(nic.name);
      }
      //TODO : button methods
    }
  }

  public byte[] getResource(String url) {
    //url = /user/hash/component_id/count
    String pts[] = url.split("/");
    String hash = pts[2];
    WebUIClient client = server.getClient(hash);
    if (client == null) {
      JFLog.log("ConfigServer.getResouce() : WebUIClient not found for hash:" + hash);
      return null;
    }
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}

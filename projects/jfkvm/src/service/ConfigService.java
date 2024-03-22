package service;

/** Config Service
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.vm.*;
import javaforce.net.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class ConfigService implements WebUIHandler {
  public static String version = "0.1";
  public WebUIServer server;
  private KeyMgmt keys;
  private VMM vmm;

  private static final String[] filter_disks = new String[] {
    ".*[.]vmdk",
    ".*[.]qcow2",
    ".*[.]iso",
  };

  private static final String[] filter_all = new String[] {
    ".*[.]vmdk",
    ".*[.]qcow2",
    ".*[.]iso",
    ".*[.]jfvm"
  };

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
    public String browse_select;
    public String[] browse_filters;
    public Runnable browse_complete;

    public PopupPanel vm_disk_popup;
    public Runnable vm_disk_init;
    public Disk vm_disk;
    public Runnable vm_disk_complete;

    public PopupPanel vm_network_popup;
    public Runnable vm_network_init;
    public Network vm_network;
    public Runnable vm_network_complete;

    public PopupPanel network_vlan_popup;
    public Runnable network_vlan_init;
    public NetworkVLAN network_vlan;
    public Runnable network_vlan_complete;

    public PopupPanel network_bridge_popup;
    public Runnable network_bridge_init;
    public NetworkBridge network_bridge;
    public Runnable network_bridge_complete;

    public PopupPanel network_virtual_popup;
    public Runnable network_virtual_init;
    public NetworkVirtual network_virtual;
    public Runnable network_virtual_complete;

    public Device device;
    public PopupPanel device_usb_popup;
    public Runnable device_usb_init;
    public PopupPanel device_pci_popup;
    public Runnable device_pci_init;
    public Runnable device_complete;

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

    ui.message_popup = messagePopupPanel(ui);
    panel.add(ui.message_popup);

    ui.confirm_popup = confirmPopupPanel(ui);
    panel.add(ui.confirm_popup);

    ui.browse_popup = browseStoragePopupPanel(ui);
    panel.add(ui.browse_popup);

    ui.vm_disk_popup = vm_disk_PopupPanel(ui);
    panel.add(ui.vm_disk_popup);

    ui.vm_network_popup = vm_network_PopupPanel(ui);
    panel.add(ui.vm_network_popup);

    ui.network_vlan_popup = network_vlan_PopupPanel(ui);
    panel.add(ui.network_vlan_popup);

    ui.network_bridge_popup = network_bridge_PopupPanel(ui);
    panel.add(ui.network_bridge_popup);

    ui.network_virtual_popup = network_virtual_PopupPanel(ui);
    panel.add(ui.network_virtual_popup);

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

    return panel;
  }

  private Panel tasksPanel(UI ui) {
    Panel panel = new Panel();
    panel.add(new Label("Tasks panel"));

    //TODO : create list of running tasks

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
    return panel;
  }

  private PopupPanel vm_disk_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Disk");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Name"));
    TextField name = new TextField("");
    row.add(name);

    row = new Row();
    panel.add(row);
    row.add(new Label("Format"));
    ComboBox type = new ComboBox();
    row.add(type);
    type.add("vmdk", "vmdk");
    type.add("qcow2", "qcow2");
    type.add("iso", "iso");

    row = new Row();
    panel.add(row);
    row.add(new Label("Size"));
    TextField size = new TextField("100");
    row.add(size);
    ComboBox size_units = new ComboBox();
    //size_units.add("B", "B");
    //size_units.add("KB", "KB");
    size_units.add("MB", "MB");
    size_units.add("GB", "GB");
    row.add(size_units);

    row = new Row();
    panel.add(row);
    row.add(new Label("Provision"));
    ComboBox provision = new ComboBox();
    provision.add("thick", "Thick");
    provision.add("thin", "Thin");
    row.add(provision);
    provision.setSelectedIndex(0);

    row = new Row();
    panel.add(row);
    row.add(new Label("Boot Order"));
    TextField boot_order = new TextField("0");
    row.add(boot_order);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.vm_disk_init = () -> {
      errmsg.setText("");
      if (ui.vm_disk == null) {
        //create
        name.setText(ui.hardware.getNextDiskName());
        name.setReadonly(false);
        type.setSelectedIndex(0);
        type.setReadonly(false);
        size.setText("100");
        size_units.setSelectedIndex(1);
        size.setReadonly(false);
        size_units.setReadonly(false);
        provision.setSelectedIndex(0);
        boot_order.setText("0");
        accept.setText("Create");
      } else {
        //update
        name.setText(ui.vm_disk.name);
        name.setReadonly(true);
        switch (ui.vm_disk.type) {
          case Disk.TYPE_VMDK: type.setSelectedIndex(0); break;
          case Disk.TYPE_QCOW2: type.setSelectedIndex(1); break;
          case Disk.TYPE_ISO: type.setSelectedIndex(2); break;
        }
        type.setReadonly(true);
        size.setText(Integer.toString(ui.vm_disk.size.size));
        size_units.setSelectedIndex(ui.vm_disk.size.unit - 2);
        if (ui.vm_disk.type == Disk.TYPE_ISO) {
          size.setReadonly(true);
          size_units.setReadonly(true);
        } else {
          size.setReadonly(false);
          size_units.setReadonly(false);
        }
        provision.setSelectedIndex(0);
        boot_order.setText(Integer.toString(ui.vm_disk.boot_order));
        accept.setText("Edit");
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      boolean create = ui.vm_disk == null;
      int _type = type.getSelectedIndex();
      if (create && type.getSelectedIndex() == 2) {
        errmsg.setText("Error:can not create iso files");
        return;
      }
      String _name = vmm.cleanName(name.getText());
      if (_name.length() == 0) {
        name.setText(_name);
        errmsg.setText("Error:invalid name");
        return;
      }
      if (create) {
        if (ui.hardware.hasDisk(_name)) {
          errmsg.setText("Error:disk name already exists");
          return;
        }
      }
      String _size_str = vmm.cleanNumber(size.getText());
      if (_size_str.length() == 0) {
        size.setText(_size_str);
        errmsg.setText("Error:invalid size");
        return;
      }
      int _size = Integer.valueOf(_size_str);
      if (_type != Disk.TYPE_ISO) {
        if (_size == 0) {
          errmsg.setText("Error:invalid size");
          return;
        }
      }
      int _size_unit = size_units.getSelectedIndex() + 2;
      String _boot_order_str = vmm.cleanNumber(boot_order.getText());
      if (_boot_order_str.length() == 0) {
        boot_order.setText(_boot_order_str);
        errmsg.setText("Error:invalid boot order");
        return;
      }
      int _boot_order = Integer.valueOf(_boot_order_str);
      if (create) {
        //create
        ui.vm_disk = new Disk();
        ui.vm_disk.pool = ui.hardware.pool;
        ui.vm_disk.folder = ui.hardware.name;
        ui.vm_disk.name = _name;
        ui.vm_disk.type = _type;
        ui.vm_disk.size = new Size(_size, _size_unit);
        ui.vm_disk.boot_order = _boot_order;
        if (!ui.vm_disk.create(vmm.getPoolByName(ui.hardware.pool), provision.getSelectedIndex())) {
          errmsg.setText("Error:Failed to create disk");
          return;
        }
      } else {
        //update (only size and boot order can be changed)
        ui.vm_disk.size = new Size(_size, _size_unit);
        ui.vm_disk.resize(vmm.getPoolByName(ui.hardware.pool));
        ui.vm_disk.boot_order = _boot_order;
      }
      if (ui.vm_disk_complete != null) {
        ui.vm_disk_complete.run();
      }
      ui.vm_disk_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.vm_disk_popup.setVisible(false);
    });

    return panel;
  }

  private static String[] nic_models = {
    "vmxnet3",
    "virtio",
    "e1000",
    "e1000e",
  };

  private PopupPanel vm_network_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Network");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Model"));
    ComboBox models = new ComboBox();
    row.add(models);
    for(String model : nic_models) {
      models.add(model, model);
    }

    row = new Row();
    panel.add(row);
    row.add(new Label("Network"));
    ComboBox networks = new ComboBox();
    row.add(networks);

    row = new Row();
    panel.add(row);
    row.add(new Label("MAC"));
    TextField mac = new TextField("");
    row.add(mac);
    row.add(new Label("(leave blank to generate)"));

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.vm_network_init = () -> {
      boolean create = ui.vm_network == null;
      networks.clear();
      ArrayList<NetworkVLAN> nics = Config.current.vlans;
      for(NetworkVLAN nic : nics) {
        networks.add(nic.name, nic.name);
      }
      if (create) {
        models.setSelectedIndex(0);
        networks.setSelectedIndex(0);
        mac.setText("");
        accept.setText("Create");
      } else {
        int idx = 0;
        for(String model : nic_models) {
          if (model.equals(ui.vm_network.model)) {
            models.setSelectedIndex(idx);
            break;
          }
          idx++;
        }
        idx = 0;
        for(NetworkVLAN nic : nics) {
          if (nic.name.equals(ui.vm_network.network)) {
            networks.setSelectedIndex(idx);
            break;
          }
          idx++;
        }
        mac.setText(ui.vm_network.mac);
        accept.setText("Edit");
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _mac = mac.getText();
      if (_mac.length() > 0) {
        if (!MAC.valid(_mac)) {
          errmsg.setText("Error:invalid mac");
          return;
        }
      } else {
        _mac = MAC.generate();
      }
      String _model = models.getSelectedValue();
      String _network = networks.getSelectedValue();
      if (ui.vm_network == null) {
        ui.vm_network = new Network(_network, _model, _mac);
      } else {
        ui.vm_network.model = models.getSelectedValue();
        ui.vm_network.network = networks.getSelectedValue();
        ui.vm_network.mac = _mac;
      }
      if (ui.vm_network_complete != null) {
        ui.vm_network_complete.run();
      }
      ui.vm_network_popup.setVisible(false);
    });

    cancel.addClickListener((me, cmp) -> {
      ui.vm_network_popup.setVisible(false);
    });

    return panel;
  }

  private PopupPanel network_vlan_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Network VLAN");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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

    ui.network_vlan_init = () -> {
      boolean create = ui.network_vlan == null;
      bridge.clear();
      NetworkBridge[] nics = NetworkBridge.list(NetworkBridge.TYPE_OS);
      for(NetworkBridge nic : nics) {
        bridge.add(nic.name, nic.name);
      }
      if (create) {
        name.setText("");
        vlan.setText("0");
        accept.setText("Create");
      } else {
        name.setText(ui.network_vlan.name);
        vlan.setText(Integer.toString(ui.network_vlan.vlan));
        int idx = 0;
        for(NetworkBridge nic : nics) {
          if (nic.name.equals(ui.network_vlan.bridge)) {
            bridge.setSelectedIndex(idx);
            break;
          }
          idx++;
        }
        accept.setText("Edit");
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _name = vmm.cleanName(name.getText());
      if (_name.length() == 0) {
        name.setText(_name);
        errmsg.setText("Error:invalid name");
        return;
      }
      //ensure name is unique
      for(NetworkVLAN nic : Config.current.vlans) {
        if (nic == ui.network_vlan) continue;
        if (nic.name.equals(_name)) {
          errmsg.setText("Error:name not unique");
          return;
        }
      }
      String _bridge = bridge.getSelectedText();
      if (_bridge == null || _bridge.length() == 0) {
        errmsg.setText("Error:invalid bridge");
        return;
      }
      String _vlan_str = vmm.cleanNumber(vlan.getText());
      if (_vlan_str.length() == 0) {
        vlan.setText(_vlan_str);
        errmsg.setText("Error:invalid VLAN");
        return;
      }
      int _vlan = Integer.valueOf(_vlan_str);
      if (_vlan < 0 || _vlan > 4095) {
        errmsg.setText("Error:invalid VLAN");
        return;
      }
      if (ui.network_vlan == null) {
        Config.current.addNetworkVLAN(new NetworkVLAN(_name, _bridge, _vlan));
      } else {
        ui.network_vlan.name = _name;
        ui.network_vlan.bridge = _bridge;
        ui.network_vlan.vlan = _vlan;
        Config.current.save();
      }
      if (ui.network_vlan_complete != null) {
        ui.network_vlan_complete.run();
      }
      ui.network_vlan_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.network_vlan_popup.setVisible(false);
    });

    return panel;
  }

  private PopupPanel network_bridge_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Network Bridge");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Name"));
    TextField name = new TextField("");
    row.add(name);

    row = new Row();
    panel.add(row);
    row.add(new Label("Physical NIC"));
    ComboBox iface = new ComboBox();
    row.add(iface);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.network_bridge_init = () -> {
      boolean create = ui.network_bridge == null;
      iface.clear();
      NetworkInterface[] nics = NetworkInterface.listPhysical();
      for(NetworkInterface nic : nics) {
        if (nic.name.equals("lo")) continue;
        iface.add(nic.name, nic.name);
      }
      if (create) {
        name.setText("");
        accept.setText("Create");
      } else {
        name.setText(ui.network_bridge.name);
        int idx = 0;
        for(NetworkInterface nic : nics) {
          if (nic.name.equals(ui.network_bridge.iface)) {
            iface.setSelectedIndex(idx);
            break;
          }
          idx++;
        }
        accept.setText("Edit");
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _name = vmm.cleanName(name.getText());
      if (_name.length() == 0) {
        name.setText(_name);
        errmsg.setText("Error:invalid name");
        return;
      }
      //ensure name is unique
      {
        NetworkInterface[] nics = NetworkInterface.listPhysical();
        for(NetworkInterface nic : nics) {
          if (nic.name.equals(_name)) {
            errmsg.setText("Error:name not unique");
            return;
          }
        }
      }
      {
        NetworkBridge[] nics = NetworkBridge.list();
        for(NetworkBridge nic : nics) {
          if (nic == ui.network_bridge) continue;
          if (nic.name.equals(_name)) {
            errmsg.setText("Error:name not unique");
            return;
          }
        }
      }
      {
        for(NetworkVirtual nic : Config.current.nics) {
          if (nic.name.equals(_name)) {
            errmsg.setText("Error:name not unique");
            return;
          }
        }
      }
      String _iface = iface.getSelectedText();
      if (_iface == null || _iface.length() == 0) {
        errmsg.setText("Error:invalid interface");
        return;
      }
      NetworkBridge.create(_name, _iface);
      if (ui.network_bridge_complete != null) {
        ui.network_bridge_complete.run();
      }
      ui.network_bridge_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.network_bridge_popup.setVisible(false);
    });

    return panel;
  }

  private PopupPanel network_virtual_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Virtual Network");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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
    row.add(new Label("MAC"));
    TextField mac = new TextField("");
    row.add(mac);
    row.add(new Label("(leave blank to generate)"));

    row = new Row();
    panel.add(row);
    row.add(new Label("IP"));
    TextField ip = new TextField("");
    row.add(ip);

    row = new Row();
    panel.add(row);
    row.add(new Label("Netmask"));
    TextField netmask = new TextField("");
    row.add(netmask);

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

    ui.network_virtual_init = () -> {
      boolean create = ui.network_virtual == null;
      bridge.clear();
      NetworkBridge[] nics = NetworkBridge.list(NetworkBridge.TYPE_OS);
      for(NetworkBridge nic : nics) {
        bridge.add(nic.name, nic.name);
      }
      if (create) {
        name.setText("");
        mac.setText("");
        ip.setText("192.168.1.2");
        netmask.setText("255.255.255.0");
        vlan.setText("0");
        accept.setText("Create");
      } else {
        name.setText(ui.network_virtual.name);
        mac.setText(ui.network_virtual.mac);
        ip.setText(ui.network_virtual.ip);
        netmask.setText(ui.network_virtual.netmask);
        vlan.setText(Integer.toString(ui.network_virtual.vlan));
        int idx = 0;
        for(NetworkBridge nic : nics) {
          if (nic.name.equals(ui.network_virtual.bridge)) {
            bridge.setSelectedIndex(idx);
            break;
          }
          idx++;
        }
        accept.setText("Edit");
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _name = vmm.cleanName(ip.getText());
      if (_name.length() == 0) {
        name.setText(_name);
        errmsg.setText("Error:invalid name");
        return;
      }
      //ensure name is unique
      {
        NetworkInterface[] nics = NetworkInterface.listPhysical();
        for(NetworkInterface nic : nics) {
          if (nic.name.equals(_name)) {
            errmsg.setText("Error:name not unique");
            return;
          }
        }
      }
      {
        NetworkBridge[] nics = NetworkBridge.list();
        for(NetworkBridge nic : nics) {
          if (nic.name.equals(_name)) {
            errmsg.setText("Error:name not unique");
            return;
          }
        }
      }
      {
        for(NetworkVirtual nic : Config.current.nics) {
          if (nic == ui.network_virtual) continue;
          if (nic.name.equals(_name)) {
            errmsg.setText("Error:name not unique");
            return;
          }
        }
      }
      String _bridge = bridge.getSelectedText();
      if (_bridge == null || _bridge.length() == 0) {
        errmsg.setText("Error:invalid bridge");
        return;
      }
      String _mac = mac.getText();
      if (_mac.length() > 0) {
        if (!MAC.valid(_mac)) {
          errmsg.setText("Error:invalid mac");
          return;
        }
      } else {
        _mac = MAC.generate();
      }
      String _ip = ip.getText();
      if (!IP4.isIP(_ip)) {
        errmsg.setText("Error:invalid IP4");
        return;
      }
      String _netmask = netmask.getText();
      if (!Subnet4.isSubnet(_netmask)) {
        errmsg.setText("Error:invalid NetMask4");
        return;
      }
      String _vlan_str = vmm.cleanNumber(vlan.getText());
      if (_vlan_str.length() == 0) {
        vlan.setText(_vlan_str);
        errmsg.setText("Error:invalid VLAN");
        return;
      }
      int _vlan = Integer.valueOf(_vlan_str);
      if (_vlan < 0 || _vlan > 4095) {
        errmsg.setText("Error:invalid VLAN");
        return;
      }
      NetworkBridge sel_bridge = null;
      NetworkBridge[] nics = NetworkBridge.list(NetworkBridge.TYPE_OS);
      for(NetworkBridge nic : nics) {
        if (nic.name.equals(_bridge)) {
          sel_bridge = nic;
          break;
        }
      }
      if (sel_bridge == null) {
        JFLog.log("Error:No bridge to create virtual network");
        return;
      }
      if (!NetworkVirtual.createVirtual(_name, sel_bridge, _mac, _ip, _netmask, _vlan)) {
        JFLog.log("Error:Failed to create virtual network");
        return;
      }
      NetworkVirtual nic = new NetworkVirtual(_name, _bridge, _mac, _ip, _netmask, _vlan);
      Config.current.addNetworkVirtual(nic);
      ui.network_virtual_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.network_virtual_popup.setVisible(false);
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
    ComboBox device = new ComboBox();
    row.add(device);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.device_usb_init = () -> {
      boolean create = ui.device == null;
      String _sel = null;
      if (create) {
        _sel = "";
        accept.setText("Create");
      } else {
        _sel = ui.device.path;
        accept.setText("Edit");
      }
      String[] groups = vmm.listDevices(Device.TYPE_USB);
      int idx = 0;
      int _sel_idx = -1;
      for(String group : groups) {
        if (group.equals(_sel)) {
          _sel_idx = idx;
        }
        device.add(group, group);
        idx++;
      }
      if (_sel_idx != -1) {
        device.setSelectedIndex(_sel_idx);
      }
    };

    accept.addClickListener((me, cmp) -> {
      String _device = device.getSelectedValue();
      if (_device == null || _device.length() == 0) {
        return;
      }
      if (ui.device == null) {
        ui.device = new Device();
        ui.device.type = Device.TYPE_USB;
      }
      ui.device.path = _device;
      if (ui.device_complete != null) {
        ui.device_complete.run();
      }
      ui.device_usb_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.device_usb_popup.setVisible(false);
    });

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
    ComboBox device = new ComboBox();
    row.add(device);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.device_pci_init = () -> {
      boolean create = ui.device == null;
      String _sel = null;
      if (create) {
        _sel = "";
        accept.setText("Create");
      } else {
        _sel = ui.device.path;
        accept.setText("Edit");
      }
      String[] groups = vmm.listDevices(Device.TYPE_PCI);
      int idx = 0;
      int _sel_idx = -1;
      for(String group : groups) {
        if (group.equals(_sel)) {
          _sel_idx = idx;
        }
        device.add(group, group);
        idx++;
      }
      if (_sel_idx != -1) {
        device.setSelectedIndex(_sel_idx);
      }
    };

    accept.addClickListener((me, cmp) -> {
      String _device = device.getSelectedValue();
      if (_device == null || _device.length() == 0) {
        return;
      }
      if (ui.device == null) {
        ui.device = new Device();
        ui.device.type = Device.TYPE_PCI;
      }
      ui.device.path = _device;
      if (ui.device_complete != null) {
        ui.device_complete.run();
      }
      ui.device_pci_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.device_pci_popup.setVisible(false);
    });

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
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    inner.add(errmsg);

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
      errmsg.setText("");
      if (Config.current.password != null) {
        errmsg.setText("Already configured, please refresh!");
        return;
      }
      String passTxt1 = password.getText();
      String passTxt2 = confirm.getText();
      if (passTxt1.length() < 8 || passTxt2.length() < 8) {
        errmsg.setText("Password too short (min 8)");
        return;
      }
      if (!passTxt1.equals(passTxt2)) {
        errmsg.setText("Passwords do not match");
        return;
      }
      Config.current.password = passTxt1;
      Config.current.save();
      client.setPanel(getRootPanel(client));
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
      ui.setRightPanel(hostPanel());
    });
    vms.addClickListener((me, cmp) -> {
      ui.setRightPanel(vmsPanel(ui));
    });
    stores.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });
    networks.addClickListener((me, cmp) -> {
      ui.setRightPanel(networkPanel(ui));
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
    TextField fqn = new TextField(Config.current.fqn);
    row.add(fqn);

    row = new Row();
    panel.add(row);
    row.add(new Label("Default iSCSI Initiator IQN"));
    TextField iqn = new TextField(Config.current.iqn);
    row.add(iqn);
    Button iqn_generate = new Button("Generate");
    row.add(iqn_generate);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button save = new Button("Save");
    tools.add(save);

    row = new Row();
    panel.add(row);
    Label msg = new Label("");
    row.add(msg);

    save.addClickListener((me, cmp) -> {
      Config.current.fqn = fqn.getText();
      Config.current.iqn = iqn.getText();
      Config.current.save();
      msg.setText("Settings saved!");
    });

    iqn_generate.addClickListener((me, cmp) -> {
      Config.current.fqn = fqn.getText();
      iqn.setText(IQN.generate(Config.current.fqn));
      msg.setText("Client IQN regenerated");
    });

    return panel;
  }

  private Panel vmsPanel(UI ui) {
    TabPanel panel = new TabPanel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button create = new Button("Create");
    tools.add(create);
    Button edit = new Button("Edit");
    tools.add(edit);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button console = new Button("Console");
    tools.add(console);
    Button start = new Button("Start");
    tools.add(start);
    Button stop = new Button("Stop");
    tools.add(stop);
    Button restart = new Button("Restart");
    tools.add(restart);
    Button poweroff = new Button("PowerOff");
    tools.add(poweroff);
    Button unreg = new Button("Unregister");
    tools.add(unreg);

    ListBox list = new ListBox();
    panel.add(list);
    VirtualMachine[] vms = VirtualMachine.list();
    for(VirtualMachine vm : vms) {
      list.add(vm.name + ":" + vm.getStateString());
    }

    create.addClickListener((me, cmp) -> {
      Hardware hw = new Hardware();
      VirtualMachine vm = new VirtualMachine(hw);
      ui.setRightPanel(vmAddPanel(vm, hw, ui));
    });

    edit.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      VirtualMachine vm = vms[idx];
      Hardware hardware = vm.loadHardware();
      if (hardware == null) {
        JFLog.log("Error:Failed to load config for vm:" + vm.name);
        return;
      }
      ui.setRightPanel(vmEditPanel(vm, hardware, false, ui));
    });

    refresh.addClickListener((me, cmp) -> {
      ui.setRightPanel(vmsPanel(ui));
    });

    console.addClickListener((me, cmp) -> {
      //TODO : redir external ports to internal ports
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      VirtualMachine vm = vms[idx];
      ui.message_message.setText("Open VNC client to 127.0.0.1:" + vm.getVNC());
      ui.message_popup.setVisible(true);
    });

    start.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      VirtualMachine vm = vms[idx];
      if (vm.getState() != 0) {
        errmsg.setText("Error:VM is already powered on.");
        return;
      }
      ui.confirm_button.setText("Start");
      ui.confirm_message.setText("Start VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Start") {
          public void doTask() {
            if (vm.start()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        KVMService.tasks.addTask(task);
      };
      ui.confirm_popup.setVisible(true);
    });

    //stop
    stop.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      VirtualMachine vm = vms[idx];
      if (vm.getState() == 0) {
        errmsg.setText("Error:VM is already stopped.");
        return;
      }
      ui.confirm_button.setText("Stop");
      ui.confirm_message.setText("Stop VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Stop") {
          public void doTask() {
            if (vm.stop()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        KVMService.tasks.addTask(task);
      };
      ui.confirm_popup.setVisible(true);
    });

    //restart
    restart.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      VirtualMachine vm = vms[idx];
      if (vm.getState() == 0) {
        errmsg.setText("Error:VM is not live.");
        return;
      }
      ui.confirm_button.setText("Restart");
      ui.confirm_message.setText("Restart VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Restart") {
          public void doTask() {
            if (vm.restart()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        KVMService.tasks.addTask(task);
      };
      ui.confirm_popup.setVisible(true);
    });

    //poweroff
    poweroff.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      VirtualMachine vm = vms[idx];
      if (vm.getState() == 0) {
        errmsg.setText("Error:VM is already powered down.");
        return;
      }
      ui.confirm_button.setText("Power Off");
      ui.confirm_message.setText("Power Off VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Power Off") {
          public void doTask() {
            if (vm.poweroff()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        KVMService.tasks.addTask(task);
      };
      ui.confirm_popup.setVisible(true);
    });

    unreg.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      VirtualMachine vm = vms[idx];
      if (vm.getState() != 0) {
        errmsg.setText("Error:Can not unregister a live VM.");
        return;
      }
      ui.confirm_button.setText("Unregister");
      ui.confirm_message.setText("Unregister VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Unregister") {
          public void doTask() {
            if (vm.getState() != 0) {
              setResult("Error:Can not unregister a live VM.");
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
      };
      ui.confirm_popup.setVisible(true);
    });

    return panel;
  }

  private Panel vmAddPanel(VirtualMachine vm, Hardware hardware, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    //name [   ]
    row = new Row();
    panel.add(row);
    row.add(new Label("Name"));
    TextField vm_name = new TextField(hardware.name);
    row.add(vm_name);
    //pool [  v]
    row = new Row();
    panel.add(row);
    row.add(new Label("Storage Pool"));
    ComboBox vm_pool = new ComboBox();
    row.add(vm_pool);
    Storage[] pools = vmm.listPools();
    for(Storage p : pools) {
      String _name = p.name;
      vm_pool.add(_name, _name);
    }

    //next / cancel
    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button b_next = new Button("Next");
    tools.add(b_next);
    Button b_cancel = new Button("Cancel");
    tools.add(b_cancel);

    b_next.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _vm_name = vmm.cleanName(vm_name.getText());
      if (_vm_name.length() == 0) {
        errmsg.setText("Error:invalid name");
        return;
      }
      String _vm_pool = vm_pool.getSelectedValue();
      if (_vm_pool == null || _vm_pool.length() == 0) {
        errmsg.setText("Error:invalid storage pool");
        return;
      }
      Storage pool = vmm.getPoolByName(_vm_pool);
      if (pool == null) {
        errmsg.setText("Error:pool does not exist");
        return;
      }
      if (!new File(pool.getPath()).exists()) {
        errmsg.setText("Error:pool not mounted");
        return;
      }
      hardware.name = _vm_name;
      hardware.pool = _vm_pool;
      vm.name = _vm_name;
      vm.pool = _vm_pool;
      File file = new File(hardware.getPath());
      if (file.exists()) {
        errmsg.setText("Error:folder already exists in storage pool with that name");
        return;
      }
      file.mkdirs();
      ui.setRightPanel(vmEditPanel(vm, hardware, true, ui));
    });
    b_cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(vmsPanel(ui));
    });

    return panel;
  }

  private Panel vmEditPanel(VirtualMachine vm, Hardware hardware, boolean create, UI ui) {
    Panel panel = new Panel();
    ui.hardware = hardware;
    Row row;
    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);
    //name [   ]
    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + hardware.name));
    //pool [  v]
    row = new Row();
    panel.add(row);
    row.add(new Label("Storage Pool:" + hardware.pool));
    //memory [   ] [MB/GB]
    row = new Row();
    panel.add(row);
    row.add(new Label("Memory"));
    TextField memory = new TextField(Integer.toString(hardware.memory.size));
    row.add(memory);
    ComboBox memory_units = new ComboBox();
    memory_units.add("MB", "MB");
    memory_units.add("GB", "GB");
    memory_units.setSelectedIndex(hardware.memory.unit - 2);
    row.add(memory_units);
    //cpus [   ]
    row = new Row();
    panel.add(row);
    row.add(new Label("CPU Cores"));
    TextField cores = new TextField(Integer.toString(hardware.cores));
    row.add(cores);
    //firmware [BIOS/UEFI]
    row = new Row();
    panel.add(row);
    row.add(new Label("Firmware"));
    ComboBox firmware = new ComboBox();
    firmware.add("BIOS", "BIOS");
    firmware.add("UEFI", "UEFI");
    if (hardware.bios_efi) {
      firmware.setSelectedIndex(1);
    }
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
    Button b_disk_edit = new Button("Edit");
    disk_ops.add(b_disk_edit);
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
    Button b_net_edit = new Button("Edit");
    net_ops.add(b_net_edit);
    Button b_net_delete = new Button("Delete");
    net_ops.add(b_net_delete);
    ListBox net_list = new ListBox();
    networks.add(net_list);
    for(Network nic : hardware.networks) {
      net_list.add("net:" + nic.network);
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
    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button b_save = new Button("Save");
    tools.add(b_save);
    Button b_cancel = new Button("Cancel");
    tools.add(b_cancel);

    b_disk_create.addClickListener((me, cmp) -> {
      ui.vm_disk = null;
      ui.vm_disk_init.run();
      ui.vm_disk_complete = () -> {
        ui.hardware.addDisk(ui.vm_disk);
        disk_list.add(ui.vm_disk.name);
      };
      ui.vm_disk_popup.setVisible(true);
    });
    b_disk_add.addClickListener((me, cmp) -> {
      ui.browse_path = ui.hardware.getPath();
      ui.browse_filters = filter_disks;
      ui.browse_init.run();
      ui.browse_complete = () -> {
        String path_file = ui.browse_select;
        String pool = getPool(path_file);
        String folder = getFolder(path_file);
        String file = getFile(path_file);
        Disk disk = new Disk();
        disk.name = removeExt(file);
        disk.pool = pool;
        disk.folder = folder;
        disk.type = Disk.getType(getExt(file));
        disk.size = new Size(0, 0);  //unknown size
        if (disk.type == -1) {
          ui.browse_errmsg.setText("Error:unknown disk type");
          return;
        }
        if (ui.hardware.hasDisk(disk.name)) {
          ui.browse_errmsg.setText("Error:disk name already exists");
          return;
        }
        ui.hardware.addDisk(disk);
        disk_list.add(disk.name);
        ui.browse_popup.setVisible(false);
      };
      ui.browse_popup.setVisible(true);
    });
    b_disk_edit.addClickListener((me, cmp) -> {
      int idx = disk_list.getSelectedIndex();
      if (idx == -1) return;
      ui.vm_disk = ui.hardware.disks.get(idx);
      ui.vm_disk_init.run();
      ui.vm_disk_complete = null;
      ui.vm_disk_popup.setVisible(true);
    });
    b_disk_delete.addClickListener((me, cmp) -> {
      int idx = disk_list.getSelectedIndex();
      if (idx == -1) return;
      Disk disk = ui.hardware.disks.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Disk : " + disk.name);
      ui.confirm_action = () -> {
        ui.hardware.removeDisk(disk);
        disk_list.remove(idx);
      };
      ui.confirm_popup.setVisible(true);
    });
    b_net_add.addClickListener((me, cmp) -> {
      ui.vm_network = null;
      ui.vm_network_complete = () -> {
        hardware.addNetwork(ui.vm_network);
        net_list.add(ui.vm_network.network);
      };
      ui.vm_network_init.run();
      ui.vm_network_popup.setVisible(true);
    });
    b_net_edit.addClickListener((me, cmp) -> {
      int idx = net_list.getSelectedIndex();
      if (idx == -1) return;
      ui.vm_network = hardware.networks.get(idx);
      ui.vm_network_complete = null;
      ui.vm_network_init.run();
      ui.vm_network_popup.setVisible(true);
    });
    b_net_delete.addClickListener((me, cmp) -> {
      int idx = net_list.getSelectedIndex();
      if (idx == -1) return;
      net_list.remove(idx);
      hardware.removeNetwork(hardware.networks.get(idx));
    });
    b_dev_add_usb.addClickListener((me, cmp) -> {
      ui.device = null;
      ui.device_usb_init.run();
      ui.device_complete = () -> {
        ui.hardware.addDevice(ui.device);
        dev_list.add(ui.device.name);
      };
      ui.device_usb_popup.setVisible(true);
    });
    b_dev_add_pci.addClickListener((me, cmp) -> {
      ui.device = null;
      ui.device_pci_init.run();
      ui.device_complete = () -> {
        ui.hardware.addDevice(ui.device);
        dev_list.add(ui.device.name);
      };
      ui.device_pci_popup.setVisible(true);
    });
    b_dev_delete.addClickListener((me, cmp) -> {
      int idx = dev_list.getSelectedIndex();
      if (idx == -1) return;
      Device device = ui.hardware.devices.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Device : " + device.name);
      ui.confirm_action = () -> {
        ui.hardware.removeDevice(device);
        dev_list.remove(idx);
      };
      ui.confirm_popup.setVisible(true);
    });

    b_save.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _size_str = vmm.cleanNumber(memory.getText());
      if (_size_str.length() == 0) {
        memory.setText(_size_str);
        errmsg.setText("Error:invalid memory size");
        return;
      }
      int mem = Integer.valueOf(_size_str);
      if (mem == 0) {
        errmsg.setText("Error:invalid memory size");
        return;
      }
      int mem_units = memory_units.getSelectedIndex() + 2;
      hardware.memory = new Size(mem, mem_units);
      _size_str = vmm.cleanNumber(cores.getText());
      if (_size_str.length() == 0) {
        cores.setText(_size_str);
        errmsg.setText("Error:invalid cores");
        return;
      }
      int _cores = Integer.valueOf(_size_str);
      if (_cores == 0) {
        errmsg.setText("Error:invalid cores");
        return;
      }
      hardware.cores = _cores;
      hardware.bios_efi = firmware.getSelectedIndex() == 1;
      hardware.validate();
      if (!VirtualMachine.register(vm, hardware, vmm)) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      vm.saveHardware(hardware);
      ui.setRightPanel(vmsPanel(ui));
    });
    b_cancel.addClickListener((me, cmp) -> {
      if (create) {
        JF.deletePathEx(hardware.getPath());
      }
      ui.setRightPanel(vmsPanel(ui));
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
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button browse = new Button("Browse");
    tools.add(browse);
    Button start = new Button("Start");
    tools.add(start);
    Button stop = new Button("Stop");
    tools.add(stop);
/*
    Button mount = new Button("Mount");
    tools.add(mount);
    Button unmount = new Button("Unmount");
    tools.add(unmount);
*/
    Button format = new Button("Format");
    tools.add(format);
    Button delete = new Button("Delete");
    tools.add(delete);
    ListBox list = new ListBox();
    panel.add(list);
    Storage[] pools = vmm.listPools();
    for(Storage pool : pools) {
      String _name = pool.name;
      list.add(_name + ":" + pool.getStateString());
    }

    add.addClickListener((me, cmp) -> {
      ui.setRightPanel(addStoragePanel(ui));
    });
    edit.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = pools[idx];
      ui.setRightPanel(editStoragePanel(pool, ui));
    });
    refresh.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });
    browse.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = pools[idx];
      ui.browse_path = pool.getPath();
      ui.browse_filters = filter_all;
      ui.browse_init.run();
      ui.browse_complete = () -> {
        if (ui.browse_select.endsWith(".jfvm")) {
          Hardware hardware = Hardware.load(ui.browse_select);
          if (hardware == null) {
            ui.browse_errmsg.setText("Failed to load VM, see logs.");
            return;
          }
          VirtualMachine vm = new VirtualMachine(hardware);
          if (!VirtualMachine.register(vm, hardware, vmm)) {
            ui.browse_errmsg.setText("Failed to register VM, see logs.");
            return;
          }
          ui.browse_popup.setVisible(false);
        } else {
          ui.browse_popup.setVisible(false);
          //TODO : no action
        }
      };
      ui.browse_popup.setVisible(true);
    });
    start.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = pools[idx];
      pool.start();
      ui.setRightPanel(storagePanel(ui));
    });
    stop.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = pools[idx];
      pool.stop();
      ui.setRightPanel(storagePanel(ui));
    });
/*
    mount.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = pools[idx];
      pool.mount();
      ui.setRightPanel(storagePanel(ui));
    });
    unmount.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = pools[idx];
      ui.confirm_button.setText("Unmount");
      ui.confirm_message.setText("Unmount storage pool:" + pool.name);
      ui.confirm_action = () -> {
        pool.unmount();
        ui.setRightPanel(storagePanel(ui));
      };
      ui.confirm_popup.setVisible(true);
    });
*/
    format.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = pools[idx];
      switch (pool.type) {
        case Storage.TYPE_LOCAL_PART:
          ui.confirm_button.setText("Format");
          ui.confirm_message.setText("Format storage pool");
          ui.confirm_action = () -> {
            //TODO : create task thread
            pool.format(Storage.TYPE_EXT4);
            ui.setRightPanel(storagePanel(ui));
          };
          ui.confirm_popup.setVisible(true);
          break;
        case Storage.TYPE_ISCSI:
          ui.message_message.setText("TODO : iSCSI format");
          ui.message_popup.setVisible(true);
          break;
        case Storage.TYPE_NFS:
          ui.message_message.setText("Can not format NFS storage");
          ui.message_popup.setVisible(true);
          break;
      }
    });
    delete.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      Storage pool = Config.current.pools.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete storage pool");
      ui.confirm_action = () -> {
        pool.unregister();
        Config.current.removeStorage(pool);
        ui.setRightPanel(storagePanel(ui));
      };
      ui.confirm_popup.setVisible(true);
    });

    return panel;
  }

  private Panel addStoragePanel(UI ui) {
    Panel panel = new Panel();
    Label desc = new Label("New Storage Pool");
    panel.add(desc);
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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
    type.add("local_part", "Local Partition");
//    type.add("local_disk", "Local Disk");  //TODO
    row.add(type);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button next = new Button("Next");
    tools.add(next);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    next.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _name = vmm.cleanName(name.getText());
      if (_name.length() == 0) {
        name.setText(_name);
        errmsg.setText("Error:invalid name");
        return;
      }
      Storage[] pools = vmm.listPools();
      for(Storage pool : pools) {
        if (pool.name.equals(_name)) {
          errmsg.setText("Error:name is not unique");
          return;
        }
      }
      switch (type.getSelectedValue()) {
        case "nfs":
          ui.setRightPanel(nfs_StoragePanel(new Storage(Storage.TYPE_NFS, _name, null), true, ui));
          break;
        case "iscsi":
          ui.setRightPanel(iscsi_StoragePanel(new Storage(Storage.TYPE_ISCSI, _name, null), true, ui));
          break;
        case "local_part":
          ui.setRightPanel(local_StoragePanel(new Storage(Storage.TYPE_LOCAL_PART, _name, null), true, ui));
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
      case Storage.TYPE_NFS: return nfs_StoragePanel(store, false, ui);
      case Storage.TYPE_ISCSI: return iscsi_StoragePanel(store, false, ui);
      case Storage.TYPE_LOCAL_PART: return local_StoragePanel(store, false, ui);
    }
    return null;
  }

  private Panel nfs_StoragePanel(Storage pool, boolean create, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + pool.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Host:"));
    TextField host = new TextField("");
    row.add(host);
    if (pool.host != null) {
      host.setText(pool.host);
    }

    row = new Row();
    panel.add(row);
    row.add(new Label("Source Path:"));
    TextField path = new TextField("");
    row.add(path);
    if (pool.path != null) {
      path.setText(pool.path);
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _host = host.getText();
      String _path = path.getText();
      if (_host.length() == 0) {
        errmsg.setText("Error:host invalid");
        return;
      }
      if (_path.length() == 0) {
        errmsg.setText("Error:path invalid");
        return;
      }
      pool.host = _host;
      pool.path = _path;
      if (!pool.register()) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      if (create) {
        Config.current.addStorage(pool);
      } else {
        Config.current.save();
      }
      ui.setRightPanel(storagePanel(ui));
    });
    cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });

    return panel;
  }

  private Panel iscsi_StoragePanel(Storage pool, boolean create, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + pool.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Host:"));
    TextField host = new TextField("");
    row.add(host);
    if (pool.host != null) {
      host.setText(pool.host);
    }

    row = new Row();
    panel.add(row);
    row.add(new Label("Target IQN:"));
    TextField target = new TextField("");
    row.add(target);
    if (pool.target != null) {
      target.setText(pool.target);
    }

    row = new Row();
    panel.add(row);
    row.add(new Label("Initiator IQN:"));
    TextField initiator = new TextField("");
    row.add(initiator);
    if (pool.initiator != null) {
      initiator.setText(pool.initiator);
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _host = host.getText();
      String _target = target.getText();
      String _init = initiator.getText();
      if (_host.length() == 0) {
        errmsg.setText("Error:host invalid");
        return;
      }
      if (_target.length() == 0) {
        errmsg.setText("Error:target iqn invalid");
        return;
      }
      if (_init.length() == 0) {
        errmsg.setText("Error:initiator iqn invalid");
        return;
      }
      pool.host = _host;
      pool.target = _target;
      pool.initiator = _init;
      if (!pool.register()) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      if (create) {
        Config.current.addStorage(pool);
      }
      Config.current.save();
      ui.setRightPanel(storagePanel(ui));
    });
    cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });

    return panel;
  }

  private Panel local_StoragePanel(Storage pool, boolean create, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + pool.name));

    //TODO : only list know partitions and exclude system partitions
    row = new Row();
    panel.add(row);
    row.add(new Label("Device:"));
    TextField dev = new TextField("");
    row.add(dev);
    if (pool.path != null) {
      dev.setText(pool.path);
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _dev = dev.getText();
      if (_dev.length() == 0) {
        errmsg.setText("Error:device invalid");
        return;
      }
      if (!new File(_dev).exists()) {
        errmsg.setText("Error:device not found");
        return;
      }
      pool.path = _dev;
      if (!pool.register()) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      if (create) {
        Config.current.addStorage(pool);
      }
      Config.current.save();
      ui.setRightPanel(storagePanel(ui));
    });
    cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });

    return panel;
  }

  private PopupPanel browseStoragePopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Browse");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    row.add(errmsg);
    ui.browse_errmsg = errmsg;

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button cdup = new Button("&#x21e7;");  //unicode up arrow
    tools.add(cdup);
    Button select = new Button("Select");
    tools.add(select);
    Button upload = new Button("Upload");
//    tools.add(upload);
    Button delete = new Button("Delete");
    tools.add(delete);

    row = new Row();
    panel.add(row);
    TextField path = new TextField("");
    row.add(path);
    path.setReadonly(true);

    ListBox list = new ListBox();
    panel.add(list);

    ui.browse_init = () -> {
      errmsg.setText("");
      path.setText(ui.browse_path);
      list.removeAll();
      File[] files = new File(ui.browse_path).listFiles();
      if (files == null) {
        //TODO : handle error
        files = new File[0];
      }
      for(File file : files) {
        String name = file.getName();
        if (file.isDirectory()) {
          list.add("/" + name);
        } else {
          for(String filter : ui.browse_filters) {
            if (name.matches(filter)) {
              list.add(name);
              break;
            }
          }
        }
      }
    };
    ui.browse_path = "/volumes";

    refresh.addClickListener((me, cmp) -> {
      ui.browse_init.run();
    });
    cdup.addClickListener((me, cmp) -> {
      int idx = ui.browse_path.lastIndexOf('/');
      if (idx == -1 || idx == 0) return;
      ui.browse_path = ui.browse_path.substring(0, idx);
      ui.browse_init.run();
    });
    upload.addClickListener((me, cmp) -> {
      //TODO : upload files - use filezilla for now
    });
    delete.addClickListener((me, cmp) -> {
      String item = list.getSelectedItem();
      if (item == null) return;
      if (item.startsWith("/")) {
        ui.confirm_message.setText("Delete folder:" + item);
        ui.confirm_action = () -> {
          new File(ui.browse_path + item).delete();
          ui.browse_init.run();
        };
      } else {
        ui.confirm_message.setText("Delete file:" + item);
        ui.confirm_action = () -> {
          new File(ui.browse_path + "/" + item).delete();
          ui.browse_init.run();
        };
      }
    });
    list.addClickListener((me, cmp) -> {
      String item = list.getSelectedItem();
      if (item == null) return;
      if (item.startsWith("/")) {
        ui.browse_path += item;
        ui.browse_init.run();
      }
    });
    select.addClickListener((me, cmp) -> {
      ui.browse_select = ui.browse_path + "/" + list.getSelectedItem();
      ui.browse_complete.run();
    });

    return panel;
  }

  private Panel networkPanel(UI ui) {
    TabPanel panel = new TabPanel();
    networkPanel_vlans(panel, ui);
    networkPanel_bridges(panel, ui);
    networkPanel_nics(panel, ui);
    networkPanel_phys(panel, ui);
    return panel;
  }

  private void networkPanel_phys(TabPanel panel, UI ui) {
    {
      Panel tab = new Panel();
      panel.addTab(tab, "Physical NICs");
      ListBox phys_list = new ListBox();
      tab.add(phys_list);
      NetworkInterface[] nics = vmm.listNetworkInterface();
      for(NetworkInterface nic : nics) {
        if (nic.name.equals("lo")) continue;
        phys_list.add(nic.name + ":" + nic.ip + "/" + nic.netmask + ":" + nic.mac);
      }
    }
  }

  private void networkPanel_bridges(TabPanel panel, UI ui) {
    //bridges (virtual switches)
    {
      Panel tab = new Panel();
      panel.addTab(tab, "Virtual Switches");
      ToolBar tools = new ToolBar();
      tab.add(tools);
      Button create = new Button("Create");
      tools.add(create);
      Button edit = new Button("Edit");
      tools.add(edit);
      Button delete = new Button("Delete");
      tools.add(delete);
      tab.add(new Label("NOTE : 'os' bridges are required for VLAN tagging guest networks. Please convert 'br' bridges if present."));
      ListBox list = new ListBox();
      tab.add(list);
      NetworkBridge[] nics = NetworkBridge.list();
      for(NetworkBridge nic : nics) {
        list.add(nic.name + ":" + nic.type + ":" + nic.iface);
      }

      create.addClickListener((me, cmp) -> {
        ui.network_bridge = null;
        ui.network_bridge_complete = () -> {
          list.removeAll();
          NetworkBridge[] nics1 = NetworkBridge.list();
          for (NetworkBridge nic : nics1) {
            list.add(nic.name + ":" + nic.type + ":" + nic.iface);
          }
        };
        ui.network_bridge_init.run();
        ui.network_bridge_popup.setVisible(true);
      });
      edit.addClickListener((me, cmp) -> {
        int idx = list.getSelectedIndex();
        //TODO : edit virtual switch (bridge): edit/remove nics, etc.
      });
      delete.addClickListener((me, cmp) -> {
        int idx = list.getSelectedIndex();
        if (idx == -1) return;
        NetworkBridge nic = nics[idx];
        if (nic.remove()) {
          list.remove(idx);
          list.setSelectedIndex(-1);
        }
      });
    }
  }

  private void networkPanel_vlans(TabPanel panel, UI ui) {
    //network VLANs
    {
      Panel tab = new Panel();
      panel.addTab(tab, "Networks");
      ToolBar tools = new ToolBar();
      tab.add(tools);
      Button create = new Button("Create");
      tools.add(create);
      Button edit = new Button("Edit");
      tools.add(edit);
      Button delete = new Button("Delete");
      tools.add(delete);
      ListBox list = new ListBox();
      tab.add(list);

      ui.network_vlan_complete = () -> {
        list.removeAll();
        for(NetworkVLAN nic : Config.current.vlans) {
          list.add(nic.name + ":" + nic.vlan + ":" + nic.getUsage());
        }
      };
      ui.network_vlan_complete.run();

      create.addClickListener((me, cmp) -> {
        if (NetworkBridge.list(NetworkBridge.TYPE_OS).length == 0) {
          ui.message_message.setText("Must create bridge (virtual switch) first.");
          ui.message_popup.setVisible(true);
          return;
        }
        ui.network_vlan = null;
        ui.network_vlan_init.run();
        ui.network_vlan_popup.setVisible(true);
      });
      edit.addClickListener((me, cmp) -> {
        int idx = list.getSelectedIndex();
        if (idx == -1) return;
        ui.network_vlan = Config.current.vlans.get(idx);
        ui.network_vlan_init.run();
        ui.network_vlan_popup.setVisible(true);
      });
      delete.addClickListener((me, cmp) -> {
        int idx = list.getSelectedIndex();
        if (idx == -1) return;
        NetworkVLAN nic = Config.current.vlans.get(idx);
        if (nic.getUsage() > 0) {
          ui.message_message.setText("Network is in use");
          ui.message_popup.setVisible(true);
          return;
        }
        ui.confirm_action = () -> {
          int idx1 = list.getSelectedIndex();
          NetworkVLAN nic1 = Config.current.vlans.get(idx1);
          Config.current.removeNetworkVLAN(nic1);
          ui.network_vlan_complete.run();
        };
        ui.confirm_button.setText("Delete");
        ui.confirm_message.setText("Delete VLAN:" + nic.name);
        ui.confirm_popup.setVisible(true);
      });
    }
  }

  private void networkPanel_nics(TabPanel panel, UI ui) {
    //server nics
    {
      Panel tab = new Panel();
      panel.addTab(tab, "Server Virtual NICs");
      ToolBar tools = new ToolBar();
      tab.add(tools);
      Button create = new Button("Create");
      tools.add(create);
      Button edit = new Button("Edit");
      tools.add(edit);
      Button delete = new Button("Delete");
      tools.add(delete);
      ListBox list = new ListBox();
      tab.add(list);
      ArrayList<NetworkVirtual> nics = Config.current.nics;
      for(NetworkVirtual nic : nics) {
        list.add(nic.name);
      }
      create.addClickListener((me, cmp) -> {
        ui.network_virtual = null;
        ui.network_virtual_complete = null;
        ui.network_virtual_popup.setVisible(true);
      });
      edit.addClickListener((me, cmp) -> {
        int idx = list.getSelectedIndex();
        if (idx == -1) return;
        //TODO : edit virtual nic
        NetworkVirtual nic = nics.get(idx);
        ui.network_virtual = nic;
        ui.network_virtual_complete = () -> {
          //TODO : edit virt nic
        };
        //ui.network_virtual_popup.setVisible(true);
      });
      delete.addClickListener((me, cmp) -> {
        int idx = list.getSelectedIndex();
        if (idx == -1) return;
        NetworkVirtual nic = nics.get(idx);
        ui.confirm_button.setText("Delete");
        ui.confirm_message.setText("Delete NIC : " + nic.name);
        ui.confirm_action = () -> {
          list.remove(idx);
          nic.remove();
          Config.current.removeNetworkVirtual(nic);
        };
      });
    }
  }

  private String getPool(String path_file) {
    // /volumes/pool/path/file
    int i1 = JF.indexOf(path_file, '/', 2);
    if (i1 == -1) return null;
    int i2 = JF.indexOf(path_file, '/', 3);
    if (i2 == -1) return null;
    return path_file.substring(i1 + 1, i2);
  }

  private String getFolder(String path_file) {
    // /volumes/pool/path/file
    int i1 = JF.indexOf(path_file, '/', 3);
    if (i1 == -1) return null;
    int i2 = path_file.lastIndexOf('/');
    if (i2 == -1) return null;
    return path_file.substring(i1 + 1, i2);
  }

  private String getFile(String path_file) {
    // /volumes/pool/path/file
    int idx = path_file.lastIndexOf('/');
    if (idx == -1) return null;
    return path_file.substring(idx + 1);
  }

  private String removeExt(String file) {
    //remove ext from disk filename
    int idx = file.lastIndexOf('.');
    if (idx == -1) return null;
    return file.substring(0, idx);
  }

  private String getExt(String file) {
    //get ext from disk filename
    int idx = file.lastIndexOf('.');
    if (idx == -1) return null;
    return file.substring(idx + 1);
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

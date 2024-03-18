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
    public String browse_path;

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

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.vm_disk_init = () -> {
      if (ui.vm_disk == null) {
        //create
        name.setText(ui.hardware.getNextDiskName());
        type.setSelectedIndex(0);
        size.setText("100");
        size_units.setSelectedIndex(1);
      } else {
        //update
        name.setText(ui.vm_disk.name);
        switch (ui.vm_disk.type) {
          case Disk.TYPE_VMDK: type.setSelectedIndex(0); break;
          case Disk.TYPE_QCOW2: type.setSelectedIndex(1); break;
        }
        size.setText(Integer.toString(ui.vm_disk.size.size));
        size_units.setSelectedIndex(ui.vm_disk.size.unit - 2);
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
      String _size_str = vmm.cleanNumber(size.getText());
      if (_size_str.length() == 0) {
        size.setText(_size_str);
        errmsg.setText("Error:invalid size");
        return;
      }
      int _size = Integer.valueOf(_size_str);
      int _size_unit = size_units.getSelectedIndex() + 2;
      if (ui.vm_disk == null) {
        //create
        Disk disk = new Disk();
        disk.pool = ui.hardware.pool;
        disk.name = _name;
        disk.type = type.getSelectedIndex();
        disk.size = new Size(_size, _size_unit);
        //TODO : create disk file (vmdk/qcow2)
        ui.hardware.addDisk(disk);
      } else {
        //update
        Disk disk = ui.vm_disk;
        disk.name = _name;
        disk.type = type.getSelectedIndex();
        disk.size = new Size(_size, _size_unit);
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
      networks.clear();
      ArrayList<NetworkVLAN> nics = Config.current.vlans;
      for(NetworkVLAN nic : nics) {
        networks.add(nic.name, nic.name);
      }
      if (ui.vm_network == null) {
        models.setSelectedIndex(0);
        networks.setSelectedIndex(0);
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
    Button accept = new Button("Okay");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.network_vlan_init = () -> {
      bridge.clear();
      NetworkBridge[] nics = NetworkBridge.list(NetworkBridge.TYPE_OS);
      for(NetworkBridge nic : nics) {
        bridge.add(nic.name, nic.name);
      }
      if (ui.network_vlan == null) {
        name.setText("");
        vlan.setText("0");
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
      iface.clear();
      NetworkInterface[] nics = NetworkInterface.listPhysical();
      for(NetworkInterface nic : nics) {
        if (nic.name.equals("lo")) continue;
        iface.add(nic.name, nic.name);
      }
      if (ui.network_bridge == null) {
        name.setText("");
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
      bridge.clear();
      NetworkBridge[] nics = NetworkBridge.list(NetworkBridge.TYPE_OS);
      for(NetworkBridge nic : nics) {
        bridge.add(nic.name, nic.name);
      }
      if (ui.network_virtual == null) {
        name.setText("");
        mac.setText("");
        ip.setText("192.168.1.2");
        netmask.setText("255.255.255.0");
        vlan.setText("0");
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
      String _sel = null;
      if (ui.device != null) {
        _sel = ui.device.path;
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
        ui.hardware.addDevice(ui.device);
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
      String _sel = null;
      if (ui.device != null) {
        _sel = ui.device.path;
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
        ui.hardware.addDevice(ui.device);
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
    VirtualMachine[] vms = VirtualMachine.list();
    for(VirtualMachine vm : vms) {
      list.add(vm.name + ":" + vm.getStateString());
    }

    create.addClickListener((me, cmp) -> {
      Hardware hw = new Hardware();
      VirtualMachine vm = new VirtualMachine(hw);
      ui.split.setRightComponent(vmEditPanel(vm, hw, ui));
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
      ui.split.setRightComponent(vmEditPanel(vm, hardware, ui));
    });

    start.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String name = list.getSelectedItem();
      ui.confirm_button.setText("Start");
      ui.confirm_message.setText("Start VM : " + name);
      ui.confirm_action = () -> {
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
      ui.confirm_action = () -> {
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
      ui.confirm_action = () -> {
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
      };
      ui.confirm_popup.setVisible(true);
    });

    unreg.addClickListener((me, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String name = list.getSelectedItem();
      ui.confirm_button.setText("Unregister");
      ui.confirm_message.setText("Unregister VM : " + name);
      ui.confirm_action = () -> {
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
      };
      ui.confirm_popup.setVisible(true);
    });

    return panel;
  }

  private Panel vmEditPanel(VirtualMachine vm, Hardware hardware, UI ui) {
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
    row.add(new Label("Name"));
    TextField name = new TextField(hardware.name);
    row.add(name);
    //pool [  v]
    row = new Row();
    panel.add(row);
    row.add(new Label("Storage Pool"));
    ComboBox pool = new ComboBox();
    row.add(pool);
    Storage[] pools = vmm.listPools();
    for(Storage p : pools) {
      String _name = p.name;
      pool.add(_name, _name);
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
    memory_units.setSelectedIndex(1);
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
    row = new Row();
    panel.add(row);
    Button b_save = new Button("Save");
    row.add(b_save);
    Button b_cancel = new Button("Cancel");
    row.add(b_cancel);

    b_disk_create.addClickListener((me, cmp) -> {
      ui.vm_disk = null;
      ui.vm_disk_init.run();
      ui.vm_disk_popup.setVisible(true);
    });
    b_disk_add.addClickListener((me, cmp) -> {
      ui.browse_path = ui.hardware.getPath();
      ui.browse_init.run();
      ui.browse_popup.setVisible(true);
    });
    b_disk_delete.addClickListener((me, cmp) -> {
      int idx = disk_list.getSelectedIndex();
      if (idx == -1) return;
      Disk disk = ui.hardware.disks.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Disk : " + disk.name);
      ui.confirm_action = () -> {
        ui.hardware.removeDisk(disk);
      };
      ui.confirm_popup.setVisible(true);
    });
    b_net_add.addClickListener((me, cmp) -> {
      ui.vm_network = null;
      ui.vm_network_complete = () -> {
        hardware.addNetwork(ui.vm_network);
        net_list.add(ui.vm_network.network);
      };
      ui.vm_network_popup.setVisible(true);
    });
    b_net_edit.addClickListener((me, cmp) -> {
      int idx = net_list.getSelectedIndex();
      if (idx == -1) return;
      ui.vm_network = hardware.networks.get(idx);
      ui.vm_network_complete = null;
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
      ui.device_usb_popup.setVisible(true);
    });
    b_dev_add_pci.addClickListener((me, cmp) -> {
      ui.device = null;
      ui.device_pci_init.run();
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
      };
      ui.confirm_popup.setVisible(true);
    });

    b_save.addClickListener((me, cmp) -> {
      errmsg.setText("");
      //TODO : get values
      hardware.pool = pool.getSelectedValue();
      if (!VirtualMachine.register(vm, hardware, vmm)) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      ui.split.setRightComponent(vmsPanel(ui));
    });
    b_cancel.addClickListener((me, cmp) -> {
      ui.split.setRightComponent(vmsPanel(ui));
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
    Button browse = new Button("Browse");
    tools.add(browse);
    Button format = new Button("Format");
    tools.add(format);
    Button delete = new Button("Delete");
    tools.add(delete);
    ListBox list = new ListBox();
    Storage[] pools = vmm.listPools();
    for(Storage pool : pools) {
      String _name = pool.name;
      list.add(_name);
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
      ui.browse_path = pool.getPath();
      ui.browse_init.run();
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

    row = new Row();
    panel.add(row);
    TextField errmsg = new TextField("");
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
    type.add("local", "Local");

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
        case "local":
          ui.setRightPanel(local_StoragePanel(new Storage(Storage.TYPE_LOCAL, _name, null), true, ui));
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
      case Storage.TYPE_LOCAL: return local_StoragePanel(store, false, ui);
    }
    return null;
  }

  private Panel nfs_StoragePanel(Storage store, boolean create, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _host = host.getText();
      String _path = path.getText();
      store.host = _host;
      store.path = _path;
      if (!store.register()) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      if (create) {
        Config.current.addStorage(store);
      }
      Config.current.save();
      ui.setRightPanel(storagePanel(ui));
    });
    cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });

    return panel;
  }

  private Panel iscsi_StoragePanel(Storage store, boolean create, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _host = host.getText();
      String _target = target.getText();
      String _init = initiator.getText();
      store.host = _host;
      store.target = _target;
      store.initiator = _init;
      if (!store.register()) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      if (create) {
        Config.current.addStorage(store);
      }
      Config.current.save();
      ui.setRightPanel(storagePanel(ui));
    });
    cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });

    return panel;
  }

  private Panel local_StoragePanel(Storage store, boolean create, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _dev = dev.getText();
      if (!new File(_dev).exists()) {
        dev.setBackColor(Color.red);
        return;
      }
      store.path = _dev;
      if (!store.register()) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      if (create) {
        Config.current.addStorage(store);
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

    ui.browse_init = () -> {
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

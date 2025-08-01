package service;

/** Config Service : jfKVM
 *
 * @author pquiring
 */

import java.io.*;
import java.awt.Font;
import java.awt.Graphics;
import java.util.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.vm.*;
import javaforce.linux.*;
import javaforce.net.*;
import javaforce.service.*;
import javaforce.webui.*;
import javaforce.webui.event.*;
import javaforce.webui.tasks.*;
import static javaforce.webui.Component.*;
import static javaforce.webui.event.KeyEvent.*;

public class ConfigService implements WebUIHandler {
  public static String version = "3.0";
  public static String appname = "jfKVM";
  public static boolean debug = false;
  public WebUIServer server;
  private KeyMgmt keys;
  private VMM vmm;
  private boolean genkey;

  private static final String[] filter_disks = new String[] {
    ".*[.]vmdk",
    ".*[.]qcow2",
    ".*[.]iso",
  };

  private static final String[] filter_all = new String[] {
    ".*[.]vmdk",
    ".*[.]qcow2",
    ".*[.]iso",
    ".*[.]jfvm",
    ".*[.]vmx",
  };

  //stats image dimensions
  private static final int img_width = 50 + 540 + 25;
  private static final int img_height = 25 + 250 + 25;

  private static final int data_width = 540;  //180 * 3
  private static final int data_height = 250;  //25 pixels per 10 units

  private static final int data_margin_left = 50;
  private static final int data_margin_right = 25;
  private static final int data_margin_top = 25;
  private static final int data_margin_bottom = 25;

  private static final int col_height = 25;

  private static final long _20sec_ns_ = 20L * 1000L * 1000L * 1000L;  //20 seconds in nano seconds

  private static final long day_ms = 1000L * 60L * 60L * 24L;
  private static final long hour_ms = 1000L * 60L * 60L;

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
    String keyfile = Paths.dataPath + "/jfkvm.key";
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
    public Button browse_button;
    public String browse_file;
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

    public Device[] devices;
    public Device device;
    public PopupPanel device_usb_popup;
    public Runnable device_usb_init;
    public PopupPanel device_pci_popup;
    public Runnable device_pci_init;
    public Runnable device_complete;

    public PopupPanel device_addr_pci_popup;
    public PopupPanel device_addr_usb_popup;
    public Runnable device_addr_pci_init;
    public Runnable device_addr_usb_init;
    public Address device_addr_addr;
    public Runnable device_addr_complete;

    public String[] ctrl_models;
    public String ctrl_type;
    public Controller ctrl;
    public PopupPanel ctrl_model_popup;
    public Runnable ctrl_model_init;
    public Runnable ctrl_model_complete;

    public Hardware hardware;  //editing VM hardware

    public NetworkInterface[] nics_iface;
    public NetworkBridge[] nics_bridge;
    public ArrayList<NetworkVirtual> nics_virt;
    public NetworkVLAN[] nics_vlans;

    public void resize() {
      if (right_panel == null || top_bottom_split == null || left_right_split == null || client == null) return;
      int height = top_bottom_split.getDividerPosition();
      right_panel.setHeight(height);
      int width = client.getWidth() - left_right_split.getDividerPosition();
      right_panel.setWidth(width);
    }

    public void setRightPanel(Panel panel) {
      if (panel == null) return;
      right_panel = panel;
      resize();
      left_right_split.setRightComponent(panel);
    }
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    if (name.equals("console")) {
      return getWebConsole(params, client);
    }
    if (Config.passwd == null) {
      return installPanel(client);
    }
    String user = (String)client.getProperty("user");
    if (user == null) {
      return loginPanel();
    }
    Panel panel = new Panel();
    UI ui = new UI();
    ui.client = client;
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

    ui.device_addr_pci_popup = device_adv_pci_PopupPanel(ui);
    panel.add(ui.device_addr_pci_popup);

    ui.device_addr_usb_popup = device_adv_usb_PopupPanel(ui);
    panel.add(ui.device_addr_usb_popup);

    ui.ctrl_model_popup = ctrl_scsi_PopupPanel(ui);
    panel.add(ui.ctrl_model_popup);

    int topSize = client.getHeight() - 128;
    ui.top_bottom_split = new SplitPanel(SplitPanel.HORIZONTAL);
    panel.add(ui.top_bottom_split);
    ui.top_bottom_split.setDividerPosition(topSize);

    int leftSize = 128;
    ui.left_right_split = new SplitPanel(SplitPanel.VERTICAL);
    ui.left_right_split = ui.left_right_split;
    ui.left_right_split.setDividerPosition(leftSize);
    ui.left_right_split.setLeftComponent(leftPanel(ui, leftSize));
    ui.left_right_split.setRightComponent(hostPanel(ui, HOST_WELCOME));

    Panel tasks = tasksPanel(ui);

    ui.top_bottom_split.setTopComponent(ui.left_right_split);
    ui.top_bottom_split.setBottomComponent(tasks);

/*
    b_help.addClickListener((MouseEvent e, Component button) -> {
        client.openURL("http://jfkvm.sourceforge.net/help.html");
      });
*/

    ui.top_bottom_split.addChangedListener((cmp) -> {
      ui.resize();
    });
    ui.left_right_split.addChangedListener((cmp) -> {
      ui.resize();
    });

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

  private PopupPanel vm_disk_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Disk");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    panel.add(grid);

    TextField name = new TextField("");
    grid.addRow(new Component[] {new Label("Name"), name});

    ComboBox pool = new ComboBox();
    grid.addRow(new Component[] {new Label("Pool"), pool});

    TextField path = new TextField("");
    path.setReadonly(true);
    grid.addRow(new Component[] {new Label("Path"), path});

    ComboBox type = new ComboBox();
    type.add("vmdk", "vmdk");
    type.add("qcow2", "qcow2");
    type.add("iso", "iso");
    grid.addRow(new Component[] {new Label("Format"), type});

    row = new Row();
    TextField size = new TextField("100");
    row.add(size);
    ComboBox size_units = new ComboBox();
    //size_units.add("B", "B");
    //size_units.add("KB", "KB");
    size_units.add("MB", "MB");
    size_units.add("GB", "GB");
    row.add(size_units);
    grid.addRow(new Component[] {new Label("Size"), row});

    ComboBox provision = new ComboBox();
    provision.add("thick", "Thick");
    provision.add("thin", "Thin");
    provision.setSelectedIndex(0);
    grid.addRow(new Component[] {new Label("Provision"), provision});

    TextField boot_order = new TextField("0");
    grid.addRow(new Component[] {new Label("Boot Order"), boot_order});

    ComboBox bus = new ComboBox();
    bus.add("auto", "auto");
    bus.add("sata", "sata");
    bus.add("scsi", "scsi");
    bus.add("ide", "ide");
    grid.addRow(new Component[] {new Label("Interface"), bus});

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.vm_disk_init = () -> {
      errmsg.setText("");
      ArrayList<Storage> pools = Config.current.pools;
      pool.clear();
      if (ui.vm_disk == null) {
        //create
        name.setText(ui.hardware.getNextDiskName());
        name.setReadonly(false);
        Storage disk_pool = null;
        int idx = 0;
        for(Storage _pool : pools) {
          String _name = _pool.name;
          pool.add(_name, _name);
          if (_name.equals(ui.hardware.pool)) {
            pool.setSelectedIndex(idx);
            disk_pool = _pool;
          }
          idx++;
        }
        pool.setReadonly(false);
        if (disk_pool != null) {
          path.setText(disk_pool.getPath() + "/" + ui.hardware.name);
        } else {
          path.setText(ui.hardware.getPath());
        }
        type.setSelectedIndex(0);
        type.setReadonly(false);
        bus.setSelectedIndex(0);
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
        int idx = 0;
        for(Storage _pool : pools) {
          String _name = _pool.name;
          pool.add(_name, _name);
          if (_name.equals(ui.hardware.pool)) {
            pool.setSelectedIndex(idx);
          }
          idx++;
        }
        pool.setReadonly(true);
        path.setText(ui.vm_disk.getPath());
        switch (ui.vm_disk.type) {
          case Disk.TYPE_VMDK: type.setSelectedIndex(0); break;
          case Disk.TYPE_QCOW2: type.setSelectedIndex(1); break;
          case Disk.TYPE_ISO: type.setSelectedIndex(2); break;
        }
        type.setReadonly(true);
        switch (ui.vm_disk.target_bus) {
          case "auto": bus.setSelectedIndex(0); break;
          case "sata": bus.setSelectedIndex(1); break;
          case "scsi": bus.setSelectedIndex(2); break;
          case "ide": bus.setSelectedIndex(3); break;
        }
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

    pool.addChangedListener((cmp) -> {
      //update path
      Storage disk_pool = vmm.getPoolByName(pool.getSelectedValue());
      if (disk_pool == null) return;
      path.setText(disk_pool.getPath() + "/" + ui.hardware.name);
    });

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      boolean create = ui.vm_disk == null;
      int _type = type.getSelectedIndex();
      if (create && type.getSelectedIndex() == 2) {
        errmsg.setText("Error:can not create iso files");
        return;
      }
      String _bus = bus.getSelectedText();
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
      int _provision = provision.getSelectedIndex();
      if (create) {
        //create
        ui.vm_disk = new Disk();
        ui.vm_disk.pool = pool.getSelectedValue();
        ui.vm_disk.folder = ui.hardware.name;
        ui.vm_disk.name = _name;
        ui.vm_disk.type = _type;
        ui.vm_disk.size = new Size(_size, _size_unit);
        ui.vm_disk.boot_order = _boot_order;
        ui.vm_disk.target_bus = _bus;
        if (!ui.vm_disk.create(_provision)) {
          errmsg.setText("Error:Failed to create disk");
          return;
        }
      } else {
        //update (only size and boot order can be changed)
        ui.vm_disk.size = new Size(_size, _size_unit);
        ui.vm_disk.resize(vmm.getPoolByName(ui.hardware.pool));
        ui.vm_disk.boot_order = _boot_order;
        ui.vm_disk.target_bus = _bus;
      }
      if (ui.vm_disk_complete != null) {
        ui.vm_disk_complete.run();
      }
      ui.vm_disk_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.vm_disk_popup.setVisible(false);
    });

    panel.setOnClose( () -> {
      cancel.click();
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
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    panel.add(grid);

    ComboBox models = new ComboBox();
    for(String model : nic_models) {
      models.add(model, model);
    }
    grid.addRow(new Component[] {new Label("Model"), models});

    ComboBox networks = new ComboBox();
    grid.addRow(new Component[] {new Label("Network"), networks});

    row = new Row();
    TextField mac = new TextField("");
    row.add(mac);
    row.add(new Label("(leave blank to generate)"));
    grid.addRow(new Component[] {new Label("MAC"), row});

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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

    panel.setOnClose( () -> {
      cancel.click();
    });
    return panel;
  }

  private PopupPanel network_vlan_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Network VLAN");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    panel.add(grid);

    TextField name = new TextField("");
    grid.addRow(new Component[] {new Label("Name"), name});

    ComboBox bridge = new ComboBox();
    grid.addRow(new Component[] {new Label("Switch"), bridge});

    TextField vlan = new TextField("");
    grid.addRow(new Component[] {new Label("VLAN"), vlan});

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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
      String _bridge = bridge.getSelectedValue();
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

    panel.setOnClose( () -> {
      cancel.click();
    });
    return panel;
  }

  private PopupPanel network_bridge_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Network Bridge");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    panel.add(grid);

    TextField name = new TextField("");
    grid.addRow(new Component[] {new Label("Name"), name});

    ComboBox iface = new ComboBox();
    grid.addRow(new Component[] {new Label("Physical NIC"), iface});

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.network_bridge_init = () -> {
      boolean create = ui.network_bridge == null;
      iface.clear();
      NetworkInterface[] nics = NetworkInterface.listPhysical();
      NetworkBridge[] bridges = NetworkBridge.list();
      for(NetworkInterface nic : nics) {
        if (nic.name.equals("lo")) continue;
        if (nic.name.equals("ovs-system")) continue;
        boolean ok = true;
        for(NetworkVirtual vnic : Config.current.nics) {
          if (vnic.name.equals(nic.name)) {
            ok = false;
            break;
          }
        }
        for(NetworkBridge bnic : bridges) {
          if (bnic.name.equals(nic.name)) {
            ok = false;
            break;
          }
        }
        if (!ok) continue;
        String val = nic.name;
        iface.add(val, val);
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
      String _iface = iface.getSelectedValue();
      if (_iface == null || _iface.length() == 0) {
        errmsg.setText("Error:invalid interface");
        return;
      }
      int idx = _iface.indexOf(':');
      if (idx != -1) {
        _iface = _iface.substring(0, idx);
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

    panel.setOnClose( () -> {
      cancel.click();
    });
    return panel;
  }

  private PopupPanel network_virtual_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Virtual Network");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    panel.add(grid);

    TextField name = new TextField("");
    grid.addRow(new Component[] {new Label("Name"), name});

    ComboBox bridge = new ComboBox();
    grid.addRow(new Component[] {new Label("Switch"), bridge});

    TextField ip = new TextField("");
    grid.addRow(new Component[] {new Label("IP"), ip});

    TextField netmask = new TextField("");
    grid.addRow(new Component[] {new Label("Netmask"), netmask});

    TextField vlan = new TextField("");
    grid.addRow(new Component[] {new Label("VLAN"), vlan});

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.network_virtual_init = () -> {
      boolean create = ui.network_virtual == null;
      bridge.clear();
      NetworkBridge[] nics = NetworkBridge.list(NetworkBridge.TYPE_OS);
      for(NetworkBridge nic : nics) {
        bridge.add(nic.name, nic.name);
      }
      if (create) {
        name.setText("");
//        mac.setText("");
        ip.setText("192.168.1.2");
        netmask.setText("255.255.255.0");
        vlan.setText("0");
        accept.setText("Create");
      } else {
        name.setText(ui.network_virtual.name);
//        mac.setText(ui.network_virtual.mac);
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
      String _bridge = bridge.getSelectedValue();
      if (_bridge == null || _bridge.length() == 0) {
        errmsg.setText("Error:invalid bridge");
        return;
      }
/*
      String _mac = mac.getText();
      if (_mac.length() > 0) {
        if (!MAC.valid(_mac)) {
          errmsg.setText("Error:invalid mac");
          return;
        }
      } else {
        _mac = MAC.generate();
      }
*/
      String _mac = "";  //not supported
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
        errmsg.setText("Error:No bridge to create virtual network");
        return;
      }
      NetworkVirtual nic = NetworkVirtual.createVirtual(_name, sel_bridge, _mac, _ip, _netmask, _vlan);
      if (nic == null) {
        errmsg.setText("Error:Failed to create virtual network");
        return;
      }
      Config.current.addNetworkVirtual(nic);
      if (ui.network_virtual_complete != null) {
        ui.network_virtual_complete.run();
      }
      ui.network_virtual_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.network_virtual_popup.setVisible(false);
    });

    panel.setOnClose( () -> {
      cancel.click();
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

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.device_usb_init = () -> {
      boolean create = ui.device == null;
      String _sel = null;
      if (create) {
        _sel = "";
        accept.setText("Create");
      } else {
        _sel = ui.device.name;
        accept.setText("Edit");
      }
      ui.devices = Device.list(Device.TYPE_USB);
      int idx = 0;
      int _sel_idx = -1;
      for(Device dev: ui.devices) {
        if (dev.name.equals(_sel)) {
          _sel_idx = idx;
        }
        device.add(dev.name, dev.name + ":" + dev.desc);
        idx++;
      }
      if (_sel_idx != -1) {
        device.setSelectedIndex(_sel_idx);
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = device.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.device = ui.devices[idx];
      if (ui.device_complete != null) {
        ui.device_complete.run();
      }
      ui.device_usb_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.device_usb_popup.setVisible(false);
    });

    panel.setOnClose( () -> {
      cancel.click();
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

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.device_pci_init = () -> {
      boolean create = ui.device == null;
      String _sel = null;
      if (create) {
        _sel = "";
        accept.setText("Create");
      } else {
        _sel = ui.device.name;
        accept.setText("Edit");
      }
      ui.devices = Device.list(Device.TYPE_PCI);
      int idx = 0;
      int _sel_idx = -1;
      for(Device dev: ui.devices) {
        if (dev.name.equals(_sel)) {
          _sel_idx = idx;
        }
        device.add(dev.name, dev.name + ":" + dev.desc);
        idx++;
      }
      if (_sel_idx != -1) {
        device.setSelectedIndex(_sel_idx);
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = device.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.device = ui.devices[idx];
      if (ui.device_complete != null) {
        ui.device_complete.run();
      }
      ui.device_pci_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.device_pci_popup.setVisible(false);
    });

    panel.setOnClose( () -> {
      cancel.click();
    });
    return panel;
  }

  //PCI Address : domain, bus, slot, function
  private PopupPanel device_adv_pci_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("PCI Device Guest Address");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    CheckBox auto = new CheckBox("Auto");
    row.add(auto);

    row = new Row();
    panel.add(row);
    row.add(new Label("Domain"));
    TextField domain = new TextField("");
    row.add(domain);

    row = new Row();
    panel.add(row);
    row.add(new Label("Bus"));
    TextField bus = new TextField("");
    row.add(bus);

    row = new Row();
    panel.add(row);
    row.add(new Label("Slot"));
    TextField slot = new TextField("");
    row.add(slot);

    row = new Row();
    panel.add(row);
    row.add(new Label("Function"));
    TextField function = new TextField("");
    row.add(function);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.device_addr_pci_init = () -> {
      if (ui.device_addr_addr == null) {
        ui.device_addr_addr = new Address();
      }
      auto.setSelected(ui.device_addr_addr.getType().equals("auto"));
      domain.setText(ui.device_addr_addr.getDomain());
      bus.setText(ui.device_addr_addr.getBus(true));
      slot.setText(ui.device_addr_addr.getSlot());
      function.setText(ui.device_addr_addr.getFunction());
    };

    accept.addClickListener((me, cmp) -> {
      boolean _auto = auto.isSelected();
      String _domain = Address.cleanHex(domain.getText(), 0xffff);
      String _bus = Address.cleanHex(bus.getText(), 0xff);
      String _slot = Address.cleanHex(slot.getText(), 0xff);
      String _function = Address.cleanHex(function.getText(), 0xf);
      if (_auto) {
        ui.device_addr_addr.addr_type = "auto";
      } else {
        ui.device_addr_addr.addr_type = "pci";
      }
      ui.device_addr_addr.domain = _domain;
      ui.device_addr_addr.bus = _bus;
      ui.device_addr_addr.slot = _slot;
      ui.device_addr_addr.function = _function;

      ui.device_addr_pci_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.device_addr_pci_popup.setVisible(false);
    });

    panel.setOnClose( () -> {
      cancel.click();
    });
    return panel;
  }

  //USB : bus, port
  private PopupPanel device_adv_usb_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("USB Device Guest Address");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    CheckBox auto = new CheckBox("Auto");
    row.add(auto);

    row = new Row();
    panel.add(row);
    row.add(new Label("Bus"));
    TextField bus = new TextField("");
    row.add(bus);

    row = new Row();
    panel.add(row);
    row.add(new Label("Port"));
    TextField port = new TextField("");
    row.add(port);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    ui.device_addr_usb_init = () -> {
      if (ui.device_addr_addr == null) {
        ui.device_addr_addr = new Address();
      }
      auto.setSelected(ui.device_addr_addr.getType().equals("auto"));
      bus.setText(ui.device_addr_addr.getBus(false));
      port.setText(ui.device_addr_addr.getPort());
    };

    accept.addClickListener((me, cmp) -> {
      boolean _auto = auto.isSelected();
      String _bus = Address.cleanDec(bus.getText(), 0xff);
      String _port = Address.cleanDec(port.getText(), 0xff);
      if (_auto) {
        ui.device_addr_addr.addr_type = "auto";
      } else {
        ui.device_addr_addr.addr_type = "usb";
      }
      ui.device_addr_addr.bus = _bus;
      ui.device_addr_addr.port = _port;

      ui.device_addr_usb_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.device_addr_usb_popup.setVisible(false);
    });

    panel.setOnClose( () -> {
      cancel.click();
    });
    return panel;
  }

  private PopupPanel ctrl_scsi_PopupPanel(UI ui) {
    PopupPanel panel = new PopupPanel("Controller");
    panel.setPosition(256, 128);
    panel.setModal(true);
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Type:"));
    Label type = new Label("");
    row.add(type);

    row = new Row();
    panel.add(row);
    row.add(new Label("Model:"));
    ComboBox model = new ComboBox();
    row.add(model);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Create");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ui.ctrl_model_init = () -> {
      boolean create = ui.ctrl == null;
      type.setText(ui.ctrl_type);
      String _sel = null;
      if (create) {
        _sel = "";
        accept.setText("Create");
        ui.ctrl = new Controller(ui.ctrl_type, "auto");
      } else {
        _sel = ui.ctrl.model;
        accept.setText("Edit");
      }
      int idx = 0;
      int _sel_idx = -1;
      model.clear();
      for(String _model: ui.ctrl_models) {
        if (_model.equals(_sel)) {
          _sel_idx = idx;
        }
        model.add(_model, _model);
        idx++;
      }
      if (_sel_idx != -1) {
        model.setSelectedIndex(_sel_idx);
      }
    };

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = model.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.ctrl.model = model.getSelectedText();
      if (ui.ctrl_model_complete != null) {
        ui.ctrl_model_complete.run();
      }
      ui.ctrl_model_popup.setVisible(false);
    });
    cancel.addClickListener((me, cmp) -> {
      ui.ctrl_model_popup.setVisible(false);
    });

    panel.setOnClose( () -> {
      cancel.click();
    });
    return panel;
  }

  public Panel installPanel(WebUIClient client) {
    Panel panel = new Panel();
    panel.removeClass("column");
    panel.setAlign(CENTER);
    InnerPanel inner = new InnerPanel("jfKVM Setup");
    inner.setAlign(CENTER);
    inner.setMaxWidth();
    inner.setMaxHeight();
    Row row;
    Label header = new Label("jfKVM has not been setup yet, please supply the admin password.");
    inner.add(header);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    inner.add(errmsg);

    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    grid.setAlign(CENTER);
    inner.add(grid);

    TextField password = new TextField("");
    password.setPassword(true);
    grid.addRow(new Component[] {new Label("Password:"), password});

    TextField confirm = new TextField("");
    confirm.setPassword(true);
    grid.addRow(new Component[] {new Label("Confirm:"), confirm});

    row = new Row();
    inner.add(row);
    Button login = new Button("Save");
    row.add(login);

    login.addClickListener( (MouseEvent m, Component c) -> {
      errmsg.setText("");
      if (Config.passwd != null) {
        errmsg.setText("Already configured, please refresh");
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
      Config.passwd = new Password(Password.TYPE_SYSTEM, "jfkvm", passTxt1);
      Config.passwd.save();
      client.setPanel(getPanel("root", null, client));
    });
    panel.add(inner);
    return panel;
  }

  private Panel loginPanel() {
    Panel panel = new Panel();
    panel.removeClass("column");
    InnerPanel inner = new InnerPanel(appname + " Login");
    inner.setAlign(CENTER);
    inner.setMaxWidth();
    inner.setMaxHeight();
    Label msg = new Label("");
    inner.add(msg);

    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    grid.setAlign(CENTER);
    inner.add(grid);

    TextField username = new TextField("");
    grid.addRow(new Component[] {new Label("Username"), username});

    TextField password = new TextField("");
    password.setPassword(true);
    grid.addRow(new Component[] {new Label("Password"), password});

    Button login = new Button("Login");
    inner.add(login);

    username.addKeyDownListener((ke, cmp) -> {
      if (ke.keyCode == VK_ENTER) {
        password.setFocus();
      }
    });

    password.addKeyDownListener((ke, cmp) -> {
      if (ke.keyCode == VK_ENTER) {
        login.click();
      }
    });

    login.addClickListener( (MouseEvent m, Component c) -> {
      String userTxt = username.getText();
      String passTxt = password.getText();
      WebUIClient webclient = c.getClient();
      if (passTxt.equals(Config.passwd.password)) {
        webclient.setProperty("user", userTxt);
        webclient.setPanel(getPanel("root", null, webclient));
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
      ui.setRightPanel(hostPanel(ui, HOST_WELCOME));
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

  private Panel welcomePanel(UI ui) {
    Panel panel = new Panel();
    panel.add(new Label("jfKVM/" + version));
    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button help = new Button("Help");
    tools.add(help);
    Button license = new Button("License");
    tools.add(license);
    Button support = new Button("Support");
    tools.add(support);
    TextArea msg = new TextArea(
      "Welcome to jfKVM!\n" +
      "\n" +
      "jfKVM is a Type1 hypervisor using Linux KVM/QEMU\n" +
      "Virtual Machine Management System using libvirt\n" +
      "Designed to be a drop-in replacement for VMWare ESXi hypervisor.\n" +
      "\n" +
      "Features supported:\n" +
      " - Linux and Windows guests\n" +
      " - Disks : vmdk, qcow2, iso (thick and thin provisioning)\n" +
      " - Networking : bridge, guests on VLANs\n" +
      " - Storage Pools : Local Disk Partition, NFS, iSCSI Shared Disks, Gluster and Ceph Replicated Disks (vSAN)\n" +
      " - provide guest with direct access to host devices\n" +
      " - import vmware machines\n" +
      " - live/offline compute migration\n" +
      " - offline data migration/clone\n" +
      " - autostart machines\n" +
      " - iSCSI requires manual setup (see online help for more info)\n" +
      "\n" +
      "Not supported:\n" +
      " - VMFS storage pools\n" +
      " - vmdk file split into multiple 2GB extents\n" +
      "\n" +
      "License: LGPL\n" +
      "\n" +
      "Thanks to Broadcom for the motivation to create this project! &#x263a;\n" +  //unicode smiley face
      "\n" +
      "By : Peter Quiring\n" +
      "\n" +
      "\n"
    );
    msg.setMaxWidth();
    msg.setMaxHeight();
    msg.setFontSize(16);
    panel.add(msg);

    help.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfkvm/docs/help.html");
    });
    license.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/license.txt");
    });
    support.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://github.com/pquiring/javaforce/issues");
    });

    panel.setMaxWidth();
    panel.setMaxHeight();

    return panel;
  }

  private static final int HOST_WELCOME = 0;
  private static final int HOST_HOST = 1;
  private static final int HOST_CONFIG = 2;
  private static final int HOST_AUTOSTART = 3;
  private static final int HOST_CLUSTER = 4;
  private static final int HOST_ADMIN = 5;
  private static final int HOST_SERVICES = 6;

  private Panel hostPanel(UI ui, int idx) {
    TabPanel panel = new TabPanel();
    panel.addTab(welcomePanel(ui), "Welcome");
    panel.addTab(hostHostPanel(ui), "Host");
    panel.addTab(hostConfigPanel(ui), "Settings");
    panel.addTab(hostAutoStartPanel(ui), "Auto Start");
    panel.addTab(hostClusterPanel(ui), "Cluster");
    panel.addTab(hostAdminPanel(ui), "Admin");
    panel.addTab(hostServicesPanel(ui), "Services");
    panel.setTabIndex(idx);
    return panel;
  }

  private Panel hostHostPanel(UI ui) {
    Panel panel = new Panel();
    panel.add(new Label("jfKVM/" + version));
    Row row;

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button reboot = new Button("Reboot");
    tools.add(reboot);
    Button shutdown = new Button("Shutdown");
    tools.add(shutdown);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Memory Total:"));
    Size total_memory = new Size(VMHost.total_memory());
    row.add(new Label(total_memory.toString()));

    row = new Row();
    panel.add(row);
    row.add(new Label("Free Total:"));
    Size free_memory = new Size(VMHost.free_memory());
    row.add(new Label(free_memory.toString()));

    row = new Row();
    panel.add(row);
    row.add(new Label("CPU Load:"));
    long cpu_load = VMHost.cpu_load();
    row.add(new Label(Long.toString(cpu_load) + '%'));

    refresh.addClickListener((me, cmp) -> {
      ui.setRightPanel(hostPanel(ui, HOST_HOST));
    });

    reboot.addClickListener((me, cmp) -> {
      if (vmm.any_vm_running()) {
        errmsg.setText("Please shutdown or suspend VMs before rebooting host!");
        return;
      }
      ui.confirm_message.setText("Reboot host?");
      ui.confirm_button.setText("Reboot");
      ui.confirm_action = () -> {
        Task task = new Task("Reboot") {
          public void doTask() {
            try {
              Linux.reboot();
              setStatus("Completed");
            } catch (Exception e) {
              setStatus("Error:Reboot failed, check logs.");
              JFLog.log(e);
            }
            genkey = false;
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });

    shutdown.addClickListener((me, cmp) -> {
      if (vmm.any_vm_running()) {
        errmsg.setText("Please shutdown or suspend VMs before host shutdown!");
        return;
      }
      ui.confirm_message.setText("Shutdown host?");
      ui.confirm_button.setText("Shutdown");
      ui.confirm_action = () -> {
        Task task = new Task("Shutdown") {
          public void doTask() {
            try {
              Linux.shutdown();
              setStatus("Completed");
            } catch (Exception e) {
              setStatus("Error:Shutdown failed, check logs.");
              JFLog.log(e);
            }
            genkey = false;
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });

    return panel;
  }

  private Panel hostConfigPanel(UI ui) {
    Panel panel = new Panel();
    Row row;

    GridLayout grid = new GridLayout(3, 0, new int[] {RIGHT, LEFT, LEFT});
    panel.add(grid);

    String ip = Config.current.ip_mgmt;
    IP4[] ips = IP4.list(true);
    if (ip.length() == 0 && ips != null && ips.length > 0) {
      ip = ips[0].toString();
    }
    TextField ip_mgmt = new TextField(ip);
    grid.addRow(new Component[] {new Label("Management IP"), ip_mgmt});

    ip = Config.current.ip_storage;
    TextField ip_storage = new TextField(ip);
    grid.addRow(new Component[] {new Label("Storage IP"), ip_storage, new Label("(optional)")});

    TextField iqn = new TextField(Storage.getSystemIQN());
    Button iqn_generate = new Button("Generate");
    grid.addRow(new Component[] {new Label("iSCSI Initiator IQN"), iqn, iqn_generate});

    TextField stats_days = new TextField(Integer.toString(Config.current.stats_days));
    grid.addRow(new Component[] {new Label("Stats Retention (days)"), stats_days, new Label("(1-365) (default:3)")});

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button save = new Button("Save");
    tools.add(save);

    row = new Row();
    panel.add(row);
    Label msg = new Label("");
    row.add(msg);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    save.addClickListener((me, cmp) -> {
      msg.setText("");
      String _stats_days_str = vmm.cleanNumber(stats_days.getText());
      if (_stats_days_str.length() == 0 || !_stats_days_str.equals(stats_days.getText())) {
        errmsg.setText("Error:Stats Retention is invalid");
        return;
      }
      int _stats_days_int = Integer.valueOf(_stats_days_str);
      if (_stats_days_int < 1 || _stats_days_int > 365) {
        errmsg.setText("Error:Stats Retention is invalid");
        return;
      }
      errmsg.setText("");
      Config.current.ip_mgmt = ip_mgmt.getText();
      Config.current.ip_storage = ip_storage.getText();
      Config.current.stats_days = _stats_days_int;
      Storage.setSystemIQN(iqn.getText());
      Config.current.save();
      msg.setText("Settings saved");
    });

    iqn_generate.addClickListener((me, cmp) -> {
      Config.current.ip_mgmt = ip_mgmt.getText();
      iqn.setText(IQN.generate(Config.current.ip_mgmt));
      msg.setText("Client IQN regenerated");
    });

    return panel;
  }

  private Panel hostAutoStartPanel(UI ui) {
    Panel panel = new Panel();
    Row row;

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button save = new Button("Save");
    tools.add(save);

    row = new Row();
    panel.add(row);
    Table table = new Table(new int[] {150, 50}, col_height, 2, 0);
    row.add(table);
//    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    table.addRow(new String[] {"Name", "Order"});
    VirtualMachine[] vms = VirtualMachine.list();
    TextField[] tfs = new TextField[vms.length];
    {
      ArrayList<String> auto_start_vms = Config.current.auto_start_vms;
      int vmidx = 0;
      for(VirtualMachine vm : vms) {
        int order = 0;
        int auto_start_idx = 0;
        for(String auto_start_name : auto_start_vms) {
          if (vm.name.equals(auto_start_name)) {
            order = auto_start_idx + 1;
            break;
          }
        }
        tfs[vmidx] = new TextField(Integer.toString(order));
        table.addRow(new Component[] {new Label(vm.name), tfs[vmidx]});
        vmidx++;
      }
    }

    row = new Row();
    panel.add(row);
    Label msg = new Label("");
    row.add(msg);

    save.addClickListener((me, cmp) -> {
      int idx = 0;
      ArrayList<String> auto_start_vms = new ArrayList<>();
      ArrayList<Integer> auto_start_order = new ArrayList<>();
      for(TextField tf : tfs) {
        String _value = vmm.cleanNumber(tf.getText());
        if (_value.length() == 0) {
          _value = "0";
        }
        tf.setText(_value);
        int order = Integer.valueOf(_value);
        if (order > 0) {
          auto_start_vms.add(vms[idx].name);
          auto_start_order.add(order);
        }
        idx++;
      }
      //sort by auto_start_order
      for(int i1=0;i1<auto_start_vms.size()-1;i1++) {
        for(int i2=i1+1;i2<auto_start_vms.size();i2++) {
          int o1 = auto_start_order.get(i1);
          int o2 = auto_start_order.get(i2);
          if (o1 > o2) {
            //swap i1 & i2
            String n1 = auto_start_vms.get(i1);
            String n2 = auto_start_vms.get(i2);
            auto_start_vms.set(i1, n2);
            auto_start_vms.set(i2, n1);
            auto_start_order.set(i1, o2);
            auto_start_order.set(i2, o1);
          }
        }
      }

      //save list to config
      Config.current.auto_start_vms = auto_start_vms;

      Config.current.save();
      msg.setText("Settings saved");
    });

    return panel;
  }

  private Panel hostClusterPanel(UI ui) {
    Panel panel = new Panel();
    Row row;

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button help = new Button("Help");
    tools.add(help);

    row = new Row();
    panel.add(row);
    row.add(new Label("Local Key:"));
    Button local_key_generate = new Button("Generate");
    row.add(local_key_generate);
    Label local_key_status = new Label("Status:" + Config.current.getKeyStatus());
    row.add(local_key_status);

    row = new Row();
    panel.add(row);
    row.add(new Label("Local Token:"));
    TextField local_token = new TextField(Config.current.getToken());
    local_token.setReadonly(true);
    row.add(local_token);
    Button local_token_generate = new Button("Generate");
    row.add(local_token_generate);

    row = new Row();
    panel.add(row);
    row.add(new Label("Remote Token:"));
    TextField remote_token = new TextField("");
    row.add(remote_token);
    row.add(new Label("Remote Host IP:"));
    TextField remote_host = new TextField("");
    row.add(remote_host);
    Button connect = new Button("Connect");
    row.add(connect);

    row = new Row();
    panel.add(row);
    Label local_errmsg = new Label("");
    local_errmsg.setColor(Color.red);
    row.add(local_errmsg);

    row = new Row();
    panel.add(row);
    row.add(new Label("Remote Hosts:"));

    ToolBar tools2 = new ToolBar();
    panel.add(tools2);
    Button gluster = new Button("Gluster Probe");
    tools2.add(gluster);
    Button ceph = null;
    if (!Ceph.exists()) {
      ceph = new Button("Ceph Setup");
      tools2.add(ceph);
    }
    Button remove = new Button("Remove Host");
    tools2.add(remove);

    row = new Row();
    panel.add(row);
    Label remote_errmsg = new Label("");
    remote_errmsg.setColor(Color.red);
    row.add(remote_errmsg);

    row = new Row();
    panel.add(row);
    Table table = new Table(new int[] {150, 150, 75, 75, 75, 150, 150}, col_height, 7, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    table.addRow(new String[] {"Host", "Hostname", "Version", "Online", "Valid", "Gluster", "Ceph"});

    Host[] hosts = Config.current.getHosts();

    try {
      for(Host host : hosts) {
        table.addRow(host.getState());
      }
    } catch (Exception e) {
      JFLog.log(e);
    }

    if (Gluster.exists()) {
      row = new Row();
      panel.add(row);

      row.add(new Label("Gluster Status: "));

      Label gluster_status = new Label("Checking...");
      row.add(gluster_status);

      new Thread() { public void run() {
        gluster_status.setText(Gluster.getStatus());
      } }.start();
    }

    if (Ceph.exists()) {
      row = new Row();
      panel.add(row);

      Button ceph_admin = new Button("Ceph Admin");
      row.add(ceph_admin);

      ceph_admin.addClickListener((me, cmp) -> {
        cmp.getClient().openURL("https://" + Config.current.ip_mgmt + ":8443");
      });

      row.add(new Label("Ceph Status: "));

      Label ceph_status = new Label("Checking...");
      row.add(ceph_status);

      new Thread() { public void run() {
        ceph_status.setText(Ceph.getStatus());
      } }.start();
    }

    refresh.addClickListener((me, cmp) -> {
      ui.setRightPanel(hostPanel(ui, HOST_CLUSTER));
    });

    help.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfkvm/docs/help_cluster.html");
    });

    local_key_generate.addClickListener((me, cmp) -> {
      if (Config.current.isKeyValid()) {
        local_errmsg.setText("Key is already valid");
        return;
      }
      if (genkey) {
        local_errmsg.setText("Busy");
        return;
      }
      Task task = new Task("Generate Key") {
        public void doTask() {
          try {
            try {new File("/root/cluster/localhost").delete();} catch (Exception e) {}
            try {new File("/root/cluster/localhost.pub").delete();} catch (Exception e) {}
            try {new File("/root/.ssh/authorized_keys").delete();} catch (Exception e) {}
            ShellProcess sp = new ShellProcess();
            sp.run(new String[] {"ssh-keygen", "-b", "2048", "-t", "rsa", "-f", Paths.clusterPath + "/localhost", "-q", "-N", ""}, true);
            if (sp.getErrorLevel() != 0) {
              throw new Exception("ErrorLevel != 0");
            }
            new File("/root/.ssh").mkdir();
            new File("/root/.ssh/authorized_keys").delete();
            sp.run(new String[] {"mv", Paths.clusterPath + "/localhost.pub", "/root/.ssh/authorized_keys"}, true);
            setStatus("Completed");
          } catch (Exception e) {
            setStatus("Error:Generate keys failed, check logs.");
            JFLog.log(e);
          }
          genkey = false;
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });

    local_token_generate.addClickListener((me, cmp) -> {
      ui.confirm_message.setText("Regenerate token will disconnect other hosts.");
      ui.confirm_button.setText("Generate");
      ui.confirm_action = () -> {
        Config.current.token = JF.generateUUID();
        Config.current.save();
        ui.setRightPanel(hostPanel(ui, HOST_CLUSTER));
      };
      ui.confirm_popup.setVisible(true);
    });

    connect.addClickListener((me, cmp) -> {
      //download to clusterPath
      String _remote_host = remote_host.getText();
      String _remote_token = remote_token.getText();
      if (!IP4.isIP(_remote_host)) {
        local_errmsg.setText("Remote Host IP address required!");
        return;
      }
      if (_remote_host.equals(Config.current.ip_mgmt) || _remote_host.equals("127.0.0.1")) {
        local_errmsg.setText("Can not connect to localhost");
        return;
      }
      Task task = new Task("Connect to host:" + _remote_host) {
        public void doTask() {
          try {
            HTTPS https = new HTTPS();
            if (!https.open(_remote_host)) throw new Exception("connect failed");
            byte[] data = https.get("/api/keyfile?token=" + _remote_token);
            if (Config.current.saveHost(_remote_host, data, _remote_token)) {
              setStatus("Connected to host:" + _remote_host);
            } else {
              setStatus("Connection failed, check logs.");
            }
            https.close();
          } catch (Exception e) {
            setStatus("Connection failed, check logs.");
            JFLog.log(e);
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });

    gluster.addClickListener((me, cmp) -> {
      remote_errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        remote_errmsg.setText("Error:no selection");
        return;
      }
      Host host = hosts[idx];
      String host_host = host.host;
      if (!host.online) {
        remote_errmsg.setText("Host is not online");
        return;
      }
      Task task = new Task("Gluster Probe host:" + host_host) {
        public void doTask() {
          try {
            if (Gluster.probe(host_host)) {
              Hosts.hosts.check_now();
              setStatus("Completed");
            } else {
              setStatus("Probe failed, check logs.");
            }
          } catch (Exception e) {
            setStatus("Probe failed, check logs.");
            JFLog.log(e);
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });

    if (ceph != null) {
      ceph.addClickListener((me, cmp) -> {
        remote_errmsg.setText("");
        if (hosts.length < 2) {
          remote_errmsg.setText("Ceph requires a cluster of 3 or more hosts");
          return;
        }
        for(Host host : hosts) {
          if (!host.online || !host.valid) {
            remote_errmsg.setText("Host is not online:" + host.hostname);
            return;
          }
        }
        ui.confirm_message.setText("Ceph setup");
        ui.confirm_action = () -> {
          Task task = new Task("Ceph setup") {
            public void doTask() {
              try {
                //check if already setup
                if (Ceph.exists()) {
                  setStatus("Ceph is already setup!");
                  return;
                }

                //check for known broken versions
                String distro = Linux.getDistro();
                if (distro == null) distro = "unknown";
                String verstr = Linux.getVersion();
                if (verstr == null) verstr = "0";
                float verfloat = Float.valueOf(verstr);
                if (distro.equals("Debian") && verfloat < 13) {
                  //Debian/12 is known to have broken Ceph version
                  setStatus("Debian/12 or less is not supported, please upgrade!");
                  return;
                }

                //start ceph setup progress
                Config.current.ceph_setup = true;
                if (Ceph.setup(this)) {
                  setStatus("Completed");
                } else {
                  setStatus("Ceph setup failed, check logs.");
                }
                Config.current.ceph_setup = false;
              } catch (Exception e) {
                setStatus("Ceph setup failed, check logs.");
                JFLog.log(e);
              }
            }
          };
          Tasks.tasks.addTask(ui.tasks, task);
        };
        ui.confirm_button.setText("Start");
        ui.confirm_popup.setVisible(true);
      });
    }

    remove.addClickListener((me, cmp) -> {
      remote_errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        remote_errmsg.setText("Error:no selection");
        return;
      }
      ui.confirm_message.setText("Remove Host:" + hosts[idx].host);
      ui.confirm_action = () -> {
        String host = hosts[idx].host;
        Config.current.removeHost(host);
        ui.setRightPanel(hostPanel(ui, HOST_CLUSTER));
      };
      ui.confirm_button.setText("Remove");
      ui.confirm_popup.setVisible(true);
    });

    return panel;
  }

  private Panel hostAdminPanel(UI ui) {
    Panel panel = new Panel();
    Row row;

    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    panel.add(grid);

    TextField old_pass = new TextField("");
    old_pass.setPassword(true);
    grid.addRow(new Component[] {new Label("Current Password"), old_pass});

    TextField new_pass = new TextField("");
    new_pass.setPassword(true);
    grid.addRow(new Component[] {new Label("New Password"), new_pass});

    TextField cfm_pass = new TextField("");
    cfm_pass.setPassword(true);
    grid.addRow(new Component[] {new Label("Confirm Password"), cfm_pass});

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button save = new Button("Save");
    tools.add(save);

    row = new Row();
    panel.add(row);
    Label msg = new Label("");
    row.add(msg);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    save.addClickListener((me, cmp) -> {
      msg.setText("");
      errmsg.setText("");
      String _old = old_pass.getText();
      String _new = new_pass.getText();
      String _cfm = cfm_pass.getText();
      if (_new.length() < 8) {
        errmsg.setText("Password too short (min 8)");
        return;
      }
      if (!_new.equals(_cfm)) {
        errmsg.setText("Passwords do not match");
        return;
      }
      if (!_old.equals(Config.passwd.password)) {
        errmsg.setText("Wrong current password");
        return;
      }
      old_pass.setText("");
      new_pass.setText("");
      cfm_pass.setText("");
      Config.passwd.password = _new;
      Config.passwd.save();
      msg.setText("Password saved");
    });

    return panel;
  }

  private static String[] services = {
    "glusterd",
    "libvirtd",
    "iscsid",
    "pcsd",
    "ocfs2",
  };

  private Panel hostServicesPanel(UI ui) {
    Panel panel = new Panel();
    Row row;

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button start = new Button("Start");
    tools.add(start);
    Button stop = new Button("Stop");
    tools.add(stop);
    Button enable = new Button("Enable");
    tools.add(enable);
    Button disable = new Button("Disable");
    tools.add(disable);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    Table table = new Table(new int[] {100, 75, 50}, col_height, 3, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    table.addRow(new String[] {"Name", "Enabled", "Active"});
    for(String service : services) {
      table.addRow(ServiceControl.getStates(service));
    }

    refresh.addClickListener((me, cmp) -> {
      ui.setRightPanel(hostPanel(ui, HOST_SERVICES));
    });

    start.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Task task = new Task("Start Service") {
        public void doTask() {
          try {
            ServiceControl.start(services[idx]);
            setStatus("Completed");
          } catch (Exception e) {
            setStatus("Error occured, check logs.");
            JFLog.log(e);
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });
    stop.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Task task = new Task("Stop Service") {
        public void doTask() {
          try {
            ServiceControl.stop(services[idx]);
            setStatus("Completed");
          } catch (Exception e) {
            setStatus("Error occured, check logs.");
            JFLog.log(e);
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });
    enable.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Task task = new Task("Enable Service") {
        public void doTask() {
          try {
            ServiceControl.enable(services[idx]);
            setStatus("Completed");
          } catch (Exception e) {
            setStatus("Error occured, check logs.");
            JFLog.log(e);
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });
    disable.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Task task = new Task("Disable Service") {
        public void doTask() {
          try {
            ServiceControl.disable(services[idx]);
            setStatus("Completed");
          } catch (Exception e) {
            setStatus("Error occured, check logs.");
            JFLog.log(e);
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });

    return panel;
  }

  private Panel vmsPanel(UI ui) {
    Panel panel = new Panel();
    Row row;

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
    Button monitor = new Button("Monitor");
    tools.add(monitor);
    Button start = new Button("Start");
    tools.add(start);
    Button stop = new Button("Stop");
    tools.add(stop);
    Button restart = new Button("Restart");
    tools.add(restart);
    Button poweroff = new Button("PowerOff");
    tools.add(poweroff);
    Button clone = new Button("Clone");
    tools.add(clone);
    Button migrate = new Button("Migrate");
    tools.add(migrate);
    Button unreg = new Button("Unregister");
    tools.add(unreg);
    Button help = new Button("Help");
    tools.add(help);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    Table table = new Table(new int[] {150, 50, 100}, col_height, 3, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    table.addRow(new String[] {"Name", "State", "Storage"});
    VirtualMachine[] vms = VirtualMachine.list();
    for(VirtualMachine vm : vms) {
      table.addRow(vm.getStates());
    }

    create.addClickListener((me, cmp) -> {
      Hardware hw = new Hardware();
      VirtualMachine vm = new VirtualMachine(hw);
      ui.setRightPanel(vmAddPanel(vm, hw, ui));
    });

    edit.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      Hardware hardware = vm.loadHardware();
      if (hardware == null) {
        errmsg.setText("Error:Failed to load config for vm:" + vm.name);
        return;
      }
      ui.setRightPanel(vmEditPanel(vm, hardware, false, ui));
    });

    refresh.addClickListener((me, cmp) -> {
      ui.setRightPanel(vmsPanel(ui));
    });

    console.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() != VirtualMachine.STATE_ON) {
        errmsg.setText("VM is not active");
        return;
      }
      ConsoleSession sess = new ConsoleSession();
      sess.id = JF.generateUUID();
      sess.vm = vm;
      sess.ts = System.currentTimeMillis();
      sess.put();
      cmp.getClient().openURL("/console?id=" + sess.id);
    });

    monitor.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() != VirtualMachine.STATE_ON) {
        errmsg.setText("VM is not active");
        return;
      }
      ui.setRightPanel(vmMonitorPanel(vm, ui));
    });

    start.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() != VirtualMachine.STATE_OFF) {
        errmsg.setText("Error:VM is already powered on.");
        return;
      }
      ui.confirm_button.setText("Start");
      ui.confirm_message.setText("Start VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Start VM : " + vm.name) {
          public void doTask() {
            if (vm.start()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });

    //stop
    stop.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() == VirtualMachine.STATE_OFF) {
        errmsg.setText("Error:VM is already stopped.");
        return;
      }
      ui.confirm_button.setText("Stop");
      ui.confirm_message.setText("Stop VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Stop VM : " + vm.name) {
          public void doTask() {
            if (vm.stop()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });

    //restart
    restart.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() == VirtualMachine.STATE_OFF) {
        errmsg.setText("Error:VM is not live.");
        return;
      }
      ui.confirm_button.setText("Restart");
      ui.confirm_message.setText("Restart VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Restart VM : " + vm.name) {
          public void doTask() {
            if (vm.restart()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });

    //poweroff
    poweroff.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() == VirtualMachine.STATE_OFF) {
        errmsg.setText("Error:VM is already powered down.");
        return;
      }
      ui.confirm_button.setText("Power Off");
      ui.confirm_message.setText("Power Off VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Power Off VM : " + vm.name) {
          public void doTask() {
            if (vm.poweroff()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });

    clone.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() != VirtualMachine.STATE_OFF) {
        errmsg.setText("Can not data clone live VM");
        return;
      }
      ui.setRightPanel(vmCloneDataPanel(vm, ui));
    });

    migrate.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      ui.setRightPanel(vmMigratePanel(vm, ui));
    });

    unreg.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      VirtualMachine vm = vms[idx];
      if (vm.getState() != VirtualMachine.STATE_OFF) {
        errmsg.setText("Error:Can not unregister a live VM.");
        return;
      }
      ui.confirm_button.setText("Unregister");
      ui.confirm_message.setText("Unregister VM : " + vm.name);
      ui.confirm_action = () -> {
        Task task = new Task("Unregister VM : " + vm.name) {
          public void doTask() {
            if (vm.getState() != VirtualMachine.STATE_OFF) {
              setResult("Error:Can not unregister a live VM.");
              return;
            }
            if (vm.unregister()) {
              Config.current.removeVirtualMachine(vm);
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });

    help.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfkvm/docs/help_vm.html");
    });

    return panel;
  }

  private Panel vmAddPanel(VirtualMachine vm, Hardware hardware, UI ui) {
    Panel panel = new Panel();
    Row row;

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
    ArrayList<Storage> pools = Config.current.pools;
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

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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
      if (!pool.mounted()) {
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
    hardware.validate();
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    panel.add(grid);
    Row row;
    //name [   ]
    TextField _name = new TextField(hardware.name);
    _name.setReadonly(true);
    grid.addRow(new Component[] {new Label("Name"), _name});
    //pool [   ]
    TextField _pool = new TextField(hardware.pool);
    _pool.setReadonly(true);
    grid.addRow(new Component[] {new Label("Storage Pool"), _pool});
    //operating system type
    ComboBox os_type = new ComboBox();
    os_type.add("Linux", "Linux");
    os_type.add("Windows", "Windows");
    os_type.setSelectedIndex(hardware.os);
    grid.addRow(new Component[] {new Label("OS Type"), os_type});
    //memory [   ] [MB/GB]
    row = new Row();
    TextField memory = new TextField(Integer.toString(hardware.memory.size));
    row.add(memory);
    ComboBox memory_units = new ComboBox();
    memory_units.add("MB", "MB");
    memory_units.add("GB", "GB");
    memory_units.setSelectedIndex(hardware.memory.unit - 2);
    row.add(memory_units);
    grid.addRow(new Component[] {new Label("Memory"), row});
    //cpus [   ]
    TextField cores = new TextField(Integer.toString(hardware.cores));
    grid.addRow(new Component[] {new Label("CPU Cores"), cores});
    //firmware [BIOS/UEFI]
    ComboBox firmware = new ComboBox();
    firmware.add("BIOS", "BIOS");
    firmware.add("UEFI", "UEFI");
    if (hardware.bios_efi) {
      firmware.setSelectedIndex(1);
    }
    grid.addRow(new Component[] {new Label("Firmware"), firmware});
    //machine type
    ComboBox machine = new ComboBox();
    machine.add("pc", "pc");
    machine.add("q35", "q35");
    switch (hardware.machine) {
      default:
      case "pc":
        machine.setSelectedIndex(0);
        break;
      case "q35":
        machine.setSelectedIndex(1);
        break;
    }
    grid.addRow(new Component[] {new Label("Machine"), machine});
    //TPM
    ComboBox tpm = new ComboBox();
    tpm.add("0", "None");
    tpm.add("1", "1.2");
    tpm.add("2", "2.0");
    switch (hardware.tpm) {
      default:
      case Hardware.TPM_NONE:
        tpm.setSelectedIndex(0);
        break;
      case Hardware.TPM_1_2:
        tpm.setSelectedIndex(1);
        break;
      case Hardware.TPM_2_0:
        tpm.setSelectedIndex(2);
        break;
    }
    grid.addRow(new Component[] {new Label("TPM"), tpm});
    //video card
    ComboBox video = new ComboBox();
    video.add("vmvga", "vmvga");
    video.add("vga", "vga");
    video.add("cirrus", "cirrus");
    video.add("virtio", "virtio");
    video.add("bochs", "bochs");
    switch (hardware.video) {
      default:
      case "vmvga":
        video.setSelectedIndex(0);
        break;
      case "vga":
        video.setSelectedIndex(1);
        break;
      case "cirrus":
        video.setSelectedIndex(2);
        break;
      case "virtio":
        video.setSelectedIndex(3);
        break;
      case "bochs":
        video.setSelectedIndex(4);
        break;
    }
    grid.addRow(new Component[] {new Label("Video"), video});
    //video memory
    row = new Row();
    TextField vram = new TextField("");
    row.add(vram);
    row.add(new Label("(kb)"));
    vram.setText(Integer.toString(hardware.vram));
    grid.addRow(new Component[] {new Label("Video Memory"), row});
    //video : 3d accel
    row = new Row();
    panel.add(row);
    CheckBox video_3d_accel = new CheckBox("Video 3D Accel");
    row.add(video_3d_accel);
    if (hardware.video_3d_accel) {
      video_3d_accel.setSelected(true);
    }
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
      disk_list.add(disk.toString());
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
    Button b_net_addr = new Button("Address");
    net_ops.add(b_net_addr);
    Button b_net_delete = new Button("Delete");
    net_ops.add(b_net_delete);
    ListBox net_list = new ListBox();
    networks.add(net_list);
    for(Network nic : hardware.networks) {
      net_list.add("net:" + nic.network);
    }
    //devices
    InnerPanel devices = new InnerPanel("Host Devices");
    panel.add(devices);
    ToolBar dev_ops = new ToolBar();
    devices.add(dev_ops);
    Button b_dev_add_usb = new Button("Add USB");
    dev_ops.add(b_dev_add_usb);
    Button b_dev_add_pci = new Button("Add PCI");
    dev_ops.add(b_dev_add_pci);
    Button b_dev_addr = new Button("Address");
    dev_ops.add(b_dev_addr);
    Button b_dev_delete = new Button("Delete");
    dev_ops.add(b_dev_delete);
    ListBox dev_list = new ListBox();
    devices.add(dev_list);
    for(Device dev : hardware.devices) {
      dev_list.add(dev.toString());
    }

    //devices
    InnerPanel ctrls = new InnerPanel("Controllers");
    panel.add(ctrls);
    row = new Row();
    row.add(new Label("NOTE:Controllers are automatically added as needed"));
    ctrls.add(row);
    ToolBar ctrl_ops = new ToolBar();
    ctrls.add(ctrl_ops);
    Button b_ctrl_add_scsi = new Button("Add SCSI");
    ctrl_ops.add(b_ctrl_add_scsi);
    Button b_ctrl_add_usb = new Button("Add USB");
    ctrl_ops.add(b_ctrl_add_usb);
    Button b_ctrl_add_ide = new Button("Add IDE");
    ctrl_ops.add(b_ctrl_add_ide);
    Button b_ctrl_edit = new Button("Edit");
    ctrl_ops.add(b_ctrl_edit);
    Button b_ctrl_addr = new Button("Address");
    ctrl_ops.add(b_ctrl_addr);
    Button b_ctrl_delete = new Button("Delete");
    ctrl_ops.add(b_ctrl_delete);
    ListBox ctrl_list = new ListBox();
    ctrls.add(ctrl_list);
    for(Controller ctrl : hardware.controllers) {
      ctrl_list.add(ctrl.toString());
    }

    //save / cancel
    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button b_save = new Button("Save");
    tools.add(b_save);
    Button b_cancel = new Button("Cancel");
    tools.add(b_cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    b_disk_create.addClickListener((me, cmp) -> {
      ui.vm_disk = null;
      ui.vm_disk_init.run();
      ui.vm_disk_complete = () -> {
        ui.hardware.addDisk(ui.vm_disk);
        disk_list.add(ui.vm_disk.toString());
      };
      ui.vm_disk_popup.setVisible(true);
    });
    b_disk_add.addClickListener((me, cmp) -> {
      ui.browse_path = ui.hardware.getPath();
      ui.browse_filters = filter_disks;
      ui.browse_init.run();
      ui.browse_complete = () -> {
        String path_file = ui.browse_file;
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
        disk_list.add(disk.toString());
        ui.browse_popup.setVisible(false);
      };
      ui.browse_button.setText("Select");
      ui.browse_popup.setVisible(true);
    });
    b_disk_edit.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = disk_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.vm_disk = ui.hardware.disks.get(idx);
      ui.vm_disk_init.run();
      ui.vm_disk_complete = () -> {
        disk_list.remove(idx);
        disk_list.add(idx, ui.vm_disk.toString());
      };
      ui.vm_disk_popup.setVisible(true);
    });
    b_disk_delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = disk_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
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
        net_list.add("net:" + ui.vm_network.network);
      };
      ui.vm_network_init.run();
      ui.vm_network_popup.setVisible(true);
    });
    b_net_edit.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = net_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.vm_network = hardware.networks.get(idx);
      ui.vm_network_complete = () -> {
        net_list.remove(idx);
        net_list.add(idx, "net:" + ui.vm_network.network);
      };
      ui.vm_network_init.run();
      ui.vm_network_popup.setVisible(true);
    });
    b_net_addr.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = net_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.vm_network = hardware.networks.get(idx);
      ui.device_addr_addr = ui.vm_network;
      ui.device_addr_pci_init.run();
      ui.device_addr_pci_popup.setVisible(true);
    });
    b_net_delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = net_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      net_list.remove(idx);
      hardware.removeNetwork(hardware.networks.get(idx));
    });

    b_dev_add_usb.addClickListener((me, cmp) -> {
      ui.device = null;
      ui.device_usb_init.run();
      ui.device_complete = () -> {
        ui.hardware.addDevice(ui.device);
        dev_list.add(ui.device.toString());
      };
      ui.device_usb_popup.setVisible(true);
    });
    b_dev_add_pci.addClickListener((me, cmp) -> {
      ui.device = null;
      ui.device_pci_init.run();
      ui.device_complete = () -> {
        ui.hardware.addDevice(ui.device);
        dev_list.add(ui.device.toString());
      };
      ui.device_pci_popup.setVisible(true);
    });
    b_dev_addr.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = dev_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Device device = ui.hardware.devices.get(idx);
      ui.device_addr_addr = device.guest_addr;
      switch (device.type) {
        case Device.TYPE_PCI:
          ui.device_addr_pci_init.run();
          ui.device_addr_pci_popup.setVisible(true);
          break;
        case Device.TYPE_USB:
          ui.device_addr_usb_init.run();
          ui.device_addr_usb_popup.setVisible(true);
          break;
      }
    });
    b_dev_delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = dev_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Device device = ui.hardware.devices.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Device : " + device.name);
      ui.confirm_action = () -> {
        ui.hardware.removeDevice(device);
        dev_list.remove(idx);
      };
      ui.confirm_popup.setVisible(true);
    });

    b_ctrl_add_scsi.addClickListener((me, cmp) -> {
      ui.ctrl = null;
      ui.ctrl_models = Controller.get_scsi_models();
      ui.ctrl_type = "scsi";
      ui.ctrl_model_init.run();
      ui.ctrl_model_complete = () -> {
        ui.hardware.addController(ui.ctrl);
        ctrl_list.add(ui.ctrl.toString());
      };
      ui.ctrl_model_popup.setVisible(true);
    });
    b_ctrl_add_usb.addClickListener((me, cmp) -> {
      ui.ctrl = null;
      ui.ctrl_models = Controller.get_usb_models();
      ui.ctrl_type = "usb";
      ui.ctrl_model_init.run();
      ui.ctrl_model_complete = () -> {
        ui.hardware.addController(ui.ctrl);
        ctrl_list.add(ui.ctrl.toString());
      };
      ui.ctrl_model_popup.setVisible(true);
    });
    b_ctrl_add_ide.addClickListener((me, cmp) -> {
      ui.ctrl = null;
      ui.ctrl_models = Controller.get_ide_models();
      ui.ctrl_type = "ide";
      ui.ctrl_model_init.run();
      ui.ctrl_model_complete = () -> {
        ui.hardware.addController(ui.ctrl);
        ctrl_list.add(ui.ctrl.toString());
      };
      ui.ctrl_model_popup.setVisible(true);
    });
    b_ctrl_edit.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = ctrl_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.ctrl = ui.hardware.controllers.get(idx);
      ui.ctrl_type = ui.ctrl.type;
      switch (ui.ctrl.type) {
        case "scsi":
          ui.ctrl_models = Controller.get_scsi_models();
          break;
        case "usb":
          ui.ctrl_models = Controller.get_usb_models();
          break;
        case "ide":
          ui.ctrl_models = Controller.get_ide_models();
          break;
      }
      ui.ctrl_model_init.run();
      ui.ctrl_model_complete = () -> {};
      ui.ctrl_model_popup.setVisible(true);
    });
    b_ctrl_addr.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = ctrl_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Controller ctrl = ui.hardware.controllers.get(idx);
      ui.device_addr_addr = ctrl;
      ui.device_addr_pci_init.run();
      ui.device_addr_pci_popup.setVisible(true);
    });
    b_ctrl_delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = ctrl_list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Controller ctrl = ui.hardware.controllers.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Controller : " + ctrl.toString());
      ui.confirm_action = () -> {
        ui.hardware.removeController(ctrl);
        ctrl_list.remove(idx);
      };
      ui.confirm_popup.setVisible(true);
    });

    b_save.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int _os = os_type.getSelectedIndex();
      hardware.os = _os;
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
      hardware.machine = machine.getSelectedValue();
      hardware.tpm = tpm.getSelectedIndex();
      hardware.video = video.getSelectedValue();
      _size_str = vmm.cleanNumber(vram.getText());
      if (_size_str.length() == 0) {
        cores.setText(_size_str);
        errmsg.setText("Error:invalid vram");
        return;
      }
      int _vram = Integer.valueOf(_size_str);
      if (_vram == 0) {
        errmsg.setText("Error:invalid vram");
        return;
      }
      hardware.vram = _vram;
      hardware.video_3d_accel = video_3d_accel.isSelected();
      hardware.validate();
      if (!VirtualMachine.register(vm, hardware, vmm)) {
        errmsg.setText("Error Occured : View Logs for details");
        return;
      }
      vm.saveHardware(hardware);
      Config.current.addVirtualMachine(vm);
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

  private Panel vmMigratePanel(VirtualMachine vm, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("VM:" + vm.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Select Data or Compute migration"));

    row = new Row();
    panel.add(row);
    CheckBox data = new CheckBox("Data Migration");
    row.add(data);

    row = new Row();
    panel.add(row);
    CheckBox compute = new CheckBox("Compute Migration");
    row.add(compute);

    row = new Row();
    panel.add(row);
    Button next = new Button("Next");
    row.add(next);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    data.addClickListener((me, cmp) -> {
      compute.setSelected(false);
    });
    compute.addClickListener((me, cmp) -> {
      data.setSelected(false);
    });
    next.addClickListener((me, cmp) -> {
      errmsg.setText("");
      Hardware hw = vm.loadHardware();
      if (hw == null) {
        errmsg.setText("Error:Failed to load VM hardware");
        return;
      }
      if (data.isSelected()) {
        ui.setRightPanel(vmMigrateDataPanel(vm, hw, ui));
        return;
      }
      if (compute.isSelected()) {
        //check if vm storage is local
        Storage pool = vmm.getPoolByName(vm.pool);
        if (pool == null) {
          errmsg.setText("Error:Storage not found for VM");
          return;
        }
        if (pool.type == Storage.TYPE_LOCAL_PART || pool.type == Storage.TYPE_LOCAL_DISK) {
          errmsg.setText("Error:Can not compute migrate VM with local storage");
          return;
        }
        //check if vm disks are local
        for(Disk disk : hw.disks) {
          Storage disk_store = vmm.getPoolByName(disk.pool);
          if (disk_store == null) {
            errmsg.setText("Error:Storage not found for disk:" + disk.name);
            return;
          }
          if (disk_store.type == Storage.TYPE_LOCAL_PART || disk_store.type == Storage.TYPE_LOCAL_DISK) {
            errmsg.setText("Error:Can not compute migrate VM with disk using local storage:" + disk.name);
            return;
          }
        }
        ui.setRightPanel(vmMigrateComputePanel(vm, hw, ui));
        return;
      }
      errmsg.setText("You must make a selection");
    });

    return panel;
  }

  private Panel vmMigrateDataPanel(VirtualMachine vm, Hardware hw, UI ui) {
    Panel panel = new Panel();
    Row row;
    ArrayList<Storage> pools = Config.current.pools;

    row = new Row();
    panel.add(row);
    row.add(new Label("Select a storage pool"));

    row = new Row();
    panel.add(row);
    ListBox list = new ListBox();
    row.add(list);
    for(Storage pool : pools) {
      list.add(pool.name);
    }

    row = new Row();
    panel.add(row);
    Button next = new Button("Next");
    row.add(next);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    next.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage dest = pools.get(idx);
      if (dest.name.equals(vm.pool)) {
        errmsg.setText("That VM is already in that storage pool");
        return;
      }
      ui.setRightPanel(vmMigrateDataStartPanel(vm, hw, dest, ui));
    });

    return panel;
  }

  private Panel vmMigrateDataStartPanel(VirtualMachine vm, Hardware hw, Storage dest, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("VM:" + vm.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Source:" + vm.pool));

    row = new Row();
    panel.add(row);
    row.add(new Label("Dest:" + dest.name));

    row = new Row();
    panel.add(row);
    Button start = new Button("Start");
    row.add(start);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    start.addClickListener((me, cmp) -> {
      if (vm.getState() != VirtualMachine.STATE_OFF) {
        errmsg.setText("Can not data migrate live VM");
        return;
      }
      Size src = vmm.getPoolByName(vm.pool).getFolderSize(vm.name);
      JFLog.log("Data Migration:Src size=" + src);
      Size dest_free = dest.getFreeSize();
      JFLog.log("Data Migration:Dest size=" + dest_free);
      if (src.greaterThan(dest_free)) {
        errmsg.setText("Insufficent space available in dest storage pool");
        return;
      }
      Task task = new Task("Data Migrate VM : " + vm.name) {
        public void doTask() {
          Hardware hw = vm.loadHardware();
          if (hw == null) {
            setResult("Error occured, see logs.");
            return;
          }
          if (vmm.migrateData(vm, hw, dest, this)) {
            setResult("Completed");
          } else {
            setResult("Error occured, see logs.");
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
      ui.setRightPanel(vmsPanel(ui));
    });

    return panel;
  }

  private Panel vmMigrateComputePanel(VirtualMachine vm, Hardware hw, UI ui) {
    Panel panel = new Panel();
    Row row;
    Host[] hosts = Config.current.getHosts();

    row = new Row();
    panel.add(row);
    row.add(new Label("Select a remote host"));

    row = new Row();
    panel.add(row);
    ListBox list = new ListBox();
    row.add(list);
    for(Host host : hosts) {
      list.add(host.host);
    }

    row = new Row();
    panel.add(row);
    Button next = new Button("Next");
    row.add(next);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    next.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Host dest = hosts[idx];
      if (!dest.online) {
        errmsg.setText("Remote Host is offline");
        return;
      }
      ui.setRightPanel(vmMigrateComputeStartPanel(vm, hw, dest, ui));
    });

    return panel;
  }

  private Panel vmMigrateComputeStartPanel(VirtualMachine vm, Hardware hw, Host remote, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("VM:" + vm.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Dest:" + remote.host));

    row = new Row();
    panel.add(row);
    Button start = new Button("Start");
    row.add(start);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    //TODO : confirm move is possible (check cpu,memory,network,device requirements)

    start.addClickListener((me, cmp) -> {
      errmsg.setText("");
      //perform checks before starting
      //check if remote host is using vnc port
      int vnc=vm.getVNC();
      if (vmm.vnc_port_inuse_remote(remote, vnc)) {
        errmsg.setText("Error:VNC Port in use on remote host");
        return;
      }
      //check networks are compatible
      for(Network nw : hw.networks) {
        int remote_vlan = remote.getNetworkVLAN(nw.network);
        if (remote_vlan == -1) {
          errmsg.setText("Error:Network not found on remote host:" + nw.network);
          return;
        }
        NetworkVLAN local_nw = Config.current.getNetworkVLAN(nw.network);
        if (local_nw == null) {
          errmsg.setText("Error:Network not found on local host:" + nw.network);
          return;
        }
        int local_vlan = local_nw.vlan;
        if (local_vlan != remote_vlan) {
          errmsg.setText("Error:Network VLAN does not match:" + nw.network);
          return;
        }
      }
      if (hw.devices.size() > 0) {
        errmsg.setText("Error:Can not migrate VM with host devices");
        return;
      }
      Task task = new Task("Compute Migrate VM : " + vm.name) {
        public void doTask() {
          if (vmm.migrateCompute(vm, remote.host)) {
            setResult("Completed");
            //notify other host of transfer
            remote.notify("migratevm", vm.name);
          } else {
            setResult("Error occured, see logs.");
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
      ui.setRightPanel(vmsPanel(ui));
    });

    return panel;
  }

  private Panel vmCloneDataPanel(VirtualMachine vm, UI ui) {
    Panel panel = new Panel();
    Row row;
    ArrayList<Storage> pools = Config.current.pools;

    Calendar cal = Calendar.getInstance();
    String name_yyyy_mm = String.format("%s-%04d-%02d", vm.name, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);

    row = new Row();
    panel.add(row);
    row.add(new Label("New Name"));
    TextField new_name = new TextField(name_yyyy_mm);
    row.add(new_name);

    row = new Row();
    panel.add(row);
    row.add(new Label("Select a storage pool"));

    row = new Row();
    panel.add(row);
    ListBox list = new ListBox();
    row.add(list);
    for(Storage pool : pools) {
      list.add(pool.name);
    }

    row = new Row();
    panel.add(row);
    Button next = new Button("Next");
    row.add(next);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    next.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = list.getSelectedIndex();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage dest = pools.get(idx);
      String _new_name = vmm.cleanName(new_name.getText());
      if (_new_name.length() == 0) {
        new_name.setText(_new_name);
        errmsg.setText("Invalid new name");
        return;
      }
      if (vm.name.equals(_new_name)) {
        errmsg.setText("New name can not be the same");
        return;
      }
      ui.setRightPanel(vmCloneDataStartPanel(vm, dest, _new_name, ui));
    });

    return panel;
  }

  private Panel vmCloneDataStartPanel(VirtualMachine vm, Storage dest, String new_name, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("VM:" + vm.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Source:" + vm.pool));

    row = new Row();
    panel.add(row);
    row.add(new Label("Dest:" + dest.name));

    row = new Row();
    panel.add(row);
    Button start = new Button("Start");
    row.add(start);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    //TODO : confirm clone is possible (check storage requirements)

    start.addClickListener((me, cmp) -> {
      if (vm.getState() != VirtualMachine.STATE_OFF) {
        errmsg.setText("Can not data clone live VM");
        return;
      }
      Task task = new Task("Data Clone VM : " + vm.name) {
        public void doTask() {
          if (vmm.cloneData(vm, dest, new_name, this)) {
            setResult("Completed");
          } else {
            setResult("Error occured, see logs.");
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
      ui.setRightPanel(vmsPanel(ui));
    });

    return panel;
  }

  private Panel vmMonitorPanel(VirtualMachine vm, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("VM:" + vm.name));

    row = new Row();
    panel.add(row);
    ToolBar tools = new ToolBar();
    row.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);

    ui.now = Calendar.getInstance();
    int _year = ui.now.get(Calendar.YEAR);
    int _month = ui.now.get(Calendar.MONTH) + 1;
    int _day = ui.now.get(Calendar.DAY_OF_MONTH);
    int _hour = ui.now.get(Calendar.HOUR_OF_DAY);
    String _file = String.format("%04d-%02d-%02d-%02d", _year, _month, _day, _hour);
    String uuid = vm.getUUID();

    row = new Row();
    panel.add(row);
    row.add(new Label("Year:"));
    TextField year = new TextField(Integer.toString(_year));
    year.setReadonly(true);
    row.add(year);

    row = new Row();
    panel.add(row);
    row.add(new Label("Month:"));
    TextField month = new TextField(Integer.toString(_month));
    month.setReadonly(true);
    row.add(month);

    row = new Row();
    panel.add(row);
    row.add(new Label("Day:"));
    TextField day = new TextField(Integer.toString(_day));
    day.setReadonly(true);
    row.add(day);
    Button day_prev = new Button("<");
    row.add(day_prev);
    Button day_next = new Button(">");
    row.add(day_next);

    row = new Row();
    panel.add(row);
    row.add(new Label("Hour:"));
    TextField hour = new TextField(Integer.toString(_hour));
    hour.setReadonly(true);
    row.add(hour);
    Button hour_prev = new Button("<");
    row.add(hour_prev);
    Button hour_next = new Button(">");
    row.add(hour_next);

    row = new Row();
    panel.add(row);
    Label lbl_mem = new Label("Memory");
    lbl_mem.setWidth(img_width);
    row.add(lbl_mem);
    Label lbl_cpu = new Label("CPU");
    lbl_cpu.setWidth(img_width);
    row.add(lbl_cpu);

    row = new Row();
    panel.add(row);
    Image img_mem = new Image("stats?uuid=" + uuid + "&type=mem&file=" + _file + ".png");
    img_mem.setWidth(img_width);
    img_mem.setHeight(img_height);
    row.add(img_mem);
    Image img_cpu = new Image("stats?uuid=" + uuid + "&type=cpu&file=" + _file + ".png");
    img_cpu.setWidth(img_width);
    img_cpu.setHeight(img_height);
    row.add(img_cpu);

    row = new Row();
    panel.add(row);
    Label lbl_disk = new Label("Disk");
    lbl_disk.setWidth(img_width);
    row.add(lbl_disk);
    Label lbl_net = new Label("Network");
    lbl_net.setWidth(img_width);
    row.add(lbl_net);

    row = new Row();
    panel.add(row);
    Image img_dsk = new Image("stats?uuid=" + uuid + "&type=dsk&file=" + _file + ".png");
    img_dsk.setWidth(img_width);
    img_dsk.setHeight(img_height);
    row.add(img_dsk);
    Image img_net = new Image("stats?uuid=" + uuid + "&type=net&file=" + _file + ".png");
    img_net.setWidth(img_width);
    img_net.setHeight(img_height);
    row.add(img_net);

    Runnable reload = () -> {
      int __year = Integer.valueOf(year.getText());
      int __month = Integer.valueOf(month.getText());
      int __day = Integer.valueOf(day.getText());
      int __hour = Integer.valueOf(hour.getText());
      String __file = String.format("%04d-%02d-%02d-%02d", __year, __month, __day, __hour);
      img_mem.setImage("stats?uuid=" + uuid + "&type=mem&file=" + __file + ".png");
      img_cpu.setImage("stats?uuid=" + uuid + "&type=cpu&file=" + __file + ".png");
      img_dsk.setImage("stats?uuid=" + uuid + "&type=dsk&file=" + __file + ".png");
      img_net.setImage("stats?uuid=" + uuid + "&type=net&file=" + __file + ".png");
    };

    refresh.addClickListener((me, cmp) -> {
      reload.run();
    });

    day_prev.addClickListener((me, cmp) -> {
      long ts = ui.now.getTimeInMillis();
      ts -= day_ms;
      ui.now.setTimeInMillis(ts);
      int __year = ui.now.get(Calendar.YEAR);
      int __month = ui.now.get(Calendar.MONTH) + 1;
      int __day = ui.now.get(Calendar.DAY_OF_MONTH);
      int __hour = ui.now.get(Calendar.HOUR_OF_DAY);
      year.setText(Integer.toString(__year));
      month.setText(Integer.toString(__month));
      day.setText(Integer.toString(__day));
      hour.setText(Integer.toString(__hour));
      reload.run();
    });

    day_next.addClickListener((me, cmp) -> {
      long ts = ui.now.getTimeInMillis();
      ts += day_ms;
      ui.now.setTimeInMillis(ts);
      int __year = ui.now.get(Calendar.YEAR);
      int __month = ui.now.get(Calendar.MONTH) + 1;
      int __day = ui.now.get(Calendar.DAY_OF_MONTH);
      int __hour = ui.now.get(Calendar.HOUR_OF_DAY);
      year.setText(Integer.toString(__year));
      month.setText(Integer.toString(__month));
      day.setText(Integer.toString(__day));
      hour.setText(Integer.toString(__hour));
      reload.run();
    });

    hour_prev.addClickListener((me, cmp) -> {
      long ts = ui.now.getTimeInMillis();
      ts -= hour_ms;
      ui.now.setTimeInMillis(ts);
      int __year = ui.now.get(Calendar.YEAR);
      int __month = ui.now.get(Calendar.MONTH) + 1;
      int __day = ui.now.get(Calendar.DAY_OF_MONTH);
      int __hour = ui.now.get(Calendar.HOUR_OF_DAY);
      year.setText(Integer.toString(__year));
      month.setText(Integer.toString(__month));
      day.setText(Integer.toString(__day));
      hour.setText(Integer.toString(__hour));
      reload.run();
    });

    hour_next.addClickListener((me, cmp) -> {
      long ts = ui.now.getTimeInMillis();
      ts += hour_ms;
      ui.now.setTimeInMillis(ts);
      int __year = ui.now.get(Calendar.YEAR);
      int __month = ui.now.get(Calendar.MONTH) + 1;
      int __day = ui.now.get(Calendar.DAY_OF_MONTH);
      int __hour = ui.now.get(Calendar.HOUR_OF_DAY);
      year.setText(Integer.toString(__year));
      month.setText(Integer.toString(__month));
      day.setText(Integer.toString(__day));
      hour.setText(Integer.toString(__hour));
      reload.run();
    });

    return panel;
  }

  private Panel storagePanel(UI ui) {
    Panel panel = new Panel();
    Row row;

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
    Button mount = new Button("Mount");
    tools.add(mount);
    Button unmount = new Button("Unmount");
    tools.add(unmount);
    Button format = new Button("Format");
    tools.add(format);
    Button gluster_volume_create = new Button("Gluster Volume Create");
    tools.add(gluster_volume_create);
    Button gluster_volume_start = new Button("Gluster Volume Start");
    tools.add(gluster_volume_start);
    Button delete = new Button("Delete");
    tools.add(delete);
    Button help = new Button("Help");
    tools.add(help);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    panel.add(row);
    Table table = new Table(new int[] {100, 75, 50, 75, 50, 50}, col_height, 6, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    table.addRow(new String[] {"Name", "Type", "State", "Mounted", "Size", "Free"});
    ArrayList<Storage> pools = Config.current.pools;
    for(Storage pool : pools) {
      //TODO : this can block if pool is remote and offline
      table.addRow(pool.getStates());
    }

    add.addClickListener((me, cmp) -> {
      ui.setRightPanel(addStoragePanel(ui));
    });
    edit.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage pool = pools.get(idx);
      ui.setRightPanel(editStoragePanel(pool, ui));
    });
    refresh.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });
    browse.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage pool = pools.get(idx);
      ui.browse_path = pool.getPath();
      ui.browse_filters = filter_all;
      ui.browse_init.run();
      ui.browse_complete = () -> {
        if (ui.browse_file.endsWith(".jfvm")) {
          ui.browse_popup.setVisible(false);
          ui.confirm_button.setText("Register");
          ui.confirm_message.setText("Register VM : " + ui.browse_file);
          ui.confirm_action = () -> {
            Hardware hardware = Hardware.load(ui.browse_file);
            if (hardware == null) {
              ui.message_message.setText("Failed to load VM, see logs.");
              ui.message_popup.setVisible(true);
              return;
            }
            if (!hardware.pool.equals(pool.name)) {
              //user has manaully moved this vm files
              hardware.pool = pool.name;
              //check disks too
              for(Disk disk : hardware.disks) {
                if (!disk.exists() && disk.exists(pool.name)) {
                  //disk has moved
                  disk.pool = pool.name;
                }
              }
            }
            VirtualMachine vm = new VirtualMachine(hardware);
            if (!VirtualMachine.register(vm, hardware, vmm)) {
              ui.message_message.setText("Failed to register VM, see logs.");
              ui.message_popup.setVisible(true);
              return;
            }
            Config.current.addVirtualMachine(vm);
          };
          ui.confirm_popup.setVisible(true);
        } else if (ui.browse_file.endsWith(".vmx")) {
          ui.browse_popup.setVisible(false);
          ui.confirm_button.setText("Convert");
          ui.confirm_message.setText("Convert VM : " + ui.browse_file);
          ui.confirm_action = () -> {
            Hardware hardware = vmm.convertVMX(ui.browse_file, getPool(ui.browse_file), getFolder(ui.browse_file), getFile(ui.browse_file));
            if (hardware != null) {
              VirtualMachine vm = new VirtualMachine(hardware);
              ui.setRightPanel(vmEditPanel(vm, hardware, false, ui));
            } else {
              ui.message_message.setText("Failed to convert VM, see logs.");
              ui.message_popup.setVisible(true);
            }
          };
          ui.confirm_popup.setVisible(true);
        } else {
          ui.browse_popup.setVisible(false);
        }
      };
      ui.browse_button.setText("Register");
      ui.browse_popup.setVisible(true);
    });
    start.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage pool = pools.get(idx);
      Task task = new Task("Start Pool : " + pool.name) {
        public void doTask() {
          if (pool.type == Storage.TYPE_ISCSI) {
            if (pool.user != null && pool.user.length() > 0) {
              Password password = Password.load(Password.TYPE_STORAGE, pool.name);
              if (password == null) {
                setResult("Error occured, see logs.");
                return;
              }
              Secret.create(pool.name, password.password);
            }
          }
          if (pool.start()) {
/* // do not auto mount - may need to format first
            if (pool.type == Storage.TYPE_ISCSI) {
              if (!pool.mount()) {
                setResult("Error occured, see logs.");
                return;
              }
            }
*/
            setResult("Completed");
          } else {
            setResult("Error occured, see logs.");
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });
    stop.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage pool = pools.get(idx);
      ui.confirm_button.setText("Stop");
      ui.confirm_message.setText("Stop storage pool:" + pool.name);
      ui.confirm_action = () -> {
        Task task = new Task("Stop Pool : " + pool.name) {
          public void doTask() {
            if (pool.isMountedManually()) {
              if (pool.mounted()) {
                if (!pool.unmount()) {
                  setResult("Error occured, see logs.");
                  return;
                }
              }
            }
            if (pool.stop()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });
    mount.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage pool = pools.get(idx);
      if (!pool.isMountedManually()) {
        errmsg.setText("Can only mount iSCSI, Gluster, Ceph storage pools, use start for other types");
        return;
      }
      if (pool.getState() != Storage.STATE_ON) {
        errmsg.setText("Error:Pool not running");
        return;
      }
      Task task = new Task("Mount Pool : " + pool.name) {
        public void doTask() {
          if (pool.mount()) {
            setResult("Completed");
          } else {
            setResult("Error occured, see logs.");
          }
        }
      };
      Tasks.tasks.addTask(ui.tasks, task);
    });
    unmount.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage pool = pools.get(idx);
      if (!pool.isMountedManually()) {
        errmsg.setText("Can only unmount iSCSI, Gluster, Ceph storage pools, use stop for other types");
        return;
      }
      if (pool.getState() != Storage.STATE_ON) {
        errmsg.setText("Error:Pool not running");
        return;
      }
      ui.confirm_button.setText("Unmount");
      ui.confirm_message.setText("Unmount storage pool:" + pool.name);
      ui.confirm_action = () -> {
        Task task = new Task("Unmount Pool : " + pool.name) {
          public void doTask() {
            if (pool.unmount()) {
              setResult("Completed");
            } else {
              setResult("Error occured, see logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
      };
      ui.confirm_popup.setVisible(true);
    });
    format.addClickListener((me, cmp) -> {
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Select a storage pool");
        return;
      }
      Storage pool = pools.get(idx);
      if (pool.type == Storage.TYPE_ISCSI) {
        if (!Storage.format_supported(Storage.FORMAT_GFS2) && !Storage.format_supported(Storage.FORMAT_OCFS2)) {
          ui.message_message.setText("Can not format iSCSI storage because no Cluster FS support is loaded (GFS2 or OCFS2)");
          ui.message_popup.setVisible(true);
          return;
        }
        if (pool.getState() != Storage.STATE_ON) {
          ui.message_message.setText("iSCSI must be on to format");
          ui.message_popup.setVisible(true);
          return;
        }
        if (pool.mounted()) {
          ui.message_message.setText("iSCSI must not be mounted to format");
          ui.message_popup.setVisible(true);
          return;
        }
      }
      if (pool.type == Storage.TYPE_NFS) {
        ui.message_message.setText("Can not format NFS storage");
        ui.message_popup.setVisible(true);
        return;
      }
      if (pool.type == Storage.TYPE_LOCAL_PART) {
        if (pool.getState() != Storage.STATE_OFF) {
          ui.message_message.setText("Local Partition must be off to format");
          ui.message_popup.setVisible(true);
          return;
        }
      }
      if (pool.type == Storage.TYPE_GLUSTER) {
        if (!Storage.format_supported(Storage.FORMAT_XFS)) {
          ui.message_message.setText("Can not format Gluster storage because xfs is not loaded");
          ui.message_popup.setVisible(true);
          return;
        }
        if (pool.getState() != Storage.STATE_OFF) {
          ui.message_message.setText("Gluster must be off to format");
          ui.message_popup.setVisible(true);
          return;
        }
        if (pool.mounted()) {
          ui.message_message.setText("Gluster must not be mounted to format");
          ui.message_popup.setVisible(true);
          return;
        }
      }
      if (pool.type == Storage.TYPE_CEPHFS) {
        ui.message_message.setText("Can not format CephFS storage");
        ui.message_popup.setVisible(true);
        return;
      }
      ui.setRightPanel(storageFormatPanel(pool, ui));
    });
    gluster_volume_create.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      if (!Gluster.ready()) {
        errmsg.setText("Not all hosts are probed with Gluster");
        return;
      }
      Storage pool = Config.current.pools.get(idx);
      ui.confirm_button.setText("Create Gluster Volume");
      ui.confirm_message.setText("Create Gluster Volume");
      ui.confirm_action = () -> {
        Task task = new Task("Create Gluster Volume:" + pool.name) {
          public void doTask() {
            try {
              if (Gluster.volume_create(Config.current.getHostNames(), pool.getName(), pool.getGlusterVolume())) {
                setStatus("Completed");
              } else {
                setStatus("Error occured, check logs.");
              }
            } catch (Exception e) {
              JFLog.log(e);
              setStatus("Error occured, check logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
        ui.setRightPanel(storagePanel(ui));
      };
      ui.confirm_popup.setVisible(true);
    });
    gluster_volume_start.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      if (!Gluster.ready()) {
        errmsg.setText("Not all hosts are probed with Gluster");
        return;
      }
      Storage pool = Config.current.pools.get(idx);
      ui.confirm_button.setText("Start Gluster Volume");
      ui.confirm_message.setText("Start Gluster Volume");
      ui.confirm_action = () -> {
        Task task = new Task("Start Gluster Volume:" + pool.name) {
          public void doTask() {
            try {
              if (Gluster.volume_start(pool.getName())) {
                setStatus("Completed");
              } else {
                setStatus("Error occured, check logs.");
              }
            } catch (Exception e) {
              JFLog.log(e);
              setStatus("Error occured, check logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
        ui.setRightPanel(storagePanel(ui));
      };
      ui.confirm_popup.setVisible(true);
    });
    delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      Storage pool = Config.current.pools.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete storage pool");
      ui.confirm_action = () -> {
        if (!pool.unregister()) {
          errmsg.setText("Failed to delete pool");
          return;
        }
        Config.current.removeStorage(pool);
        ui.setRightPanel(storagePanel(ui));
      };
      ui.confirm_popup.setVisible(true);
    });
    help.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfkvm/docs/help_storage.html");
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
    type.add("local_part", "Local Partition");
//    type.add("local_disk", "Local Disk");  //TODO
    type.add("gluster", "Gluster");
    if (Ceph.exists()) {
      type.add("cephfs", "CephFS");
    }
    row.add(type);

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button next = new Button("Next");
    tools.add(next);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    next.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _name = vmm.cleanName(name.getText());
      if (_name.length() == 0) {
        name.setText(_name);
        errmsg.setText("Error:invalid name");
        return;
      }
      ArrayList<Storage> pools = Config.current.pools;
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
        case "gluster":
          ui.setRightPanel(local_StoragePanel(new Storage(Storage.TYPE_GLUSTER, _name, null), true, ui));
          break;
        case "cephfs":
          //check if cephfs already exists
          if (Config.current.hasPool(Storage.TYPE_CEPHFS)) {
            errmsg.setText("CephFS already exists!");
            return;
          }
          ui.setRightPanel(local_StoragePanel(new Storage(Storage.TYPE_CEPHFS, _name, null, "admin@.cephfs=/"), true, ui));
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
      case Storage.TYPE_GLUSTER: return local_StoragePanel(store, false, ui);
      case Storage.TYPE_CEPHFS: return local_StoragePanel(store, false, ui);
    }
    return null;
  }

  private Panel nfs_StoragePanel(Storage pool, boolean create, UI ui) {
    Panel panel = new Panel();
    Row row;

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

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

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
    TextField initiator = new TextField(Storage.getSystemIQN());
    initiator.setReadonly(true);
    row.add(initiator);

    row = new Row();
    panel.add(row);
    row.add(new Label("Chap User:"));
    TextField user = new TextField("");
    row.add(user);
    if (pool.user != null) {
      user.setText(pool.user);
    }

    Password password = Password.load(Password.TYPE_STORAGE, pool.name);
    row = new Row();
    panel.add(row);
    row.add(new Label("Chap Password:"));
    TextField pass = new TextField("");
    pass.setPassword(true);
    row.add(pass);
    if (password != null) {
      pass.setText(password.password);
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    accept.addClickListener((me, cmp) -> {
      errmsg.setText("");
      String _host = host.getText();
      String _target = target.getText();
      String _init = initiator.getText();
      String _user = user.getText();
      String _pass = pass.getText();
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
      pool.user = _user;
      if (_user.length() > 0) {
        Password _password = new Password(Password.TYPE_STORAGE, pool.name, _pass);
        if (!_password.save()) {
          errmsg.setText("Error Occured : View Logs for details");
          return;
        }
        Secret.create(pool.name, _pass);
      } else {
        Password.delete(Password.TYPE_STORAGE, pool.name);
      }
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
    row.add(new Label("Name:" + pool.name));

    row = new Row();
    panel.add(row);
    row.add(new Label("Device:"));
    ComboBox dev = new ComboBox();
    row.add(dev);
    if (pool.path != null) {
      dev.add(pool.path, pool.path);
    }
    if (pool.type != Storage.TYPE_CEPHFS) {
      String[] parts = Storage.listLocalParts();
      for(String part : parts) {
        if (pool.path != null) {
          if (pool.path.equals(part)) continue;
        }
        dev.add(part, part);
      }
    }

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    if (dev.getCount() == 0) {
      errmsg.setText("Error:No available partitions found");
    }

    accept.addClickListener((me, cmp) -> {
      if (dev.getCount() == 0) {
        return;
      }
      errmsg.setText("");
      String _dev = dev.getSelectedValue();
      if (_dev.length() == 0) {
        errmsg.setText("Error:device invalid");
        return;
      }
      if (pool.type != Storage.TYPE_CEPHFS) {
        if (!new File(_dev).exists()) {
          errmsg.setText("Error:device not found");
          return;
        }
      }
      if (false) {
        //convert /dev/sd? to /dev/disk/by-uuid/UUID
        //the UUID can also change after format
        String uuid = Storage.getDiskUUID(_dev);
        if (uuid == null) {
          errmsg.setText("Error:Unable to find disk UUID");
          return;
        }
        _dev = "/dev/disk/by-uuid/" + uuid;
      }
      if (pool.type == Storage.TYPE_GLUSTER) {
        pool.host = Config.current.ip_storage;
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

    ToolBar tools = new ToolBar();
    panel.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button cdup = new Button("&#x21e7;");  //unicode up arrow
    tools.add(cdup);
    Button select = new Button("Select");
    tools.add(select);
    ui.browse_button = select;
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

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    row.add(errmsg);
    ui.browse_errmsg = errmsg;

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
      ui.browse_file = ui.browse_path + "/" + list.getSelectedItem();
      ui.browse_complete.run();
    });

    return panel;
  }

  private Panel storageFormatPanel(Storage pool, UI ui) {
    Panel panel = new Panel();
    Row row;

    row = new Row();
    panel.add(row);
    row.add(new Label("Name:" + pool.name));

    Size size = pool.getDeviceSize();
    if (size == null) {
      size = new Size(0);
    }

    row = new Row();
    panel.add(row);
    row.add(new Label("Size:" + size.toString()));

    row = new Row();
    panel.add(row);
    row.add(new Label("Select Format:"));

    row = new Row();
    panel.add(row);
    CheckBox ext4 = new CheckBox("ext4");
    if (pool.type == Storage.TYPE_LOCAL_PART) {
      row.add(ext4);
    }

    row = new Row();
    panel.add(row);
    CheckBox xfs = new CheckBox("xfs");
    if (pool.type == Storage.TYPE_GLUSTER) {
      row.add(xfs);
    }

    row = new Row();
    panel.add(row);
    CheckBox gfs2 = new CheckBox("gfs2");
    if (pool.type == Storage.TYPE_ISCSI && Storage.format_supported(Storage.FORMAT_GFS2)) {
      row.add(gfs2);
    }

    row = new Row();
    panel.add(row);
    CheckBox ocfs2 = new CheckBox("ocfs2");
    if (pool.type == Storage.TYPE_ISCSI && Storage.format_supported(Storage.FORMAT_OCFS2)) {
      row.add(ocfs2);
    }

    row = new Row();
    panel.add(row);
    Button format = new Button("Format");
    row.add(format);
    Button cancel = new Button("Cancel");
    row.add(cancel);

    row = new Row();
    panel.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    ext4.addClickListener((me, cmp) -> {
      gfs2.setSelected(false);
      ocfs2.setSelected(false);
    });

    gfs2.addClickListener((me, cmp) -> {
      ext4.setSelected(false);
      ocfs2.setSelected(false);
    });

    ocfs2.addClickListener((me, cmp) -> {
      ext4.setSelected(false);
      gfs2.setSelected(false);
    });

    format.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int _fmt = -1;
      if (ext4.isSelected()) _fmt = Storage.FORMAT_EXT4;
      if (gfs2.isSelected()) _fmt = Storage.FORMAT_GFS2;
      if (ocfs2.isSelected()) _fmt = Storage.FORMAT_OCFS2;
      if (xfs.isSelected()) _fmt = Storage.FORMAT_XFS;
      if (_fmt == -1) {
        errmsg.setText("Please select a format");
        return;
      }
      int fmt = _fmt;
      ui.confirm_button.setText("Format");
      ui.confirm_message.setText("Format storage pool:" + pool.name + " with " + Storage.getFormatString(fmt));
      ui.confirm_action = () -> {
        Task task = new Task("Format Storage Pool:" + pool.name + " with " + Storage.getFormatString(fmt)) {
          public void doTask() {
            try {
              if (pool.format(fmt)) {
                setStatus("Completed");
              } else {
                setStatus("Error occured, check logs.");
              }
            } catch (Exception e) {
              JFLog.log(e);
              setStatus("Error occured, check logs.");
            }
          }
        };
        Tasks.tasks.addTask(ui.tasks, task);
        ui.setRightPanel(storagePanel(ui));
      };
      ui.confirm_popup.setVisible(true);
    });

    cancel.addClickListener((me, cmp) -> {
      ui.setRightPanel(storagePanel(ui));
    });

    return panel;
  }

  private Panel networkPanel(UI ui) {
    TabPanel panel = new TabPanel();
    panel.addTab(networkPanel_vlans(ui), "Networks");
    panel.addTab(networkPanel_bridges(ui), "Virtual Switches");
    panel.addTab(networkPanel_virt(ui), "Server Virtual NICs");
    panel.addTab(networkPanel_iface(ui), "Physical NICs");
    return panel;
  }

  private Panel networkPanel_iface(UI ui) {
    Panel tab = new Panel();
    Row row;

    ToolBar tools = new ToolBar();
    tab.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button link_up = new Button("Link UP");
    tools.add(link_up);
    Button link_down = new Button("Link DOWN");
    tools.add(link_down);

    row = new Row();
    tab.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    tab.add(row);
    Label msg = new Label("");
    row.add(msg);

    row = new Row();
    tab.add(row);
    Table table = new Table(new int[] {100, 200, 150, 50}, col_height, 4, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    Runnable init;

    init = () -> {
      table.removeAll();
      table.addRow(new String[] {"Name", "IP/NetMask", "MAC", "Link"});
      ui.nics_iface = NetworkInterface.listPhysical();
      for(NetworkInterface nic : ui.nics_iface) {
        if (nic.name.equals("lo")) continue;
        if (nic.name.equals("ovs-system")) continue;
        table.addRow(nic.getState());
      }
    };
    init.run();

    refresh.addClickListener((me, cmp) -> {
      init.run();
    });

    link_up.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      NetworkInterface nic = ui.nics_iface[idx];
      nic.link_up();
      msg.setText("Link UP:" + nic.name);
    });

    link_down.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      NetworkInterface nic = ui.nics_iface[idx];
      nic.link_down();
      msg.setText("Link DOWN:" + nic.name);
    });
    return tab;
  }

  private Panel networkPanel_bridges(UI ui) {
    Panel tab = new Panel();
    Row row;

    ToolBar tools = new ToolBar();
    tab.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button create = new Button("Create");
    tools.add(create);
//      Button edit = new Button("Edit");
//      tools.add(edit);
    Button delete = new Button("Delete");
    tools.add(delete);
    Button help = new Button("Help");
    tools.add(help);

    row = new Row();
    tab.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    tab.add(row);
    Table table = new Table(new int[] {100, 50, 100}, col_height, 3, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    Runnable init;

    init = () -> {
      table.removeAll();
      table.addRow(new String[] {"Name", "Type", "Interface"});
      ui.nics_bridge = NetworkBridge.list();
      for(NetworkBridge nic : ui.nics_bridge) {
        table.addRow(new String[] {nic.name, nic.type, nic.iface});
      }
    };
    init.run();

    refresh.addClickListener((me, cmp) -> {
      init.run();
    });

    create.addClickListener((me, cmp) -> {
      ui.network_bridge = null;
      ui.network_bridge_complete = () -> {
        init.run();
      };
      ui.network_bridge_init.run();
      ui.network_bridge_popup.setVisible(true);
    });
/*
    edit.addClickListener((me, cmp) -> {
      int idx = table.getSelectedRow();
      //TODO : edit virtual switch (bridge): edit/remove nics, etc.
    });
*/
    delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      NetworkBridge nic = ui.nics_bridge[idx];
      ui.confirm_action = () -> {
        if (nic.remove()) {
          table.removeRow(idx);
        } else {
          ui.message_message.setText("Failed to remove bridge, see logs.");
          ui.message_popup.setVisible(true);
        }
      };
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Bridge:" + nic.name);
      ui.confirm_popup.setVisible(true);
    });

    help.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfkvm/docs/help_network.html");
    });
    return tab;
  }

  private Panel networkPanel_vlans(UI ui) {
    Panel tab = new Panel();
    Row row;

    ToolBar tools = new ToolBar();
    tab.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button create = new Button("Create");
    tools.add(create);
    Button edit = new Button("Edit");
    tools.add(edit);
    Button delete = new Button("Delete");
    tools.add(delete);
    Button help = new Button("Help");
    tools.add(help);

    row = new Row();
    tab.add(row);
    Label errmsg = new Label("");
    errmsg.setColor(Color.red);
    row.add(errmsg);

    row = new Row();
    tab.add(row);
    Table table = new Table(new int[] {100, 50, 50}, col_height, 3, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    ui.network_vlan_complete = () -> {
      table.removeAll();
      table.addRow(new String[] {"Name", "VLAN", "Usage"});
      for(NetworkVLAN nic : Config.current.vlans) {
        table.addRow(new String[] {nic.name, Integer.toString(nic.vlan), Integer.toString(nic.getUsage())});
      }
    };
    ui.network_vlan_complete.run();

    refresh.addClickListener((me, cmp) -> {
      ui.network_vlan_complete.run();
    });

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
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      ui.network_vlan = Config.current.vlans.get(idx);
      ui.network_vlan_init.run();
      ui.network_vlan_popup.setVisible(true);
    });
    delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      NetworkVLAN nic = Config.current.vlans.get(idx);
      if (nic.getUsage() > 0) {
        ui.message_message.setText("Network is in use");
        ui.message_popup.setVisible(true);
        return;
      }
      ui.confirm_action = () -> {
        int idx1 = table.getSelectedRow();
        NetworkVLAN nic1 = Config.current.vlans.get(idx1);
        Config.current.removeNetworkVLAN(nic1);
        ui.network_vlan_complete.run();
      };
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete VLAN:" + nic.name);
      ui.confirm_popup.setVisible(true);
    });
    help.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfkvm/docs/help_network.html");
    });
    return tab;
  }

  private Panel networkPanel_virt(UI ui) {
    Panel tab = new Panel();
    Row row;

    ToolBar tools = new ToolBar();
    tab.add(tools);
    Button refresh = new Button("Refresh");
    tools.add(refresh);
    Button link_up = new Button("Link UP");
    tools.add(link_up);
    Button link_down = new Button("Link DOWN");
    tools.add(link_down);
    Button create = new Button("Create");
    tools.add(create);
//      Button edit = new Button("Edit");
//      tools.add(edit);
/*
    Button start = new Button("Start");
    tools.add(start);
    Button stop = new Button("Stop");
    tools.add(stop);
*/
    Button delete = new Button("Delete");
    tools.add(delete);
    Button help = new Button("Help");
    tools.add(help);

    row = new Row();
    tab.add(row);
    Label errmsg = new Label("");
    row.add(errmsg);
    errmsg.setColor(Color.red);

    row = new Row();
    tab.add(row);
    Label msg = new Label("");
    row.add(msg);

    row = new Row();
    tab.add(row);
    Table table = new Table(new int[] {100, 200, 150, 50, 100, 50}, col_height, 6, 0);
    row.add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);
    table.setHeader(true);

    Runnable init;

    init = () -> {
      table.removeAll();
      table.addRow(new String[] {"Name", "IP/NetMask", "MAC", "VLAN", "Bridge", "Link"});
      ui.nics_virt = Config.current.nics;
      NetworkVirtual.getInfo(ui.nics_virt.toArray(new NetworkVirtual[0]));
      for(NetworkVirtual nic : ui.nics_virt) {
        table.addRow(nic.getState());
      }
    };
    init.run();

    refresh.addClickListener((me, cmp) -> {
      init.run();
      errmsg.setText("");
      msg.setText("");
    });

    link_up.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      NetworkVirtual nic = ui.nics_virt.get(idx);
      nic.link_up();
      nic.set_ip();
      errmsg.setText("");
      msg.setText("Link UP:" + nic.name);
    });

    link_down.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      NetworkVirtual nic = ui.nics_virt.get(idx);
      nic.link_down();
      errmsg.setText("");
      msg.setText("Link DOWN:" + nic.name);
    });

    create.addClickListener((me, cmp) -> {
      errmsg.setText("");
      msg.setText("");
      if (NetworkBridge.list().length == 0) {
        errmsg.setText("Error:No bridges exist");
        return;
      }
      ui.network_virtual = null;
      ui.network_virtual_complete = () -> {init.run();};
      ui.network_virtual_init.run();
      ui.network_virtual_popup.setVisible(true);
    });

/*
    edit.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      errmsg.setText("");
      msg.setText("");
      NetworkVirtual nic = ui.nics_virt.get(idx);
      ui.network_virtual = nic;
      ui.network_virtual_init.run();
      ui.network_virtual_complete = () -> {
        Config.current.save();
      };
      ui.network_virtual_popup.setVisible(true);
    });
*/

/*
    start.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      errmsg.setText("");
      msg.setText("");
      NetworkVirtual nic = ui.nics_virt.get(idx);
      if (!nic.start()) {
        errmsg.setText("Error:Failed to start nic:" + nic.name);
      } else {
        msg.setText("Started Virtual nic:" + nic.name);
      }
    });

    stop.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      errmsg.setText("");
      msg.setText("");
      NetworkVirtual nic = ui.nics_virt.get(idx);
      if (!nic.stop()) {
        errmsg.setText("Error:Failed to stop nic:" + nic.name);
      } else {
        msg.setText("Stopped Virtual nic:" + nic.name);
      }
    });
*/

    delete.addClickListener((me, cmp) -> {
      errmsg.setText("");
      int idx = table.getSelectedRow();
      if (idx == -1) {
        errmsg.setText("Error:no selection");
        return;
      }
      errmsg.setText("");
      msg.setText("");
      NetworkVirtual nic = ui.nics_virt.get(idx);
      ui.confirm_button.setText("Delete");
      ui.confirm_message.setText("Delete Virtual nic : " + nic.name);
      ui.confirm_action = () -> {
        if (nic.remove()) {
          table.removeRow(idx);
          Config.current.removeNetworkVirtual(nic);
        } else {
          errmsg.setText("Error:Failed to remove Virtual nic:" + nic.name);
        }
      };
      ui.confirm_popup.setVisible(true);
    });

    help.addClickListener((me, cmp) -> {
      cmp.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfkvm/docs/help_network.html");
    });
    return tab;
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

  private void draw_frame(JFImage img, long max_y, int font_width, boolean percent) {
    String eng;
    double div;
    long value_step_y = max_y / 10L;
    if (max_y < 1024L) {
      //bytes / percent
      div = 1;
      if (percent) {
        eng = "";
      } else {
        eng = "B";
      }
    } else if (max_y < 1024L * 512L) {
      //KB
      div = 1024L;
      eng = "KB";
    } else if (max_y < 1024L * 1024L * 512L) {
      //MB
      div = 1024L * 1024L;
      eng = "MB";
    } else {
      //GB
      div = 1024L * 1024L * 1024L;
      eng = "GB";
    }
    int x;
    int y;
    Graphics g = img.getGraphics();
    g.setColor(java.awt.Color.gray);
    {
      //draw grid
      int step_x = data_width / 10;
      int step_y = data_height / 10;
      for(int i=0;i<=10;i++) {
        //horizontal ---
        x = data_margin_left;
        y = data_margin_top + i * step_y;
        g.drawLine(x, y, x + data_width, y);
        //vertical |||
        x = data_margin_left + i * step_x;
        y = data_margin_top;
        g.drawLine(x, y, x, y + data_height);
      }
    }
    g.setColor(java.awt.Color.black);
    {
      //draw axis lines
      x = data_margin_left;
      y = data_margin_top + data_height;
      g.drawLine(x, y, x, y - data_height);
      g.drawLine(x, y, x + data_width, y);
    }
    {
      //draw left axis labels (10 divs)
      x = data_margin_left;
      y = data_margin_top + data_height - 5;
      double value = 0;
      int step_y = data_height / 10;  //25
      for(int step = 0;step <= 10;step++) {
        String str = String.format("%.1f%s", value / div, eng);
        int len = str.length();
        g.drawChars(str.toCharArray(), 0, len, x - (len * font_width), y);
        value += value_step_y;
        y -= step_y;
      }
    }
    {
      //draw bottom axis labels (time : 1 hour / 10 = 6 mins)
      x = data_margin_left;
      y = data_margin_top + data_height + 10;
      int min = 0;
      int step_x = data_width / 10;  //54
      for(int step = 0;step <= 10;step++) {
        String str = String.format("%dmin", min);
        int len = str.length();
        g.drawChars(str.toCharArray(), 0, len, x - (len/2 * font_width), y);
        min += 6;
        x += step_x;
      }
    }
  }

  public Panel getWebConsole(HTTP.Parameters params, WebUIClient client) {
    String id = params.get("id");
    if (id == null) {
      JFLog.log("VNC:vmname==null");
      return null;
    }
    ConsoleSession sess = ConsoleSession.get(id);
    if (sess == null) {
      JFLog.log("VNC:sess==null");
      return null;
    }
    return VNCWebConsole.createPanel(sess.vm.getVNC(), Config.current.vnc_password, VNCWebConsole.OPT_TOOLBAR, client);
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
    //url = /api/...
    if (debug) {
      JFLog.log("url=" + url);
    }
    if (!url.startsWith("/api/")) {
      return null;
    }
    url = url.substring(5);
    switch (url) {
      case "keyfile": {
        File file = new File("/root/cluster/localhost");
        if (!file.exists()) {
          JFLog.log("ssh client key not found");
          return null;
        }
        if (Config.current.token == null) {
          JFLog.log("token not setup");
          return null;
        }
        String token = params.get("token");
        if (token == null) {
          JFLog.log("token not supplied");
          return null;
        }
        if (!token.equals(Config.current.token)) return null;
        //send ssh key
        try {
          FileInputStream fis = new FileInputStream(file);
          byte[] data = fis.readAllBytes();
          fis.close();
          return data;
        } catch (Exception e) {
          JFLog.log(e);
          return null;
        }
      }
      case "addsshkey": {
        String token = params.get("token");
        if (token == null) {
          JFLog.log("token not supplied");
          return null;
        }
        if (!token.equals(Config.current.token)) {
          JFLog.log("token invalid");
          return null;
        }
        String sshkey = params.get("sshkey");
        if (sshkey == null) {
          JFLog.log("sshkey not supplied");
          return null;
        }
        JFLog.log("addsshkey");
        if (Linux.addsshkey(sshkey)) {
          return "okay".getBytes();
        } else {
          return "error".getBytes();
        }
      }
      case "gluster_status": {
        if (Gluster.exists()) {
          return Gluster.getStatus().getBytes();
        } else {
          return "Not setup".getBytes();
        }
      }
      case "ceph_setup_start": {
        String token = params.get("token");
        if (token == null) {
          JFLog.log("token not supplied");
          return null;
        }
        if (!token.equals(Config.current.token)) {
          JFLog.log("token invalid");
          return null;
        }
        if (Config.current.ceph_setup) {
          return "busy".getBytes();
        }
        String hostname = params.get("hostname");
        if (hostname == null) return null;
        Host host = Config.current.getHostByHostname(hostname);
        if (host == null) return null;
        if (Config.current.ceph_setup) {
          return "busy".getBytes();
        }
        host.ceph_setup = true;
        return "okay".getBytes();
      }
      case "ceph_setup_complete": {
        String token = params.get("token");
        if (token == null) {
          JFLog.log("token not supplied");
          return null;
        }
        if (!token.equals(Config.current.token)) {
          JFLog.log("token invalid");
          return null;
        }
        String hostname = params.get("hostname");
        if (hostname == null) return null;
        Host host = Config.current.getHostByHostname(hostname);
        if (host == null) return null;
        host.ceph_setup = false;
        return "okay".getBytes();
      }
      case "ceph_status": {
        if (Ceph.exists()) {
          return Ceph.getStatus().getBytes();
        } else {
          return "Not setup".getBytes();
        }
      }
      case "notify": {
        String token = params.get("token");
        String msg = params.get("msg");
        if (msg == null) return null;
        if (!token.equals(Config.current.token)) return null;
        switch (msg) {
          case "migratevm": {
            return "okay".getBytes();
          }
        }
        break;
      }
      case "getver": {
        return version.getBytes();
      }
      case "gethostname": {
        return VMHost.getHostname().getBytes();
      }
      case "getstorageip": {
        return Config.current.ip_storage.getBytes();
      }
      case "checkvncport": {
        String port = params.get("port");
        String result = "free";
        if (vmm.vnc_port_inuse_local(JF.atoi(port))) {
          result = "inuse";
        }
        return result.getBytes();
      }
      case "getnetworkvlan": {
        String token = params.get("token");
        String network = params.get("network");
        if (network == null) return null;
        if (!token.equals(Config.current.token)) return null;
        NetworkVLAN vlan = Config.current.getNetworkVLAN(network);
        if (vlan == null) return "-1".getBytes();
        return Integer.toString(vlan.vlan).getBytes();
      }
      case "stats": {
        String uuid = params.get("uuid");
        String type = params.get("type");
        String file = params.get("file");
        //replace .png with .stat
        file = file.replace(".png", ".stat");
        String filename = Paths.statsPath + "/" + uuid + "/" + type + "-" + file;
        JFImage img = new JFImage(img_width, img_height);
        Font font = new Font(Font.DIALOG, Font.PLAIN, 10);
        int[] font_metrics = JFAWT.getFontMetrics(font);
        int font_width = font_metrics[0];
        int font_ascent = font_metrics[1];
        int font_decent = font_metrics[2];
        int font_height = font_ascent + font_decent;
        img.setFont(font);
        //generate image
        File stat = new File(filename);
        if (stat.exists()) {
          img.fill(0, 0, img_width, img_height, Color.white);
          try {
            FileInputStream fis = new FileInputStream(filename);
            byte[] data = fis.readAllBytes();
            int pos = 0;
            int longs = data.length / 8;
            switch (type) {
              case "mem": {
                //sample, max, unused (kb)
                int cnt = longs / 4;
                long max = 1024L * 1024L;  //1MB
                //find max value
                for(int a=0;a<cnt;a++) {
                  long sample = LE.getuint64(data, pos); pos += 8;
                  long mem_max = LE.getuint64(data, pos) * 1024L; pos += 8;
                  long mem_unused = LE.getuint64(data, pos) * 1024L; pos += 8;
                  long mem_active = mem_max - mem_unused;
                  pos += 8;  //reserved
                  if (mem_max > max) {
                    max = mem_max;
                  }
                }
                max += 1024L * 1024L;  //1MB
                draw_frame(img, max, font_width, false);
                pos = 0;
                int x;
                int y1;
                int y2;
                int lx = 0;
                int ly1 = 0;
                int ly2 = 0;
                int ys = data_margin_top + data_height;
                for(int a=0;a<cnt;a++) {
                  long sample = LE.getuint64(data, pos); pos += 8;
                  long mem_max = LE.getuint64(data, pos) * 1024L; pos += 8;
                  long mem_unused = LE.getuint64(data, pos) * 1024L; pos += 8;
                  long mem_active = mem_max - mem_unused;
                  pos += 8;  //reserved
                  x = data_margin_left + (int)(sample * 3);
                  y1 = ys - (int)(mem_max * data_height / max);
                  if (a > 0) img.line(lx, ly1, x, y1, Color.blue);
                  y2 = ys - (int)(mem_active * data_height / max);
                  if (a > 0) img.line(lx, ly2, x, y2, Color.red);
                  lx = x;
                  ly1 = y1;
                  ly2 = y2;
                }
                break;
              }
              case "cpu": {
                //sample, time (ns)
                int cnt = longs / 4;
                long max = 100L;  //percent
                draw_frame(img, max, font_width, true);
                long cpu_last = 0;
                pos = 0;
                int x;
                int y;
                int lx = 0;
                int ly = 0;
                int ys = data_margin_top + data_height;
                for(int a=0;a<cnt;a++) {
                  long sample = LE.getuint64(data, pos); pos += 8;
                  long cpu_time = LE.getuint64(data, pos); pos += 8;
                  pos += 8;  //reserved
                  pos += 8;  //reserved
                  x = data_margin_left + (int)(sample * 3);
                  y = ys - (int)((cpu_time - cpu_last) * data_height / _20sec_ns_);  //max = 20 seconds
                  if (y < data_margin_top) {
                    y = data_margin_top;
                  }
                  if (a > 1) img.line(lx, ly, x, y, Color.blue);
                  cpu_last = cpu_time;
                  lx = x;
                  ly = y;
                }
                break;
              }
              case "dsk": {
                //sample, read, write (bytes)
                int cnt = longs / 4;
                long max = 1024;  //1KB
                long last_read = 0;
                long last_write = 0;
                long last_total = 0;
                //find max value
                for(int a=0;a<cnt;a++) {
                  long sample = LE.getuint64(data, pos); pos += 8;
                  long dsk_read = LE.getuint64(data, pos); pos += 8;
                  long dsk_write = LE.getuint64(data, pos); pos += 8;
                  pos += 8;  //reserved
                  long total = dsk_read + dsk_write;
                  long this_total = (dsk_read - last_read) + (dsk_write - last_write);
                  if (a > 1 && this_total > max) {
                    max = total;
                  }
                  last_read = dsk_read;
                  last_write = dsk_write;
                  last_total = total;
                }
                max += 1024L;
                draw_frame(img, max, font_width, false);
                last_read = 0;
                last_write = 0;
                last_total = 0;
                pos = 0;
                int x;
                int y1;
                int y2;
                int y3;
                int lx = 0;
                int ly1 = 0;
                int ly2 = 0;
                int ly3 = 0;
                int ys = data_margin_top + data_height;
                for(int a=0;a<cnt;a++) {
                  long sample = LE.getuint64(data, pos); pos += 8;
                  long dsk_read = LE.getuint64(data, pos); pos += 8;
                  long dsk_write = LE.getuint64(data, pos); pos += 8;
                  pos += 8;  //reserved
                  long total = dsk_read + dsk_write;
                  x = data_margin_left + (int)(sample * 3);
                  y1 = ys - (int)((dsk_read - last_read) * data_height / max);
                  if (a > 1) img.line(lx, ly1, x, y1, Color.green);
                  y2 = ys - (int)((dsk_write - last_write) * data_height / max);
                  if (a > 1) img.line(lx, ly2, x, y2, Color.blue);
                  y3 = ys - (int)((total - last_total) * data_height / max);
                  if (a > 1) img.line(lx, ly3, x, y3, Color.red);
                  last_read = dsk_read;
                  last_write = dsk_write;
                  last_total = total;
                  lx = x;
                  ly1 = y1;
                  ly2 = y2;
                  ly3 = y3;
                }
                break;
              }
              case "net": {
                //sample, read, write (bytes)
                int cnt = longs / 4;
                long max = 1024;  //1KB
                long last_read = 0;
                long last_write = 0;
                long last_total = 0;
                //find max value
                for(int a=0;a<cnt;a++) {
                  long sample = LE.getuint64(data, pos); pos += 8;
                  long net_read = LE.getuint64(data, pos); pos += 8;
                  long net_write = LE.getuint64(data, pos); pos += 8;
                  pos += 8;  //reserved
                  long total = net_read + net_write;
                  long this_total = (net_read - last_read) + (net_write - last_write);
                  if (a > 1 && this_total > max) {
                    max = total;
                  }
                  last_read = net_read;
                  last_write = net_write;
                  last_total = total;
                }
                max += 1024L;
                draw_frame(img, max, font_width, false);
                last_read = 0;
                last_write = 0;
                last_total = 0;
                pos = 0;
                int x;
                int y1;
                int y2;
                int y3;
                int lx = 0;
                int ly1 = 0;
                int ly2 = 0;
                int ly3 = 0;
                int ys = data_margin_top + data_height;
                for(int a=0;a<cnt;a++) {
                  long sample = LE.getuint64(data, pos); pos += 8;
                  long net_read = LE.getuint64(data, pos); pos += 8;
                  long net_write = LE.getuint64(data, pos); pos += 8;
                  pos += 8;  //reserved
                  long total = net_read + net_write;
                  x = data_margin_left + (int)(sample * 3);
                  y1 = ys - (int)((net_read - last_read) * data_height / max);
                  if (a > 1) img.line(lx, ly1, x, y1, Color.green);
                  y2 = ys - (int)((net_write - last_write) * data_height / max);
                  if (a > 1) img.line(lx, ly2, x, y2, Color.blue);
                  y3 = ys - (int)((total - last_total) * data_height / max);
                  if (a > 1) img.line(lx, ly3, x, y3, Color.red);
                  last_read = net_read;
                  last_write = net_write;
                  last_total = total;
                  lx = x;
                  ly1 = y1;
                  ly2 = y2;
                  ly3 = y3;
                }
                break;
              }
            }
          } catch (Exception e) {
            JFLog.log(e);
          }
        } else {
          Graphics g = img.getGraphics();
          img.fill(0, 0, img_width, img_height, Color.grey);
          g.setColor(java.awt.Color.black);
          g.drawChars("n/a".toCharArray(), 0, 3, img_width / 2, img_height / 2);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        img.savePNG(out);
        return out.toByteArray();
      }
    }
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}

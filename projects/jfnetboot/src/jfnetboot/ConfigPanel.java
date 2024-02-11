package jfnetboot;

/** Config Panel
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class ConfigPanel extends Panel {
  private SplitPanel split;
  private Panel nav;

  private PopupPanel form0;
  private PopupPanel form1;
  private PopupPanel form2;
  private PopupPanel form_client;
  private PopupPanel form_purge;
  private PopupPanel form_password;

  private Client edit_client;
  private Client purge_client;

  private Runnable action0, action1, action2;

  private Label form0_msg;
  private Button form0_confirm;

  private Label form1_msg;
  private Label form1_name_label;
  private TextField form1_name;
  private Button form1_confirm;

  private Label form2_msg;
  private Label form2_name_label;
  private TextField form2_name;
  private Label form2_value_label;
  private TextField form2_value;
  private Button form2_confirm;

  private Label form_client_serial;
  private ComboBox form_client_command;
  private TextField form_client_opts;
  private TextField form_client_hostname;
  private CheckBox form_client_ts;
  private TextField form_client_ts_x;
  private TextField form_client_ts_y;
  private CheckBox form_client_vm;
  private TextField form_client_vm_group;
  private TextField form_client_vm_mode;

  private ComboBox form_purge_filesystem;

  public ConfigPanel() {
    split = new SplitPanel(SplitPanel.VERTICAL);
    split.setDividerPosition(100);
    nav = createMenuPanel();
    split.setLeftComponent(nav);
    split.setRightComponent(createHomePanel());
    add(split);
    add(form0 = createForm0PopupPanel());
    add(form1 = createForm1PopupPanel());
    add(form2 = createForm2PopupPanel());
    add(form_client = createEditClientPopupPanel());
    add(form_purge = createPurgeClientPopupPanel());
    add(form_password = createChangePasswordPopupPanel());
  }

  private Panel createMenuPanel() {
    Panel panel = new Panel();
    //left side
    ListBox list = new ListBox();
    //add menu options
    Button opt1 = new Button("Dashboard");
    list.add(opt1);
    opt1.addClickListener( (MouseEvent me, Component c) -> {
      split.setRightComponent(createHomePanel());
    });
    Button opt2 = new Button("File Systems");
    list.add(opt2);
    opt2.addClickListener( (MouseEvent me, Component c) -> {
      split.setRightComponent(createFileSystemsPanel());
    });
    Button opt3 = new Button("Clients");
    list.add(opt3);
    opt3.addClickListener( (MouseEvent me, Component c) -> {
      split.setRightComponent(createClientsPanel());
    });
    Button opt4 = new Button("Commands");
    list.add(opt4);
    opt4.addClickListener( (MouseEvent me, Component c) -> {
      split.setRightComponent(createCommandsPanel());
    });
    Button opt5 = new Button("DHCP");
    list.add(opt5);
    opt5.addClickListener( (MouseEvent me, Component c) -> {
      split.setRightComponent(createDHCPPanel());
    });
    panel.add(list);
    panel.setMaxWidth();
    return panel;
  }

  private Panel createHomePanel() {
    Panel panel = new Panel();
    Column col = new Column();
    panel.add(col);
    col.add(new Label("jfNetBoot/" + Settings.version));
    col.add(new HTMLContainer("hr"));
    col.add(new Label("FileSystems:" + FileSystems.getCount()));
    col.add(new Label("Clients:" + Clients.getCount()));
    Row row = new Row();
    row.add(new Label("Password:"));
    Button change = new Button("Change");
    change.addClickListener( (MouseEvent me, Component c) -> {
      form_password.setVisible(true);
    });
    row.add(change);
    col.add(row);
    return panel;
  }

  private Panel createFileSystemsPanel() {
    Panel panel = new Panel();
    Column col = new Column();
    panel.add(col);
    //add top Label
    col.add(new Label("File Systems"));
    col.add(new HTMLContainer("hr"));
    //add buttons on top : [Create] [Help]
    Row opts = new Row();
    col.add(opts);
    Button create = new Button("Create");
    opts.add(create);
    create.addClickListener( (MouseEvent me, Component c) -> {
      action1 = new Runnable() {
        public void run() {
          String newName = JF.filter(form1_name.getText(), JF.filter_alpha_numeric);
          if (newName.length() == 0) return;
          FileSystems.create(newName, "bios");
          FileSystems.create(newName, "arm");
          FileSystems.create(newName, "x86");
          form1.setVisible(false);
          split.setRightComponent(createFileSystemsPanel());
        }
      };
      form1.setTitle("Create New File System");
      form1_msg.setText("Create a new File System (see help for building the new file system)");
      form1_name_label.setText("Name:");
      form1_name.setText("");
      form1_confirm.setText("Create");
      form1.setVisible(true);
    });
    Button help = new Button("Help");
    opts.add(help);
    help.addClickListener( (MouseEvent me, Component c) -> {
      c.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfnetboot/docs/help_filesystems.html");
    });
    //default file system
    Row row1 = new Row();
    col.add(row1);
    row1.add(new Label("Default FileSystem:"));
    ComboBox cb = new ComboBox();
    String[] fss = FileSystems.getFileSystemNames();
    int idx = -1;
    int pos = 0;
    for(String fs : fss) {
      cb.add(fs, fs);
      if (fs.equals(Settings.current.filesystem_default)) {
        idx = pos;
      }
      pos++;
    }
    if (idx != -1) {
      cb.setSelectedIndex(idx);
    }
    row1.add(cb);
    Button save = new Button("Save");
    row1.add(save);
    Label saved = new Label("");
    row1.add(saved);
    save.addClickListener( (MouseEvent me, Component c) -> {
      String opt = cb.getSelectedText();
      Settings.current.filesystem_default = opt;
      Settings.current.save();
      saved.setText("Saved!");
    });

    //list file systems : <name> <arch> [Archive] [Delete]
    FileSystem[] list = FileSystems.getFileSystems();
    Label status = new Label("");
    col.add(status);
    Table table = new Table(100,32,4,list.length);
    table.add(new Label("Name"), 0, 0);
    table.add(new Label("Arch"), 1, 0);
    table.add(new Label("Actions"), 2, 0);
    col.add(table);
    int tidx = 1;
    for(FileSystem fs : list) {
      String name = fs.getName();
      table.add(new Label(name), 0, tidx);
      String arch = fs.getArch();
      table.add(new Label(arch), 1, tidx);
      Button archive = new Button("Archive");
      table.add(archive, 2, tidx);
      archive.addClickListener( (MouseEvent me, Component c) -> {
        status.setText("Archiving:" + fs.name + ":" + fs.arch + ":please wait...");
        fs.archive(new Runnable() {
          public void run() {
            status.setText("Archive Complete:" + fs.name + ":" + fs.arch);
          }
        });
      });
      if (!name.equals("default")) {
        Button delete = new Button("Delete");
        table.add(delete, 3, tidx);
        delete.addClickListener( (MouseEvent me, Component c) -> {
          action0 = new Runnable() {
            public void run() {
              form0.setVisible(false);
              fs.delete();
              split.setRightComponent(createFileSystemsPanel());
            }
          };
          form0.setTitle("Delete File System");
          form0_msg.setText("Confirm you want to delete file system:" + name);
          form0_confirm.setText("Delete Forever");
          form0.setVisible(true);
        });
      }
      tidx++;
    }
    return panel;
  }

  private Panel createClientsPanel() {
    Panel panel = new Panel();
    Column col = new Column();
    panel.add(col);
    //add top Label
    col.add(new Label("Clients"));
    col.add(new HTMLContainer("hr"));
    //add buttons on top : [Help]
    Row opts = new Row();
    col.add(opts);
    Button help = new Button("Help");
    opts.add(help);
    help.addClickListener( (MouseEvent me, Component c) -> {
      c.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfnetboot/docs/help_clients.html");
    });
    Label status = new Label("");
    opts.add(status);
    //list clients [Edit] [Clone] [Purge] [Shutdown] [Reboot] [Delete]
    Client[] clients = Clients.getClients();
    Table table = new Table(100, 32, 8, clients.length + 1);
    col.add(table);
    table.add(new Label("Name"), 0, 0);
    table.add(new Label("Command"), 1, 0);
    table.add(new Label("Actions"), 2, 0);
    int tidx = 1;
    for(Client client : clients) {
      table.add(new Label(client.getHostname()), 0, tidx);
      table.add(new Label(client.cmd), 1, tidx);
      Button edit = new Button("Edit");
      table.add(edit, 2, tidx);
      edit.addClickListener( (MouseEvent me, Component c) -> {
        form_client_serial.setText(client.getSerial());
        int idx, selected;

        idx = 0;
        selected = -1;
        Command[] cmds = Commands.getCommands();
        form_client_command.clear();
        for(Command cmd : cmds) {
          form_client_command.add(cmd.name, cmd.name);
          if (cmd.name.equals(client.cmd)) {
            selected = idx;
          }
          idx++;
        }
        form_client_command.setSelectedIndex(selected);
        form_client_opts.setText(client.opts);
        form_client_hostname.setText(client.hostname);
        if (client.touchscreen.length() > 0) {
          form_client_ts.setSelected(true);
          String[] ps = client.touchscreen.split(",");
          for(String p : ps) {
            idx = p.indexOf('=');
            if (idx == -1) continue;
            String key = p.substring(0, idx);
            String value = p.substring(idx + 1);
            switch (key) {
              case "x": form_client_ts_x.setText(value); break;
              case "y": form_client_ts_y.setText(value); break;
            }
          }
        } else {
          form_client_ts.setSelected(false);
          form_client_ts_x.setText("");
          form_client_ts_y.setText("");
        }
        if (client.videomode.length() > 0) {
          form_client_vm.setSelected(true);
          String[] ps = client.videomode.split(",");
          for(String p : ps) {
            idx = p.indexOf('=');
            if (idx == -1) continue;
            String key = p.substring(0, idx);
            String value = p.substring(idx + 1);
            switch (key) {
              case "hdmi_group": form_client_vm_group.setText(value); break;
              case "hdmi_mode": form_client_vm_mode.setText(value); break;
            }
          }
        } else {
          form_client_vm.setSelected(false);
          form_client_vm_group.setText("");
          form_client_vm_mode.setText("");
        }
        edit_client = client;
        form_client.setVisible(true);
      });
      Button clone = new Button("Clone");
      table.add(clone, 3, tidx);
      clone.addClickListener( (MouseEvent me, Component c) -> {
        action1 = new Runnable() {
          public void run() {
            String newName = JF.filter(form1_name.getText(), JF.filter_alpha_numeric);
            if (newName.length() == 0) return;
            form1.setVisible(false);
            status.setText("Clone in progress, please wait...");
            new Thread() {
              public void run() {
                client.clone(newName, new Runnable() {
                  public void run() {
                    status.setText("Clone complete!");
                  }
                });
              }
            }.start();
          }
        };
        form1.setTitle("Clone Client File System");
        form1_msg.setText("Creates a clone of clients current File System");
        form1_name_label.setText("Name (alpha-numeric only):");
        form1_name.setText("");
        form1_confirm.setText("Create");
        form1.setVisible(true);
      });
      Button purge = new Button("Purge");
      table.add(purge, 4, tidx);
      purge.addClickListener( (MouseEvent me, Component c) -> {
        int idx = 0;
        int selected = -1;
        String[] fss = FileSystems.getFileSystemNames();
        form_purge_filesystem.clear();
        for(String fs : fss) {
          form_purge_filesystem.add(fs, fs);
          if (fs.equals(client.filesystem)) {
            selected = idx;
          }
          idx++;
        }
        form_purge_filesystem.setSelectedIndex(selected);

        purge_client = client;
        form_purge.setVisible(true);
      });
      Button shutdown = new Button("Shutdown");
      table.add(shutdown, 5, tidx);
      shutdown.addClickListener( (MouseEvent me, Component c) -> {
        action0 = new Runnable() {
          public void run() {
            form0.setVisible(false);
            client.shutdown();
            split.setRightComponent(createClientsPanel());
          }
        };
        form0.setTitle("Shutdown Client");
        form0_msg.setText("Confirm you want to shutdown client:" + client.getSerial());
        form0_confirm.setText("Shutdown");
        form0.setVisible(true);
      });
      Button reboot = new Button("Reboot");
      table.add(reboot, 6, tidx);
      reboot.addClickListener( (MouseEvent me, Component c) -> {
        action0 = new Runnable() {
          public void run() {
            form0.setVisible(false);
            client.reboot();
            split.setRightComponent(createClientsPanel());
          }
        };
        form0.setTitle("Reboot Client");
        form0_msg.setText("Confirm you want to reboot client:" + client.getSerial());
        form0_confirm.setText("Reboot");
        form0.setVisible(true);
      });
      Button delete = new Button("Delete");
      table.add(delete, 7, tidx);
      delete.addClickListener( (MouseEvent me, Component c) -> {
        action0 = new Runnable() {
          public void run() {
            form0.setVisible(false);
            client.delete();
            split.setRightComponent(createClientsPanel());
          }
        };
        form0.setTitle("Delete Client");
        form0_msg.setText("Confirm you want to delete client:" + client.getSerial());
        form0_confirm.setText("Delete Forever");
        form0.setVisible(true);
      });
      tidx++;
    }
    return panel;
  }

  private Panel createCommandsPanel() {
    Panel panel = new Panel();
    Column col = new Column();
    panel.add(col);
    //add top Label
    col.add(new Label("Commands"));
    col.add(new HTMLContainer("hr"));
    //add buttons on top : [Create] [Help]
    Row opts = new Row();
    col.add(opts);
    Button create = new Button("Create");
    opts.add(create);
    create.addClickListener( (MouseEvent me, Component c) -> {
      action2 = new Runnable() {
        public void run() {
          String newName = JF.filter(form2_name.getText(), JF.filter_alpha_numeric);
          if (newName.length() == 0) return;
          String newValue = form2_value.getText();
          if (newValue.length() == 0) return;
          Commands.add(newName, newValue);
          Commands.save();
          form2.setVisible(false);
          split.setRightComponent(createCommandsPanel());
        }
      };
      form2.setTitle("Create New Command");
      form2_msg.setText("Create a command that a client will execute on startup.");
      form2_name_label.setText("Name:");
      form2_name.setText("");
      form2_value_label.setText("Command:");
      form2_value.setText("");
      form2_confirm.setText("Create");
      form2.setVisible(true);
    });
    Button help = new Button("Help");
    opts.add(help);
    help.addClickListener( (MouseEvent me, Component c) -> {
      c.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfnetboot/docs/help_commands.html");
    });
    //list commands : [Edit] [Delete]
    Command[] cmds = Commands.getCommands();
    Table table = new Table(100, 32, 4, cmds.length);
    col.add(table);
    table.add(new Label("Name"), 0, 0);
    table.add(new Label("Actions"), 1, 0);
    int tidx = 1;
    for(Command cmd : cmds) {
      table.add(new Label(cmd.name), 0, tidx);
      Button edit = new Button("Edit");
      table.add(edit, 1, tidx);
      edit.addClickListener( (MouseEvent me, Component c) -> {
        action1 = new Runnable() {
          public void run() {
            String newExec = form1_name.getText();
            if (newExec.length() == 0) return;
            form1.setVisible(false);
            cmd.exec = newExec;
            Commands.save();
            split.setRightComponent(createCommandsPanel());
          }
        };
        form1.setTitle("Edit Command");
        form1_msg.setText("Edit Command : " + cmd.name);
        form1_name_label.setText("Execute:");
        form1_name.setText(cmd.exec);
        form1_confirm.setText("Save");
        form1.setVisible(true);
      });
      if (cmd.name.equals("default")) {
        tidx++;
        continue;
      }
      Button delete = new Button("Delete");
      table.add(delete, 2, tidx);
      delete.addClickListener( (MouseEvent me, Component c) -> {
        action0 = new Runnable() {
          public void run() {
            form0.setVisible(false);
            Commands.delete(cmd);
            Commands.save();
            split.setRightComponent(createCommandsPanel());
          }
        };
        form0.setTitle("Delete Command");
        form0_msg.setText("Confirm you want to command:" + cmd.name);
        form0_confirm.setText("Delete Forever");
        form0.setVisible(true);
      });
      tidx++;
    }
    return panel;
  }

  private Panel createDHCPPanel() {
    Panel panel = new Panel();
    Column col = new Column();
    panel.add(col);
    //add top Label
    col.add(new Label("DHCP"));
    col.add(new HTMLContainer("hr"));
    //add buttons on top : [Help]
    Row opts = new Row();
    col.add(opts);
    Button help = new Button("Help");
    opts.add(help);
    help.addClickListener( (MouseEvent me, Component c) -> {
      c.getClient().openURL("https://pquiring.github.io/javaforce/projects/jfnetboot/docs/help_dhcp.html");
    });
    String dhcp_config_file = JF.getConfigPath() + "/jfdhcp.cfg";
    String dhcp_config = null;
    try {
      FileInputStream fis = new FileInputStream(dhcp_config_file);
      dhcp_config = new String(fis.readAllBytes());
      fis.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
    TextArea config = new TextArea(dhcp_config);
    config.setWidth(400);
    config.setHeight(400);
    col.add(config);
    Row save_msg = new Row();
    col.add(save_msg);
    Button save = new Button("Save");
    save_msg.add(save);
    Label msg = new Label("");
    save_msg.add(msg);
    save.addClickListener( (MouseEvent me, Component c) -> {
      msg.setText("Saving...");
      String new_config = config.getText();
      try {
        FileOutputStream fos = new FileOutputStream(dhcp_config_file);
        fos.write(new_config.getBytes());
        fos.close();
        Service.dhcp.close();
        Service.dhcp = new DHCPServer();
        Service.dhcp.setNotify(Service.tftp);
        Service.dhcp.start();
        msg.setText("Saved!");
      } catch (Exception e) {
        JFLog.log(e);
        msg.setText("Error! See Logs.");
      }
    });
    return panel;
  }

  /** Custom Popup Panel to confirm actions. */
  private PopupPanel createForm0PopupPanel() {
    PopupPanel popup = new PopupPanel("");
    form0_msg = new Label("");
    popup.add(form0_msg);

    Row row3 = new Row();
    popup.add(row3);
    form0_confirm = new Button("Confirm");
    form0_confirm.addClickListener( (MouseEvent me, Component c) -> {
      action0.run();
    });
    row3.add(form0_confirm);
    Button cancel = new Button("Cancel");
    cancel.addClickListener( (MouseEvent me, Component c) -> {
      popup.setVisible(false);
    });
    row3.add(cancel);
    popup.setModal(true);
    return popup;
  }

  /** Custom popup panel with one input fields. */
  private PopupPanel createForm1PopupPanel() {
    PopupPanel popup = new PopupPanel("");
    form1_msg = new Label("");
    popup.add(form1_msg);

    Row row1 = new Row();
    popup.add(row1);
    form1_name_label = new Label("");
    row1.add(form1_name_label);
    form1_name = new TextField("");
    row1.add(form1_name);

    Row row2 = new Row();
    popup.add(row2);
    form1_confirm = new Button("Action");
    form1_confirm.addClickListener( (MouseEvent me, Component c) -> {
      action1.run();
    });
    row2.add(form1_confirm);
    Button cancel = new Button("Cancel");
    cancel.addClickListener( (MouseEvent me, Component c) -> {
      popup.setVisible(false);
    });
    row2.add(cancel);
    popup.setModal(true);
    return popup;
  }

  /** Custom popup panel with two input fields. */
  private PopupPanel createForm2PopupPanel() {
    PopupPanel popup = new PopupPanel("");
    form2_msg = new Label("");
    popup.add(form2_msg);

    Row row1 = new Row();
    popup.add(row1);
    form2_name_label = new Label("");
    row1.add(form2_name_label);
    form2_name = new TextField("");
    row1.add(form2_name);

    Row row2 = new Row();
    popup.add(row2);
    form2_value_label = new Label("");
    row2.add(form2_value_label);
    form2_value = new TextField("");
    row2.add(form2_value);

    Row row3 = new Row();
    popup.add(row3);
    form2_confirm = new Button("Action");
    form2_confirm.addClickListener( (MouseEvent me, Component c) -> {
      action2.run();
    });
    row3.add(form2_confirm);
    Button cancel = new Button("Cancel");
    cancel.addClickListener( (MouseEvent me, Component c) -> {
      popup.setVisible(false);
    });
    row3.add(cancel);
    popup.setModal(true);
    return popup;
  }

  /** Custom popup panel to edit client details. */
  private PopupPanel createEditClientPopupPanel() {
    PopupPanel popup = new PopupPanel("Edit Client");

    Row row1 = new Row();
    popup.add(row1);
    row1.add(new Label("Serial:"));
    form_client_serial = new Label("?");
    row1.add(form_client_serial);

/*
    Row row2a = new Row();
    popup.add(row2a);
    row2a.add(new Label("FileSystem:"));
    form_client_filesystem = new ComboBox();
    String[] fss = FileSystems.getFileSystemNames();
    for(String fs : fss) {
      form_client_filesystem.add(fs, fs);
    }
    row2a.add(form_client_filesystem);
*/

    Row row2 = new Row();
    popup.add(row2);
    row2.add(new Label("Command:"));
    form_client_command = new ComboBox();
    Command[] cmds = Commands.getCommands();
    for(Command cmd : cmds) {
      form_client_command.add(cmd.name, cmd.name);
    }
    row2.add(form_client_command);

    Row row3 = new Row();
    popup.add(row3);
    row3.add(new Label("Kernel Options:"));
    form_client_opts = new TextField("");
    row3.add(form_client_opts);

    Row row3b = new Row();
    popup.add(row3b);
    row3b.add(new Label("Hostname:"));
    form_client_hostname = new TextField("");
    row3b.add(form_client_hostname);

    Row row4 = new Row();
    popup.add(row4);
    row4.add(new Label("Touchscreen:"));
    form_client_ts = new CheckBox("");
    row4.add(form_client_ts);
    row4.add(new Label("X:"));
    form_client_ts_x = new TextField("");
    row4.add(form_client_ts_x);
    row4.add(new Label("Y:"));
    form_client_ts_y = new TextField("");
    row4.add(form_client_ts_y);

    Row row4a = new Row();
//    popup.add(row4a);  //no longer supported
    row4a.add(new Label("VideoMode:"));
    form_client_vm = new CheckBox("");
    row4a.add(form_client_vm);
    row4a.add(new Label("Group:"));
    form_client_vm_group = new TextField("");
    row4a.add(form_client_vm_group);
    row4a.add(new Label("Mode:"));
    form_client_vm_mode = new TextField("");
    row4a.add(form_client_vm_mode);

    Row row5 = new Row();
    popup.add(row5);
    Button form2_client_save = new Button("Save");
    form2_client_save.addClickListener( (MouseEvent me, Component c) -> {
//      edit_client.filesystem = form_client_filesystem.getSelectedText();
      if (edit_client.filesystem == null) edit_client.filesystem = "default";
      edit_client.cmd = form_client_command.getSelectedText();
      if (edit_client.cmd == null) edit_client.cmd = "default";
      edit_client.opts = form_client_opts.getText();
      edit_client.hostname = form_client_hostname.getText();
      if (form_client_ts.isSelected()) {
        edit_client.touchscreen = "x=" + form_client_ts_x.getText() + ",y=" + form_client_ts_y.getText();
      } else {
        edit_client.touchscreen = "";
      }
      if (form_client_vm.isSelected()) {
        edit_client.videomode = "hdmi_group=" + form_client_vm_group.getText() + ",hdmi_mode=" + form_client_vm_mode.getText();
      } else {
        edit_client.videomode = "";
      }
      edit_client.save();
      form_client.setVisible(false);
      split.setRightComponent(createClientsPanel());
    });
    row5.add(form2_client_save);
    Button cancel = new Button("Cancel");
    cancel.addClickListener( (MouseEvent me, Component c) -> {
      popup.setVisible(false);
    });
    row5.add(cancel);

    popup.setModal(true);
    return popup;
  }

  /** Custom popup panel to purge client. */
  private PopupPanel createPurgeClientPopupPanel() {
    PopupPanel popup = new PopupPanel("Purge Client");

    Row row2a = new Row();
    popup.add(row2a);
    row2a.add(new Label("FileSystem:"));
    form_purge_filesystem = new ComboBox();
    String[] fss = FileSystems.getFileSystemNames();
    for(String fs : fss) {
      form_purge_filesystem.add(fs, fs);
    }
    row2a.add(form_purge_filesystem);

    Row row5 = new Row();
    popup.add(row5);
    Button purge = new Button("Purge");
    purge.addClickListener( (MouseEvent me, Component c) -> {
      popup.setVisible(false);
      purge_client.purge();  //long process
      purge_client.filesystem = form_purge_filesystem.getSelectedText();
      purge_client.save();
    });
    row5.add(purge);
    Button cancel = new Button("Cancel");
    cancel.addClickListener( (MouseEvent me, Component c) -> {
      popup.setVisible(false);
    });
    row5.add(cancel);

    popup.setModal(true);
    return popup;
  }

  /** Custom popup panel to change password. */
  private PopupPanel createChangePasswordPopupPanel() {
    PopupPanel popup = new PopupPanel("Change Password");

    Row row1 = new Row();
    popup.add(row1);
    row1.add(new Label("Current Password:"));
    TextField old_password = new TextField("");
    row1.add(old_password);

    Row row2 = new Row();
    popup.add(row2);
    row2.add(new Label("New Password:"));
    TextField new_password = new TextField("");
    row2.add(new_password);

    Row row3 = new Row();
    popup.add(row3);
    row3.add(new Label("Confirm Password:"));
    TextField confirm_password = new TextField("");
    row3.add(confirm_password);

    Row row4 = new Row();
    popup.add(row4);
    Label msg = new Label("");
    msg.setColor(Color.red);
    row4.add(msg);

    Row row5 = new Row();
    popup.add(row5);
    Button change = new Button("Change");
    change.addClickListener( (MouseEvent me, Component c) -> {
      String oldPass = Settings.encodePassword(old_password.getText());
      String newPass = new_password.getText();
      String conPass = confirm_password.getText();
      if (!Settings.current.password.equals(oldPass)) {
        msg.setText("Wrong password");
        return;
      }
      if (newPass.length() < 4) {
        msg.setText("Min length is 4");
        return;
      }
      if (!newPass.equals(conPass)) {
        msg.setText("Passwords do not match");
        return;
      }
      Settings.current.password = Settings.encodePassword(newPass);
      Settings.current.save();
      msg.setText("");
      popup.setVisible(false);
    });
    row5.add(change);
    Button cancel = new Button("Cancel");
    cancel.addClickListener( (MouseEvent me, Component c) -> {
      popup.setVisible(false);
    });
    row5.add(cancel);

    popup.setModal(true);
    return popup;
  }

}

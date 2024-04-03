/** ConfigService
 *
 * @author pquiring
 */

import java.util.Calendar;
import java.util.Arrays;
import java.util.Comparator;
import java.io.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

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
    String dname = "CN=jfmonitor.sourceforge.net, O=server, OU=webserver, C=CA";
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

  public Panel getRootPanel(WebUIClient client) {
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
    row.add(password);
    Button login = new Button("Login");
    row.add(login);
    inner.add(row);
    login.addClickListener( (MouseEvent m, Component c) -> {
      String passTxt = password.getText();
      WebUIClient webclient = c.getClient();
      if (passTxt.equals(Config.current.password)) {
        webclient.setProperty("password", passTxt);
        webclient.setPanel(getRootPanel(webclient));
      } else {
        msg.setText("Wrong password");
        msg.setColor(Color.red);
      }
    });
    ctr.add(inner);
    panel.add(ctr);
    return panel;
  }

  public Panel serverPanel(WebUIClient webclient) {
    Panel panel = new Panel();
    SplitPanel split = new SplitPanel(SplitPanel.VERTICAL);
    split.setName("split");
    split.setDividerPosition(120);
    Panel left = serverLeftPanel();
    Panel right = null;
    String screen = (String)webclient.getProperty("screen");
    if (screen == null) screen = "";
    switch (screen) {
      case "": right = serverHome(); break;
      case "status": right = serverStatus(); break;
      case "monitor_network": right = serverMonitorNetwork(); break;
      case "monitor_storage": right = serverMonitorStorage(); break;
      case "config_network": right = serverConfigNetwork(); break;
      case "config_storage": right = serverConfigStorage(); break;
      case "config": right = serverConfig(); break;
      case "logs": right = serverLogs(null); break;
      default: JFLog.log("Unknown screen:" + screen); break;
    }
    split.setLeftComponent(left);
    if (right != null) {
      split.setRightComponent(right);
    }
    panel.add(split);

    return panel;
  }

  public Panel serverLeftPanel() {
    Panel panel = new Panel();
    //left side
    ListBox list = new ListBox();
    list.setName("list");
    //add menu options
    Button opt1 = new Button("Status");
    list.add(opt1);
    opt1.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "status");
      webclient.refresh();
    });
    Button opt2 = new Button("Network Monitor");
    list.add(opt2);
    opt2.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "monitor_network");
      webclient.refresh();
    });
    Button opt3 = new Button("Storage Monitor");
    list.add(opt3);
    opt3.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "monitor_storage");
      webclient.refresh();
    });
    Button opt4 = new Button("Setup Network");
    list.add(opt4);
    opt4.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "config_network");
      webclient.refresh();
    });
/*
    Button opt5 = new Button("Setup Storage");
    list.add(opt5);
    opt5.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "config_storage");
      webclient.refresh();
    });
*/
    Button opt6 = new Button("Configure");
    list.add(opt6);
    opt6.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "config");
      webclient.refresh();
    });
    Button opt7 = new Button("Logs");
    list.add(opt7);
    opt7.addClickListener( (MouseEvent me, Component c) -> {
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
    Row row = new Row();

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
          if (dev != null) {
            TextField desc = new TextField(dev.desc);
            row.add(desc);
            CheckBox notify = new CheckBox("notify");
            notify.setSelected(ip.notify);
            row.add(notify);
            Button add = new Button("Save");
            add.addClickListener( (MouseEvent me, Component c) -> {
              ip.notify = notify.isSelected();
              Device newdev = Config.current.getDevice(ip.mac);
              newdev.desc = desc.getText();
              Config.save();
              WebUIClient webclient = c.getClient();
              webclient.refresh();
            });
            row.add(add);
          } else {
            TextField desc = new TextField("Unknown device");
            row.add(desc);
            CheckBox notify = new CheckBox("notify");
            notify.setSelected(ip.notify);
            row.add(notify);
            Button add = new Button("Add");
            add.addClickListener( (MouseEvent me, Component c) -> {
              ip.notify = notify.isSelected();
              Device newdev = new Device();
              newdev.mac = ip.mac;
              newdev.desc = desc.getText();
              Config.current.addDevice(newdev);
              Config.save();
              WebUIClient webclient = c.getClient();
              webclient.refresh();
            });
            row.add(add);
          }
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
      pass.setName("password");
//      password.setPassword(true);  //login may be blocked - need to create HTTPS server
      row.add(pass);
      server_panel.add(row);

      row = new Row();
      row.add(new Label("Confirm Password:"));
      TextField confirm = new TextField("");
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

  public byte[] getResource(String url) {
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
}

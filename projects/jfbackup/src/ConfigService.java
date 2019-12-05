/** ConfigService
 *
 * @author pquiring
 */

import java.io.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Stack;
import java.util.Comparator;

import javaforce.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class ConfigService implements WebUIHandler {
  public WebUIServer server;
  public Client client;

  public void start() {
    server = new WebUIServer();
    server.start(this, 34003, false);
  }

  public void stop() {
    if (server == null) return;
    server.stop();
    server = null;
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
    Row row;
    row = new Row();
    row.add(new Label("jfBackup Login"));
    panel.add(row);
    Label msg = new Label("");
    panel.add(msg);
    row = new Row();
    row.add(new Label("Password:"));
    TextField password = new TextField("");
    row.add(password);
    Button login = new Button("Login");
    row.add(login);
    panel.add(row);
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
    return panel;
  }

  public Panel serverPanel(WebUIClient webclient) {
    Panel panel = new Panel();
    SplitPanel split = new SplitPanel(SplitPanel.VERTICAL);
    split.setName("split");
    split.setDividerPosition(100);
    Panel left = serverLeftPanel();
    Panel right = null;
    String screen = (String)webclient.getProperty("screen");
    if (screen == null) screen = "";
    switch (screen) {
      case "": right = serverHome(); break;
      case "status": right = serverStatus(); break;
      case "monitor": right = serverMonitor(); break;
      case "backups": right = serverBackups(); break;
      case "restores": right = serverRestores(); break;
      case "config": right = serverConfig(); break;
      case "tapes": right = serverTapes(); break;
      case "logs": right = serverLogs(null); break;
    }
    split.setLeftComponent(left);
    split.setRightComponent(right);
    panel.add(split);

    return panel;
  }

  public Panel serverLeftPanel() {
    Panel panel = new Panel();
    //left side
    List list = new List();
    list.setName("list");
    //add menu options
    Button opt1 = new Button("Status");
    list.add(opt1);
    opt1.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "status");
      webclient.refresh();
    });
    Button opt2 = new Button("Job Monitor");
    list.add(opt2);
    opt2.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "monitor");
      webclient.refresh();
    });
    Button opt3 = new Button("Backup Jobs");
    list.add(opt3);
    opt3.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "backups");
      webclient.refresh();
    });
    Button opt4 = new Button("Restore Jobs");
    list.add(opt4);
    opt4.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "restores");
      webclient.refresh();
    });
    Button opt5 = new Button("Configure");
    list.add(opt5);
    opt5.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "config");
      webclient.refresh();
    });
    Button opt6 = new Button("Tapes");
    list.add(opt6);
    opt6.addClickListener( (MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setProperty("screen", "tapes");
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
    Label label = new Label("jfBackup/" + Config.AppVersion);
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
    Label label = new Label("jfBackup/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    panel.add(new Label("Status:"));

    TextArea text = new TextArea("Loading...");
    text.setReadonly(true);
    text.setMaxWidth();
    text.setMaxHeight();

    panel.add(text);

    new Thread() {
      public void run() {
        StringBuilder sb = new StringBuilder();
        if (Status.running) {
          sb.append("Job Status : " + Status.desc + "\r\n");
        } else {
          sb.append("Job Status : none\r\n");
        }
        sb.append("\r\n");
        if (Config.current.changerDevice.length() > 0) {
          //load changer status
          Element elements[] = new MediaChanger().list();
          if (elements != null) {
            sb.append("Media Changer:\r\n\r\n");
            for(int a=0;a<elements.length;a++) {
              Element element = elements[a];
              sb.append(element.name + ":" + element.barcode);
              sb.append("\r\n\r\n");
            }
          } else {
            sb.append("Media Changer Error\r\n");
          }
        }
        text.setText(sb.toString());
      }
    }.start();

    return panel;
  }

  public Panel serverMonitor() {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("jfBackup/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    panel.add(new Label("Monitor:"));

    TextArea text = new TextArea("Loading...");
    text.setReadonly(true);
    text.setMaxWidth();
    text.setMaxHeight();

    panel.add(text);

    new Thread() {
      public void run() {
        WebUIClient webclient = panel.getClient();
        int id = webclient.getCurrentID();
        if (id == -1) return;
        StringBuilder sb = new StringBuilder();
        while (webclient.getCurrentID() == id) {
          sb.setLength(0);
          if (Status.running) {
            sb.append("Job Status : " + Status.desc + "\r\n");
            if (Status.log != null) {
              sb.append(Status.log);
            }
          } else {
            sb.append("No job running");
          }
          text.setText(sb.toString());
          JF.sleep(1000);
        }
      }
    }.start();

    return panel;
  }

  public Panel serverBackups() {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("jfBackup/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    AutoScrollPanel scroll = new AutoScrollPanel();
    scroll.setMaxWidth();
    scroll.setMaxHeight();
    panel.add(scroll);

    scroll.add(new Label("Backup Jobs:"));

    row = new Row();
    Button create = new Button("Create Job");
    create.addClickListener((MouseEvent me, Component c) -> {
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverCreateBackupJob());
    });
    row.add(create);
    scroll.add(row);

    //list current jobs
    for(EntryJob job : Config.current.backups) {
      row = new Row();
      row.add(new Label("Job:" + job.name));
      Button edit = new Button("Edit");
      edit.addClickListener((MouseEvent me, Component c) -> {
        SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
        split.setRightComponent(serverEditBackupJob(job));
      });
      row.add(edit);
      Button delete = new Button("Delete");
      delete.addClickListener((MouseEvent me, Component c) -> {
        SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
        split.setRightComponent(serverDeleteBackupJob(job));
      });
      row.add(delete);
      scroll.add(row);
    }

    return panel;
  }

  public Panel serverCreateBackupJob() {
    Panel panel = new Panel();
    panel.add(new Label("Create Backup Job:"));
    Label msg = new Label("");
    panel.add(msg);
    Row row = new Row();
    row.add(new Label("Job Name:"));
    TextField name = new TextField("job-" + Config.current.backups.size() + 1);
    row.add(name);
    panel.add(row);
    row = new Row();
    Button next = new Button("Next");
    row.add(next);
    panel.add(row);
    next.addClickListener( (MouseEvent me, Component c) -> {
      String nameTxt = cleanHost(name.getText());
      if (nameTxt.length() == 0) {
        name.setText(nameTxt);
        msg.setColor(Color.red);
        msg.setText("Invalid name");
        return;
      }
      EntryJob job = new EntryJob();
      job.name = nameTxt;
      job.freq = "weekly";
      job.day = 1;  //Sunday
      job.hour = 21;  //9PM
      job.minute = 0;
      Config.current.backups.add(job);
      Config.save();
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJobSchedule(job));
    });
    return panel;
  }

  public Panel serverEditBackupJobSchedule(EntryJob job) {
    Panel panel = new Panel();
    panel.add(new Label("Backup Job Schedule:"));
    Label msg = new Label("");
    panel.add(msg);
    Row row = new Row();
    row.add(new Label("Backup Frequency:"));
    panel.add(row);

    row = new Row();
    CheckBox opt1 = new CheckBox("weekly");
    opt1.setSelected(job.freq.equals("weekly"));
    row.add(opt1);
    ComboBox day = new ComboBox();
    day.add("sunday", "Sunday");
    day.add("monday", "Monday");
    day.add("tuesday", "Tuesday");
    day.add("wednesday", "Wednesday");
    day.add("thurday", "Thurday");
    day.add("friday", "Friday");
    day.add("saturday", "Saturday");
    row.add(day);
    day.setSelectedIndex(job.day - 1);

    panel.add(row);

    row = new Row();
    CheckBox opt2 = new CheckBox("daily");
    opt2.setSelected(job.freq.equals("daily"));
    row.add(opt2);
    panel.add(row);

    opt1.addChangedListener((Component c) -> {
      boolean sel = opt1.isSelected();
      opt2.setSelected(!sel);
    });
    opt2.addChangedListener((Component c) -> {
      boolean sel = opt2.isSelected();
      opt1.setSelected(!sel);
    });

    row = new Row();
    row.add(new Label("Hour:"));
    TextField hour = new TextField(Integer.toString(job.hour));
    row.add(hour);
    row.add(new Label("(0-23)"));
    panel.add(row);

    row = new Row();
    row.add(new Label("Minute:"));
    TextField minute = new TextField(Integer.toString(job.minute));
    row.add(minute);
    row.add(new Label("(0-59)"));
    panel.add(row);

    row = new Row();
    Button save = new Button("Save");
    row.add(save);
    panel.add(row);
    save.addClickListener( (MouseEvent me, Component c) -> {
      String freq = null;
      if (opt1.isSelected()) {
        freq = "weekly";
      } else if (opt2.isSelected()) {
        freq = "daily";
      }
      if (freq == null) {
        msg.setText("Make a selection");
        msg.setColor(Color.red);
        return;
      }
      job.freq = freq;
      String hourTxt = cleanNumber(hour.getText());
      if (hourTxt.length() == 0 || hourTxt.length() > 2) {
        hour.setText("");
        msg.setText("Invalid hour");
        msg.setColor(Color.red);
        return;
      }
      job.day = day.getSelectedIndex() + 1;
      int hourInt = Integer.valueOf(hourTxt);
      if (hourInt > 23) {
        hour.setText("");
        msg.setText("Invalid hour");
        msg.setColor(Color.red);
        return;
      }
      job.hour = hourInt;
      String minuteTxt = cleanNumber(minute.getText());
      if (minuteTxt.length() == 0 || minuteTxt.length() > 2) {
        minute.setText("");
        msg.setText("Invalid minute");
        msg.setColor(Color.red);
        return;
      }
      int minuteInt = Integer.valueOf(minuteTxt);
      if (minuteInt > 59) {
        minute.setText("");
        msg.setText("Invalid minute");
        msg.setColor(Color.red);
        return;
      }
      job.minute = minuteInt;
      Config.save();

      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJob(job));
    });
    return panel;
  }

  public Panel serverEditBackupJob(EntryJob job) {
    Panel panel = new Panel();
    panel.add(new Label("Edit Backup Job:" + job.name));
    Label msg = new Label("");
    panel.add(msg);

    Row row = new Row();
    Button editTime = new Button("Edit Backup Schedule");
    row.add(editTime);
    panel.add(row);
    editTime.addClickListener((MouseEvent me, Component c) -> {
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJobSchedule(job));
    });

    //display backup local/remote volumes to tape drive
    {
      row = new Row();
      row.add(new Label("Backup Remote/Local Volumes to Tape Drive:"));
      panel.add(row);
      Table table = new Table(100,25,5,job.backup.size());
      int y = 0;
      for(EntryJobVolume backup : job.backup) {
        TextField host = new TextField(backup.host);
        host.setReadonly(true);
        table.add(host,0,y);
        TextField volume = new TextField(backup.volume);
        volume.setReadonly(true);
        table.add(volume,1,y);
        TextField path = new TextField(backup.path);
        path.setReadonly(true);
        table.add(path,2,y);
        Button edit = new Button("Edit");
        table.add(edit,3,y);
        edit.addClickListener((MouseEvent me, Component c) -> {
          ServerClient backupclient = BackupService.server.getClient(backup.host);
          if (backupclient == null) {
            msg.setColor(Color.red);
            msg.setText("That client is not online");
            return;
          }
          backupclient.addRequest(new Request("listvolumes"), (Request req) -> {
            SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
            split.setRightComponent(serverEditBackupJobVolume(job, backup, req.reply, false));
          });
        });
        Button delete = new Button("Delete");
        table.add(delete,4,y);
        delete.addClickListener((MouseEvent me, Component c) -> {
          ServerClient backupclient = BackupService.server.getClient(backup.host);
          if (backupclient == null) {
            msg.setColor(Color.red);
            msg.setText("That client is not online");
            return;
          }
          SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
          split.setRightComponent(serverDeleteBackupJobVolume(job, backup));
        });
        y++;
      }
      panel.add(table);
      row = new Row();
      ComboBox hosts = new ComboBox();
      for(String host : Config.current.hosts) {
        hosts.add(host, host);
      }
      row.add(hosts);
      Button add = new Button("Add Volume");
      add.addClickListener((MouseEvent me, Component c) -> {
        String host = hosts.getSelectedValue();
        ServerClient backupclient = BackupService.server.getClient(host);
        if (backupclient == null) {
          msg.setColor(Color.red);
          msg.setText("That client is not online");
          return;
        }
        EntryJobVolume jobvolume = new EntryJobVolume();
        jobvolume.host = host;
        backupclient.addRequest(new Request("listvolumes"), (Request req) -> {
          SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
          split.setRightComponent(serverEditBackupJobVolume(job, jobvolume, req.reply, true));
        });
      });
      row.add(add);
      panel.add(row);
    }

    //show "Finish" button
    row = new Row();
    Button finish = new Button("Finish");
    finish.addClickListener((MouseEvent me, Component c) -> {
      WebUIClient webclient = c.getClient();
      webclient.setPanel(serverPanel(webclient));
    });
    row.add(finish);
    panel.add(row);

    return panel;
  }

  public Panel serverEditBackupJobVolume(EntryJob job, EntryJobVolume jobvol, String vollist, boolean create) {
    //job.host (given)
    //job.volume
    //job.path
    Panel panel = new Panel();
    Row row = new Row();
    row.add(new Label("Host:" + jobvol.host));
    panel.add(row);

    Label msg = new Label("");
    panel.add(msg);
    row = new Row();

    //display list of volumes
    row = new Row();
    row.add(new Label("Volume:"));
    ComboBox vol = new ComboBox();
    String vs[] = vollist.split("\r\n");
    int sel = -1;
    int idx = 0;
    for(String v : vs) {
      if (v.length() == 0) continue;
      vol.add(v, v);
      if (jobvol.volume != null && jobvol.volume.equals(v)) {
        sel = idx;
      }
      idx++;
    }
    if (sel != -1) {
      vol.setSelectedIndex(sel);
    }
    row.add(vol);
    panel.add(row);

    row = new Row();
    row.add(new Label("Remote Path:"));
    if (jobvol.path == null) jobvol.path = "";
    TextField remotePath = new TextField(jobvol.path);
    row.add(remotePath);
    row.add(new Label("(optional)"));
    panel.add(row);

    if (idx == 0) {
      row = new Row();
      row.add(new Label("Error:No volumes found on host to backup."));
      panel.add(row);
      return panel;
    }

    row = new Row();
    Button save = new Button("Save");
    row.add(save);
    Button cancel = new Button("Cancel");
    row.add(cancel);
    panel.add(row);

    save.addClickListener((MouseEvent me, Component c) -> {
      String remoteTxt = cleanPath(remotePath.getText());
      if (remoteTxt.length() == 0) {
        remoteTxt = "";
      }
      if (!remoteTxt.startsWith("\\")) {
        remoteTxt = "\\" + remoteTxt;
      }
      if (remoteTxt.endsWith("\\")) {
        //will also change "\\" to "" to avoid double \\ in path
        remoteTxt = remoteTxt.substring(0, remoteTxt.length() - 1);
      }
      jobvol.volume = vol.getSelectedText();
      if (jobvol.volume.length() == 0) {
        msg.setText("Please select a volume");
        msg.setColor(Color.red);
        return;
      }
      jobvol.path = remoteTxt;
      if (create) {
        job.backup.add(jobvol);
      }
      Config.save();
      //return to backup editor
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJob(job));
    });

    cancel.addClickListener((MouseEvent me, Component c) -> {
      //return to backup editor
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJob(job));
    });

    return panel;
  }

  public Panel serverDeleteBackupJobVolume(EntryJob job, EntryJobVolume jobvol) {
    Panel panel = new Panel();
    Row row = new Row();

    row.add(new Label("Confirm Deletion:" + jobvol.host + " Volume:" + jobvol.volume + jobvol.path));
    panel.add(row);

    row = new Row();
    Button delete = new Button("Delete");
    row.add(delete);
    Button cancel = new Button("Cancel");
    row.add(cancel);
    panel.add(row);

    delete.addClickListener((MouseEvent me, Component c) -> {
      job.backup.remove(jobvol);
      Config.save();
      //return to backup editor
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJob(job));
    });

    cancel.addClickListener((MouseEvent me, Component c) -> {
      //return to backup editor
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJob(job));
    });

    return panel;
  }

  public Panel serverDeleteBackupJob(EntryJob job) {
    Panel panel = new Panel();
    Row row = new Row();

    row.add(new Label("Confirm Deletion:" + job.name));
    panel.add(row);

    row = new Row();
    Button delete = new Button("Delete");
    row.add(delete);
    Button cancel = new Button("Cancel");
    row.add(cancel);
    panel.add(row);

    delete.addClickListener((MouseEvent me, Component c) -> {
      Config.current.backups.remove(job);
      Config.save();
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverHome());
    });

    cancel.addClickListener((MouseEvent me, Component c) -> {
      //return to backup editor
      SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
      split.setRightComponent(serverEditBackupJob(job));
    });

    return panel;
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

  public Panel serverRestores() {
    Panel panel = new Panel();
    Row row = new Row();
    row.add(new Label("Restore from Tape"));
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    AutoScrollPanel scroll = new AutoScrollPanel();
    scroll.setMaxWidth();
    scroll.setMaxHeight();
    panel.add(scroll);

    //list backups
    File folder = new File(Paths.catalogsPath);
    File files[] = folder.listFiles();
    if (files == null) files = new File[0];

    int count = 0;

    for(File file : files) {
      String name = file.getName();  //catalog-###.dat
      if (!name.startsWith("catalog-")) continue;
      if (!name.endsWith(".nfo")) continue;
      name = name.substring(8);  //remove catalog-
      name = name.substring(0, name.length() - 4);  //remove .nfo
      long backup = Long.valueOf(name);
      CatalogInfo info = CatalogInfo.load(backup);

      row = new Row();
      row.add(new Label("Backup:" + info.name + "@" + toDateTime(backup)));
      scroll.add(row);

      for(EntryTape tape : info.tapes) {
        row = new Row();
        row.add(new Label("  Tape:" + tape.barcode));
        scroll.add(row);
      }

      row = new Row();
      Button restore = new Button("Restore Files");
      row.add(restore);
      scroll.add(row);

      restore.addClickListener((MouseEvent me, Component c) -> {
        SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
        split.setRightComponent(serverCreateRestoreJob(info, null, null));
      });

      row = new Row();
      row.setBackColor(Color.blue);
      row.setHeight(5);
      scroll.add(row);
      count++;
    }

    if (count == 0) {
      row = new Row();
      row.add(new Label("No backups found"));
      scroll.add(row);
    }

    return panel;
  }

  private Catalog cat;
  private RestoreInfo ri;
  private Stack<EntryFolder> stack;

  private void updateSelected(Label lbl) {
    lbl.setText(String.format("%d volumes, %d folders, %d files selected", ri.restoreVolumes.size(), ri.restoreFolders.size(), ri.restoreFiles.size()));
  }

  private EntryFolder getParent() {
    if (stack.size() == 0) return null;
    return stack.pop();
  }

  private EntryFolder peekParent() {
    if (stack.size() == 0) return null;
    return stack.peek();
  }

  private Label getPath() {
    EntryFolder path[] = stack.toArray(new EntryFolder[stack.size()]);
    StringBuilder sb = new StringBuilder();
    for(EntryFolder folder : path) {
      sb.append("\\");
      sb.append(folder.name);
    }
    return new Label(sb.toString());
  }

  public Panel serverCreateRestoreJob(CatalogInfo catinfo, EntryVolume volume, EntryFolder folder) {
    Panel panel = new Panel();
    Row row = new Row();
    row.add(new Label("Restore Selection"));
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    if (ri == null) {
      ri = new RestoreInfo();
      stack = new Stack();
    }
    Label selected = new Label("...");
    updateSelected(selected);

    if (cat == null || cat.backup != catinfo.backup) {
      row = new Row();
      row.add(new Label("Loading catalog, please wait..."));
      panel.add(row);
      new Thread() {
        public void run() {
          WebUIClient webclient = panel.getClient();
          int id = webclient.getCurrentID();
          if (id == -1) return;
          Panel panel2 = new Panel();
          Row row;
          ri = new RestoreInfo();
          stack = new Stack();
          cat = Catalog.load(catinfo.backup);
          if (cat == null || cat.backup != catinfo.backup) {
            row = new Row();
            row.add(new Label("Error:That catalog is missing or corrupt"));
            panel2.add(row);
          } else {
            panel2 = serverCreateRestoreJob(catinfo, volume, folder);
          }
          SplitPanel split = (SplitPanel)webclient.getPanel().getComponent("split");
          if (webclient.getCurrentID() != id) return;  //impatient user
          split.setRightComponent(panel2);
        }
      }.start();
      return panel;
    }

    if (volume == null) {
      //list available volumes
      for(EntryVolume vol : cat.volumes) {
        row = new Row();
        CheckBox cb = new CheckBox(vol.volume);
        cb.setSelected(ri.restoreVolumes.contains(vol));
        cb.addChangedListener((Component c) -> {
          if (ri.restoreVolumes.contains(vol))
            ri.restoreVolumes.remove(vol);
          else
            ri.restoreVolumes.add(vol);
          //TODO : unselect any child folders/files
          updateSelected(selected);
        });
        row.add(cb);
        Button enter = new Button("Enter");
        enter.addClickListener((MouseEvent me, Component c) -> {
          EntryFolder volFolder = new EntryFolder();
          volFolder.name = vol.volume;
          volFolder.isVolume = true;
          stack.push(volFolder);
          SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
          split.setRightComponent(serverCreateRestoreJob(catinfo, vol, null));
        });
        row.add(enter);
        row.add(new Label(" on Host:" + vol.host));
        panel.add(row);
      }
    } else {
      EntryFolder folder2 = folder;
      if (folder2 == null) {
        folder2 = volume.root;
      }
      row = new Row();
      Button up = new Button("Up");
      up.addClickListener((MouseEvent me, Component c) -> {
        EntryFolder parent = getParent();
        if (parent.isVolume) {
          parent = null;
        } else {
          parent = peekParent();
        }
        SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
        split.setRightComponent(serverCreateRestoreJob(catinfo, parent == null ? null : volume, parent != null && !parent.isVolume ? parent : null));
      });
      row.add(up);
      row.add(getPath());
      panel.add(row);
      for(EntryFolder childFolder : folder2.folders) {
        row = new Row();
        CheckBox cb = new CheckBox("\\" + childFolder.name);
        cb.setSelected(ri.restoreFolders.contains(childFolder));
        cb.addChangedListener((Component c) -> {
          if (ri.restoreFolders.contains(childFolder))
            ri.restoreFolders.remove(childFolder);
          else
            ri.restoreFolders.add(childFolder);
          //TODO : unselect any child folders/files
          updateSelected(selected);
        });
        row.add(cb);
        Button enter = new Button("Enter");
        enter.addClickListener((MouseEvent me, Component c) -> {
          stack.push(childFolder);
          SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
          split.setRightComponent(serverCreateRestoreJob(catinfo, volume, childFolder));
        });
        row.add(enter);
        panel.add(row);
      }
      for(EntryFile file : folder2.files) {
        row = new Row();
        CheckBox cb = new CheckBox(file.name);
        cb.setSelected(ri.restoreFiles.contains(file));
        cb.addChangedListener((Component c) -> {
          if (ri.restoreFiles.contains(file))
            ri.restoreFiles.remove(file);
          else
            ri.restoreFiles.add(file);
          updateSelected(selected);
          //TODO : unselect any parent folders
        });
        row.add(cb);
        panel.add(row);
      }
    }

    row = new Row();
    Button restore = new Button("Restore");
    restore.addClickListener((MouseEvent me, Component c) -> {
      if (Status.running) {
        SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
        split.setRightComponent(serverMonitor());
      } else {
        Status.running = true;
        Status.abort = false;
        Status.desc = "Running restore job";
        Status.log = new StringBuilder();
        new RestoreJob(cat, catinfo, ri).start();
        cat = null;
        SplitPanel split = (SplitPanel)c.getClient().getPanel().getComponent("split");
        split.setRightComponent(serverMonitor());
      }
    });
    row.add(restore);
    row.add(selected);
    panel.add(row);
    if (cat.haveChanger) {
      row = new Row();
      row.add(new Label("Make sure following tape(s) are present before you start restore job."));
      panel.add(row);
      for(EntryTape tape : catinfo.tapes) {
        row = new Row();
        row.add(new Label("Tape:" + tape.barcode));
        panel.add(row);
      }
    } else {
      row = new Row();
      row.add(new Label("Make sure proper tape is inserted before you start restore job."));
      panel.add(row);
    }

    return panel;
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
    row.add(new Label("Retention Period:"));
    row.add(new Label("Years:"));
    TextField years = new TextField(Integer.toString(Config.current.retention_years));
    row.add(years);
    row.add(new Label("Months:"));
    TextField months = new TextField(Integer.toString(Config.current.retention_months));
    row.add(months);
    Button retention_save = new Button("Save");
    retention_save.addClickListener((MouseEvent me, Component c) -> {
      try {
        int y = Integer.valueOf(years.getText());
        int m = Integer.valueOf(months.getText());
        if (y > 100) y = 100;
        if (y < 0) y = 0;
        if (m > 12) m = 12;
        if (m < 0) m = 0;
        if (y == 0 && m == 0) throw new Exception("no retention");
        Config.current.retention_years = y;
        Config.current.retention_months = m;
        Config.save();
        msg.setText("Updated Retention");
        msg.setColor(Color.black);
      } catch (Exception e) {
        e.printStackTrace();
        msg.setText("Failed to save retention");
        msg.setColor(Color.red);
      }
    });
    row.add(retention_save);
    panel.add(row);

    row = new Row();
    row.add(new Label("Cleaning Tape Barcode ID:"));
    row.add(new Label("Prefix:"));
    TextField prefix = new TextField(Config.current.cleanPrefix);
    row.add(prefix);
    row.add(new Label("Suffix:"));
    TextField suffix = new TextField(Config.current.cleanSuffix);
    row.add(suffix);
    Button clean_save = new Button("Save");
    clean_save.addClickListener((MouseEvent me, Component c) -> {
      try {
        String pre = prefix.getText();
        String suf = suffix.getText();
        Config.current.cleanPrefix = pre;
        Config.current.cleanSuffix = suf;
        Config.save();
        msg.setText("Updated Cleaning Tape Barcode IDs");
        msg.setColor(Color.black);
      } catch (Exception e) {
        e.printStackTrace();
        msg.setText("Failed to save Cleaning Tape Barcode IDs");
        msg.setColor(Color.red);
      }
    });
    row.add(clean_save);
    panel.add(row);

    row = new Row();
    row.add(new Label("Device Names:"));
    row.add(new Label("Tape:"));
    TextField tape = new TextField(Config.current.tapeDevice);
    row.add(tape);
    row.add(new Label("Changer:"));
    TextField changer = new TextField(Config.current.changerDevice);
    row.add(changer);
    row.add(new Label("(leave blank if not available)"));
    Button devices_save = new Button("Save");
    devices_save.addClickListener((MouseEvent me, Component c) -> {
      try {
        String _tape = tape.getText();
        String _changer = changer.getText();
        Config.current.cleanPrefix = _tape;
        Config.current.cleanSuffix = _changer;
        Config.save();
        msg.setText("Updated Device Names");
        msg.setColor(Color.black);
      } catch (Exception e) {
        e.printStackTrace();
        msg.setText("Failed to save Device Names");
        msg.setColor(Color.red);
      }
    });
    row.add(devices_save);
    panel.add(row);

    return panel;
  }

  private int tapeSize(long blks) {
    return (int)(blks * 64);
  }

  public Panel serverTapes() {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("jfBackup/" + Config.AppVersion);
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    panel.add(new Label("Tapes Status:"));

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(2);
    panel.add(row);

    AutoScrollPanel scroll = new AutoScrollPanel();
    scroll.setMaxWidth();
    scroll.setMaxHeight();
    panel.add(scroll);

    for(EntryTape tape : Tapes.current.tapes) {
      row = new Row();
      row.add(new Label("Tape:" + tape.barcode + " used in backup " + tape.job + " on date " + toDateTime(tape.backup) + " with retention until " + toDateTime(tape.retention)));
      scroll.add(row);
      row = new Row();
      row.add(new Label("Size(KB):" + tapeSize(tape.capacity) + " Used(KB):" + tapeSize(tape.capacity - tape.left)));
      scroll.add(row);
      row = new Row();
      row.setBackColor(Color.blue);
      row.setHeight(2);
      scroll.add(row);
    }
    if (Tapes.current.tapes.size() == 0) {
      row = new Row();
      row.add(new Label("No tapes under retention"));
      scroll.add(row);
    }
    return panel;
  }

  public Panel serverLogs(String file) {
    Panel panel = new Panel();
    Row row = new Row();
    Label label = new Label("Server Logs");
    row.add(label);
    panel.add(row);

    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);

    if (file == null) {
      AutoScrollPanel scroll = new AutoScrollPanel();
      scroll.setMaxWidth();
      scroll.setMaxHeight();
      panel.add(scroll);
      //list log files (limit 1 year)
      File folder = new File(Paths.logsPath);
      File files[] = folder.listFiles();
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
        if (!name.endsWith(".log")) continue;
        if (!name.startsWith("backup") && !name.startsWith("restore")) continue;
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

  public Panel clientPanel() {
    Panel panel = new Panel();
    //TODO : add options to reset config, disconnect from server, etc.
    panel.add(new Label("jfBackup is running in client mode."));
    panel.add(new Label("There are no options to configure here."));
    panel.add(new Label("Use the server to add volumes to backup jobs."));
    panel.add(new Label("This Host=" + Config.current.this_host));
    return panel;
  }

  private static Object lockInstall = new Object();

  public Panel installPanel() {
    Row row;
    Panel panel = new Panel();
    Button client_next = new Button("Connect");
    Button server_next = new Button("Save");
    panel.add(new Label("jfBackup has not been setup yet, please select client or server mode."));
    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);
    {
      panel.add(new Label("Client Mode Setup:"));
      Label msg = new Label("");
      panel.add(msg);
      row = new Row();
      row.add(new Label("Client Host/IP:"));
      TextField name = new TextField("");
      name.setName("name");
      row.add(name);
      panel.add(row);
      row = new Row();
      row.add(new Label("Server Host/IP:"));
      TextField host = new TextField("");
      host.setName("host");
      row.add(host);
      panel.add(row);
      row = new Row();
      row.add(new Label("Server Password:"));
      TextField pass = new TextField("");
      pass.setName("password");
//      password.setPassword(true);  //login may be blocked - need to create HTTPS server instead
      row.add(pass);
      panel.add(row);
      row = new Row();
      row.add(client_next);
      panel.add(row);
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
              if (BackupService.client != null) {
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
                BackupService.client = client;
                client.start();
                client = null;
                webclient.setProperty("password", passTxt);
                webclient.setPanel(clientPanel());
              }
            }
          }
        }.start();
      });
    }
    row = new Row();
    row.setBackColor(Color.blue);
    row.setHeight(5);
    panel.add(row);
    {
      panel.add(new Label("Server Mode Setup:"));
      Label msg = new Label("");
      msg.setColor(Color.red);
      panel.add(msg);

      row = new Row();
      row.add(new Label("Server Host/IP:"));
      TextField name = new TextField("");
      row.add(name);
      panel.add(row);

      row = new Row();
      row.add(new Label("Server Password:"));
      TextField pass = new TextField("");
      pass.setName("password");
//      password.setPassword(true);  //login may be blocked - need to create HTTPS server
      row.add(pass);
      panel.add(row);

      row = new Row();
      row.add(new Label("Confirm Password:"));
      TextField confirm = new TextField("");
      confirm.setName("password");
//      password.setPassword(true);  //login may be blocked - need to create HTTPS server
      row.add(confirm);
      panel.add(row);

      row = new Row();
      row.add(new Label("Tape Drive:"));
      TextField tape = new TextField("tape0");
      row.add(tape);
      panel.add(row);

      row = new Row();
      row.add(new Label("Media Changer:"));
      TextField changer = new TextField("changer0");
      row.add(changer);
      row.add(new Label(" (leave blank if you don't have one) "));
      panel.add(row);

      //TODO : retention years/months

      row = new Row();
      row.add(server_next);
      panel.add(row);
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
        String tape0 = tape.getText();
        String changer0 = changer.getText();
        serverSaveConfig(nameTxt, passTxt, tape0, changer0);
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

  public void serverSaveConfig(String name, String password, String tape0, String changer0) {
    Config.current.this_host = name;
    Config.current.server_host = name;  //to allow local Client to connect
    Config.current.password = password;
    Config.current.tapeDevice = tape0;
    Config.current.changerDevice = changer0;
    Config.save();
  }

  public void saveConfigMode(String mode) {
    Config.current.mode = mode;
    Config.save();
    switch (mode) {
      case "client": BackupService.startClient(); break;
      case "server": BackupService.startServer(); break;
    }
  }

  public byte[] getResource(String url) {
    return null;
  }

  public void clientConnected(WebUIClient client) {
  }

  public void clientDisconnected(WebUIClient client) {
  }
}

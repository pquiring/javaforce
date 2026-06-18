/** CommandApp
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.jni.WinNative;

public class CommandApp implements ActionListener {

  private static boolean active;

  private static boolean runas1;
  private static boolean runas2;

  private static boolean sudo;

  public static void main(String[] args) {
    JFLog.log("main");
    for(String arg : args) {
      switch (arg) {
        case "--runas1": runas1 = true; break;
        case "--runas2": runas2 = true; break;
        case "--sudo": sudo = true; break;
      }
    }
    new CommandApp().addTrayIcon();
    active = true;
    while (active) {
      JF.sleep(500);
    }
  }

  public static class Action {
    public String name;
    public String cmd;
    public JMenuItem item;
  }

  private void execute(String cmd) {
    JFLog.log("execute:" + cmd);
    try {
      if (false) {
        Runtime.getRuntime().exec(JF.splitQuoted(cmd, ' '));
      } else {
        ProcessBuilder proc = new ProcessBuilder();
        proc.command(JF.splitQuoted(cmd, ' '));
        proc.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == exit) {
      active = false;
      System.exit(0);
    }
    for(Action action : actions) {
      if (o == action.item) {
        execute(action.cmd);
      }
    }
  }

  private String filename = JF.getConfigPath() + "/jfcommand.cfg";

  private void createConfig() {
    StringBuilder cfg = new StringBuilder();
    if (JF.isWindows()) {
      cfg.append("[settings]\n");
      cfg.append("#domain={domain}\n");
      cfg.append("#runas={user:pass}\n");
      cfg.append("[actions]\n");
      cfg.append("command=cmd.exe /c start cmd.exe\n");  //cmd.exe alone runs in current console environment, need to run a second copy to open new console window
      cfg.append("services=mmc.exe services.msc\n");
      cfg.append("diskmgmt=mmc.exe diskmgmt.msc\n");
      cfg.append("virtmgmt=mmc.exe virtmgmt.msc\n");
      cfg.append("regedit=regedit.exe\n");

//these do not work and still present the UAC prompt (control.exe inherits rights from explorer.exe shell) (trying to run explorer.exe with admin rights fails)
//      cfg.append("appwiz=control.exe appwiz.cpl\n");
//      cfg.append("ncpa=control.exe ncpa.cpl\n");
      cfg.append("");
    } else {
      cfg.append("[settings]\n");
      cfg.append("#runas=sudo\n");
      cfg.append("[actions]\n");
      cfg.append("terminal=jfterm\n");
    }
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(cfg.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
    loadConfig(false);
  }

  private static final int NONE = 0;
  private static final int SETTINGS = 1;
  private static final int ACTIONS = 2;

  private String domain = ".";

  private void loadConfig(boolean create) {
    actions = new ArrayList<>();
    try {
      File file = new File(filename);
      if (!file.exists()) {
        if (create) {
          createConfig();
        }
        return;
      }
      FileInputStream fis = new FileInputStream(file);
      String cfg = new String(fis.readAllBytes());
      fis.close();
      int section = NONE;
      String[] lns = cfg.replaceAll("\r", "").split("\n");
      for(String ln : lns) {
        ln = ln.trim();
        if (ln.startsWith("#")) continue;
        if (ln.startsWith("[") && ln.endsWith("]")) {
          //section
          String name = ln.substring(1, ln.length() - 1);
          switch (name.toLowerCase()) {
            case "settings": section = SETTINGS; break;
            case "actions": section = ACTIONS; break;
          }
          continue;
        }
        int idx;
        idx = ln.indexOf("=");
        if (idx == -1) continue;
        String key = ln.substring(0, idx);
        String value = ln.substring(idx + 1);
        switch (section) {
          case SETTINGS:
            switch (key) {
              case "domain":
                domain = value;
                break;
              case "runas":
                if (JF.isWindows()) {
                  if (runas2) break;  //already elevated
                  idx = value.indexOf(":");
                  if (idx == -1) continue;
                  String user = value.substring(0, idx);
                  String pass = value.substring(idx + 1);
                  elevate_win(user, pass);
                } else {
                  if (sudo) break;  //already elevated
                  elevate_lnx(value);
                }
                System.exit(0);
                break;
            }
            break;
          case ACTIONS:
            addAction(key, value);
            break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void addAction(String name, String cmd) {
    Action action = new Action();
    action.name = name;
    action.cmd = cmd;
    actions.add(action);
  }

  private void elevate_win(String user, String pass) {
    if (false) {
      //does not work
      WinNative.impersonateUser(domain, user, pass);
    } else {
      int opts = 0;
      String cmdline;
      if (runas1 == false) {
        //stage 1
        cmdline = "--runas1";
        if (false) {
          //does not work
          WinNative.createProcessAsUser(domain, user, pass, "jfcommand.exe", "jfcommand.exe " + cmdline, opts);
        } else {
          //works
          try {
            Runtime.getRuntime().exec(new String[] {"powershell.exe", "-Command", "\"Start-Process -Credential (New-Object System.Management.Automation.PSCredential('" + user + "', (ConvertTo-SecureString '" + pass + "' -AsPlainText -Force))) -FilePath 'jfcommand.exe' -ArgumentList '--runas1'\""});
          } catch (Exception e) {
            JFLog.log(e);
          }
        }
      } else {
        //stage 2
        cmdline = "--runas2";
        if (false) {
          //does not work
          opts = WinNative.FLAG_ELEVATE;
          WinNative.createProcessAsUser(domain, user, pass, "jfcommand.exe", "jfcommand.exe " + cmdline, opts);
        } else {
          //works
          try {
            Runtime.getRuntime().exec(new String[] {"powershell.exe", "-Command", "\"Start-Process -Verb RunAs -FilePath 'jfcommand.exe' -ArgumentList '--runas2'\""});
          } catch (Exception e) {
            JFLog.log(e);
          }
        }
      }
    }
  }

  private void elevate_lnx(String cmd) {
    try {
      //user must have sudo rights to elevate jfCommand
      Runtime.getRuntime().exec(new String[] {cmd, "jfcommand", "--sudo"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void addTrayIcon() {
    try {
      trayicon = new JFImage();
      trayicon.loadPNG(this.getClass().getClassLoader().getResourceAsStream("jfcommand_tray.png"));
    } catch (Exception e) {
      JFLog.log(e);
    }
    loadConfig(true);
    //create tray icon
    try {
      JPopupMenu popup = new JPopupMenu();
      for(Action action : actions) {
        action.item = new JMenuItem(action.name);
        action.item.addActionListener(this);
        popup.add(action.item);
      }
      popup.addSeparator();
      exit = new JMenuItem("Exit");
      exit.addActionListener(this);
      popup.add(exit);
      icon = new JTrayIcon(trayicon.getImage(), "Command", popup);
      SystemTray.getSystemTray().add(icon);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private JFImage trayicon;
  private JTrayIcon icon;
  private JMenuItem exit;
  private ArrayList<Action> actions;
}

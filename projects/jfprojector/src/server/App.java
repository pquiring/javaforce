package server;

/** Server
 *
 * @author pquiring
 */

import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javaforce.*;
import javaforce.awt.*;

import common.Config;

public class App extends Thread implements ActionListener {
  public static void main(String args[]) {
    App server = new App();
    server.start();
  }

  private ServerSocket ss;
  private boolean active;

  public void run() {
    initTray();
    addTray();
    try {
      ss = new ServerSocket(Config.port);
      active = true;
      JFLog.log("Waiting for connection on port " + Config.port + "...");
      while (active) {
        Socket s = ss.accept();
        Session sess = new Session();
        sess.start(s);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public SystemTray tray;
  public TrayIcon icon;
  public MenuItem stop;

  private void initTray() {
    if (SystemTray.isSupported()) {
      tray = SystemTray.getSystemTray();
      // create a popup menu
      PopupMenu popup = new PopupMenu();
      stop = new MenuItem("Stop");
      stop.addActionListener(this);
      popup.add(stop);
      Dimension size = tray.getTrayIconSize();
      JFImage appicon = new JFImage();
      InputStream is = this.getClass().getResourceAsStream("/jfprojector.png");
      appicon.loadPNG(is);
      try {is.close();} catch (Exception e) {}
      JFImage scaled = new JFImage(size.width, size.height);
      scaled.fill(0, 0, size.width, size.height, 0x00000000, true);  //fill with alpha transparent
      if (true) {
        //scaled image (looks bad sometimes)
        scaled.getGraphics().drawImage(appicon.getImage()
          , 0, 0, size.width, size.height
          , 0, 0, appicon.getIconWidth(), appicon.getIconHeight()
          , null);
      } else {
        //center image
        scaled.getGraphics().drawImage(appicon.getImage()
          , (size.width - appicon.getIconWidth()) / 2
          , (size.height - appicon.getIconHeight()) / 2
          , null);
      }
      icon = new TrayIcon(scaled.getImage(), "jfProjector Server", popup);
      icon.addActionListener(this);
    }
  }

  private void addTray() {
    try { tray.add(icon); } catch (Exception e) { JFLog.log(e); }
  }

  public void actionPerformed(ActionEvent e) {
    Object o = e.getSource();
    if (o == stop) {
      System.exit(0);
    }
  }
}

package jffile;

/*
 * FileApp.java
 *
 * Created on Jan 2, 2011, 9:28:14 PM
 * Modified for jffile : Mar 22, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.jni.*;
import javaforce.jbus.*;
import javaforce.utils.*;

public class FileApp extends javax.swing.JFrame implements KeyEventDispatcher, AWTEventListener {

  public static String version = "0.7";

  /**
   * Creates new form FileApp
   */
  public FileApp() {
    initComponents();
    setPosition();
    This = this;
    monitordir.init();
    initDND();
    int rid = Math.abs(new Random().nextInt());
    JFLog.init(JF.getUserPath() + "/.jffile.log", true);
    Settings.loadSettings();
    if (Settings.settings.defaultView == JFileBrowser.VIEW_LIST) List.setSelected(true);
    if (Settings.settings.defaultView == JFileBrowser.VIEW_DETAILS) Details.setSelected(true);
    NetworkShares.loadShares();
    if (!JF.isWindows()) {
      jbusClient = new JBusClient("org.jflinux.jffile.j" + rid, new JBusMethods());
      jbusClient.start();
    }
    localSite = new SiteDetails();
    localSite.host = "localhost";
    localSite.protocol = "local";
    localSite.localDir = (args.length > 0 && args[0].length() > 0
      ? getCanonicalPath(args[0])
      : JF.getUserPath());
    localSite.remoteDir = JF.getUserPath();
    localSite.name = "Computer";
    addLocalSite();
    if (args.length > 0) {
      //reset for next use
      localSite.localDir = JF.getUserPath();
    }
    listPlaces();
    //install app-wide event listeners
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
  }

  private String getCanonicalPath(String path) {
    try {
      return new File(path).getCanonicalPath();
    } catch (Exception e) {
      return "/";
    }
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    placesMenu = new javax.swing.JPopupMenu();
    unmount = new javax.swing.JMenuItem();
    rename = new javax.swing.JMenuItem();
    buttonGroup1 = new javax.swing.ButtonGroup();
    split = new javax.swing.JSplitPane();
    tabs = new javax.swing.JTabbedPane();
    jScrollPane1 = new javax.swing.JScrollPane();
    places = new javax.swing.JList();
    jMenuBar1 = new javax.swing.JMenuBar();
    jMenu1 = new javax.swing.JMenu();
    newtLocalTab = new javax.swing.JMenuItem();
    siteMgr = new javax.swing.JMenuItem();
    jSeparator1 = new javax.swing.JPopupMenu.Separator();
    importSites = new javax.swing.JMenuItem();
    exportSites = new javax.swing.JMenuItem();
    jSeparator3 = new javax.swing.JPopupMenu.Separator();
    closeTab = new javax.swing.JMenuItem();
    jSeparator2 = new javax.swing.JPopupMenu.Separator();
    exit = new javax.swing.JMenuItem();
    jMenu5 = new javax.swing.JMenu();
    cut = new javax.swing.JMenuItem();
    copy = new javax.swing.JMenuItem();
    paste = new javax.swing.JMenuItem();
    jMenu3 = new javax.swing.JMenu();
    showHidden = new javax.swing.JCheckBoxMenuItem();
    jSeparator4 = new javax.swing.JPopupMenu.Separator();
    showLocalTree = new javax.swing.JCheckBoxMenuItem();
    showRemoteSide = new javax.swing.JCheckBoxMenuItem();
    showRemoteTree = new javax.swing.JCheckBoxMenuItem();
    jSeparator5 = new javax.swing.JPopupMenu.Separator();
    Icons = new javax.swing.JRadioButtonMenuItem();
    List = new javax.swing.JRadioButtonMenuItem();
    Details = new javax.swing.JRadioButtonMenuItem();
    jMenu4 = new javax.swing.JMenu();
    connectMapping = new javax.swing.JMenuItem();
    disconnectMapping = new javax.swing.JMenuItem();
    jSeparator6 = new javax.swing.JPopupMenu.Separator();
    drivesMenu = new javax.swing.JMenuItem();
    jSeparator7 = new javax.swing.JPopupMenu.Separator();
    jMenuItem2 = new javax.swing.JMenuItem();
    jSeparator8 = new javax.swing.JPopupMenu.Separator();
    jMenuItem1 = new javax.swing.JMenuItem();
    jMenu2 = new javax.swing.JMenu();
    help = new javax.swing.JMenuItem();
    about = new javax.swing.JMenuItem();

    unmount.setText("unmount");
    unmount.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        unmountActionPerformed(evt);
      }
    });
    placesMenu.add(unmount);

    rename.setText("Rename");
    rename.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        renameActionPerformed(evt);
      }
    });
    placesMenu.add(rename);

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("jffile");
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent evt) {
        formWindowClosing(evt);
      }
    });
    addComponentListener(new java.awt.event.ComponentAdapter() {
      public void componentMoved(java.awt.event.ComponentEvent evt) {
        formComponentMoved(evt);
      }
      public void componentResized(java.awt.event.ComponentEvent evt) {
        formComponentResized(evt);
      }
    });
    addWindowStateListener(new java.awt.event.WindowStateListener() {
      public void windowStateChanged(java.awt.event.WindowEvent evt) {
        formWindowStateChanged(evt);
      }
    });

    split.setResizeWeight(0.1);

    tabs.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        tabsStateChanged(evt);
      }
    });
    split.setRightComponent(tabs);

    places.setModel(model);
    places.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    places.setComponentPopupMenu(placesMenu);
    places.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
      public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
        placesValueChanged(evt);
      }
    });
    jScrollPane1.setViewportView(places);

    split.setLeftComponent(jScrollPane1);

    jMenu1.setMnemonic('F');
    jMenu1.setText("File");

    newtLocalTab.setText("New Local Tab");
    newtLocalTab.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        newtLocalTabActionPerformed(evt);
      }
    });
    jMenu1.add(newtLocalTab);

    siteMgr.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
    siteMgr.setMnemonic('S');
    siteMgr.setText("Site Manager");
    siteMgr.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        siteMgrActionPerformed(evt);
      }
    });
    jMenu1.add(siteMgr);
    jMenu1.add(jSeparator1);

    importSites.setMnemonic('I');
    importSites.setText("Import Sites...");
    importSites.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        importSitesActionPerformed(evt);
      }
    });
    jMenu1.add(importSites);

    exportSites.setMnemonic('E');
    exportSites.setText("Export Sites...");
    exportSites.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        exportSitesActionPerformed(evt);
      }
    });
    jMenu1.add(exportSites);
    jMenu1.add(jSeparator3);

    closeTab.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_DOWN_MASK));
    closeTab.setMnemonic('C');
    closeTab.setText("Close Site");
    closeTab.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        closeTabActionPerformed(evt);
      }
    });
    jMenu1.add(closeTab);
    jMenu1.add(jSeparator2);

    exit.setMnemonic('X');
    exit.setText("Exit");
    exit.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        exitActionPerformed(evt);
      }
    });
    jMenu1.add(exit);

    jMenuBar1.add(jMenu1);

    jMenu5.setMnemonic('e');
    jMenu5.setText("Edit");
    jMenu5.setToolTipText("");

    cut.setText("Cut");
    cut.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cutActionPerformed(evt);
      }
    });
    jMenu5.add(cut);

    copy.setText("Copy");
    copy.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        copyActionPerformed(evt);
      }
    });
    jMenu5.add(copy);

    paste.setText("Paste");
    paste.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        pasteActionPerformed(evt);
      }
    });
    jMenu5.add(paste);

    jMenuBar1.add(jMenu5);

    jMenu3.setMnemonic('V');
    jMenu3.setText("View");

    showHidden.setSelected(true);
    showHidden.setText("Show Hidden Files");
    showHidden.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showHiddenActionPerformed(evt);
      }
    });
    jMenu3.add(showHidden);
    jMenu3.add(jSeparator4);

    showLocalTree.setSelected(true);
    showLocalTree.setText("Local Tree");
    showLocalTree.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showLocalTreeActionPerformed(evt);
      }
    });
    jMenu3.add(showLocalTree);

    showRemoteSide.setSelected(true);
    showRemoteSide.setText("Remote Side");
    showRemoteSide.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showRemoteSideActionPerformed(evt);
      }
    });
    jMenu3.add(showRemoteSide);

    showRemoteTree.setSelected(true);
    showRemoteTree.setText("Remote Tree");
    showRemoteTree.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        showRemoteTreeActionPerformed(evt);
      }
    });
    jMenu3.add(showRemoteTree);
    jMenu3.add(jSeparator5);

    buttonGroup1.add(Icons);
    Icons.setSelected(true);
    Icons.setText("Icons");
    Icons.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        IconsActionPerformed(evt);
      }
    });
    jMenu3.add(Icons);

    buttonGroup1.add(List);
    List.setText("List");
    List.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        ListActionPerformed(evt);
      }
    });
    jMenu3.add(List);

    buttonGroup1.add(Details);
    Details.setText("Details");
    Details.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        DetailsActionPerformed(evt);
      }
    });
    jMenu3.add(Details);

    jMenuBar1.add(jMenu3);

    jMenu4.setMnemonic('t');
    jMenu4.setText("Tools");

    connectMapping.setText("Map Network Share");
    connectMapping.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        connectMappingActionPerformed(evt);
      }
    });
    jMenu4.add(connectMapping);

    disconnectMapping.setText("Disconnect Network Share");
    disconnectMapping.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        disconnectMappingActionPerformed(evt);
      }
    });
    jMenu4.add(disconnectMapping);
    jMenu4.add(jSeparator6);

    drivesMenu.setText("Drives");
    drivesMenu.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        drivesMenuActionPerformed(evt);
      }
    });
    jMenu4.add(drivesMenu);
    jMenu4.add(jSeparator7);

    jMenuItem2.setText("Connect to Server");
    jMenuItem2.setToolTipText("");
    jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem2ActionPerformed(evt);
      }
    });
    jMenu4.add(jMenuItem2);
    jMenu4.add(jSeparator8);

    jMenuItem1.setText("Settings");
    jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem1ActionPerformed(evt);
      }
    });
    jMenu4.add(jMenuItem1);

    jMenuBar1.add(jMenu4);

    jMenu2.setMnemonic('H');
    jMenu2.setText("Help");

    help.setText("Help");
    help.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        helpActionPerformed(evt);
      }
    });
    jMenu2.add(help);

    about.setMnemonic('A');
    about.setText("About");
    about.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        aboutActionPerformed(evt);
      }
    });
    jMenu2.add(about);

    jMenuBar1.add(jMenu2);

    setJMenuBar(jMenuBar1);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(split, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(split, javax.swing.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void siteMgrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_siteMgrActionPerformed
      inDialog = true;
      SiteDetails sdArray[] = SiteMgr.showSiteMgr(this);
      inDialog = false;
      if (sdArray == null) {
        return;
      }
      for (SiteDetails sd : sdArray) {
        connect(sd);
      }
    }//GEN-LAST:event_siteMgrActionPerformed

    private void importSitesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importSitesActionPerformed
      Settings.importSettings();
    }//GEN-LAST:event_importSitesActionPerformed

    private void exportSitesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportSitesActionPerformed
      Settings.exportSettings();
    }//GEN-LAST:event_exportSitesActionPerformed

    private void aboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutActionPerformed
      inDialog = true;
      JFAWT.showMessage("About", "jffile/" + version + "\nFile manager.\nBy : Peter Quiring(pquiring@jflinux.org)");
      inDialog = false;
    }//GEN-LAST:event_aboutActionPerformed

    private void exitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitActionPerformed

      exit();
    }//GEN-LAST:event_exitActionPerformed

    private void closeTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeTabActionPerformed
      closeSite();
    }//GEN-LAST:event_closeTabActionPerformed

  private void placesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_placesValueChanged
    gotoPlaces();
  }//GEN-LAST:event_placesValueChanged

  private void showLocalTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showLocalTreeActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.showLocalTree = showLocalTree.isSelected();
    site.buildGUI();
  }//GEN-LAST:event_showLocalTreeActionPerformed

  private void showRemoteSideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showRemoteSideActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.showRemoteSide = showRemoteSide.isSelected();
    site.buildGUI();
  }//GEN-LAST:event_showRemoteSideActionPerformed

  private void showRemoteTreeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showRemoteTreeActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.showRemoteTree = showRemoteTree.isSelected();
    site.buildGUI();
  }//GEN-LAST:event_showRemoteTreeActionPerformed

  private void tabsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabsStateChanged
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    showLocalTree.setSelected(site.showLocalTree);
    showRemoteSide.setSelected(site.showRemoteSide);
    showRemoteTree.setSelected(site.showRemoteTree);
    showHidden.setSelected(site.showHidden);
    Icons.setSelected(site.view == JFileBrowser.VIEW_ICONS);
    List.setSelected(site.view == JFileBrowser.VIEW_LIST);
    Details.setSelected(site.view == JFileBrowser.VIEW_DETAILS);
  }//GEN-LAST:event_tabsStateChanged

  private void unmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unmountActionPerformed
    int idx = places.getSelectedIndex();
    if (idx <= 8) return;
    String path = placesPath.get(idx);
    if (!path.startsWith("/media/")) return;
    JFLog.log("umount: " + path);
    jbusClient.call("org.jflinux.jfsystemmgr", "umount", "\"" + path + "\"");
  }//GEN-LAST:event_unmountActionPerformed

  private void showHiddenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showHiddenActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.showHidden = showHidden.isSelected();
    site.local_ls();
    site.remote_ls();
  }//GEN-LAST:event_showHiddenActionPerformed

  private void newtLocalTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newtLocalTabActionPerformed
    addLocalSite();
  }//GEN-LAST:event_newtLocalTabActionPerformed

  private void helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpActionPerformed
    try {
      Runtime.getRuntime().exec(new String[] {"jfhelp", "jffile"});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }//GEN-LAST:event_helpActionPerformed

  private void renameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameActionPerformed
    int idx = places.getSelectedIndex();
    if (idx <= 8) return;
    String path = placesPath.get(idx);
    if (!path.startsWith("/media/"));
    renameDevice(path.substring(path.lastIndexOf('/')+1));
    listPlaces();
  }//GEN-LAST:event_renameActionPerformed

  private void connectMappingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectMappingActionPerformed
    FileApp.inDialog = true;
    MapNetworkShareDialog dialog = new MapNetworkShareDialog(null, true);
    dialog.setVisible(true);
    FileApp.inDialog = false;
    listPlaces();
  }//GEN-LAST:event_connectMappingActionPerformed

  private void disconnectMappingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnectMappingActionPerformed
    FileApp.inDialog = true;
    UnmapNetworkShareDialog dialog = new UnmapNetworkShareDialog(null, true);
    dialog.setVisible(true);
    FileApp.inDialog = false;
    listPlaces();
  }//GEN-LAST:event_disconnectMappingActionPerformed

  private void formComponentMoved(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentMoved
    if (Settings.settings.bWindowMax) return;
    Point loc = getLocation();
    Settings.settings.WindowXPos = loc.x;
    Settings.settings.WindowYPos = loc.y;
  }//GEN-LAST:event_formComponentMoved

  private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
    if (Settings.settings.bWindowMax) return;
    Dimension size = getSize();
    Settings.settings.WindowXSize = size.width;
    Settings.settings.WindowYSize = size.height;
  }//GEN-LAST:event_formComponentResized

  private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged
    Settings.settings.bWindowMax = evt.getNewState() == MAXIMIZED_BOTH;
  }//GEN-LAST:event_formWindowStateChanged

  private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    Settings.saveSettings();
    if (jbusClient != null) jbusClient.close();
    closeTabs();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
    Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    System.exit(0);
  }//GEN-LAST:event_formWindowClosing

  private void IconsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IconsActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.setView(JFileBrowser.VIEW_ICONS);
  }//GEN-LAST:event_IconsActionPerformed

  private void ListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ListActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.setView(JFileBrowser.VIEW_LIST);
  }//GEN-LAST:event_ListActionPerformed

  private void DetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DetailsActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.setView(JFileBrowser.VIEW_DETAILS);
  }//GEN-LAST:event_DetailsActionPerformed

  private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
    EditSettings dialog = new EditSettings(null, true);
    inDialog = true;
    dialog.setVisible(true);
    inDialog = false;
  }//GEN-LAST:event_jMenuItem1ActionPerformed

  private void drivesMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_drivesMenuActionPerformed
    drives = new Drives(null, true, jbusClient);
    inDialog = true;
    drives.setVisible(true);
    inDialog = false;
    drives = null;
  }//GEN-LAST:event_drivesMenuActionPerformed

  private void copyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    if (!site.isFocusBrowser()) return;
    site.getFocusBrowser().copy();
  }//GEN-LAST:event_copyActionPerformed

  private void cutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    if (!site.isFocusBrowser()) return;
    site.getFocusBrowser().cut();
  }//GEN-LAST:event_cutActionPerformed

  private void pasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteActionPerformed
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    if (!site.isFocusBrowser()) return;
    site.getFocusBrowser().paste();
  }//GEN-LAST:event_pasteActionPerformed

  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
    quick_connect();
  }//GEN-LAST:event_jMenuItem2ActionPerformed

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    if (System.getenv("JID") == null && !JF.isWindows()) {
      //avoid future headaches
      JFAWT.showError("Error", "Failed to connect to JBus");
      return;
    }
    if (args == null) args = new String[0];
    FileApp.args = args;
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new FileApp().setVisible(true);
      }
    });
  }
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JRadioButtonMenuItem Details;
  private javax.swing.JRadioButtonMenuItem Icons;
  private javax.swing.JRadioButtonMenuItem List;
  private javax.swing.JMenuItem about;
  private javax.swing.ButtonGroup buttonGroup1;
  private javax.swing.JMenuItem closeTab;
  private javax.swing.JMenuItem connectMapping;
  private javax.swing.JMenuItem copy;
  private javax.swing.JMenuItem cut;
  private javax.swing.JMenuItem disconnectMapping;
  private javax.swing.JMenuItem drivesMenu;
  private javax.swing.JMenuItem exit;
  private javax.swing.JMenuItem exportSites;
  private javax.swing.JMenuItem help;
  private javax.swing.JMenuItem importSites;
  private javax.swing.JMenu jMenu1;
  private javax.swing.JMenu jMenu2;
  private javax.swing.JMenu jMenu3;
  private javax.swing.JMenu jMenu4;
  private javax.swing.JMenu jMenu5;
  private javax.swing.JMenuBar jMenuBar1;
  private javax.swing.JMenuItem jMenuItem1;
  private javax.swing.JMenuItem jMenuItem2;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JPopupMenu.Separator jSeparator1;
  private javax.swing.JPopupMenu.Separator jSeparator2;
  private javax.swing.JPopupMenu.Separator jSeparator3;
  private javax.swing.JPopupMenu.Separator jSeparator4;
  private javax.swing.JPopupMenu.Separator jSeparator5;
  private javax.swing.JPopupMenu.Separator jSeparator6;
  private javax.swing.JPopupMenu.Separator jSeparator7;
  private javax.swing.JPopupMenu.Separator jSeparator8;
  private javax.swing.JMenuItem newtLocalTab;
  private javax.swing.JMenuItem paste;
  private javax.swing.JList places;
  private javax.swing.JPopupMenu placesMenu;
  private javax.swing.JMenuItem rename;
  private javax.swing.JCheckBoxMenuItem showHidden;
  private javax.swing.JCheckBoxMenuItem showLocalTree;
  private javax.swing.JCheckBoxMenuItem showRemoteSide;
  private javax.swing.JCheckBoxMenuItem showRemoteTree;
  private javax.swing.JMenuItem siteMgr;
  private javax.swing.JSplitPane split;
  private javax.swing.JTabbedPane tabs;
  private javax.swing.JMenuItem unmount;
  // End of variables declaration//GEN-END:variables

  private SiteDetails localSite;
  private static String[] args;
  private DefaultListModel model = new DefaultListModel();
  public static JBusClient jbusClient;
  public static FileApp This;
  private Drives drives;

  public void connect(SiteDetails sd) {
    File localDir = new File(sd.localDir);
    if (!localDir.exists() || !localDir.isDirectory()) {
      JFAWT.showError("Error", "Can not find:" + sd.localDir);
      return;
    }
    Site site = null;
    site = new Site();
    site.putClientProperty("tabs", tabs);
    tabs.addTab(sd.name, site);
    tabs.doLayout();
    site.init(sd.localDir, sd.protocol.equals("local"));
    tabs.setSelectedComponent(site);
    JFTask task = new JFTask() {
      private Site site;
      private SiteDetails sd;
      public boolean work() {
        site = (Site)this.getProperty("site");
        sd = (SiteDetails)this.getProperty("sd");
        java.awt.EventQueue.invokeLater(new Runnable() {
          public void run() {
            if (!site.connect(sd)) {
              closeSite(site);
              JFAWT.showError("Error", "Failed to connect");
            }
          }
        });
        return true;
      }
    };
    task.setProperty("site", site);
    task.setProperty("sd", sd);
    task.start();
    site.requestFocus();
  }

  public void exit() {
    dispose();
  }

  public void closeSite() {
    try {
      Site site = (Site) tabs.getSelectedComponent();
      if (site == null) {
        return;
      }
      site.disconnect();
      tabs.remove(site);
      if (tabs.getTabCount() == 0) {
        addLocalSite();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void closeSite(Site site) {
    try {
      site.disconnect();
      tabs.remove(site);
      if (tabs.getTabCount() == 0) {
        addLocalSite();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void addLocalSite() {
    connect(localSite);
    listPlaces();
  }

  private ArrayList<String> placesPath = new ArrayList<String>();

  public void listPlaces() {
    String home = JF.getUserPath();
    model.clear();
    placesPath.clear();
    model.addElement("Home");
    placesPath.add(home);
    model.addElement("Desktop");
    placesPath.add(home + "/Desktop");
    model.addElement("Documents");
    placesPath.add(home + "/Documents");
    model.addElement("Downloads");
    placesPath.add(home + "/Downloads");
    model.addElement("Music");
    placesPath.add(home + "/Music");
    model.addElement("Pictures");
    placesPath.add(home + "/Pictures");
    model.addElement("Videos");
    placesPath.add(home + "/Videos");
    model.addElement("File System");
    placesPath.add("/");
    model.addElement("Trash");
    placesPath.add(home + "/.local/share/Trash");
    File file = new File("/media");
    if (file.exists()) {
      File media[] = file.listFiles();
      for(int a=0;a<media.length;a++) {
        model.addElement(media[a].getName());
        placesPath.add(media[a].getAbsolutePath());
      }
    }
    Mappings.loadMaps();
    Mappings.Maps maps = Mappings.getMaps();
    for(int a=0;a<maps.map.length;a++) {
      String mount = maps.map[a].mount;
      String uri = maps.map[a].uri;
      int idx = uri.lastIndexOf('/');
      if (idx == -1) idx = 0; else idx++;
      model.addElement(uri.substring(idx));
      placesPath.add(mount);
    }
  }

  private void gotoPlaces() {
    int idx = places.getSelectedIndex();
    if (idx == -1) return;
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    site.local_chdir(placesPath.get(idx));
  }

  private void delete() {
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    JComponent focus = (JComponent)getFocusOwner();
    String focusName = focus.getName();
    if (focusName == null) return;
    if (focusName.equals("localFiles")) {
      site.local_delete();
    } else if (focusName.equals("remoteFiles")) {
      site.remote_delete();
    }
  }

  private void rename() {
    int idx = tabs.getSelectedIndex();
    System.out.println("rename:" + idx);
    if (idx == -1) return;
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    JComponent focus = (JComponent)getFocusOwner();
    System.out.println("focus=" + focus.getName());
    String focusName = focus.getName();
    if (focusName == null) return;
    if (focusName.equals("localFiles")) {
      site.local_rename();
    } else if (focusName.equals("remoteFiles")) {
      site.remote_rename();
    }
  }

  private void open() {
    int idx = tabs.getSelectedIndex();
    if (idx == -1) return;
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    JComponent focus = (JComponent)getFocusOwner();
    String focusName = focus.getName();
    if (focusName == null) return;
    if (focusName.equals("localFiles")) {
      site.local_open("open");
    } else if (focusName.equals("remoteFiles")) {
      site.remote_open("open");
    }
  }

  SearchWindow searchWindow;
  String searchString;

  private void search(char ch) {
    if ((ch == 8) && ((searchString == null) || (searchString.length() == 0))) return;
    int idx = tabs.getSelectedIndex();
    if (idx == -1) return;
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    JComponent focus = (JComponent)getFocusOwner();
    if (focus == null) return;
    String focusName = focus.getName();
    if (focusName == null) return;
    if (searchWindow == null) {
      Point p = null;
      if (focusName.equals("localFiles")) {
        p = site.getLocalLocation();
      } else if (focusName.equals("remoteFiles")) {
        p = site.getRemoteLocation();
      }
      if (p == null) return;
      searchWindow = new SearchWindow(null);
      searchWindow.setLocation(p);
      searchWindow.setVisible(true);
    }
//System.out.println("search:"+(int)ch);
    if (ch == 8) {
      searchString = searchString.substring(0, searchString.length()-1);
    } else {
      if (searchString == null) searchString = "" + ch; else searchString += ch;
    }
    searchWindow.setText(searchString);
    boolean found = false;
    if (focusName.equals("localFiles")) {
      found = site.searchLocal(searchString.toLowerCase());
    } else if (focusName.equals("remoteFiles")) {
      found = site.searchRemote(searchString.toLowerCase());
    }
    searchWindow.setColor(found ? Color.white : Color.red);
  }

  private void hideSearch() {
    if (searchWindow == null) return;
//System.out.println("hide");
    searchWindow.setVisible(false);
    searchWindow.dispose();
    searchWindow = null;
    searchString = null;
  }

  public static boolean inDialog = false;

  public boolean dispatchKeyEvent(KeyEvent e) {
    if (inDialog) return false;
    int id = e.getID();
    char ch = e.getKeyChar();
    int cc = e.getKeyCode();
    int mod = e.getModifiersEx() & JFAWT.KEY_MASKS;
//    JFLog.log("keyEvent:" + mod + "," + (char)cc + "," + getFocusOwner());
    if (mod == KeyEvent.CTRL_DOWN_MASK) {
      switch (id) {
        case KeyEvent.KEY_TYPED:
          switch (cc) {
            case KeyEvent.VK_L: quick_connect(); break;
          }
          break;
        case KeyEvent.KEY_PRESSED:
          break;
        case KeyEvent.KEY_RELEASED:
          break;
      }
    }
    if (mod != 0) return false;
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return false;
    if (!site.isFocusBrowser()) return false;
    switch (id) {
      case KeyEvent.KEY_TYPED:
        if ((ch >= KeyEvent.VK_SPACE) && (ch != KeyEvent.VK_DELETE)) search(ch);
        if (ch == KeyEvent.VK_BACK_SPACE) search(ch);
        break;
      case KeyEvent.KEY_PRESSED:
        switch (cc) {
          case KeyEvent.VK_DELETE: delete(); break;
          case KeyEvent.VK_F2: rename(); break;
          case KeyEvent.VK_ESCAPE: hideSearch(); break;
          case KeyEvent.VK_ENTER: hideSearch(); open(); break;
          case KeyEvent.VK_BACK_SPACE: cdup(); break;
        }
        break;
      case KeyEvent.KEY_RELEASED:
        break;
    }
    return false;
  }

  public void eventDispatched(AWTEvent event) {
    //hide search on any mouse events
    hideSearch();
  }

  private void setPosition() {
    setSize(Settings.settings.WindowXSize, Settings.settings.WindowYSize);
    setLocation(Settings.settings.WindowXPos, Settings.settings.WindowYPos);
    if (Settings.settings.bWindowMax) setExtendedState(MAXIMIZED_BOTH);
  }

  private void renameDevice(String name) {
    inDialog = true;
    String newName = JFAWT.getString("Enter a new name", name);
    inDialog = false;
    if (newName == null) return;
    jbusClient.call("org.jflinux.jfsystemmgr", "renameDevice", "\"" + name + "\",\"" + newName + "\"");
  }

  private void closeTabs() {
    int cnt = tabs.getComponentCount();
    for(int a=0;a<cnt;a++) {
      Site site = (Site)tabs.getComponentAt(a);
      site.disconnect();
    }
  }

  private void initDND() {
    places.setTransferHandler(new TransferHandler() {
      public boolean canImport(TransferHandler.TransferSupport info) {
        // we only import Files
        if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          return false;
        }

//        DropLocation dl = (DropLocation) info.getDropLocation();
//        Point pt = dl.getDropPoint();

        return true;
      }

      public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
          return false;
        }

        // Check for file flavor
        if (!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          return false;
        }

//        DropLocation dl = info.getDropLocation();
//        Point pt = dl.getDropPoint();
//        JComponent c = (JComponent)places.getComponentAt(pt);

        int idx = places.getSelectedIndex();
        if (idx == -1) return false;
        String folder = placesPath.get(idx);

        // Get the file(s) that are being dropped.
        Transferable t = info.getTransferable();
        java.util.List<File> data;
        try {
          data = (java.util.List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception e) {
          return false;
        }

        // Perform the actual import.
        ArrayList<String> cmd = new ArrayList<String>();
        boolean move = false;
        boolean copy = false;
        String fn;
        for(int a=0;a<data.size();a++) {
          switch (info.getDropAction()) {
            case COPY:
              if (move) return false;  //Can that happen?
              copy = true;
              fn = ((File)data.get(a)).getAbsolutePath();
              cmd.add(fn);
              break;
            case MOVE:
              if (copy) return false;  //Can that happen?
              move = true;
              cmd.add(((File)data.get(a)).getAbsolutePath());
              break;
            case LINK:
              return false;  //BUG : not supported : ???
          }
        }
        if (cmd.isEmpty()) return false;
        if (copy) {
          cmd.add(0, "jfcp");
        } else if (move) {
          cmd.add(0, "jfmv");
        } else {
          return false;
        }
        cmd.add(folder);
        JFileBrowser browser = null;
        Site site = (Site)tabs.getSelectedComponent();
        if (site != null) {
          browser = site.localBrowser;
        }
        JFileBrowser.runCmd(browser, cmd.toArray(new String[0]));
        return true;
      }

      public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
      }

      protected Transferable createTransferable(JComponent c) {
        return null;
      }

      protected void exportDone(JComponent source, Transferable data, int action) {
      }
    });
  }

  private void cdup() {
    Site site = (Site)tabs.getSelectedComponent();
    if (site == null) return;
    if (site.getFocusBrowser() == site.remoteBrowser) {
      site.remote_cdup();
    } else {
      site.local_cdup();
    }
  }

  private void quick_connect() {
    QuickConnect dialog = new QuickConnect(this, true);
    dialog.setVisible(true);
    if (!dialog.accepted) return;
    SiteDetails sd = dialog.getDetails();
    connect(sd);
  }

  public class JBusMethods {
    public void rescanMedia() {
      //a request to rename a device label has succeeded
      //or umount() succeeded
      //or autoMounter mounted something
      //etc.
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          listPlaces();
          if (drives != null) {
            drives.rescan();
          }
          for(int a=0;a<tabs.getTabCount();a++) {
            Site site = (Site)tabs.getTabComponentAt(a);
            site.rescan();
          }
        }
      });
    }
    public void storageInfo(String dev, String volName, String fsType, String mountPt) {
      if (drives == null) return;
      drives.storageInfo(dev, volName, fsType, mountPt);
    }
    public void getFileSelection(String fs) {
      //call paste in JFileBrowser
      if (fs == null) return;
      int idx = tabs.getSelectedIndex();
      if (idx == -1) return;
      Site site = (Site)tabs.getSelectedComponent();
      if (site == null) return;
      site.getFocusBrowser().paste(JBusClient.decodeString(fs));
    }
  }
}

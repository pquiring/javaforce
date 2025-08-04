/**
 *
 * @author pquiring
 *
 * Created : Oct 10, 2013
 */

import java.awt.*;
import javax.swing.*;

import javaforce.awt.*;
import javaforce.ansi.client.*;

public class Menu {
  private static JTabbedPane tabs;
  public static void create(JFrame frame, JTabbedPane tabs, boolean local) {
    Menu.tabs = tabs;
    JMenuBar menu = create(local, true);
    frame.setJMenuBar(menu);
  }
  private static JMenuBar create(boolean local, boolean exit) {
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem item;

    menuBar = new JMenuBar();

    menu = new JMenu("File");
    menu.setMnemonic('F');
    menuBar.add(menu);

    if (local) {

      item = new JMenuItem("New Local Tab");
      item.setMnemonic('N');
      item.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
          localAction();
        }
      });
      menu.add(item);

      menu.add(new JSeparator());

    }

    item = new JMenuItem("Site Manager");
    item.setMnemonic('S');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        Profile[] sdArray = SiteMgr.showSiteMgr(null);
        if (sdArray == null) return;
        for(Profile profile : sdArray) connect(profile);
      }
    });
    menu.add(item);

    item = new JMenuItem("Close Tab");
    item.setMnemonic('C');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (tabs.getTabCount() == 0) return;
        BufferViewer viewer = (BufferViewer)((JComponent)tabs.getSelectedComponent()).getClientProperty("buffer");;
        if (viewer == null) return;
        viewer.close();
      }
    });
    menu.add(item);

    item = new JMenuItem("Log...");
    item.setMnemonic('L');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (tabs.getTabCount() == 0) return;
        BufferViewer viewer = (BufferViewer)((JComponent)tabs.getSelectedComponent()).getClientProperty("buffer");;
        if (viewer == null) return;
        viewer.logFile();
      }
    });
    menu.add(item);

    if (exit) {

      menu.add(new JSeparator());

      item = new JMenuItem("Exit");
      item.setMnemonic('X');
      item.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
          exit();
        }
      });
      menu.add(item);

    }

    menu = new JMenu("Edit");
    menu.setMnemonic('E');
    menuBar.add(menu);

    item = new JMenuItem("Copy");
    item.setMnemonic('C');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (tabs.getTabCount() == 0) return;
        BufferViewer viewer = (BufferViewer)((JComponent)tabs.getSelectedComponent()).getClientProperty("buffer");
        if (viewer == null) return;
        viewer.copy();
      }
    });
    menu.add(item);

    item = new JMenuItem("Paste");
    item.setMnemonic('P');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (tabs.getTabCount() == 0) return;
        BufferViewer viewer = (BufferViewer)((JComponent)tabs.getSelectedComponent()).getClientProperty("buffer");;
        if (viewer == null) return;
        viewer.paste();
      }
    });
    menu.add(item);

    menu.add(new JSeparator());

    item = new JMenuItem("Settings");
    item.setMnemonic('T');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        BufferViewer viewer;
        int oldFontSize = Settings.settings.fontSize;
        int oldFontX = Settings.settings.fontWidth;
        int oldFontY = Settings.settings.fontHeight;
        int oldFontDescent = Settings.settings.fontDescent;
        int oldScrollBack = Settings.settings.scrollBack;
        EditSettings.editSettings(null);
        boolean changeFont =
          oldFontSize != Settings.settings.fontSize ||
          oldFontX != Settings.settings.fontWidth ||
          oldFontY != Settings.settings.fontHeight ||
          oldFontDescent != Settings.settings.fontDescent;
        for(int a=0;a<tabs.getTabCount();a++) {
          viewer = (BufferViewer)((JComponent)tabs.getComponentAt(a)).getClientProperty("buffer");
          if (oldScrollBack != Settings.settings.scrollBack) viewer.changeScrollBack(Settings.settings.scrollBack);
          if (changeFont) {
            viewer.changeFont();
            viewer.reSize();
          }
        }
      }
    });
    menu.add(item);

    menu = new JMenu("Tools");
    menu.setMnemonic('T');
    menuBar.add(menu);

    item = new JMenuItem("Run Script...");
    item.setMnemonic('R');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (tabs.getTabCount() == 0) return;
        BufferViewer viewer = (BufferViewer)((JComponent)tabs.getSelectedComponent()).getClientProperty("buffer");;
        if (viewer == null) return;
        if (viewer.buffer.script != null) {
          JFAWT.showError("Error", "Another script is already running");
          return;
        }
        viewer.buffer.script = Script.load(viewer);
      }
    });
    menu.add(item);

    item = new JMenuItem("Stop Script");
    item.setMnemonic('S');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (tabs.getTabCount() == 0) return;
        BufferViewer viewer = (BufferViewer)((JComponent)tabs.getSelectedComponent()).getClientProperty("buffer");;
        if (viewer == null) return;
        viewer.buffer.script = null;
      }
    });
    menu.add(item);

    menu = new JMenu("Help");
    menu.setMnemonic('H');
    menuBar.add(menu);

    item = new JMenuItem("Keyboard");
    item.setMnemonic('K');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JFAWT.showMessage("Keyboard",
          "CTRL-W = Close Tab\n" +
          "CTRL-A = Select All\n" +
          "F5/PAUSE = Break\n" +
          "SHIFT + F1-F12 = F13-F24\n" +
          "ALT-HOME = Clear Screen\n" +
          "SHIFT-INSERT = Paste\n" +
          "CTRL-INSERT = Copy"
        );
      }
    });
    menu.add(item);

    menu.add(new JSeparator());

    item = new JMenuItem("Online");
    item.setMnemonic('O');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JFAWT.openURL("https://pquiring.github.io/javaforce/projects/jfterm/docs/help.html");
      }
    });
    menu.add(item);

    item = new JMenuItem("About");
    item.setMnemonic('A');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JFAWT.showMessage("About", "jfTerm/" + TermApp.version + "\nWebSite : jfterm.sourceforge.net\nAuthor : Peter Quiring");
        System.gc();
      }
    });
    menu.add(item);



    return menuBar;
  }

  public static void connect(Profile profile) {

    //apply settings to profile
    profile.foreColor = Settings.settings.foreColor.getRGB();
    profile.backColor = Settings.settings.backColor.getRGB();
    profile.selectColor = Settings.settings.selectColor.getRGB();
    profile.fnt = Settings.fnt;

    BufferViewer viewer = new BufferViewer(profile) {
      public void close() {
        super.close();
        tabs.remove((JComponent)getClientProperty("panel"));
      }
      public void nextTab() {
        int cnt = tabs.getTabCount();
        int idx = tabs.getSelectedIndex();
        idx++;
        if (idx >= cnt) idx = 0;
        tabs.setSelectedIndex(idx);
      }
      public void prevTab() {
        int cnt = tabs.getTabCount();
        int idx = tabs.getSelectedIndex();
        idx--;
        if (idx == -1) idx = cnt-1;
        tabs.setSelectedIndex(idx);
      }
      public void setTab(int idx) {
        int cnt = tabs.getTabCount();
        if (idx > cnt) return;
        tabs.setSelectedIndex(idx);
      }
      public void setName(String str) {
        int idx = tabs.getSelectedIndex();
        tabs.setTitleAt(idx, str);
      }
    };

    JScrollPane pane = new JScrollPane(viewer);
    pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    JPanel panel = new JPanel(new GridLayout());
    panel.putClientProperty("buffer", viewer);
    viewer.putClientProperty("panel", panel);
    viewer.putClientProperty("pane", pane);
    panel.add(pane);
    tabs.addTab(profile.name, panel);
    tabs.setSelectedComponent(panel);
    tabs.revalidate();
  }

  public static void exit() {
    Settings.saveSettings();
    System.exit(0);
  }

  public static void localAction() {
    Profile profile = new Profile();
    profile.autoSize = true;
    profile.protocol = "local";
    profile.termType = "xterm";
    profile.name = "localhost";
    profile.utf8 = true;
    profile.termApp = "/usr/bin/bash";
    profile.termArgs = new String[] {profile.termApp, "-i", "-l"};
    connect(profile);
  }
}

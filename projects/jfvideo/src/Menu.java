/**
 *
 * @author pquiring
 *
 * Created : Oct 10, 2013
 */

import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class Menu {
  private static MainPanel panel;
  public static void create(JFrame frame, MainPanel panel) {
    Menu.panel = panel;
    JMenuBar menu = create();
    frame.setJMenuBar(menu);
  }
  private static JMenuBar create() {
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem item;

    menuBar = new JMenuBar();

    menu = new JMenu("File");
    menuBar.add(menu);

    item = new JMenuItem("New Project");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.newProject();
      }
    });
    menu.add(item);

    item = new JMenuItem("Open Project");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.open();
      }
    });
    menu.add(item);

    item = new JMenuItem("Save Project");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.save();
      }
    });
    menu.add(item);

    item = new JMenuItem("Close Project");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.close();
      }
    });
    menu.add(item);

    menu = new JMenu("Project");
    menuBar.add(menu);

    item = new JMenuItem("Render");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.render();
      }
    });
    menu.add(item);

    item = new JMenuItem("Properties");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.props();
      }
    });
    menu.add(item);

    menu = new JMenu("Library");
    menuBar.add(menu);

    item = new JMenuItem("Add Folder");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.addFolder();
      }
    });
    menu.add(item);

    item = new JMenuItem("Rescan");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.reScan();
      }
    });
    menu.add(item);

    menu = new JMenu("Help");
    menuBar.add(menu);

    item = new JMenuItem("Content");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JFAWT.openURL("http://jfvideo.sourceforge.net/help.php");
      }
    });
    menu.add(item);

    item = new JMenuItem("About");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JFAWT.showMessage("About", "jfVideo Creator/" + VideoApp.version + "\nWebSite : http://jfvideo.sourceforge.net");
      }
    });
    menu.add(item);

    return menuBar;
  }
}

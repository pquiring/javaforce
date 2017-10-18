/**
 *
 * @author pquiring
 *
 * Created : Oct 10, 2013
 */

import javaforce.*;
import javax.swing.*;

public class Menu {
  private static MainPanel panel;
  public static void create(JFrame frame, MainPanel panel) {
    Menu.panel = panel;
    JMenuBar menu = create();
    frame.setJMenuBar(menu);
  }
  public static void create(JApplet applet, MainPanel panel) {
    Menu.panel = panel;
    JMenuBar menu = create();
    applet.setJMenuBar(menu);
  }
  private static JMenuBar create() {
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem item;

    menuBar = new JMenuBar();

    menu = new JMenu("File");
    menu.setMnemonic('f');
    menuBar.add(menu);

    item = new JMenuItem("New Project");
    item.setMnemonic('n');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.newSong();
      }
    });
    menu.add(item);

    item = new JMenuItem("Open Project");
    item.setMnemonic('o');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.open();
      }
    });
    menu.add(item);

    item = new JMenuItem("Save Project");
    item.setMnemonic('s');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.save();
      }
    });
    menu.add(item);

    item = new JMenuItem("Close Project");
    item.setMnemonic('c');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.close();
      }
    });
    menu.add(item);

    menu = new JMenu("Edit");
    menu.setMnemonic('e');
    menuBar.add(menu);

    item = new JMenuItem("Insert Row");
    item.setMnemonic('i');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.insertRow();
      }
    });
    menu.add(item);

    item = new JMenuItem("Delete Row");
    item.setMnemonic('d');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.deleteRow();
      }
    });
    menu.add(item);

    item = new JMenuItem("Duplicate Pattern");
    item.setMnemonic('p');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.dupPattern();
      }
    });
    menu.add(item);

    menu = new JMenu("Tools");
    menu.setMnemonic('t');
    menuBar.add(menu);

    item = new JMenuItem("MIDI Keyboard");
    item.setMnemonic('k');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        MidiKeyboard dialog = new MidiKeyboard(null, true, null);
        dialog.setVisible(true);
      }
    });
    menu.add(item);

    item = new JMenuItem("Export to Audio");
    item.setMnemonic('e');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.export();
      }
    });
    menu.add(item);

    item = new JMenuItem("Settings");
    item.setMnemonic('s');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        EditSettings dialog = new EditSettings(null, true);
        dialog.setVisible(true);
      }
    });
    menu.add(item);

    menu = new JMenu("Help");
    menu.setMnemonic('h');
    menuBar.add(menu);

    item = new JMenuItem("Content");
    item.setMnemonic('c');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JF.openURL("http://jfmusic.sourceforge.net/help.php");
      }
    });
    menu.add(item);

    item = new JMenuItem("About");
    item.setMnemonic('a');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JFAWT.showMessage("About", "jfMusic/" + MusicApp.version + "\nWebSite : http://jfmusic.sourceforge.net");
      }
    });
    menu.add(item);

    return menuBar;
  }
}

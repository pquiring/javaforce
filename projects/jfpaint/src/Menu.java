/**
 *
 * @author pquiring
 *
 * Created : Oct 11, 2013
 */

import javaforce.*;

import java.awt.event.*;
import javax.swing.*;

public class Menu {
  private static MainPanel panel;
  public static void create(JFrame frame, MainPanel panel) {
    Menu.panel = panel;
    JMenuBar menu = create(true);
    frame.setJMenuBar(menu);
  }
  public static void create(JApplet applet, MainPanel panel) {
    Menu.panel = panel;
    JMenuBar menu = create(false);
    applet.setJMenuBar(menu);
  }
  private static JMenuBar create(boolean exit) {
    JMenuBar menuBar;
    JMenu menu;
    JMenuItem item;

    menuBar = new JMenuBar();

    menu = new JMenu("File");
    menu.setMnemonic('f');
    menuBar.add(menu);

    item = new JMenuItem("New");
    item.setMnemonic('n');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.newTab();
      }
    });
    menu.add(item);

    item = new JMenuItem("Open");
    item.setMnemonic('o');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.openTab();
      }
    });
    menu.add(item);

    item = new JMenuItem("Save");
    item.setMnemonic('s');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.saveTab();
      }
    });
    menu.add(item);

    item = new JMenuItem("Save As");
    item.setMnemonic('a');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.saveAs();
      }
    });
    menu.add(item);

    item = new JMenuItem("Close");
    item.setMnemonic('c');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.closeTab(false);
      }
    });
    menu.add(item);

    if (exit) {

      menu.add(new JSeparator());

      item = new JMenuItem("Exit");
      item.setMnemonic('x');
      item.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
          System.exit(0);
        }
      });
      menu.add(item);

    }

    menu = new JMenu("Edit");
    menu.setMnemonic('e');
    menuBar.add(menu);

    item = new JMenuItem("Cut");
    item.setMnemonic('t');
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK, false));
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.delSel();
      }
    });
    menu.add(item);

    item = new JMenuItem("Copy");
    item.setMnemonic('y');
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK, false));
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        //copy is automatic when drawing a selection box
      }
    });
    menu.add(item);

    item = new JMenuItem("Paste");
    item.setMnemonic('p');
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK, false));
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.doPaste(false);
      }
    });
    menu.add(item);

    item = new JMenuItem("Paste (System)");
    item.setMnemonic('p');
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK, false));
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.doPaste(true);
      }
    });
    menu.add(item);

    menu = new JMenu("Help");
    menu.setMnemonic('h');
    menuBar.add(menu);

    item = new JMenuItem("About");
    item.setMnemonic('a');
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        JFAWT.showMessage("About", "jfPaint/" + MainPanel.version + "\n\nWebSite: jfpaint.sourceforge.net");
      }
    });
    menu.add(item);

    return menuBar;
  }
}

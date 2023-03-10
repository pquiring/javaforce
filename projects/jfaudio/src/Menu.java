/** Menu
 *
 * @author pquiring
 *
 * Created : Oct 10, 2013
 */

import javax.swing.*;

public class Menu {
  private static MainPanel panel;
  public static void create(JFrame frame, MainPanel panel) {
    Menu.panel = panel;
    JMenuBar menu = create();
    frame.setJMenuBar(menu);
  }
  private static JMenuBar create() {
    JMenuBar menuBar;
    JMenu menu, submenu;
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

    item = new JMenuItem("Open Project/Audio");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.openFile();
      }
    });
    menu.add(item);

    item = new JMenuItem("Save Project");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.saveProject();
      }
    });
    menu.add(item);

    item = new JMenuItem("Rename Project");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.renameProject();
      }
    });
    menu.add(item);

    item = new JMenuItem("Close Project");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.closeProject();
      }
    });
    menu.add(item);

    menu.add(new JSeparator());

    item = new JMenuItem("Import Audio");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.importFile();
      }
    });
    menu.add(item);

    item = new JMenuItem("Export Audio");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.exportFile(false);
      }
    });
    menu.add(item);

    item = new JMenuItem("Export Selection");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.exportFile(true);
      }
    });
    menu.add(item);

    menu.add(new JSeparator());

    item = new JMenuItem("Exit");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
      }
    });
    menu.add(item);

    menu = new JMenu("Edit");
    menuBar.add(menu);

    item = new JMenuItem("Undo");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.undo();
      }
    });
    menu.add(item);

    menu.add(new JSeparator());

    item = new JMenuItem("Cut");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.cut();
      }
    });
    menu.add(item);

    item = new JMenuItem("Copy");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.copy();
      }
    });
    menu.add(item);

    item = new JMenuItem("Paste");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.paste();
      }
    });
    menu.add(item);

    menu.add(new JSeparator());

    item = new JMenuItem("Settings");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        EditSettings dialog = new EditSettings(null, true);
        dialog.setVisible(true);
      }
    });
    menu.add(item);

    menu = new JMenu("Tracks");
    menuBar.add(menu);

    submenu = new JMenu("New");
    menu.add(submenu);

    item = new JMenuItem("Mono Track");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.newTrack(1);
      }
    });
    submenu.add(item);

    item = new JMenuItem("Stereo Track");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.newTrack(2);
      }
    });
    submenu.add(item);

    item = new JMenuItem("Info");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.trackInfo();
      }
    });
    menu.add(item);

    item = new JMenuItem("Mix");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.mixTracks();
      }
    });
    menu.add(item);

    item = new JMenuItem("Resample");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.resampleTrack();
      }
    });
    menu.add(item);

    menu = new JMenu("Generate");
    menuBar.add(menu);

    item = new JMenuItem("Tone");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.genTone();
      }
    });
    menu.add(item);

    item = new JMenuItem("Silence");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.genSilence();
      }
    });
    menu.add(item);

    menu = new JMenu("Effects");
    menuBar.add(menu);

    item = new JMenuItem("Amplify");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.fxAmplify();
      }
    });
    menu.add(item);

    item = new JMenuItem("Fade In");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.fxFadeIn();
      }
    });
    menu.add(item);

    item = new JMenuItem("Fade Out");
    item.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        panel.fxFadeOut();
      }
    });
    menu.add(item);

    return menuBar;
  }
}

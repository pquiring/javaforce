package javaforce;

/** Deprecated AWT/Swing functions
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.*;
import java.security.*;
import javax.swing.*;
import javax.swing.filechooser.*;

public class JFAWT {

  /** Opens a URL in default web browser */
  public static void openURL(String url) {
    try {
      java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static JFrame createJFrame(String title, int x, int y, int w, int h, LayoutManager lm) {
    //NOTE : When you add components, you must validate() the frame
    JFrame frame = new JFrame(title);
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.setVisible(true);
    frame.setLocation(x, y);
    //JAVA BUG : getInsets() doesn't work until the Window is visible
    Insets insets = frame.getInsets();
    frame.setSize(w + insets.left + insets.right, h + insets.top + insets.bottom);
    Container mainpane = frame.getContentPane();
    mainpane.setLayout(lm);
    return frame;
  }

  public static JPanel createJPanel(LayoutManager lm, Container parent) {
    JPanel ret = new JPanel();
    ret.setLayout(lm);
    if (parent != null) {
      ret.setBounds(0, 0, parent.getWidth(), parent.getHeight());
    }
    ret.setVisible(false);
    if (parent != null) {
      parent.add(ret);
    }
    return ret;
  }

  public static JPanel createJPanel(int x, int y, int w, int h, LayoutManager lm, Container parent) {
    JPanel ret = new JPanel();
    ret.setLayout(lm);
    ret.setBounds(x, y, w, h);
    ret.setVisible(false);
    if (parent != null) {
      parent.add(ret);
    }
    return ret;
  }

  public static JFImage createJFImage(Container parent) {
    JFImage ret = new JFImage();
    if (parent != null) {
      ret.setBounds(0, 0, parent.getWidth(), parent.getHeight());
      parent.add(ret);
    }
    return ret;
  }

  public static JFImage createJFImage(int x, int y, int w, int h, Container parent) {
    JFImage ret = new JFImage();
    ret.setBounds(x, y, w, h);
    if (parent != null) {
      parent.add(ret);
    }
    return ret;
  }

  /** Returns font metrics for a "monospaced" font.
   *
   * [0] = width
   * [1] = ascent
   * [2] = descent
   *
   * height = [1] + [2]
   */
  public static int[] getFontMetrics(Font fnt) {
    //after doing extensive testing none of the Java font metrics are correct
    //so I just draw a char and "measure" it for myself
    //return : [0]=width [1]=ascent [2]=descent (total height = ascent + descent)
    //Note : add ascent with y coordinate of print() to start printing below your coordinates
    //on Mac I noticed the draw square is 1 pixel higher than specified
    JFImage tmp = new JFImage(128, 128);
    Graphics g = tmp.getGraphics();
    g.setFont(fnt);
    g.setColor(Color.white);
    g.drawString("\u2588", 0, 64);
    int ascent = 1;
    for(int y=63;y>=0;y--,ascent++) {
      if (tmp.getPixel(0, y) == 0) break;
    }
    int descent = 0;
    for(int y=65;y<128;y++,descent++) {
      if (tmp.getPixel(0, y) == 0) break;
    }
    int width = 0;
    for(int x=0;x<128;x++,width++) {
      if (tmp.getPixel(x, 63) == 0) break;
    }
    int[] ret = new int[3];
    ret[0] = width;
    ret[1] = ascent;
    ret[2] = descent;
    return ret;
  }

  /** Returns font metrics for regular fonts.
   *
   * [0] = width
   * [1] = ascent
   * [2] = descent
   *
   * height = [1] + [2]
   */
  public static int[] getFontMetrics(Font font, String txt) {
    JFImage tmp = new JFImage(1,1);
    FontMetrics fm = tmp.getGraphics().getFontMetrics(font);
    int[] ret = new int[3];
    char[] ca = txt.toCharArray();
    ret[0] = fm.charsWidth(ca, 0, ca.length);
    ret[1] = fm.getAscent();
    ret[2] = fm.getDescent();
    return ret;
  }

  public static void showMessage(String title, String msg) {
    JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
  }

  public static void showError(String title, String msg) {
    JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
  }

  public static String getString(String msg, String str) {
    return JOptionPane.showInputDialog(null, msg, str);
  }

  public static boolean showConfirm(String title, String msg) {
    return JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
  }
  public static final int YES = JOptionPane.YES_OPTION;
  public static final int NO = JOptionPane.NO_OPTION;
  public static final int CANCEL = JOptionPane.CANCEL_OPTION;

  public static final int showConfirm3(String title, String msg) {
    return JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.YES_NO_CANCEL_OPTION);
  }

  /** Assigns a single hot key to activate button.
   * Use mnemonics for key combos.
   */
  public static void assignHotKey(JDialog dialog, final JButton button, int vk) {
    String name = "Action" + vk;
    Action event = new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        button.doClick();
      }
    };
    JRootPane root = dialog.getRootPane();
    root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(vk, 0), name);
    root.getActionMap().put(name, event);
  }

  /** Assigns a single hot key to activate button.
   * Use mnemonics for key combos.
   */
  public static void assignHotKey(JRootPane root, final JButton button, int vk) {
    String name = "Action" + vk;
    Action event = new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        button.doClick();
      }
    };
    root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(vk, 0), name);
    root.getActionMap().put(name, event);
  }

  /** Due to JVM bugs finding a monospaced font is not that easy...
   * See : Java Bug # 9009891 @ bugs.sun.com
   */
  public static Font getMonospacedFont(int style, int size) {
    int width;
    Font fnt;
    JFImage tmp = new JFImage(1,1);
    Graphics g = tmp.getGraphics();
    FontMetrics fm;

    fnt = new Font("monospaced", style, size);
    g.setFont(fnt);
    fm = g.getFontMetrics();
    try {
      width = fm.charWidth('.');
      if (fm.charWidth('w') != width) throw new Exception("nope");
      if (fm.charWidth('\u2588') != width) throw new Exception("nope");
      return fnt;  //as of 1.7.0_u51 works on linux but not windows
    } catch (Exception e) {}
    fnt = new Font("Lucida Console", style, size);
    g.setFont(fnt);
    fm = g.getFontMetrics();
    try {
      width = fm.charWidth('.');
      if (fm.charWidth('w') != width) throw new Exception("nope");
      if (fm.charWidth('\u2588') != width) throw new Exception("nope");
      return fnt;  //as of 1.7.0_u51 works on windows but not linux
    } catch (Exception e) {}
    JFLog.log("JF.getMonospacedFont():Unable to find a fixed width font");
    return null;  //die!
  }

  /** Same as java.awt.GraphicsEnvironment.getMaximumWindowBounds() except works after a screen mode change.
   *
   * See : http://stackoverflow.com/questions/22467544/java-awt-graphicsenvironment-getmaximumwindowbounds-does-not-change-after-scre
   */
  public static Rectangle getMaximumBounds() {
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
    DisplayMode mode = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
    Rectangle bounds = new Rectangle();
    bounds.x = insets.left;
    bounds.y = insets.top;
    bounds.width = mode.getWidth() - (insets.left + insets.right);
    bounds.height = mode.getHeight() - (insets.top + insets.bottom);
    return bounds;
  }

  /** Centers a window on screen (works with java.awt.Window/Frame javax.swing.JWindow/JFrame/JDialog */
  public static void centerWindow(java.awt.Window window) {
    Dimension d = window.getSize();
    Rectangle s = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    window.setLocation(s.width/2 - d.width/2, s.height/2 - d.height/2);
  }

  //taken from JOGL GLCanvas.java
  //NOTE : should place this in addNotify() and call it before super on X11 and after for Windows.
  private static boolean disableBackgroundEraseInitialized;
  private static Method  disableBackgroundEraseMethod;
  public static void disableBackgroundErase(Component comp) {
    final Component _comp = comp;
    if (!disableBackgroundEraseInitialized) {
      try {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
              try {
                Class<?> clazz = _comp.getToolkit().getClass();
                while (clazz != null && disableBackgroundEraseMethod == null) {
                  try {
                    disableBackgroundEraseMethod =
                      clazz.getDeclaredMethod("disableBackgroundErase",
                                              new Class[] { Canvas.class });
                    disableBackgroundEraseMethod.setAccessible(true);
                  } catch (Exception e) {
                    clazz = clazz.getSuperclass();
                  }
                }
              } catch (Exception e) {
              }
              return null;
            }
          });
      } catch (Exception e) {
      }
      disableBackgroundEraseInitialized = true;
    }
    if (disableBackgroundEraseMethod != null) {
      Throwable t=null;
      try {
        disableBackgroundEraseMethod.invoke(comp.getToolkit(), new Object[] { comp });
      } catch (Exception e) {
        t = e;
      }
    }
  }

  /** Modifies a JPanel so it can use a JMenuBar.
   *
   * Usage:
   *   - Create a JPanel (parent) with another JPanel (child) inside it that
   *     fills the space
   *   - place your controls in the child JPanel
   *   - Create a JMenuBar (NetBeans will place it in "Other Components")
   *   - in the ctor after initComponents() call setJPanelMenuBar()
   *     ie: setJPanelMenuBar(this, child, menuBar);
   *
   * Note:
   *   a client property "root" is set in the parent JPanel to the JRootPane created
   *     if you need later.
   *
   */
  public static void setJPanelMenuBar(JPanel parent, JPanel child, JMenuBar menuBar) {
    parent.removeAll();
    parent.setLayout(new BorderLayout());
    JRootPane root = new JRootPane();
    parent.add(root, BorderLayout.CENTER);
    root.setJMenuBar(menuBar);
    root.getContentPane().add(child);
    parent.putClientProperty("root", root);  //if you need later
  }

  public static void donate() {
    showMessage("Donate", "If you find this program useful,\nplease send $5 US or more via Paypal to pquiring@gmail.com\nThanks!");
  }

  public static String getOpenFile(String path) {
    return getOpenFile(path, null);
  }

  /** Show open file dialog.
   *
   * @param path = init path
   * @param filters[][] = new String[][] { {"desc", "txt"}, ...};
   * @return
   */
  public static String getOpenFile(String path, String filters[][]) {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(path));
    if (filters != null) {
      for(int a=0;a<filters.length;a++) {
        javax.swing.filechooser.FileFilter ff = new FileNameExtensionFilter(filters[a][0], filters[a][1]);
        chooser.addChoosableFileFilter(ff);
        if (a == 0) chooser.setFileFilter(ff);
      }
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile().getAbsolutePath();
    } else {
      return null;
    }
  }

  public static String getSaveFile(String file) {
    return getSaveFile(file, null);
  }

  /** Show save file dialog.
   *
   * @param file = init file
   * @param filters[][] = new String[][] { {"desc", "txt"}, ...};
   * @return
   */
  public static String getSaveFile(String file, String filters[][]) {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(file).getParentFile());
    if (filters != null) {
      for(int a=0;a<filters.length;a++) {
        javax.swing.filechooser.FileFilter ff = new FileNameExtensionFilter(filters[a][0], filters[a][1]);
        chooser.addChoosableFileFilter(ff);
        if (a == 0) chooser.setFileFilter(ff);
      }
    }
    chooser.setSelectedFile(new File(file));
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile().getAbsolutePath();
    } else {
      return null;
    }
  }

  public static String getSaveAsFile(String path) {
    return getSaveAsFile(path, null);
  }

  /** Show save file dialog.
   *
   * @param path = init path
   * @param filters[][] = new String[][] { {"desc", "txt"}, ...};
   * @return
   */
  public static String getSaveAsFile(String path, String filters[][]) {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(path));
    if (filters != null) {
      for(int a=0;a<filters.length;a++) {
        javax.swing.filechooser.FileFilter ff = new FileNameExtensionFilter(filters[a][0], filters[a][1]);
        chooser.addChoosableFileFilter(ff);
        if (a == 0) chooser.setFileFilter(ff);
      }
    }
    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile().getAbsolutePath();
    } else {
      return null;
    }
  }

  public static String getOpenFolder(String path) {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(path));
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile().getAbsolutePath();
    } else {
      return null;
    }
  }

  /** Java9+ treats the right Alt as AltGr.  This function will remove this 'bug'. */
  public static void removeAltGraph() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
      public boolean dispatchKeyEvent(KeyEvent e) {
        int mods = e.getModifiers();
        if ((mods & KeyEvent.ALT_GRAPH_MASK) != 0) {
          mods &= ~KeyEvent.ALT_GRAPH_MASK;
          e.setModifiers(mods);
        }
        return false;
      }
    });
  }

  public static void setMetalLAF() {
    try {
      javax.swing.UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static double getScaling() {
    GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    return device.getDisplayMode().getWidth() / (double) device.getDefaultConfiguration().getBounds().width;
  }

  public static void main(String[] args) {
    while (true) {
      System.out.println("scaling=" + getScaling());
      JF.sleep(1000);
    }
  }
}

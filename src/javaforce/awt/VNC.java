package javaforce.awt;

/** VNC Client.
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javaforce.*;

public class VNC extends javax.swing.JFrame implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {

  private static RFB rfb;

  private String host;
  private int port;
  private String pass;

  private static JFImage image;
  private int buttons;
  private static boolean is_fullscreen;
  private VNC vnc_windowed;
  private VNC vnc_fullscreen;

  public static boolean debug;

  private static boolean fast = true;

  /**
   * Creates new form VNC
   */
  public VNC(String host, int port, String pass) {
    initComponents();
    this.host = host;
    this.port = port;
    this.pass = pass;
    this.vnc_windowed = this;
    new Connect().start();
  }

  /** Create fullscreen window. */
  public VNC(VNC windowed) {
    this.vnc_windowed = windowed;
    this.setUndecorated(true);
    is_fullscreen = true;
    initComponents();
    this.remove(tools);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jButton2 = new javax.swing.JButton();
    tools = new javax.swing.JToolBar();
    jButton3 = new javax.swing.JButton();
    jButton1 = new javax.swing.JButton();
    jButton4 = new javax.swing.JButton();
    fullscreen = new javax.swing.JButton();
    scroll = new javax.swing.JScrollPane();

    jButton2.setText("jButton2");

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

    tools.setRollover(true);

    jButton3.setText("Refresh");
    jButton3.setFocusable(false);
    jButton3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    jButton3.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButton3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton3ActionPerformed(evt);
      }
    });
    tools.add(jButton3);

    jButton1.setText("C+A+D");
    jButton1.setFocusable(false);
    jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton1ActionPerformed(evt);
      }
    });
    tools.add(jButton1);

    jButton4.setText("WinKey");
    jButton4.setFocusable(false);
    jButton4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    jButton4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButton4.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton4ActionPerformed(evt);
      }
    });
    tools.add(jButton4);

    fullscreen.setText("FullScreen");
    fullscreen.setFocusable(false);
    fullscreen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    fullscreen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    fullscreen.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        fullscreenActionPerformed(evt);
      }
    });
    tools.add(fullscreen);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(tools, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
      .addComponent(scroll)
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addComponent(tools, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE))
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    ctrl_alt_del();
  }//GEN-LAST:event_jButton1ActionPerformed

  private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
    win_key();
  }//GEN-LAST:event_jButton4ActionPerformed

  private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    refresh();
  }//GEN-LAST:event_jButton3ActionPerformed

  private void fullscreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullscreenActionPerformed
    fullscreen();
  }//GEN-LAST:event_fullscreenActionPerformed

  /**
   * @param args the command line arguments
   */
  public static void main(String args[]) {
    String host = null;
    int port = 5900;
    String pass = null;
    if (args.length > 0) {
      host = args[0];
    }
    if (args.length > 1) {
      pass = args[1];
    }
    for(int a=2;a<args.length;a++) {
      switch (args[a]) {
        case "--debug":
          debug = true;
          RFB.debug = true;
          JFImage.debug = true;
          break;
        case "--fast":
          fast = true;
          break;
        case "--lean":
          fast = false;
          break;
      }
    }
    if (host == null) {
      host = JFAWT.getString("Enter VNC Host[:port]", "");
      if (host == null) return;
    }
    int idx = host.indexOf(':');
    if (idx != -1) {
      port = Integer.valueOf(host.substring(idx + 1));
      host = host.substring(0, idx);
    }
    if (pass == null) {
      pass = JFAWT.getString("Enter Password", "");
      if (pass == null) return;
    }
    String _host = host;
    int _port = port;
    String _pass = RFB.checkPassword(pass);
    /* Create and display the form */
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        new VNC(_host, _port, _pass).setVisible(true);
      }
    });
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton fullscreen;
  private javax.swing.JButton jButton1;
  private javax.swing.JButton jButton2;
  private javax.swing.JButton jButton3;
  private javax.swing.JButton jButton4;
  private javax.swing.JScrollPane scroll;
  private javax.swing.JToolBar tools;
  // End of variables declaration//GEN-END:variables


  public void setupEvents() {
    if (!is_fullscreen) {
      image.addMouseListener(this);
      image.addMouseMotionListener(this);
      image.addMouseListener(this);
      image.setFocusable(true);
      image.addKeyListener(this);
    }
    addKeyListener(this);
  }

  public void keyDown(int code) {
    rfb.writeKeyEvent(code, true);
  }

  public void keyUp(int code) {
    rfb.writeKeyEvent(code, false);
  }

  public void mouse(int x, int y, int buttons) {
    if (debug) {
      JFLog.log("mouse:" + x + "," + y + ":" + buttons);
    }
    rfb.writeMouseEvent(x, y, buttons);
  }

  public void ctrl_alt_del() {
    keyDown(RFB.VK_CONTROL);
    keyDown(RFB.VK_ALT);
    JF.sleep(10);
    keyDown(RFB.VK_DELETE);
    JF.sleep(50);
    keyUp(RFB.VK_DELETE);
    JF.sleep(10);
    keyUp(RFB.VK_ALT);
    keyUp(RFB.VK_CONTROL);
  }

  public void win_key() {
    //this is done with CTRL+ESC sequence
    keyDown(RFB.VK_CONTROL);
    JF.sleep(10);
    keyDown(RFB.VK_ESCAPE);
    JF.sleep(50);
    keyUp(RFB.VK_ESCAPE);
    JF.sleep(10);
    keyUp(RFB.VK_CONTROL);
  }

  public void refresh() {
    if (rfb == null) return;
    int width = rfb.getWidth();
    int height = rfb.getHeight();
    rfb.writeBufferUpdateRequest(0, 0, width, height, false);
  }

  public void fullscreen() {
    try {
      if (is_fullscreen) {
        JFLog.log("!fullscreen");
        vnc_fullscreen.dispose();
        vnc_windowed.setVisible(true);
        vnc_windowed.scroll.setViewportView(image);
        is_fullscreen = false;
      } else {
        JFLog.log("fullscreen");
        vnc_windowed.setVisible(false);
        vnc_fullscreen = new VNC(this);
        vnc_fullscreen.setVisible(true);
        vnc_fullscreen.scroll.setViewportView(image);
        GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(vnc_fullscreen);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public class Connect extends Thread {
    public void run() {
      try {
        while (true) {
          rfb = new RFB();

          if (rfb.connect(host, port)) {
            float server_version = rfb.readVersion();
            rfb.writeVersion(RFB.VERSION_3_8);
            byte[] auths = rfb.readAuthTypes();
            if (auths == null || auths.length == 0) {
              JFLog.log("VNC:No auth types available");
              System.exit(0);
            }
            rfb.writeAuthType(auths[0]);
            boolean ok = false;
            switch (auths[0]) {
              case RFB.AUTH_FAIL:
                break;
              case RFB.AUTH_NONE:
                ok = true;
                break;
              case RFB.AUTH_VNC:
                byte[] challenge = rfb.readAuthChallenge();
                byte[] response = RFB.encodeResponse(challenge, pass.getBytes());
                rfb.writeAuthResponse(response);
                ok = rfb.readAuthResult();
                break;
            }

            if (ok) {
              rfb.writeClientInit(true);
              if (rfb.readServerInit()) {
                int width = rfb.getWidth();
                int height = rfb.getHeight();
                image = new JFImage(width, height);
                image.setResizeOperation(JFImage.ResizeOperation.NONE);
                image.fill(0, 0, width, height, JFImage.OPAQUE);
                setupEvents();
                setSize(width, height);
                scroll.setViewportView(image);
                new MainLoop().start();
                break;
              }
            }
          }

          JFLog.log("Connection failed...");

          rfb = null;

          pass = JFAWT.getString("Enter Password", pass);
          if (pass == null) {
            dispose();
            System.exit(0);
          }
          pass = RFB.checkPassword(pass);
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public class MainLoop extends Thread {
    public void run() {
      if (fast) {
        rfb.writeEncodingsFast();
      } else {
        rfb.writeEncodingsLean();
      }
      rfb.writePixelFormat();
      {
        int width = rfb.getWidth();
        int height = rfb.getHeight();
        rfb.writeBufferUpdateRequest(0, 0, width, height, false);
      }
      try {
        while (true) {
          int msg = rfb.readMessageType();
          switch (msg) {
            case RFB.S_MSG_CLOSE:
              dispose();
              System.exit(0);
              break;
            case RFB.S_MSG_BUFFER_UPDATE:
              RFB.Rectangle rect = rfb.readBufferUpdate();
              if (debug) JFLog.log("VNC:Rectangle Update=" + rect);
              int width = rfb.getWidth();
              int height = rfb.getHeight();
              if (rect.newSize) {
                if (debug) {
                  JFLog.log("VNC:New Desktop Size:" + width + "x" + height);
                }
                image.setSize(width, height);
                rfb.writeBufferUpdateRequest(0, 0, width, height, false);
              } else {
                if (debug) {
                  JFLog.log("VNC:DrawRect:" + rect);
                }
                image.putPixels(rfb.getBuffer(), rect.x, rect.y, rect.width, rect.height, rect.y * width + rect.x, width);
                rfb.writeBufferUpdateRequest(0, 0, width, height, true);
              }
              java.awt.EventQueue.invokeLater(() -> {
                image.repaint();
              });
              break;
            case RFB.S_MSG_BELL:
              rfb.readBell();
              break;
            case RFB.S_MSG_COLOR_MAP:
              rfb.readColorMap();
              break;
            case RFB.S_MSG_CUT_TEXT:
              rfb.readCutText();
              break;
            default:
              JFLog.log("Unknown msg:" + msg);
              break;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    int button = e.getButton();
    buttons &= 0x7;
    switch (button) {
      case 1: buttons |= 1; break;
      case 2: buttons |= 2; break;
      case 3: buttons |= 4; break;
    }
    mouse(x, y, buttons);
  }

  public void mouseReleased(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    int button = e.getButton();
    buttons &= 0x7;
    switch (button) {
      case 1: buttons &= 0xf - 1; break;
      case 2: buttons &= 0xf - 2; break;
      case 3: buttons &= 0xf - 4; break;
    }
    mouse(x, y, buttons);
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mouseDragged(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    buttons &= 0x7;
    mouse(x, y, buttons);
  }

  public void mouseMoved(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    buttons &= 0x7;
    mouse(x, y, buttons);
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    int x = e.getX();
    int y = e.getY();
    int wheel = e.getWheelRotation();
    if (wheel > 0) {
      buttons |= 8;
    }
    if (wheel < 0) {
      buttons |= 16;
    }
    mouse(x, y, buttons);
  }

  public void keyTyped(KeyEvent e) {
    e.consume();
  }

  public void keyPressed(KeyEvent e) {
    char ch = e.getKeyChar();
    int code = e.getKeyCode();
    if (debug) {
      JFLog.log("VNC:KeyPressed:" + Integer.toString(ch) + ":" + code);
    }
    if (e.isAltDown() && e.isShiftDown() && e.isControlDown()) {
      if (code == KeyEvent.VK_F) {
        fullscreen();
      }
      e.consume();
      return;
    }
    if (ch == KeyEvent.CHAR_UNDEFINED) {
      keyDown(VNCRobot.convertJavaKeyCode(code));
    } else {
      if (e.isControlDown()) {
        ch += 0x60;
      }
      if (ch < 0x20) {
        ch = (char)VNCRobot.convertJavaKeyCode(ch);
      }
      keyDown(ch);
    }
  }

  public void keyReleased(KeyEvent e) {
    char ch = e.getKeyChar();
    int code = e.getKeyCode();
    if (debug) {
      JFLog.log("VNC:KeyReleased:" + Integer.toString(ch) + ":" + code);
    }
    if (ch == KeyEvent.CHAR_UNDEFINED) {
      keyUp(VNCRobot.convertJavaKeyCode(code));
    } else {
      if (e.isControlDown()) {
        ch += 0x60;
      }
      if (ch < 0x20) {
        ch = (char)VNCRobot.convertJavaKeyCode(ch);
      }
      keyUp(ch);
    }
  }
}

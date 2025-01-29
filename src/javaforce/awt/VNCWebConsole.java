package javaforce.awt;

/** VNCWebConsole
 *
 * Connects to local VNC server and render output thru WebUI.
 *
 * @author pquiring
 */

import java.awt.event.KeyEvent;

import javaforce.*;
import javaforce.awt.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class VNCWebConsole extends Thread implements Resized {
  private int vnc_port;
  private String vnc_password;
  private Panel panel;
  private ToolBar tools;
  private Canvas canvas;

  private RFB rfb;
  private WebUIClient client;

  private static final boolean debug = false;

  public static final int OPT_TOOLBAR = 1;
  public static final int OPT_SCALE = 2;

  public VNCWebConsole(int vnc_port, String password, Canvas canvas) {
    this.vnc_port = vnc_port;
    this.vnc_password = password;
    this.canvas = canvas;
  }

  public static Panel createPanel(int vnc_port, String password, int opts) {
    boolean opt_toolbar = (opts & OPT_TOOLBAR) != 0;
    boolean opt_scale = (opts & OPT_SCALE) != 0;
    Panel panel = new Panel();
    Canvas canvas = new Canvas();
    VNCWebConsole console = new VNCWebConsole(vnc_port, password, canvas);
    console.panel = panel;
    canvas.addResizedListener(console);
    panel.addResizedListener(console);
    panel.requestSize();
    canvas.settransformorigin(0, 0);
    if (opt_toolbar) {
      ToolBar tools = new ToolBar();
      console.tools = tools;
      panel.add(tools);
      Button refresh = new Button("Refresh");
      tools.add(refresh);
      Button cad = new Button("C+A+D");
      tools.add(cad);
      Button winkey = new Button("WinKey");
      tools.add(winkey);
      Button scale = new Button("Scale");
      tools.add(scale);
      //setup button events
      refresh.addClickListener((me, cmp) -> {
        console.refresh();
      });
      cad.addClickListener((me, cmp) -> {
        console.cad();
      });
      winkey.addClickListener((me, cmp) -> {
        console.winkey();
      });
      scale.addClickListener((me, cmp) -> {
        console.scale();
      });
      tools.addResizedListener(console);
      tools.requestSize();
    }
    panel.add(canvas);
    //setup canvas events (must setup before sent to client)
    canvas.addMouseDownListener((me, cmp) -> {
      console.mouse(me.x, me.y, me.buttons);
      canvas.setFocus();
    });
    canvas.addMouseUpListener((me, cmp) -> {
      console.mouse(me.x, me.y, me.buttons);
    });
    canvas.addMouseMoveListener((me, cmp) -> {
      console.mouse(me.x, me.y, me.buttons);
    });
    canvas.addKeyDownListenerPreventDefault((ke, cmp) -> {
      if (ke.keyChar != 0) {
        console.keyDown(ke.keyChar, false);
      } else {
        console.keyDown(ke.keyCode, true);
      }
    });
    canvas.addKeyUpListenerPreventDefault((ke, cmp) -> {
      if (ke.keyChar != 0) {
        console.keyUp(ke.keyChar, false);
      } else {
        console.keyUp(ke.keyCode, true);
      }
    });
    if (opt_scale) {
      console.scale();
    }
    console.start();
    return panel;
  }

  private boolean isConnected() {
    return client.isConnected();
  }

  public void run() {
    if (debug) {
      RFB.debug = true;
    }
    client = canvas.getClient();  //waits until panel is loaded
    while (isConnected()) {
      rfb = new RFB();
      if (!rfb.connect("127.0.0.1", vnc_port)) {
        JFLog.log("VNCWeb:connection failed");
        JF.sleep(1000);
        continue;
      }
      float server_version = rfb.readVersion();
      rfb.writeVersion(RFB.VERSION_3_8);
      byte[] auths = rfb.readAuthTypes();
      if (auths == null || auths.length == 0) {
        JFLog.log("VNCWeb:No auth types available");
        return;
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
          byte[] response = RFB.encodeResponse(challenge, vnc_password.getBytes());
          rfb.writeAuthResponse(response);
          ok = rfb.readAuthResult();
          break;
      }

      if (!ok) {
        JFLog.log("VNCWeb:auth failed");
        return;
      }

      rfb.writeClientInit(true);
      if (!rfb.readServerInit()) {
        JFLog.log("VNCWeb:server init failed");
        JF.sleep(1000);
        continue;
      }

      //setup canvas size
      {
        int width = rfb.getWidth();
        int height = rfb.getHeight();
        canvas.setSize(width, height);
      }

      canvas.setFocus();

      main();

      rfb.disconnect();
    }
  }

  public void refresh() {
    int width = rfb.getWidth();
    int height = rfb.getHeight();
    if (debug) {
      JFLog.log("VNCWeb:refresh:" + width + "x" + height);
    }
    rfb.writeBufferUpdateRequest(0, 0, width, height, false);
  }

  public void cad() {
    if (debug) {
      JFLog.log("VNCWeb:C+A+D");
    }
    keyDown(KeyEvent.VK_CONTROL, true);
    keyDown(KeyEvent.VK_ALT, true);
    JF.sleep(10);
    keyDown(KeyEvent.VK_DELETE, true);
    JF.sleep(50);
    keyUp(KeyEvent.VK_DELETE, true);
    JF.sleep(10);
    keyUp(KeyEvent.VK_ALT, true);
    keyUp(KeyEvent.VK_CONTROL, true);
  }

  public void winkey() {
    //this is done with CTRL+ESC sequence
    if (debug) {
      JFLog.log("VNCWeb:WinKey");
    }
    keyDown(KeyEvent.VK_CONTROL, true);
    JF.sleep(10);
    keyDown(KeyEvent.VK_ESCAPE, true);
    JF.sleep(50);
    keyUp(KeyEvent.VK_ESCAPE, true);
    JF.sleep(10);
    keyUp(KeyEvent.VK_CONTROL, true);
  }

  private boolean scaled;

  public void scale() {
    if (scaled) {
      scaled = false;
      canvas.setscale(1.0f, 1.0f);
    } else {
      scaled = true;
      canvas.requestSize();  //triggers onResized()
    }
  }

  public void onResized(Component cmp, int width, int height) {
    if (!scaled) return;
    if (cmp != canvas) return;
    float panel_width = panel.getWidth();
    float panel_height = panel.getHeight();
    if (panel_width == 0 || panel_height == 0) {
      JFLog.log("VNCWeb:scale:panel size not known yet");
      return;
    }
    if (tools != null) {
      float tools_height = tools.getHeight();
      if (tools_height == 0) {
        JFLog.log("VNCWeb:scale:tools size not known yet");
        return;
      }
      panel_height -= tools_height;
    }
    float canvas_width = width;
    float canvas_height = height;
    float scale_width = panel_width / canvas_width;
    float scale_height = panel_height / canvas_height;
    canvas.setscale(scale_width, scale_height);
  }

  public void mouse(int x, int y, int buttons) {
    if (rfb == null || !rfb.isConnected()) return;
    //need to swap b2 and b3
    boolean b1 = (buttons & 0x01) != 0;
    boolean b2 = (buttons & 0x02) != 0;
    boolean b3 = (buttons & 0x04) != 0;
    buttons = 0;
    if (b1) buttons |= 0x01;
    if (b3) buttons |= 0x02;
    if (b2) buttons |= 0x04;
    rfb.writeMouseEvent(x, y, buttons);
  }

  public void keyDown(int code, boolean convert) {
    if (rfb == null || !rfb.isConnected()) return;
    if (convert) {
      code = VNCRobot.convertJavaKeyCode(code);
    }
    rfb.writeKeyEvent(code, true);
  }

  public void keyUp(int code, boolean convert) {
    if (rfb == null || !rfb.isConnected()) return;
    if (convert) {
      code = VNCRobot.convertJavaKeyCode(code);
    }
    rfb.writeKeyEvent(code, false);
  }

  private void main() {
    rfb.writeEncodingsFast();
    rfb.writePixelFormat();
    {
      int width = rfb.getWidth();
      int height = rfb.getHeight();
      rfb.writeBufferUpdateRequest(0, 0, width, height, false);
    }
    try {
      while (isConnected() && rfb.isConnected()) {
        int msg = rfb.readMessageType();
        switch (msg) {
          case RFB.S_MSG_CLOSE:
            JFLog.log("VNCWeb:Connection closed");
            return;
          case RFB.S_MSG_BUFFER_UPDATE:
            RFB.Rectangle rect = rfb.readBufferUpdate();
            int width = rfb.getWidth();
            int height = rfb.getHeight();
            if (rect.newSize) {
              canvas.setSize(width, height);
              rfb.writeBufferUpdateRequest(0, 0, width, height, false);
            } else {
              JFImage image = rfb.getImage(rect);
              rfb.writeBufferUpdateRequest(0, 0, width, height, true);
              canvas.drawImage(image, new Point(rect.x, rect.y));
            }
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
            JFLog.log("VNCWeb:Unknown msg:" + msg);
            break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    try {
      rfb.disconnect();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

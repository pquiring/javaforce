package service;

/** WebConsole
 *
 * Connects to local VNC server and render output thru WebUI.
 *
 * @author pquiring
 */

import java.awt.event.KeyEvent;
import javaforce.*;
import javaforce.awt.*;
import javaforce.webui.*;

public class WebConsole extends Thread {
  public HTTP.Parameters params;
  public Canvas canvas;
  public Button refresh;
  public Button cad;
  public Button winkey;

  private RFB rfb;
  private WebUIClient client;
  
  private static final boolean debug = false;

  public WebConsole() {
  }

  public void run() {
    String id = params.get("id");
    if (id == null) {
      JFLog.log("VNC:vmname==null");
      return;
    }
    ConsoleSession sess = ConsoleSession.get(id);
    if (id == null) {
      JFLog.log("VNC:sess==null");
      return;
    }
    rfb = new RFB();
    if (debug) {
      RFB.debug = true;
    }
    if (!rfb.connect("127.0.0.1", sess.vm.getVNC())) {
      JFLog.log("VNC:connection failed");
      return;
    }
    float server_version = rfb.readVersion();
    rfb.writeVersion(RFB.VERSION_3_8);
    byte[] auths = rfb.readAuthTypes();
    if (auths == null || auths.length == 0) {
      JFLog.log("VNC:No auth types available");
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
        byte[] response = RFB.encodeResponse(challenge, Config.current.vnc_password.getBytes());
        rfb.writeAuthResponse(response);
        ok = rfb.readAuthResult();
        break;
    }

    if (!ok) {
      JFLog.log("VNC:auth failed");
      return;
    }

    rfb.writeClientInit(true);
    if (!rfb.readServerInit()) {
      JFLog.log("VNC:server init failed");
      return;
    }

    //setup canvas size
    {
      int width = rfb.getWidth();
      int height = rfb.getHeight();
      canvas.setSize(width, height);
    }

    //setup button events
    refresh.addClickListener((me, cmp) -> {
      int width = rfb.getWidth();
      int height = rfb.getHeight();
      if (debug) {
        JFLog.log("VNC:refresh:" + width + "x" + height);
      }
      rfb.writeBufferUpdateRequest(0, 0, width, height, false);
    });
    cad.addClickListener((me, cmp) -> {
      if (debug) {
        JFLog.log("VNC:C+A+D");
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
    });
    winkey.addClickListener((me, cmp) -> {
      //this is done with CTRL+ESC sequence
      if (debug) {
        JFLog.log("VNC:WinKey");
      }
      keyDown(KeyEvent.VK_CONTROL, true);
      JF.sleep(10);
      keyDown(KeyEvent.VK_ESCAPE, true);
      JF.sleep(50);
      keyUp(KeyEvent.VK_ESCAPE, true);
      JF.sleep(10);
      keyUp(KeyEvent.VK_CONTROL, true);
    });

    client = canvas.getClient();
    
    canvas.setFocus();

    main();
  }
  
  public void mouse(int x, int y, int buttons) {
    if (rfb == null) return;
    rfb.writeMouseEvent(x, y, buttons);
  }

  public void keyDown(int code, boolean convert) {
    if (rfb == null) return;
    if (convert) {
      code = RFB.convertKeyCode(code);
    }
    rfb.writeKeyEvent(code, true);
  }

  public void keyUp(int code, boolean convert) {
    if (rfb == null) return;
    if (convert) {
      code = RFB.convertKeyCode(code);
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
      while (client.isConnected()) {
        int msg = rfb.readMessageType();
        switch (msg) {
          case RFB.S_MSG_CLOSE:
            JFLog.log("VNC:Connection closed");
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
            JFLog.log("VNC:Unknown msg:" + msg);
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

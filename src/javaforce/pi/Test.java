package javaforce.pi;

/** Raspberry Pi Test
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;
import javaforce.webui.*;
import javaforce.webui.event.*;

public class Test implements WebUIHandler {
  public static void main(String[] args) {
    if (args.length > 0) {
      switch (args[0]) {
        case "read": read(args[1], args[2]); break;
        case "write": write(args[1], args[2], args[3]); break;
        default: JFLog.log("usage: read|write ip tag [value]"); break;
      }
    } else {
      new Test().start();
    }
  }
  public void start() {
    if (!GPIO.init()) {
      JFLog.log("GPIO.init() failed");
      return;
    }
    for(int a=0;a<8;a++) {
      GPIO.configInput(a);
      GPIO.configOutput(a+8);
    }
    initResources();
    WebUIServer server = new WebUIServer();
    server.start(this, 8080, false);
    Worker worker = new Worker();
    worker.start();
  }

  public void clientConnected(WebUIClient client) {}
  public void clientDisconnected(WebUIClient client) {}

  public class TestPanel extends Panel {
    public Label[] il = new Label[8];
    public Image[] i = new Image[8];
    public Button[] o = new Button[8];
    public Image[] oi = new Image[8];
  }
  public static TestPanel panel;
  public static Resource on, off;

  private void initResources() {
    on = Resource.readResource("javaforce/pi/on.png", Resource.PNG);
    off = Resource.readResource("javaforce/pi/off.png", Resource.PNG);
  }

  public Panel getRootPanel(WebUIClient client) {
    panel = new TestPanel();
    Column col = new Column();
    panel.add(col);
    for(int a=0;a<8;a++) {
      Row row = new Row();
      col.add(row);
      row.add(new Pad());
      panel.il[a] = new Label("I" + (a+1));
      row.add(panel.il[a]);
      panel.i[a] = new Image(off);
      row.add(panel.i[a]);
      row.add(new Pad());
    }
    for(int a=0;a<8;a++) {
      Row row = new Row();
      col.add(row);
      row.add(new Pad());
      panel.o[a] = new Button("O" + (a+1));
      final int idx = a;
      panel.o[a].addClickListener((MouseEvent event, Component button) -> {
        boolean state = !outputs[idx];
        outputs[idx] = state;
        GPIO.write(idx + 8, state);
        panel.oi[idx].setImage(state ? on : off);
      });
      row.add(panel.o[a]);
      panel.oi[a] = new Image(off);
      row.add(panel.oi[a]);
      row.add(new Pad());
    }
    return panel;
  }

  public byte[] getResource(String url) {
    return null;
  }

  public static boolean[] outputs = new boolean[8];
  public static boolean[] inputs = new boolean[8];
  public static boolean[] display = new boolean[8];

  public static class Worker extends Thread {
    public volatile boolean active;

    public void run() {
      active = true;
      while (active) {
        //get inputs
        for(int a=0;a<8;a++) {
          inputs[a] = GPIO.read(a);
          if (panel != null) {
            if (inputs[a] != display[a]) {
              display[a] = inputs[a];
              panel.i[a].setImage(inputs[a] ? on : off);
            }
          }
        }
        JF.sleep(100);
      }
    }
  }

  public static void write(String ip, String tag, String value) {
    Controller c = new Controller();
    if (!c.connect("MODBUS:" + ip)) {
      JFLog.log("Error:connect() failed");
      return;
    }
    byte[] data = new byte[2];
    switch (value) {
      case "true": data[0] = 1; break;
      case "false": break;
      default: BE.setuint16(data, 0, Integer.valueOf(value)); break;
    }
    c.write(tag, data);
    JF.sleep(500);
    JFLog.log("write:" + tag + "=" + value);
  }
  public static void read(String ip, String tag) {
    Controller c = new Controller();
    if (!c.connect("MODBUS:" + ip)) {
      JFLog.log("Error:connect() failed");
      return;
    }
    byte[] data = c.read(tag);
    if (data == null) {
      JFLog.log("Error:read() == null");
      return;
    }
    int value = 0;
    switch (data.length) {
      case 0: JFLog.log("Error:data.length==0"); break;
      case 1: value = data[0] & 0xff; break;
      default:
      case 2: value = BE.getuint16(data, 0); break;
    }
    JFLog.log("read:" + tag + "=" + value);
  }
}

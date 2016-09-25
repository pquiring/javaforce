package javaforce.pi;

/** Raspberry Pi Test
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

public class Test implements WebUIHandler {
  public static void main(String args[]) {
    new Test().start();
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
    Server server = new Server();
    server.start(this, 8080, false);
    Worker worker = new Worker();
    worker.start();
  }

  public class TestPanel extends Panel {
    public Label il[] = new Label[8];
    public Image i[] = new Image[8];
    public Button o[] = new Button[8];
    public Image oi[] = new Image[8];
  }
  public static TestPanel panel;
  public static Resource on, off;

  private void initResources() {
    on = Resource.readResource("javaforce/pi/on.png", Resource.PNG);
    off = Resource.readResource("javaforce/pi/off.png", Resource.PNG);
  }

  public Panel getRootPanel(Client client) {
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
      panel.o[a].addClickListener((Button b) -> {
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

  public static boolean outputs[] = new boolean[8];
  public static boolean inputs[] = new boolean[8];
  public static boolean display[] = new boolean[8];

  public static class Worker extends Thread {
    public volatile boolean active;

    public void run() {
      active = true;
      while (active) {
        //get inputs
        for(int a=0;a<8;a++) {
          inputs[a] = GPIO.read(a);
          if (inputs[a] != display[a]) {
            display[a] = inputs[a];
            panel.i[a].setImage(inputs[a] ? on : off);
          }
        }
        JF.sleep(1000);
      }
    }
  }
}

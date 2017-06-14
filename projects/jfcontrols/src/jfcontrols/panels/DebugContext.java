package jfcontrols.panels;

import javaforce.JF;

/** Debugging context
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.functions.*;

public class DebugContext extends Thread {
  private volatile boolean active = true;
  private WebUIClient client;
  private int fid;  //function id
  private Object lock = new Object();

  public DebugContext(WebUIClient client, int fid) {
    this.client = client;
    this.fid = fid;
  }
  public void run() {
    boolean debug[][] = FunctionService.getDebugFlags(fid);
    if (debug == null) {
      active = false;
      return;
    }
    int cnt = debug.length;
    Panel panel = client.getPanel();
    try {
      while (active) {
        JF.sleep(100);
        for(int a=0;a<cnt && active;a++) {
          client.sendEvent(panel.getComponent("en_0_" + a).id, "setbackclr", new String[] {"clr=" + (debug[a][0] ? "#0c0" : "#ccc")});
          client.sendEvent(panel.getComponent("en_1_" + a).id, "setbackclr", new String[] {"clr=" + (debug[a][1] ? "#0c0" : "#ccc")});
        }
        //TODO : ping/pong client to ensure it's up-to-date
      }
      for(int a=0;a<cnt;a++) {
        client.sendEvent(panel.getComponent("en_0_" + a).id, "setbackclr", new String[] {"clr=" + "#fff"});
        client.sendEvent(panel.getComponent("en_1_" + a).id, "setbackclr", new String[] {"clr=" + "#fff"});
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    synchronized(lock) {
      lock.notify();
    }
  }
  public void cancel() {
    if (active == false) return;
    active = false;
    synchronized(lock) {
      active = false;
      try {lock.wait();} catch (Exception e) {}
    }
  }
}

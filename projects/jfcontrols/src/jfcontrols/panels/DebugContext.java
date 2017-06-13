package jfcontrols.panels;

import javaforce.JF;

/** Debugging context
 *
 * @author pquiring
 */

import javaforce.webui.*;

import jfcontrols.functions.*;

public class DebugContext extends Thread {
  private volatile boolean active = true;
  private WebUIClient client;
  private int fid;  //function id
  private Object lock = new Object();

  public DebugContext(WebUIClient client, int fid) {
    this.fid = fid;
  }
  public void run() {
    boolean debug[][] = FunctionService.getDebugFlags(fid);
    if (debug == null) {
      active = false;
      return;
    }
    JF.sleep(250);
    while (active) {
      JF.sleep(100);
      int cnt = debug.length;
      for(int a=0;a<cnt && active;a++) {
        client.sendEvent("en_0_" + a, "setbackcolor", new String[] {"clr=" + (debug[a][0] ? "#0c0" : "000")});
        client.sendEvent("en_1_" + a, "setbackcolor", new String[] {"clr=" + (debug[a][1] ? "#0c0" : "000")});
      }
      //TODO : ping/pong client to ensure it's up-to-date
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

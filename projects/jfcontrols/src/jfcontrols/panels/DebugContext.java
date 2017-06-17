package jfcontrols.panels;

import javaforce.JF;

/** Debugging context
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.functions.*;
import jfcontrols.tags.*;

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
    ClientContext context = (ClientContext)client.getProperty("context");
    Panel panel = client.getPanel();
    boolean debug[][] = FunctionService.getDebugFlags(fid);
    if (debug == null) {
      active = false;
      return;
    }
    boolean state[][] = new boolean[debug.length][2];
    Component cmp[][] = new Component[debug.length][2];
    for(int a=0;a<debug.length;a++) {
      cmp[a][0] = panel.getComponent("en_0_" + a);
      cmp[a][1] = panel.getComponent("en_1_" + a);
    }
    TagBase tags[] = context.taglist.toArray(new TagBase[context.taglist.size()]);
    Label tv[] = new Label[tags.length];
    for(int a=0;a<tags.length;a++) {
      tv[a] = (Label)panel.getComponent("tv_" + a);
    }
    boolean first = true;

    int cnt = debug.length;
    try {
      while (active) {
        JF.sleep(100);
        //update enables
        for(int a=0;a<cnt && active;a++) {
          if (first || state[a][0] != debug[a][0]) {
            client.sendEvent(cmp[a][0].id, "setbackclr", new String[] {"clr=" + (debug[a][0] ? "#0c0" : "#ccc")});
            state[a][0] = debug[a][0];
          }
          if (first || state[a][1] != debug[a][1]) {
            client.sendEvent(cmp[a][1].id, "setbackclr", new String[] {"clr=" + (debug[a][1] ? "#0c0" : "#ccc")});
            state[a][1] = debug[a][1];
          }
        }
        //update tag values
        for(int a=0;a<tags.length;a++) {
          TagBase tag = tags[a];
          if (tag == null) continue;
          Label lbl = tv[a];
          if (lbl == null) continue;
          String cv = lbl.getText();
          String nv = tag.getValue();
          if (nv == null) continue;
          lbl.setText(nv);
        }
        //TODO : ping/pong client to ensure it's up-to-date
        first = false;
      }
      for(int a=0;a<cnt;a++) {
        client.sendEvent(cmp[a][0].id, "setbackclr", new String[] {"clr=" + "#fff"});
        client.sendEvent(cmp[a][1].id, "setbackclr", new String[] {"clr=" + "#fff"});
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

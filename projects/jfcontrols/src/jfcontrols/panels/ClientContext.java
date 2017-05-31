package jfcontrols.panels;

/** Monitors tag changes for components on Panel.
 *
 * @author pquiring
 */

import java.util.*;
import javaforce.JF;

import javaforce.webui.*;

import jfcontrols.tags.*;

public class ClientContext extends Thread {
  private WebUIClient client;
  private ArrayList<Monitor> listeners = new ArrayList<>();
  private volatile boolean active;
  private Object lock = new Object();
  private ArrayList<Monitor> stack = new ArrayList<>();

  public ClientContext(WebUIClient client) {
    this.client = client;
  }
  private static class Monitor implements TagBaseListener {
    public TagAddr ta;
    public MonitoredTag tag;
    public Component comp;
    public ClientContext ctx;
    public String value;
    public Monitor(TagAddr ta, MonitoredTag tag, Component comp, ClientContext ctx) {
      this.ta = ta;
      this.tag = tag;
      this.comp = comp;
      this.ctx = ctx;
    }
    public void tagChanged(TagBase tag, int idx, String value) {
      synchronized(ctx.lock) {
        this.value = value;
        ctx.stack.add(this);
        ctx.lock.notify();
      }
    }
  }

  public void addListener(TagAddr ta, MonitoredTag tag, Component comp) {
    if (tag == null) return;
    Monitor monitor = new Monitor(ta, tag, comp, this);
    listeners.add(monitor);
    tag.addListener(monitor);
  }

  public void clear() {
    while (listeners.size() > 0) {
      Monitor monitor = listeners.remove(0);
      monitor.tag.removeListener(monitor);
    }
  }

  public void run() {
    Monitor monitor;
    active = true;
    //wait till client is ready
    while (!client.isReady()) {
      JF.sleep(100);
    }
    while (active) {
      synchronized(lock) {
        try {lock.wait();} catch (Exception e) {}
        if (stack.size() == 0) continue;
        monitor = stack.remove(0);
      }
      if (monitor == null) continue;
      Component c = monitor.comp;
      if (c instanceof Label) {
        ((Label)c).setText(monitor.value);
      }
    }
  }

  public void cancel() {
    active = false;
    synchronized(lock) {
      lock.notify();
    }
  }
}

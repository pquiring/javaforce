package jfcontrols.panels;

/** Monitors tag changes for components on Panel.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.webui.*;

import jfcontrols.tags.*;

public class ClientContext extends Thread {
  private WebUIClient client;
  private ArrayList<Pair> listeners = new ArrayList<>();
  private volatile boolean active;
  private Object lock = new Object();
  private ArrayList<Pair> stack = new ArrayList<>();

  public ClientContext(WebUIClient client) {
    this.client = client;
  }
  private static class Pair implements TagBaseListener {
    public TagAddr ta;
    public MonitoredTag tag;
    public Component comp;
    public ClientContext ctx;
    public String value;
    public Pair(TagAddr ta, MonitoredTag tag, Component comp, ClientContext ctx) {
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
    Pair pair = new Pair(ta, tag, comp, this);
    listeners.add(pair);
    tag.addListener(pair);
  }

  public void clear() {
    while (listeners.size() > 0) {
      Pair pair = listeners.remove(0);
      pair.tag.removeListener(pair);
    }
  }

  public void run() {
    Pair pair;
    active = true;
    while (active) {
      synchronized(lock) {
        try {lock.wait();} catch (Exception e) {}
        if (stack.size() == 0) continue;
        pair = stack.remove(0);
      }
      if (pair == null) continue;
      Component c = pair.comp;
      if (c instanceof Label) {
        ((Label)c).setText(pair.value);
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

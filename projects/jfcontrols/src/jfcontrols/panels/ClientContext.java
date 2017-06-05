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
  private TagsCache tags = new TagsCache();

  public HashMap<String, Component> alarms = new HashMap<>();
  public int lastAlarmID;

  public ClientContext(WebUIClient client) {
    this.client = client;
  }

  public TagBase getTag(String name) {
    TagAddr ta = tags.decode(name);
    return tags.getTag(ta);
  }

  public TagBase getTag(TagAddr ta) {
    return tags.getTag(ta);
  }

  public String read(String name) {
    return tags.read(name);
  }

  public void write(String name, String value) {
    tags.write(name, value);
  }

  public TagAddr decode(String name) {
    return tags.decode(name);
  }

  private static class Monitor implements TagBaseListener {
    public TagAddr ta;
    public MonitoredTag tag;
    public ClientContext ctx;
    public String oldValue, newValue;
    public Component cmp;
    public TagID id;
    public TagAction action;
    public Monitor(TagAddr ta, MonitoredTag tag, Component cmp, TagAction action, ClientContext ctx) {
      this.ta = ta;
      this.tag = tag;
      this.cmp = cmp;
      this.action = action;
      this.ctx = ctx;
    }
    public void tagChanged(TagBase tag, TagID id, String oldValue, String newValue) {
      //NOTE : this function is running in FuntionService - it must return asap
      synchronized(ctx.lock) {
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.id = id;
        ctx.stack.add(this);
        ctx.lock.notify();
      }
    }
  }

  public void addListener(TagAddr ta, TagBase tag, Component cmp, TagAction action) {
    if (tag == null) return;
    MonitoredTag mtag = (MonitoredTag)tag;
    Monitor monitor = new Monitor(ta, mtag, cmp, action, this);
    listeners.add(monitor);
    mtag.addListener(monitor);
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
      monitor.action.tagChanged(monitor.tag, monitor.id, monitor.oldValue, monitor.newValue, monitor.cmp);
    }
  }

  public void cancel() {
    active = false;
    synchronized(lock) {
      lock.notify();
    }
  }
}

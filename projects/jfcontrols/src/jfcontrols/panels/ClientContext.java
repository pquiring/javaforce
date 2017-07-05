package jfcontrols.panels;

/** Monitors tag changes for components on Panel.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.sql.*;
import jfcontrols.tags.*;

public class ClientContext extends Thread {
  private WebUIClient client;
  private ArrayList<Monitor> listeners = new ArrayList<>();
  private volatile boolean active;
  private Object lock = new Object();
  private ArrayList<Monitor> stack = new ArrayList<>();
  private TagsCache tags = new TagsCache();

  public SQL sql;
  public HashMap<String, Component> alarms = new HashMap<>();
  public int lastAlarmID;
  public int debug_en_idx;
  public int debug_tv_idx;
  public DebugContext debug;
  public WatchContext watch;
  public boolean alarmActive;

  public ClientContext(WebUIClient client) {
    this.client = client;
    sql = SQLService.getSQL();
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
    public MonitoredTag tag;
    public ClientContext ctx;
    public String oldValue, newValue;
    public Component cmp;
    public TagAction action;
    public boolean anyChange;

    public Monitor(MonitoredTag tag, Component cmp, TagAction action, ClientContext ctx) {
//      JFLog.log("addListener2:" + tag.getTagID() + "," + tag.getIndex() + "," + tag.getMember() + "," + tag.getMemberIndex() + ":" + this);
      this.tag = tag;
      this.cmp = cmp;
      this.action = action;
      this.ctx = ctx;
    }
    public void tagChanged(TagBase tagBase, TagID id, String oldValue, String newValue) {
      //NOTE : this function is running in FunctionService - it must return asap
      synchronized(ctx.lock) {
        if (!anyChange) {
          if (id.tid != tag.getTagID()) return;
          if (id.mid != tag.getMember()) return;
          if (id.midx != tag.getMemberIndex()) return;
        }
//        JFLog.log("tagChanged:" + tag + ":" + id + ":" + oldValue + ":" + newValue + ":" + this);
        this.oldValue = oldValue;
        this.newValue = newValue;
        ctx.stack.add(this);
        ctx.lock.notify();
      }
    }
  }

  public void addListener(TagBase tag, Component cmp, boolean anyChange, TagAction action) {
    if (tag == null) return;
    MonitoredTag mtag = (MonitoredTag)tag;
    Monitor monitor = new Monitor(mtag, cmp, action, this);
    monitor.anyChange = anyChange;
    listeners.add(monitor);
    mtag.addListener(monitor);
  }

  public void clear() {
    while (listeners.size() > 0) {
      Monitor monitor = listeners.remove(0);
      monitor.tag.removeListener(monitor);
    }
    tags.clear();
    if (debug != null) {
      debug.cancel();
      debug = null;
    }
    if (watch != null) {
      watch.cancel();
      watch = null;
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
//      JFLog.log("tagChanged:" + monitor.tag + ":" + monitor.oldValue + ":" + monitor.newValue);
      monitor.action.tagChanged(monitor.tag, monitor.oldValue, monitor.newValue, monitor.cmp);
    }
  }

  public void cancel() {
    active = false;
    synchronized(lock) {
      lock.notify();
    }
    if (debug != null) {
      debug.cancel();
      debug = null;
    }
    sql.close();
    sql = null;
  }
}

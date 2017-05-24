package jfcontrols.tags;

/** Monitored Tag
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public abstract class MonitoredTag extends Tag {

  private ArrayList<TagListener> listeners = new ArrayList<>();
  private Object lock = new Object();

  public MonitoredTag(String name, int type, SQL sql) {
    super(name, type, sql);
  }

  public abstract void updateRead(SQL sql);
  public abstract void updateWrite(SQL sql);

  public void addListener(TagListener listener) {
    synchronized(lock) {
      listeners.add(listener);
    }
  }

  public void removeListener(TagListener listener) {
    synchronized(lock) {
      listeners.remove(listener);
    }
  }

  public void tagChanged() {
    synchronized(lock) {
      int cnt = listeners.size();
      for(int a=0;a<cnt;a++) {
        listeners.get(0).tagChanged(this, value);
      }
    }
  }
}

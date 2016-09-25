package javaforce.webui;

/** Base class for all components.
 *
 * @author pquiring
 */

import java.util.*;

public abstract class Component {
  public String name;
  public String cls = "";
  private static class Event {
    public String event;
    public String js;
  }
  public ArrayList<Event> events = new ArrayList<Event>();
  public Component() {
    peer = Client.NULL;
  }
  /** Perform any initialization with the client peer. */
  public void init() {}
  public void setClient(Client client) {
    if (name != null) return;
    peer = client;
    name = "c" + client.getNextID();
    init();
  }
  /** Returns HTML to render component. */
  public abstract String html();
  /** Dispatched event. */
  public void dispatchEvent(String event, String args[]) {}
  public Container parent;
  public Client peer;
  /** Component constructor.
   * @param parent = Panel
   * @param name = name of component
   */
  private HashMap<String,Object> map = new HashMap<>();
  /** Set user define property. */
  public void setProperty(String key, Object value) {
    map.put(key, value);
  }
  /** Get user define property. */
  public Object getProperty(String key) {
    return map.get(key);
  }
  public void setClass(String cls) {
    this.cls = cls;
  }
  public void addClass(String cls) {
    if (this.cls.length() > 0) this.cls += " ";
    this.cls += cls;
  }
  private Event getEvent(String onX) {
    int cnt = events.size();
    for(int a=0;a<cnt;a++) {
      Event event = events.get(a);
      if (event.event.equals(onX)) return event;
    }
    return null;
  }
  public void addEvent(String onX, String js) {
    Event event;
    event = getEvent(onX);
    if (event != null) {
      event.js = js;
    } else {
      event = new Event();
      event.event = onX;
      event.js = js;
      events.add(event);
    }
  }
  public String getEvents() {
    StringBuffer sb = new StringBuffer();
    int cnt = events.size();
    for(int a=0;a<cnt;a++) {
      sb.append(' ');
      Event event = events.get(a);
      sb.append(event.event);
      sb.append("='");
      sb.append(event.js);
      sb.append("'");
    }
    return sb.toString();
  }
}

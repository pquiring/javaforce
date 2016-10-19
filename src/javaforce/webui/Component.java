package javaforce.webui;

/** Base class for all components.
 *
 * @author pquiring
 */

import java.util.*;

public abstract class Component {
  public String id;
  public Container parent;
  public Client client;
  public ArrayList<String> classes = new ArrayList<String>();
  public HashMap<String, String> attrs = new HashMap<String, String>();
  public HashMap<String, String> styles = new HashMap<String, String>();

  private static class Event {
    public String event;
    public String js;
  }
  public ArrayList<Event> events = new ArrayList<Event>();

  public Component() {
    client = Client.NULL;
  }

  /** Provides the client (connection to web browser side) and init other variables. */
  public void setClient(Client client) {
    if (id != null) return;
    this.client = client;
    id = "c" + client.getNextID();
    init();
  }
  /** Perform any initialization with the client.
   * Containers should call init() on all sub-components.
   */
  public void init() {}
  /** Returns HTML to render component. */
  public abstract String html();
  /** Dispatches event. */
  public void dispatchEvent(String event, String args[]) {}
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
    classes.clear();
    classes.add(cls);
  }
  public boolean hasClass(String cls) {
    return classes.contains(cls);
  }
  public void addClass(String cls) {
    if (hasClass(cls)) return;
    classes.add(cls);
  }
  public void removeClass(String cls) {
    classes.remove(cls);
  }

  public boolean hasAttr(String attr) {
    return attrs.containsKey(attr);
  }
  public void addAttr(String attr, String value) {
    attrs.put(attr, value);
  }
  public void removeAttr(String attr) {
    attrs.remove(attr);
  }

  public boolean hasStyle(String style) {
    return styles.containsKey(style);
  }
  public void setStyle(String style, String value) {
    styles.put(style, value);
  }
  public void removeStyle(String style) {
    styles.remove(style);
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
  public void setWidth(String width) {
    setStyle("width", width);
  }
  public void setHeight(String height) {
    setStyle("height", height);
  }
  public void setBackColor(String clr) {
    setStyle("background-color", clr);
  }
  public String getAttrs() {
    StringBuffer sb = new StringBuffer();
    sb.append(" id='" + id + "'");
    if (attrs.size() > 0) {
      int size = attrs.size();
      String keys[] = attrs.keySet().toArray(new String[size]);
      String vals[] = attrs.values().toArray(new String[size]);
      for(int a=0;a<size;a++) {
        sb.append(" " + keys[a] + "='" + vals[a] + "'");
      }
    }
    if (classes.size() > 0) {
      sb.append(" class='");
      for(int a=0;a<classes.size();a++) {
        if (a > 0) sb.append(' ');
        sb.append(classes.get(a));
      }
      sb.append("'");
    }
    sb.append(getEvents());
    if (styles.size() > 0) {
      sb.append(" style='");
      int size = styles.size();
      String keys[] = styles.keySet().toArray(new String[size]);
      String vals[] = styles.values().toArray(new String[size]);
      for(int a=0;a<size;a++) {
        sb.append(keys[a] + ":" + vals[a] + ";");
      }
      sb.append("'");
    }
    return sb.toString();
  }
}

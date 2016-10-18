package javaforce.webui;

/** Base class for all components.
 *
 * @author pquiring
 */

import java.util.*;

public abstract class Component {
  public String id;
  public String cls = "";
  public Container parent;
  public Client client;
  public String width, height, backclr;

  public Component() {
    client = Client.NULL;
  }

  private static class Event {
    public String event;
    public String js;
  }
  public ArrayList<Event> events = new ArrayList<Event>();

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
  public void setWidth(String width) {
    this.width = width;
  }
  public void setHeight(String height) {
    this.height = height;
  }
  public void setBackColor(String clr) {
    backclr = clr;
  }
  public String getAttrs() {
    StringBuffer sb = new StringBuffer();
    sb.append(" id='" + id + "' ");
    sb.append(" class='" + cls + "' ");
    sb.append(getEvents());
    sb.append(" style='");
    if (width != null) sb.append("width:" + width + ";");
    if (height != null) sb.append("height:" + height + ";");
    if (backclr != null) sb.append("background-color:" + backclr + ";");
    sb.append("'");
    return sb.toString();
  }
}

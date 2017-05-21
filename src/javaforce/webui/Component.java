package javaforce.webui;

/** Base class for all components.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.webui.event.*;

public abstract class Component {
  public String id;
  public Container parent;
  public WebUIClient client;
  public String name;
  public ArrayList<String> classes = new ArrayList<String>();
  public HashMap<String, String> attrs = new HashMap<String, String>();
  public HashMap<String, String> styles = new HashMap<String, String>();
  public int x,y,width,height;  //position and size (updated with mouseenter event)

  private static class OnEvent {
    public String event;
    public String js;
  }
  public ArrayList<OnEvent> events = new ArrayList<OnEvent>();

  /** Component constructor.
   * @param parent = Panel
   * @param name = name of component
   */
  public Component() {}

  /** Sets Component's name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Gets Component's name. */
  public String getName() {
    return name;
  }

  /** Returns Component's parent Container. */
  public Container getParent() {
    return parent;
  }

  /** Returns Component's top-most parent Container. */
  public Container getTopParent() {
    Container top = parent;
    do {
      Container next = top.getParent();
      if (next == null) return top;
      top = next;
    } while (true);
  }

  /** Provides the client (connection to web browser side) and init other variables. */
  public void setClient(WebUIClient client) {
    if (id != null) return;
    this.client = client;
    id = "c" + client.getNextID();
  }

  /** Returns client. */
  public WebUIClient getClient() {
    return client;
  }

  /** Perform any initialization with the client.
   * Containers should call init() on all sub-components.
   */
  public void init() {}
  /** Returns HTML to render component. */
  public abstract String html();
  private HashMap<String,Object> map = new HashMap<>();
  /** Set user define property. */
  public void setProperty(String key, Object value) {
    map.put(key, value);
  }
  /** Get user define property. */
  public Object getProperty(String key) {
    return map.get(key);
  }
  /** Invokes getClient().sendEvent() */
  public void sendEvent(String event, String args[]) {
    if (client != null) {
      client . sendEvent(id, event, args);
    }
  }
  /** Invokes getClient().sendData() */
  public void sendData(byte data[]) {
    if (client != null) {
      client . sendData(data);
    }
  }
  public void setClass(String cls) {
    classes.clear();
    classes.add(cls);
    sendEvent("setclass", new String [] {"cls=" + cls});
  }
  public boolean hasClass(String cls) {
    return classes.contains(cls);
  }
  public void addClass(String cls) {
    if (hasClass(cls)) return;
    classes.add(cls);
    sendEvent("addclass", new String[] {"cls=" + cls});
  }
  public void removeClass(String cls) {
    classes.remove(cls);
    sendEvent("delclass", new String[] {"cls=" + cls});
  }

  public void setFlex(boolean state) {
    if (state) addClass("pad"); else removeClass("pad");
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

  public void setFontSize(int size) {
    setStyle("font-size", size + "pt");
  }
  public final static int LEFT = 0;
  public final static int CENTER = 1;
  public final static int RIGHT = 2;
  public void setAlign(int align) {
    switch (align) {
      case LEFT:
        setStyle("text-align", "left");
        break;
      case CENTER:
        setStyle("text-align", "center");
        break;
      case RIGHT:
        setStyle("text-align", "right");
        break;
    }
  }

  private OnEvent getEvent(String onX) {
    int cnt = events.size();
    for(int a=0;a<cnt;a++) {
      OnEvent event = events.get(a);
      if (event.event.equals(onX)) return event;
    }
    return null;
  }
  public void addEvent(String onX, String js) {
    OnEvent event;
    event = getEvent(onX);
    if (event != null) {
      event.js = js;
    } else {
      event = new OnEvent();
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
      OnEvent event = events.get(a);
      sb.append(event.event);
      sb.append("='");
      sb.append(event.js);
      sb.append("'");
    }
    return sb.toString();
  }
  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    setStyle("width", width + "px");
    setStyle("height", height + "px");
    sendEvent("setsize", new String[] {"w=" + width, "h=" + height});
  }
  public void setWidth(int width) {
    this.width = width;
    setStyle("width", width + "px");
    sendEvent("setwidth", new String[] {"w=" + width});
  }
  public void setHeight(int height) {
    this.height = height;
    setStyle("height", height + "px");
    sendEvent("setheight", new String[] {"h=" + height});
  }
  public void setColor(String clr) {
    setStyle("color", clr);
    sendEvent("setclr", new String[] {"clr=" + clr});
  }
  public void setBackColor(String clr) {
    setStyle("background-color", clr);
    sendEvent("setbackclr", new String[] {"clr=" + clr});
  }
  public void setBorderColor(String clr) {
    setStyle("border-color", clr);
    sendEvent("setborderclr", new String[] {"clr=" + clr});
  }
  /** Returns all attributes defined for a component (id, attrs, class, styles) */
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

  public String display = "inline-block";

  public void setVisible(boolean state) {
    if (state)
      sendEvent("display", new String[] {"val=" + display});
    else
      sendEvent("display", new String[] {"val=none"});
    if (visible != null) {
      visible.onVisible(this, state);
    }
  }

  public void setPosition(int x, int y) {
    sendEvent("setpos", new String[] {"x=" + x, "y=" + y});
    this.x = x;
    this.y = y;
    setStyle("left", Integer.toString(x));
    setStyle("top", Integer.toString(y));
  }

  public void setReadonly(boolean state) {
    if (state) {
      addAttr("readonly", "readonly");
    } else {
      removeAttr("readonly");
    }
  }

  public void setDisabled(boolean state) {
    if (state) {
      addAttr("disabled", "true");
    } else {
      removeAttr("disabled");
    }
  }

  public void setBorderGray(boolean state) {
    if (state)
      addClass("bordergray");
    else
      removeClass("bordergray");
  }

  public void setBorder(boolean state) {
    if (state)
      addClass("border");
    else
      removeClass("border");
  }

  public void setFocus() {
    sendEvent("focus", null);
  }

  public String toString() {
    return getClass().getName() + ":" + id;
  }

  //event handlers

  /** Dispatches event. */
  public void dispatchEvent(String event, String args[]) {
    MouseEvent e = new MouseEvent();
    for(int a=0;a<args.length;a++) {
      if (args[a].equals("ck=true")) {
        e.ctrlKey = true;
      }
      if (args[a].equals("ak=true")) {
        e.altKey = true;
      }
      if (args[a].equals("sk=true")) {
        e.shiftKey = true;
      }
    }
    switch (event) {
      case "click":
        onClick(args);
        if (click != null) click.onClick(e, this);
        break;
      case "changed":
        onChanged(args);
        if (changed != null) changed.onChanged(this);
        break;
      case "mousedown":
        onMouseDown(args);
        if (mouseDown != null) mouseDown.onMouseDown(this);
        break;
      case "mouseup":
        onMouseUp(args);
        if (mouseUp != null) mouseUp.onMouseUp(this);
        break;
      case "mousemove":
        onMouseMove(args);
        if (mouseMove != null) mouseMove.onMouseMove(this);
        break;
      case "mouseenter":
        onMouseEnter(args);
        if (mouseEnter != null) mouseEnter.onMouseEnter(this);
        break;
      case "possize":
        onPosSize(args);
        break;
      case "pos":
        onPos(args);
        break;
      case "size":
        onSize(args);
        break;
      case "ack":
        onLoaded(args);
        break;
    }
  }

  public void onPosSize(String args[]) {
    for(int c=0;c<args.length;c++) {
      String a = args[c];
      if (a.startsWith("x=")) {
        x = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("y=")) {
        y = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("w=")) {
        width = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("h=")) {
        height = Integer.valueOf(a.substring(2));
      }
    }
  }

  public void onSize(String args[]) {
    for(int c=0;c<args.length;c++) {
      String a = args[c];
      if (a.startsWith("w=")) {
        width = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("h=")) {
        height = Integer.valueOf(a.substring(2));
      }
    }
  }

  public void onPos(String args[]) {
    for(int c=0;c<args.length;c++) {
      String a = args[c];
      if (a.startsWith("x=")) {
        x = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("y=")) {
        y = Integer.valueOf(a.substring(2));
      }
    }
  }

  public void onLoaded(String args[]) {}

  protected void onClick(String args[]) {}
  private Click click;
  public void addClickListener(Click handler) {
    addEvent("onclick", "onClick(event, this);");
    click = handler;
  }

  protected void onMouseUp(String args[]) {}
  private MouseUp mouseUp;
  public void addMouseUpListener(MouseUp handler) {
    mouseUp = handler;
  }

  protected void onMouseDown(String args[]) {}
  private MouseDown mouseDown;
  public void addMouseDownListener(MouseDown handler) {
    mouseDown = handler;
  }

  protected void onMouseMove(String args[]) {}
  private MouseMove mouseMove;
  public void addMouseMoveListener(MouseMove handler) {
    mouseMove = handler;
  }

  protected void onMouseEnter(String args[]) {}
  private MouseEnter mouseEnter;
  public void addMouseEnterListener(MouseEnter handler) {
    mouseEnter = handler;
  }

  protected void onChanged(String args[]) {}
  private Changed changed;
  public void addChangedListener(Changed handler) {
    changed = handler;
  }

  private Visible visible;
  public void addVisibleListener(Visible handler) {
    visible = handler;
  }

  private Validate validate;
  public void addValidateListener(Validate handler) {
    validate = handler;
  }
  public boolean validate() {
    if (validate == null) return true;
    return validate.validate(this);
  }

  private Action action;
  public void addActionListener(Action handler) {
    action = handler;
  }
  public void action() {
    if (action == null) return;
    action.action(this);
  }
}

package javaforce.webui;

/** Base class for all components.
 *
 * @author pquiring
 */

import java.util.*;
import java.net.*;

import javaforce.*;
import javaforce.webui.event.*;
import static javaforce.webui.event.KeyEvent.*;

public abstract class Component {

  public static final Component[] ArrayType = new Component[0];

  public String id;
  public Container parent;
  public WebUIClient client;
  public String name;
  public ArrayList<String> classes = new ArrayList<String>();
  public HashMap<String, String> attrs = new HashMap<String, String>();
  public HashMap<String, String> styles = new HashMap<String, String>();
  public int x,y,width,height;  //position and size (updated with mouseenter event)
  public int clr = 0, backclr = 0xcccccc, borderclr = 0;
//  private boolean drag;
  private int drag_type;
  private int x1, y1, x2, y2;  //drag bounds
  private ArrayList<Event> pendingEvents;

  //define constants
  public static final int VERTICAL = 1;
  public static final int HORIZONTAL = 2;

  public static final int LEFT = 1;
  public static final int CENTER = 2;
  public static final int RIGHT = 3;
  public static final int TOP = 4;
  public static final int BOTTOM = 5;

  //overflow constants
  public static final int VISIBLE = 10;
  public static final int HIDDEN = 11;
  public static final int SCROLL = 12;
  public static final int AUTO = 13;

  public static class Event {
    public String msg;
    public String[] args;
  }

  private static class OnEvent {
    public String event;
    public String js;
  }
  public ArrayList<OnEvent> events = new ArrayList<OnEvent>();

  /** Component constructor.
   */
  public Component() {
  }

  /** Gets Component's ID.*/
  public String getID() {
    return id;
  }

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

  /** Returns client (waits until Component is presented to user). */
  public WebUIClient getClient() {
    while (client == null) {
      JF.sleep(50);
    }
    return client;
  }

  /** Perform any initialization with the client.
   * Containers should call init() on all sub-components.
   */
  public void init() {}

  /** Perform any post loading events.
   * Containers should call events() on all sub-components.
   */
  public void events() {
    if (pendingEvents != null) {
      for(Event event : pendingEvents) {
        sendEvent(event.msg, event.args);
      }
      pendingEvents.clear();
    }
  }

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
  public void sendEvent(String msg, String[] args) {
    if (isLoaded()) {
      client . sendEvent(id, msg, args);
    } else {
      if (pendingEvents == null) pendingEvents = new ArrayList<>();
      Event event = new Event();
      event.msg = msg;
      event.args = args;
      pendingEvents.add(event);
    }
  }
  /** Invokes getClient().sendData() */
  public void sendData(byte[] data) {
    if (isLoaded()) {
      client . sendData(data);
    }
  }
  /** Triggers onresize event on client side. */
  public void sendOnResize() {
    sendEvent("onresize", null);
  }
  public void setClass(String cls) {
    classes.clear();
    classes.add(cls);
    if (isLoaded()) {
      sendEvent("setclass", new String [] {"cls=" + cls});
    }
  }
  public boolean hasClass(String cls) {
    return classes.contains(cls);
  }
  public void addClass(String cls) {
    if (hasClass(cls)) return;
    classes.add(cls);
    if (isLoaded()) {
      sendEvent("addclass", new String[] {"cls=" + cls});
    }
  }
  public void removeClass(String cls) {
    classes.remove(cls);
    if (isLoaded()) {
      sendEvent("delclass", new String[] {"cls=" + cls});
    }
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
  public String getStyle(String style) {
    return styles.get(style);
  }

  public void setFontSize(int size) {
    setStyle("font-size", size + "pt");
  }

  public void setAlign(int align) {
    switch (align) {
      case LEFT:
        setStyle("text-align", "left");
        break;
      case CENTER:
        setStyle("text-align", "center");
        setStyle("margin", "0 auto");
        break;
      case RIGHT:
        setStyle("text-align", "right");
        break;
    }
  }

  public void setVerticalAlign(int align) {
    switch (align) {
      case TOP:
        setStyle("vertical-align", "top");
        break;
      case CENTER:
        setStyle("vertical-align", "middle");
        break;
      case BOTTOM:
        setStyle("vertical-align", "bottom");
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
      event.js = js.replaceAll("'", "\"");
    } else {
      event = new OnEvent();
      event.event = onX;
      event.js = js.replaceAll("'", "\"");
      events.add(event);
    }
  }
  public String getEvents() {
    StringBuilder sb = new StringBuilder();
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
  public void requestPos() {
    sendEvent("getpos", null);
  }
  public void requestPosSize() {
    sendEvent("getpossize", null);
  }
  public void requestSize() {
    sendEvent("getsize", null);
  }
  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
    if (width > 0) {setStyle("width", width + "px");}
    if (height > 0) {setStyle("height", height + "px");}
    if (isLoaded()) {
      sendEvent("setsize", new String[] {"w=" + width, "h=" + height});
    }
  }
  public int getX() {
    return x;
  }
  public int getWidth() {
    return width;
  }
  public void setWidth(int width) {
    this.width = width;
    setStyle("width", width + "px");
    if (isLoaded()) {
      sendEvent("setwidth", new String[] {"w=" + width});
    }
  }
  public void setMaxWidth() {
    setStyle("width", "100%");
  }
  public void setAutoWidth() {
    setStyle("width", "auto");
  }
  public int getY() {
    return y;
  }
  public int getHeight() {
    return height;
  }
  public void setHeight(int height) {
    this.height = height;
    setStyle("height", height + "px");
    if (isLoaded()) {
      sendEvent("setheight", new String[] {"h=" + height});
    }
  }
  public void setMaxHeight() {
    setStyle("height", "100%");
  }
  public void setAutoHeight() {
    setStyle("height", "auto");
  }
  public void setColor(int clr) {
    this.clr = clr;
    String style = String.format("#%06x", clr);
    setStyle("color", style);
    if (isLoaded()) {
      sendEvent("setclr", new String[] {"clr=" + style});
    }
  }
  public int getColor() {
    return clr;
  }
  public void setBackColor(int clr) {
    this.backclr = clr;
    String style = String.format("#%06x", clr);
    setStyle("background-color", style);
    if (isLoaded()) {
      sendEvent("setbackclr", new String[] {"clr=" + style});
    }
  }
  public int getBackColor() {
    return backclr;
  }
  public void setBorderColor(int clr) {
    this.borderclr = clr;
    String style = String.format("#%06x", clr);
    setStyle("border-color", style);
    if (isLoaded()) {
      sendEvent("setborderclr", new String[] {"clr=" + style});
    }
  }
  public int getBorderColor() {
    return borderclr;
  }
  /** Returns all attributes defined for a component (id, attrs, class, styles) */
  public String getAttrs() {
    StringBuilder sb = new StringBuilder();
    sb.append(" id='" + id + "'");
    if (attrs.size() > 0) {
      int size = attrs.size();
      String[] keys = attrs.keySet().toArray(new String[size]);
      String[] vals = attrs.values().toArray(new String[size]);
      for(int a=0;a<size;a++) {
        if (vals[a] == null) {
          sb.append(" " + keys[a]);
        } else {
          sb.append(" " + keys[a] + "='" + vals[a] + "'");
        }
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
      String[] keys = styles.keySet().toArray(new String[size]);
      String[] vals = styles.values().toArray(new String[size]);
      for(int a=0;a<size;a++) {
        sb.append(keys[a] + ":" + vals[a] + ";");
      }
      sb.append("'");
    }
    return sb.toString();
  }

  private boolean isVisible = true;

  /** Sets component to initial invisible. */
  protected void initInvisible() {
    isVisible = false;
    setStyle("display", "none");
  }

  public void setVisible(boolean state) {
    isVisible = state;
    if (state) {
      if (isLoaded()) {
        sendEvent("display", new String[] {"val="});  //reset to css rules
      }
      setStyle("display", "");
    } else {
      if (isLoaded()) {
        sendEvent("display", new String[] {"val=none"});
      }
      setStyle("display", "none");
    }
    for(int a=0;a<visible.length;a++) {
      visible[a].onVisible(this, state);
    }
  }

  public boolean isLoaded() {
    return client != null;
  }

  public boolean isVisible() {
    return isVisible;
  }

  public boolean isPopup() {
    return false;
  }

  public void setPosition(int x, int y) {
    if (isLoaded()) {
      sendEvent("setpos", new String[] {"x=" + x, "y=" + y});
    }
    this.x = x;
    this.y = y;
    setStyle("left", x + "px");
    setStyle("top", y + "px");
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

  public void settransform(String transform) {
    setStyle("transform", transform);
    if (isLoaded()) {
      sendEvent("settransform", new String[] {"transform=" + transform});
    }
  }

  public String gettransform() {
    return getStyle("transform");
  }

  private String scale;

  public void setscale(float x, float y) {
    scale = "scale(" + x + "," + y + ")";
    settransform(build_transform());
  }

  private String rotate;

  public void setrotate(float deg) {
    rotate = "rotate(" + deg + "deg)";
    settransform(build_transform());
  }

  private String translate;

  public void settranslate(float x, float y) {
    translate = "translate(" + x + "px," + y + "px)";
    settransform(build_transform());
  }

  private String build_transform() {
    StringBuilder transform = new StringBuilder();
    if (scale != null) {
      transform.append(scale);
    }
    if (rotate != null) {
      if (transform.length() > 0) {
        transform.append(" ");
      }
      transform.append(rotate);
    }
    if (translate != null) {
      if (transform.length() > 0) {
        transform.append(" ");
      }
      transform.append(translate);
    }
    return transform.toString();
  }

  public static final int DRAG_MOVE = 1;
  public static final int DRAG_NS = 2;
  public static final int DRAG_EW = 3;
  public static final int DRAG_NESW = 4;
  public static final int DRAG_NWSE = 5;

  /** Enables dragging a component.
   * drag_type = DRAG_... types (sets cursor type)
   * coords = (x1,y1)-(x2,y2) = dragging bounds (-1=no bounds)
   */
  public void enableDragging(int drag_type, int x1, int y1, int x2, int y2) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    switch (drag_type) {
      case DRAG_MOVE: addClass("drag_move"); break;
      case DRAG_NS: addClass("drag_ns_resize"); break;
      case DRAG_EW: addClass("drag_ew_resize"); break;
      case DRAG_NESW: addClass("drag_nesw_resize"); break;
      case DRAG_NWSE: addClass("drag_nwse_resize"); break;
    }
    sendEvent("enabledrag", new String[] {"x1=" + x1,"y1=" + y1,"x2=" + x2,"y2=" + y2, "type=" + drag_type});
  }

  public String encode(String str) {
    try {
      return URLEncoder.encode(str, "utf-8");
    } catch (Exception e) {
      return null;
    }
  }

  public String decode(String url) {
    try {
      return URLDecoder.decode(url, "utf-8");
    } catch (Exception e) {
      return null;
    }
  }

  public String toString() {
    return getClass().getName() + ":" + id;
  }

  //event handlers

  /** Dispatches event. */
  public void dispatchEvent(String event, String[] args) {
    MouseEvent me = new MouseEvent();
    KeyEvent ke = new KeyEvent();
    if (args != null) {
      for(int a=0;a<args.length;a++) {
        String arg = args[a];
        int idx = arg.indexOf('=');
        if (idx == -1) continue;
        String key = arg.substring(0, idx);
        String value = arg.substring(idx + 1);
        boolean bin;
        switch (key) {
          case "ck":
            bin = value.equals("true");
            me.ctrlKey = bin;
            ke.ctrlKey = bin;
            break;
          case "ak":
            bin = value.equals("true");
            me.altKey = bin;
            ke.altKey = bin;
            break;
          case "sk":
            bin = value.equals("true");
            me.shiftKey = bin;
            ke.shiftKey = bin;
            break;
          case "key": {
            int keyCode = 0;
            char keyChar = (char)0;
            if (value.length() == 1) {
              keyChar = value.charAt(0);
              keyCode = Character.toUpperCase(value.charAt(0));
            } else {
              switch (value) {
                case "Enter": keyCode = VK_ENTER; break;
                case "Tab": keyCode = VK_TAB; break;
                case "Control": keyCode = VK_CONTROL; break;
                case "Alt": keyCode = VK_ALT; break;
                case "Shift": keyCode = VK_SHIFT; break;
                case "Escape": keyCode = VK_ESCAPE; break;
                case "ArrowUp": keyCode = VK_ARROW_UP; break;
                case "ArrowDown": keyCode = VK_ARROW_DOWN; break;
                case "ArrowLeft": keyCode = VK_ARROW_LEFT; break;
                case "ArrowRight": keyCode = VK_ARROW_RIGHT; break;
                case "Insert": keyCode = VK_INSERT; break;
                case "Delete": keyCode = VK_DELETE; break;
                case "Home": keyCode = VK_HOME; break;
                case "End": keyCode = VK_END; break;
                case "PageUp": keyCode = VK_PAGE_UP; break;
                case "PageDown": keyCode = VK_PAGE_DOWN; break;
                case "\\\\": keyChar = '\\'; break;
                case "Backspace": keyCode = VK_BACK_SPACE; break;
                default: JFLog.log("webui:unknown key code:" + value);
              }
            }
            ke.keyChar = keyChar;
            ke.keyCode = keyCode;
            break;
          }
          case "x":
            me.x = Integer.valueOf(value);
            break;
          case "y":
            me.y = Integer.valueOf(value);
            break;
          case "b":
            me.buttons = Integer.valueOf(value);
            break;
        }
      }
    }
    switch (event) {
      case "click":
        onClick(args, me);
        break;
      case "action":
        action();
        break;
      case "changed":
        onChanged(args);
        break;
      case "mousedown":
        onMouseDown(args, me);
        break;
      case "mouseup":
        onMouseUp(args, me);
        break;
      case "mousemove":
        onMouseMove(args, me);
        break;
      case "mouseenter":
        onMouseEnter(args, me);
        break;
      case "keydown":
        onKeyDown(args, ke);
        break;
      case "keyup":
        onKeyUp(args, ke);
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
      case "onloaded":
        onLoaded(args);
        break;
      case "drawrect":
        onDrawRect(args);
        break;
      default:
        onEvent(event, args);
        break;
    }
  }

  /** Process custom events. */
  public void onEvent(String event, String[] args) {}

  public void onPosSize(String[] args) {
    for(int a=0;a<moved.length;a++) {
      moved[a].onMoved(this, x, y);
    }
    for(int a=0;a<resized.length;a++) {
      resized[a].onResized(this, width, height);
    }
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

  public void onSize(String[] args) {
    for(int c=0;c<args.length;c++) {
      String a = args[c];
      if (a.startsWith("w=")) {
        width = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("h=")) {
        height = Integer.valueOf(a.substring(2));
      }
    }
    for(int a=0;a<resized.length;a++) {
      resized[a].onResized(this, width, height);
    }
  }

  public void onPos(String[] args) {
    for(int c=0;c<args.length;c++) {
      String a = args[c];
      if (a.startsWith("x=")) {
        x = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("y=")) {
        y = Integer.valueOf(a.substring(2));
      }
    }
    for(int a=0;a<moved.length;a++) {
      moved[a].onMoved(this, x, y);
    }
  }

  protected void onLoaded(String[] args) {
    for(int a=0;a<loaded.length;a++) {
      loaded[a].loaded(this);
    }
  }
  private Loaded[] loaded = new Loaded[0];
  public void addLoadedListener(Loaded handler) {
    loaded = Arrays.copyOf(loaded, loaded.length + 1);
    loaded[loaded.length-1] = handler;
  }

  protected void onClick(String[] args, MouseEvent me) {
    if (WebUIServer.debug) JFLog.log("onClick:" + this + ":" + click.length);
    for(int a=0;a<click.length;a++) {
      click[a].onClick(me, this);
    }
  }
  private Click[] click = new Click[0];
  public void addClickListener(Click handler) {
    addEvent("onclick", "onClick(event, this);");
    addEvent("oncontextmenu", "return false;");
    click = Arrays.copyOf(click, click.length + 1);
    click[click.length-1] = handler;
  }
  public void click() {
    onClick(null, new MouseEvent());
  }

  protected void onMouseUp(String[] args, MouseEvent me) {
    me.action = "up";
    for(int a=0;a<mouseUp.length;a++) {
      mouseUp[a].onMouseUp(me, this);
    }
  }
  private MouseUp[] mouseUp = new MouseUp[0];
  public void addMouseUpListener(MouseUp handler) {
    addEvent("onmouseup", "onMouseUp(event, this);");
    addEvent("oncontextmenu", "return false;");
    mouseUp = Arrays.copyOf(mouseUp, mouseUp.length + 1);
    mouseUp[mouseUp.length-1] = handler;
  }

  protected void onMouseDown(String[] args, MouseEvent me) {
    me.action = "down";
    for(int a=0;a<mouseDown.length;a++) {
      mouseDown[a].onMouseDown(me, this);
    }
  }
  private MouseDown[] mouseDown = new MouseDown[0];
  public void addMouseDownListener(MouseDown handler) {
    addEvent("onmousedown", "onMouseDown(event, this);");
    addEvent("oncontextmenu", "return false;");
    mouseDown = Arrays.copyOf(mouseDown, mouseDown.length + 1);
    mouseDown[mouseDown.length-1] = handler;
  }

  protected void onMouseMove(String[] args, MouseEvent me) {
    me.action = "move";
    for(int a=0;a<mouseMove.length;a++) {
      mouseMove[a].onMouseMove(me, this);
    }
  }
  private MouseMove[] mouseMove = new MouseMove[0];
  public void addMouseMoveListener(MouseMove handler) {
    addEvent("onmousemove", "onMouseMove(event, this);");
    mouseMove = Arrays.copyOf(mouseMove, mouseMove.length + 1);
    mouseMove[mouseMove.length-1] = handler;
  }

  protected void onMouseEnter(String[] args, MouseEvent me) {
    me.action = "enter";
    for(int a=0;a<mouseEnter.length;a++) {
      mouseEnter[a].onMouseEnter(me, this);
    }
  }
  private MouseEnter[] mouseEnter = new MouseEnter[0];
  public void addMouseEnterListener(MouseEnter handler) {
    //TODO : addEvent() ???
    mouseEnter = Arrays.copyOf(mouseEnter, mouseEnter.length + 1);
    mouseEnter[mouseEnter.length-1] = handler;
  }

  protected void onKeyUp(String[] args, KeyEvent ke) {
    ke.action = "up";
    for(int a=0;a<keyUp.length;a++) {
      keyUp[a].onKeyUp(ke, this);
    }
  }
  private KeyUp[] keyUp = new KeyUp[0];
  public void addKeyUpListener(KeyUp handler) {
    addEvent("onkeyup", "onKeyUp(event, this);");
    keyUp = Arrays.copyOf(keyUp, keyUp.length + 1);
    keyUp[keyUp.length-1] = handler;
  }
  public void addKeyUpListenerPreventDefault(KeyUp handler) {
    addEvent("onkeyup", "onKeyUp2(event, this);");
    keyUp = Arrays.copyOf(keyUp, keyUp.length + 1);
    keyUp[keyUp.length-1] = handler;
  }

  protected void onKeyDown(String[] args, KeyEvent ke) {
    ke.action = "down";
    for(int a=0;a<keyDown.length;a++) {
      keyDown[a].onKeyDown(ke, this);
    }
  }
  private KeyDown[] keyDown = new KeyDown[0];
  public void addKeyDownListener(KeyDown handler) {
    addEvent("onkeydown", "onKeyDown(event, this);");
    keyDown = Arrays.copyOf(keyDown, keyDown.length + 1);
    keyDown[keyDown.length-1] = handler;
  }
  public void addKeyDownListenerPreventDefault(KeyDown handler) {
    addEvent("onkeydown", "onKeyDown2(event, this);");
    keyDown = Arrays.copyOf(keyDown, keyDown.length + 1);
    keyDown[keyDown.length-1] = handler;
  }

  protected void onChanged(String[] args) {
    for(int a=0;a<changed.length;a++) {
      changed[a].onChanged(this);
    }
  }
  private Changed[] changed = new Changed[0];
  public void addChangedListener(Changed handler) {
    changed = Arrays.copyOf(changed, changed.length + 1);
    changed[changed.length-1] = handler;
  }

  private Visible[] visible = new Visible[0];
  public void addVisibleListener(Visible handler) {
    visible = Arrays.copyOf(visible, visible.length + 1);
    visible[visible.length-1] = handler;
  }

  private Validate[] validate = new Validate[0];
  public void addValidateListener(Validate handler) {
    validate = Arrays.copyOf(validate, validate.length + 1);
    validate[validate.length-1] = handler;
  }
  public boolean validate() {
    for(int a=0;a<validate.length;a++) {
      if (!validate[a].validate(this)) return false;
    }
    return true;
  }

  private Action[] action = new Action[0];
  public void addActionListener(Action handler) {
    action = Arrays.copyOf(action, action.length + 1);
    action[action.length-1] = handler;
  }
  public void action() {
    for(int a=0;a<action.length;a++) {
      action[a].action(this);
    }
  }

  private Resized[] resized = new Resized[0];
  public void addResizedListener(Resized handler) {
    addEvent("onresize", "onResize(event, this);");
    resized = Arrays.copyOf(resized, resized.length + 1);
    resized[resized.length-1] = handler;
  }

  private Moved[] moved = new Moved[0];
  public void addMovedListener(Moved handler) {
    addEvent("onmoved", "onMoved(event, this);");
    moved = Arrays.copyOf(moved, moved.length + 1);
    moved[moved.length-1] = handler;
  }

  public void onDrawRect(String[] args) {
    Rectangle rect = new Rectangle();
    for(int c=0;c<args.length;c++) {
      String a = args[c];
      if (a.startsWith("x=")) {
        rect.x = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("y=")) {
        rect.y = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("w=")) {
        rect.width = Integer.valueOf(a.substring(2));
      }
      if (a.startsWith("h=")) {
        rect.height = Integer.valueOf(a.substring(2));
      }
    }
    onDrawRect(rect);
  }
  public void onDrawRect(Rectangle rect) {}
}

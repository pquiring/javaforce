package javaforce.webui;

/** Base class for components that can "contain" other components.
 *
 * @author pquiring
 */

import java.util.*;

public abstract class Container extends Component {
  public void setClient(WebUIClient client) {
    super.setClient(client);
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      get(a).setClient(client);
    }
  }
  public void init() {
    super.init();
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      get(a).init();
    }
  }
  public Component getComponent(String name) {
    if (this.name != null && this.name.equals(name)) {
      return this;
    }
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Component child = get(a);
      if (child instanceof Container) {
        Container container = (Container)child;
        child = container.getComponent(name);
        if (child != null) return child;
      } else {
        if (child.name != null && child.name.equals(name)) {
          return child;
        }
      }
    }
    return null;
  }
  private ArrayList<Component> components = new ArrayList<Component>();
  public Component get(int idx) {
    return components.get(idx);
  }
  public Component get(String name) {
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Component comp = get(a);
      if (comp.id.equals(name)) return comp;
      if (comp instanceof Container) {
        Container container = (Container)comp;
        comp = container.get(name);
        if (comp != null) return comp;
      }
    }
    return null;
  }
  public Component[] getAll() {
    return components.toArray(new Component[count()]);
  }
  public void set(int idx, Component c) {
    components.set(idx, c);
  }
  /** Add component to end of components. */
  public void add(Component comp) {
    comp.parent = this;
    components.add(comp);
    if (client != null) {
      comp.setClient(client);
      comp.init();
    }
    if (id != null) {
      sendEvent("add", new String[] {"html=" + comp.html()});
    }
  }
  /** Add component at index. */
  public void add(int idx, Component comp) {
    comp.parent = this;
    Component before = (idx < 0 || idx >= count()) ? null : components.get(idx);
    components.add(idx, comp);
    if (client != null) {
      comp.setClient(client);
      comp.init();
    }
    if (id != null) {
      if (before == null)
        sendEvent("add", new String[] {"html=" + comp.html()});
      else
        sendEvent("addbefore", new String[] {"html=" + comp.html(), "beforeid=" + before.id});
    }
  }
  public void remove(Component comp) {
    components.remove(comp);
  }
  public void remove(int idx) {
    components.remove(idx);
  }
  public void removeAll() {
    components.clear();
  }
  public int count() {
    return components.size();
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}

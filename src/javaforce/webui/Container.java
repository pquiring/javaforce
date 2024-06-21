package javaforce.webui;

/** Base class for components that can "contain" other components.
 *
 * @author pquiring
 */

import java.util.*;

public class Container extends Component {
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
  public void events() {
    super.events();
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      get(a).events();
    }
  }
  /** Get Component by user assigned name. */
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
  /** Get Component by id. */
  public Component get(String id) {
    if (id.equals(this.id)) return this;
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Component comp = get(a);
      if (comp.id == null) continue;
      if (comp.id.equals(id)) return comp;
      if (comp instanceof Container) {
        Container container = (Container)comp;
        comp = container.get(id);
        if (comp != null) return comp;
      }
    }
    return null;
  }
  public Component[] getAll() {
    return components.toArray(new Component[count()]);
  }
  public void set(int idx, Component c) {
    remove(idx);
    add(idx, c);
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
      comp.events();
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
      comp.events();
    }
  }
  public void remove(Component comp) {
    components.remove(comp);
    if (id != null) {
      sendEvent("remove", new String[] {"child=" + comp.id});
    }
  }
  public void remove(int idx) {
    Component comp = components.remove(idx);
    if (id != null) {
      sendEvent("remove", new String[] {"child=" + comp.id});
    }
  }
  public void removeAll() {
    while (count() > 0) {
      remove(0);
    }
  }
  public int count() {
    return components.size();
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
}

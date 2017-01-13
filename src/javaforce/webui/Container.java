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
  public void set(int idx, Component c) {
    components.set(idx, c);
  }
  public void add(Component comp) {
    comp.parent = this;
    components.add(comp);
    if (client != WebUIClient.NULL) {
      comp.setClient(client);
      comp.init();
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
}

package javaforce.webui;

/** Base class for components that can "contain" other components.
 *
 * @author pquiring
 */

import java.util.*;

public abstract class Container extends Component {
  public void setClient(Client client) {
    super.setClient(client);
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      get(a).setClient(client);
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
  public void add(Component comp) {
    comp.parent = this;
    components.add(comp);
    if (peer != Client.NULL) {
      comp.setClient(peer);
    }
  }
  public void remove(Component comp) {
    components.remove(comp);
  }
  public void remove(int idx) {
    components.remove(idx);
  }
  public int count() {
    return components.size();
  }
}

package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_seat object.
 *
 * @author pquiring
 */

public class WLSeat extends WLObject {
  @SuppressWarnings("unchecked")
  public WLSeat(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("get_pointer", new Class[] {int.class}),
        cls.getMethod("get_keyboard", new Class[] {int.class}),
        cls.getMethod("get_touch", new Class[] {int.class}),
        cls.getMethod("release", new Class[] {}),
      };
      events = new Method[] {
        cls.getMethod("capabilities", new Class[] {int.class}),
        cls.getMethod("name", new Class[] {String.class}),
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private String name;
  private int caps;

  public String get_wl_name() {
    return "wl_seat";
  }

  public String getName() {
    return name;
  }

  public int getCapabilities() {
    return caps;
  }

  //requests

  public void get_pointer(int wl_pointer) {
    invokeRequest(id, 0, wl_pointer);
  }

  public void get_keyboard(int wl_keyboard) {
    invokeRequest(id, 1, wl_keyboard);
  }

  public void get_touch(int wl_touch) {
    invokeRequest(id, 2, wl_touch);
  }

  public void release() {
    invokeRequest(id, 3);
  }

  //events

  public void capabilities(int caps) {
    this.caps = caps;
  }

  public void name(String name) {
    this.name = name;
  }
}

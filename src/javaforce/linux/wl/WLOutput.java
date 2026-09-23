package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_output object.
 *
 * @author pquiring
 */

public class WLOutput extends WLObject {
  @SuppressWarnings("unchecked")
  public WLOutput(WLClient client, int id) {
    super(client, id);
    setVersion(4);
    Class cls = getClass();
    try {
      requests = new Method[] {
        cls.getMethod("release", new Class[] {}),
      };
      events = new Method[] {
        cls.getMethod("geometry", new Class[] {int.class, int.class, int.class, int.class, int.class, String.class, String.class, int.class}),
        cls.getMethod("mode", new Class[] {int.class, int.class, int.class, int.class}),
        cls.getMethod("done", new Class[] {}),
        cls.getMethod("scale", new Class[] {int.class}),
        cls.getMethod("name", new Class[] {String.class}),
        cls.getMethod("description", new Class[] {String.class}),
      };
    } catch (Exception e) {
      client.log(e);
    }
  }

  public String get_wl_name() {
    return "wl_output";
  }

  //requests

  //V3

  public void release() {
    if (debug) client.log("WLOutput.release");
    invokeRequest(id, 0);
  }

  //events

  public void geometry(int x, int y, int physical_width, int physical_height, int subpixel, String make, String model, int transform) {
    if (debug) client.log("WLOutput.geometry");
  }

  public void mode(int flags, int width, int height, int refresh) {
    if (debug) client.log("WLOutput.mode");
  }

  //V2

  public void done() {
    if (debug) client.log("WLOutput.done");
  }

  public void scale(int factor) {
    if (debug) client.log("WLOutput.scale");
  }

  //V4

  public void name(String name) {
    if (debug) client.log("WLOutput.name:" + name);
  }

  public void description(String desc) {
    if (debug) client.log("WLOutput.description:" + desc);
  }
}

package javaforce.webui;

/** Canvas for 2D and 3D output.
 *
 * @author pquiring
 */

public class Canvas extends Container {
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<canvas" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</canvas>");
    return sb.toString();
  }
  /** Tells the client to start WebGL on canvas. */
  public void initWebGL() {
    getClient().sendEvent(id, "initwebgl", null);
  }
}

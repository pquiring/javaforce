package javaforce.webui;

/** Test WebUI / WebGL.
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class TestGL implements WebUIHandler {
  public Resource img;

  public TestGL() {
  }

  public static void main(String args[]) {
    new WebUIServer().start(new TestGL(), 8080, false);
  }

  public byte[] getResource(String url) {
    //TODO : return static images, etc needed by webpage
    return null;
  }

  public Panel getRootPanel(WebUIClient client) {
    Panel panel = new Panel() {
      public void onLoaded(String args[]) {
        Canvas canvas = (Canvas)getProperty("canvas");
        getClient().sendEvent(canvas.id, "initwebgl", null);
      }
    };

    Canvas canvas = new Canvas();
    panel.add(canvas);

    panel.setProperty("canvas", canvas);

    return panel;
  }
}

package javaforce.webui;

/** Test WebUI.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.webui.event.*;
import javaforce.service.*;

public class TestMedia implements WebUIHandler {

  public TestMedia() {
  }

  public static void main(String[] args) {
    new WebUIServer().start(new TestMedia(), 8080);
  }

  public void clientConnected(WebUIClient client) {}
  public void clientDisconnected(WebUIClient client) {
    System.exit(0);
  }

  public byte[] getResource(String url, HTTP.Parameters params, WebResponse res) {
    //TODO : return static images, etc needed by webpage
    return null;
  }

  public Panel getPanel(String name, HTTP.Parameters params, WebUIClient client) {
    Panel panel = new Panel();

    Video video = new Video();
    video.setWidth(256);
    video.setHeight(256);
    panel.add(video);

    Button b = new Button("Start");
    panel.add(b);
    b.addClickListener(new Click() {
      public void onClick(MouseEvent me, Component c) {
        video.sendEvent("video_init", null);
      }
    });

    return panel;
  }
}

package javaforce.webui.panel;

import javaforce.webui.*;

/** IFrame in a Panel
 *
 * @author pquiring
 */

public class IFramePanel extends Panel {
  private IFrame frame;
  public IFramePanel(String url) {
    frame = new IFrame(url);
    frame.setMaxWidth();
    frame.setMaxHeight();
    add(frame);
  }
}

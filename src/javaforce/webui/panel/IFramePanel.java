package javaforce.webui.panel;

/** IFrame in a Panel
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class IFramePanel extends Panel {
  private IFrame frame;
  public IFramePanel(String url) {
    frame = new IFrame(url);
    frame.setMaxWidth();
    frame.setMaxHeight();
    add(frame);
  }
}

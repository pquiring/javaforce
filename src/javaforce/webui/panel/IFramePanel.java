package javaforce.webui.panel;

/** IFrame in a Panel
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class IFramePanel extends Panel {
  public IFramePanel(String url) {
    add(new IFrame(url));
  }
}

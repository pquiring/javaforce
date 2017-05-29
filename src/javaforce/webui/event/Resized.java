package javaforce.webui.event;

/** Resized handler.
 *
 * @author pquiring
 */

import javaforce.webui.*;

public interface Resized {
  public void onResized(Component comp, int width, int height);
}

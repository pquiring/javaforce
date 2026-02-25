package javaforce.ui;

/** UI Events
 *
 * @author pquiring
 */

import java.lang.foreign.*;

public interface UIEvents {
  public void dispatchEvent(int type, int v1, int v2);
}

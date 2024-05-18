package javaforce.webui.event;

/** Mouse Event details
 *
 * @author pquiring
 */

public class MouseEvent {
  public String action;  //down, up, move
  public boolean ctrlKey, altKey, shiftKey;
  public int x, y;
  public int buttons;
}

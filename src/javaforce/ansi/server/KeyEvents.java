package javaforce.ansi.server;

/** KeyEvents.
 *
 * @author pquiring
 */

public interface KeyEvents {

  public void keyPressed(int keyCode, int keyMods);

  public void keyTyped(char key);

}

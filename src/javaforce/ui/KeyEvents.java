package javaforce.ui;

/** KeyEvents
 *
 * @author pquiring
 */

public interface KeyEvents extends KeyCode {
  public void keyTyped(char ch);

  public void keyPressed(int key);

  public void keyReleased(int key);
}

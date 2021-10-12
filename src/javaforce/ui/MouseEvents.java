package javaforce.ui;

/** MouseEvents
 *
 * @author pquiring
 */

public interface MouseEvents {
  public void mouseMove(int x, int y);

  public void mouseDown(int button);

  public void mouseUp(int button);

  public void mouseScroll(int x, int y);
}

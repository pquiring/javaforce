package javaforce.ansi;

/** Dialog interface
 *
 * @author peterq.admin
 */

public interface Dialog {
  public void draw();
  public void keyPressed(int keyCode, int keyMods);
  public void keyTyped(char key);
  public boolean isClosed();
  public void setClosed(boolean closed);
}

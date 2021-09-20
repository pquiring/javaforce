package javaforce.awt;

/**
 * The interface used by ReplaceDialog for event handling.
 *
 * @author Peter Quiring
 */
public interface ReplaceEvent {

  public boolean findEvent(ReplaceDialog dialog);

  public void replaceEvent(ReplaceDialog dialog);

  public void replaceAllEvent(ReplaceDialog dialog);
}

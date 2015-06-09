package javaforce;

import javaforce.JFTimer;

/**
 * The interface used by JFTimer for event handling.
 *
 * @author Peter Quiring
 */
public interface TimerEvent {

  public void timerEvent(JFTimer id);
}

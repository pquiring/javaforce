package javaforce;

import javaforce.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simpler to use Timer, that uses a callback interface.
 *
 * @see TimerEvent
 * @author Peter Quiring
 */
public class JFTimer {

  private Timer timer;
  private TimerEvent timerevent = null;

  private class timertask extends TimerTask {

    private JFTimer jftimer;

    public timertask(JFTimer t) {
      jftimer = t;
    }

    public void run() {
      timerevent.timerEvent(jftimer);
    }
  }

  public JFTimer() {
  }

  public JFTimer(TimerEvent eh) {
    timerevent = eh;
  }

  public void setTimerEvent(TimerEvent eh) {
    timerevent = eh;
  }

  public void start(long delay) {
    if (timer != null) {
      stop();
    }
    timer = new java.util.Timer();
    timer.schedule(new timertask(this), delay, delay);
  }

  public void startOneTime(long delay) {
    if (timer != null) {
      stop();
    }
    timer = new java.util.Timer();
    timer.schedule(new timertask(this), delay);
  }

  public void stop() {
    timer.cancel();
    timer = null;
  }
}

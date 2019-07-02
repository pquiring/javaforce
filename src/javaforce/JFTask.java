package javaforce;

/**
 * Created : Mar 12, 2012
 *
 * @author pquiring
 */

import java.util.*;

/**
 * A special Thread (task) for ProgressDialog.<br> You should override work();
 */
public abstract class JFTask extends Thread implements ShellProcessListener {

  private JFTaskListener listener;
  private boolean status;
  private HashMap<String, Object> properties = new HashMap<String, Object>();

  public volatile boolean abort = false;

  public void start(JFTaskListener pd) {
    this.listener = pd;
    start();
  }

  public void run() {
    status = work();
    if (listener != null) listener.done();
  }

  public void abort() {
    abort = true;
  }

  public void dispose() {
    if (listener != null) listener.dispose();
  }

  public abstract boolean work();

  public boolean getStatus() {
    return status;
  }

  public void setLabel(String txt) {
    listener.setLabel(txt);
  }

  public void setTitle(String txt) {
    listener.setTitle(txt);
  }

  public void setProgress(int value) {
    listener.setProgress(value);
  }

  public void shellProcessOutput(String out) {
  }

  public void setProperty(String key, Object value) {
    properties.put(key, value);
  }

  public Object getProperty(String key) {
    return properties.get(key);
  }
}

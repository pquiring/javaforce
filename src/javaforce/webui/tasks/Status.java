package javaforce.webui.tasks;

import javaforce.*;

/** Task status callback.
 *
 * @author pquiring
 */

public interface Status {
  public void setStatus(String msg);
  public void setPercent(int value);
  public void setResult(String msg, boolean success);
  public void setResult(boolean success);
  public static Status null_status = new Status() {
    public void setStatus(String msg) {
      JFLog.log(msg);
    }
    public void setPercent(int value) {
    }
    public void setResult(String msg, boolean result) {
    }
    public void setResult(boolean success) {
    }
  };
}

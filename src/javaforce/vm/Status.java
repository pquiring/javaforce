package javaforce.vm;

/** Task status callback.
 *
 * @author pquiring
 */

public interface Status {
  public void setStatus(String msg);
  public void setPercent(int value);
  public void setResult(boolean result);
  public static Status null_status = new Status() {
    public void setStatus(String msg) {
    }
    public void setPercent(int value) {
    }
    public void setResult(boolean result) {
    }
  };
}

package javaforce;

/** Convenience thread to pass in value via ctor.
 *
 * Extend and override run() as in Thread usage.
 *
 * @author pquiring
 */

public class JFThread<T> extends Thread {
  public T value;
  public JFThread(T value) {
    this.value = value;
  }
  public T getValue() {
    return value;
  }
  public void setValue(T value) {
    this.value = value;
  }
}

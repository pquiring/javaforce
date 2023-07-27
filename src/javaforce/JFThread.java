package javaforce;

/** Convenience thread to pass in value via ctor.
 *
 * @author pquiring
 */

public class JFThread<T> extends Thread {
  public T user;
  public JFThread(T user) {
    this.user = user;
  }
}

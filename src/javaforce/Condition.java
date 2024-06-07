package javaforce;

/** Condition
 *
 * Similar to java.util.concurrent.Callable but only returns primitive boolean type.
 *
 * @author pquiring
 */

public interface Condition {
  public boolean call();
}

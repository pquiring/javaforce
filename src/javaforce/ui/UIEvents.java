package javaforce.ui;

/** UI Events
 *
 * NOTE : These methods must NOT invoke other "native" APIs.
 *
 * @author pquiring
 */

public interface UIEvents {
  public void dispatchEvent(int type, int v1, int v2);
}

package javaforce.jni;

/** Native Library
 *
 * @author pquiring
 */

public class Library {
  public String name, path;
  public boolean once;

  public Library(String name) {
    this.name = name;
  }
  public Library(String name, boolean once) {
    this.name = name;
    this.once = once;
  }
}

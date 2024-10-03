package javaforce.jni;

/** Native Library
 *
 * @author pquiring
 */

public class Library {
  public String name, path;
  public String match, libmatch;

  public Library(String name) {
    this.name = name;
  }
}

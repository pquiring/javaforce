package javaforce.jni;

/** This class is probed during startup to initiate native loading.
 *
 * @author pquiring
 */

public class Startup {
  static {
    JFNative.load();
  }
}

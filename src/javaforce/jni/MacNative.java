package javaforce.jni;

/**
 *
 * @author pquiring
 */

public class MacNative {
  public static void load() {
    JFNative.load();  //ensure native library is loaded
    if (JFNative.loaded) {
      macInit();
    }
  }

  private static native boolean macInit();
}

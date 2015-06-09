package javaforce.jni;

/**
 *
 * @author pquiring
 */

public class MacNative {
  static {
    JFNative.load();  //ensure native library is loaded
    macInit();
  }

  public static void load() {}  //ensure native library is loaded

  private static native boolean macInit();
}

package javaforce.jni;

/**
 *
 * @author pquiring
 */

public class MacNative {
  public static void load() {
    macInit();
  }

  private static native boolean macInit();
}

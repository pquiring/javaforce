package javaforce.jni;

import javaforce.api.*;
import javaforce.io.*;

/**MonitorFolder JNI implementation.
 *
 * @author pquiring
 */

public class MonitorFolderJNI implements MonitorFolderAPI {

  private static MonitorFolderJNI instance;

  public static MonitorFolderJNI getInstance() {
    if (instance == null) {
      JFNative.load();
      instance = new MonitorFolderJNI();
    }
    return instance;
  }

  public native long monitorFolderCreate(String folder);

  public native void monitorFolderPoll(long handle, FolderListener listener);

  public native void monitorFolderClose(long handle);
}

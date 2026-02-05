package javaforce.jni;

/**
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.io.*;

public class MonitorFolderJNI implements MonitorFolderAPI {

  private static MonitorFolderJNI instance;

  public static MonitorFolderJNI getInstance() {
    if (instance == null) {
      instance = new MonitorFolderJNI();
    }
    return instance;
  }

  public native long monitorFolderCreate(String folder);

  public native void monitorFolderPoll(long handle, FolderListener listener);

  public native void monitorFolderClose(long handle);
}

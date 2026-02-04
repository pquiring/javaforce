package javaforce.jni;

/**
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.io.*;

public class MonitorFolderJNI implements MonitorFolderAPI {

  public native long monitorFolderCreate(String folder);

  public native void monitorFolderPoll(long handle, FolderListener listener);

  public native void monitorFolderClose(long handle);
}

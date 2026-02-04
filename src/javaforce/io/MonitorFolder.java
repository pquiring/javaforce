package javaforce.io;

/** Monitor Folder for changes.
 *
 * @author pquiring
 */

import javaforce.api.*;
import javaforce.jni.*;
import javaforce.ffm.*;

public class MonitorFolder {
  private long handle;
  private MonitorFolderAPI api;
  private String folder;
  public static MonitorFolder getInstance() {
    MonitorFolder mf = new MonitorFolder();
    mf.api = new MonitorFolderJNI();
    return mf;
  }
  /** Starts monitoring a folder.
   */
  public boolean create(String folder) {
    if (handle != 0) return false;
    this.folder = folder;
    handle = api.monitorFolderCreate(folder);
    return handle != 0;
  }
  /** Polls folder changes until closed.
   */
  public void poll(FolderListener listener) {
    api.monitorFolderPoll(handle, listener);
  }
  /** Closes a monitor folder instance.
   */
  public void close() {
    if (handle == 0) return;
    api.monitorFolderClose(handle);
    handle = 0;
  }
  /** Returns folder being monitored.
   */
  public String getFolder() {
    return folder;
  }
}

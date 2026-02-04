package javaforce.api;

/** Monitor Folder API
 *
 * Linux : inotify
 * Windows : FindFirstChangeNotification, FindNextChangeNotification, ReadDirectoryChanges
 *
 * @author pquiring
 */

import javaforce.io.*;

public interface MonitorFolderAPI {
  public long monitorFolderCreate(String folder);
  public void monitorFolderPoll(long handle, FolderListener listener);  //blocking until closed
  public void monitorFolderClose(long handle);
}

package javaforce.api;

import javaforce.io.*;

/** Monitor Folder API.
 *
 * Linux : inotify
 * Windows : FindFirstChangeNotification, FindNextChangeNotification, ReadDirectoryChanges
 *
 * @author pquiring
 */

public interface MonitorFolderAPI {
  public long monitorFolderCreate(String folder);
  public void monitorFolderPoll(long handle, FolderListener listener);
  public void monitorFolderClose(long handle);
}

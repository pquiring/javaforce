package javaforce.api;

import javaforce.io.*;
import javaforce.ffm.*;

/** Monitor Folder API.
 *
 * Linux : inotify
 * Windows : FindFirstChangeNotification, FindNextChangeNotification, ReadDirectoryChanges
 *
 * @author pquiring
 */

public interface MonitorFolderAPI {

  public static MonitorFolderAPI getInstance() {
    return MonitorFolderFFM.getInstance();
  }

  public long monitorFolderCreate(String folder);
  public void monitorFolderPoll(long handle, FolderListener listener);
  public void monitorFolderClose(long handle);
}

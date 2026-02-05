package javaforce.io;

/** Folder Change Event Listener
 *
 * @author pquiring
 */

import java.lang.foreign.*;

import javaforce.*;

public interface FolderListener {
  public static boolean debug = true;
  /** Folder Change Event. */
  public void folderChangeEvent(String event, String path);
  /** Default FFM implementation of folderChangeEvent(). Do not implement. */
  default public void folderChangeEvent(MemorySegment event, MemorySegment path) {
    String event_str = event.reinterpret(1024).getString(0);
    if (debug) JFLog.log("event=" + event_str);
    String path_str = path.reinterpret(1024).getString(0);
    if (debug) JFLog.log("path=" + path_str);
    folderChangeEvent(event_str, path_str);
    if (debug) JFLog.log("folderChangeEvent done");
  }
}

package javaforce.io;

/** Folder Change Event Listener
 *
 * @author pquiring
 */

import java.lang.foreign.*;

public interface FolderListener {
  /** Folder Change Event. */
  public void folderChangeEvent(String event, String path);
  /** Default FFM implementation of folderChangeEvent(). Do not implement. */
  default public void folderChangeEvent(MemorySegment event, MemorySegment path) {
    folderChangeEvent(event.reinterpret(1024).getString(0), path.reinterpret(1024).getString(0));
  }
}

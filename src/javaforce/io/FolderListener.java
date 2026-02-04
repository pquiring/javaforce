package javaforce.io;

/** Folder Change Event Listener
 *
 * @author pquiring
 */

public interface FolderListener {
  public void folderChangeEvent(String event, String path);
}

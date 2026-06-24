package javaforce.io;

/** Folder Change Event Listener.
 *
 * NOTE : These methods must NOT invoke other "native" APIs.
 *
 * @author pquiring
 */

public interface FolderListener {
  public static boolean debug = false;
  /** Folder Change Event. */
  public void folderChangeEvent(String event, String path);
}

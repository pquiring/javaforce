package jffile;

import java.io.*;
import java.util.*;

import javaforce.awt.*;

/** FileClipboard
 *
 * @author pquiring
 */

public class FileClipboard {
  public FileList get() {
    return JFClipboard.readFiles();
  }
  public void set(FileList files) {
    JFClipboard.writeFiles(files);
  }
  public void clear() {
    JFClipboard.clearClipboard();
  }
}

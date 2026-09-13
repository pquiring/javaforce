package javaforce.awt;

import java.io.*;

/** File List for JFClipboard.
 *
 * @author pquiring
 */

public class FileList {

  public static final int NONE = 0;
  public static final int COPY = 1;
  public static final int MOVE = 2;  //cut
  public static final int COPY_OR_MOVE = 3;

  public int action;
  public java.util.List<File> files;
}

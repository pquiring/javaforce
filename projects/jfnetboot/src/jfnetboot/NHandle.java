package jfnetboot;

/** NHandle
 *
 * @author pquiring
 */

import java.io.*;

public abstract class NHandle {
  public long handle;  //filesystem handle (inode)
  public String name;
  public String path;  //relative path
  public String local;  //absolute path
  public NHandle parent;

  //HARD LINKS require unique handles
  public static long HARD_LINK  = 0x0100000000000000L;
  public static long INODE_MASK = 0x00ffffffffffffffL;

  public abstract boolean isFile();
  public abstract boolean isFolder();
  public boolean exists() {
    return FileOps.exists(local);
  }
}

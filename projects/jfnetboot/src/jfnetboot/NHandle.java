package jfnetboot;

/** NHandle
 *
 * @author pquiring
 */

import java.io.*;

public abstract class NHandle {
  public long handle;  //filesystem handle
  public String name;
  public String path;  //relative path
  public String local;  //absolute path
  public String symlink;  //sym link
  public NFolder parent;

  public boolean rw;  //client has changed file

  public abstract boolean isFile();
  public abstract boolean isFolder();
  public boolean exists() {
    return FileOps.exists(local);
  }
}

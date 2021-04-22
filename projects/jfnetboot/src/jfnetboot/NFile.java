package jfnetboot;

/** NFile
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class NFile extends NHandle {

  public NFile() {}

  public NFile(long handle, String local, String path, String name) {
    this.handle = handle;
    this.local = local;
    this.path = path;
    this.name = name;
  }

  private NFile(NFile that) {
    this.handle = that.handle;
    this.local = that.local;
    this.path = that.path;
    this.name = that.name;
    this.symlink = that.symlink;
    //do NOT copy 'copy' or 'parent'
  }

  public NFile clone() {
    return new NFile(this);
  }

  public boolean isFile() {
    return true;
  }

  public boolean isFolder() {
    return false;
  }

  public void makeCopy(String newBase) {
    if (rw) return;
    if (symlink != null) {
      JFLog.log("Error:makeCopy() with symlink:" + path);
    }
    rw = true;
    String newLocal = newBase + path;
    try {
      File fwrite = new File(newLocal);
      File fread = new File(local);
      if (fread.exists()) {
        FileOps.copyFile(fread.toString(), fwrite.toString());
      } else {
        FileOps.create(fwrite.toString());
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    local = newLocal;
  }

  public String toString() {
    return "NFile:" + Long.toUnsignedString(handle, 16);
  }
}

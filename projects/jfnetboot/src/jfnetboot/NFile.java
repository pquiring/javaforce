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

  public String toString() {
    return "NFile:" + Long.toUnsignedString(handle, 16);
  }
}

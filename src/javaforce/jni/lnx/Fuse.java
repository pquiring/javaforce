package javaforce.jni.lnx;

/** Fuse base class
 * To create a Fuse system, extend this class and override the fuse system calls.
 * Then call LnxNative.fuse()
 * Later use 'fusermount -u mount_point' to terminate the server.
 * Make sure to use '-f' option or this crashes! (Java doesn't like detach from terminal)
 *
 * General usage: jfuse... resource mount_point [options]
 *
 * @author pquiring
 *
 * Created : Nov 1, 2013
 */

import java.util.*;
import javaforce.jni.LnxNative;

public class Fuse {
  public int getattr(String path, FuseStat stat) {return -1;}
  public int mkdir(String path, int mode) {return -1;}
  public int unlink(String path) {return -1;}
  public int rmdir(String path) {return -1;}
  public int symlink(String target, String link) {return -1;}
  public int link(String target, String link) {return -1;}
  public int chmod(String path, int mode) {return -1;}
  public int truncate(String path, long length) {return -1;}

  public int open(String path, int mode, int fd) {return -1;}
  public int create(String path, int mode, int fd) {return -1;}
  public int read(String path, byte buf[], long offset, int fd) {return -1;}
  public int write(String path, byte buf[], long offset, int fd) {return -1;}
  public int close(String path, int fd) {return -1;}

  public int statfs(String path) {return -1;}  //return available sectors (512 bytes each)

  public String[] readdir(String path) {return null;}

  private HashMap<Integer, Object> handles = new HashMap<Integer, Object>();

  public void attachObject(int fd, Object obj) {
    handles.put(fd, obj);
  }

  public Object getObject(int fd) {
    return handles.get(fd);
  }

  public void detachObject(int fd) {
    handles.remove(fd);
  }

  public void start(String args[]) {
    LnxNative.fuse(args, this);
  }

  //must return -errno
  public static final int EPERM = 1;  //not permitted
  public static final int ENOENT = 2;  //file/dir not found
  public static final int EIO = 5;  //input/output error
  public static final int EACCESS = 13;  //access denied
  public static final int EEXIST = 17;  //file already exists
  public static final int ENOTDIR = 20;  //not a directory
  public static final int EISDIR = 21;  //is a directory
  public static final int EINVAL = 22;  //invalid
  public static final int EFBIG = 27;  //file too large
  public static final int ENOSPC = 28;  //device out of space
  public static final int EROFS = 30;  //read only file system
  //etc.
  public static final int ENOTSUP = 95;  //not supported
}

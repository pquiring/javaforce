package javaforce.jni.lnx;

/**
 *
 * @author pquiring
 */
public class FuseStat {
  public boolean folder;
  public boolean symlink;
  public int mode;  //rwx rwx rwx
  public long size;
  public long atime, mtime, ctime;  //unix time

  public static FuseStat allocate() {
    return new FuseStat();
  }
}

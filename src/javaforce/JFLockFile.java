package javaforce;

/** File Locking object.
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.channels.*;

public class JFLockFile {
  private RandomAccessFile lockraf; // The file we'll lock
  private FileChannel lockChannel; // The channel to the file
  private FileLock lockLock; // The lock object we hold
  private File lockFile;

  public boolean lock(String fileName) {
    try {
      lockFile = new File(fileName);
      lockraf = new RandomAccessFile(lockFile, "rw");
      lockChannel = lockraf.getChannel();
      lockLock = lockChannel.tryLock();
      if (lockLock == null) return false;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public void unlock() {
    if (lockLock == null) return;
    try {
      lockLock.release();
      lockraf.close();
      lockFile.delete();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

/** Tape Drive API
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.jni.*;

public class TapeDrive {
  private long handle;
  public boolean open(String name) {
    handle = WinNative.tapeOpen("\\\\.\\" + name);
    return handle != 0;
  }
  public void close() {
    if (handle == 0) return;
    WinNative.tapeClose(handle);
    handle = 0;
  }
  public long getpos(int attempt) {
    return WinNative.tapeGetpos(handle);
  }
  public boolean setpos(long blk, int attempt) {
    return WinNative.tapeSetpos(handle, blk);
  }
  public MediaInfo getMediaInfo() {
    MediaInfo info = new MediaInfo();
    if (!WinNative.tapeMedia(handle)) return null;
    info.capacity = WinNative.tapeMediaSize();
    info.readonly = WinNative.tapeMediaReadOnly();
    return info;
  }
  public DriveInfo getDriveInfo() {
    DriveInfo info = new DriveInfo();
    if (!WinNative.tapeDrive(handle)) return null;
    info.defaultBlockSize = WinNative.tapeDriveDefaultBlockSize();
    info.minimumBlockSize = WinNative.tapeDriveMinBlockSize();
    info.maximumBlockSize = WinNative.tapeDriveMaxBlockSize();
    return info;
  }
  public int read(byte data[], int offset, int length) {
    return WinNative.tapeRead(handle, data, offset, length);
  }
  public boolean write(byte data[], int offset, int length) {
    return WinNative.tapeWrite(handle, data, offset, length) == data.length;
  }
}

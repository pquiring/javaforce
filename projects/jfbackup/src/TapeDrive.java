/** Tape Drive API
 *
 * @author pquiring
 */

import javaforce.api.*;

public class TapeDrive {
  private long handle;
  public boolean open(String name) {
    handle = WindowsAPI.getInstance().tapeOpen("\\\\.\\" + name);
    return handle != 0;
  }
  public void close() {
    if (handle == 0) return;
    WindowsAPI.getInstance().tapeClose(handle);
    handle = 0;
  }
  public boolean format(int blocksize) {
    if (handle == 0) return false;
    return WindowsAPI.getInstance().tapeFormat(handle, blocksize);
  }
  public long getpos(int attempt) {
    return WindowsAPI.getInstance().tapeGetpos(handle);
  }
  public boolean setpos(long blk, int attempt) {
    return WindowsAPI.getInstance().tapeSetpos(handle, blk);
  }
  public MediaInfo getMediaInfo() {
    MediaInfo info = new MediaInfo();
    if (!WindowsAPI.getInstance().tapeMedia(handle)) return null;
    info.capacity = WindowsAPI.getInstance().tapeMediaSize();
    info.blocksize = WindowsAPI.getInstance().tapeMediaBlockSize();
    info.readonly = WindowsAPI.getInstance().tapeMediaReadOnly();
    return info;
  }
  public DriveInfo getDriveInfo() {
    DriveInfo info = new DriveInfo();
    if (!WindowsAPI.getInstance().tapeDrive(handle)) return null;
    info.defaultBlockSize = WindowsAPI.getInstance().tapeDriveDefaultBlockSize();
    info.minimumBlockSize = WindowsAPI.getInstance().tapeDriveMinBlockSize();
    info.maximumBlockSize = WindowsAPI.getInstance().tapeDriveMaxBlockSize();
    return info;
  }
  public int read(byte data[], int offset, int length) {
    return WindowsAPI.getInstance().tapeRead(handle, data, offset, length);
  }
  public int written;
  public boolean write(byte data[], int offset, int length) {
    int written = WindowsAPI.getInstance().tapeWrite(handle, data, offset, length);
    return written == data.length;
  }
  public String lastError() {
    return String.format("0x%08x : Handle=0x%08x", WindowsAPI.getInstance().tapeLastError(), handle);
  }
}

/** Tape Drive API
 *
 * @author pquiring
 */

import javaforce.*;

public class TapeDrive {
  public long getpos(int attempt) {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.tapeDevice, "getpos"}, false);
    String lns[] = out.split("\r\n");
    //position:%lld
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a] + " (attempt " + attempt + " of 3)");
        return -1;
      }
      if (lns[a].startsWith("position:")) {
        return Long.valueOf(lns[a].substring(9));
      }
    }
    return -1;
  }
  public boolean setpos(long blk, int attempt) {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.tapeDevice, "setpos", Long.toString(blk)}, false);
    String lns[] = out.split("\r\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a] + " (attempt " + attempt + " of 3)");
        return false;
      }
    }
    return true;
  }
  public MediaInfo getMediaInfo() {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.tapeDevice, "media"}, false);
    String lns[] = out.split("\r\n");
    MediaInfo info = new MediaInfo();
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a]);
        return null;
      }
      else if (lns[a].startsWith("capacity:")) {
        info.capacity = Long.valueOf(lns[a].substring(9));
      }
      else if (lns[a].startsWith("writeprotect:")) {
        String value = lns[a].substring(13);
        info.readonly = value.equals("true");
      }
    }
    return info;
  }
  public DriveInfo getDriveInfo() {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.tapeDevice, "drive"}, false);
    String lns[] = out.split("\r\n");
    DriveInfo info = new DriveInfo();
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a]);
        return null;
      }
      else if (lns[a].startsWith("defaultblocksize:")) {
        info.defaultBlockSize = Long.valueOf(lns[a].substring(17));
      }
      else if (lns[a].startsWith("minimumblocksize:")) {
        info.minimumBlockSize = Long.valueOf(lns[a].substring(17));
      }
      else if (lns[a].startsWith("maximumblocksize:")) {
        info.maximumBlockSize = Long.valueOf(lns[a].substring(17));
      }
    }
    return info;
  }
  public boolean read(String file, long bytes) {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.tapeDevice, "read", file, Long.toString(bytes)}, false);
    String lns[] = out.split("\r\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a]);
        return false;
      }
    }
    return true;
  }
  public boolean write(String file) {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"tapetool", Config.current.tapeDevice, "write", file}, false);
    String lns[] = out.split("\r\n");
    for(int a=0;a<lns.length;a++) {
      if (lns[a].startsWith("Error:")) {
        JFLog.log(2, "Tape:" + lns[a]);
        return false;
      }
    }
    return true;
  }
}

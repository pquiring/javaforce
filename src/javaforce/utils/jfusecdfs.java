package javaforce.utils;

import javaforce.*;
import javaforce.jni.*;
import javaforce.jni.lnx.*;

/**
 * Mount audio CD-ROMs using libcdio.
 * NOTE : requires root access to device.
 * NOTE : use -oallow_other option to allow others to use mounted file system.
 *
 * see : http://www.gnu.org/software/libcdio/
 *
 * @author pquiring
 *
 * Created : Feb 8, 2014
 */

public class jfusecdfs extends Fuse {
//NOTE : it takes 75 sectors to make 1 second of audio (44100 * 2 * 2 / 2352 = 75)
//m = 4500 sectors each
//s = 75 sectors each
//f = x sectors

  private long ptr;
  private int nTracks;
  private int starts[];  //starting sector #
  private int lengths[];  //# of sectors

  public boolean auth(String args[], String pass) {
    ptr = LnxNative.cdio_open_linux(args[0]);
    if (ptr == 0) {
      JFLog.log("Error:Failed to open device:" + args[0]);
      return false;
    }
    nTracks = LnxNative.cdio_get_num_tracks(ptr);
    if (nTracks <= 0) {
      JFLog.log("Error:no audio tracks found");
      LnxNative.cdio_destroy(ptr);
      ptr = 0;
      return false;
    }
    JFLog.log("Ok:Found " + nTracks + " tracks");
    starts = new int[nTracks];
    lengths = new int[nTracks];
    for(int a=0;a<nTracks;a++) {
      starts[a] = LnxNative.cdio_get_track_lsn(ptr, a+1);
      lengths[a] = LnxNative.cdio_get_track_sec_count(ptr, a+1);
    }
    return true;
  }

  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage : jfuse-cdfs /dev/cdrom mount");
      return;
    }
    new jfusecdfs().main2(args);
  }

  public void main2(String args[]) {
    try {
      if (!auth(args, null)) return;
      LnxNative.fuse(args, this);
      LnxNative.cdio_destroy(ptr);
      ptr = 0;
    } catch (Exception e) {
      JFLog.log("Error:" + e);
    }
  }

  public int getattr(String path, FuseStat stat) {
//    JFLog.log("getattr:" + path);
    if (path.equals("/")) {
      stat.folder = true;
      return 0;
    }
    if (!path.endsWith(".wav")) return -1;
    int idx1 = path.indexOf("track");
    int idx2 = path.indexOf(".wav");
    int track = JF.atoi(path.substring(idx1+5,idx2));
    track--;
    if ((track < 0) || (track >= nTracks)) return -1;
    stat.size = lengths[track] * 2352 + WAV_SIZE;
    return 0;
  }

  public int mkdir(String path, int mode) {
//    JFLog.log("mkdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
      //TODO
      return -1;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int unlink(String path) {
//    JFLog.log("unlink:" + path);
    try {
      //TODO
      return -1;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int rmdir(String path) {
//    JFLog.log("rmdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
      //TODO
      return -1;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int symlink(String target, String link) {
//    JFLog.log("symlink:" + link + "->" + target);
    return -1;
  }

  public int link(String target, String link) {
//    JFLog.log("link:" + link + "->" + target);
    return -1;
  }

  public int chmod(String path, int mode) {
//    JFLog.log("chmod:" + path);
    return -1;
  }

  public int chown(String path, int mode) {
//    JFLog.log("chown:" + path);
    return -1;
  }

  public int truncate(String path, long size) {
//    JFLog.log("truncate:" + path);
    try {
      //TODO
      return -1;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return -1;
  }

  private class FileState {
    byte buffer[];
    long offset;
    int bufpos, bufsiz;
    int sectors_left;
    int sector_pos;
    boolean canWrite;
    boolean canRead;
    Object lock = new Object();
    byte data[] = new byte[4096];  //transfer buffer
  }

  //wav headers
  static int RIFF_SIZE = 12;
  static int FMT_SIZE = 24;
  static int DATA_SIZE = 8;
  static int WAV_SIZE = 44;  //total header size

  private void createWavHeader(FileState fs) {
    byte header[] = fs.buffer;
    LE.setString(header, 0, 4, "RIFF");
    LE.setuint32(header, 4, 4 + FMT_SIZE + DATA_SIZE + fs.sectors_left * 2352);
    LE.setString(header, 8, 4, "WAVE");
    LE.setString(header, 12, 4, "fmt ");
    LE.setuint32(header, 16, FMT_SIZE - 8);
    LE.setuint16(header, 20, 1);  //PCM format
    LE.setuint16(header, 22, 2);  //# channels
    LE.setuint32(header, 24, 44100);  //sample rate
    LE.setuint32(header, 28, 44100 * 4);  //byte rate
    LE.setuint16(header, 32, 4);  //block align (# channels * bytes/sample)
    LE.setuint16(header, 34, 16);  //bits/sample
    LE.setString(header, 36, 4, "data");
    LE.setuint32(header, 40, fs.sectors_left * 2352);  //size of actual data
    fs.bufsiz = header.length;
  }

  public int open(String path, int mode, int fd) {
//    JFLog.log("open:" + path);
    if (!path.endsWith(".wav")) return -1;
    int idx1 = path.indexOf("track");
    int idx2 = path.indexOf(".wav");
    int track = JF.atoi(path.substring(idx1+5,idx2));
    track--;
    if ((track < 0) || (track >= nTracks)) return -1;
    try {
      FileState fs = new FileState();
      fs.canRead = true;
      fs.canWrite = false;
      fs.buffer = new byte[2352 * 32];
      fs.bufpos = 0;
      fs.bufsiz = 0;
      fs.sectors_left = lengths[track];
      fs.sector_pos = starts[track];
      createWavHeader(fs);
      attachObject(fd, fs);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int read(String path, byte buf[], long offset, int fh) {
    int size = buf.length;
//    JFLog.log("read:" + path + ",size=" + size + ",offset=" + offset);
    FileState fs = (FileState)getObject(fh);
    if (fs == null) {
//      JFLog.log("no fs");
      return -1;
    }
    if (!fs.canRead) {
//      JFLog.log("!read");
      return -1;
    }
    int read = 0;
    try {
      synchronized(fs.lock) {
        if (offset != fs.offset) {
          JFLog.log("Error:Read not sequential");
          return -1;
        }
        int pos = 0;
        while (read != size) {
          int amt = size - read;
          if (amt > 4096) amt = 4096;  //transfer buffer size
          if (fs.bufsiz == 0) {
            if (fs.sectors_left == 0) break;  //no more to read
            //read more sectors
            int sectors_to_read = 32;
            if (sectors_to_read > fs.sectors_left) sectors_to_read = fs.sectors_left;
            //TODO : add fault tolerance here (jitter???)
//            JFLog.log("read_sectors:pos=" + fs.sector_pos + ",len=" + sectors_to_read);
            int res = LnxNative.cdio_read_audio_sectors(ptr, fs.buffer, fs.sector_pos, sectors_to_read);
            if (res != 0) {
              JFLog.log("Error:reading disc");
              return -1;
            }
            fs.bufsiz = sectors_to_read * 2352;
            fs.bufpos = 0;
            fs.sectors_left -= sectors_to_read;
            fs.sector_pos += sectors_to_read;
          }
          if (amt > fs.bufsiz) amt = fs.bufsiz;
          System.arraycopy(fs.buffer, fs.bufpos, buf, pos, amt);
          fs.bufpos += amt;
          fs.bufsiz -= amt;
          read += amt;
          pos += amt;
        }
        fs.offset += read;
      }
      return read;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int write(String path, byte buf[], long offset, int fd) {
    return -1;
  }

  public int statfs(String path) {
//    JFLog.log("statfs:" + path);
    return -1;
  }

  public int close(String path, int fd) {
//    JFLog.log("release:" + path);
    FileState fs = (FileState)getObject(fd);
    if (fs == null) return 0;
    synchronized(fs.lock) {
      detachObject(fd);
    }
    return 0;
  }

  public String[] readdir(String path) {
//    JFLog.log("readdir:" + path);
    if (!path.endsWith("/")) path += "/";
    String dir[] = new String[nTracks];
    for(int a=0;a<nTracks;a++) {
      dir[a] = "track" + (a+1) + ".wav";
    }
//      JFLog.log("readdir done");
    return dir;
  }

  public int create(String path, int mode, int fd) {
//    JFLog.log("create:" + path);
    try {
      //TODO
      return -1;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return -1;
  }
}

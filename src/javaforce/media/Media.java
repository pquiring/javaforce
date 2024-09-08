package javaforce.media;

/** JavaForce Media File
 *
 * Open multi-media file format supporting multiple streams.
 *
 * Designed to be very quick and easy to use.
 *
 * file ext : .jfav
 *
 * Data is written in big-endian format.
 * Currently limited to 2GB file sizes.
 *
 * If audio and video are added then stream zero must be video.
 *
 * @author peter.quiring
 */

import java.io.*;

import javaforce.*;
import javaforce.voip.*;

public class Media {
  private RandomAccessFile raf;
  private Header header;
  private boolean write;
  private int currentFrame;
  private int[] indexes = new int[4096];
  private long[] tses = new long[4096];
  private Packet frame;
  private long ts_keyframe;

  private static final int JFAV = 0x4a464156;  //file magic id
  private static final int V32 = 32;  //file version (limited to 2GB)
  private static final int V64 = 64;  //file version (future edition)
  private static final long V32_max_file_size = 2L * JF.GB;  //max file size for V32
  private static final int max_streams = 16;

  public static boolean debug = false;

  /** Open existing file for reading. */
  public boolean open(String file) {
    return open(file, "r");
  }

  private boolean open(String file, String mode) {
    if (raf != null) return false;
    frame = new Packet();
    frame.data = new byte[4096];
    try {
      write = false;
      currentFrame = 0;
      raf = new RandomAccessFile(file, mode);
      header = readHeader();
      if (header == null) {
        abort();
        return false;
      }
      readIndexes();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  /** Create new file for writing.
   * If file exists it is truncated. */
  public boolean create(String file, int[] streamIDs, CodecInfo info) {
    if (raf != null) return false;
    if (streamIDs == null || streamIDs.length == 0 || streamIDs.length > max_streams) return false;
    header = new Header();
    header.streamCount = streamIDs.length;
    header.streams = streamIDs;
    if (info != null) {
      header.info = info;
      if (info.chs != 0 && info.freq != 0) {
        header.flags |= FLAG_AUDIO;
      }
      if (info.width != 0 && info.height != 0) {
        header.flags |= FLAG_VIDEO;
      }
    }
    try {
      write = true;
      currentFrame = 0;
      ts_keyframe = 0;
      raf = new RandomAccessFile(file, "rw");
      raf.setLength(0);
      if (!writeHeader()) {
        abort();
        return false;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public boolean create(String file, int[] streamIDs) {
    return create(file, streamIDs, null);
  }

  /** Open existing file to writing. */
  public boolean append(String file) {
    if (raf != null) return false;
    if (!open(file, "rw")) return false;
    //seek to indexOffset and truncate file
    try {
      raf.seek(header.indexOffset);
      raf.setLength(raf.getFilePointer());
      write = true;
      currentFrame = header.keyFrames;
      ts_keyframe = tses[currentFrame - 1];
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      abort();
      return false;
    }
  }

  /** If file was created or appended an index table is written at end of file for fast seeking. */
  public boolean close() {
    if (raf == null) return false;
    if (write) {
      writeIndexes();
      updateHeader();
    }
    try { raf.close(); } catch (Exception e) {}
    raf = null;
    return true;
  }

  private void abort() {
    try {
      if (raf != null) raf.close();
    } catch (Exception e) {}
    write = false;
  }

  public int[] getStreamIDs() {
    return header.streams;
  }

  /** Return # of key frames. */
  public int getKeyFrameCount() {
    return header.keyFrames;
  }

  /** Return # of key frames. */
  public int getAllFrameCount() {
    return header.allFrames;
  }

  /** Returns timestamp of first frame. */
  public long getTimeBase() {
    return tses[0];
  }

  /** Returns codec info that describes the media (optional). */
  public CodecInfo getCodecInfo() {
    return header.info;
  }

  /** Seek to frame #.
   * Seeking is not supported on files opened for writing (create() or append()).
   */
  public boolean seekFrame(int frame) {
    if (write) return false;
    try {
      if (frame >= header.keyFrames) return false;
      int index = indexes[frame];
      raf.seek(index);
      currentFrame = frame;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Seek to frame closest to timestamp.
   */
  public boolean seekTime(long ts) {
    if (write) return false;
    for(int idx=0;idx<header.keyFrames;idx++) {
      if (tses[idx] >= ts) {
        return seekFrame(idx);
      }
    }
    return false;
  }

  /** Read next Frame.
   *
   * Do not modify fields in Frame.
   *
   * @return frame = next frame or null at end of file or error
   */
  public Packet readFrame() {
    if (write) return null;
    try {
      long pos = raf.getFilePointer();
      if (pos >= header.indexOffset) return null;
      if (pos == indexes[currentFrame]) {
        frame.ts = tses[currentFrame];
        ts_keyframe = frame.ts;
        if (debug) JFLog.log("Media.readFrame():key_frame_ts=" + frame.ts + "@" + currentFrame);
        currentFrame++;
      }
      frame.stream = raf.readByte();
      short delta_ts = raf.readShort();
      if (debug) JFLog.log("Media.readFrame():delta_ts=" + delta_ts);
      frame.ts = ts_keyframe + delta_ts;
      frame.length = raf.readInt();
      while (frame.length > frame.data.length) {
        frame.data = new byte[frame.data.length << 1];
      }
      raf.read(frame.data, 0, frame.length);
      return frame;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private void growIndexes(boolean keep) {
    while (indexes.length <= header.keyFrames) {
      int[] old_indexes = indexes;
      indexes = new int[indexes.length << 1];
      long[] old_tses = tses;
      tses = new long[tses.length << 1];
      if (keep) {
        System.arraycopy(old_indexes, 0, indexes, 0, old_indexes.length);
        System.arraycopy(old_tses, 0, tses, 0, old_tses.length);
      }
    }
  }

  /** Write next frame.
   *
   * @param stream = stream index
   * @param data = raw codec data
   * @param offset = offset into data
   * @param length = length of data
   * @param ts = timestamp of frame
   * @param keyFrame = is frame a key frame?
   */
  public boolean writeFrame(int stream, byte[] data, int offset, int length, long ts, boolean keyFrame) {
    try {
      long file_offset = raf.getFilePointer();
      long file_end = file_offset + length + 5;
      if (file_end >= V32_max_file_size) {
        JFLog.log("Media:max_file_size reached (2GB)");
        return false;
      }
      if (stream == 0) {
        if (keyFrame) {
          if (indexes.length == header.keyFrames) {
            growIndexes(true);
          }
          indexes[header.keyFrames] = (int)file_offset;
          tses[header.keyFrames] = ts;
          header.keyFrames++;
          ts_keyframe = ts;
        }
        header.allFrames++;
      }
      raf.writeByte(stream);
      raf.writeShort((short)(ts - ts_keyframe));
      raf.writeInt(length);
      raf.write(data, offset, length);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  //private classes
  private static class Header {
    int magic;  //magic id
    int version;  //file version
    int flags;  //see FLAG_... below
    int keyFrames;  //# of "key" frames
    int allFrames;  //# of all frames (key + delta)
    int indexOffset;  //offset to indexes
    int streamCount;
    int[] streams;
    CodecInfo info;
  }

  private static int FLAG_AUDIO = 0x01;
  private static int FLAG_VIDEO = 0x02;
  private static int FLAG_MAX_VALUE = 0x03;

  private Header readHeader() {
    Header header = new Header();
    try {
      header.magic = raf.readInt();
      if (header.magic != JFAV) throw new Exception("invalid file");
      header.version = raf.readInt();
      if (header.version != V32) throw new Exception("invalid file");
      header.flags = raf.readInt();
      if (header.flags > FLAG_MAX_VALUE) throw new Exception("invalid file");
      if (header.flags > 0) {
        header.info = new CodecInfo();
        if ((header.flags & FLAG_AUDIO) != 0) {
          header.info.chs = raf.readInt();
          header.info.freq = raf.readInt();
        }
        if ((header.flags & FLAG_VIDEO) != 0) {
          header.info.width = raf.readInt();
          header.info.height = raf.readInt();
          header.info.fps = raf.readFloat();
        }
      }
      header.keyFrames = raf.readInt();
      header.allFrames = raf.readInt();
      header.indexOffset = raf.readInt();
      header.streamCount = raf.readInt();
      if (header.streamCount < 1 || header.streamCount > max_streams) throw new Exception("invalid file");
      header.streams = new int[header.streamCount];
      for(int idx=0;idx<header.streamCount;idx++) {
        header.streams[idx] = raf.readInt();
      }
      return header;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private boolean writeHeader() {
    try {
      raf.writeInt(JFAV);  //header
      raf.writeInt(V32);  //version (32bit)
      raf.writeInt(header.flags);
      if (header.info != null) {
        if ((header.flags & FLAG_AUDIO) != 0) {
          //write audio details
          raf.writeInt(header.info.chs);
          raf.writeInt(header.info.freq);
        }
        if ((header.flags & FLAG_VIDEO) != 0) {
          //write video details
          raf.writeInt(header.info.width);
          raf.writeInt(header.info.height);
          raf.writeFloat(header.info.fps);
        }
      }
      raf.writeInt(header.keyFrames);
      raf.writeInt(header.allFrames);
      raf.writeInt(header.indexOffset);
      raf.writeInt(header.streamCount);
      for(int idx=0;idx<header.streamCount;idx++) {
        raf.writeInt(header.streams[idx]);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private boolean updateHeader() {
    //MUST call writeIndexes() first to update header.indexOffset
    try {
      raf.seek(8);
      raf.writeInt(header.keyFrames);
      raf.writeInt(header.indexOffset);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private boolean readIndexes() {
    try {
      indexes = new int[4096];
      tses = new long[4096];
      growIndexes(false);
      raf.seek(header.indexOffset);
      for(int idx=0;idx<header.keyFrames;idx++) {
        indexes[idx] = raf.readInt();
        tses[idx] = raf.readLong();
      }
      //seek to first frame
      int index = indexes[0];
      if (index < 0) index *= -1;  //keyframe?
      raf.seek(index);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private boolean writeIndexes() {
    try {
      header.indexOffset = (int)raf.length();
      raf.seek(header.indexOffset);
      for(int idx=0;idx<header.keyFrames;idx++) {
        raf.writeInt(indexes[idx]);
        raf.writeLong(tses[idx]);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private void view() {
    JFLog.log("# key frames=" + header.keyFrames);
    for(int a=0;a<header.keyFrames;a++) {
      JFLog.log("frame = " + a);
      JFLog.log(" ts[] = " + tses[a]);
      JFLog.log("idx[] = " + indexes[a]);
    }
    do {
      Packet frame = readFrame();
      if (frame == null) break;
    } while (true);
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      JFLog.log("usage:Media view file.jfav");
      System.exit(0);
    }
    debug = true;
    switch (args[0]) {
      case "view": {
        Media media = new Media();
        media.open(args[1]);
        media.view();
        break;
      }
      default:
        JFLog.log("Unknown command:" + args[0]);
        break;
    }
  }
}

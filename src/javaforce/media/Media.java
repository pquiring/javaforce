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
 * @author peter.quiring
 */

import java.io.*;

import javaforce.*;

public class Media {
  private RandomAccessFile raf;
  private Header header;
  private boolean write;
  private int currentFrame;
  private int[] indexes = new int[4096];
  private long[] tses = new long[4096];
  private Frame frame;

  private static final int JFAV = 0x4a464156;  //file magic id
  private static final int V32 = 32;  //file version (limited to 2GB)
  private static final int V64 = 64;  //file version (future edition)
  private static final long V32_max_file_size = 2L * JF.GB;  //max file size for V32
  private static final int max_streams = 16;

  /** Open existing file for reading. */
  public boolean open(String file) {
    return open(file, "r");
  }

  private boolean open(String file, String mode) {
    if (raf != null) return false;
    frame = new Frame();
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
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  /** Create new file for writing.
   * If file exists it is truncated. */
  public boolean create(String file, int[] streamIDs) {
    if (raf != null) return false;
    if (streamIDs == null || streamIDs.length == 0 || streamIDs.length > max_streams) return false;
    header = new Header();
    header.streamCount = streamIDs.length;
    header.streams = streamIDs;
    try {
      write = true;
      currentFrame = 0;
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

  /** Open existing file to writing. */
  public boolean append(String file) {
    if (raf != null) return false;
    if (!open(file, "rw")) return false;
    //seek to indexOffset and truncate file
    try {
      raf.seek(header.indexOffset);
      raf.setLength(raf.getFilePointer());
      write = true;
      currentFrame = header.frameCount;
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

  public int getFrameCount() {
    return header.frameCount;
  }

  public static class Frame {
    public int stream;  //stream index
    public long ts;  //timestamp from beginning (ms)
    public byte[] data;  //raw codec data
    public int offset;  //offset of data
    public int length;  //length of data
  }

  /** Seek to frame #.
   * Seeking is not supported on files opened for writing (create() or append()).
   */
  public boolean seekFrame(int frame) {
    if (write) return false;
    try {
      if (frame >= header.frameCount) return false;
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
    for(int idx=0;idx<header.frameCount;idx++) {
      if (tses[idx] >= ts) {
        return seekFrame(idx);
      }
    }
    return false;
  }

  public Frame readFrame() {
    if (write) return null;
    try {
      long pos = raf.getFilePointer();
      if (pos >= header.indexOffset) return null;
      if (pos >= indexes[currentFrame]) {
        frame.ts = tses[currentFrame];
        currentFrame++;
      }
      frame.stream = raf.readByte();
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
    while (indexes.length <= header.frameCount) {
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

  public boolean writeFrame(int stream, byte[] data, int offset, int length, long ts, boolean keyFrame) {
    try {
      long file_offset = raf.getFilePointer();
      long file_end = file_offset + length + 5;
      if (file_end >= V32_max_file_size) {
        JFLog.log("Media:max_file_size reached (2GB)");
        return false;
      }
      if (keyFrame) {
        if (indexes.length == header.frameCount) {
          growIndexes(true);
        }
        indexes[header.frameCount] = (int)file_offset;
        tses[header.frameCount] = ts;
        header.frameCount++;
      }
      raf.writeByte(stream);
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
    int frameCount;  //# of "key" frames
    int indexOffset;  //offset to indexes
    int streamCount;
    int[] streams;
  }

  private Header readHeader() {
    Header header = new Header();
    try {
      header.magic = raf.readInt();
      if (header.magic != JFAV) throw new Exception("invalid file");
      header.version = raf.readInt();
      if (header.version != V32) throw new Exception("invalid file");
      header.frameCount = raf.readInt();
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
      raf.writeInt(header.frameCount);
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
      raf.writeInt(header.frameCount);
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
      for(int idx=0;idx<header.frameCount;idx++) {
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
      for(int idx=0;idx<header.frameCount;idx++) {
        raf.writeInt(indexes[idx]);
        raf.writeLong(tses[idx]);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}

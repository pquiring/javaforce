/**
 *
 * @author pquiring
 *
 * Created : Sept 21, 2013
 */

import java.io.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.media.*;

public class Decoder implements MediaIO {
  MediaDecoder decoder = new MediaDecoder();
  RandomAccessFile fin;
  Object oout;
  public boolean decode(String in) {
    try {
      fin = new RandomAccessFile(in, "r");
      if (!decoder.start(this, -1, -1, -1, -1, true)) throw new Exception("Decoder Failed to start");
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public short[] getSamples() {
    int type;
    do {
      type = decoder.read();
      if (type == MediaCoder.END_FRAME) return null;
    } while (type == MediaCoder.NULL_FRAME);
    return decoder.getAudio();
  }

  public int getSampleRate() {
    return decoder.getSampleRate();
  }

  public int getChannels() {
    return decoder.getChannels();
  }

  public void stop() {
    decoder.stop();
  }

  public int read(MediaCoder coder, byte[] bytes) {
    if (coder == decoder) {
      try {
        return fin.read(bytes, 0, bytes.length);
      } catch (Exception e) {
        JFLog.log(e);
        return 0;
      }
    }
    return 0;
  }

  public int write(MediaCoder coder, byte[] bytes) {
    return 0;
  }

  public long seek(MediaCoder coder, long pos, int how) {
    if (coder == decoder) {
      try {
        switch (how) {
          case MediaCoder.SEEK_SET: break;
          case MediaCoder.SEEK_CUR: pos += fin.getFilePointer(); break;
          case MediaCoder.SEEK_END: pos += fin.length(); break;
        }
        fin.seek(pos);
        return pos;
      } catch (Exception e) {
        JFLog.log(e);
      }
      return 0;
    }
    return 0;
  }
}

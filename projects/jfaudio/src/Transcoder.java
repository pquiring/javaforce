/**
 *
 * @author pquiring
 *
 * Created : Sept 20, 2013
 */

import java.io.*;

import javaforce.*;
import javaforce.media.*;

public class Transcoder implements MediaIO {
  MediaEncoder encoder = new MediaEncoder();
  MediaDecoder decoder = new MediaDecoder();
  RandomAccessFile fin, fout;
  public boolean transcode(String in, String out, String outCodec) {
    int chs, freq;
    try {
      fin = new RandomAccessFile(in, "r");
      fout = new RandomAccessFile(out, "rw");
      fout.setLength(0);
      if (!decoder.start(this, -1, -1, -1, -1, true)) throw new Exception("Decoder Failed to start");
      chs = decoder.getChannels();
      freq = decoder.getSampleRate();
      JFLog.log("decoder:chs=" + chs + ",freq=" + freq);
      if (!encoder.start(this, -1, -1, -1, chs, freq, outCodec, false, true)) throw new Exception("Encoder Failed to start");
      short samples[];
      while (true) {
        int type = decoder.read();
        if (type == MediaCoder.NULL_FRAME) continue;  //metadata
        if (type == MediaCoder.END_FRAME) break;  //end of data
        samples = decoder.getAudio();
        if (samples == null) break;
        encoder.addAudio(samples);
      }
      decoder.stop();
      encoder.stop();
      fin.close();
      fout.close();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
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
    if (coder == encoder) {
      try {
        fout.write(bytes);
        return bytes.length;
      } catch (Exception e) {
        JFLog.log(e);
        return 0;
      }
    }
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
    } else if (coder == encoder) {
      try {
        switch (how) {
          case MediaCoder.SEEK_SET: break;
          case MediaCoder.SEEK_CUR: pos += fout.getFilePointer(); break;
          case MediaCoder.SEEK_END: pos += fout.length(); break;
        }
        fout.seek(pos);
        return pos;
      } catch (Exception e) {
        JFLog.log(e);
      }
      return 0;
    }
    return 0;
  }
}

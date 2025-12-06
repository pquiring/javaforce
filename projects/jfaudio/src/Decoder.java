/**
 *
 * @author pquiring
 *
 * Created : Sept 21, 2013
 */

import java.io.*;

import javaforce.*;
import javaforce.voip.*;
import javaforce.media.*;

public class Decoder implements MediaIO {
  MediaInput decoder = new MediaInput();
  MediaAudioDecoder audio_decoder;
  RandomAccessFile fin;
  public boolean decode(String in) {
    try {
      fin = new RandomAccessFile(in, "r");
      if (!decoder.open(this)) throw new Exception("Decoder Failed to start");
      audio_decoder = decoder.createAudioDecoder();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public short[] getSamples() {
    Packet packet = decoder.readPacket();
    if (packet == null) return null;
    return audio_decoder.decode(packet);
  }

  public int getSampleRate() {
    return audio_decoder.getSampleRate();
  }

  public int getChannels() {
    return audio_decoder.getChannels();
  }

  public void stop() {
    if (audio_decoder != null) {
      audio_decoder = null;
    }
    if (decoder != null) {
      decoder.close();
      decoder = null;
    }
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

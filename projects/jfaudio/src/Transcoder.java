/**
 *
 * @author pquiring
 *
 * Created : Sept 20, 2013
 */

import java.io.*;

import javaforce.*;
import javaforce.voip.*;
import javaforce.media.*;

public class Transcoder implements MediaIO {
  MediaOutput encoder = new MediaOutput();
  MediaAudioEncoder audio_encoder;
  MediaInput decoder = new MediaInput();
  MediaAudioDecoder audio_decoder;
  RandomAccessFile fin, fout;
  public boolean transcode(String in, String out, String outCodec, int bit_rate) {
    int chs, freq;
    try {
      fin = new RandomAccessFile(in, "r");
      fout = new RandomAccessFile(out, "rw");
      fout.setLength(0);
      if (!decoder.open(this)) throw new Exception("Decoder Failed to start");
      audio_decoder = decoder.createAudioDecoder();
      chs = audio_decoder.getChannels();
      freq = audio_decoder.getSampleRate();
      JFLog.log("decoder:chs=" + chs + ",freq=" + freq);
      if (!encoder.create(this, outCodec)) throw new Exception("Encoder Failed to start");
      CodecInfo info = new CodecInfo();
      info.audio_bit_rate = bit_rate;
      info.audio_codec = MediaCoder.AV_CODEC_ID_DEFAULT;  //BUG ??? Need value from outCodec
      info.chs = chs;
      info.bits = 16;
      info.freq = freq;
      audio_encoder = encoder.createAudioEncoder(info);
      short samples[];
      while (true) {
        Packet packet_in = decoder.readPacket();
        if (packet_in == null) break;
        samples = audio_decoder.decode(packet_in);
        if (samples == null) break;
        Packet packet_out = audio_encoder.encode(samples, 0, samples.length);
        if (packet_out != null) {
          encoder.writePacket(packet_out);
        }
      }
      audio_decoder.stop();
      decoder.close();
      audio_encoder.stop();
      encoder.close();
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

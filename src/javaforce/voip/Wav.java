package javaforce.voip;

import javaforce.*;
import java.io.*;

/** Loads a WAV file specific to VoIP requirements (must be 8000Hz, mono, 16bit PCM)
 *  Samples are returned in 20ms blocks (last partial block is chopped).
 */

public class Wav {
  public short[] samples;
  public String errmsg;
  private int pos, len;

  public boolean load(String fn) {
    errmsg = "";
    FileInputStream wav = null;
    try {
      wav = new FileInputStream(fn);
      return load(wav);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean load(InputStream wav) {
    errmsg = "";
    try {
      byte[] data = new byte[30];
      //read RIFF header (20 bytes);
      wav.read(data, 0, 20);
      if (!LE.getString(data, 0, 4).equals("RIFF")) throw new Exception("Not a valid WAV file (RIFF)");
      if (!LE.getString(data, 8, 4).equals("WAVE")) throw new Exception("Not a valid WAV file (WAVE)");
      if (!LE.getString(data, 12, 4).equals("fmt ")) throw new Exception("Not a valid WAV file (fmt )");
      int fmtsiz = LE.getuint32(data, 16);
      if ((fmtsiz < 16) || (fmtsiz > 30)) throw new Exception("Not a valid WAV file (fmtsiz)");
      wav.read(data, 0, fmtsiz);
      if (LE.getuint16(data, 0) != 1) throw new Exception("Not PCM");
      if (LE.getuint16(data, 2) != 1) throw new Exception("Not mono");
      if (LE.getuint32(data, 4) != 8000) throw new Exception("Not 8000Hz");
      if (LE.getuint16(data, 12) != 2) throw new Exception("Not 16bits");
      wav.read(data, 0, 8);
      while (!LE.getString(data, 0, 4).equals("data")) {
        //ignore block (FACT, INFO, etc.)
        len = LE.getuint32(data, 4);
        byte[] junk = new byte[len];
        wav.read(junk);
        wav.read(data, 0, 8);
      }
      if (!LE.getString(data, 0, 4).equals("data")) throw new Exception("Not a valid WAV file (data)");
      len = LE.getuint32(data, 4);
      if (len < 320) throw new Exception("Not a valid WAV file (len<320)");
      len = len / 320 * 320;  //chop to 20ms frames
      byte[] samples8 = JF.readAll(wav, len);
      samples = new short[len/2];
      len >>= 1;
      for(int a=0;a<len;a++) {
        samples[a] = samples8[a*2 + 1];
        samples[a] <<= 8;
        samples[a] += samples8[a*2 + 0];
      }
      try { if (wav != null) wav.close(); } catch (Exception e5) {}
    } catch (java.io.FileNotFoundException e2) {
      errmsg = "WAV file not found";
      try { if (wav != null) wav.close(); } catch (Exception e3) {}
      samples = null;
      return false;
    } catch (Exception e1) {
      errmsg = e1.toString();
      try { if (wav != null) wav.close(); } catch (Exception e4) {}
      samples = null;
      return false;
    }
    pos = 0;
    return true;
  }
  public boolean isLoaded() {
    return samples != null;
  }
  public short[] getSamples() {
    short[] buf = new short[160];
    System.arraycopy(samples, pos, buf, 0, 160);
    pos += 160;
    if (pos == len) pos = 0;
    return buf;
  }
  public void reset() {
    pos = 0;
  }
}

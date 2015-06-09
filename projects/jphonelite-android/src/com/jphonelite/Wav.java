package com.jphonelite;

import javaforce.*;
import java.io.*;

public class Wav {
  public short samples[];
  private int pos, len;

  public boolean load(String fn) {
    FileInputStream wav = null;
    try {
      byte data[] = new byte[30];
      wav = new FileInputStream(fn);
      //read RIFF header (20 bytes);
      wav.read(data, 0, 20);
      if (!getString(data, 0, 4).equals("RIFF")) throw new Exception(fn + " is not a valid WAV file (RIFF)");
      if (!getString(data, 8, 4).equals("WAVE")) throw new Exception(fn + " is not a valid WAV file (WAVE)");
      if (!getString(data, 12, 4).equals("fmt ")) throw new Exception(fn + " is not a valid WAV file (fmt )");
      int fmtsiz = getuint32(data, 16);
      if ((fmtsiz < 16) || (fmtsiz > 30)) throw new Exception(fn + " is not a valid WAV file (fmtsiz)");
      wav.read(data, 0, fmtsiz);
      if (getuint16(data, 0) != 1) throw new Exception(fn + " is not PCM");
      if (getuint16(data, 2) != 1) throw new Exception(fn + " is not mono");
      if (getuint32(data, 4) != 8000) throw new Exception(fn + " is not 8000Hz");
      if (getuint16(data, 12) != 2) throw new Exception(fn + " is not 16bits");
      wav.read(data, 0, 8);
      if (getString(data, 0, 4).equals("fact")) {
        //ignore FACT header
        len = getuint32(data, 4);
        byte junk[] = new byte[len];
        wav.read(junk);
        wav.read(data, 0, 8);
      }
      if (!getString(data, 0, 4).equals("data")) throw new Exception(fn + " is not a valid WAV file (data)");
      len = getuint32(data, 4);
      if (len < 320) throw new Exception(fn + " is not a valid WAV file (len<320)");
      len = len / 320 * 320;  //chop to 20ms frames
      byte samples8[] = new byte[len];
      samples = new short[len/2];
      wav.read(samples8, 0, len);
      len >>= 1;
      for(int a=0;a<len;a++) {
        samples[a] = samples8[a*2 + 1];
        samples[a] <<= 8;
        samples[a] += samples8[a*2 + 0];
      }
      try { if (wav != null) wav.close(); } catch (Exception e5) {}
    } catch (java.io.FileNotFoundException e2) {
      try { if (wav != null) wav.close(); } catch (Exception e3) {}
      samples = null;
      return false;
    } catch (Exception e1) {
      JFLog.log(e1);
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
    short buf[] = new short[160];
    System.arraycopy(samples, pos, buf, 0, 160);
    pos += 160;
    if (pos == len) pos = 0;
    return buf;
  }
  public void reset() {
    pos = 0;
  }
  private int getuint16(byte[] data, int offset) {
    int ret;
    ret  = (int)data[offset] & 0xff;
    ret += ((int)data[offset+1] & 0xff) << 8;
    return ret;
  }
  private int getuint32(byte[] data, int offset) {
    int ret;
    ret  = (int)data[offset] & 0xff;
    ret += ((int)data[offset+1] & 0xff) << 8;
    ret += ((int)data[offset+2] & 0xff) << 16;
    ret += ((int)data[offset+3] & 0xff) << 24;
    return ret;
  }
  private String getString(byte[] data, int offset, int len) {
    String ret = "";
    while (len > 0) {
      if (data[offset]==0) break;
      ret += (char)data[offset++];
      len--;
    }
    return ret;
  }
}

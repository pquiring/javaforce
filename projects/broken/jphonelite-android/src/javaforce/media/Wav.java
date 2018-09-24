package javaforce.media;

import java.io.*;
import java.nio.*;

import javaforce.*;

/** Load .wav audio files (supports 16,24,32bit,1-2 channels,any frequency,PCM only)
 * Note:readAllSamples() does not convert 24bit samples.
 */

public class Wav {
  public String errmsg;  //last errmsg if any
  public int chs = -1;  //# channels
  public int rate = -1;  //sample rate (freq)
  public int bits = -1;  //bits per sample
  public int bytes = -1;  //byte per sample
  public byte samples8[];  //if readAllSamples() was called
  public short samples16[];  //if readAllSamples() was called
  public int samples32[];  //if readAllSamples() was called
  public int dataLength;  //bytes

  private InputStream wav = null;

  public boolean load(String fn) {
    try {
      wav = new FileInputStream(fn);
      return load(wav);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean load(InputStream is) {
    wav = is;
    errmsg = "";
    try {
      byte data[] = new byte[30];
      //read RIFF header (20 bytes);
      wav.read(data, 0, 20);
      if (!LE.getString(data, 0, 4).equals("RIFF")) throw new Exception("wav is not a valid WAV file (RIFF)");
      if (!LE.getString(data, 8, 4).equals("WAVE")) throw new Exception("wav is not a valid WAV file (WAVE)");
      if (!LE.getString(data, 12, 4).equals("fmt ")) throw new Exception("wav is not a valid WAV file (fmt )");
      int fmtsiz = LE.getuint32(data, 16);
      if ((fmtsiz < 16) || (fmtsiz > 30)) throw new Exception("wav is not a valid WAV file (fmtsiz)");
      wav.read(data, 0, fmtsiz);
      if (LE.getuint16(data, 0) != 1) throw new Exception("wav is not PCM");
      chs = LE.getuint16(data, 2);
      if (chs < 1 || chs > 2) throw new Exception("wav is not supported (# chs)");
      rate = LE.getuint32(data, 4);
      bits = LE.getuint16(data, 14);
      switch (bits) {
//        case 8: bytes = 1; break;  //can't support 8bit for now (upscale later ???)
        case 16: bytes = 2; break;
        case 24: bytes = 3; break;
        case 32: bytes = 4; break;
        default: throw new Exception("wav is not supported (bits="+bits+")");
      }
      wav.read(data, 0, 8);
      while (true) {
        dataLength = LE.getuint32(data, 4);
        if (LE.getString(data, 0, 4).equals("data")) break;
        //ignore chunk (FACT, INFO, etc.)
        wav.skip(dataLength);
        wav.read(data, 0, 8);
      }
    } catch (java.io.FileNotFoundException e2) {
      errmsg = "WAV file not found";
      try { if (wav != null) wav.close(); } catch (Exception e3) {}
      return false;
    } catch (Exception e1) {
      errmsg = e1.toString();
      try { if (wav != null) wav.close(); } catch (Exception e4) {}
      return false;
    }
    return true;
  }
  /** Closes wav file */
  public void close() {
    try { wav.close(); } catch (Exception e) {}
  }
  /** Reads all samples and closes file. */
  public boolean readAllSamples() {
    try {
      samples8 = JF.readAll(wav, dataLength);
      wav.close();
      switch (bits) {
        case 8: return true;
        case 24:
          //TODO!!!???
          break;
        case 16:
          samples16 = new short[dataLength / 2];
          ByteBuffer.wrap(samples8).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples16);
          break;
        case 32:
          samples32 = new int[dataLength / 4];
          ByteBuffer.wrap(samples8).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(samples32);
          break;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Returns next chunk of samples. */
  public byte[] readSamples(int nSamples) {
    int byteLength = nSamples*bytes*chs;
    byte read8[];
    read8 = JF.readAll(wav, byteLength);
    if (read8 == null) return null;
    byte read32[];
    int lenXchs = nSamples * chs, pos = 0, pos24 = 0;
    switch (bits) {
      case 16:
        read32 = new byte[nSamples*2*chs];
        for(int a=0;a<lenXchs;a++) {
          read32[pos+0] = read8[pos+0];
          read32[pos+1] = read8[pos+1];
          pos += 2;
        }
        return read32;
      case 32:
        read32 = new byte[nSamples*4*chs];
        for(int a=0;a<lenXchs;a++) {
          read32[pos+0] = read8[pos+0];
          read32[pos+1] = read8[pos+1];
          read32[pos+2] = read8[pos+2];
          read32[pos+3] = read8[pos+3];
          pos += 4;
        }
        return read32;
      case 24:
        //must convert to 32bits
        read32 = new byte[nSamples*4*chs];
        for(int a=0;a<lenXchs;a++) {
          read32[pos+0] = read8[pos24+0];
          read32[pos+1] = read8[pos24+1];
          read32[pos+2] = read8[pos24+2];
          pos += 4;
          pos24 += 3;
        }
        return read32;
    }
    return null;
  }

  public boolean save(String fn) {
    try {
      FileOutputStream fos = new FileOutputStream(fn);
      boolean ret = save(fos);
      fos.close();
      return ret;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Save entire wav file (supports 16/32bit only) */
  public boolean save(OutputStream os) {
    if (bits != 16 && bits != 32) return false;
    int size = 0;
    switch (bits) {
      case 16:
        bytes = 2;
        size = samples16.length * 2;
        break;
      case 32:
        bytes = 4;
        size = samples32.length * 4;
        break;
    }
    try {
      byte data[] = new byte[20];
      //write RIFF header (20 bytes);
      LE.setString(data, 0, 4, "RIFF");
      LE.setuint32(data, 4, size + 36);  //rest of file size
      LE.setString(data, 8, 4, "WAVE");
      LE.setString(data, 12, 4, "fmt ");
      LE.setuint32(data, 16, 16);  //fmt size
      os.write(data, 0, 20);
      //write fmt header (16 bytes)
      data = new byte[16 + 4 + 4];
      LE.setuint16(data, 0, 1);  //PCM
      LE.setuint16(data, 2, chs);
      LE.setuint32(data, 4, rate);
      LE.setuint32(data, 8, bytes * chs * rate);  //bytes rate/sec
      LE.setuint32(data, 12, bytes * chs);  //block align
      LE.setuint16(data, 14, bits);
      LE.setString(data, 16, 4, "data");
      LE.setuint32(data, 20, size);
      os.write(data, 0, 16 + 4 + 4);
      switch (bits) {
        case 16: os.write(LE.shortArray2byteArray(samples16, null)); break;
        case 32: os.write(LE.intArray2byteArray(samples32, null)); break;
      }
    } catch (Exception e) {
      errmsg = e.toString();
      return false;
    }
    return false;
  }
}

package javaforce.voip;

import java.util.*;

/**
 * Either plays wav file or generates intermittent beep to party member on hold.
 */

public class MusicOnHold {

  private final double vol = 4000.0;
  private int idx = 0;
  private int off = 0;
  private static final int freq = 500;
  private static Wav wav;
  private int wavIdx;

  /** Fills next 20ms (160 samples) into buffer with MOH */
  public boolean getSamples(short buffer[]) {
    if (wav != null) {
      System.arraycopy(wav.samples, wavIdx, buffer, 0, 160);
      wavIdx += 160;
      if (wavIdx >= wav.samples.length) {
        wavIdx = 0;
      }
      return true;
    }
    //generate an intermitent beep
    idx++;
    if (idx == 50 * 6) idx = 0;
    if (idx < 50 * 5) {
      Arrays.fill(buffer, (short)0);
      return true;
    }

    for (int a = 0; a < 160; a++) {
      buffer[a] = (short) (Math.sin((2.0 * Math.PI / (8000.0 / freq)) * (a + off)) * vol);
    }
    off += 160;
    if (off == 8000) {
      off = 0;
    }
    return true;
  }

  public static void loadWav(String fn) {
    wav = new Wav();
    if (!wav.load(fn)) {
      wav = null;
    }
  }
}

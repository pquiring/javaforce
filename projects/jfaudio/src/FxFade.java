/**
 * Created : Jun 28, 2012
 *
 * @author pquiring
 */

import javaforce.*;

public class FxFade {
  public static void fadeIn(TrackPanel track) {
    long sStart, sStop, length;
    if (track.selectStop < track.selectStart) {
      sStart = track.selectStop;
      sStop = track.selectStart;
    } else {
      sStart = track.selectStart;
      sStop = track.selectStop;
    }
    length = (sStop - sStart + 1);
    //calc factor, slope
    double factor = 0.0;
    double slope = 1.0 / length;
    fade(track, sStart, sStop, length, factor, slope, 0.0);
  }
  public static void fadeOut(TrackPanel track) {
    long sStart, sStop, length;
    if (track.selectStop < track.selectStart) {
      sStart = track.selectStop;
      sStop = track.selectStart;
    } else {
      sStart = track.selectStart;
      sStop = track.selectStop;
    }
    length = (sStop - sStart + 1);
    //calc factor, slope
    double factor = 1.0;
    double slope = -1.0 / length;
    fade(track, sStart, sStop, length, factor, slope, 0.0);
  }
  private static void fade(TrackPanel track, long start, long end, long length, double factor, double slope, double adjust) {
    long offset = start;
    track.createModifyUndo();
    while (length > 0) {
      int read = TrackPanel.maxChunkSize;
      if (read > length) read = (int)length;
      for(int ch=0;ch<track.channels;ch++) {
        double xfactor = factor;
        byte samples[] = track.getSamples(offset, read, ch);
        switch (track.bits) {
          case 16:
            short samples16[] = LE.byteArray2shortArray(samples, null);
            for(int a=0;a<samples16.length;a++) {
              samples16[a] *= (xfactor + adjust);
              xfactor += slope;
            }
            samples = LE.shortArray2byteArray(samples16, null);
            break;
          case 32:
            int samples32[] = LE.byteArray2intArray(samples, null);
            for(int a=0;a<samples32.length;a++) {
              samples32[a] *= (xfactor + adjust);
              xfactor += slope;
            }
            samples = LE.intArray2byteArray(samples32, null);
            break;
        }
        track.setSamples(offset, samples, ch);
      }
      factor += slope * read;
      length -= read;
      offset += read;
    }
  }
}

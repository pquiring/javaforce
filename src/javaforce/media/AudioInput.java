package javaforce.media;

import java.util.ArrayList;
import javaforce.BE;
import javaforce.JFLog;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 * AudioInput.
 *
 * Samples are Big Endian
 *
 * @author pquiring
 */

public class AudioInput {
  private TargetDataLine tdl;
  private AudioFormat af;
  private byte[] buf8;

  public String[] listDevices() {
    ArrayList<String> mixers = new ArrayList<String>();
    Mixer.Info[] mi = AudioSystem.getMixerInfo();
    mixers.add("<default>");
    for (int a = 0; a < mi.length; a++) {
      String name = mi[a].getName();
      Mixer m = AudioSystem.getMixer(mi[a]);
      if (m.getTargetLineInfo().length == 0) {
        continue; //no target lines
      }
      mixers.add(name);
    }
    return mixers.toArray(new String[0]);
  }

  public boolean start(int chs, int freq, int bits, int bufsiz, String device) {
    buf8 = new byte[bufsiz];
    if (device == null) {
      device = "<default>";
    }
    af = new AudioFormat((float) freq, bits, chs, true, true);
    JFLog.log("AudioInput:AudioFormat=" + af);
    Mixer.Info[] mi = AudioSystem.getMixerInfo();
    int idx = -1;
    for (int a = 0; a < mi.length; a++) {
      if (mi[a].getName().equalsIgnoreCase(device)) {
        idx = a;
        break;
      }
    }
    try {
      if (idx == -1) {
        tdl = AudioSystem.getTargetDataLine(af);
      } else {
        tdl = AudioSystem.getTargetDataLine(af, mi[idx]);
      }
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    try {
      tdl.open(af);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    tdl.start();
    return true;
  }

  public boolean read(byte[] buf) {
    if (tdl.available() < buf.length) {
      return false; //do not block (causes audio glitches)
    }
    int ret = tdl.read(buf, 0, buf.length);
    if (ret != buf.length) {
      return false;
    }
    return true;
  }

  public boolean read(short[] buf16) {
    if (!read(buf8)) {
      return false;
    }
    BE.byteArray2shortArray(buf8, buf16);
    return true;
  }
  
  public void flush() {
    tdl.drain();
    tdl.flush();
  }

  public boolean stop() {
    if (tdl == null) {
      return false;
    }
    tdl.stop();
    tdl.close();
    tdl = null;
    return true;
  }

}

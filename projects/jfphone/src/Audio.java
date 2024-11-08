import java.util.*;

import javaforce.*;
import javaforce.media.*;
import javaforce.voip.*;

/** Handles all aspects of audio processing (recording, playback, ringing sounds, conference mixing, etc.) */

public class Audio {
  //audio data
  private short silence[] = new short[882];
  private short silence8[] = new short[160];
  private short silence16[] = new short[320];
  private short silence32[] = new short[640];
  private short mixed[] = new short[882];
  private short recording[] = new short[882];
  private short indata8[] = new short[160];
  private short indata16[] = new short[320];
  private short indata32[] = new short[640];
  private short outdata[] = new short[882];  //read from mic
  private short dspdata[] = new short[882];
  private short ringing[] = new short[882];
  private short callWaiting[] = new short[882];
  private short data8[] = new short[160];
  private short data16[] = new short[320];
  private short data32[] = new short[640];
  private AudioOutput output;
  private AudioInput input;
  private Timer timer;
  private Player player;
  private Reader reader;
  private Thread restart;
  private boolean primeOutput;
  private PhoneLine lines[];
  private int line = -1;
  private boolean inRinging = false, outRinging = false;
  private MeterController mc;
  private int volPlay = 100, volRec = 100;
  private boolean mute = false;
  private DTMF dtmf = new DTMF(44100);
  private boolean active = false;
  private Object activeLock = new Object();
  private int deactivateDelay;
  private static final int deactivateDelayInit = 50 * 5;  //5 seconds
  private int underBufferCount;
  private javaforce.voip.Wav inWav, outWav;
  private int speakerDelay = 0;
  private int sampleRate, sampleRate50;
  private long dsp_ctx = 0;

  /** Init audio system.  Audio needs access to the lines and the MeterController to send audio levels back to the panel. */

  public boolean init(PhoneLine lines[], MeterController mc) {
    this.lines = lines;
    this.mc = mc;
    sampleRate = 44100;
    sampleRate50 = sampleRate / 50;
    //setup inbound ring tone
    loadRingTones();

    dsp_ctx = speex.speex_dsp_init(44100);

    if (!start()) return false;

    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        process();
      }
    }, 0, 20);
    return true;
  }

  private void loadRingTones() {
    if (Settings.current.inRingtone.startsWith("*")) {
      if (Settings.current.inRingtone.equals("*RING")) {
        inWav = new javaforce.voip.Wav();
        inWav.load(getClass().getResourceAsStream("ringing.wav"));
      } else if (Settings.current.inRingtone.equals("*NA")) {
        initInRinging(440, 480, 2000, 4000, 2000, 4000);  //north america
      } else if (Settings.current.inRingtone.equals("*UK")) {
        initInRinging(400, 450, 400, 200, 400, 2000);  //UK
      } else {
        inWav = new javaforce.voip.Wav();
        inWav.load(getClass().getResourceAsStream("ringing.wav"));
      }
    } else {
      inWav = new javaforce.voip.Wav();
      if (!inWav.load(Settings.current.inRingtone)) {
        JFLog.log("Failed to load : " + Settings.current.inRingtone);
        inWav = new javaforce.voip.Wav();
        inWav.load(getClass().getResourceAsStream("ringing.wav"));
      }
    }
    //setup outbound ringback tone
    if (Settings.current.outRingtone.startsWith("*")) {
      if (Settings.current.outRingtone.equals("*RING")) {
        outWav = new javaforce.voip.Wav();
        outWav.load(getClass().getResourceAsStream("ringing.wav"));
      } else if (Settings.current.outRingtone.equals("*NA")) {
        initOutRinging(440, 480, 2000, 4000, 2000, 4000);  //north america
      } else if (Settings.current.outRingtone.equals("*UK")) {
        initOutRinging(400, 450, 400, 200, 400, 2000);  //UK
      } else {
        initOutRinging(440, 480, 2000, 4000, 2000, 4000);  //north america
      }
    } else {
      outWav = new javaforce.voip.Wav();
      if (!outWav.load(Settings.current.outRingtone)) {
        JFLog.log("Failed to load : " + Settings.current.outRingtone);
        initOutRinging(440, 480, 2000, 4000, 2000, 4000);  //north america
      }
    }
  }

  private boolean start() {
    output = new AudioOutput();
    input = new AudioInput();
    JFLog.log("output=" + output + ",input=" + input);
    output.listDevices();
    input.listDevices();
    deactivateDelay = deactivateDelayInit;
    underBufferCount = 0;
    if (!input.start(1, sampleRate, 16, sampleRate50, Settings.current.audioInput)) {
      JFLog.log("Input.start() failed");
      return false;
    }
    if (!output.start(1, sampleRate, 16, sampleRate50, Settings.current.audioOutput)) {
      JFLog.log("Output.start() failed");
      return false;
    }
    primeOutput = true;
    synchronized(activeLock) {
      active = true;
    }
    player = new Player();
    player.start();
    reader = new Reader();
    reader.start();
    return true;
  }

  int snd_id_play = -1, snd_id_record = -1;

  private void stop() {
    if (player != null) {
      player.cancel();
      player = null;
    }
    if (reader != null) {
      reader.cancel();
      reader = null;
    }
    if (active) {
      synchronized(activeLock) {
        active = false;
      }
      output.stop();
      input.stop();
    }
    if (mc != null) {
      mc.setMeterPlay(0);
      mc.setMeterRec(0);
    }
  }

  /** Frees resources. */

  public void uninit() {
    if (timer != null) {
      timer.cancel();
      timer = null;
      JF.sleep(25);  //wait for any pending timer events
    }
    if (record != null) {
      record.close();
      record = null;
    }
    stop();
    if (dsp_ctx != 0) {
      speex.speex_dsp_uninit(dsp_ctx);
      dsp_ctx = 0;
    }
    inWav = null;
    outWav = null;
  }

  /** Returns if software volume control on recording. */

  public boolean isSWVolRec() {
    return true;
  }

  /** Returns if software volume control on playing. */

  public boolean isSWVolPlay() {
    return true;
  }

  /** Changes which line user wants to listen to. */

  public void selectLine(int line) {
    this.line = line;
    if (player != null) {
      player.flush();
    }
    if (reader != null) {
      reader.flush();
    }
  }

  /** Changes software/hardware playback volume level. */

  public void setVolPlay(int lvl) {
    volPlay = lvl;
    Settings.current.volPlaySW = volPlay;
//    Settings.saveSettings();
  }

  /** Changes software/hardware recording volume level. */

  public void setVolRec(int lvl) {
    volRec = lvl;
    Settings.current.volRecSW = volRec;
//    Settings.saveSettings();
  }

  /** Sets mute state. */

  public void setMute(boolean state) {
    mute = state;
  }

  /** Scales samples to a software volume control. */

  private void scaleBufferVolume(short buf[], int len, int scale) {
    float fscale;
    if (scale == 0) {
      for (int a = 0; a < len; a++) {
        buf[a] = 0;
      }
    } else {
      if (scale <= 75) {
        fscale = 1.0f - ((75-scale) * 0.014f);
        for (int a = 0; a < len; a++) {
          buf[a] = (short) (buf[a] * fscale);
        }
      } else {
        fscale = 1.0f + ((scale-75) * 0.04f);
        float value;
        for (int a = 0; a < len; a++) {
          value = buf[a] * fscale;
          if (value < Short.MIN_VALUE) buf[a] = Short.MIN_VALUE;
          else if (value > Short.MAX_VALUE) buf[a] = Short.MAX_VALUE;
          else buf[a] = (short)value;
        }
      }
    }
  }

  /** Writes data to the audio system (output to speakers). */

  private void write(short buf[]) {
    if (player == null || !active) return;
    scaleBufferVolume(buf, 882, volPlay);
    if (primeOutput) {
      player.add(silence);
      primeOutput = false;
    }
    player.add(buf);
    synchronized(player.lock) {
      player.lock.notify();
    }
    int lvl = 0;
    for (int a = 0; a < 882; a++) {
      if (Math.abs(buf[a]) > lvl) lvl = Math.abs(buf[a]);
    }
    mc.setMeterPlay(lvl * 100 / 32768);
    if (Settings.current.speakerMode && !Settings.current.dsp_echo_cancel && (lvl >= Settings.current.speakerThreshold)) {
      if (speakerDelay <= 0) {
        mc.setSpeakerStatus(false);
      }
      speakerDelay = Settings.current.speakerDelay;
    }
  }

  /** Reads data from the audio system (input from mic). */

  private boolean read(short buf[]) {
    if (reader == null || !active) return false;
    if (!reader.read(buf)) return false;
    scaleBufferVolume(buf, buf.length, volRec);
    int lvl = 0;
    for (int a = 0; a < buf.length; a++) {
      if (Math.abs(buf[a]) > lvl) lvl = Math.abs(buf[a]);
    }
    mc.setMeterRec(lvl * 100 / 32768);
    if (speakerDelay > 0) {
      speakerDelay -= 20;
      System.arraycopy(silence, 0, buf, 0, 882);
      if (speakerDelay <= 0) {
        mc.setSpeakerStatus(true);
      }
    }
    return true;
  }

  /** Timer event that is triggered every 20ms.  Processes playback / recording. */

  public void process() {
    //20ms timer
    if (timer == null) return;
    long start = System.nanoTime();
    try {
      //do playback
      int cc = 0;  //conf count
      byte encoded[];
      if (!active) {
        for (int a = 0; a < 6; a++) {
          if ((lines[a].talking) || (lines[a].ringing) || (lines[a].dtmf != 'x')) {
            if (restart == null) {
              restart = new Thread() {
                public void run() {
                  Audio.this.start();
                  restart = null;
                }
              };
              restart.start();
            }
            break;
          }
        }
      } else {
        if (!Settings.current.keepAudioOpen) {
          int iuc = 0;  //in use count
          for (int a = 0; a < 6; a++) {
            if ((lines[a].talking) || (lines[a].ringing) || (lines[a].dtmf != 'x')) {
              iuc++;
            }
          }
          if (iuc == 0) {
            deactivateDelay--;
            if (deactivateDelay <= 0) {
              JFLog.log("Audio inactive");
              stop();
            }
          } else {
            deactivateDelay = deactivateDelayInit;
          }
        }
      }
      for (int a = 0; a < 6; a++) {
        if (lines[a].talking) {
          if ((lines[a].cnf) && (!lines[a].hld)) cc++;
        }
      }
      for (int a = 0; a < 6; a++) {
        if (lines[a].ringing && !lines[a].ringback && !lines[a].incoming) {
          if (!outRinging) {
            if (outWav != null) {
              outWav.reset();
            } else {
              startRinging();
            }
            outRinging = true;
          }
          break;
        }
        if (a == 5) {
          outRinging = false;
        }
      }
      for (int a = 0; a < 6; a++) {
        if (lines[a].incoming) {
          if (!inRinging) {
            if (inWav != null) {
              inWav.reset();
            } else {
              startRinging();
            }
            inRinging = true;
          }
          break;
        }
        if (a == 5) {
          inRinging = false;
        }
      }
      if ((cc > 1) && (line != -1) && (lines[line].cnf)) {
        //conference mode
        System.arraycopy(silence, 0, mixed, 0, 882);
        for (int a = 0; a < 6; a++) {
          if (lines[a].talking) {
            lines[a].samples = getSamples(a);
            if ((lines[a].samples != null) && (lines[a].cnf) && (!lines[a].hld)) {
              mix(mixed, lines[a].samples, 0 + a);
            }
          }
        }
        if (inRinging) mix(mixed, getCallWaiting(), 6);
        if (lines[line].dtmf != 'x') mix(mixed, dtmf.getSamples(lines[line].dtmf), 7);
        write(mixed);
      } else {
        //single mode
        System.arraycopy(silence, 0, mixed, 0, 882);
        if (line != -1) {
          if (lines[line].dtmf != 'x') mix(mixed, dtmf.getSamples(lines[line].dtmf), 8);
        }
        boolean inuse = false;
        for (int a = 0; a < 6; a++) {
          if (!lines[a].talking) continue;
          boolean doline = line == a && !lines[a].hld;
          RTPChannel channel = lines[a].audioRTP.getDefaultChannel();
          int rate = channel.coder.getSampleRate();
          switch (rate) {
            case 8000:  //G729, G711, GSM
              if (channel.getSamples(indata8) && doline) {
                mix(mixed, indata8, 9);
              }
              break;
            case 16000:  //G722, speex16
              if (channel.getSamples(indata16) && doline) {
                mix(mixed, indata16, 9);
              }
              break;
            case 32000:  //speex32
              if (channel.getSamples(indata32) && doline) {
                mix(mixed, indata32, 9);
              }
              break;
          }
          if (doline) {
            if (inRinging) mix(mixed, getCallWaiting(), 10);
            write(mixed);
            inuse = true;
          }
        }
        if (!inuse) {
          if (inRinging || outRinging) mix(mixed, getRinging(), 11);
          if (active) write(mixed);
        }
      }
      if (record != null) System.arraycopy(mixed, 0, recording, 0, 882);
      //do recording
      if (!active) return;
      if (read(outdata)) {
        underBufferCount = 0;
        if (Settings.current.speakerMode && Settings.current.dsp_echo_cancel && dsp_ctx != 0) {
          speex.speex_dsp_echo(dsp_ctx, outdata, mixed, dspdata);
          System.arraycopy(dspdata, 0, outdata, 0, 882);
        }
      } else {
        underBufferCount++;
        if (underBufferCount > 10) {  //a few is normal
          JFLog.log("Audio:mic underbuffer");
        }
        System.arraycopy(silence, 0, outdata, 0, 882);
      }
      if (mute) {
        System.arraycopy(silence, 0, outdata, 0, 882);
      }
      for (int a = 0; a < 6; a++) {
        if ((lines[a].talking) && (!lines[a].hld)) {
          if (lines[a].ringback) {
            //send only silence during ringback
            encoded = encodeSilence(lines[a].audioRTP.getDefaultChannel().coder);
          } else {
            if ((lines[a].cnf) && (cc > 1)) {
              //conference mode (mix = outdata + all other cnf lines except this one)
              System.arraycopy(outdata, 0, mixed, 0, 882);
              for (int b = 0; b < 6; b++) {
                if (b == a) continue;
                if ((lines[b].talking) && (lines[b].cnf) && (!lines[b].hld) && (lines[b].samples != null)) mix(mixed, lines[b].samples, 12 + b);
              }
              encoded = encode(lines[a].audioRTP.getDefaultChannel().coder, mixed, 36 + a);
              if (record != null && line == a) mix(recording, mixed, 18 + a);
            } else {
              //single mode
              if (line == a) {
                encoded = encode(lines[a].audioRTP.getDefaultChannel().coder, outdata, 42 + a);
                if (record != null) mix(recording, outdata, 24 + a);
              } else {
                encoded = encodeSilence(lines[a].audioRTP.getDefaultChannel().coder);
              }
            }
          }
          if (lines[a].dtmfend) {
            lines[a].audioRTP.getDefaultChannel().writeDTMF(lines[a].dtmf, true);
          } else if (lines[a].dtmf != 'x') {
            lines[a].audioRTP.getDefaultChannel().writeDTMF(lines[a].dtmf, false);
          } else {
            if (BasePhone.debug) {
              JFLog.log("Audio:writeRTP");
            }
            lines[a].audioRTP.getDefaultChannel().writeRTP(encoded,0,encoded.length);
          }
        }
        if (lines[a].dtmfend) {
          lines[a].dtmfend = false;
          lines[a].dtmf = 'x';
        }
      }
      if (record != null) record.add(recording);
      long stop = System.nanoTime();
      long diff = (stop - start) / 1000;
      if (diff > 20000) {
        JFLog.log("process took:" + diff);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Gets samples from RTPChannel for a line. */
  private short[] getSamples(int idx) {
    RTPChannel channel = lines[idx].audioRTP.getDefaultChannel();
    switch (channel.coder.getSampleRate()) {
      case 8000:
        if (!channel.getSamples(lines[idx].samples8)) return null;
        return lines[idx].samples8;
      case 16000:
        if (!channel.getSamples(lines[idx].samples16)) return null;
        return lines[idx].samples16;
      case 32000:
        if (!channel.getSamples(lines[idx].samples32)) return null;
        return lines[idx].samples32;
    }
    return null;
  }

  private byte[] encode(RTPAudioCoder coder, short in[], int bufIdx) {
    byte encoded[] = null;
    int rate = coder.getSampleRate();
    switch (rate) {
      case 8000:
        interpolate(data8, in, bufIdx);
        encoded = coder.encode(data8);
        break;
      case 16000:
        interpolate(data16, in, bufIdx);
        encoded = coder.encode(data16);
        break;
      case 32000:
        interpolate(data32, in, bufIdx);
        encoded = coder.encode(data32);
        break;
    }
    return encoded;
  }

  private byte[] encodeSilence(RTPAudioCoder coder) {
    byte encoded[] = null;
    int rate = coder.getSampleRate();
    switch (rate) {
      case 8000:
        encoded = coder.encode(silence8);
        break;
      case 16000:
        encoded = coder.encode(silence16);
        break;
      case 32000:
        encoded = coder.encode(silence32);
        break;
    }
    return encoded;
  }

  /** These samples hold the last sample of a buffer used to interpolate the next
   * block of samples.
   */

  private short lastSamples[] = new short[48];

  /** Mixes 'in' samples into 'out' samples.
   * Uses linear interpolation if out.length != in.length
   *
   * bufIdx : array index into lastSamples to store last sample used in interpolation
   *
   */

  public void mix(short out[], short in[], int bufIdx) {
    int outLength = out.length;
    int inLength = in.length;
    if (outLength == inLength) {
      //no interpolation
      for (int a = 0; a < outLength; a++) {
        out[a] += in[a];
      }
    } else {
      //linear interpolation
      //there is a one sample delay due to interpolation
      float d = ((float)inLength) / ((float)outLength);
      float p1 = 1.0f;
      float p2 = 0.0f;
      short s1 = lastSamples[bufIdx];
      short s2 = in[0];
      int inIdx = 1;
      int outIdx = 0;
      while (true) {
        out[outIdx++] += (s1 * p1 + s2 * p2);
        if (outIdx == outLength) break;
        p1 -= d;
        p2 += d;
        while (p1 < 0.0f) {
          s1 = s2;
          s2 = in[inIdx++];
          p1 += 1.0f;
          p2 -= 1.0f;
        }
      }
      lastSamples[bufIdx] = s2;
    }
  }

  /** Interpolate 'in' onto 'out' (does not mix) */

  public void interpolate(short out[], short in[], int bufIdx) {
    int outLength = out.length;
    int inLength = in.length;
    if (outLength == inLength) {
      //no interpolation
      for (int a = 0; a < outLength; a++) {
        out[a] = in[a];
      }
    } else {
      //linear interpolation
      //there is a one sample delay due to interpolation
      float d = ((float)inLength) / ((float)outLength);
      float p1 = 1.0f;
      float p2 = 0.0f;
      short s1 = lastSamples[bufIdx];
      short s2 = in[0];
      int inIdx = 1;
      int outIdx = 0;
      while (true) {
        out[outIdx++] = (short)(s1 * p1 + s2 * p2);
        if (outIdx == outLength) break;
        p1 -= d;
        p2 += d;
        while (p1 < 0.0f) {
          s1 = s2;
          s2 = in[inIdx++];
          p1 += 1.0f;
          p2 -= 1.0f;
        }
      }
      lastSamples[bufIdx] = s2;
    }
  }

  /** Starts a generated ringing phone sound. */

  private void startRinging() {
    outRingFreqCount = 0;
    outRingCycle = 0;
    outRingCount = 0;
    outRingFreq1Pos = 0.0;
    outRingFreq2Pos = 0.0;

    inRingFreqCount = 0;
    inRingCycle = 0;
    inRingCount = 0;
    inRingFreq1Pos = 0.0;
    inRingFreq2Pos = 0.0;

    waitCount = 0;
    waitCycle = 0;
  }

  private final double ringVol = 9000.0;

  private int outRingFreq1, outRingFreq2;
  private int outRingFreqCount;
  private double outRingFreq1Pos, outRingFreq2Pos;
  private int outRingCycle;
  private int outRingCount;
  private int outRingTimes[] = new int[4];

  private int inRingFreq1, inRingFreq2;
  private int inRingFreqCount;
  private double inRingFreq1Pos, inRingFreq2Pos;
  private int inRingCycle;
  private int inRingCount;
  private int inRingTimes[] = new int[4];

  private void initOutRinging(int freq1, int freq2, int o1, int p1, int o2, int p2) {
    outRingFreq1 = freq1;
    outRingFreq2 = freq2;
    outRingTimes[0] = o1;
    outRingTimes[1] = p1;
    outRingTimes[2] = o2;
    outRingTimes[3] = p2;
  }

  private void initInRinging(int freq1, int freq2, int o1, int p1, int o2, int p2) {
    inRingFreq1 = freq1;
    inRingFreq2 = freq2;
    inRingTimes[0] = o1;
    inRingTimes[1] = p1;
    inRingTimes[2] = o2;
    inRingTimes[3] = p2;
  }

  private static final double pi2 = Math.PI * 2.0;

  /** Returns next 20ms of ringing phone. */

  public short[] getRinging() {
    if (outRinging && outWav != null) {
      return outWav.getSamples();
    }
    if (inRinging && inWav != null) {
      return inWav.getSamples();
    }
    if (outRinging) {
      outRingCount += 20;
      if (outRingCount == outRingTimes[outRingCycle]) {
        outRingCount = 0;
        outRingCycle++;
        if (outRingCycle == 4) outRingCycle = 0;
      }
      if (outRingCycle % 2 == 1) {
        outRingFreqCount = 0;
        outRingFreq1Pos = 0.0;
        outRingFreq2Pos = 0.0;
        return silence;
      }
      //freq1
      double theta1 = pi2 * outRingFreq1 / 44100.0;
      for (int a = 0; a < 882; a++) {
        ringing[a] = (short) (Math.sin(outRingFreq1Pos) * ringVol);
        outRingFreq1Pos += theta1;
      }
      //freq2
      double theta2 = pi2 * outRingFreq2 / 44100.0;
      for (int a = 0; a < 882; a++) {
        ringing[a] += (short) (Math.sin(outRingFreq2Pos) * ringVol);
        outRingFreq2Pos += theta2;
      }
      outRingFreqCount += 882;
      if (outRingFreqCount == 44100) {
        outRingFreqCount = 0;
        outRingFreq1Pos = 0.0;
        outRingFreq2Pos = 0.0;
      }
      return ringing;
    }
    if (inRinging) {
      inRingCount += 20;
      if (inRingCount == inRingTimes[inRingCycle]) {
        inRingCount = 0;
        inRingCycle++;
        if (inRingCycle == 4) inRingCycle = 0;
      }
      if (inRingCycle % 2 == 1) {
        inRingFreqCount = 0;
        inRingFreq1Pos = 0.0;
        inRingFreq2Pos = 0.0;
        return silence;
      }
      //freq1
      double theta1 = pi2 * inRingFreq1 / 44100.0;
      for (int a = 0; a < 882; a++) {
        ringing[a] = (short) (Math.sin(inRingFreq1Pos) * ringVol);
        inRingFreq1Pos += theta1;
      }
      //freq2
      double theta2 = pi2 * inRingFreq2 / 44100.0;
      for (int a = 0; a < 882; a++) {
        ringing[a] = (short) (Math.sin(inRingFreq2Pos) * ringVol);
        inRingFreq2Pos += theta2;
      }
      inRingFreqCount += 882;
      if (inRingFreqCount == 44100) {
        inRingFreqCount = 0;
        inRingFreq1Pos = 0.0;
        inRingFreq2Pos = 0.0;
      }
      return ringing;
    }
    return silence;  //does not happen
  }

  private int waitCount;
  private double waitPos;
  private static final double waitTheta = pi2 * 440.0 / 44100.0;
  private int waitCycle;

  /** Returns next 20ms of a generated call waiting sound (beep beep). */

  public short[] getCallWaiting() {
    //440 (2 bursts for 0.3 seconds)
    //2on 2off 2on 200off[4sec]
    waitCycle++;
    if (waitCycle == 206) waitCycle = 0;
    if ((waitCycle > 6) || (waitCycle == 2) || (waitCycle == 3)) {
      waitCount = 0;
      waitPos = 0.0;
      return silence;
    }
    //440
    for (int a = 0; a < 882; a++) {
      callWaiting[a] = (short) (Math.sin(waitPos) * ringVol);
      waitPos += waitTheta;
    }
    waitCount += 882;
    if (waitCount == 44100) {
      waitCount = 0;
      waitPos = 0.0;
    }
    return callWaiting;
  }

  public Record record;

  private class Player extends Thread {
    private volatile boolean playerActive = true;
    private volatile boolean done = false;
    private AudioBuffer buffer = new AudioBuffer(44100, 1, 2);  //freq, chs, seconds
    public final Object lock = new Object();
    public void run() {
      short buf[] = new short[882];
      while (playerActive) {
        synchronized(lock) {
          if (buffer.size() < 882) {
            try { lock.wait(); } catch (Exception e) {}
          }
          if (buffer.size() < 882) continue;
        }
        buffer.get(buf, 0, 882);
        synchronized(activeLock) {
          if (active) {
            output.write(buf);
          }
        }
      }
      done = true;
    }
    public void add(short buf[]) {
      buffer.add(buf, 0, 882);
    }
    public void flush() {
      buffer.clear();
    }
    public void cancel() {
      playerActive = false;
      while (!done) {
        synchronized(lock) {
          lock.notify();
        }
        JF.sleep(10);
      }
    }
  }
  private class Reader extends Thread {
    private volatile boolean readerActive = true;
    private volatile boolean done = false;
    private AudioBuffer buffer = new AudioBuffer(44100, 1, 2);  //freq, chs, seconds
    public void run() {
      short buf[] = new short[882];
      while (readerActive) {
        synchronized(activeLock) {
          if (active) {
            if (input.read(buf)) {
              buffer.add(buf, 0, 882);
              continue;
            }
          }
        }
        JF.sleep(10);
      }
      done = true;
    }
    public void flush() {
      buffer.clear();
    }
    public void cancel() {
      readerActive = false;
      while (!done) {
        JF.sleep(10);
      }
    }
    public boolean read(short buf[]) {
      if (buffer.size() < buf.length) return false;
      buffer.get(buf, 0, buf.length);
      return true;
    }
  }
}

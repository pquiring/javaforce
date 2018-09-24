package com.jphonelite;

import javaforce.*;
import javaforce.media.*;
import javaforce.voip.*;

import java.io.*;
import java.util.*;

import android.util.*;
import android.media.*;
import android.content.*;

/** Handles all aspects of sound processing (recording, playback, ringing sounds, conference mixing, etc.) */

public class Audio {
  //sound data
  private short silence[] = new short[882];
  private short silence8[] = new short[160];
  private short silence16[] = new short[320];
  private short mixed[] = new short[882];
  private short recording[] = new short[882];
  private short indata8[] = new short[160];
  private short indata16[] = new short[320];
  private short outdata[] = new short[882];  //read from mic
  private short ringing[] = new short[882];
  private short callWaiting[] = new short[882];
  private short data8[] = new short[160];
  private short data16[] = new short[320];
  private Timer timer;
  private PhoneLine lines[];
  private int line = -1;
  private boolean inRinging = false, outRinging = false, isRinging = false;
  private DTMF dtmf = new DTMF(8000);  //44100 ???
  private volatile boolean is_playing = false;
  private volatile boolean is_recording = false;
  private AudioRecord ar;
  private AudioTrack at;
  private long atPlay, atBuffer;
  private int atStream = -1;
  private boolean mute = false;  //unchangeable for now
  private AudioManager am;
  private boolean first = true;
  private WriteThread writeThread;
  private ReadThread readThread;
  private javaforce.voip.Wav inWav, outWav;
  private int speakerDelay = 0;
  private boolean restartAudioTrack = false;
  private boolean restartAudioRecord = false;

  private void showThread(String type) {
    JFLog.log("thread(" + type + ") = " + Thread.currentThread());
  }

  /** Init sound system.  Sound needs access to the lines and the MeterController to send audio levels back to the panel. */

  public boolean init(PhoneLine lines[], Context ctx) {
    uninit();  //Engine.reinit() will recall init() without uninit() first
    this.lines = lines;
    JFLog.log("ar.min=" + AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
    JFLog.log("at.min=" + AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT));
    inWav = new javaforce.voip.Wav();
    inWav.load("/sdcard/inringtone.wav");
    outWav = new javaforce.voip.Wav();
    outWav.load("/sdcard/outringtone.wav");
    try {
      am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
//      showThread("init");
      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          process();
        }
      }, 0, 20);
      initInRinging(440, 480, 2000, 4000, 2000, 4000);  //north america
      initOutRinging(440, 480, 2000, 4000, 2000, 4000);  //north america
    } catch (Exception e) {
      JFLog.log("err:sound init", e);
      return false;
    }
    JFLog.log("Audio.init() ok");
    return true;
  }

  /** Frees resources. */

  public void uninit() {
    //must stop and release all resources
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (ar != null) {
      if (is_recording) {
        ar.stop();
        is_recording = false;
      }
      ar.release();
      ar = null;
    }
    if (at != null) {
      if (is_playing) {
        at.stop();
        is_playing = false;
      }
      at.release();
      at = null;
    }
  }

  /** Changes which line user wants to use. */

  public void selectLine(int newline) {
    line = newline;
  }

  /** Writes data to the audio system (output to speakers). */

  private void write(short buf[]) {
    boolean created = false;
    if ( ( (atStream != AudioManager.STREAM_RING) && (isRinging) ) || ( (atStream == AudioManager.STREAM_RING) && (!isRinging) ) ) {
      restartAudioTrack = true;
    }
    if (restartAudioTrack) {
      if (at != null) {
        if (is_playing) {
//          playing = false;  //we are still playing
          at.stop();
        }
        at.release();
        at = null;
      }
      restartAudioTrack = false;
    }
    if (at == null) {
      atStream = isRinging ? AudioManager.STREAM_RING : (Settings.current.speakerMode ? AudioManager.STREAM_MUSIC : AudioManager.STREAM_VOICE_CALL);
      if (!createAudioTrack()) {
        at = null;
        return;
      }
      created = true;
    }
    if (!created) {
      atPlay = System.nanoTime() / 1000;
      long diff = atBuffer - atPlay;
      if (diff < 0) {
        JFLog.log("Warning:output buffer underflow");
        at.write(silence, 0, 882);
        atBuffer = atPlay + 20000;
      }
    }
    int len = at.write(buf, 0, 882);
    if (created) {
      atPlay = System.nanoTime() / 1000;
      atBuffer = atPlay + 40000;  //+20 for silence write() in createAudioTrack(), +20 for this write()
      at.play();
    } else {
      atBuffer += 20000;
    }
    int lvl = 0;
    for (int a = 0; a < 882; a++) {
      if (Math.abs(buf[a]) > lvl) lvl = Math.abs(buf[a]);
    }
    if ((Settings.current.speakerMode) && (lvl >= Settings.current.speakerThreshold)) {
      if (speakerDelay <= 0) {
        Main._setBackgroundResource(Main.spk, R.drawable.spk_red);
      }
      speakerDelay = Settings.current.speakerDelay;
    }
  }

  /** Reads data from the audio system (input from mic). */

  private boolean read(short buf[]) {
    if (restartAudioRecord) {
      if (ar != null) {
        if (is_recording) {
          ar.stop();
        }
        ar.release();
        ar = null;
      }
      restartAudioRecord = false;
    }
    if (ar == null) {
      for(int a=0;a<10;a++) {
        createAudioRecord();
        if ((ar != null) && (ar.getState() != ar.STATE_UNINITIALIZED)) {
          break;
        }
        if (ar != null) {
          ar.release();  //release bad AudioRecord
          ar = null;
        }
      }
      if ((ar == null) || (ar.getState() == ar.STATE_UNINITIALIZED)) {
        JFLog.log("ERROR: Unable to open MIC");
        return false;
      }
      ar.startRecording();
    }
    int len = ar.read(buf, 0, 882);
    if (speakerDelay > 0) {
      speakerDelay -= 20;
      System.arraycopy(silence, 0, buf, 0, 882);
      if ((speakerDelay <= 0) && (Settings.current.speakerMode)) {
        Main._setBackgroundResource(Main.spk, R.drawable.spk_green);
      }
    }
    return true;
  }

  /** Flushes output buffers.  Should be called at start of a call. */
  public void flush() {
    if (at == null) return;
    at.flush();
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
    }
    return null;
  }

  private byte[] encode(Coder coder, short in[], int bufIdx) {
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
    }
    return encoded;
  }

  private byte[] encodeSilence(Coder coder) {
    byte encoded[] = null;
    int rate = coder.getSampleRate();
    switch (rate) {
      case 8000:
        encoded = coder.encode(silence8);
        break;
      case 16000:
        encoded = coder.encode(silence16);
        break;
    }
    return encoded;
  }

  /** Timer event that is triggered every 20ms.  Processes playback / recording. */

  public synchronized void process() {
    //20ms timer
    //do playback
    long startTime = System.currentTimeMillis();
    try {
//JFLog.log("Audio.process() : " + System.nanoTime() / 1000000);
      if (first) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        first = false;
      }
      int cc = 0;  //conf count
      byte encoded[];
      if (!is_playing) {
        for (int a = 0; a < 6; a++) {
          if ((lines[a].talking) || (lines[a].ringing)) {
            is_playing = true;
            writeThread = new WriteThread();
            writeThread.start();
            break;
          }
        }
      } else {
//JFLog.log("playing:wlist.size()==" + wlist.size());
        int pc = 0;  //playing count
        for (int a = 0; a < 6; a++) {
          if ((lines[a].talking) || (lines[a].ringing)) {
            pc++;
          }
        }
        if (pc == 0) {
//          JFLog.log("playing = false");
          is_playing = false;
          at.stop();
          at.release();
          at = null;
        }
      }
      for (int a = 0; a < 6; a++) {
        if ((lines[a].talking) && (lines[a].cnf) && (!lines[a].hld)) cc++;
      }
      for (int a = 0; a < 6; a++) {
        if (lines[a].ringing) {
          if (!outRinging) {
            startRinging();
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
            if (inWav.isLoaded()) {
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
        if (inRinging) mix(mixed, getCallWaiting());
        if (lines[line].dtmf != 'x') {
          mix(mixed, dtmf.getSamples(lines[line].dtmf));
        }
        isRinging = false;
        wlistAdd(mixed);
      } else {
        //single mode
        System.arraycopy(silence, 0, mixed, 0, 882);
        if (line != -1) {
          if (lines[line].dtmf != 'x') mix(mixed, dtmf.getSamples(lines[line].dtmf), 8);
        }
        if ((line != -1) && (lines[line].talking) && (!lines[line].hld)) {
          RTPChannel channel = lines[line].audioRTP.getDefaultChannel();
          int rate = channel.coder.getSampleRate();
          switch (rate) {
            case 8000:  //G729, G711
              if (channel.getSamples(indata8)) {
                mix(mixed, indata8, 9);
              }
              break;
            case 16000:  //G722
              if (channel.getSamples(indata16)) {
                mix(mixed, indata16, 9);
              }
              break;
          }
          if (inRinging) mix(mixed, getCallWaiting(), 10);
          isRinging = false;
          wlistAdd(mixed);
        } else {
          if (inRinging || outRinging) mix(mixed, getRinging());
          isRinging = inRinging;
          if (is_playing) wlistAdd(mixed);
        }
      }
//JFLog.log("Audio.process(2) : " + System.currentTimeMillis() );
      if (!is_playing) {
        if (is_recording) {
          is_recording = false;
          ar.stop();
          ar.release();
          ar = null;
        }
        return;
      } else {
        if (!is_recording) {
          is_recording = true;
          readThread = new ReadThread();
          readThread.start();
        }
      }
      //do recording
      if (readThread.buffer.size() < 882) {
        JFLog.log("Warning : input buffer underflow");
        System.arraycopy(silence, 0, outdata, 0, 882);
      } else {
        readThread.buffer.get(outdata, 0, 882);
        if (mute) {
          System.arraycopy(silence, 0, outdata, 0, 882);
        }
      }
      for (int a = 0; a < 6; a++) {
        if ((lines[a].talking) && (!lines[a].hld)) {
          if ((lines[a].cnf) && (cc > 1)) {
            //conference mode (mix = data + all other cnf lines except this one)
            System.arraycopy(outdata, 0, mixed, 0, 882);
            for (int b = 0; b < 6; b++) {
              if (b == a) continue;
              if ((lines[b].talking) && (lines[b].cnf) && (!lines[b].hld)) mix(mixed, lines[b].samples);
            }
            encoded = encode(lines[a].audioRTP.getDefaultChannel().coder, mixed, 36 + a);
          } else {
            //single mode
            if (line == a) {
              encoded = encode(lines[a].audioRTP.getDefaultChannel().coder, outdata, 42 + a);
            } else {
              encoded = encodeSilence(lines[a].audioRTP.getDefaultChannel().coder);
            }
          }
          if (lines[a].dtmfend) {
            lines[a].audioRTP.getDefaultChannel().writeDTMF(lines[a].dtmf, true);
            lines[a].dtmfend = false;
            lines[a].dtmf = 'x';
          } else if (lines[a].dtmf != 'x') {
            lines[a].audioRTP.getDefaultChannel().writeDTMF(lines[a].dtmf, false);
            lines[a].dtmfcnt--;
            if (lines[a].dtmfcnt <= 0) {
              lines[a].dtmfend = true;
            }
          } else {
            lines[a].audioRTP.getDefaultChannel().writeRTP(encoded,0,encoded.length);
          }
        }
      }
      if (System.currentTimeMillis() - startTime > 20) {
        JFLog.log("WARNING : Sound.process() took more than 20ms");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Mixes 'in' samples into 'out' samples. */

  public void mix(short out[], short in[]) {
    for (int a = 0; a < 882; a++) {
      out[a] += in[a];
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

  /** Returns next 20ms of a generated ringing phone. */

  public short[] getRinging() {
    if (outRinging && outWav.isLoaded()) {
      return outWav.getSamples();
    }
    if (inRinging && inWav.isLoaded()) {
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

  public void restartSound() {
//    JFLog.log("Audio.restartSound():speakerMode=" + Settings.current.speakerMode);
//    am.setSpeakerphoneOn(Settings.current.speakerMode);  //doesn't work???  (I use STREAM_MUSIC instead - another ugly hack)
//    am.setMicrophoneMute(false);  //not supported
//    am.setWiredHeadsetOn(Settings.current.use_earpiece);  //deprecated???
//    am.setBluetoothScoOn(false);  //not supported
    restartAudioTrack = true;
  }

  private boolean createAudioTrack() {
    JFLog.log("atStream = " + atStream);
    try {
      at = new AudioTrack(atStream, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 16 * 1024, AudioTrack.MODE_STREAM);
    } catch (Exception e) {
      JFLog.log("err:createAudioTrack failed:", e);
      return false;
    }
    //fill the buffer a little to avoid jitter
    at.write(silence, 0, 882);
    return true;
  }

  private boolean createAudioRecord() {
    try {
      ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 16 * 1024);
    } catch (Exception e) {
      JFLog.log("err:createAudioRecord failed:", e);
      return false;
    }
    return true;
  }

  private class ReadThread extends Thread {
    public AudioBuffer buffer = new AudioBuffer(44100, 1, 2);  //freq, chs, seconds
    public void run() {
      short sams[];
      android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
      sams = new short[882];
      try {
        while (is_recording) {
          if (read(sams)) {
            buffer.add(sams,0,882);
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private synchronized void wlistAdd(short buf[]) {
    writeThread.buffer.add(buf,0,buf.length);
    synchronized(writeThread.lock) {
      writeThread.lock.notify();
    }
  }

  private class WriteThread extends Thread {
    private volatile boolean active = true;
    private volatile boolean done = false;
    public AudioBuffer buffer = new AudioBuffer(44100, 1, 2);  //freq, chs, seconds
    public final Object lock = new Object();
    public void run() {
      short buf[] = new short[882];
      try {
        while (active) {
          synchronized(lock) {
            if (buffer.size() < 882) {
              try { lock.wait(); } catch (Exception e) {}
            }
            if (buffer.size() < 882) continue;
          }
          buffer.get(buf, 0, 882);
          write(buf);
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      done = true;
    }
    public void cancel() {
      active = false;
      while (!done) {
        synchronized(lock) {
          lock.notify();
        }
        JF.sleep(10);
      }
    }
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
}

package com.jphonelite;

import javaforce.voip.*;
import javaforce.*;

import java.io.*;
import java.util.*;

import android.util.*;
import android.media.*;
import android.content.*;

/** Handles all aspects of sound processing (recording, playback, ringing sounds, conference mixing, etc.) */

public class Sound {
  //sound data
  private short silence[] = new short[160];
  private short silence441[] = new short[882];
  private short mixed[] = new short[160];
  private Timer timer;
  private PhoneLine lines[];
  private int line = -1;
  private short data[] = new short[160];
  private short playdata441[] = new short[882];
  private short recdata441[] = new short[882];
  private boolean inRinging = false, outRinging = false, isRinging = false;
  private DTMF dtmf = new DTMF();
  private volatile boolean playing = false;
  private volatile boolean recording = false;
  private AudioRecord ar;
  private AudioTrack at;
  private int atStream = -1;
  private boolean mute = false;  //unchangeable for now
  private boolean rec441 = false;
  private boolean play441 = false;
  private AudioManager am;
  private boolean first = true;
  private Thread readThread, writeThread;
  private Vector<short[]> rlist, wlist;
  private Wav wav;
  private int speakerDelay = 0;
  private final String TAG = "JPLSOUND";
  private boolean restartAudioTrack = false;
  private boolean restartAudioRecord = false;

  private void showThread(String type) {
    Log.i(TAG, "thread(" + type + ") = " + Thread.currentThread());
  }

  /** Init sound system.  Sound needs access to the lines and the MeterController to send audio levels back to the panel. */

  public boolean init(PhoneLine lines[], Context ctx) {
    this.lines = lines;
    int idx;
    Log.i(TAG, "ar.min=" + AudioRecord.getMinBufferSize(rec441 ? 44100 : 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
    Log.i(TAG, "at.min=" + AudioTrack.getMinBufferSize(play441 ? 44100 : 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT));
    wav = new Wav();
    wav.load("/sdcard/ringtone.wav");
    try {
      am = (AudioManager)ctx.getSystemService(Context.AUDIO_SERVICE);
//      showThread("init");
      timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
        public void run() {
          process();
        }
      }, 0, 20);
    } catch (Exception e) {
      Log.i(TAG, "err:sound init", e);
      return false;
    }
    Log.i(TAG, "Sound.init() ok");
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
      if (recording) {
        ar.stop();
        recording = false;
      }
      ar.release();
      ar = null;
    }
    if (at != null) {
      if (playing) {
        at.stop();
        playing = false;
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
    if (restartAudioTrack) {
      if (at != null) {
        if (playing) {
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
      if (!createAudioTrack()) return;
      created = true;
    } else {
      //check if we need to switch to/from STREAM_RING / STREAM_VOICE_CALL
      if ( ( (atStream != AudioManager.STREAM_RING) && (isRinging) ) || ( (atStream == AudioManager.STREAM_RING) && (!isRinging) ) ) {
        //free AudioTrack and recreate it
        at.stop();
        at.release();
        at = null;
        atStream = isRinging ? AudioManager.STREAM_RING : AudioManager.STREAM_VOICE_CALL;
        if (!createAudioTrack()) return;
        restartAudioRecord = true;
        created = true;
      }
    }
    int len = -1;
    if (play441) {
      //sample to 44100Hz (160 / 882 = 0.1814...)
      int p = 0;  //buf position
      int f = 0;  //fraction
      for(int a=0;a<882;a++) {
        playdata441[a] = buf[p];
        f += 1814;
        if (f > 10000) {
          p++;
          f -= 10000;
        }
      }
//Log.i(TAG, "Sound.process(w1) : " + System.currentTimeMillis() + ":head=" + at.getPlaybackHeadPosition());
      len = at.write(playdata441, 0, 882);
//Log.i(TAG, "Sound.process(w2) : " + System.currentTimeMillis() + ":head=" + at.getPlaybackHeadPosition());
    } else {
//Log.i(TAG, "Sound.process(w1) : " + System.currentTimeMillis() + ":head=" + at.getPlaybackHeadPosition());
      len = at.write(buf, 0, 160);
//Log.i(TAG, "Sound.process(w2) : " + System.currentTimeMillis() + ":head=" + at.getPlaybackHeadPosition());
    }
//    Log.i(TAG, "at.write()=" + len);
    if (created) at.play();
    int lvl = 0;
    for (int a = 0; a < 160; a++) {
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
    int len;
    if (restartAudioRecord) {
      if (ar != null) {
        if (recording) {
          ar.stop();
        }
        ar.release();
        ar = null;
      }
      restartAudioRecord = false;
    }
    if (ar == null) {
      createAudioRecord();
      for(int a=0;a<10;a++) {
        if ((ar == null) || (ar.getState() == ar.STATE_UNINITIALIZED)) {
          if (ar != null) {
            ar.release();  //release bad AudioRecord
            ar = null;
          }
          createAudioRecord();  //retry
        } else {
          break;
        }
      }
      if ((ar == null) || (ar.getState() == ar.STATE_UNINITIALIZED)) {
        Log.i(TAG, "ERROR: Unable to open MIC");
        return false;
      }
      ar.startRecording();
    }
    if (rec441) {
      len = ar.read(recdata441, 0, 882);
      if (len != 882) return false;
      //sample to 8000Hz  (882 / 160 = 5.5125)
      int p = 0;  //441 position
      int f = 0;  //fraction
      for(int a=0;a<160;a++) {
        buf[a] = recdata441[p];
        p += 5;
        f += 5125;
        if (f > 10000) {
          p++;
          f -= 10000;
        }
      }
    } else {
//Log.i(TAG, "Sound.process(r1) : " + System.currentTimeMillis());
      len = ar.read(buf, 0, 160);
//Log.i(TAG, "Sound.process(r2) : " + System.currentTimeMillis());
      if (len != 160) return false;
    }
//Log.i(TAG, "ar.read() = " + len + ":" + buf[0]);
    if (speakerDelay > 0) {
      speakerDelay -= 20;
      System.arraycopy(silence, 0, buf, 0, 160);
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

  /** Timer event that is triggered every 20ms.  Processes playback / recording. */

  public synchronized void process() {
    //20ms timer
    //do playback
//    showThread("process");
    long startTime = System.currentTimeMillis();
    try {
//Log.i(TAG, "Sound.process(1) : " + System.currentTimeMillis());
      if (first) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        first = false;
      }
      int cc = 0;  //conf count
      byte encoded[];
      if (!playing) {
        for (int a = 0; a < 6; a++) {
          if ((lines[a].talking) || (lines[a].ringing)) {
            playing = true;
            wlist = new Vector<short[]>();
            writeThread = new WriteThread();
            writeThread.start();
            break;
          }
        }
      } else {
//Log.i(TAG, "playing:wlist.size()==" + wlist.size());
        int pc = 0;  //playing count
        for (int a = 0; a < 6; a++) {
          if ((lines[a].talking) || (lines[a].ringing)) {
            pc++;
          }
        }
        if (pc == 0) {
//          Log.i(TAG, "playing = false");
          playing = false;
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
            if (wav.isLoaded()) {
              wav.reset();
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
        System.arraycopy(silence, 0, mixed, 0, 160);
        for (int a = 0; a < 6; a++) {
          if ((lines[a].talking) && (lines[a].cnf) && (!lines[a].hld) && (lines[a].rtp.getSamples(lines[a].samples)))
            mix(mixed, lines[a].samples);
        }
        if (inRinging) mix(mixed, getCallWaiting());
        if (lines[line].dtmf != 'x') {
          mix(mixed, dtmf.getSamples(lines[line].dtmf));
        }
        isRinging = false;
        wlistAdd(mixed);
      } else {
        //single mode
        System.arraycopy(silence, 0, mixed, 0, 160);
        if ((line != -1) && (lines[line].talking) && (!lines[line].hld)) {
          if (lines[line].dtmf != 'x') {
            mix(mixed, dtmf.getSamples(lines[line].dtmf));
          }
          if (lines[line].rtp.getSamples(data)) mix(mixed, data); else Log.i(TAG, "WARNING : rtp.getSamples() was empty.");
          if (inRinging) mix(mixed, getCallWaiting());
          isRinging = false;
          wlistAdd(mixed);
        } else {
          if (inRinging || outRinging) mix(mixed, getRinging());
          isRinging = inRinging;
          if (playing) wlistAdd(mixed);
        }
      }
//Log.i(TAG, "Sound.process(2) : " + System.currentTimeMillis() );
      if (!playing) {
        if (recording) {
          recording = false;
          ar.stop();
          ar.release();
          ar = null;
        }
        return;
      } else {
        if (!recording) {
          recording = true;
          rlist = new Vector<short[]>();
          readThread = new ReadThread();
          readThread.start();
        }
      }
      //do recording
      if (rlist.size() == 0) {
Log.i(TAG, "WARNING : No recording data available (a few warnings is normal)");
        System.arraycopy(silence, 0, data, 0, 160);
      } else {
//Log.i(TAG, "INFO : rlist.size()=" + rlist.size());
        if (mute)
          System.arraycopy(silence, 0, data, 0, 160);
        else
          System.arraycopy(rlist.get(0), 0, data, 0, 160);
        rlist.remove(0);
      }
      for (int a = 0; a < 6; a++) {
        if ((lines[a].talking) && (!lines[a].hld)) {
          if ((lines[a].cnf) && (cc > 1)) {
            //conference mode (mix = data + all other cnf lines except this one)
            System.arraycopy(data, 0, mixed, 0, 160);
            for (int b = 0; b < 6; b++) {
              if (b == a) continue;
              if ((lines[b].talking) && (lines[b].cnf) && (!lines[b].hld)) mix(mixed, lines[b].samples);
            }
            encoded = lines[a].rtp.coder.encode(mixed);
          } else {
            //single mode
//Log.i(TAG, "Sound.process(3) : " + System.currentTimeMillis() );
            if (line == a)
              encoded = lines[a].rtp.coder.encode(data);
            else
              encoded = lines[a].rtp.coder.encode(silence);
          }
//Log.i(TAG, "Sound.process(4) : " + System.currentTimeMillis() );
          if (lines[a].dtmfend) {
            lines[a].rtp.getDefaultChannel().writeDTMF(lines[a].dtmf, true);
            lines[a].dtmfend = false;
            lines[a].dtmf = 'x';
//Log.i(TAG, "dtmfend");
          } else if (lines[a].dtmf != 'x') {
            lines[a].rtp.getDefaultChannel().writeDTMF(lines[a].dtmf, false);
            lines[a].dtmfcnt--;
            if (lines[a].dtmfcnt <= 0) {
              lines[a].dtmfend = true;
            }
          } else {
            lines[a].rtp.getDefaultChannel().writeRTP(encoded,0,encoded.length);
          }
        }
      }
//Log.i(TAG, "Sound.process(5) : " + System.currentTimeMillis() + "\n");
      if (System.currentTimeMillis() - startTime > 20) {
        Log.i(TAG, "WARNING : Sound.process() took more than 20ms");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Mixes 'in' samples into 'out' samples. */

  public void mix(short out[], short in[]) {
    for (int a = 0; a < 160; a++) {
      out[a] += in[a];
    }
  }

  /** Starts a generated ringing phone sound. */

  public void startRinging() {
    ring_440 = 0;
    ring_480 = 0;
    ringCycle = 0;
    ringCount = 0;
    wait_440 = 0;
    waitCycle = 0;
  }

  private final double ringVol = 8000.0;
  private int ring_440, ring_480;
  private int ringCycle;
  private int ringCount;
  private int wait_440;
  private int waitCycle;

  /** Returns next 20ms of a generated ringing phone. */

  public short[] getRinging() {
    //440 + 480
    //2 seconds on/3 seconds off
    if ((inRinging) && (wav.isLoaded())) {
      return wav.getSamples();
    }
    ringCount += 160;
    if (ringCount == 8000) {
      ringCount = 0;
      ringCycle++;
    }
    if (ringCycle == 5) ringCycle = 0;
    if (ringCycle > 1) {
      ring_440 = 0;
      ring_480 = 0;
      return silence;
    }
    short buf[] = new short[160];
    //440
    for (int a = 0; a < 160; a++) {
      buf[a] = (short) (Math.sin((2.0 * Math.PI / (8000.0 / 440.0)) * (a + ring_440)) * ringVol);
    }
    ring_440 += 160;
    if (ring_440 == 8000) ring_440 = 0;
    //480
    for (int a = 0; a < 160; a++) {
      buf[a] += (short) (Math.sin((2.0 * Math.PI / (8000.0 / 480.0)) * (a + ring_480)) * ringVol);
    }
    ring_480 += 160;
    if (ring_480 == 8000) ring_480 = 0;
    return buf;
  }

  /** Returns next 20ms of a generated call waiting sound (beep beep). */

  public short[] getCallWaiting() {
    //440 (2 bursts for 0.3 seconds)
    //2on 2off 2on 200off[4sec]
    waitCycle++;
    if (waitCycle == 206) waitCycle = 0;
    if ((waitCycle > 6) || (waitCycle == 2) || (waitCycle == 3)) {
      wait_440 = 0;
      return silence;
    }
    short buf[] = new short[160];
    //440
    for (int a = 0; a < 160; a++) {
      buf[a] = (short) (Math.sin((2.0 * Math.PI / (8000.0 / 440.0)) * (a + wait_440)) * ringVol);
    }
    wait_440 += 160;
    if (wait_440 == 8000) wait_440 = 0;
    return buf;
  }

  public void restartSound() {
//    Log.i(TAG, "Sound.restartSound():speakerMode=" + Settings.current.speakerMode);
//    am.setSpeakerphoneOn(Settings.current.speakerMode);  //doesn't work???  (I use STREAM_MUSIC instead - another ugly hack)
//    am.setMicrophoneMute(false);  //not supported
//    am.setWiredHeadsetOn(Settings.current.use_earpiece);  //deprecated???
//    am.setBluetoothScoOn(false);  //not supported
    restartAudioTrack = true;
  }

  private boolean createAudioTrack() {
//    Log.i(TAG, "atStream = " + atStream);
    try {
      at = new AudioTrack(atStream, play441 ? 44100 : 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 16 * 1024, AudioTrack.MODE_STREAM);
    } catch (Exception e) {
      Log.i(TAG, "err:createAudioTrack failed:", e);
      return false;
    }
    //fill the buffer a little to avoid jitter
    if (play441)
      at.write(silence441, 0, 882);
    else
      at.write(new short[1024], 0, 1024);
    return true;
  }

  private boolean createAudioRecord() {
    try {
      ar = new AudioRecord(MediaRecorder.AudioSource.MIC, rec441 ? 44100 : 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 16 * 1024);
    } catch (Exception e) {
      Log.i(TAG, "err:createAudioRecord failed:", e);
      return false;
    }
    return true;
  }

  private class ReadThread extends Thread {
    public void run() {
      short buffer[];
      android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
      while (recording) {
        buffer = new short[160];
        if (read(buffer)) {
          rlist.add(buffer);
        }
      }
    }
  }

  private synchronized void wlistAdd(short buf[]) {
    short newbuf[] = new short[160];
    System.arraycopy(buf, 0, newbuf, 0, 160);
    wlist.add(newbuf);
  }

  private class WriteThread extends Thread {
    private int cnt = 0;
    public void run() {
      short buffer[];
      int size;
      android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
      while (playing) {
        if (wlist.size() == 0) {
Log.i(TAG, "WARNING : WriteThread using silence (a few is normal)");
          buffer = silence;
        } else {
//Log.i(TAG, "wlist.size()=" + wlist.size());
          buffer = wlist.get(0);
          wlist.remove(0);
          if (wlist.size() > 5) wlist.remove(0);  //flush buffer
        }
        write(buffer);
      }
    }
  }
}

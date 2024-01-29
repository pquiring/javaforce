package javaforce.media;

/**
 * Music player.
 *
 * Plays tracked music.
 * Although similar to MOD type files it is not compatible with any and conversion would be difficult.
 * Also supports adding sound effects during playback.
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;

public class Music {
  public static final int VERSION = 1;
  public static interface Listener {
    /** Triggered when the song ends (even if repeating) */
    public void musicEnded();
    /** Triggered each time a chunk of samples is going to be played.
     Use to apply effects, etc.*/
    public void musicSamples(short[] samples);
    /** Triggered as the music moves to each row. */
    public void musicRow(int sequence, int pattern, int row);
  }
  public static class Notes {
    public int A = 0;
    public int As = 1;
    public int B = 2;
    public int C = 3;
    public int Cs = 4;
    public int D = 5;
    public int Ds = 6;
    public int E = 7;
    public int F = 8;
    public int Fs = 9;
    public int G = 10;
    public int Gs = 11;
  }
  private enum Play {song, pattern, note};
  public static class Version implements Serializable {
    public static final long serialVersionUID = 1L;
    public int version = VERSION;
  }
  public static class Track implements Serializable {
    public static final long serialVersionUID = 1L;
    public byte startInstrument;
    public float startVolL, startVolR;  //0.0f - 1.0f : full vol = max(L,R)
    public float startVolVibrateSpeed;  //speed for vibrate cmds
    public float startPanVibrateSpeed;  //speed for vibrate cmds  //use vol
    public float startFreqVibrateSpeed;  //speed for vibrate cmds
    public byte[] notes = new byte[64];  //MIDI : 0=C-2 ... 0x7f = ??? (-1 = no note)
    public byte[] volcmds = new byte[64];
    public int[] volparams = new int[64];  //or float
    public byte[] fxcmds = new byte[64];
    public int[] fxparams = new int[64];  //or float
    public int flags;  //not used

    public transient float sIdx;  //sample index
    public transient float freq;
    public transient float freqPow2;
    public transient boolean playing;
    public transient Instrument i;  //instrument
    public transient Region r;  //region
    public transient Sample s;  //sample
    public transient int volcmd;  //current fx
    public transient float volparam;  //current param
    public transient int fxcmd;  //current fx
    public transient float fxparam;  //current param
    public transient float volL, volR;  //volumes
    public transient float vol;  //volume
    public transient boolean loop;  //holding note (looping)
    public transient boolean sustain;  //looping in sustain
    public transient float volVibrateSpeed, panVibrateSpeed, freqVibrateSpeed;
    public transient float port;  //port up/down
    public transient float lastFreq, targetFreq;  //port to note
    public transient int on, off;  //tremor
    public transient boolean on_off;  //tremor
    public transient int on_off_cnt;  //tremor
    public transient boolean mute;
    public transient int delay;
    public transient float volSlide, panSlide;
    public transient float volVibratePos;
    public transient float volVibrateDir;
    public transient float volVibrateMag;
    public transient float panVibratePos;
    public transient float panVibrateDir;
    public transient float panVibrateMag;
    public transient float freqVibratePos;
    public transient float freqVibrateDir;
    public transient float freqVibrateMag;
    public transient boolean interpolate;  //linear interpolation (see http://en.wikipedia.org/wiki/Interpolation)
    public transient float attenuation;

    public Track() {
      //set default values
      for(int n=0;n<64;n++) {
        notes[n] = -1;
      }
      startVolL = 1.0f;
      startVolR = 1.0f;
      startVolVibrateSpeed = 1.0f / 44100.0f * 0.25f;
      startPanVibrateSpeed = 1.0f / 44100.0f * 0.25f;
      startFreqVibrateSpeed = 1.0f / 44100.0f * 0.25f;
    }

    public void reset(Music music) {
      sIdx = 0.0f;
      freq = -0.0f;  //negative zero denotes no current note
      playing = false;
      i = music.song.instruments.get(startInstrument);
      volL = startVolL;
      volR = startVolR;

      volVibrateSpeed = startVolVibrateSpeed;
      panVibrateSpeed = startPanVibrateSpeed;
      freqVibrateSpeed = startFreqVibrateSpeed;

      nextNote(music);
    }

    public void nextNote(Music music) {
      lastFreq = freq;
      int idx = music.rowIdx;
      int note = notes[idx];
      if (note != -1) {
        sIdx = 0.0f;
        interpolate = note % 12 != 0;
        playing = true;
        vol = 1.0f;
        r = i.getRegion(note);
        s = music.song.samples.get(r.sample);
        freq = (note - r.unity) / 12.0f;
        freqPow2 = (float)Math.pow(2.0, freq);
        sustain = s.sustainStart != -1;
        loop = !sustain && s.loopStart != -1;
        attenuation = s.attenuation;
      }
      setCmds(volcmds[idx], volparams[idx], fxcmds[idx], fxparams[idx], music);
    }

    /** Assign new commands */
    public void setCmds(byte volcmd, int volparam, byte fxcmd, int fxparam, Music music) {
      this.volcmd = volcmd;
      this.volparam = volparam;
      mute = false;
      delay = 0;
      switch (volcmd) {
        case Music.VOLCMD_SET_VOLUME:
          vol = Float.intBitsToFloat(volparam);
          break;
        case Music.VOLCMD_SET_VOL_VIBRATE_SPEED:
          volVibrateSpeed = Float.intBitsToFloat(volparam);
          break;
        case Music.VOLCMD_SET_PAN_VIBRATE_SPEED:
          panVibrateSpeed = Float.intBitsToFloat(volparam);
          break;
        case Music.VOLCMD_TREMOLO:  //vibrate vol
          volVibrateMag = Float.intBitsToFloat(volparam);
          volVibrateDir = volVibrateSpeed;
          volVibratePos = 0.0f;
          break;
        case Music.VOLCMD_PANBRELLO:  //vibrate pan
          panVibrateMag = Float.intBitsToFloat(volparam);
          panVibrateDir = panVibrateSpeed;
          panVibratePos = 0.0f;
          break;
        case Music.VOLCMD_SET_PANNING:
          float pan = Float.intBitsToFloat(volparam);
          if (pan == 0.0f) {
            volR = 1.0f;
            volL = 1.0f;
          } else if (pan > 0.0f) {
            volR = 1.0f;
            volL = 1.0f - pan;
          } else {
            volL = 1.0f;
            volR = 1.0f + pan;
          }
          break;
        case Music.VOLCMD_SLIDE:
          volSlide = Float.intBitsToFloat(volparam);
          break;
        case Music.VOLCMD_PAN_SLIDE:
          panSlide = Float.intBitsToFloat(volparam);
          break;
      }
      this.fxcmd = fxcmd;
      this.fxparam = fxparam;
      switch (fxcmd) {
        case FXCMD_KEY_OFF:
          sustain = false;
          break;
        case FXCMD_CUT_OFF:
          playing = false;
          break;
        case FXCMD_PATTERN_BREAK:
          music.rowIdx = 63;
          break;
        case Music.FXCMD_PORTAMENTO_TO_NOTE:
          if (fxparam != 0) port = Float.intBitsToFloat(fxparam);
          targetFreq = freq;
          freq = lastFreq;
          break;
        case Music.FXCMD_SET_VIBRATE_SPEED:
          freqVibrateSpeed = Float.intBitsToFloat(fxparam);
          break;
        case Music.FXCMD_VIBRATO:
          freqVibrateMag = Float.intBitsToFloat(fxparam);
          freqVibrateDir = freqVibrateSpeed;
          freqVibratePos = 0.0f;
          break;
        case Music.FXCMD_PORTAMENTO:
          if (fxparam != 0) port = Float.intBitsToFloat(fxparam);
          break;
        case Music.FXCMD_TREMOR:
          if (fxparam != 0) {
            on = fxparam >>> 16;
            if (on == 0) on++;
            off = fxparam & 0xffff;
            if (off == 0) off++;
            on_off = true;
            on_off_cnt = 0;
          }
          break;
        case Music.FXCMD_SET_INSTRUMENT:
          i = music.song.instruments.get(fxparam);
          break;
        case Music.FXCMD_DELAY_START:
          delay = fxparam;
          break;
        case Music.FXCMD_SAMPLE_OFFSET:
          sIdx = fxparam;
          break;
        case Music.FXCMD_SET_BPM:
          music.samplesPerBeat = 44100 * 60 / fxparam;
          music.samplesThisBeat = 0;
          break;
      }
    }

    /** Process commands (per sample) */
    public void doCmds() {
      switch (volcmd) {
        case Music.VOLCMD_TREMOLO:
          if (volVibrateDir > 0.0f) {
            vol += volVibrateMag;
          } else {
            vol -= volVibrateMag;
          }
          if (vol > 1.0f) vol = 1.0f;
          if (vol < 0.0f) vol = 0.0f;
          volVibratePos += volVibrateDir;
          if (volVibratePos > 1.0f || volVibratePos < 0.0f) {
            volVibrateDir *= -1.0f;
          }
          break;
        case Music.VOLCMD_PANBRELLO:
          if (panVibrateDir > 0.0f) {
            volR += panVibrateMag;
            volL -= panVibrateMag;
          } else {
            volR -= panVibrateMag;
            volL += panVibrateMag;
          }
          if (volR > 1.0f) volR = 1.0f;
          if (volR < 0.0f) volR = 0.0f;
          if (volL > 1.0f) volL = 1.0f;
          if (volL < 0.0f) volL = 0.0f;
          panVibratePos += panVibrateDir;
          if (panVibratePos > 1.0f || panVibratePos < 0.0f) {
            panVibrateDir *= -1.0f;
          }
          break;
        case Music.VOLCMD_SLIDE:
          vol += volSlide;
          if (vol > 1.0f) vol = 1.0f;
          if (vol < 0.0f) vol = 0.0f;
          break;
        case Music.VOLCMD_PAN_SLIDE:
          volR += panSlide;
          if (volR > 1.0f) volR = 1.0f;
          if (volR < 0.0f) volR = 0.0f;
          volL -= panSlide;
          if (volL > 1.0f) volL = 1.0f;
          if (volL < 0.0f) volL = 0.0f;
          break;
      }
      switch (fxcmd) {
        case Music.FXCMD_PORTAMENTO_TO_NOTE:
          if (freq != targetFreq) {
            if (freq < targetFreq) {
              freq += port;
              if (freq > targetFreq) freq = targetFreq;
            } else {
              freq -= port;
              if (freq < targetFreq) freq = targetFreq;
            }
            freqPow2 = (float)Math.pow(2.0, freq);
            interpolate = true;
          }
          break;
        case Music.FXCMD_VIBRATO:
          if (freqVibrateDir > 0.0f) {
            freq += freqVibrateMag;
          } else {
            freq -= freqVibrateMag;
          }
          freqPow2 = (float)Math.pow(2.0, freq);
          interpolate = true;
          freqVibratePos += freqVibrateDir;
          if (freqVibratePos > 1.0f || freqVibratePos < 0.0f) {
            freqVibrateDir *= -1.0f;
          }
          break;
        case Music.FXCMD_PORTAMENTO:
          freq += port;
          freqPow2 = (float)Math.pow(2.0, freq);
          interpolate = true;
          break;
        case Music.FXCMD_TREMOR:
          mute = on_off;
          on_off_cnt++;
          if (on_off && on_off_cnt == on) {
            on_off = false;
            on_off_cnt = 0;
          }
          else if (!on_off && on_off_cnt == off) {
            on_off = true;
            on_off_cnt = 0;
          }
          break;
      }
    }
  }

  public static class Pattern implements Serializable {
    public static final long serialVersionUID = 1L;
    public String name;  //stanza name (verse, chorus, etc.)
    public int bpm = 80;
    public int flags;  //not used
    public ArrayList<Track> tracks = new ArrayList<Track>();

    public void reset(Music music) {
      music.samplesPerBeat = 44100 * 60 / bpm;
      music.samplesThisBeat = 0;
      music.rowIdx = 0;
      int nTracks = tracks.size();
      for(int a=0;a<nTracks;a++) {
        tracks.get(a).reset(music);
      }
    }
    public void addTracks(int cnt) {
      for(int a=0;a<cnt;a++) {
        tracks.add(new Track());
      }
    }
  }
  /** Each audio sample for an Instrument must be 16bit, 44100Hz, mono */
  public static class Instrument implements Serializable {
    public static final long serialVersionUID = 1L;
    public String name;
    public int flags;  //not used
    public ArrayList<Region> regions = new ArrayList<Region>();

    public Region getRegion(int note) {
      for(int a=0;a<regions.size();a++) {
        Region r = regions.get(a);
        if (note >= r.low && note <= r.high) {
          return r;
        }
      }
      return null;
    }
  }

  public static class Region implements Serializable {
    public static final long serialVersionUID = 1L;
    public int low, high, unity;
    public int fineTune;  //not currently used
    public int flags;  //not used
    public int sample;
  }

  public static class Sample implements Serializable {
    public static final long serialVersionUID = 1L;
    public String name;
    public int loopStart, loopEnd;
    public int sustainStart, sustainEnd;
    public float attenuation;  //volume drop per sample (recommend: 0.00001f thru 0.00005f)
    public int flags;  //not used
    public short[] samples;
    public Sample() {
      loopStart = loopEnd = -1;
      sustainStart = sustainEnd = -1;
    }
  }

  public static class Song implements Serializable {
    public static final long serialVersionUID = 1L;
    public String name, comment;
    public int flags;
    public ArrayList<Pattern> patterns = new ArrayList<Pattern>();
    public ArrayList<Integer> sequence = new ArrayList<Integer>();  //Integer = index into patterns
    public ArrayList<Instrument> instruments = new ArrayList<Instrument>();
    public ArrayList<Sample> samples = new ArrayList<Sample>();
  }

  private Track[] sounds = new Track[0];

  public Song song;

  private boolean playing;  //music ready?
  private Object lock = new Object();
  private Play play;

  public static final byte NOTE_NONE = -1;

  public static final byte VOLCMD_NONE               = 0;
  public static final byte VOLCMD_SET_VOLUME         = 1;  //float+ = 0.0 - 1.0
  public static final byte VOLCMD_SET_PANNING        = 2;  //float-+ = -1.0 - 1.0
  public static final byte VOLCMD_SLIDE              = 3;  //float-+
  public static final byte VOLCMD_PAN_SLIDE          = 4;  //float-+
  public static final byte VOLCMD_SET_VOL_VIBRATE_SPEED  = 5;  //float+ = sets vibrate Hz (default defined in track)
  public static final byte VOLCMD_SET_PAN_VIBRATE_SPEED  = 6;  //float+ = sets vibrate Hz (default defined in track)
  public static final byte VOLCMD_TREMOLO            = 7;  //float+ = vibrate volume  (sets distance)
  public static final byte VOLCMD_PANBRELLO          = 8;  //float+ = vibrate panning
  public static final byte VOLCMD_MASTER_FLAG        = (byte)0x80;  //same as all above except effects ALL tracks

  public static final byte FXCMD_NONE                =  0;
  public static final byte FXCMD_PORTAMENTO          =  1;  //float-+ = pitch slide up (per sample)
  public static final byte FXCMD_PORTAMENTO_TO_NOTE  =  2;  //float+ = change pitch from last note to this one
  public static final byte FXCMD_SET_VIBRATE_SPEED   =  3;  //float+ = sets vibrate Hz (default defined in track)
  public static final byte FXCMD_VIBRATO             =  4;  //float+ = vibrate pitch
  public static final byte FXCMD_TREMOR              =  5;  //short/short = turns sample on/off rapidly (short = time in samples)
  public static final byte FXCMD_PATTERN_BREAK       =  6;  //no param = end pattern
  public static final byte FXCMD_KEY_OFF             =  7;  //no param = end sustain (may start looping if defined)
  public static final byte FXCMD_CUT_OFF             =  8;  //no param = end note
  public static final byte FXCMD_SET_INSTRUMENT      =  9;  //int = change instrument
  public static final byte FXCMD_DELAY_START         = 10;  //int = samples delay
  public static final byte FXCMD_SAMPLE_OFFSET       = 11;  //int = samples offset
  public static final byte FXCMD_SET_BPM             = 12;  //int = new bpm

  private Listener listener;
  private AudioOutput output;
  private int samplesPerBuffer;
  private short[] samples;
  private Timer timer;

  //sound channel stuff (not related to music)
  private static ArrayList<Instrument> loadedSounds = new ArrayList<Instrument>();
  private static ArrayList<Sample> loadedSoundsSamples = new ArrayList<Sample>();

  private int seqIdx = 0;  //current seq #
  private int patternIdx = 0;  //current pattern
  private Pattern pattern;  //current pattern
  private boolean repeatSong;  //repeat song continuously
  private float m_volL = 1.0f, m_volR = 1.0f;  //overall music volume level
  private float s_volL = 1.0f, s_volR = 1.0f;  //overall sound volume level
  private int samplesPerBeat;
  private int samplesThisBeat;
  private int rowIdx;  //0-63

  public static final float[] octave12 = new float[12];  //freq for each note in one octave (12 steps)

  static {
    float step = 1.0f / 12.0f;
    float value = 1.0f;
    for(int a=0;a<12;a++) {
      octave12[a] = value;
      value += step;
    }
  }

  private static final int chs = 2;

  /** Starts the sound output engine.
   * After you can start music or sound playback.
   */
  public boolean start(int milliSec, int soundChannels) {
    if (output != null) stop();
    output = new AudioOutput();
    sounds = new Track[soundChannels];
    for(int a=0;a<soundChannels;a++) {
      sounds[a] = new Track();
    }
    samplesPerBuffer = 44100 * milliSec / 1000;
    samples = new short[samplesPerBuffer * chs];
    if (!output.start(chs, 44100, 16, samplesPerBuffer * chs * 2, null)) {  //*2 = bytes
      JFLog.log("Music.start() failed");
      return false;
    }
    //prime the buffers (a buffer underflow causes delay in output)
    output.write(samples);
    output.write(samples);
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {public void run() {process();}}, milliSec / 2, milliSec);
    return true;
  }

  /** Stops the sound output engine, which stops music and sound output.  */
  public void stop() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (output != null) {
      output.stop();
      output = null;
    }
  }

  public static Song load(String fn) {
    try {
      FileInputStream fis = new FileInputStream(fn);
      Song song = load(fis);
      fis.close();
      return song;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static Song load(InputStream is) {
    try {
      ObjectInputStream ois = new ObjectInputStream(is);
      Version version = (Version)ois.readObject();
      Song song = (Song)ois.readObject();
      if (song.samples == null) song.samples = new ArrayList<Sample>();  //beta upgrade
      return song;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public boolean load(Song song) {
    close();
    this.song = song;
    return true;
  }

  public boolean save(String fn) {
    //save a .mproj file
    try {
      FileOutputStream fos = new FileOutputStream(fn);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      song.flags = 0;
      oos.writeObject(new Version());
      oos.writeObject(song);
      fos.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void close() {
    synchronized(lock) {
      playing = false;
    }
    song = null;
  }
  public void reset() {
    close();
    song = new Song();
    Pattern pattern = new Pattern();
    pattern.name = "Verse 1";
    pattern.bpm = 80;
    pattern.addTracks(4);
    song.patterns.add(pattern);
  }
  public void replay() {
    synchronized(lock) {
      playing = false;
    }
    seqIdx = 0;
    rowIdx = 0;
    if (!prepNextPattern()) return;
    playing = true;
  }
  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void playSong(boolean repeat) {
    if (playing) {
      JFLog.log("Music:error:playSong:already playing");
      return;
    }
    this.repeatSong = repeat;
    play = Play.song;
    seqIdx = 0;
    rowIdx = 0;
    if (!prepNextPattern()) return;
    if (listener != null) listener.musicRow(seqIdx, patternIdx, rowIdx);
    playing = true;
  }

  public void playPattern(int patternIdx) {
    if (playing) {
      JFLog.log("Music:error:playPattern:already playing");
      return;
    }
    play = Play.pattern;
    seqIdx = -1;
    this.patternIdx = patternIdx;
    rowIdx = 0;
    pattern = song.patterns.get(patternIdx);
    pattern.reset(this);
    if (listener != null) listener.musicRow(seqIdx, patternIdx, rowIdx);
    playing = true;
  }

  public void playRow(int patternIdx, int rowIdx) {
    if (playing) return;
    play = Play.note;
    seqIdx = -1;
    this.patternIdx = patternIdx;
    this.rowIdx = rowIdx;
    pattern = song.patterns.get(patternIdx);
    pattern.reset(this);
    playing = true;
  }

  /** Stops current music (but sounds continue to play) */
  public void stopMusic() {
    synchronized(lock) {
      playing = false;
    }
  }

  public boolean isRunning() {
    return output != null;
  }

  public boolean isPlaying() {
    return playing;
  }

  /** Plays one note of an instrument thru a sound channel, returns idx of sound channel used.
   * @param idx = instrument index
   * @param volL,volR = left/right volume (0.0 - 1.0)
   * @param note = note to play (midi value)
   * @return idx to use with other Sound channel functions, -1 if can't play now
   */
  public synchronized int instrumentPlay(int idx, float volL, float volR, int note) {
    for(int a=0;a<sounds.length;a++) {
      if (!sounds[a].playing) {
        Track t = sounds[a];
        t.volL = volL;
        t.volR = volR;
        t.vol = 1.0f;
        t.i = song.instruments.get(idx);
        t.r = t.i.getRegion(note);
        t.s = song.samples.get(t.r.sample);
        t.freq = (note - t.r.unity) / 12.0f;
        t.freqPow2 = (float)Math.pow(2.0, t.freq);
        t.interpolate = t.freq % 1.0f != 0.0f;
        t.sIdx = 0;
        t.sustain = t.s.sustainStart != -1;
        t.loop = t.s.loopStart != -1 && t.s.sustainStart == -1;
        t.playing = true;
        return a;
      }
    }
    return -1;
  }

  /** Plays sample thru a sound channel, returns idx of sound channel used.
   * @param idx = instrument index
   * @param L,R = left/right volume (0.0 - 1.0)
   * @return idx to use with other Sound channel functions, -1 if can't play now
   */
  public synchronized int samplePlay(int idx, float L, float R) {
    for(int a=0;a<sounds.length;a++) {
      if (!sounds[a].playing) {
        Track t = sounds[a];
        t.volL = L;
        t.volR = R;
        t.vol = 1.0f;
        t.i = null;
        t.r = null;
        t.s = song.samples.get(idx);
        t.freq = 0.0f;
        t.freqPow2 = (float)Math.pow(2.0, t.freq);
        t.interpolate = t.freq % 1.0f != 0.0f;
        t.sIdx = 0;
        t.sustain = t.s.sustainStart != -1;
        t.loop = t.s.loopStart != -1 && t.s.sustainStart == -1;
        t.playing = true;
        return a;
      }
    }
    return -1;
  }

  //Sound API

  /** Loads an audio file and returns its idx */
  public synchronized int soundLoad(short[] samples, int loopStart, int loopEnd
    , int sustainStart, int sustainEnd, float attenuation, int unityNote) {
    Instrument i = new Instrument();
    i.name = "";
    Region r = new Region();
    i.regions.add(r);
    r.low = 0;
    r.high = 0x7f;
    r.unity = unityNote;
    r.sample = loadedSoundsSamples.size();
    Sample s = new Sample();
    s.samples = samples;
    s.loopStart = loopStart;
    s.loopEnd = loopEnd;
    s.sustainStart = sustainStart;
    s.sustainEnd = sustainEnd;
    s.attenuation = attenuation;
    loadedSounds.add(i);
    loadedSoundsSamples.add(s);
    return loadedSounds.size() - 1;
  }
  public synchronized int soundLoad(String fn, int loopStart, int loopEnd
    , int sustainStart, int sustainEnd, float attenuation) {
    for(int a=0;a<loadedSounds.size();a++) {
      if (loadedSounds.get(a).name.equals(fn)) return a;
    }
    Wav wav = new Wav();
    wav.load(fn);
    wav.readAllSamples();
    return soundLoad(wav.samples16, loopStart, loopEnd, sustainStart, sustainEnd, attenuation, 0x3c);
  }
  public void soundRemove(int idx) {
    loadedSounds.remove(idx);
    loadedSoundsSamples.remove(idx);
  }
  public void soundClear() {
    loadedSounds.clear();
    loadedSoundsSamples.clear();
  }
  public void soundAttenuation(int idx, float value) {
    loadedSoundsSamples.get(idx).attenuation = value;
  }
  /** Plays a sound, returns channel idx to modify during playback.
   * @param idx = sound index
   * @param volL = left volume (0.0 - 1.0)
   * @param volR = right volume (0.0 - 1.0)
   * @param note = midi note to play (0-11)
   * @return idx to use with other Sound functions, -1 if can't play now
   */
  public synchronized int soundPlay(int idx, float volL, float volR, int note) {
    for(int a=0;a<sounds.length;a++) {
      if (!sounds[a].playing) {
        Track t = sounds[a];
        t.volL = volL;
        t.volR = volR;
        t.vol = 1.0f;
        t.i = loadedSounds.get(idx);
        t.r = t.i.getRegion(note);
        t.s = loadedSoundsSamples.get(t.r.sample);
        t.freq = (note - t.r.unity) / 12.0f;
        t.freqPow2 = (float)Math.pow(2.0, t.freq);
        t.interpolate = t.freq % 1.0f != 0.0f;
        t.sIdx = 0;
        t.sustain = t.s.sustainStart != -1;
        t.loop = t.s.loopStart != -1 && t.s.sustainStart == -1;
        t.attenuation = t.s.attenuation;
        t.playing = true;
        return a;
      }
    }
    return -1;
  }

  //API to modify a sound channel that is currently playing a sound

  public void channelStop(int idx) {
    sounds[idx].playing = false;
  }
  public void channelKeyUp(int idx) {
    sounds[idx].sustain = false;
  }
  public void channelAttenuation(int idx, float value) {
    sounds[idx].attenuation = value;
  }
  public void channelPan(int idx, float L, float R) {
    sounds[idx].volL = L;
    sounds[idx].volR = R;
  }
  public void channelFreq(int idx, float freq) {
    sounds[idx].freq = freq;
    sounds[idx].freqPow2 = (float)Math.pow(2.0, freq);
    sounds[idx].interpolate = freq % 1.0f != 0.0f;
  }
  public void channelApplyFX(int idx, byte fxcmd, int fxparam) {
    sounds[idx].setCmds((byte)0, (byte)0, fxcmd, fxparam, this);
  }

  //API to edit music
  public void addTrack(int pattern) {
    song.patterns.get(pattern).tracks.add(new Track());
  }

  public void removeTrack(int pattern, int track) {
    song.patterns.get(pattern).tracks.remove(track);
  }

  public void addPattern(int nTracks) {
    Pattern newPattern = new Pattern();
    newPattern.addTracks(nTracks);
    song.patterns.add(newPattern);
  }

  public void removePattern(int pattern) {
    song.patterns.remove(pattern);
    //remove all references to pattern in sequence
    for(int a=0;a<song.sequence.size();) {
      Integer seq = song.sequence.get(a);
      if (seq == pattern) {
        song.sequence.remove(a);
      } else {
        a++;
      }
    }
  }

  public boolean addSamples(String name, short[] samples, int loopStart, int loopEnd
    , int sustainStart, int sustainEnd, float attenuation)
  {
    Sample s = new Sample();
    s.name = name;
    s.loopStart = loopStart;
    s.loopEnd = loopEnd;
    s.sustainStart = sustainStart;
    s.sustainEnd = sustainEnd;
    s.attenuation = attenuation;
    s.samples = samples;
    song.samples.add(s);
    return true;
  }

  public boolean addSamples(String name, String fn, int loopStart, int loopEnd
    , int sustainStart, int sustainEnd, float attenuation)
  {
    Wav wav = new Wav();
    if (!wav.load(fn)) return false;
    wav.readAllSamples();
    return addSamples(name, wav.samples16, loopStart, loopEnd, sustainStart, sustainEnd, attenuation);
  }

  public void removeSamples(int idx) {
    song.samples.remove(idx);
    //patch instrument region indexes
    for(int a=0;a<song.instruments.size();a++) {
      Instrument i = song.instruments.get(a);
      for(int b=0;b<i.regions.size();b++) {
        Region r = i.regions.get(b);
        if (r.sample > idx) {
          r.sample--;
        } else if (r.sample == idx) {
          r.sample = 0;
        }
      }
    }
  }

  public int addInstrument(String name) {
    Instrument i = new Instrument();
    i.name = name;
    song.instruments.add(i);
    return song.instruments.size() - 1;
  }

  public void removeInstrument(int idx) {
    song.instruments.remove(idx);
    //now cleanup ALL patterns (delete notes with this instrument and bump others above it)
    for(int p=0;p<song.patterns.size();p++) {
      Pattern pat = song.patterns.get(p);
      for(int t=0;t<pat.tracks.size();t++) {
        Track trk = pat.tracks.get(t);
        if (trk.startInstrument == idx)
          trk.startInstrument = 0;
        else if (trk.startInstrument > idx)
          trk.startInstrument--;
        for(int n=0;n<64;n++) {
          if (trk.fxcmds[n] == FXCMD_SET_INSTRUMENT) {
            if (trk.fxparams[n] == idx) {
              trk.fxcmds[n] = 0;
              trk.fxparams[n] = 0;
            } else if (trk.fxparams[n] > idx) {
              trk.fxparams[n]--;
            }
          }
        }
      }
    }
  }

  public void addRegion(int iidx, int low, int high, int unityNote, int sample) {
    Region r = new Region();
    r.low = low;
    r.high = high;
    r.unity = unityNote;
    r.sample = sample;
    song.instruments.get(iidx).regions.add(r);
  }

  public void removeRegion(int iidx, int ridx) {
    song.instruments.get(iidx).regions.remove(ridx);
  }

  // volume control

  /** Range: 0.0f -> 1.0f */
  public void setMasterMusicVolume(float l, float r) {
    m_volL = l;
    m_volR = r;
  }

  /** Range: 0.0f -> 1.0f */
  public void setMasterSoundVolume(float l, float r) {
    s_volL = l;
    s_volR = r;
  }

  //private code

  private boolean prepNextPattern() {
    if (seqIdx >= song.sequence.size()) {
      playing = false;
      if (listener != null) listener.musicEnded();
      return false;
    }
    patternIdx = song.sequence.get(seqIdx);
    pattern = song.patterns.get(patternIdx);
    pattern.reset(this);
    return true;
  }

  //output next audio chunk
  private void process() {
    try {
      Arrays.fill(samples, (short)0);
      synchronized(lock) {
        if (playing) processMusic();
        processSound();
        if (listener != null) listener.musicSamples(samples);
      }
      if (output != null) output.write(samples);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void processMusic() {
    int idx = 0;
    for(int a=0;a<samplesPerBuffer;a++) {
      float L = 0, R = 0;
      samplesThisBeat++;
      if (samplesThisBeat == samplesPerBeat) {
        samplesThisBeat = 0;
        if (play == Play.note) {
          playing = false;
          if (listener != null) listener.musicEnded();
          return;
        }
        rowIdx++;
        if (rowIdx == 64) {
          rowIdx = 0;
          seqIdx++;
          if (play == Play.pattern) {
            playing = false;
            if (listener != null) listener.musicEnded();
            return;
          }
          if (seqIdx == song.sequence.size()) {
            if (listener != null) listener.musicEnded();
            if (!repeatSong) {
              playing = false;
              if (listener != null) listener.musicEnded();
              return;
            }
            seqIdx = 0;
          }
          if (!prepNextPattern()) return;
        } else {
          int nTracks = pattern.tracks.size();
          for(int t=0;t<nTracks;t++) {
            pattern.tracks.get(t).nextNote(this);
          }
        }
        if (listener != null) listener.musicRow(seqIdx, patternIdx, rowIdx);
      }
      int nTracks = pattern.tracks.size();
      for(int b=0;b<nTracks;b++) {
        Track t = pattern.tracks.get(b);
        if (!t.playing) continue;
        if (t.mute) continue;
        if (t.delay > 0) {
          t.delay--;
          continue;
        }
        int cIdx = (int)t.sIdx;
        float sample;
        if (t.interpolate && ((cIdx + 1) < t.s.samples.length)) {
          float yA = t.s.samples[cIdx];
          float yB = t.s.samples[cIdx + 1];
          float mB = t.sIdx % 1.0f;
          float mA = 1.0f - mB;
          sample = yA * mA + yB * mB;
        } else {
          sample = t.s.samples[cIdx];
        }
        t.sIdx += t.freqPow2;
        cIdx = (int)t.sIdx;
        t.vol -= t.attenuation;
        L += sample * t.volL * t.vol;
        R += sample * t.volR * t.vol;
        if (t.vol <= 0.0f) {
          t.playing = false;
        }
        else if (t.sustain && (cIdx >= t.s.sustainEnd)) {
          t.sIdx -= (t.s.sustainEnd - t.s.sustainStart);
        }
        else if (t.loop && (cIdx >= t.s.loopEnd)) {
          t.sIdx -= (t.s.loopEnd - t.s.loopStart);
        }
        else if (cIdx >= t.s.samples.length) {
          t.playing = false;
        }
        if (t.playing) {
          t.doCmds();
        }
      }
      L *= m_volL;
      R *= m_volR;
      int iL = (int)L, iR = (int)R;
      //clamp to short
      if (iL < -32768) iL = -32768;
      if (iL > 32767) iL = 32767;
      if (iR < -32768) iR = -32768;
      if (iR > 32767) iR = 32767;
      samples[idx++] = (short)iL;
      samples[idx++] = (short)iR;
    }
  }

  private void processSound() {
    int idx = 0;
    for(int b=0;b<samplesPerBuffer;b++) {
      float samL = 0.0f;
      float samR = 0.0f;
      for(int a=0;a<sounds.length;a++) {
        Track t = sounds[a];
        if (!t.playing) continue;
        if (t.mute) continue;
        if (t.delay > 0) {
          t.delay--;
          continue;
        }
        int cIdx = (int)t.sIdx;
        float sample;
        if (t.interpolate && ((cIdx + 1) < t.s.samples.length)) {
          float yA = t.s.samples[cIdx];
          float yB = t.s.samples[cIdx + 1];
          float mB = t.sIdx % 1.0f;
          float mA = 1.0f - mB;
          sample = yA * mA + yB * mB;
        } else {
          sample = t.s.samples[cIdx];
        }
        t.sIdx += t.freqPow2;
        cIdx = (int)t.sIdx;
        t.vol -= t.attenuation;
        samL += sample * t.volL * t.vol;
        samR += sample * t.volR * t.vol;
        if (t.vol <= 0.0f) {
          t.playing = false;
        }
        else if (t.sustain && (cIdx >= t.s.sustainEnd)) {
          t.sIdx -= (t.s.sustainEnd - t.s.sustainStart);
        }
        else if (t.loop && (cIdx >= t.s.loopEnd)) {
          t.sIdx -= (t.s.loopEnd - t.s.loopStart);
        }
        else if (cIdx >= t.s.samples.length) {
          t.playing = false;
        }
        if (t.playing) {
          t.doCmds();
        }
      }
      samL *= s_volL;
      samR *= s_volR;
      int isamL = (int)samL, isamR = (int)samR;
      isamL += samples[idx];
      if (chs > 1) {
        isamR += samples[idx+1];
      }
      //clamp to short
      if (isamL < -32768) isamL = -32768;
      if (isamL > 32767) isamL = 32767;
      if (isamR < -32768) isamR = -32768;
      if (isamR > 32767) isamR = 32767;
      samples[idx++] = (short)isamL;
      if (chs > 1) {
        samples[idx++] = (short)isamR;
      }
    }
  }
}

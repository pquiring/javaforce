package jfpbx.core;

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.voip.*;

/** Relays RTP packets between calling parties or from one party to local service (VM, IVR). */

public class RTPRelay implements RTPInterface {
  private volatile boolean active_src, active_dst;
  private RTP rtp_src, rtp_dst;

  private CallDetailsPBX cd;
  private short samples[];
  private byte samples8[];
  private RandomAccessFile recording;
  private int recordinglen;
  private FileInputStream playing;
  private PBXEventHandler eh;
  private String lang = "en";
  private int maxrecordinglen;
  private PBXAPI api;
  private Vector<String> playList = new Vector<String>();
  private boolean recv_src = true, recv_dst = true;  //can recv from
  private boolean send_src = true, send_dst = true;  //can send to
  private MusicOnHold moh_src = new MusicOnHold();
  private MusicOnHold moh_dst = new MusicOnHold();
  private boolean moh = false;  //force moh mode
  private String content;  //name of stream

  public final static short silence[] = new short[160];

  /** Init RTPRelay.  Ports may be -1 for NATing. */

  public boolean init() {
    rtp_src = new RTP();
    rtp_src.init(this);
    rtp_dst = new RTP();
    rtp_dst.init(this);
    return true;
  }

  public boolean init(CallDetailsPBX cd, PBXEventHandler eh, PBXAPI api) {
    this.cd = cd;
    this.eh = eh;
    this.api = api;
    samples = new short[160];
    samples8 = new byte[160*2];
    return true;
  }

  public int getPort_src() {return rtp_src.getlocalrtpport();}

  public int getPort_dst() {return rtp_dst.getlocalrtpport();}

  public void setLang(String lang) {
    this.lang = lang;
  }

  public String getContent() {return content;}
  public void setContent(String in) {content = in;}

  /* Raw mode controls if RTPInterface.rtpPacket (if true) or rtpSamples (if false) is called. */
  public void setRawMode(boolean state) {
    rtp_src.setRawMode(state);
    rtp_dst.setRawMode(state);
  }

  public boolean start_src(SDP.Stream stream) {
    if (active_src) return true;
    active_src = true;
    rtp_src.start();
    if (rtp_src.createChannel(stream) == null) return false;
/*
    if (stream.isSecure()) {
      SRTPChannel channel = (SRTPChannel)rtp_src.getDefaultChannel();
      if (stream.keyExchange == SDP.KeyExchange.DTLS) {
        channel.setDTLS(true, RTP.genIceufrag(), RTP.genIcepwd());
      } else if (stream.keyExchange == SDP.KeyExchange.SDP) {
        //TODO!!!
        JFLog.log("RTPRelay:TODO:SDP Key Exchange");
      } else {
        JFLog.log("RTPRelay:Error:RTP Channel is secure but key exchange is undefined.");
      }
    }
*/
    rtp_src.getDefaultChannel().start();
    return true;
  }

  public boolean start_dst(SDP.Stream stream) {
    if (active_dst) return true;
    active_dst = true;
    rtp_dst.start();
    if (rtp_dst.createChannel(stream) == null) return false;
/*
    if (stream.isSecure()) {
      SRTPChannel channel = (SRTPChannel)rtp_dst.getDefaultChannel();
      if (stream.keyExchange == SDP.KeyExchange.DTLS) {
        channel.setDTLS(false, RTP.genIceufrag(), RTP.genIcepwd());
      } else if (stream.keyExchange == SDP.KeyExchange.SDP) {
        //TODO!!!
        JFLog.log("RTPRelay:TODO:SDP Key Exchange");
      } else {
        JFLog.log("RTPRelay:Error:RTP Channel is secure but key exchange is undefined.");
      }
    }
*/
    rtp_dst.getDefaultChannel().start();
    return true;
  }

  public boolean start(SDP.Stream src, SDP.Stream dst) {
    //NOTE:No codecs are needed since packets are only relayed as is
//    JFLog.log("RTPRelay:start:src.ice=" + src.sdp.hashCode() + ":dst.ice=" + dst.sdp.hashCode());
    if (active_src)
      change_src(src);
    else
      start_src(src);
    if (active_dst)
      change_dst(dst);
    else
      start_dst(dst);
    return true;
  }

  /** Swap src and dst. */
  public void swap() {
    RTP rtp;
    rtp = rtp_src;
    rtp_src = rtp_dst;
    rtp_dst = rtp;
    boolean sendrecv;
    sendrecv = recv_dst;
    recv_dst = recv_src;
    recv_src = sendrecv;
    sendrecv = send_dst;
    send_dst = send_src;
    send_src = sendrecv;
    MusicOnHold moh;
    moh = moh_src;
    moh_src = moh_dst;
    moh_dst = moh;
  }

  /** Swaps the dst RTP in two relays. */
  public static void swap(RTPRelay r1, RTPRelay r2) {
    RTP rtp;
    rtp = r1.rtp_dst;
    r1.rtp_dst = r2.rtp_dst;
    r2.rtp_dst = rtp;
    //reset interfaces
    r1.rtp_dst.setInterface(r1);
    r2.rtp_dst.setInterface(r2);
  }

  public void uninit() {
    uninit_src();
    uninit_dst();
  }

  public void uninit_src() {
    if (active_src) {
      active_src = false;
      rtp_src.uninit();
    }
    cleanup();
  }

  public void uninit_dst() {
    if (active_dst) {
      active_dst = false;
      rtp_dst.uninit();
    }
    cleanup();
  }

  public void cleanup() {
    if (recording != null) {
      closeRecording(true);
    }
    if (playing != null) {
      closePlaying(true);
    }
  }

  public void change_src(SDP.Stream stream) {
    rtp_src.getDefaultChannel().change(stream);
    recv_src = stream.canRecv();
    send_src = stream.canSend();
  }

  public void change_dst(SDP.Stream stream) {
    rtp_dst.getDefaultChannel().change(stream);
    recv_dst = stream.canRecv();
    send_dst = stream.canSend();
  }

  public boolean playSound(String fn) {
    fn = Paths.sounds + lang + "/" + fn + ".wav";
    return playSoundFull(fn);
  }

  public boolean playSoundFull(String fn) {
    if (playing != null) return addSoundFull(fn);
    api.log(cd, "playing:" + fn);
    FileInputStream wav = null;
    try {
      byte data[] = new byte[30];
      wav = new FileInputStream(fn);
      //read RIFF header (20 bytes);
      wav.read(data, 0, 20);
      if (!LE.getString(data, 0, 4).equals("RIFF")) throw new Exception(fn + " is not a valid WAV file (RIFF)");
      if (!LE.getString(data, 8, 4).equals("WAVE")) throw new Exception(fn + " is not a valid WAV file (WAVE)");
      if (!LE.getString(data, 12, 4).equals("fmt ")) throw new Exception(fn + " is not a valid WAV file (fmt )");
      int fmtsiz = LE.getuint32(data, 16);
      if ((fmtsiz < 16) || (fmtsiz > 30)) throw new Exception(fn + " is not a valid WAV file (fmtsiz)");
      wav.read(data, 0, fmtsiz);
      if (LE.getuint16(data, 0) != 1) throw new Exception(fn + " is not PCM");
      if (LE.getuint16(data, 2) != 1) throw new Exception(fn + " is not mono");
      if (LE.getuint32(data, 4) != 8000) throw new Exception(fn + " is not 8000Hz");
      if (LE.getuint16(data, 12) != 2) throw new Exception(fn + " is not 16bits");
      wav.read(data, 0, 8);
      if (!LE.getString(data, 0, 4).equals("data")) {
        //ignore block
        int len = LE.getuint32(data, 4);
        byte junk[] = new byte[len];
        wav.read(junk);
        wav.read(data, 0, 8);
      }
      if (!LE.getString(data, 0, 4).equals("data")) throw new Exception(fn + " is not a valid WAV file (data)");
      playing = wav;  //let it rip
    } catch (Exception e) {
      api.log(cd, e);
      try { if (wav != null) wav.close(); } catch (Exception e2) {}
      return false;
    }
    return true;
  }

  public boolean addSound(String fn) {
    playList.add(Paths.sounds + lang + "/" + fn + ".wav");
    return true;
  }

  public boolean addSoundFull(String fn) {
    playList.add(fn);
    return true;
  }

  public boolean addNumber(int num) {
    String numstr = Integer.toString(num);
    int sl = numstr.length();
    for(int i=0;i<sl;i++) {
      addSound("vm-" + numstr.charAt(i));
    }
    return true;
  }

  /** Records to a full filename (extension .wav added). */

  public boolean recordSoundFull(String fn, int maxSeconds) {
    byte data[] = new byte[44];
    maxrecordinglen = maxSeconds * 8000 * 2;  //*2 for 16bit
    fn += ".wav";
    try {
      File file = new File(fn);
      if (file.exists()) file.delete();  //delete if exists
      RandomAccessFile wav = new RandomAccessFile(fn, "rw");
      LE.setString(data, 0, 4, "RIFF");
//      setuint32(data, 4, filelength-8);  //pending time travel code branch prediction in next release, or just patch when closing file
      LE.setString(data, 8, 4, "WAVE");
      LE.setString(data, 12, 4, "fmt ");
      LE.setuint32(data, 16, 16);  //fmt header size
      LE.setuint16(data, 20, 1);  //PCM
      LE.setuint16(data, 22, 1);  //mono
      LE.setuint32(data, 24, 8000);  //hz
      LE.setuint32(data, 28, 0x3e80);  //bytes/sec
      LE.setuint16(data, 32, 2);  //bytes/sam
      LE.setuint16(data, 34, 0x10);  //???
      LE.setString(data, 36, 4, "data");
//      setuint32(data, 40, filelength-44);  //same deal
      wav.write(data);
      recordinglen = 0;
      recording = wav;  //let it rip
    } catch (Exception e) { return false; }
    return true;
  }

  /** Returns length of last recording in seconds. */

  public int getRecordingLength() {
    return recordinglen / (8000 * 2);
  }

  /* Convert samples to samples8. BE -> LE.*/
  private void short2byte() {
    for (int a = 0; a < 160; a++) {
      samples8[a * 2 + 1] = (byte) (samples[a] >>> 8);
      samples8[a * 2] = (byte) (samples[a] & 0xff);
    }
  }

  /* Convert samples8 to samples. LE -> BE. */
  private void byte2short() {
    for (int a = 0; a < 160; a++) {
      samples[a] = (short) ((((short) (samples8[a * 2 + 1])) << 8) + (((short) (samples8[a * 2])) & 0xff));
    }
  }

  public void rtpSamples(RTPChannel channel) {
    if (recording != null) {
      if (channel.getSamples(samples)) {
        short2byte();
        try {
          recording.write(samples8);
          recordinglen += 160*2;
        } catch (Exception e) {
          recordinglen = maxrecordinglen;
        }
        if (recordinglen >= maxrecordinglen) {
          closeRecording(false);
        }
      }
      System.arraycopy(silence, 0, samples, 0, 160);
    } else if (playing != null) {
      try {
        if (playing.read(samples8, 0, 160*2) != 160*2) {
          System.arraycopy(silence, 0, samples, 0, 160);
          closePlaying(false);
        } else {
          byte2short();
        }
      } catch (Exception e) {
        closePlaying(false);
        System.arraycopy(silence, 0, samples, 0, 160);
      }
    } else {
      channel.getSamples(samples);
      if (moh) {
        moh_src.getSamples(samples);
      }
      if (eh != null) {
        eh.samples(cd, samples);
      } else {
        System.arraycopy(silence, 0, samples, 0, 160);
      }
    }
    try {
      byte packet[] = channel.coder.encode(samples);
      channel.writeRTP(packet, 0, packet.length);
//      api.log(cd, "VM : Sent samples, length=" + packet.length);
    } catch (Exception e) {
      api.log(cd, e);
    }
  }

  public void rtpDigit(RTPChannel channel, char digit) {
    if (recording != null) {
      closeRecording(true);
    } else if (playing != null) {
      closePlaying(true);
    }
    eh.event(cd, eh.DIGIT, digit, true);
  }

  public void rtpPacket(RTPChannel channel, int codec, byte data[], int off, int len) {
    switch (codec) {
      case CodecType.RTP: rtpPacket(channel, data, off, len); break;
      case CodecType.RTCP: rtcpPacket(channel, data, off, len); break;
    }
  }

  public void rtcpPacket(RTPChannel channel, byte data[], int off, int len) {
    if (channel.rtp == rtp_src) {
      rtp_dst.getDefaultChannel().writeRTCP(data, off, len);
    } else if (channel.rtp == rtp_dst) {
      rtp_src.getDefaultChannel().writeRTCP(data, off, len);
    }
  }

  public void rtpPacket(RTPChannel channel, byte data[], int off, int len) {
//    JFLog.log("rtpPacket:" + channel + ",len=" + len);
    try {
      if (channel.rtp == rtp_src) {
        if (!recv_dst) {
          moh_src.getSamples(samples);
          byte packet[] = rtp_src.getDefaultChannel().coder.encode(samples);
          rtp_src.getDefaultChannel().writeRTP(packet, 0, packet.length);
        }
        if (send_dst) {
//          JFLog.log("sending to:" + rtp_dst.getDefaultChannel());
          rtp_dst.getDefaultChannel().writeRTP(data, off, len);
        }
      } else if (channel.rtp == rtp_dst) {
        if (!recv_src) {
          moh_dst.getSamples(samples);
          byte packet[] = rtp_dst.getDefaultChannel().coder.encode(samples);
          rtp_dst.getDefaultChannel().writeRTP(packet, 0, packet.length);
        }
        if (send_src) {
//          JFLog.log("sending to:" + rtp_src.getDefaultChannel());
          rtp_src.getDefaultChannel().writeRTP(data, off, len);
        }
      } else {
        JFLog.log("Error: Unknown RTP Packet:" + channel.rtp);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void rtpInactive(RTPChannel rtp) {}

  private void closeRecording(boolean interrupted) {
    try {
      byte data[] = new byte[4];
      recording.seek(4);
      LE.setuint32(data, 0, recordinglen+44-8);
      recording.write(data);
      recording.seek(40);
      LE.setuint32(data, 0, recordinglen);
      recording.write(data);
      recording.close();
    } catch (Exception e) {}
    recording = null;
    eh.event(cd, eh.SOUND, ' ', interrupted);
  }

  private void closePlaying(boolean interrupted) {
    try {playing.close(); } catch (Exception e) {}
    playing = null;
    if ((!interrupted) && (playList.size() > 0)) {
      playSoundFull(playList.get(0));
      playList.remove(0);
    } else {
      playList.clear();
      eh.event(cd, eh.SOUND, ' ', interrupted);
    }
  }

  /** Forces playing MOH to both parties. */
  public void setMOH(boolean state) {
    moh = state;
  }

  public String toString() {
    return "RTPRelay:{src=" + rtp_src + ",dst=" + rtp_dst + "}";
  }
}

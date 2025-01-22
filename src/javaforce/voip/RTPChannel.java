package javaforce.voip;

import java.util.*;
import java.net.*;

import javaforce.*;
import javaforce.media.*;
import javaforce.voip.codec.*;

public class RTPChannel {

  private int seqnum = 0;
  private int timestamp = 0;
  protected int ssrc_src = -1, ssrc_dst = -1;
  private static Random r = new Random();
  private static Object rlock = new Object();
  private short dtmfduration = 0;
  protected short turn1ch, turn2ch;
  //dynnamic codec payload ids
  private int rfc2833_id = -1;
  private int speex_id = -1;
  private int opus_id = -1;
  private int vp8_id = -1;
  private int vp9_id = -1;
  private int h263_1998_id = -1;
  private int h263_2000_id = -1;
  private int h264_id = -1;
  private int h265_id = -1;
  private char dtmfChar;
  private boolean dtmfSent = false;
  private AudioBuffer buffer = new AudioBuffer(32000, 1, 2);  //freq, chs, seconds
  private DTMF dtmf;
  private static short[] silence8 = new short[160];
  private static short[] silence16 = new short[320];
  protected long turnBindExpires;
  private long lastPacket = 0;
  protected boolean active = false;
  private MusicOnHold moh = new MusicOnHold();
  private static int speex_quality = 5;
  private static boolean speex_enhanced_decoder = false;

  public RTP rtp;
  public SDP.Stream stream;
  public RTPAudioCoder coder;  //selected audio encoder
  public int log;

  public static boolean debug = false;

  protected RTPChannel(RTP rtp, int ssrc, SDP.Stream stream) {
    this.rtp = rtp;
    this.ssrc_src = ssrc;
    this.stream = stream;
  }

  public void setLog(int id) {
    log = id;
  }

  public int getVP8id() {
    return vp8_id;
  }

  public int getVP9id() {
    return vp9_id;
  }

  public int getH264id() {
    return h264_id;
  }

  public int getH265id() {
    return h265_id;
  }

  public int getH263_1998id() {
    return h263_1998_id;
  }

  public int getH263_2000id() {
    return h263_2000_id;
  }

  public int getRFC2833id() {
    return rfc2833_id;
  }

  /**
   * Writes a packet to the RTP port.
   */
  public void writeRTP(byte[] data, int off, int len) {
    if (!rtp.active) {
      JFLog.log(log, "RTPChannel.writeRTP() : not active");
      return;
    }
    if (stream.getPort() == -1) {
      JFLog.log(log, "RTPChannel.writeRTP() : not ready (NATing)");
      return;  //not ready yet (NATing)
    }
    if (!stream.canSend()) {
      JFLog.log(log, "RTPChannel.writeRTP() : stream without send");
      return;
    }
    if (debug) {
      JFLog.log("RTPChannel.writeRTP:" + stream.getIP() + ":" + stream.getPort());
    }
    try {
      if (RTP.useTURN) {
        rtp.stun1.sendData(turn1ch, data, off, len);
      } else {
        rtp.sock1.send(data, off, len, InetAddress.getByName(stream.getIP()), stream.getPort());
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
  }

  /**
   * Writes a packet to the RTCP port.
   */
  public void writeRTCP(byte[] data, int off, int len) {
    if (!rtp.active) {
      return;
    }
    if (stream.getPort() == -1) {
      return;  //not ready yet (NATing)
    }
    try {
      if (RTP.useTURN) {
        rtp.stun2.sendData(turn2ch, data, off, len);
      } else {
        rtp.sock2.send(data, off, len, InetAddress.getByName(stream.getIP()), stream.getPort() + 1);
      }
    } catch (Exception e) {
      JFLog.log(log, "err:RTP.writeRTCP:failed");
      JFLog.log(log, e);
    }
  }

  /**
   * Writes a RFC 2833 (DTMF) RTP packet.
   */
  public void writeDTMF(char digit, boolean end) {
    byte[] data = new byte[16];
    buildHeader(data, RTP.CODEC_RFC2833.id, getseqnum(), gettimestamp(160), getssrc(), false);
    switch (digit) {
      case '*':
        data[12] = 10;
        break;
      case '#':
        data[12] = 11;
        break;
      default:
        data[12] = (byte) (digit - '0');
        break;
    }
    if (end) {
      data[13] = (byte) 0x8a;  //volume=10 + end of packet
    } else {
      data[13] = 0x0a;  //volume=10
    }
    data[14] = (byte) ((dtmfduration & 0xff00) >> 8);
    data[15] = (byte) (dtmfduration & 0xff);
    dtmfduration += 160;
    writeRTP(data, 0, data.length);
    if (end) {
      //send 'end of DTMF' 3 times to ensure it's received
      writeRTP(data, 0, data.length);
      writeRTP(data, 0, data.length);
      dtmfduration = 0;
    }
  }

  /**
   * Builds RTP header in first 12 bytes of data[].
   */
  public static void buildHeader(byte[] data, int id, int seqnum, int timestamp, int ssrc, boolean last) {
    //build RTP header
    data[0] = (byte) 0x80;  //version
    data[1] = (byte) id;    //0=g711u 3=gsm 8=g711a 18=g729a 26=JPEG 34=H.263 etc.
    if (last) {
      data[1] |= 0x80;
    }
    BE.setuint16(data, 2, seqnum);
    BE.setuint32(data, 4, timestamp);
    BE.setuint32(data, 8, ssrc);
  }

  public void buildHeader(byte[] data, int type) {
    buildHeader(data, type, 0, 0, getssrc(), false);
  }

  public int getseqnum() {
    return seqnum++;
  }

  public int gettimestamp(int delta) {
    int ret = timestamp;
    timestamp += delta;
    return ret;
  }

  public int getssrc() {
    if (ssrc_src != -1) {
      return ssrc_src;
    }
    synchronized (rlock) {
      ssrc_src = r.nextInt() & 0x7fffffff;
    }
    return ssrc_src;
  }

  public static int getseqnum(byte[] data, int off) {
    return BE.getuint16(data, 2 + off);
  }

  public static int gettimestamp(byte[] data, int off) {
    return BE.getuint32(data, 4 + off);
  }

  public static int getssrc(byte[] data, int off) {
    return BE.getuint32(data, 8 + off);
  }

  public String getremoteip() {
    if (RTP.useTURN) {
      return rtp.stun1.getIP();
    } else {
      return stream.getIP();
    }
  }

  public int getremoteport() {
    return stream.getPort();
  }

  public boolean start() {
    lastPacket = System.currentTimeMillis();
    JFLog.log(log, "RTPChannel.start() : localhost:" + rtp.getlocalrtpport() + " remote=" + stream.getIP() + ":" + stream.getPort());

    if ((stream.codecs != null) && (stream.codecs.length > 0)) {
      Codec codec_rfc2833 = stream.getCodec(RTP.CODEC_RFC2833);
      if (codec_rfc2833 != null) {
        rfc2833_id = codec_rfc2833.id;
      }
      Codec codec_vp8 = stream.getCodec(RTP.CODEC_VP8);
      if (codec_vp8 != null) {
        vp8_id = codec_vp8.id;
      }

      Codec codec_vp9 = stream.getCodec(RTP.CODEC_VP9);
      if (codec_vp9 != null) {
        vp9_id = codec_vp9.id;
      }

      Codec codec_h264 = stream.getCodec(RTP.CODEC_H264);
      if (codec_h264 != null) {
        h264_id = codec_h264.id;
      }

      Codec codec_h265 = stream.getCodec(RTP.CODEC_H265);
      if (codec_h265 != null) {
        h265_id = codec_h265.id;
      }

      Codec codec_h263_1998 = stream.getCodec(RTP.CODEC_H263_1998);
      if (codec_h263_1998 != null) {
        h263_1998_id = codec_h263_1998.id;
      }

      Codec codec_h263_2000 = stream.getCodec(RTP.CODEC_H263_2000);
      if (codec_h263_2000 != null) {
        h263_2000_id = codec_h263_2000.id;
      }

      Codec codec_speex = stream.getCodec(RTP.CODEC_SPEEX);
      if (codec_speex != null) {
        speex_id = codec_speex.id;
      }

      Codec codec_speex16 = stream.getCodec(RTP.CODEC_SPEEX16);
      if (codec_speex16 != null) {
        speex_id = codec_speex16.id;
      }

      Codec codec_speex32 = stream.getCodec(RTP.CODEC_SPEEX32);
      if (codec_speex32 != null) {
        speex_id = codec_speex32.id;
      }

      Codec codec_opus = stream.getCodec(RTP.CODEC_OPUS);
      if (codec_opus != null) {
        opus_id = codec_opus.id;
      }

      if (stream.type == SDP.Type.audio) {
        //must use first available codec
        coder = null;
        for(int a=0;a<stream.codecs.length;a++) {
          Codec codec = stream.codecs[a];
          if (codec.equals(RTP.CODEC_G711u)) {
            coder = new g711u(rtp);
            JFLog.log(log, "codec = g711u");
            break;
          } else if (codec.equals(RTP.CODEC_G711a)) {
            coder = new g711a(rtp);
            JFLog.log(log, "codec = g711a");
            break;
          } else if (codec.equals(RTP.CODEC_GSM)) {
            coder = new gsm(rtp);
            JFLog.log(log, "codec = gsm");
            break;
          } else if (codec.equals(RTP.CODEC_G722)) {
            coder = new g722(rtp);
            JFLog.log(log, "codec = g722");
            break;
          } else if (codec.equals(RTP.CODEC_G729a)) {
            coder = new g729a(rtp);
            JFLog.log(log, "codec = g729a");
            break;
          } else if (codec.equals(RTP.CODEC_SPEEX)) {
            coder = new speex(rtp, codec.rate).setQuality(speex_quality).setEnhancedMode(speex_enhanced_decoder);
            coder.setid(codec.id);
            JFLog.log(log, "codec = speex");
            break;
          } else if (codec.equals(RTP.CODEC_SPEEX16)) {
            coder = new speex(rtp, codec.rate).setQuality(speex_quality).setEnhancedMode(speex_enhanced_decoder);
            coder.setid(codec.id);
            JFLog.log(log, "codec = speex16");
            break;
          } else if (codec.equals(RTP.CODEC_SPEEX32)) {
            coder = new speex(rtp, codec.rate).setQuality(speex_quality).setEnhancedMode(speex_enhanced_decoder);
            coder.setid(codec.id);
            JFLog.log(log, "codec = speex32");
            break;
          } else if (codec.equals(RTP.CODEC_OPUS)) {
            coder = new opus(rtp, codec.rate);
            coder.setid(codec.id);
            JFLog.log(log, "codec = opus");
            break;
          }
        }
        if (coder == null) {
          JFLog.log(log, "RTP.start() : Warning : no compatible audio codec selected");
        }
        dtmf = new DTMF(coder.getSampleRate());
      } else {
        boolean haveVcodec = false;
        for(int a=0;a<stream.codecs.length;a++) {
          Codec codec = stream.codecs[a];
          if (codec.equals(RTP.CODEC_H263)) {
            JFLog.log(log, "codec = H.263");
            haveVcodec = true;
            break;
          } else if (codec.equals(RTP.CODEC_H263_1998)) {
            JFLog.log(log, "codec = H.263-1998");
            haveVcodec = true;
            break;
          } else if (codec.equals(RTP.CODEC_H263_2000)) {
            JFLog.log(log, "codec = H.263-2000");
            haveVcodec = true;
            break;
          } else if (codec.equals(RTP.CODEC_JPEG)) {
            JFLog.log(log, "codec = JPEG");
            haveVcodec = true;
            break;
          } else if (codec.equals(RTP.CODEC_H264)) {
            JFLog.log(log, "codec = H.264");
            haveVcodec = true;
            break;
          } else if (codec.equals(RTP.CODEC_H265)) {
            JFLog.log(log, "codec = H.265");
            haveVcodec = true;
            break;
          } else if (codec.equals(RTP.CODEC_VP8)) {
            JFLog.log(log, "codec = VP8");
            haveVcodec = true;
            break;
          }
        }
        if (!haveVcodec) {
          JFLog.log(log, "RTP.start() : Warning : no compatible video codec selected");
        }
      }
    } else {
      JFLog.log(log, "RTP:Error:No codecs provided");
    }

    if (RTP.useTURN) {
      try {
        synchronized (rtp.bindLock) {
          rtp.bindingChannel = this;
          turn1ch = rtp.getNextTURNChannel();
          rtp.wait4reset();
          rtp.stun1.requestBind(turn1ch, stream.getIP(), stream.getPort());
          rtp.wait4reply();
          turn2ch = rtp.getNextTURNChannel();
          rtp.wait4reset();
          rtp.stun2.requestBind(turn2ch, stream.getIP(), stream.getPort() + 1);
          rtp.wait4reply();
        }
      } catch (Exception e) {
        JFLog.log(log, e);
        return false;
      }
    }
    active = true;
    return true;
  }

  public void stop() {
    if (coder != null) {
      coder.close();
      coder = null;
    }
  }

  /**
   * Changes the SDP.Stream for this RTP session. Could occur in a SIP reINVITE.
   */
  public boolean change(SDP.Stream new_stream) {
    lastPacket = System.currentTimeMillis();
    stream = new_stream;
    if (stream.type == SDP.Type.audio) {
      if (coder != null) {
        coder.close();
        coder = null;
      }
      if (new_stream.hasCodec(RTP.CODEC_G711u)) {
        coder = new g711u(rtp);
      } else if (new_stream.hasCodec(RTP.CODEC_G711a)) {
        coder = new g711a(rtp);
      } else if (new_stream.hasCodec(RTP.CODEC_GSM)) {
        coder = new gsm(rtp);
      } else if (new_stream.hasCodec(RTP.CODEC_G722)) {
        coder = new g722(rtp);
      } else if (new_stream.hasCodec(RTP.CODEC_G729a)) {
        coder = new g729a(rtp);
      } else if (new_stream.hasCodec(RTP.CODEC_SPEEX)) {
        coder = new speex(rtp, 8000).setQuality(speex_quality).setEnhancedMode(speex_enhanced_decoder);
      } else if (new_stream.hasCodec(RTP.CODEC_SPEEX16)) {
        coder = new speex(rtp, 16000).setQuality(speex_quality).setEnhancedMode(speex_enhanced_decoder);
      } else if (new_stream.hasCodec(RTP.CODEC_SPEEX32)) {
        coder = new speex(rtp, 32000).setQuality(speex_quality).setEnhancedMode(speex_enhanced_decoder);
      } else if (new_stream.hasCodec(RTP.CODEC_OPUS)) {
        coder = new opus(rtp, 48000);
      }
      dtmf = new DTMF(coder.getSampleRate());
    }
    if (RTP.useTURN) {
      synchronized (rtp.bindLock) {
        rtp.bindingChannel = this;
        rtp.stun1.requestBind(turn1ch, stream.getIP(), stream.getPort());
        rtp.stun2.requestBind(turn2ch, stream.getIP(), stream.getPort() + 1);
      }
    }
    return true;
  }

  protected void processRTP(byte[] data, int off, int len) {
    lastPacket = RTP.now;
    if (rtp.rawMode) {
      rtp.iface.rtpPacket(this, CodecType.RAW, data, off, len);
      return;
    }
    int id = data[off + 1] & 0x7f;  //payload id
    if (id < 96) {
      switch (id) {
        case 0:
        case 3:
        case 8:
        case 9:
        case 18:
          dtmfSent = false;  //just in case end of dtmf was not received
          int packetSize = coder.getPacketSize();
          if (packetSize != -1 && len != packetSize + 12) {  //12 = RTP header
            JFLog.log(log, "RTP:Bad RTP packet length:type=" + coder.getClass().getName());
            break;
          }
          addSamples(coder.decode(data, off, len));
          rtp.iface.rtpSamples(this);
          break;
        case 26:
          rtp.iface.rtpPacket(this, CodecType.JPEG, data, off, len);
          break;
        case 34:
          rtp.iface.rtpPacket(this, CodecType.H263, data, off, len);
          break;
      }
    } else {
      if (id == rfc2833_id) {
        dtmfChar = ' ';
        if ((data[off + 12] >= 0) && (data[off + 12] <= 9)) {
          dtmfChar = (char) ('0' + data[off + 12]);
        }
        if (data[off + 12] == 10) {
          dtmfChar = '*';
        }
        if (data[off + 12] == 11) {
          dtmfChar = '#';
        }
        if (data[off + 13] < 0) {   //0x80 == end of dtmf
          dtmfSent = false;
          dtmfChar = ' ';
        }
        if (dtmfChar == ' ') {
          switch (coder.getSampleRate()) {
            case 8000: addSamples(silence8); break;
            case 16000: addSamples(silence16); break;
          }
        } else {
          addSamples(dtmf.getSamples(dtmfChar));
          if (!dtmfSent) {
            rtp.iface.rtpDigit(this, dtmfChar);
            dtmfSent = true;
          }
        }
      } else if (id == vp8_id) {
        rtp.iface.rtpPacket(this, CodecType.VP8, data, off, len);
      } else if (id == vp9_id) {
        rtp.iface.rtpPacket(this, CodecType.VP9, data, off, len);
      } else if (id == h264_id) {
        rtp.iface.rtpPacket(this, CodecType.H264, data, off, len);
      } else if (id == h265_id) {
        rtp.iface.rtpPacket(this, CodecType.H265, data, off, len);
      } else if (id == h263_1998_id) {
        rtp.iface.rtpPacket(this, CodecType.H263_1998, data, off, len);
      } else if (id == h263_2000_id) {
        rtp.iface.rtpPacket(this, CodecType.H263_2000, data, off, len);
      } else if (id == speex_id) {
        int packetSize = coder.getPacketSize();
        if (packetSize != -1 && len != packetSize + 12) {  //12 = RTP header
            JFLog.log(log, "RTP:Bad RTP packet length:type=" + coder.getClass().getName());
        }
        addSamples(coder.decode(data, off, len));
        rtp.iface.rtpSamples(this);
      } else if (id == opus_id) {
        int packetSize = coder.getPacketSize();
        if (packetSize != -1 && len != packetSize + 12) {  //12 = RTP header
            JFLog.log(log, "RTP:Bad RTP packet length:type=" + coder.getClass().getName());
        }
        addSamples(coder.decode(data, off, len));
        rtp.iface.rtpSamples(this);
      } else {
        if (debug) JFLog.log("RTPChannel:unknown codec id:" + id + ":" + rtp);
      }
    }
  }

  protected void processRTCP(byte[] data, int off, int len) {
    if (rtp.rawMode) {
      rtp.iface.rtpPacket(this, CodecType.RTCP, data, off, len);
    }
    //TODO : RTCP ???
  }

  protected void keepalive(long now) {
    //do refreshes a little sooner (75 seconds) (in case nonce changes)
    if (RTP.useTURN && (now + 75 * 1000) > turnBindExpires) {
      //request another 10 mins (actually just 5???)
      synchronized (rtp.bindLock) {
        rtp.bindingChannel = this;
        rtp.stun1.requestBind(turn1ch, stream.getIP(), stream.getPort());
        rtp.stun2.requestBind(turn2ch, stream.getIP(), stream.getPort() + 1);
      }
    }
    if (active && stream.type == SDP.Type.audio && stream.canRecv() && (now - 45 * 1000) > lastPacket) {
      rtp.iface.rtpInactive(this);
    }
  }

  /**
   * Returns a packet of decoded samples.
   */
  public boolean getSamples(short[] data) {
    if (!stream.canRecv()) {
      return moh.getSamples(data);
    }
    return buffer.get(data, 0, data.length);
  }

  private void addSamples(short[] data) {
    if (data == null) return;
    buffer.add(data, 0, data.length);
  }

  /** Set speex encoder quality (0-10)
   * Default = 5.
   * Affects only new speex instances.
   */
  public static void setSpeexQuality(int value) {
    if (value < 0) value = 0;
    if (value > 10) value = 10;
    speex_quality = value;
  }

  /** Set speex decoder enhanced mode.
   * Default = false
   * Affects only new speex instances.
   */
  public static void setSpeexEnhancedMode(boolean mode) {
    speex_enhanced_decoder = mode;
  }

  public String toString() {
    return "RTPChannel:{src=" + ssrc_src + ",dst=" + ssrc_dst + "," + stream + "}";
  }
}

package javaforce.voip;

import java.util.*;
import java.net.*;

import javaforce.*;
import javaforce.media.*;

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
  private int vp8_id = -1;
  private int h264_id = -1;
  private int h263_1998_id = -1;
  private int h263_2000_id = -1;
  private char dtmfChar;
  private boolean dtmfSent = false;
  private AudioBuffer buffer = new AudioBuffer(8000, 1, 2);  //freq, chs, seconds
  private DTMF dtmf;
  private static short silence8[] = new short[160];
  private static short silence16[] = new short[320];
  protected long turnBindExpires;
  private long lastPacket = 0;
  protected boolean active = false;
  private MusicOnHold moh = new MusicOnHold();

  public RTP rtp;
  public SDP.Stream stream;
  public Coder coder_g711u, coder_g711a, coder_g722, coder_g729a;
  public Coder coder;  //selected audio encoder

  protected RTPChannel(RTP rtp, int ssrc, SDP.Stream stream) {
    this.rtp = rtp;
    this.ssrc_src = ssrc;
    this.stream = stream;
  }

  public int getVP8id() {
    return vp8_id;
  }

  public int getH264id() {
    return h264_id;
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
  public void writeRTP(byte data[], int off, int len) {
    if (!rtp.active) {
      JFLog.log("RTPChannel.writeRTP() : not active");
      return;
    }
    if (stream.getPort() == -1) {
      JFLog.log("RTPChannel.writeRTP() : not ready (NATing)");
      return;  //not ready yet (NATing)
    }
    if (!stream.canSend()) {
      JFLog.log("RTPChannel.writeRTP() : stream without send");
      return;
    }
    try {
      if (rtp.useTURN) {
        rtp.stun1.sendData(turn1ch, data, off, len);
      } else {
        rtp.sock1.send(new DatagramPacket(data, off, len, InetAddress.getByName(stream.getIP()), stream.getPort()));
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /**
   * Writes a packet to the RTCP port.
   */
  public void writeRTCP(byte data[], int off, int len) {
    if (!rtp.active) {
      return;
    }
    if (stream.getPort() == -1) {
      return;  //not ready yet (NATing)
    }
    try {
      if (rtp.useTURN) {
        rtp.stun2.sendData(turn2ch, data, off, len);
      } else {
        rtp.sock2.send(new DatagramPacket(data, off, len, InetAddress.getByName(stream.getIP()), stream.getPort() + 1));
      }
    } catch (Exception e) {
      JFLog.log("err:RTP.writeRTCP:failed");
      JFLog.log(e);
    }
  }

  /**
   * Writes a RFC 2833 (DTMF) RTP packet.
   */
  public void writeDTMF(char digit, boolean end) {
    byte data[] = new byte[16];
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
  public static void buildHeader(byte data[], int id, int seqnum, int timestamp, int ssrc, boolean last) {
    //build RTP header
    data[0] = (byte) 0x80;  //version
    data[1] = (byte) id;    //0=g711u 8=g711a 18=g729a 26=JPEG 34=H.263 etc.
    if (last) {
      data[1] |= 0x80;
    }
    BE.setuint16(data, 2, seqnum);
    BE.setuint32(data, 4, timestamp);
    BE.setuint32(data, 8, ssrc);
  }

  public void buildHeader(byte data[], int type) {
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
    if (rtp.useTURN) {
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
    JFLog.log("RTPChannel.start() : localhost:" + rtp.getlocalrtpport() + " remote=" + stream.getIP() + ":" + stream.getPort());

    if ((stream.codecs != null) && (stream.codecs.length > 0)) {
      Codec codec_rfc2833 = stream.getCodec(RTP.CODEC_RFC2833);
      if (codec_rfc2833 != null) {
        rfc2833_id = codec_rfc2833.id;
      }
      Codec codec_vp8 = stream.getCodec(RTP.CODEC_VP8);
      if (codec_vp8 != null) {
        vp8_id = codec_vp8.id;
      }

      Codec codec_h264 = stream.getCodec(RTP.CODEC_H264);
      if (codec_h264 != null) {
        h264_id = codec_h264.id;
      }

      Codec codec_h263_1998 = stream.getCodec(RTP.CODEC_H263_1998);
      if (codec_h263_1998 != null) {
        h263_1998_id = codec_h263_1998.id;
      }

      Codec codec_h263_2000 = stream.getCodec(RTP.CODEC_H263_2000);
      if (codec_h263_2000 != null) {
        h263_2000_id = codec_h263_2000.id;
      }

      coder_g711u = new g711u(rtp);
      coder_g711a = new g711a(rtp);
      coder_g722 = new g722(rtp);
      coder_g729a = new g729a(rtp);
      if (stream.type == SDP.Type.audio) {
        if (stream.hasCodec(RTP.CODEC_G711u)) {
          coder = coder_g711u;
          JFLog.log("codec = g711u");
        } else if (stream.hasCodec(RTP.CODEC_G711a)) {
          coder = coder_g711a;
          JFLog.log("codec = g711a");
        } else if (stream.hasCodec(RTP.CODEC_G722)) {
          coder = coder_g722;
          JFLog.log("codec = g722");
        } else if (stream.hasCodec(RTP.CODEC_G729a)) {
          JFLog.log("codec = g729a");
          coder = coder_g729a;
        } else {
          JFLog.log("RTP.start() : Warning : no compatible audio codec selected");
        }
        dtmf = new DTMF(coder.getSampleRate());
      } else {
        if (stream.hasCodec(RTP.CODEC_H263)) {
          JFLog.log("codec = H.263");
        } else if (stream.hasCodec(RTP.CODEC_H263_1998)) {
          JFLog.log("codec = H.263-1998");
        } else if (stream.hasCodec(RTP.CODEC_H263_2000)) {
          JFLog.log("codec = H.263-2000");
        } else if (stream.hasCodec(RTP.CODEC_JPEG)) {
          JFLog.log("codec = JPEG");
        } else if (stream.hasCodec(RTP.CODEC_H264)) {
          JFLog.log("codec = H.264");
        } else if (stream.hasCodec(RTP.CODEC_VP8)) {
          JFLog.log("codec = VP8");
        } else {
          JFLog.log("RTP.start() : Warning : no compatible video codec selected");
        }
      }
    } else {
      JFLog.log("RTP:Error:No codecs provided");
    }

    if (rtp.useTURN) {
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
        JFLog.log(e);
        return false;
      }
    }
    active = true;
    return true;
  }

  /**
   * Changes the SDP.Stream for this RTP session. Could occur in a SIP reINVITE.
   */
  public boolean change(SDP.Stream new_stream) {
    lastPacket = System.currentTimeMillis();
    stream = new_stream;
    if (stream.type == SDP.Type.audio) {
      if (new_stream.hasCodec(RTP.CODEC_G711u)) {
        coder = coder_g711u;
      } else if (new_stream.hasCodec(RTP.CODEC_G711a)) {
        coder = coder_g711a;
      } else if (new_stream.hasCodec(RTP.CODEC_G722)) {
        coder = coder_g722;
      } else if (new_stream.hasCodec(RTP.CODEC_G729a)) {
        coder = coder_g729a;
      }
      dtmf = new DTMF(coder.getSampleRate());
    }
    if (rtp.useTURN) {
      synchronized (rtp.bindLock) {
        rtp.bindingChannel = this;
        rtp.stun1.requestBind(turn1ch, stream.getIP(), stream.getPort());
        rtp.stun2.requestBind(turn2ch, stream.getIP(), stream.getPort() + 1);
      }
    }
    return true;
  }

  protected void processRTP(byte data[], int off, int len) {
    lastPacket = RTP.now;
    if (rtp.rawMode) {
      rtp.iface.rtpPacket(this, data, off, len);
      return;
    }
    int id = data[off + 1] & 0x7f;  //payload id
    if (id < 96) {
      switch (id) {
        case 0:
          dtmfSent = false;  //just in case end of dtmf was not received
          if (len != 160 + 12) {
            JFLog.log("RTP:Bad g711u length");
            break;
          }
          addSamples(coder_g711u.decode(data, off));
          rtp.iface.rtpSamples(this);
          break;
        case 8:
          dtmfSent = false;  //just in case end of dtmf was not received
          if (len != 160 + 12) {
            JFLog.log("RTP:Bad g711a length");
            break;
          }
          addSamples(coder_g711a.decode(data, off));
          rtp.iface.rtpSamples(this);
          break;
        case 9:
          dtmfSent = false;  //just in case end of dtmf was not received
          if (len != 160 + 12) {
            JFLog.log("RTP:Bad g722 length");
            break;
          }
          addSamples(coder_g722.decode(data, off));
          rtp.iface.rtpSamples(this);
          break;
        case 18:
          dtmfSent = false;  //just in case end of dtmf was not received
          if (len != 20 + 12) {
            JFLog.log("RTP:Bad g729a length");
            break;
          }
          addSamples(coder_g729a.decode(data, off));
          rtp.iface.rtpSamples(this);
          break;
        case 26:
          rtp.iface.rtpJPEG(this, data, off, len);
          break;
        case 34:
          rtp.iface.rtpH263(this, data, off, len);
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
        rtp.iface.rtpVP8(this, data, off, len);
      } else if (id == h264_id) {
        rtp.iface.rtpH264(this, data, off, len);
      } else if (id == h263_1998_id) {
        rtp.iface.rtpH263_1998(this, data, off, len);
      } else if (id == h263_2000_id) {
        rtp.iface.rtpH263_2000(this, data, off, len);
      }
    }
  }

  protected void processRTCP(byte data[], int off, int len) {
    if (rtp.rawMode) {
      rtp.iface.rtcpPacket(this, data, off, len);
    }
    //TODO : RTCP ???
  }

  protected void keepalive(long now) {
    //do refreshes a little sooner (75 seconds) (in case nonce changes)
    if (rtp.useTURN && (now + 75 * 1000) > turnBindExpires) {
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
  public boolean getSamples(short data[]) {
    if (!stream.canRecv()) {
      return moh.getSamples(data);
    }
    return buffer.get(data, 0, data.length);
  }

  private void addSamples(short data[]) {
    buffer.add(data, 0, data.length);
  }

  public String toString() {
    return "RTPChannel:{src=" + ssrc_src + ",dst=" + ssrc_dst + ",ip=" + stream.getIP() + ":" + stream.getPort() + "}";
  }
}

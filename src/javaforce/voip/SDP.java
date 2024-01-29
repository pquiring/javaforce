package javaforce.voip;

import java.util.*;

import javaforce.*;

/**
 * SDP (Session Description Protocol)
 *
 * @author pquiring
 *
 * Created : Nov 30, 2013
 */

public class SDP implements Cloneable {
  public static enum Type {audio, video, other};
  public static enum Mode {sendonly, recvonly, sendrecv, inactive};
  public static enum Profile {AVP, SAVP, AVPF, SAVPF, UNKNOWN};
  public static enum KeyExchange {NONE, SDP, DTLS};
  public class Key {
    public String crypto;
    public byte key[];
    public byte salt[];
  }
  public class Stream {
    public Type type;
    public Mode mode = Mode.sendrecv;
    public String ip;  //optional (stream specific) [rare]
    public Codec codecs[] = new Codec[0];
    public int port = -1;  //NATing if not specified
    public String content;
    public SDP sdp;
    public Profile profile = Profile.AVP;
    public KeyExchange keyExchange = KeyExchange.NONE;
    public Key[] keys;  //if KeyExchange == SDP

    public String getType() {
      switch (type) {
        case audio: return "audio";
        case video: return "video";
      }
      return "?";
    }
    public String getMode() {
      switch (mode) {
        case sendonly: return "sendonly";
        case recvonly: return "recvonly";
        case sendrecv: return "sendrecv";
        case inactive: return "inactive";
      }
      return "?";
    }
    public boolean hasCodec(Codec codec) {
      return SIP.hasCodec(codecs, codec);
    }
    public Codec addCodec(Codec codec) {
      codecs = SIP.addCodec(codecs, codec);
      return codec;
    }
    public void delCodec(Codec codec) {
      codecs = SIP.delCodec(codecs, codec);
    }
    public void setCodec(Codec codec) {
      codecs = new Codec[1];
      codecs[0] = codec;
    }
    public Codec getCodec(Codec codec) {
      return SIP.getCodec(codecs, codec);
    }
    public String getIP() {
      if (ip != null) return ip;
      return SDP.this.ip;
    }
    public void setIP(String ip) {
      this.ip = ip;
    }
    public int getPort() {
      return port;
    }
    public void setPort(int port) {
      this.port = port;
    }
    public boolean canSend() {
      return mode == Mode.sendrecv || mode == Mode.sendonly;
    }
    public boolean canRecv() {
      return mode == Mode.sendrecv || mode == Mode.recvonly;
    }
    public boolean isSecure() {
      return profile == Profile.SAVP || profile == Profile.SAVPF;
    }
    /**
     * Used to add SRTP keys to SDP (obsoleted by DTLS method)
     * Must set KeyExchange to SDP.
     *
     * @param crypto = AES_CM_128_HMAC_SHA1_80, etc.
     * @param key = 16 byte key
     * @param salt = 14 byte salt
     */
    public void addKey(String crypto, byte[] key, byte[] salt) {
      Key newkey = new Key();
      newkey.crypto = crypto;
      newkey.key = key;
      newkey.salt = salt;
      if (keys == null) keys = new Key[0];
      Key[] newKeys = new Key[keys.length + 1];
      newKeys[keys.length] = newkey;
      keys = newKeys;
    }
    public Key getKey(String crypto) {
      if (keys == null) return null;
      for(int a=0;a<keys.length;a++) {
        if (keys[a].crypto.equals(crypto)) return keys[a];
      }
      return null;
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(type);
      sb.append(",");
      sb.append(mode);
      sb.append(",codecs={");
      for(Codec codec : codecs) {
        sb.append(codec);
      }
      sb.append("}");
      return sb.toString();
    }
  }
  public int log;
  public void setLog(int id) {
    log = id;
  }
  public String ip;  //global connection
  public String iceufrag, icepwd, fingerprint;
  public Stream[] streams = new Stream[0];
  public String owner;
  public String session;
  public long o1 = 256, o2 = 256;
  public long time_start, time_stop;
  public float framerate;
  public ArrayList<String> otherAttributes = new ArrayList<String>();  //list of unknown attributes (a=...)
  public ArrayList<String> otherParameters = new ArrayList<String>();  //list of unknown parameters (?=...)

  public Stream addStream(Type type) {
    JFLog.log(log, "SDP.addStream:" + type);
    Stream stream = new Stream();
    stream.sdp = this;
    stream.type = type;
    streams = Arrays.copyOf(streams, streams.length+1);
    streams[streams.length-1] = stream;
    return stream;
  }
  public void delStream(Stream stream) {
    for(int a=0;a<streams.length;a++) {
      if (streams[a] == stream) {
        streams = (Stream[])JF.copyOfExcluding(streams, a);
        return;
      }
    }
  }
  public void delAudio() {
    for(int a=0;a<streams.length;) {
      if (streams[a].type == Type.audio) {
        streams = (Stream[])JF.copyOfExcluding(streams, a);
      } else {
        a++;
      }
    }
  }
  public void delVideo() {
    for(int a=0;a<streams.length;) {
      if (streams[a].type == Type.video) {
        streams = (Stream[])JF.copyOfExcluding(streams, a);
      } else {
        a++;
      }
    }
  }
  public Stream getFirstAudioStream() {
    for(int a=0;a<streams.length;a++) {
      if (streams[a].type == Type.audio) return streams[a];
    }
    return null;
  }
  public Stream getFirstVideoStream() {
    for(int a=0;a<streams.length;a++) {
      if (streams[a].type == Type.video) return streams[a];
    }
    return null;
  }
  public boolean hasAudio() {
    return getFirstAudioStream() != null;
  }
  public boolean hasVideo() {
    return getFirstVideoStream() != null;
  }
  public float getFrameRate() {
    return framerate;
  }
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SDP:" + streams.length + "[");
    for(int a=0;a<streams.length;a++) {
      if (a > 0) sb.append(",");
      sb.append("Stream=" + streams[a]);
    }
    sb.append("]");
    return sb.toString();
  }
  public Object clone() {
    try {
      return super.clone();
    } catch (Exception e) {
      return null;
    }
  }
  /**
   * Builds SDP packet. (RFC 2327)
   */
  public String[] build(String localhost) {
    //build SDP content
    ArrayList<String> content = new ArrayList<>();
    content.add("v=0");
    content.add("o=- " + o1 + " " + o2 + " IN IP4 " + localhost);
    content.add("s=" + SIP.useragent);
    content.add("c=IN IP4 " + ip);
    content.add("t=" + time_start + " " + time_stop);
    if (iceufrag != null) content.add("a=ice-ufrag:" + iceufrag);
    if (icepwd != null) content.add("a=ice-pwd:" + icepwd);
    if (fingerprint != null) content.add("a=fingerprint:sha-256 " + fingerprint);
    for(int a=0;a<streams.length;a++) {
      Stream stream = streams[a];
      if (stream.codecs.length == 0) continue;
      Codec rfc2833 = stream.getCodec(RTP.CODEC_RFC2833);
      StringBuilder m = new StringBuilder();
      m.append("m=" + stream.getType() + " " + stream.port + " ");
      if (stream.keyExchange == KeyExchange.DTLS) {
        m.append("UDP/TLS/");
      }
      m.append("RTP/" + stream.profile);
      for(int b=0;b<stream.codecs.length;b++) {
        m.append(" " + stream.codecs[b].id);
      }
      if (stream.type == Type.audio && rfc2833 == null) {
        rfc2833 = RTP.CODEC_RFC2833;
        m.append(" " + rfc2833.id);
      }
      content.add(m.toString());

      if (stream.keyExchange == KeyExchange.SDP && stream.keys != null) {
        for(int c=0;c<stream.keys.length;c++) {
          Key keys = stream.keys[c];
          byte key_salt[] = new byte[16 + 14];
          System.arraycopy(keys.key, 0, key_salt, 0, 16);
          System.arraycopy(keys.salt, 0, key_salt, 16, 14);
          String keystr = new String(javaforce.Base64.encode(key_salt));
                                               //keys      | lifetime| mki:length
          String ln = keys.crypto + " inline:" + keystr + "|2^48" + "|1:32";
          content.add("a=crypto:" + (c+1) + " " + ln);
        }
      }
      if (stream.content != null) {
        content.add("a=content:" + stream.content);
      }
      content.add("a=" + stream.getMode());
      if (stream.ip != null) {
        content.add("c=IN IP4 " + stream.ip);
      }
      content.add("a=ptime:20");
      if (stream.hasCodec(RTP.CODEC_G711u)) {
        content.add("a=rtpmap:0 PCMU/8000");
      }
      if (stream.hasCodec(RTP.CODEC_G711a)) {
        content.add("a=rtpmap:8 PCMA/8000");
      }
      if (stream.hasCodec(RTP.CODEC_GSM)) {
        content.add("a=rtpmap:3 GSM/8000");
      }
      if (stream.hasCodec(RTP.CODEC_G722)) {
        content.add("a=rtpmap:9 G722/8000");  //NOTE:It's really 16000 but an error in RFC claims it as 8000
      }
      if (stream.hasCodec(RTP.CODEC_G729a)) {
        content.add("a=rtpmap:18 G729/8000");
        content.add("a=fmtp:18 annexb=no");
        content.add("a=silenceSupp:off - - - -");
      }
      if (stream.type == Type.audio) {
        content.add("a=rtpmap:" + rfc2833.id + " telephone-event/8000");
        content.add("a=fmtp:" + rfc2833.id + " 0-15");
      }
      if (stream.hasCodec(RTP.CODEC_JPEG)) {
        content.add("a=rtpmap:26 JPEG/90000");
      }
      if (stream.hasCodec(RTP.CODEC_H263)) {
        content.add("a=rtpmap:34 H263/90000");
      }
      if (stream.hasCodec(RTP.CODEC_H263_1998)) {
        content.add("a=rtpmap:" + stream.getCodec(RTP.CODEC_H263_1998).id + " H263-1998/90000");
      }
      if (stream.hasCodec(RTP.CODEC_H263_2000)) {
        content.add("a=rtpmap:" + stream.getCodec(RTP.CODEC_H263_2000).id + " H263-2000/90000");
      }
      if (stream.hasCodec(RTP.CODEC_H264)) {
        content.add("a=rtpmap:" + stream.getCodec(RTP.CODEC_H264).id + " H264/90000");
      }
      if (stream.hasCodec(RTP.CODEC_H265)) {
        content.add("a=rtpmap:" + stream.getCodec(RTP.CODEC_H265).id + " H265/90000");
      }
      if (stream.hasCodec(RTP.CODEC_VP8)) {
        content.add("a=rtpmap:" + stream.getCodec(RTP.CODEC_VP8).id + " VP8/90000");
      }
      JFLog.log("keyexchange=" + stream.keyExchange);
      if (stream.keyExchange == KeyExchange.DTLS) {
        content.add("a=rtcp-mux");  //http://tools.ietf.org/html/rfc5761
      }
    }
    return content.toArray(new String[0]);
  }
}

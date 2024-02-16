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
    public byte[] key;
    public byte[] salt;
  }
  public class Stream {
    public Type type;
    public Mode mode = Mode.sendrecv;
    public String ip;  //optional (stream specific) [rare]
    public Codec[] codecs = new Codec[0];
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
      sb.append("Stream:{");
      sb.append("ip=" + getIP());
      sb.append(",port=" + getPort());
      sb.append(",type=" + type);
      sb.append(",mode=" + mode);
      sb.append(",codecs={");
      for(Codec codec : codecs) {
        sb.append(codec);
      }
      sb.append("}");
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
    sb.append("FPS=" + framerate);
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
          byte[] key_salt = new byte[16 + 14];
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
      if (stream.hasCodec(RTP.CODEC_VP9)) {
        content.add("a=rtpmap:" + stream.getCodec(RTP.CODEC_VP9).id + " VP9/90000");
      }
      JFLog.log("keyexchange=" + stream.keyExchange);
      if (stream.keyExchange == KeyExchange.DTLS) {
        content.add("a=rtcp-mux");  //http://tools.ietf.org/html/rfc5761
      }
    }
    if (framerate != 0.0f) {
      content.add("a=framerate:" + framerate);
    }
    return content.toArray(JF.StringArrayType);
  }

  public static SDP getSDP(String[] msg) {
    return getSDP(msg, 0);
  }

  /**
   * Parses the SDP content.
   *
   * @param msg = SDP text
   * @param log = JFLog log id
   */
  public static SDP getSDP(String[] msg, int log) {
    String type = HTTP.getParameter(msg, "Content-Type");
    if (type == null) type = HTTP.getParameter(msg, "c");  //short form
    if (type == null || type.indexOf("application/sdp") == -1) return null;
    SDP sdp = new SDP();
    sdp.setLog(log);
    SDP.Stream stream = null;
    int idx;
    int start = -1;
    for(int a=0;a<msg.length;a++) {
      if (msg[a].length() == 0) {start = a+1; break;}
    }
    if (start == -1) {
      JFLog.log(log, "SIP.getSDP() : No SDP found");
      return null;
    }
    int acnt = 1;
    int vcnt = 1;
    for(int a=start;a<msg.length;a++) {
      String ln = msg[a];
      if (ln.startsWith("c=")) {
        //c=IN IP4 1.2.3.4
        idx = ln.indexOf("IP4 ");
        if (idx == -1) {JFLog.log(log, "SIP.getSDP() : Unsupported c field:" + ln); continue;}
        String ip = ln.substring(idx+4);
        if (stream == null) {
          sdp.ip = ip;
        } else {
          stream.ip = ip;
        }
      } else if (ln.startsWith("m=")) {
        //m=audio <port> [UDP/TLS/]RTP/<profile> <codecs>
        if (stream != null) {
          if (stream.content == null) {
            switch (stream.type) {
              case audio: stream.content = "audio" + (acnt++); break;
              case video: stream.content = "video" + (vcnt++); break;
            }
          }
        }
        if (ln.startsWith("m=audio")) {
          stream = sdp.addStream(SDP.Type.audio);
        } else if (ln.startsWith("m=video")) {
          stream = sdp.addStream(SDP.Type.video);
        } else {
          JFLog.log("SIP.getSDP() : Unsupported m field:" + ln);
          stream = sdp.addStream(SDP.Type.other);
          continue;
        }
        //parse static codecs
        String[] f = ln.split(" ");
        String[] p = f[2].split("/");
        //p[] = RTP/AVP
        //p[] = UDP/TLS/RTP/SRTP
        int i = 1;
        if (p[i].equals("TLS")) {
          stream.keyExchange = SDP.KeyExchange.DTLS;
          i = 3;
        }
        if (p[i].equals("AVP")) {
          stream.profile = SDP.Profile.AVP;
        } else if (p[i].equals("AVPF")) {
          stream.profile = SDP.Profile.AVPF;
        } else if (p[i].equals("SAVP")) {
          stream.profile = SDP.Profile.SAVP;
        } else if (p[i].equals("SAVPF")) {
          stream.profile = SDP.Profile.SAVPF;
        } else {
          stream.profile = SDP.Profile.UNKNOWN;
          JFLog.log(log, "SIP.getSDP() : Unsupported profile:" + p[i]);
        }
        stream.port = JF.atoi(f[1]);
        for(int b=3;b<f.length;b++) {
          int id = JF.atoi(f[b]);
          if (id < 96) {
            stream.addCodec(new Codec(SIP.getCodecName(id), id));
          }
        }
      } else if (ln.startsWith("a=")) {
        if (ln.startsWith("a=rtpmap:")) {
          //a=rtpmap:<id> <name>/<bitrate>
          String[] f = ln.substring(9).split(" ");
          int id = JF.atoi(f[0]);
          String[] n = f[1].split("/");
          if (id >= 96) {
            stream.addCodec(new Codec(n[0], id));
          }
        }
        else if (ln.startsWith("a=sendrecv")) {
          if (stream != null) {
            stream.mode = SDP.Mode.sendrecv;
          }
        }
        else if (ln.startsWith("a=sendonly")) {
          if (stream != null) {
            stream.mode = SDP.Mode.sendonly;
          }
        }
        else if (ln.startsWith("a=recvonly")) {
          if (stream != null) {
            stream.mode = SDP.Mode.sendonly;
          }
        }
        else if (ln.startsWith("a=inactive")) {
          if (stream != null) {
            stream.mode = SDP.Mode.inactive;
          }
        }
        else if (ln.startsWith("a=content:")) {
          stream.content = ln.substring(10);
        }
        else if (ln.startsWith("a=candidate:")) {
          //            0 1 2   3          4          5     6   7     8     9          10    11
          //a=candidate:0 1 UDP 2128609535 10.1.1.100 60225 typ host
          //a=candidate:1 1 UDP 1692467199 x.x.x.x    60225 typ srflx raddr 10.1.1.100 rport 60225
          //a=candidate:0 2 UDP 2128609534 10.1.1.100 60226 typ host
          //a=candidate:1 2 UDP 1692467198 x.x.x.x    60226 typ srflx raddr 10.1.1.100 rport 60226
          String[] f = ln.substring(12).split(" ");
          if (stream != null && f.length >= 8 && f[0].equals("0") && f[1].equals("1")) {
            //override ip
            stream.ip = f[4];
          }
        }
        else if (ln.startsWith("a=ice-ufrag:")) {
          sdp.iceufrag = ln.substring(12);
        }
        else if (ln.startsWith("a=ice-pwd:")) {
          sdp.icepwd = ln.substring(10);
        }
        else if (ln.startsWith("a=fingerprint:sha-256 ")) {
          sdp.fingerprint = ln.substring(22);
        }
        else if (ln.startsWith("a=crypto:")) {
          //SRTP Keys (replaced by DTLS method)
          //a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:PS1uQCVeeCFCanVmcjkpPywjNWhcYD0mXXtxaVBR|2^20|1:32
          //         # crypto                         base64_key_salt                          life mki
          stream.keyExchange = SDP.KeyExchange.SDP;
          String[] f = ln.split(" ");
          if (!f[2].startsWith("inline:")) {
            JFLog.log("a=crypto:bad keys(1)");
            continue;
          }
          String base64 = f[2].substring(7);
          int pipe = base64.indexOf("|");
          if (pipe != -1) {
            base64 = base64.substring(0, pipe);
          }
          byte[] keys = javaforce.Base64.decode(base64.getBytes());
          if (keys == null || keys.length != 30) {
            JFLog.log("a=crypto:bad keys(2)");
            continue;
          }
          byte[] key = Arrays.copyOfRange(keys, 0, 16);
          byte[] salt = Arrays.copyOfRange(keys, 16, 16 + 14);
          stream.addKey(f[1], key, salt);
        }
        else if (ln.startsWith("a=framerate:")) {
          sdp.framerate = JF.atof(ln.substring(12));
        }
        else {
          sdp.otherAttributes.add(ln.substring(2));
        }
      }
      else if (ln.startsWith("o=")) {
        //o=- {count} {count} IN IP4 {host}
        int spc = ln.indexOf(' ');
        String[] os = ln.substring(spc + 1).split(" ");
        try {
          sdp.o1 = Long.valueOf(os[0]);
        } catch (Exception e) {
          sdp.o1 = 256;
        }
        try {
          sdp.o2 = Long.valueOf(os[1]);
        } catch (Exception e) {
          sdp.o2 = 256;
        }
        sdp.owner = ln.substring(2);
      }
      else if (ln.startsWith("s=")) {
        //s=session_name
        sdp.session = ln.substring(2);
      }
      else if (ln.startsWith("t=")) {
        //t=START STOP
        String[] f = ln.substring(2).split("[ ]");
        if (f.length >= 2) {
          sdp.time_start = Long.valueOf(f[0]);
          sdp.time_stop = Long.valueOf(f[1]);
        }
      }
      else {
        sdp.otherParameters.add(ln);
      }
    }
    if ((stream != null) && (stream.content == null)) {
      switch (stream.type) {
        case audio: stream.content = "audio" + (acnt++); break;
        case video: stream.content = "video" + (vcnt++); break;
      }
    }
    return sdp;
  }
}

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
  public long time_start, time_stop;

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
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SDP:" + streams.length + "[");
    for(int a=0;a<streams.length;a++) {
      if (a > 0) sb.append(",");
      sb.append("Stream=" + streams[a].type + "," + streams[a].mode);
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
}

package javaforce.voip;

import java.util.*;
import java.net.*;
import javaforce.*;

public class RTPChannel {

  private RTP rtp;
  private int seqnum = 0;
  private int timestamp = 0;
  private int ssrc = -1;
  private static Random r = new Random();
  private static Object rlock = new Object();
  private short dtmfduration = 0;

  protected RTPChannel(RTP rtp, int ssrc) {
    this.rtp = rtp;
    this.ssrc = ssrc;
  }

  /**
   * Writes a packet to the RTP port.
   */
  public void writeRTP(byte data[], int off, int len) {
    if (!rtp.active) {
      return;
    }
    if (rtp.remoteport == -1) {
      return;  //not ready yet (NATing)
    }
    synchronized (rtp.lockHostPort) {
      try {
        rtp.sock1.send(new DatagramPacket(data, off, len, InetAddress.getByName(rtp.remoteip), rtp.remoteport));
      } catch (Exception e) {
        JFLog.log("err:RTP.writeRTP:failed");
        JFLog.log(e);
      }
    }
  }

  /**
   * Writes a packet to the RTCP port.
   */
  public void writeRTCP(byte data[], int off, int len) {
    if (!rtp.active) {
      return;
    }
    if (rtp.remoteport == -1) {
      return;  //not ready yet (NATing)
    }
    synchronized (rtp.lockHostPort) {
      try {
        rtp.sock2.send(new DatagramPacket(data, off, len, InetAddress.getByName(rtp.remoteip), rtp.remoteport + 1));
      } catch (Exception e) {
        JFLog.log("err:RTP.writeRTCP:failed");
        JFLog.log(e);
      }
    }
  }

  /**
   * Writes a RFC 2833 (DTMF) RTP packet.
   */
  public void writeDTMF(char digit, boolean end) {
    byte data[] = new byte[16];
    buildHeader(data, RTP.CODEC_RFC2833.id, getseqnum(), gettimestamp(160), getssrc());
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
  public static void buildHeader(byte data[], int id, int seqnum, int timestamp, int ssrc) {
    //build RTP header
    data[0] = (byte) 0x80;  //version
    data[1] = (byte) id;    //0=g711u 8=g711a 18=g729a 26=JPEG 34=H.263 etc.
    data[2] = (byte) ((seqnum & 0xff00) >> 8);
    data[3] = (byte) (seqnum & 0xff);
    data[4] = (byte) ((timestamp & 0xff000000) >>> 24);
    data[5] = (byte) ((timestamp & 0xff0000) >> 16);
    data[6] = (byte) ((timestamp & 0xff00) >> 8);
    data[7] = (byte) (timestamp & 0xff);
    data[8] = (byte) ((ssrc & 0xff000000) >>> 24);
    data[9] = (byte) ((ssrc & 0xff0000) >> 16);
    data[10] = (byte) ((ssrc & 0xff00) >> 8);
    data[11] = (byte) (ssrc & 0xff);
  }

  public void buildHeader(byte data[], int type) {
    buildHeader(data, type, 0, 0, getssrc());
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
    if (ssrc != -1) {
      return ssrc;
    }
    synchronized (rlock) {
      ssrc = r.nextInt() & 0x7fffffff;
    }
    return ssrc;
  }

  public int getseqnum(byte[] data, int off) {
    return getuint16(data, 2 + off);
  }

  public int gettimestamp(byte[] data, int off) {
    return getuint32(data, 4 + off);
  }

  public int getssrc(byte[] data, int off) {
    return getuint32(data, 8 + off);
  }

  private int getuint16(byte[] data, int offset) {
    int ret;
    ret = (int) data[offset] & 0xff;
    ret += ((int) data[offset + 1] & 0xff) << 8;
    return ret;
  }

  private int getuint32(byte[] data, int offset) {
    int ret;
    ret = (int) data[offset] & 0xff;
    ret += ((int) data[offset + 1] & 0xff) << 8;
    ret += ((int) data[offset + 2] & 0xff) << 16;
    ret += ((int) data[offset + 3] & 0xff) << 24;
    return ret;
  }
}

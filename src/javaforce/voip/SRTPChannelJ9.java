package javaforce.voip;

/** Secure RTP using Java9 DTLS (not working yet)
 *
 * JEP 219
 *
 * BUG : https://bugs.openjdk.org/browse/JDK-8250643
 *
 * @author pquiring
 *
 * Created : Dec ?, 2013
 *
 * RFCs:
 * http://tools.ietf.org/html/rfc3711 - SRTP
 * http://tools.ietf.org/html/rfc4568 - Using SDP to exchange keys for SRTP (old method before DTLS) (obsolete)
 * http://tools.ietf.org/html/rfc5764 - Using DTLS to exchange keys for SRTP
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.*;

import javaforce.*;

public class SRTPChannelJ9 extends RTPChannel {
  private boolean server;
  public static String local_icepwd;  //TODO

  public SRTPChannelJ9(RTP rtp, int ssrc, SDP.Stream stream, boolean server) {
    super(rtp,ssrc,stream);
    this.server = server;
  }

  public boolean start() {
    JFLog.log("SRTPChannel.start()");
    if (!super.start()) return false;
    if (rtp.rawMode) return true;
    new Thread() {
      public void run() {
        if (server) {
          UdpServer svr = new UdpServer();
          svr.start();
        } else {
          UdpClient clt = new UdpClient();
          clt.start();
        }
      }
    }.start();
    return true;
  }

  public static void _log(String side, boolean client, String msg) {
    JFLog.log(side + ":" + (client ? "client" : "server") + ":" + msg);
  }

  public static TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
      public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
      public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    }
  };

  public void writeRTP(byte[] data, int off, int len) {
    synchronized(data_output) {
      if (off == 0 && len == data.length) {
        data_output.add(data);
      } else {
        data_output.add(Arrays.copyOfRange(data, off, off+len));
      }
    }
  }

  protected void processRTP(byte[] data, int off, int len) {
    synchronized(data_input) {
      if (off == 0 && len == data.length) {
        data_input.add(data);
      } else {
        data_input.add(Arrays.copyOfRange(data, off, off+len));
      }
    }
  }

  private ArrayList<byte[]> data_input = new ArrayList<byte[]>();
  private ArrayList<byte[]> data_output = new ArrayList<byte[]>();

  public static void initCtx(SSLContext ctx) {
    try {
      char[] passphrase = "password".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream("dtls.key"), passphrase);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, passphrase);
      ctx.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public class UdpHandshake extends Thread {
    public SSLEngine ssl;
    public boolean client;
    public ByteBuffer intransfer, outtransfer, input, output;
    public void log(String msg) {
      _log("handshake", client, msg);
    }
    public void run() {
      try {
        SSLSession sess = ssl.getSession();
        int maxAppSize = sess.getApplicationBufferSize();
        int maxPackSize = sess.getPacketBufferSize();
        input = ByteBuffer.allocate(maxAppSize + 50);
        output = ByteBuffer.allocate(maxAppSize + 50);
        intransfer = ByteBuffer.allocateDirect(maxPackSize * 2);
        outtransfer = ByteBuffer.allocateDirect(maxPackSize * 2);
        DatagramPacket dp;
        SSLEngineResult res;
        int consumed;
        int produced;
        while (true) {
          log("status=" + ssl.getHandshakeStatus());
          SSLEngineResult.HandshakeStatus status = ssl.getHandshakeStatus();
          if (status == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            if (client)
              status = SSLEngineResult.HandshakeStatus.NEED_WRAP;
            else
              status = SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
          }
          switch (status) {
            case NEED_TASK:
              Runnable runnable;
              while ((runnable = ssl.getDelegatedTask()) != null) {
                runnable.run();
              }
              break;
            case NEED_WRAP:
              res = ssl.wrap(output, outtransfer);
              if (res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                log("done");
                return;
              }
              consumed = res.bytesConsumed();
              produced = res.bytesProduced();
              if (produced > 0) {
                log("rawwrite:" + produced);
                byte[] out = new byte[produced];
                outtransfer.flip();
                if (outtransfer.remaining() != produced) {
                  throw new Exception("transfer.remaining() != produced");
                }
                outtransfer.get(out);
                SRTPChannelJ9.super.writeRTP(out, 0, out.length);
              }
              outtransfer.compact();
              output.compact();
              break;
            case NEED_UNWRAP:
              byte[] data;
              synchronized(data_input) {
                if (data_input.size() == 0) break;
                data = data_input.remove(0);
              }
              int length = data.length;
              if (length > 0) {
                log("rawread:" + length);
                intransfer.put(data, 0, length);
              }
              intransfer.flip();
              res = ssl.unwrap(intransfer, input);
              if (res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                log("done");
                return;
              }
              consumed = res.bytesConsumed();
              produced = res.bytesProduced();
              intransfer.compact();
              input.compact();
              break;
            case NEED_UNWRAP_AGAIN:
              intransfer.limit(0);  //intransfer must be empty
              res = ssl.unwrap(intransfer, input);
              if (res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
                log("done");
                return;
              }
              consumed = res.bytesConsumed();
              produced = res.bytesProduced();
              intransfer.compact();
              input.compact();
              break;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public class UdpReader extends Thread {
    public SSLEngine ssl;
    public boolean client;
    public ByteBuffer transfer, input;
    public void log(String msg) {
      _log("reader", client, msg);
    }
    public void run() {
      try {
        SSLSession sess = ssl.getSession();
        int maxAppSize = sess.getApplicationBufferSize();
        int maxPackSize = sess.getPacketBufferSize();
        input = ByteBuffer.allocate(maxAppSize + 50);
        transfer = ByteBuffer.allocateDirect(maxPackSize);
        int total = 0;
        DatagramPacket dp;
        while (true) {
          byte[] data;
          synchronized(data_input) {
            if (data_input.size() == 0) break;
            data = data_input.remove(0);
          }
          int length = data.length;
          if (length > 0) {
            int maxRead = transfer.remaining();
            if (length > maxRead) length = maxRead;
            log("rawread:" + length);
            transfer.put(data, 0, length);
          }
          transfer.flip();
          SSLEngineResult res;
          res = ssl.unwrap(transfer, input);
          int consumed = res.bytesConsumed();
          int produced = res.bytesProduced();
          input.flip();
          if (produced > 0) {
            byte[] tmp = new byte[produced];
            input.get(tmp);
            //TODO : write tmp somewhere (check with client generated data)
            total += produced;
            if (total == 1024) break;
          }
          transfer.compact();
          input.compact();
          if (total == 1024) break;
        }
        log("done");
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  public class UdpWriter extends Thread {
    public SSLEngine ssl;
    public boolean client;
    public ByteBuffer output, transfer;
    public UdpReader reader;
    public void log(String msg) {
      _log("writer", client, msg);
    }
    public void run() {
      try {
        SSLSession sess = ssl.getSession();
        byte[] tmp = new byte[1024];
        Random r = new Random();
        r.nextBytes(tmp);
        output = ByteBuffer.wrap(tmp);
        transfer = ByteBuffer.allocateDirect(sess.getPacketBufferSize());
        DatagramPacket dp;
        int total = 0;
        while (true) {
          SSLEngineResult res = ssl.wrap(output, transfer);
          int consumed = res.bytesConsumed();
          int produced = res.bytesProduced();
          if (consumed == 0 && produced == 0) {
            JF.sleep(10);
            continue;
          }
          transfer.flip();
          if (produced > 0) {
            byte[] out = new byte[produced];
            transfer.get(out);
            SRTPChannelJ9.super.writeRTP(out, 0, out.length);
          }
          if (consumed > 0) {
            total += consumed;
            if (total == 1024) break;
          }
          output.flip();
          output.compact();
          transfer.compact();
        }
        log("done");
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  public class UdpServer extends Thread {
    public SSLContext ctx;
    public SSLSessionContext sessctx;
    public SSLEngine ssl;
    public void run() {
      try {
        JFLog.log("Server binding on port 1111");
        ctx = SSLContext.getInstance("DTLS");
        initCtx(ctx);
        ssl = ctx.createSSLEngine();
        ssl.setUseClientMode(false);
        sessctx = ctx.getServerSessionContext();
        UdpHandshake handshake = new UdpHandshake();
        handshake.ssl = ssl;
        handshake.client = false;
        handshake.start();
        handshake.join();
        UdpReader reader = new UdpReader();
        reader.ssl = ssl;
        UdpWriter writer = new UdpWriter();
        writer.ssl = ssl;
        writer.reader = reader;
        reader.start();
        writer.start();
        reader.join();
        writer.join();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  public class UdpClient extends Thread {
    public SSLContext ctx;
    public SSLSessionContext sessctx;
    public SSLEngine ssl;
    public void run() {
      try {
        JFLog.log("Client binding on port 2222");
        ctx = SSLContext.getInstance("DTLS");
        ctx.init(null, trustAllCerts, new java.security.SecureRandom());
        ssl = ctx.createSSLEngine("localhost", 2222);
        ssl.setUseClientMode(true);
        sessctx = ctx.getClientSessionContext();
        UdpHandshake handshake = new UdpHandshake();
        handshake.ssl = ssl;
        handshake.client = true;
        handshake.start();
        handshake.join();
        UdpReader reader = new UdpReader();
        reader.ssl = ssl;
        reader.client = true;
        UdpWriter writer = new UdpWriter();
        writer.ssl = ssl;
        writer.client = true;
        writer.reader = reader;
        reader.start();
        writer.start();
        reader.join();
        writer.join();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private final static short BINDING_REQUEST = 0x0001;

  private final static short BINDING_RESPONSE = 0x0101;

  private final static short MAPPED_ADDRESS = 0x0001;
  private final static short USERNAME = 0x0006;
  private final static short XOR_MAPPED_ADDRESS = 0x0020;
  private final static short MESSAGE_INTEGRITY = 0x0008;

  //http://tools.ietf.org/html/rfc5245 - ICE
  private final static short USE_CANDIDATE = 0x25;  //used by ICE_CONTROLLING only
  private final static short PRIORITY = 0x24;  //see section 4.1.2
  private final static short ICE_CONTROLLING = (short)0x802a;
  private final static short ICE_CONTROLLED = (short)0x8029;
  private final static short FINGERPRINT = (short)0x8028;

  private byte[] stun;

  private boolean isClassicStun(long id1) {
    return ((id1 >>> 32) != 0x2112a442);
  }

  private int IP4toInt(String ip) {
    String[] o = ip.split("[.]");
    int ret = 0;
    for (int a = 0; a < 4; a++) {
      ret <<= 8;
      ret += (JF.atoi(o[a]));
    }
    return ret;
  }

  protected void processSTUN(byte[] data, int off, int len) {
    JFLog.log("SRTPChannel:received STUN request");
    //the only command supported is BINDING_REQUEST
    String username = null, flipped = null;
    ByteBuffer bb = ByteBuffer.wrap(data, off, len);
    bb.order(ByteOrder.BIG_ENDIAN);
    int offset = off;
    short code = bb.getShort(offset);
    boolean auth = false;
    if (code != BINDING_REQUEST) {
      JFLog.log("RTPSecureChannel:Error:STUN Request is not Binding request");
      return;
    }
    offset += 2;
    int lengthOffset = offset;
    short length = bb.getShort(offset);
    offset += 2;
    long id1 = bb.getLong(offset);
    offset += 8;
    long id2 = bb.getLong(offset);
    offset += 8;
    while (offset < len) {
      short attr = bb.getShort(offset);
      offset += 2;
      length = bb.getShort(offset);
      offset += 2;
      switch (attr) {
        case USERNAME:
          username = new String(data, offset, length);
          String[] f = username.split("[:]");
          flipped = f[1] + ":" + f[0];  //reverse username
          break;
        case MESSAGE_INTEGRITY:
          bb.putShort(lengthOffset, (short) (offset));  //patch length
          byte[] correct = STUN.calcMsgIntegrity(data, offset - 4, STUN.calcKey(local_icepwd));
          byte[] supplied = Arrays.copyOfRange(data, offset, offset + 20);
          auth = Arrays.equals(correct, supplied);
          break;
      }
      offset += length;
      if ((length & 0x3) > 0) {
        offset += 4 - (length & 0x3);  //padding
      }
    }
    if (!auth) {
      return;  //wrong credentials
    }
    //build response
    if (stun == null) {
      stun = new byte[1500];
    }
    bb = ByteBuffer.wrap(stun);
    bb.order(ByteOrder.BIG_ENDIAN);
    offset = 0;
    bb.putShort(offset, BINDING_RESPONSE);
    offset += 2;
    lengthOffset = offset;
    bb.putShort(offset, (short) 0);  //length (patch later)
    offset += 2;
    bb.putLong(offset, id1);
    offset += 8;
    bb.putLong(offset, id2);
    offset += 8;

    if (isClassicStun(id1)) {
      bb.putShort(offset, MAPPED_ADDRESS);
      offset += 2;
      bb.putShort(offset, (short) 8);  //length of attr
      offset += 2;
      bb.put(offset, (byte) 0);  //reserved
      offset++;
      bb.put(offset, (byte) 1);  //IP family
      offset++;
      bb.putShort(offset, (short) stream.port);
      offset += 2;
      bb.putInt(offset, IP4toInt(stream.getIP()));
      offset += 4;
    } else {
      //use XOR_MAPPED_ADDRESS instead
      bb.putShort(offset, XOR_MAPPED_ADDRESS);
      offset += 2;
      bb.putShort(offset, (short) 8);  //length of attr
      offset += 2;
      bb.put(offset, (byte) 0);  //reserved
      offset++;
      bb.put(offset, (byte) 1);  //IP family
      offset++;
      bb.putShort(offset, (short) (stream.port ^ bb.getShort(4)));
      offset += 2;
      bb.putInt(offset, IP4toInt(stream.getIP()) ^ bb.getInt(4));
      offset += 4;
    }

    bb.putShort(lengthOffset, (short) (offset - 20 + 24));  //patch length
    byte[] id = STUN.calcMsgIntegrity(stun, offset, STUN.calcKey(local_icepwd));
    int strlen = id.length;
    bb.putShort(offset, MESSAGE_INTEGRITY);
    offset += 2;
    bb.putShort(offset, (short) strlen);
    offset += 2;
    System.arraycopy(id, 0, stun, offset, strlen);
    offset += strlen;
    if ((offset & 3) > 0) {
      offset += 4 - (offset & 3);  //padding
    }

    bb.putShort(lengthOffset, (short) (offset - 20));  //patch length

    try {
      if (rtp.useTURN) {
        rtp.stun1.sendData(turn1ch, stun, 0, offset);
      } else {
        rtp.sock1.send(stun, 0, offset, InetAddress.getByName(stream.getIP()), stream.getPort());
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

}

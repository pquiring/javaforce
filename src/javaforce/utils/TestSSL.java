package javaforce.utils;

/** Test SSL
 *
 * Note : use keytool to generate a keystore : testssl.key
 *
 * You could create the keystore using the following ant build file (ant -f testssl.xml)

  <project default="genkey">
  <property name="app" value="testssl"/>
  <target name="genkey" unless="keyexists" description="generate keys">
    <genkey alias="${app}" storepass="password" keystore="testssl.key" keyalg="RSA" dname="CN=${app}.sourceforge.net, OU=${app}, O=JavaForce, C=CA" storetype="pkcs12"/>
  </target>
  </project>

 *
 * @author pquiring
 */

import java.net.*;
import java.io.*;
import javax.net.*;
import javax.net.ssl.*;
import java.nio.*;
import java.util.*;
import java.security.cert.*;
import java.security.*;
import static javax.net.ssl.SSLEngineResult.*;

import javaforce.*;

public class TestSSL {
  public static void main(String[] args) {
    try {
      testTcp();
      testUdp();
    } catch (Exception e) {
      JFLog.log(e);
    }
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

  public static void initCtx(SSLContext ctx) {
    try {
      char[] passphrase = "password".toCharArray();
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream("testssl.key"), passphrase);
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, passphrase);
      ctx.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static class TcpReader extends Thread {
    public InputStream is;
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
        while (true) {
          int avail = is.available();
          if (avail > 0) {
            int maxRead = transfer.remaining();
            if (avail > maxRead) avail = maxRead;
            byte[] tmp = new byte[avail];
            int read = is.read(tmp);
            if (read == -1) break;
            log("rawread:" + read);
            transfer.put(tmp);
          }
          if (transfer.position() == 0) continue;
          transfer.flip();
          SSLEngineResult res = ssl.unwrap(transfer, input);
          if (res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = ssl.getDelegatedTask()) != null) {
              runnable.run();
            }
          }
          int produced = res.bytesProduced();
          if (produced > 0) {
            byte[] tmp = new byte[produced];
            input.flip();
            if (input.remaining() != produced) {
              throw new Exception("input.remaining() != produced");
            }
            input.get(tmp);
            //TODO : write tmp somewhere (check with client generated data)
            total += produced;
            if (total == 1024) break;
          }
          transfer.compact();
          input.compact();
        }
        log("done");
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  public static class TcpWriter extends Thread {
    public OutputStream os;
    public SSLEngine ssl;
    public boolean client;
    public ByteBuffer output, transfer;
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
        int total = 0;
        while (true) {
          SSLEngineResult res = ssl.wrap(output, transfer);
          if (res.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = ssl.getDelegatedTask()) != null) {
              runnable.run();
            }
          }
          int consumed = res.bytesConsumed();
          int produced = res.bytesProduced();
          if (consumed == 0 && produced == 0) {
            JF.sleep(10);
            continue;
          }
          if (produced > 0) {
            log("rawwrite:" + produced);
            byte[] out = new byte[produced];
            transfer.flip();
            if (transfer.remaining() != produced) {
              throw new Exception("transfer.remaining() != produced");
            }
            transfer.get(out);
            os.write(out);
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
  public static class TcpServer extends Thread {
    public ServerSocket ss;
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public SSLContext ctx;
    public SSLSessionContext sessctx;
    public SSLEngine ssl;
    public void run() {
      try {
        JFLog.log("Server Listening on port 1111");
        ss = new ServerSocket(1111);
        s = ss.accept();
        JFLog.log("Server received client");
        is = s.getInputStream();
        os = s.getOutputStream();
        ctx = SSLContext.getInstance("TLSv1.2");
        initCtx(ctx);
        ssl = ctx.createSSLEngine();
        ssl.setUseClientMode(false);
        sessctx = ctx.getServerSessionContext();
        TcpReader reader = new TcpReader();
        reader.is = is;
        reader.ssl = ssl;
        TcpWriter writer = new TcpWriter();
        writer.os = os;
        writer.ssl = ssl;
        reader.start();
        writer.start();
        reader.join();
        writer.join();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  public static class TcpClient extends Thread {
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public SSLContext ctx;
    public SSLSessionContext sessctx;
    public SSLEngine ssl;
    public void run() {
      try {
        JFLog.log("Client connecting to server");
        s = new Socket("localhost", 1111);
        JFLog.log("Client connected");
        is = s.getInputStream();
        os = s.getOutputStream();
        ctx = SSLContext.getInstance("TLSv1.2");
        ctx.init(null, trustAllCerts, new java.security.SecureRandom());
        ssl = ctx.createSSLEngine("localhost", 1111);
        ssl.setUseClientMode(true);
        sessctx = ctx.getClientSessionContext();
        TcpReader reader = new TcpReader();
        reader.is = is;
        reader.ssl = ssl;
        reader.client = true;
        TcpWriter writer = new TcpWriter();
        writer.os = os;
        writer.ssl = ssl;
        writer.client = true;
        reader.start();
        writer.start();
        reader.join();
        writer.join();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
  public static void testTcp() throws Exception {
    JFLog.log("Starting TCP test");
    TcpServer srv = new TcpServer();
    srv.start();
    JF.sleep(500);
    TcpClient clt = new TcpClient();
    clt.start();
    srv.join();
    clt.join();
  }

  /*** Udp ***/

  public static class UdpHandshake extends Thread {
    public DatagramSocket ds;
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
          HandshakeStatus status = ssl.getHandshakeStatus();
          if (status == HandshakeStatus.NOT_HANDSHAKING) {
            if (client)
              status = HandshakeStatus.NEED_WRAP;
            else
              status = HandshakeStatus.NEED_UNWRAP;
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
              if (res.getHandshakeStatus() == HandshakeStatus.FINISHED) {
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
                dp = new DatagramPacket(out, out.length);
                dp.setSocketAddress(new InetSocketAddress(InetAddress.getByName("localhost"), client ? 1111 : 2222));
                ds.send(dp);
              }
              outtransfer.compact();
              output.compact();
              break;
            case NEED_UNWRAP:
              dp = new DatagramPacket(new byte[maxPackSize], maxPackSize);
              ds.receive(dp);
              int length = dp.getLength();
              if (length > 0) {
                log("rawread:" + length);
                intransfer.put(dp.getData(), 0, length);
              }
              intransfer.flip();
              res = ssl.unwrap(intransfer, input);
              if (res.getHandshakeStatus() == HandshakeStatus.FINISHED) {
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
              if (res.getHandshakeStatus() == HandshakeStatus.FINISHED) {
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

  public static class UdpReader extends Thread {
    public DatagramSocket ds;
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
          dp = new DatagramPacket(new byte[maxPackSize], maxPackSize);
          ds.receive(dp);
          int length = dp.getLength();
          if (length > 0) {
            int maxRead = transfer.remaining();
            if (length > maxRead) length = maxRead;
            log("rawread:" + length);
            transfer.put(dp.getData(), 0, length);
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
  public static class UdpWriter extends Thread {
    public DatagramSocket ds;
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
        byte tmp[] = new byte[1024];
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
            dp = new DatagramPacket(out, out.length);
            dp.setSocketAddress(new InetSocketAddress(InetAddress.getByName("localhost"), client ? 1111 : 2222));
            ds.send(dp);
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
  public static class UdpServer extends Thread {
    public DatagramSocket ds;
    public SSLContext ctx;
    public SSLSessionContext sessctx;
    public SSLEngine ssl;
    public void run() {
      try {
        JFLog.log("Server binding on port 1111");
        ds = new DatagramSocket(1111);
        ctx = SSLContext.getInstance("DTLS");
        initCtx(ctx);
        ssl = ctx.createSSLEngine();
        ssl.setUseClientMode(false);
        sessctx = ctx.getServerSessionContext();
        UdpHandshake handshake = new UdpHandshake();
        handshake.ds = ds;
        handshake.ssl = ssl;
        handshake.client = false;
        handshake.start();
        handshake.join();
        UdpReader reader = new UdpReader();
        reader.ds = ds;
        reader.ssl = ssl;
        UdpWriter writer = new UdpWriter();
        writer.ds = ds;
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
  public static class UdpClient extends Thread {
    public DatagramSocket ds;
    public SSLContext ctx;
    public SSLSessionContext sessctx;
    public SSLEngine ssl;
    public void run() {
      try {
        JFLog.log("Client binding on port 2222");
        ds = new DatagramSocket(2222);
        ctx = SSLContext.getInstance("DTLS");
        ctx.init(null, trustAllCerts, new java.security.SecureRandom());
        ssl = ctx.createSSLEngine("localhost", 2222);
        ssl.setUseClientMode(true);
        sessctx = ctx.getClientSessionContext();
        UdpHandshake handshake = new UdpHandshake();
        handshake.ds = ds;
        handshake.ssl = ssl;
        handshake.client = true;
        handshake.start();
        handshake.join();
        UdpReader reader = new UdpReader();
        reader.ds = ds;
        reader.ssl = ssl;
        reader.client = true;
        UdpWriter writer = new UdpWriter();
        writer.ds = ds;
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
  public static void testUdp() throws Exception {
    JFLog.log("Starting UDP test");
    UdpServer srv = new UdpServer();
    srv.start();
    JF.sleep(500);
    UdpClient clt = new UdpClient();
    clt.start();
    srv.join();
    clt.join();
  }
}

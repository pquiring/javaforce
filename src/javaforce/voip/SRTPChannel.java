package javaforce.voip;

/** Secure RTP
 *
 * @author pquiring
 *
 * Created : Dec ?, 2013
 *
 * RFCs:
 * http://tools.ietf.org/html/rfc3711 - SRTP
 * http://tools.ietf.org/html/rfc4568 - Using SDP to exchange keys for SRTP (old method before DTLS)
 * http://tools.ietf.org/html/rfc5764 - Using DTLS to exchange keys for SRTP
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;

import javaforce.*;

import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.*;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.util.*;
import org.bouncycastle.tls.crypto.impl.bc.*;
import org.bouncycastle.asn1.pkcs.*;

public class SRTPChannel extends RTPChannel {
  public SRTPChannel(RTP rtp, int ssrc, SDP.Stream stream) {
    super(rtp,ssrc,stream);
  }
  private boolean have_keys = false;
  private boolean dtls = false;
  private boolean dtlsServerMode = false;
  private boolean stunReceived = false;
  private boolean dtlsReady = false;
  private SRTPContext srtp_in, srtp_out;
  private int _tailIn, _tailOut;
  private long _seqno = 0;  //must keep track of seqno beyond 16bits

  private DatagramSocket dtlsSocket, rawSocket;
  private DefaultTlsServer2 tlsServer;
  private DefaultTlsClient2 tlsClient;
  private DTLSTransport dtlsTransport;  //DTLS is actually never used to send/receive data (only key exchange)
  private Worker worker;
  private String local_iceufrag, local_icepwd;

  private static DTLSServerProtocol dtlsServer;
  private static DTLSClientProtocol dtlsClient;
  private static InetAddress localhost;
  private static org.bouncycastle.tls.Certificate dtlsCertChain;
  private static AsymmetricKeyParameter dtlsPrivateKey;

  private static final int KEY_LENGTH = 16;
  private static final int SALT_LENGTH = 14;

  private byte[] sharedSecret;
  private byte[] remoteKey = new byte[KEY_LENGTH], remoteSalt = new byte[SALT_LENGTH+2];
  private byte[] localKey = new byte[KEY_LENGTH], localSalt = new byte[SALT_LENGTH+2];

  public void writeRTP(byte[] data, int off, int len) {
    if (rtp.rawMode) {
      super.writeRTP(data, off, len);
      return;
    }
    if (srtp_out == null) {
      if (!have_keys) return;  //not ready
      try {
        srtp_out = new SRTPContext();
        srtp_out.setCrypto("AES_CM_128_HMAC_SHA1_80", localKey, localSalt);
        _tailOut = srtp_out.getAuthTail();
        srtp_out.deriveKeys(0);
      } catch (Exception e) {
        JFLog.log(e);
        return;
      }
    }
    try {
      byte[] payload = Arrays.copyOfRange(data, off+12, off + len);
      int ssrc = BE.getuint32(data, off + 8);
//      int stamp = BE.getuint32(data, off + 4);
      int seqno = BE.getuint16(data, off + 2);
//      if (stream.keyExchange == SDP.KeyExchange.SDP) {
//        srtp_out.deriveKeys(stamp);  //not used in RFC 5764 (RFC 3711:only needed if kdr != 0)
//      }
      _seqno &= 0xffff0000;
      _seqno |= seqno;
      encrypt(payload, ssrc, _seqno++);
      byte[] packet = new byte[len + _tailOut];
      System.arraycopy(data, off, packet, 0, 12);
      System.arraycopy(payload, 0, packet, 12, payload.length);
      appendAuth(packet, srtp_out, seqno);
      super.writeRTP(packet, 0, packet.length);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Sets keys found in SDP used on this side of SRTP (not used in DTLS mode) */
  public void setLocalKeys(byte key[], byte salt[]) {
    System.arraycopy(key, 0, localKey, 0, KEY_LENGTH);
    System.arraycopy(salt, 0, localSalt, 0, SALT_LENGTH);
    have_keys = true;
  }

  /** Sets keys found in SDP used on other side of SRTP (not used in DTLS mode) */
  public void setRemoteKeys(byte key[], byte salt[]) {
    System.arraycopy(key, 0, remoteKey, 0, KEY_LENGTH);
    System.arraycopy(salt, 0, remoteSalt, 0, SALT_LENGTH);
    have_keys = true;
  }

  /** Enables DTLS mode (otherwise you MUST call setServerKeys() AND setClientKeys() before calling start()). */
  public void setDTLS(boolean server, String local_iceufrag, String local_icepwd) {
    dtls = true;
    dtlsServerMode = server;
    this.local_iceufrag = local_iceufrag;
    this.local_icepwd = local_icepwd;
  }

  public boolean start() {
    JFLog.log("SRTPChannel.start:dtls=" + dtls);
    if (!super.start()) return false;
    if (rtp.rawMode) return true;
    if (!dtls) return have_keys;
    //create a thread to do STUN/DTLS requests
    new Thread() {
      public void run() {
        while (rtp.active && !stunReceived) {
          JF.sleep(500);
          Random r = new Random();
          byte[] request = new byte[1500];
          ByteBuffer bb = ByteBuffer.wrap(request);
          bb.order(ByteOrder.BIG_ENDIAN);
          int offset = 0;
          bb.putShort(offset, BINDING_REQUEST);
          offset += 2;
          int lengthOffset = offset;
          bb.putShort(offset, (short)0);  //length (patch later)
          offset += 2;
          long id1;
          id1 = 0x2112a442;  //magic cookie
          id1 <<= 32;
          id1 += Math.abs(r.nextInt());
          bb.putLong(offset, id1);
          offset += 8;
          long id2 = r.nextLong();
          bb.putLong(offset, id2);
          offset += 8;

          String user = stream.sdp.iceufrag + ":" + local_iceufrag;
          int strlen = user.length();
          bb.putShort(offset, USERNAME);
          offset += 2;
          bb.putShort(offset, (short)strlen);
          offset += 2;
          System.arraycopy(user.getBytes(), 0, request, offset, strlen);
          offset += strlen;
          if ((offset & 3) > 0) {
            offset += 4 - (offset & 3);  //padding
          }

          //ICE:PRIORITY (MUST)
          bb.putShort(offset, PRIORITY);
          offset += 2;
          bb.putShort(offset, (short)4);
          offset += 2;
          bb.putInt(offset, Math.abs(r.nextInt()));  //TODO : calc this???
          offset += 4;

          //ICE:ICE_CONTROLLED (MUST)
          bb.putShort(offset, ICE_CONTROLLED);
          offset += 2;
          bb.putShort(offset, (short)8);
          offset += 2;
          bb.putLong(offset, r.nextLong());  //random tie-breaker
          offset += 8;

          bb.putShort(lengthOffset, (short)(offset - 20 + 24));  //patch length (24=MSG_INT)

          byte[] id = STUN.calcMsgIntegrity(request, offset, STUN.calcKey(stream.sdp.icepwd));
          strlen = id.length;
          bb.putShort(offset, MESSAGE_INTEGRITY);
          offset += 2;
          bb.putShort(offset, (short)strlen);
          offset += 2;
          System.arraycopy(id, 0, request, offset, strlen);
          offset += strlen;
          if ((offset & 3) > 0) {
            offset += 4 - (offset & 3);  //padding
          }

          bb.putShort(lengthOffset, (short)(offset - 20 + 8));  //patch length (8=FINGERPRINT)

          //fingerprint
          bb.putShort(offset, FINGERPRINT);
          offset += 2;
          bb.putShort(offset, (short)4);
          offset += 2;
          bb.putInt(offset, STUN.calcFingerprint(request, offset - 4));
          offset += 4;

          bb.putShort(lengthOffset, (short)(offset - 20));  //patch length

          try {
            if (rtp.useTURN) {
              rtp.stun1.sendData(turn1ch, request, 0, offset);
            } else {
              DatagramPacket dp = new DatagramPacket(request, 0, offset, InetAddress.getByName(stream.getIP()), stream.getPort());
              rtp.sock1.send(dp);
            }
          } catch (Exception e) {
            JFLog.log(e);
          }
        }

        //see https://github.com/bcgit/bc-java/tree/master/core/src/test/java/org/bouncycastle/crypto/tls/test

        try {
          localhost = InetAddress.getByName("localhost");
          dtlsSocket = new DatagramSocket(RTP.getnextlocalrtpport());
          JFLog.log("dtlsSocket.port=" + dtlsSocket.getLocalPort());
          rawSocket = new DatagramSocket(RTP.getnextlocalrtpport());
          JFLog.log(" rawSocket.port=" + rawSocket.getLocalPort());
          dtlsSocket.connect(localhost, rawSocket.getLocalPort());
          rawSocket.connect(localhost, dtlsSocket.getLocalPort());
        } catch (Exception e) {
          JFLog.log(e);
          return;
        }

        worker = new Worker();
        worker.start();

        if (!dtlsServerMode) {
          try {
            dtlsClient = new DTLSClientProtocol();
          } catch (Exception e) {
            JFLog.log(e);
            dtlsClient = null;
            return;
          }
          tlsClient = new DefaultTlsClient2() {
            protected TlsSession session;
            public TlsSession getSessionToResume()
            {
              return this.session;
            }

            public ProtocolVersion[] getProtocolVersions() {
              return new ProtocolVersion[] {ProtocolVersion.DTLSv10, ProtocolVersion.DTLSv12};
            }

            public Hashtable getClientExtensions() throws IOException {
              //see : http://bouncy-castle.1462172.n4.nabble.com/DTLS-SRTP-with-bouncycastle-1-49-td4656286.html
              Hashtable table = super.getClientExtensions();
              if (table == null) table = new Hashtable();
              int[] protectionProfiles = {
                SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80  //this is the only one supported for now
  //              SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32
  //              SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32
  //              SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80
              };
              byte[] mki = new byte[0];  //do not use mki
              UseSRTPData srtpData = new UseSRTPData(protectionProfiles, mki);
              TlsSRTPUtils.addUseSRTPExtension(table, srtpData);
              return table;
            }

            public TlsAuthentication getAuthentication() throws IOException {
              return new TlsAuthentication() {
                public void notifyServerCertificate(TlsServerCertificate serverCertificate)
                    throws IOException
                {
                  //info only
                }

                public TlsCredentials getClientCredentials(CertificateRequest certificateRequest)
                    throws IOException
                {
                  short[] certificateTypes = certificateRequest.getCertificateTypes();
                  if (certificateTypes == null) return null;
                  boolean ok = false;
                  for(int a=0;a<certificateTypes.length;a++) {
                    if (certificateTypes[a] == ClientCertificateType.rsa_sign) {
                      ok = true;
                      break;
                    }
                  }
                  if (!ok) return null;

                  SignatureAndHashAlgorithm signatureAndHashAlgorithm = null;
                  Vector sigAlgs = certificateRequest.getSupportedSignatureAlgorithms();
                  if (sigAlgs != null)
                  {
                    for (int i = 0; i < sigAlgs.size(); ++i)
                    {
                      SignatureAndHashAlgorithm sigAlg = (SignatureAndHashAlgorithm) sigAlgs.elementAt(i);
                      if (sigAlg.getSignature() == SignatureAlgorithm.rsa)
                      {
                        signatureAndHashAlgorithm = sigAlg;
                        break;
                      }
                    }

                    if (signatureAndHashAlgorithm == null)
                    {
                      return null;
                    }
                  }


                  BcTlsCrypto crypto = new BcTlsCrypto();
                  TlsSigner signer = null;

                  if (dtlsPrivateKey instanceof RSAKeyParameters)
                  {
                    signer = new BcTlsRSASigner(crypto, (RSAKeyParameters)dtlsPrivateKey, null);  //TODO : public Key?
                  }
                  else if (dtlsPrivateKey instanceof DSAPrivateKeyParameters)
                  {
                    signer = new BcTlsDSASigner(crypto, (DSAPrivateKeyParameters)dtlsPrivateKey);
                  }
                  else if (dtlsPrivateKey instanceof ECPrivateKeyParameters)
                  {
                    signer = new BcTlsECDSASigner(crypto, (ECPrivateKeyParameters)dtlsPrivateKey);
                  }
                  else {
                    //TODO : support other signers?
                    JFLog.log("Unknown private key type:" + dtlsPrivateKey.getClass());
                    return null;
                  }

                  return new DefaultTlsCredentialedSigner(new TlsCryptoParameters(context), signer, dtlsCertChain, signatureAndHashAlgorithm);
                }
              };
            }
            public void notifyHandshakeComplete() throws IOException
            {
              JFLog.log("SRTPChannel:DTLS:Client:Handshake complete");
              super.notifyHandshakeComplete();

              TlsSession newSession = context.getResumableSession();
              if (newSession != null)
              {
/*
                byte[] newSessionID = newSession.getSessionID();
                String hex = Hex.toHexString(newSessionID);

                if (this.session != null && Arrays.areEqual(this.session.getSessionID(), newSessionID))
                {
                  System.out.println("Resumed session: " + hex);
                }
                else
                {
                  System.out.println("Established session: " + hex);
                }
*/
                this.session = newSession;
              }
              getKeys();
            }
          };
          new Thread() {
            public void run() {
              try {
                JFLog.log("SRTPChannel:connecting to DTLS server");
                dtlsTransport = dtlsClient.connect(tlsClient, new UDPTransport(dtlsSocket, 1500 - 20 - 8));
              } catch (Exception e) {
                JFLog.log(e);
              }
            }
          }.start();
        } else {
          try {
            dtlsServer = new DTLSServerProtocol();
          } catch (Exception e) {
            JFLog.log(e);
            dtlsServer = null;
            return;
          }

          try {
            tlsServer = new DefaultTlsServer2() {
              public void notifyClientCertificate(org.bouncycastle.tls.Certificate clientCertificate)
                throws IOException
              {
                TlsCertificate[] chain = clientCertificate.getCertificateList();
    //            JFLog.log("Received client certificate chain of length " + chain.length);
                for (int i = 0; i != chain.length; i++) {
                  TlsCertificate entry = chain[i];
    //              JFLog.log("fingerprint:SHA-256 " + KeyMgmt.fingerprintSHA256(entry.getEncoded()) + " (" + entry.getSubject() + ")");
    //              JFLog.log("cert length=" + entry.getEncoded().length);
                }
              }

            public ProtocolVersion[] getProtocolVersions() {
              return new ProtocolVersion[] {ProtocolVersion.DTLSv10, ProtocolVersion.DTLSv12};
            }

/*
              protected TlsEncryptionCredentials getRSAEncryptionCredentials()
                throws IOException
              {
                return new DefaultTlsEncryptionCredentials(context, dtlsCertChain, dtlsPrivateKey);
              }

              protected TlsSignerCredentials getRSASignerCredentials()
                throws IOException
              {
                SignatureAndHashAlgorithm signatureAndHashAlgorithm = null;
                Vector sigAlgs = supportedSignatureAlgorithms;
                if (sigAlgs != null) {
                  for (int i = 0; i < sigAlgs.size(); ++i) {
                    SignatureAndHashAlgorithm sigAlg = (SignatureAndHashAlgorithm) sigAlgs.elementAt(i);
                    if (sigAlg.getSignature() == SignatureAlgorithm.rsa) {
                      signatureAndHashAlgorithm = sigAlg;
                      break;
                    }
                  }

                  if (signatureAndHashAlgorithm == null) {
                    return null;
                  }
                }
                return new DefaultTlsCredentialedSigner(context, dtlsCertChain, dtlsPrivateKey, signatureAndHashAlgorithm);
              }
*/

              public Hashtable getServerExtensions() throws IOException {
                //see : http://bouncy-castle.1462172.n4.nabble.com/DTLS-SRTP-with-bouncycastle-1-49-td4656286.html
                Hashtable table = super.getServerExtensions();
                if (table == null) table = new Hashtable();
                int[] protectionProfiles = {
    // TODO : need to pick ONE that client offers
                  SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80  //this is the only one supported for now
    //              SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32
    //              SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32
    //              SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80
                };
                byte[] mki = new byte[0];  //should match client or use nothing
                UseSRTPData srtpData = new UseSRTPData(protectionProfiles, mki);
                TlsSRTPUtils.addUseSRTPExtension(table, srtpData);
                return table;
              }
              public void notifyHandshakeComplete() throws IOException
              {
                JFLog.log("SRTPChannel:DTLS:Server:Handshake complete");
                super.notifyHandshakeComplete();
                getKeys();
              }
            };

            new Thread() {
              public void run() {
                try {
                  JFLog.log("SRTPChannel:accepting from DTLS client");
                  dtlsTransport = dtlsServer.accept(tlsServer, new UDPTransport(dtlsSocket, 1500 - 20 - 8));
                } catch (Exception e) {
                  JFLog.log(e);
                }
              }
            }.start();
          } catch (Exception e) {
            JFLog.log(e);
            dtlsSocket = null;
            rawSocket = null;
            return;
          }
        }
        dtlsReady = true;
      }
    }.start();
    return true;
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
        rtp.sock1.send(new DatagramPacket(stun, 0, offset, InetAddress.getByName(stream.getIP()), stream.getPort()));
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static boolean initDTLS(java.util.List<byte []> certChain, byte[] privateKey, boolean pkRSA) {
    try {
      BcTlsCrypto crypto = new BcTlsCrypto();
      org.bouncycastle.asn1.x509.Certificate[] x509certs = new org.bouncycastle.asn1.x509.Certificate[certChain.size()];
      TlsCertificate[] tlscerts = new TlsCertificate[certChain.size()];
      for (int i = 0; i < certChain.size(); ++i) {
        x509certs[i] = org.bouncycastle.asn1.x509.Certificate.getInstance(certChain.get(i));
        tlscerts[i] = new BcTlsCertificate(crypto, x509certs[i]);
      }
      dtlsCertChain = new org.bouncycastle.tls.Certificate(tlscerts);
      if (pkRSA) {
        RSAPrivateKey rsa = RSAPrivateKey.getInstance(privateKey);
        dtlsPrivateKey = new RSAPrivateCrtKeyParameters(rsa.getModulus(), rsa.getPublicExponent(),
          rsa.getPrivateExponent(), rsa.getPrime1(), rsa.getPrime2(), rsa.getExponent1(),
          rsa.getExponent2(), rsa.getCoefficient());
      } else {
        dtlsPrivateKey = PrivateKeyFactory.createKey(privateKey);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  //the sharedSecret is the same on each side

  private class DefaultTlsServer2 extends DefaultTlsServer {
    public TlsCrypto crypto;
    public DefaultTlsServer2() {
      super(new BcTlsCrypto());
      crypto = this.getCrypto();
    }
    public void getKeys() {
      try {
        sharedSecret = context.exportKeyingMaterial(ExporterLabel.dtls_srtp, null, (KEY_LENGTH + SALT_LENGTH) * 2);
        if (sharedSecret == null) throw new Exception("null keys");
      } catch (Exception e) {
        JFLog.log(e);
        return;
      }
//      printArray("keys", sharedSecret, 0, sharedSecret.length);
      int offset = 0;
      System.arraycopy(sharedSecret, offset, remoteKey, 0, KEY_LENGTH);
      offset += KEY_LENGTH;
      System.arraycopy(sharedSecret, offset, localKey, 0, KEY_LENGTH);
      offset += KEY_LENGTH;
      System.arraycopy(sharedSecret, offset, remoteSalt, 0, SALT_LENGTH);
      offset += SALT_LENGTH;
      System.arraycopy(sharedSecret, offset, localSalt, 0, SALT_LENGTH);
      offset += SALT_LENGTH;
      have_keys = true;
    }
  }

  private abstract class DefaultTlsClient2 extends DefaultTlsClient {
    public TlsCrypto crypto;
    public DefaultTlsClient2() {
      super(new BcTlsCrypto());
      crypto = this.getCrypto();
    }
    public void getKeys() {
      try {
        sharedSecret = context.exportKeyingMaterial(ExporterLabel.dtls_srtp, null, (KEY_LENGTH + SALT_LENGTH) * 2);
        if (sharedSecret == null) throw new Exception("null keys");
      } catch (Exception e) {
        JFLog.log(e);
        return;
      }
//      printArray("keys", sharedSecret, 0, sharedSecret.length);
      int offset = 0;
      System.arraycopy(sharedSecret, offset, localKey, 0, KEY_LENGTH);
      offset += KEY_LENGTH;
      System.arraycopy(sharedSecret, offset, remoteKey, 0, KEY_LENGTH);
      offset += KEY_LENGTH;
      System.arraycopy(sharedSecret, offset, localSalt, 0, SALT_LENGTH);
      offset += SALT_LENGTH;
      System.arraycopy(sharedSecret, offset, remoteSalt, 0, SALT_LENGTH);
      offset += SALT_LENGTH;
      have_keys = true;
    }
  }

  protected void processDTLS(byte[] data, int off, int len) {
    if (!dtlsReady) return;  //not ready
    try {
      rawSocket.send(new DatagramPacket(data, off, len, localhost, dtlsSocket.getLocalPort()));
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Transfers DTLS packets from rawSocket to rtpSocket */
  private class Worker extends Thread {
    public void run() {
      try {
        byte data[] = new byte[1500];
        while (active) {
          DatagramPacket pack = new DatagramPacket(data, 1500);
          rawSocket.receive(pack);
          int len = pack.getLength();
          int off = 0;
          if (rtp.useTURN) {
            rtp.stun1.sendData(turn1ch, data, off, len);
          } else {
            rtp.sock1.send(new DatagramPacket(data, off, len, InetAddress.getByName(stream.getIP()), stream.getPort()));
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  protected void processRTP(byte data[], int off, int len) {
    if (rtp.rawMode) {
      rtp.iface.rtpPacket(this, data, off, len);
      return;
    }
    int firstByte = ((int)data[off]) & 0xff;
    //see http://tools.ietf.org/html/rfc5764#section-5
    if (firstByte == 0) {
      //STUN request
      processSTUN(data, off, len);
      return;
    }
    if (firstByte == 1) {
      //STUN response (contents not used)
      stunReceived = true;
      return;
    }
    if (firstByte > 127 && firstByte < 192) {
      //SRTP data
      if (!have_keys) {
        JFLog.log("SRTPChannel:warn:received SRTP data but keys undefined");
        return;
      }
      if (srtp_in == null) {
        try {
          srtp_in = new SRTPContext();
          srtp_in.setCrypto("AES_CM_128_HMAC_SHA1_80", remoteKey, remoteSalt);
          _tailIn = srtp_in.getAuthTail();
          srtp_in.deriveKeys(0);
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
      int seqno = BE.getuint16(data, off + 2);
      int ssrc = BE.getuint32(data, off + 8);
      long index = getIndex(seqno);
      updateCounters(seqno, index);
      if (checkAuth(data, len)) return;
      if (checkForReplay(index)) return;
      byte payload[] = Arrays.copyOfRange(data, off + 12, off + len);
      try {
        decrypt(payload, ssrc, seqno, index);
      } catch (Exception e) {
        JFLog.log(e);
      }
      System.arraycopy(payload, 0, data, off+12, payload.length);
      super.processRTP(data, off, len - _tailIn);
      return;
    }
    //raw DTLS data (handshaking)
    if (dtls) processDTLS(data, off, len);
  }

  private void printArray(String msg, byte data[], int off, int len) {
    StringBuilder sb = new StringBuilder();
    int sum = 0;  //crc kinda
    for(int a=0;a<len;a++) {
      sb.append(",");
      sb.append(Integer.toString(((int)data[off + a]) & 0xff, 16));
      sum += data[off + a];
    }
    JFLog.log(msg + "(" + len + ")=" + sb.toString() + "=" + sum);
  }

  private ByteBuffer getPepper(int ssrc, long idx) {
    //(SSRC * 2^64) XOR (i * 2^16)
    ByteBuffer pepper = ByteBuffer.allocate(16);
    pepper.putInt(4, ssrc);
    long sindex = idx << 16;
    pepper.putLong(8, sindex);

    return pepper;
  }

  private void decrypt(byte[] payload, int ssrc, int seqno, long index) throws GeneralSecurityException {
    ByteBuffer in = ByteBuffer.wrap(payload);
    // aes likes the buffer a multiple of 32 and longer than the input.
    int pl = (((payload.length / 32) + 2) * 32);
    ByteBuffer out = ByteBuffer.allocate(pl);
    ByteBuffer pepper = getPepper(ssrc, index);
    srtp_in.decipher(in, out, pepper);
    System.arraycopy(out.array(), 0, payload, 0, payload.length);
  }

  private void encrypt(byte[] payload, int ssrc, long idx) throws GeneralSecurityException {
    ByteBuffer in = ByteBuffer.wrap(payload);
    int pl = (((payload.length / 32) + 2) * 32);
    ByteBuffer out = ByteBuffer.allocate(pl);
    ByteBuffer pepper = getPepper(ssrc, idx);
    srtp_out.decipher(in, out, pepper);
    System.arraycopy(out.array(), 0, payload, 0, payload.length);
  }

  private void appendAuth(byte[] packet, SRTPContext sc, int seqno) {
    try {
      // strictly we might need to derive the keys here too -
      // since we might be doing auth but no crypt.
      // we don't support that so nach.
      Mac mac = sc.getAuthMac();
      int offs = packet.length - _tailOut;
      ByteBuffer m = ByteBuffer.allocate(offs + 4);
      m.put(packet, 0, offs);
      int oroc = (int) (seqno >>> 16);
      m.putInt(oroc);
      m.position(0);
      mac.update(m);
      byte[] auth = mac.doFinal();
      for (int i = 0; i < _tailOut; i++) {
        packet[offs + i] = auth[i];
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private final static int SRTPWINDOWSIZE = 64;
  private long[] _replay = new long[SRTPWINDOWSIZE];
  private long _windowLeadingEdge = 0;

  private boolean checkForReplay(long _index) {
    if (_index < _windowLeadingEdge) {
      // old packet....
      if ((_windowLeadingEdge - _index) > SRTPWINDOWSIZE) {
        JFLog.log("SRTPChannel:Replay:packet too old");
        return true;  //replay
      }
      // in window but .... is it a replay ?
      int tidx = (int) (_index % SRTPWINDOWSIZE);
      if (_replay[tidx] == _index) {
        JFLog.log("SRTPChannel:Replay:seen that packet");
        return true;  //replay
      }
    }
    return false;
  }

  private boolean checkAuth(byte[] packet, int plen) {
    try {
      srtp_in.deriveKeys(0/*_index*/);

      Mac hmac = srtp_in.getAuthMac();

      int alen = _tailIn;
      int offs = plen - alen;
      ByteBuffer m = ByteBuffer.allocate(offs + 4);
      m.put(packet, 0, offs);
      m.putInt((int) _roc);

      byte[] auth = new byte[alen];
      System.arraycopy(packet, offs, auth, 0, alen);
      int mlen = (plen - 12) - alen;
      m.position(0);
      hmac.update(m);
      byte[] mac = hmac.doFinal();

      for (int i = 0; i < alen; i++) {
        if (auth[i] != mac[i]) {
          throw new Exception("SRTPChannel:not authorized byte " + i + " does not match");
        }
      }
      return false;
    } catch (Exception e) {
      JFLog.log(e);
      return true;
    }
  }

  protected long _roc = 0; // only used for inbound we _know_ the answer for outbound.
  //roc = rollover counter (counts everytime seqno(16bit) rolls over)
  protected int _s_l = 0;  // only used for inbound we _know_ the answer for outbound.
  //s_l = seqno last seen

  long getIndex(int seqno) {
    long v = _roc; // default assumption

    // detect wrap(s)
    int diff = seqno - _s_l; // normally we expect this to be 1
    if (diff < Short.MIN_VALUE) {
        // large negative offset so
        v = _roc + 1; // if the old value is more than 2^15 smaller
        // then we have wrapped
    }
    if (diff > Short.MAX_VALUE) {
        // big positive offset
        v = _roc - 1; // we  wrapped recently and this is an older packet.
    }
    if (v < 0) {
        v = 0; // trap odd initial cases
    }
    /*
    if (_s_l < 32768) {
    v = ((seqno - _s_l) > 32768) ? (_roc - 1) % (1 << 32) : _roc;
    } else {
    v = ((_s_l - 32768) > seqno) ? (_roc + 1) % (1 << 32) : _roc;
    }*/
    long low = (long) seqno;
    long high = ((long) v << 16);
    long ret = low | high;
    return ret;
  }

  void updateCounters(int seqno, long index) {
    //SRTPProtocolImpl
    // note that we have seen it.
    int tidx = (int) (index % SRTPWINDOWSIZE);
    _replay[tidx] = index;
    // and update the leading edge if needed.
    if (index > _windowLeadingEdge) {
        _windowLeadingEdge = index;
    }

    //RTPProtocolImpl
    // note that we have seen it.
    int diff = seqno - _s_l; // normally we expect this to be 1
    if (diff < Short.MIN_VALUE) {
      // large negative offset so
      _roc++; // if the old value is more than 2^15 smaller
      // then we have wrapped
    }
    _s_l = seqno;
  }
}

/*

Data Flowcharts:

RTP Flow (0x80):
media <---> SRTPContext <---> rtpSocket

STUN Flow (0x00 or 0x01):
stun <---> rtpSocket

DTLS Flow (*):
dtlsSocket <---> rawSocket <---> rtpSocket

NOTE:DTLS:caller=client callee=server

*/

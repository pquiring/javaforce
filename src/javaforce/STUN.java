package javaforce;

/** STUN (w/ TURN support) client.
 *
 * Created : Nov 25, 2013
 *
 * See RFCs:
 * http://tools.ietf.org/html/rfc3489 - Classic STUN
 * http://tools.ietf.org/html/rfc5389 - STUN
 * http://tools.ietf.org/html/rfc5766 - TURN
 *
 * I can't stand reading RFCs.  Built this mostly with X-Lite and Wireshark as usual.
 * Thanks to resiprocate open-source project, would have NEVER figured out the HmacSHA1 stuff.
 */

import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class STUN {
  public interface Listener {
    public void stunPublicIP(STUN stun, String ip, int port);
    public void turnAlloc(STUN stun, String ip, int port, byte token[], int lifetime);
    public void turnBind(STUN stun);
    public void turnRefresh(STUN stun, int lifetime);
    public void turnFailed(STUN stun);
    public void turnData(STUN stun, byte data[], int offset, int length, short channel);
  }

  private DatagramSocket ds;
  private InetAddress addr;
  private int StunPort = 3478;

//requests
  private final static short BINDING_REQUEST = 0x0001;
  private final static short ALLOCATE_REQUEST = 0x0003;
  private final static short REFRESH_REQUEST = 0x0004;
  private final static short BIND_REQUEST = 0x0009;

//responses
  private final static short BINDING_RESPONSE = 0x0101;
  private final static short ALLOCATE_RESPONSE = 0x0103;
  private final static short REFRESH_RESPONSE = 0x0104;
  private final static short BIND_RESPONSE = 0x0109;

//attrs
  private final static short MAPPED_ADDRESS = 0x0001;
  private final static short CHANGE_REQUEST = 0x0003;
  private final static short USERNAME = 0x0006;
  private final static short MESSAGE_INTEGRITY = 0x0008;
  private final static short ERROR_CODE = 0x0009;
  private final static short CHANNEL_NUMBER = 0x000c;
  private final static short LIFETIME = 0x000d;
  private final static short XOR_PEER_ADDRESS = 0x0012;
  private final static short REALM = 0x0014;
  private final static short NONCE = 0x0015;
  private final static short XOR_RELAY_ADDRESS = 0x0016;
  private final static short DATA_INDICATION = 0x0017;
  private final static short EVEN_PORT = 0x0018;
  private final static short TRANSPORT_TYPE = 0x0019;
  private final static short XOR_MAPPED_ADDRESS = 0x0020;
  private final static short RESERVATION_TOKEN = 0x0022;

  private long id1, id2;
  private Listener listener;
  private boolean active = true;
  private String user, pass;
  private boolean sentAuth = false;
  private String realm, nonce;
  private boolean evenPort;
  private byte token[];
  private byte fulldata[];
  private ByteBuffer fulldatabb;
  private int relayPort = -1;
  private String relayIP = null;
  private int lifetime = -1;
  private short lastRequest = -1;

  /** Connects to STUN server and starts socket listening thread.
   * @param localport = localport to listen on (-1 = any)
   */
  public boolean start(int localport, String host, String user, String pass, Listener listener) {
    this.listener = listener;
    this.user = user;
    this.pass = pass;
    try {
      int idx = host.indexOf(":");
      if (idx != -1) {
        StunPort = JF.atoi(host.substring(idx+1));
        host = host.substring(0, idx);
      }
      addr = InetAddress.getByName(host);
      if (localport == -1) {
        ds = new DatagramSocket();
      } else {
        ds = new DatagramSocket(localport);
      }
      new Worker().start();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public int getLocalPort() {
    return ds.getLocalPort();
  }

  public String getLocalAddr() {
    return ds.getLocalAddress().getHostAddress();
  }

  public void close() {
    active = false;
    if (ds == null) return;
    try {
      ds.close();
      ds = null;
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void genID() {
    Random r = new Random();
    id1 = 0x2112a442;  //magic cookie
    id1 <<= 32;
    id1 += Math.abs(r.nextInt());
    id2 = r.nextLong();
  }

  /** STUN : Request real Public IP */
  public void requestPublicIP() {
    requestPublicIP(false, false);
  }

  /** STUN : Request real Public IP from a different IP and or Port */
  public void requestPublicIP(boolean change_ip, boolean change_port) {
    try {
      lastRequest = BINDING_REQUEST;
      int packetSize = 20 + 8;
      byte request[] = new byte[packetSize];
      DatagramPacket dp = new DatagramPacket(request, packetSize);
      ByteBuffer bb = ByteBuffer.wrap(request);
      bb.order(ByteOrder.BIG_ENDIAN);
      int offset = 0;
      bb.putShort(offset, BINDING_REQUEST);
      offset += 2;
      bb.putShort(offset, (short)8);  //length
      offset += 2;
      genID();
      bb.putLong(offset, id1);
      offset += 8;
      bb.putLong(offset, id2);
      offset += 8;

      bb.putShort(offset, CHANGE_REQUEST);
      offset += 2;
      bb.putShort(offset, (short)4);  //length
      offset += 2;
      bb.putInt(offset, (change_ip ? 0x04 : 0) + (change_port ? 0x02: 0));  //flags
      offset += 4;

      dp.setAddress(addr);
      dp.setPort(StunPort);
      ds.send(dp);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** TURN : Request a UDP data channel.
   *
   * Token is optional, used if last request was for even port (to alloc the odd port).
   */

  public void requestAlloc(boolean evenPort, byte token[]) {
    this.evenPort = evenPort;
    this.token = token;
    int lengthOffset;
    try {
      lastRequest = ALLOCATE_REQUEST;
      byte request[] = new byte[1024];
      ByteBuffer bb = ByteBuffer.wrap(request);
      bb.order(ByteOrder.BIG_ENDIAN);
      int offset = 0;
      bb.putShort(offset, ALLOCATE_REQUEST);
      offset += 2;
      lengthOffset = offset;
      bb.putShort(offset, (short)0);  //(patch later) length
      offset += 2;
      genID();
      bb.putLong(offset, id1);
      offset += 8;
      bb.putLong(offset, id2);
      offset += 8;

      if (evenPort) {
        bb.putShort(offset, EVEN_PORT);
        offset += 2;
        bb.putShort(offset, (short)1);  //length
        offset += 2;
        bb.put(offset, (byte)0x80);  //reserve_next=0x80
        offset += 1;
        offset += 3;  //padding
      }
      bb.putShort(offset, TRANSPORT_TYPE);
      offset += 2;
      bb.putShort(offset, (short)4);  //length
      offset += 2;
      bb.put(offset, (byte)0x11);  //UDP
      offset += 1;
      offset += 3;  //padding

      if (realm != null && nonce != null) {
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
      }

      if (realm != null) {
        int strlen = realm.length();
        bb.putShort(offset, REALM);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(realm.getBytes(), 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      if (nonce != null) {
        int strlen = nonce.length();
        bb.putShort(offset, NONCE);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(nonce.getBytes(), 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      if (token != null) {
        int strlen = token.length;
        bb.putShort(offset, RESERVATION_TOKEN);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(token, 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      //message integrity
      if (realm != null && nonce != null) {
        //length should include size of message integrity attr (even though it's not filled in yet)
        bb.putShort(lengthOffset, (short)(offset - 20 + 24));  //patch length
        byte id[] = calcMsgIntegrity(request, offset, calcKey(user, realm, pass));
        int strlen = id.length;
        bb.putShort(offset, MESSAGE_INTEGRITY);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(id, 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      bb.putShort(lengthOffset, (short)(offset - 20));  //patch length

      DatagramPacket dp = new DatagramPacket(request, offset);
      dp.setAddress(addr);
      dp.setPort(StunPort);
      ds.send(dp);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** TURN : Bind to host:port (also a keep alive)
   * @param channel : 0x4000 thru 0x7ffe (you pick one by random I guess)
   */
  public void requestBind(short channel, String host, int port) {
    int lengthOffset;
    try {
      lastRequest = BIND_REQUEST;
      byte hostaddr[] = InetAddress.getByName(host).getAddress();
      byte request[] = new byte[1024];
      ByteBuffer bb = ByteBuffer.wrap(request);
      bb.order(ByteOrder.BIG_ENDIAN);
      int offset = 0;
      bb.putShort(offset, BIND_REQUEST);
      offset += 2;
      lengthOffset = offset;
      bb.putShort(offset, (short)0);  //length (patch later)
      offset += 2;
      genID();
      bb.putLong(offset, id1);
      offset += 8;
      bb.putLong(offset, id2);
      offset += 8;

      if (realm != null && nonce != null) {
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
      }

      if (realm != null) {
        int strlen = realm.length();
        bb.putShort(offset, REALM);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(realm.getBytes(), 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      if (nonce != null) {
        int strlen = nonce.length();
        bb.putShort(offset, NONCE);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(nonce.getBytes(), 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      bb.putShort(offset, CHANNEL_NUMBER);
      offset += 2;
      bb.putShort(offset, (short)4);
      offset += 2;
      bb.putShort(offset, channel);
      offset += 2;
      offset += 2;  //reserved???

      bb.putShort(offset, XOR_PEER_ADDRESS);
      offset += 2;
      bb.putShort(offset, (short)8);
      offset += 2;
      offset++;  //reserved
      bb.put(offset, (byte)0x01);  //IP4
      offset++;
      bb.putShort(offset, (short)(port ^ bb.getShort(4)));
      offset += 2;
      for(int a=0;a<4;a++) {
        request[offset++] = (byte)(hostaddr[a] ^ request[4 + a]);
      }

      //message integrity
      if (realm != null && nonce != null) {
        //length should include size of message integrity attr (even though it's not filled in yet)
        bb.putShort(lengthOffset, (short)(offset - 20 + 24));  //patch length
        byte id[] = calcMsgIntegrity(request, offset, calcKey(user, realm, pass));
        int strlen = id.length;
        bb.putShort(offset, MESSAGE_INTEGRITY);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(id, 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      bb.putShort(lengthOffset, (short)(offset - 20));  //patch length

      DatagramPacket dp = new DatagramPacket(request, offset);
      dp.setAddress(addr);
      dp.setPort(StunPort);
      ds.send(dp);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** TURN : Refresh a connection (keep alive)
   */
  public void requestRefresh(int seconds) {
    int lengthOffset;
    try {
      lastRequest = REFRESH_REQUEST;
      byte request[] = new byte[1024];
      ByteBuffer bb = ByteBuffer.wrap(request);
      bb.order(ByteOrder.BIG_ENDIAN);
      int offset = 0;
      bb.putShort(offset, REFRESH_REQUEST);
      offset += 2;
      lengthOffset = offset;
      bb.putShort(offset, (short)0);  //length (patch later)
      offset += 2;
      genID();
      bb.putLong(offset, id1);
      offset += 8;
      bb.putLong(offset, id2);
      offset += 8;

      if (realm != null && nonce != null) {
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
      }

      if (realm != null) {
        int strlen = realm.length();
        bb.putShort(offset, REALM);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(realm.getBytes(), 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      if (nonce != null) {
        int strlen = nonce.length();
        bb.putShort(offset, NONCE);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(nonce.getBytes(), 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      bb.putShort(offset, LIFETIME);
      offset += 2;
      bb.putShort(offset, (short)4);
      offset += 2;
      bb.putInt(offset, seconds);
      offset += 4;

      //message integrity
      if (realm != null && nonce != null) {
        //length should include size of message integrity attr (even though it's not filled in yet)
        bb.putShort(lengthOffset, (short)(offset - 20 + 24));  //patch length
        byte id[] = calcMsgIntegrity(request, offset, calcKey(user, realm, pass));
        int strlen = id.length;
        bb.putShort(offset, MESSAGE_INTEGRITY);
        offset += 2;
        bb.putShort(offset, (short)strlen);
        offset += 2;
        System.arraycopy(id, 0, request, offset, strlen);
        offset += strlen;
        if ((offset & 3) > 0) {
          offset += 4 - (offset & 3);  //padding
        }
      }

      bb.putShort(lengthOffset, (short)(offset - 20));  //patch length

      DatagramPacket dp = new DatagramPacket(request, offset);
      dp.setAddress(addr);
      dp.setPort(StunPort);
      ds.send(dp);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getIP() {return relayIP;}
  public int getPort() {return relayPort;}

  /** TURN : Send out a UDP packet.
   */
  public void sendData(short channel, byte data[], int offset, int length) {
    if (fulldata == null || fulldata.length != length + 4) {
      fulldata = new byte[length + 4];
      fulldatabb = ByteBuffer.wrap(fulldata);
      fulldatabb.order(ByteOrder.BIG_ENDIAN);
    }
    fulldatabb.putShort(0, channel);
    fulldatabb.putShort(2, (short)length);
    System.arraycopy(data, offset, fulldata, 4, length);
    DatagramPacket dp = new DatagramPacket(fulldata, length + 4);
    try {
      dp.setAddress(addr);
      dp.setPort(StunPort);
      ds.send(dp);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //for short-term credentials
  public static byte[] calcKey(String pass) {
    return pass.getBytes();
  }

  //for long-term credentials
  public static byte[] calcKey(String user, String realm, String pass) {
    String msg = user + ":" + realm + ":" + pass;
    MD5 md5 = new MD5();
    md5.init();
    md5.add(msg.getBytes(), 0, msg.length());
    return md5.done();
  }

  public static byte[] calcMsgIntegrity(byte data[], int length, byte key[]) {
    try {
      SecretKeySpec ks = new SecretKeySpec(key, "HmacSHA1");
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(ks);
      return mac.doFinal(Arrays.copyOfRange(data, 0, length));
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  //see http://tools.ietf.org/html/rfc5389#section-15.5
  public static int calcFingerprint(byte data[], int length) {
    CRC32 crc = new CRC32();
    crc.update(data, 0, length);
    return ((int)crc.getValue()) ^ 0x5354554e;
  }

  private class Worker extends Thread {
    public void run() {
      DatagramPacket dp;
      boolean resendAuth;
      int errcode;
      int ip[], port;
      byte response[] = new byte[1500];
      ByteBuffer bb = ByteBuffer.wrap(response);
      bb.order(ByteOrder.BIG_ENDIAN);
      while (active) {
        try {
          resendAuth = false;
          dp = new DatagramPacket(response, 1500);
          ds.receive(dp);
          //TODO : validate packet source
          int packetLength = dp.getLength();
          //decode response
          int offset = 0;
          short code = bb.getShort(0);
          if (code >= 0x4000) {
            //it's TURN data received back
            listener.turnData(STUN.this, response, 4, bb.getShort(2), code);
            continue;
          }
//          JFLog.log("STUN:code=0x" + Integer.toString(code, 16));
          if (code == BIND_RESPONSE) {
            listener.turnBind(STUN.this);
          }
          if (code == DATA_INDICATION) {
            continue;
          }
          offset += 2;
          short length = bb.getShort(offset);
          if (length + 20 != packetLength) {
            throw new Exception("STUN:bad packet:incorrect length");
          }
          offset += 2;
          long _id1 = bb.getLong(offset);
          offset += 8;
          long _id2 = bb.getLong(offset);
          offset += 8;
          if (id1 != _id1 || id2 != _id2) {
            throw new Exception("STUN:bad packet:id mismatch");
          }
          while (offset < packetLength) {
            short attr = bb.getShort(offset);
            offset += 2;
            length = bb.getShort(offset);
            offset += 2;
            switch (attr) {
              case MAPPED_ADDRESS:
                //CLASSIC STUN
                port = ((int)bb.getShort(offset + 2)) & 0xffff;
                ip = new int[4];
                for(int a=0;a<4;a++) {
                  ip[a] = ((int)response[offset + 4 + a]) & 0xff;
                }
                if (code == BINDING_RESPONSE) {
                  listener.stunPublicIP(STUN.this, String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]), port);
                }
                break;
              case XOR_MAPPED_ADDRESS:
                //NEW STUN
                port = (bb.getShort(offset + 2) ^ bb.getShort(4)) & 0xffff;
                ip = new int[4];
                for(int a=0;a<4;a++) {
                  ip[a] = (response[offset + 4 + a] ^ response[4 + a]) & 0xff;
                }
                if (code == BINDING_RESPONSE) {
                  listener.stunPublicIP(STUN.this, String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]), port);
                }
                break;
              case REALM:
                realm = new String(response, offset, length);
                JFLog.log("STUN:realm=" + realm);
                break;
              case NONCE:
                nonce = new String(response, offset, length);
                JFLog.log("STUN:nonce=" + nonce);
                break;
              case ERROR_CODE:
                errcode = bb.getShort(offset + 2);
                switch (errcode) {
                  case 0x401:
                    if (sentAuth) {
                      listener.turnFailed(STUN.this);
                      JFLog.log("STUN:Error:" + Integer.toString(errcode, 16) + " (Bad Auth)");
                    } else {
                      resendAuth = true;
                    }
                    break;
                  default:
                    JFLog.log("STUN:Error:" + Integer.toString(errcode, 16));
                    break;
                }
                break;
              case XOR_RELAY_ADDRESS:
                relayPort = (bb.getShort(offset + 2) ^ bb.getShort(4)) & 0xffff;
                ip = new int[4];
                for(int a=0;a<4;a++) {
                  ip[a] = (response[offset + 4 + a] ^ response[4 + a]) & 0xff;
                }
                relayIP = String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
                if (relayIP.equals("0.0.0.0")) {
                  //use turn host address
                  byte ip4[] = addr.getAddress();
                  relayIP = String.format("%d.%d.%d.%d", ip4[0], ip4[1], ip4[2], ip4[3]);;
                }
                break;
              case RESERVATION_TOKEN:
                token = new byte[length];
                System.arraycopy(response, offset, token, 0, length);
                break;
              case LIFETIME:
                lifetime = bb.getInt(offset);
                break;
            }
            offset += length;
            if ((length & 0x3) > 0) {
              offset += 4 - (length & 0x3);  //padding
            }
          }
          if (resendAuth) {
            if (lastRequest == ALLOCATE_REQUEST) {
              //resend alloc request with auth
              sentAuth = true;
              requestAlloc(evenPort, token);
            }
          }
          if (code == ALLOCATE_RESPONSE) {
            listener.turnAlloc(STUN.this, relayIP, relayPort, token, lifetime);
            token = null;
          }
          if (code == REFRESH_RESPONSE) {
            listener.turnRefresh(STUN.this, lifetime);
          }
        } catch (Exception e) {
          if (active) JFLog.log(e);
        }
      }
    }
  }

  public enum NAT {Unknown, None, FullCone, RestrictedCone, RestrictedPort, SymmetricFirewall, SymmetricNAT};

  public static class Test implements Listener {
    private volatile String ip;
    private volatile int port;
    private volatile boolean ok;
    public NAT run(int localport, String host1, String host2) {
      boolean t1, t2, t3, t1b;
      STUN stun = new STUN();
      stun.start(localport, host1, null, null, this);
      ok = false;
      stun.requestPublicIP(false, false);
      JF.sleep(1000);
      if (!ok) {
        t1 = false;
        JFLog.log("STUN:Test I:Failed");
      } else {
        t1 = true;
        JFLog.log("STUN:Test I:IP=" + ip + ":" + port);
      }
      ok = false;
      stun.requestPublicIP(true, true);
      JF.sleep(1000);
      if (!ok) {
        t2 = false;
        JFLog.log("STUN:Test II:Failed");
      } else {
        t2 = true;
        JFLog.log("STUN:Test II:IP=" + ip + ":" + port);
      }
      ok = false;
      stun.requestPublicIP(false, true);
      JF.sleep(1000);
      if (!ok) {
        t3 = false;
        JFLog.log("STUN:Test III:Failed");
      } else {
        t3 = true;
        JFLog.log("STUN:Test III:IP=" + ip + ":" + port);
      }
      String localIP = stun.getLocalAddr();
      stun.close();
      String ip1 = ip;
      int port1 = port;
      if (host2 != null) {
        stun = new STUN();
        stun.start(localport, host2, null, null, this);
        ok = false;
        stun.requestPublicIP(false, false);
        JF.sleep(1000);
        if (!ok) {
          t1b = false;
          JFLog.log("STUN:Test I(Server #2):Failed");
        } else {
          t1b = true;
          JFLog.log("STUN:Test I(Server #2):IP=" + ip + ":" + port);
        }
        stun.close();
      } else {
        t1b = false;
      }
      JFLog.log("STUN:Tests Complete");
      if (!t1) return NAT.Unknown;
      if (localIP.equals(ip1)) {
        //no NAT
        if (t2)
          return NAT.None;
        else
          return NAT.SymmetricFirewall;
      }
      if (t2) return NAT.FullCone;
      if (t1b) {
        if (!ip1.equals(ip) || port1 != port) {
          return NAT.SymmetricNAT;
        }
      } else {
        JFLog.log("STUN:Test:Warning:2nd STUN server failed or skipped, Symmetric NAT test undetermined.");
      }
      if (t3)
        return NAT.RestrictedCone;
      else
        return NAT.RestrictedPort;
    }
    public void stunPublicIP(STUN stun, String ip, int port) {
      this.ip = ip;
      this.port = port;
      ok = true;
    };
    public void turnAlloc(STUN stun, String ip, int port, byte token[], int lifetime) {};
    public void turnBind(STUN stun) {};
    public void turnRefresh(STUN stun, int lifetime) {};
    public void turnFailed(STUN stun) {};
    public void turnData(STUN stun, byte data[], int offset, int length, short channel) {};
  }

  /** Performs a quick test to determine your firewall type. */
  public static NAT doTest(int port, String host1, String host2) {
    return new Test().run(port, host1, host2);
  }
  public static void main(String args[]) {
    if (args.length < 2) {
      System.out.println("Desc: Determine your Firewall NAT type.");
      System.out.println("Usage: javaforce.STUN port server1 [server2]");
      System.out.println("Two servers are recommended to detect Symmetric router.");
    } else {
      int port = (int)Integer.valueOf(args[0]);
      String s1 = args[1];
      String s2;
      if (args.length > 2) s2 = args[2]; else s2 = null;
      System.out.println("Result=" + new Test().run(port, s1, s2));
    }
  }
}

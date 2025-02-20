package javaforce;

/** MQTT client
 *
 * https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html
 *
 * @author peter.quiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import static javaforce.service.MQTTServer.*;

public class MQTT {
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private MQTTEvents events;
  private Worker worker;
  private long last_packet;
  private int ver = 5;

  private static boolean debug = false;
  private static boolean debug_msg = false;

  //wildcards
  public static final String wildcard_single_level = "+";
  public static final String wildcard_multi_level = "#";
  public static final char wildcard_single_level_char = '+';
  public static final char wildcard_multi_level_char = '#';

  /** Sets protocol version.
   * Must be set before connect()
   *
   * Version 3 = MQTT/3.1  (not supported)
   * Version 4 = MQTT/3.1.1
   * Version 5 = MQTT/5.0
   *
   * @param ver = 4 or 5 (default = 5)
   *
   */
  public void setVersion(int ver) {
    if (s != null) return;
    if (ver < 4 || ver > 5) return;
    this.ver = ver;
  }

  /** Connects to MQTT service port. */
  public boolean connect(String host) {
    return connect(host, 1883);
  }

  /** Connects to MQTT service port. */
  public boolean connect(String host, int port) {
    return connect(host, port, null);
  }

  /** Connects to MQTT service port over TLS. */
  public boolean connect(String host, KeyMgmt keys) {
    return connect(host, 8883, keys);
  }

  /** Connects to MQTT service port over TLS. */
  public boolean connect(String host, int port, KeyMgmt keys) {
    disconnect();
    try {
      s = new Socket(host, port);
      if (keys != null) {
        s = JF.connectSSL(s, keys);  //upgrade to SSL
      }
      is = s.getInputStream();
      os = s.getOutputStream();
      worker = new Worker(s);
      worker.start();
      last_packet = System.currentTimeMillis();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Disconnects from MQTT service port. */
  public void disconnect() {
    if (debug) {
      JFLog.log("disconnect");
    }
    if (s != null) {
      //cmd, length, reason code(0)
      byte[] packet = new byte[3];
      packet[0] = (byte)(CMD_DISCONNECT << 4);
      packet[1] = 1;  //packet length
      packet[2] = 0;  //disconnect reason code
      try { os.write(packet); } catch (Exception e) {}
      try { s.close(); } catch (Exception e) {}
      s = null;
      os = null;
      is = null;
    }
    if (worker != null) {
      worker.cancel();
      worker = null;
    }
  }

  public void setListener(MQTTEvents events) {
    this.events = events;
  }

  /** Get last packet received time stamp. */
  public long getLastPacketTimestamp() {
    return last_packet;
  }

  private int calcPacketLength(boolean has_id, int topic_length, boolean has_opts, int msg_length) {
    //does NOT include the header or length itself
    int length = 0;
    if (has_id) {
      length += 2;  //id
    }
    if (topic_length > 0) {
      length += 2;  //short length;
      length += topic_length;
    }
    if (has_opts) {
      length++;  //sub : topic options
    }
    if (ver == 5) {
      length++;  //properties
    }
    if (msg_length > 0) {
      length += msg_length;
    }
    return length;
  }

  /** Send MQTT CONNECT command. */
  public void connect() {
    int length = 16 + 8;
    if (ver == 5) length++;  //properties
    byte[] packet = new byte[length];
    int pos = 0;
    packet[pos++] = (byte)(CMD_CONNECT << 4);
    packet[pos++] = 15 + 8;  //packet length
    packet[pos++] = 0;
    packet[pos++] = 4;  //string length (short)
    packet[pos++] = 'M';
    packet[pos++] = 'Q';
    packet[pos++] = 'T';
    packet[pos++] = 'T';
    packet[pos++] = 5;  //protocol version
    packet[pos++] = (byte)(FLAG_CLEAN_START);  //connect flags
    packet[pos++] = 0;
    packet[pos++] = 120;  //keep alive interval (2 mins)
    if (ver == 5) {
      packet[pos++] = 0;  //properties length
    }
    packet[pos++] = 0;
    packet[pos++] = 2 + 8;  //client id length (short)
    packet[pos++] = 'J';  //client id
    packet[pos++] = 'F';
    Random r = new Random();
    String hex = Integer.toString(r.nextInt(0x7fffffff) | 0x10000000, 16);
    System.arraycopy(hex.getBytes(), 0, packet, pos, 8);
    pos += 8;
    try {
      os.write(packet);
    } catch (Exception e) {
      disconnect();
      JFLog.log(e);
    }
  }

  /** Send MQTT CONNECT command with username/password. */
  public void connect(String user, String pass) {
    byte[] user_bytes = user.getBytes();
    int user_length = user_bytes.length;
    byte[] pass_bytes = pass.getBytes();
    int pass_length = pass_bytes.length;
    int packet_length = 14 + 8 + 2 + user_length + 2 + pass_length;
    if (ver == 5) packet_length++;  //properties
    int length_bytes = getLengthBytes(packet_length);
    byte[] packet = new byte[1 + length_bytes + packet_length];
    int pos = 0;
    packet[pos++] = (byte)(CMD_CONNECT << 4);
    setPacketLength(packet, length_bytes);
    pos += length_bytes;
    packet[pos++] = 0;
    packet[pos++] = 4;  //string length (short)
    packet[pos++] = 'M';
    packet[pos++] = 'Q';
    packet[pos++] = 'T';
    packet[pos++] = 'T';
    packet[pos++] = 5;  //protocol version
    packet[pos++] = (byte)(FLAG_CLEAN_START + FLAG_USER + FLAG_PASS);  //connect flags
    packet[pos++] = 0;
    packet[pos++] = 120;  //keep alive interval (2 mins)
    if (ver == 5) {
      packet[pos++] = 0;  //properties length
    }
    packet[pos++] = 0;
    packet[pos++] = 2 + 8;  //client id length (short)
    packet[pos++] = 'J';  //client id
    packet[pos++] = 'F';
    Random r = new Random();
    String hex = Integer.toString(r.nextInt(0x7fffffff) | 0x10000000, 16);
    System.arraycopy(hex.getBytes(), 0, packet, pos, 8);
    pos += 8;
    setStringLength(packet, pos, (short)user_length);
    pos += 2;
    setString(packet, pos, user_bytes, user_length);
    pos += user_length;
    setStringLength(packet, pos, (short)pass_length);
    pos += 2;
    setString(packet, pos, pass_bytes, pass_length);
    pos += pass_length;
    try {
      os.write(packet);
    } catch (Exception e) {
      disconnect();
      JFLog.log(e);
    }
  }

  public boolean isConnected() {
    if (worker == null) return false;
    return worker.active;
  }

  private short id = 0x0001;

  /** Send MQTT PUBLISH command. */
  public void publish(String topic, String msg, byte qos) {
    byte[] topic_bytes = topic.getBytes();
    int topic_length = topic_bytes.length;
    byte[] msg_bytes = msg.getBytes();
    int msg_length = msg_bytes.length;
    int length = calcPacketLength(qos > 0, topic_length, false, msg_length);
    int length_bytes = getLengthBytes(length);
    byte[] packet = new byte[1 + length_bytes + length];
    packet[0] = (byte)((CMD_PUBLISH << 4) | qos << 1);
    setPacketLength(packet, length_bytes);
    int pos = 1 + length_bytes;
    setStringLength(packet, pos, (short)topic_length);
    pos += 2;
    System.arraycopy(topic_bytes, 0, packet, pos, topic_length);
    pos += topic_length;
    if (qos > 0) {
      setPacketID(packet, pos, id++);
      if (id == 0x7fff) {
        id = 1;
      }
      pos += 2;
    }
    if (ver == 5) {
      pos++;  //properties length
    }
    System.arraycopy(msg_bytes, 0, packet, pos, msg_length);
    pos += msg_length;
    if (debug) {
      JFLog.log("publish:" + topic + "=" + msg);
    }
    try {
      os.write(packet);
    } catch (Exception e) {
      disconnect();
      JFLog.log(e);
    }
  }

  /** Send MQTT PUBLISH command. (QOS = 0) */
  public void publish(String topic, String msg) {
    publish(topic, msg, QOS_0);
  }

  /** Send MQTT SUBSCRIBE command. */
  public void subscribe(String topic) {
    byte[] topic_bytes = topic.getBytes();
    int topic_length = topic_bytes.length;
    int length = calcPacketLength(true, topic_length, true, 0);
    int length_bytes = getLengthBytes(length);
    byte[] packet = new byte[1 + length_bytes + length];
    packet[0] = (byte)(CMD_SUBSCRIBE << 4) + RESERVED_2;
    setPacketLength(packet, length_bytes);
    int pos = 1 + length_bytes;
    setPacketID(packet, pos, id++);
    if (id == 0x7fff) {
      id = 1;
    }
    pos += 2;
    if (ver == 5) {
      pos++;  //properties length
    }
    setStringLength(packet, pos, (short)topic_length);
    pos += 2;
    System.arraycopy(topic_bytes, 0, packet, pos, topic_length);
    pos += topic_length;
    pos++;  //subscribe options
    if (debug) {
      JFLog.log("subscribe:" + topic);
    }
    try {
      os.write(packet);
    } catch (Exception e) {
      disconnect();
      JFLog.log(e);
    }
  }

  /** Send MQTT UNSUBSCRIBE command. */
  public void unsubscribe(String topic) {
    byte[] topic_bytes = topic.getBytes();
    int topic_length = topic_bytes.length;
    int length = calcPacketLength(true, topic_length, false, 0);
    int length_bytes = getLengthBytes(length);
    byte[] packet = new byte[1 + length_bytes + length];
    packet[0] = (byte)(CMD_UNSUBSCRIBE << 4) + RESERVED_2;
    setPacketLength(packet, length_bytes);
    int pos = 1 + length_bytes;
    setPacketID(packet, pos, id++);
    if (id == 0x7fff) {
      id = 1;
    }
    pos += 2;
    if (ver == 5) {
      pos++;  //properties length
    }
    setStringLength(packet, pos, (short)topic_length);
    pos += 2;
    System.arraycopy(topic_bytes, 0, packet, pos, topic_length);
    pos += topic_length;
    if (debug) {
      JFLog.log("unsubscribe:" + topic);
    }
    try {
      os.write(packet);
    } catch (Exception e) {
      disconnect();
      JFLog.log(e);
    }
  }

  public void ping() {
    byte[] packet = new byte[2];
    packet[0] = (byte)(CMD_PING << 4);
    packet[1] = 0;  //packet length
    try {
      os.write(packet);
    } catch (Exception e) {
      disconnect();
      JFLog.log(e);
    }
  }

  private static int bufsiz = 4096;

  private int getLength(byte[] data, int pos, int length) {
    int multi = 1;
    int value = 0;
    int next;
    do {
      if (pos >= length) return -1;
      next = data[pos++] & 0xff;
      value += (next & 0x7f) * multi;
      multi *= 0x80;
    } while (next >= 0x80);
    return value;
  }

  private int getStringLength(byte[] data, int topicPosition) {
    return BE.getuint16(data, topicPosition);
  }

  private String getString(byte[] data, int offset, int length) {
    return new String(data, offset, length);
  }

  private short getPacketID(byte[] data, int idPosition) {
    return (short)BE.getuint16(data, idPosition);
  }

  private void setPacketLength(byte[] packet, int length_bytes) {
    int value = packet.length - 1 - length_bytes;
    int pos = 1;
    byte ebyte;
    do {
      ebyte = (byte)(value % 0x80);
      value /= 0x80;
      if (value > 0) {
        ebyte |= 0x80;
      }
      packet[pos++] = ebyte;
    } while (value > 0);
  }

  private void setPacketID(byte[] data, int offset, short id) {
    BE.setuint16(data, offset, id);
  }

  private void setStringLength(byte[] data, int offset, short length) {
    BE.setuint16(data, offset, length);
  }

  private void setString(byte[] data, int offset, byte[] str, int length) {
    System.arraycopy(str, 0, data, offset, length);
  }

  private int getLengthBytes(int length) {
    //does not include the header or length bytes itself
    if (length <= 0x7f) return 1;
    if (length <= 0x3ff) return 2;
    if (length <= 0x1fffff) return 3;
    if (length <= 0xfffffff) return 4;
    return -1;
  }

  private class Worker extends Thread {
    private boolean active = true;
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public String ip;
    public Worker(Socket s) {
      this.s = s;
    }
    public void run() {
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        ip = s.getInetAddress().getHostAddress();
        if (debug) JFLog.log("connect:" + ip);
        byte[] buf = new byte[bufsiz];
        while (active) {
          int totalRead = 0;
          int packetLength = -1;  //excluding header + length fields
          int totalLength = -1;  //total packet length
          int read;
          Arrays.fill(buf, (byte)0);
          while (active) {
            if (packetLength == -1) {
              read = is.read(buf, totalRead, 1);
            } else {
              read = is.read(buf, totalRead, totalLength - totalRead);
            }
            if (debug) JFLog.log("read=" + read);
            if (read == -1) throw new Exception("bad read");
            totalRead += read;
            if (totalRead < 2) continue;
            if (packetLength == -1) {
              packetLength = getLength(buf, 1, totalRead);
              if (packetLength != -1) {
                totalLength = 1 + getLengthBytes(packetLength) + packetLength;
                if (debug) JFLog.log("totalLength=" + totalLength);
              }
            }
            if (packetLength == -1) continue;
            if (totalRead < totalLength) continue;
            last_packet = System.currentTimeMillis();
            try {
              process(buf, totalLength, packetLength);
            } catch (Exception e) {
              JFLog.log(e);
            }
            break;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
        active = false;
      }
      if (debug) JFLog.log("disconnect:" + ip);
    }
    private void process(byte[] packet, int totalLength, int packetLength) throws Exception {
      byte[] reply = null;
      byte cmd = (byte)((packet[0] & 0xf0) >> 4);
      short id = 0;
      int pos = 1 + getLengthBytes(packetLength);
      int topicLength;
      String topic_name;
      int msgLength;
      String msg;
      if (debug) JFLog.log("cmd=" + cmd);
      switch (cmd) {
        case CMD_CONNECT_ACK:
          if (debug_msg) JFLog.log("connect_ack");
          if (events != null) {
            events.onConnect();
          }
          break;
        case CMD_PUBLISH: {
          boolean dup = (packet[0] & 0x08) != 0;
          byte qos = (byte)((packet[0] & 0x06) >> 1);
          boolean retain = (packet[0] & 0x01) != 0;
          topicLength = getStringLength(packet, pos);
          if (debug) JFLog.log("topic=" + pos + "/" + topicLength);
          pos += 2;
          topic_name = getString(packet, pos, topicLength);
          pos += topicLength;
          if (qos > 0) {
            id = getPacketID(packet, pos);
            if (debug) JFLog.log("id=" + id);
            pos += 2;
          }
          if (ver == 5) {
            int props_length = getLength(packet, pos, totalLength);
            if (props_length == -1) throw new Exception("malformed packet");
            int props_length_bytes = getLengthBytes(props_length);
            pos += props_length_bytes;
            if (props_length > 0) {
              pos += props_length;
            }
          }
          msgLength = totalLength - pos;
          if (debug) JFLog.log("msg=" + pos + "/" + msgLength);
          msg = new String(packet, pos, msgLength);
          if (debug_msg) JFLog.log("PUBLISH:" + ip + ":" + topic_name + ":" + msg);
          switch (qos) {
            case QOS_1: {
              //CMD_PUBLISH_ACK
              reply = new byte[4];
              reply[0] = (byte)(CMD_PUBLISH_ACK << 4);
              //reply = header , size , id_hi, id_lo
              setPacketLength(reply, 1);
              setPacketID(reply, 2, id);
              break;
            }
            case QOS_2: {
              //CMD_PUBLISH_REC
              reply = new byte[4];
              reply[0] = (byte)(CMD_PUBLISH_REC << 4);
              //reply = header , size , id_hi, id_lo
              setPacketLength(reply, 1);
              setPacketID(reply, 2, id);
              break;
            }
          }
          if (events != null) {
            events.onMessage(topic_name, msg);
          }
          break;
        }
        case CMD_PUBLISH_ACK:
          //???
          break;
        case CMD_PUBLISH_REC:
          reply = new byte[4];
          reply[0] = (byte)(CMD_PUBLISH_REL << 4) + RESERVED_2;
          setPacketLength(reply, 1);
          id = getPacketID(packet, 2);
          setPacketID(reply, 2, id);
          break;
        case CMD_PUBLISH_REL:
          //???
          break;
        case CMD_PUBLISH_CMP:
          //???
          break;
        case CMD_SUBSCRIBE_ACK:
          //TODO : decode topic ?
          if (debug_msg) JFLog.log("subscribe_ack");
          if (events != null) {
            events.onSubscribe(null);
          }
          break;
        case CMD_PING:
          reply = new byte[2];
          reply[0] = (byte)(CMD_PONG << 4);
//          setPacketLength(reply);  //zero
          if (debug_msg) JFLog.log("PING:" + ip);
          break;
        case CMD_PONG:
          break;
        case CMD_DISCONNECT:
          active = false;
          break;
      }
      if (reply != null) {
        send(reply);
      }
    }
    private void send(byte[] reply) throws Exception {
      os.write(reply);
    }
    public void cancel() {
      active = false;
      if (s != null) {
        try { s.close(); } catch (Exception e) {}
        s = null;
      }
    }
  }

  private static String resub = null;

  private static void usage() {
    System.out.println("Usage:MQTT server [-u user] [-p pass] [publish topic msg]");
    System.out.println("Usage:MQTT server [-u user] [-p pass] [subscribe topic]");
  }

  private static class TestEvents implements MQTTEvents {
    public void onConnect() {
      JFLog.log("connected");
    }
    public void onSubscribe(String topic) {}
    public void onMessage(String topic, String msg) {
      JFLog.log("msg:" + topic + "=" + msg);
      resub = topic;
    }
    public void onPing() {
      JFLog.log("ping");
    }
    public void onPong() {
      JFLog.log("pong");
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      usage();
      return;
    }
    try {
      MQTT client = new MQTT();
      client.setListener(new TestEvents());
      String host = args[0];
      String cmd = null;
      String topic = null;
      String msg = null;
      String user = null;
      String pass = null;
      for(int idx = 1;idx < args.length;idx++) {
        String arg = args[idx];
        if (arg.equals("-u")) {
          user = args[idx + 1];
          idx++;
          continue;
        }
        if (arg.equals("-p")) {
          pass = args[idx + 1];
          idx++;
          continue;
        }
        if (arg.equals("-d")) {
//          MQTT.debug = true;  //too verbose
          MQTT.debug_msg = true;
          continue;
        }
        if (cmd == null) {
          cmd = arg;
          continue;
        }
        if (topic == null) {
          topic = arg;
          continue;
        }
        if (msg == null) {
          msg = arg;
          continue;
        }
        JFLog.log("Unknown arg:" + arg);
      }
      if (cmd == null || topic == null) {
        usage();
        return;
      }
      client.connect(host);
      if (user != null && pass != null) {
        client.connect(user, pass);
      } else {
        client.connect();
      }
      switch (cmd) {
        case "publish":
          if (msg == null) {
            usage();
            return;
          }
          client.publish(topic, msg);
          break;
        case "subscribe":
          client.subscribe(topic);
          break;
      }
      while (client.isConnected()) {
        for(int a=0;a<60;a++) {
          if (resub != null) {
            client.subscribe(resub);
            resub = null;
          }
          JF.sleep(1000);
        }
        client.ping();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

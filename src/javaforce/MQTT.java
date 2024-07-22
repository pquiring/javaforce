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
  public static boolean debug;
  public static boolean debug_msg;

  public boolean connect(String host) {
    return connect(host, 1883);
  }

  public boolean connect(String host, int port) {
    disconnect();
    try {
      s = new Socket(host, port);
      is = s.getInputStream();
      os = s.getOutputStream();
      worker = new Worker(s);
      worker.start();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

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
    length++;  //properties
    if (msg_length > 0) {
      length += msg_length;
    }
    return length;
  }

  public void connect() {
    byte[] packet = new byte[17 + 8];
    packet[0] = (byte)(CMD_CONNECT << 4);
    packet[1] = 15 + 8;  //packet length
    packet[2] = 0;
    packet[3] = 4;  //string length (short)
    packet[4] = 'M';
    packet[5] = 'Q';
    packet[6] = 'T';
    packet[7] = 'T';
    packet[8] = 5;  //protocol version
    packet[9] = (byte)(FLAG_CLEAN_START);  //connect flags
    packet[10] = 0;
    packet[11] = 120;  //keep alive interval (2 mins)
    packet[12] = 0;  //properties length
    packet[13] = 0;
    packet[14] = 2 + 8;  //client id length (short)
    packet[15] = 'J';  //client id
    packet[16] = 'F';
    Random r = new Random();
    String hex = Integer.toString(r.nextInt(0x7fffffff) | 0x10000000, 16);
    System.arraycopy(hex.getBytes(), 0, packet, 17, 8);
    try {
      os.write(packet);
    } catch (Exception e) {
      disconnect();
      JFLog.log(e);
    }
  }

  public void connect(String user, String pass) {
    byte[] user_bytes = user.getBytes();
    int user_length = user_bytes.length;
    byte[] pass_bytes = pass.getBytes();
    int pass_length = pass_bytes.length;
    int packet_length = 15 + 8 + 2 + user_length + 2 + pass_length;
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
    packet[pos++] = 0;  //properties length
    packet[pos++] = 0;
    packet[pos++] = 2 + 8;  //client id length (short)
    packet[pos++] = 'J';  //client id
    packet[pos++] = 'F';
    Random r = new Random();
    String hex = Integer.toString(r.nextInt(0x7fffffff) | 0x10000000, 16);
    System.arraycopy(hex.getBytes(), 0, packet, 17, 8);
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

  public void publish(String topic, String msg) {
    byte[] topic_bytes = topic.getBytes();
    int topic_length = topic_bytes.length;
    byte[] msg_bytes = msg.getBytes();
    int msg_length = msg_bytes.length;
    int length = calcPacketLength(true, topic_length, false, msg_length);
    int length_bytes = getLengthBytes(length);
    byte[] packet = new byte[1 + length_bytes + length];
    packet[0] = (byte)((CMD_PUBLISH << 4) | QOS_1 << 1);
    setPacketLength(packet, length_bytes);
    int pos = 1 + length_bytes;
    setStringLength(packet, pos, (short)topic_length);
    pos += 2;
    System.arraycopy(topic_bytes, 0, packet, pos, topic_length);
    pos += topic_length;
    setPacketID(packet, pos, id++);
    if (id == 0x7fff) {
      id = 1;
    }
    pos += 2;
    pos++;  //properties length
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

  public void subscribe(String topic) {
    byte[] topic_bytes = topic.getBytes();
    int topic_length = topic_bytes.length;
    int length = calcPacketLength(true, topic_length, true, 0);
    int length_bytes = getLengthBytes(length);
    byte[] packet = new byte[1 + length_bytes + length];
    packet[0] = (byte)(CMD_SUBSCRIBE << 4);
    setPacketLength(packet, length_bytes);
    int pos = 1 + length_bytes;
    setPacketID(packet, pos, id++);
    pos += 2;
    pos++;  //properties length
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

  public void unsubscribe(String topic) {
    byte[] topic_bytes = topic.getBytes();
    int topic_length = topic_bytes.length;
    int length = calcPacketLength(true, topic_length, false, 0);
    int length_bytes = getLengthBytes(length);
    byte[] packet = new byte[1 + length_bytes + length];
    packet[0] = (byte)(CMD_UNSUBSCRIBE << 4);
    setPacketLength(packet, length_bytes);
    int pos = 1 + length_bytes;
    setPacketID(packet, pos, id++);
    pos += 2;
    pos++;  //properties length
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
      int pos;
      int topicLength;
      String topic_name;
      int msgLength;
      String msg;
      if (debug) JFLog.log("cmd=" + cmd);
      switch (cmd) {
        case CMD_CONNECT_ACK:
          if (debug_msg) JFLog.log("connect_ack");
          break;
        case CMD_PUBLISH: {
          boolean dup = (packet[0] & 0x08) != 0;
          byte qos = (byte)((packet[0] & 0x06) >> 1);
          boolean retain = (packet[0] & 0x01) != 0;
          pos = 1 + getLengthBytes(packetLength);
          topicLength = getStringLength(packet, pos);
          if (debug) JFLog.log("topic=" + pos + "/" + topicLength);
          pos += 2;
          topic_name = new String(packet, pos, topicLength);
          pos += topicLength;
          if (qos > 0) {
            id = getPacketID(packet, pos);
            if (debug) JFLog.log("id=" + id);
            pos += 2;
          }
          int props_length = getLength(packet, pos, totalLength);
          if (props_length == -1) throw new Exception("malformed packet");
          int props_length_bytes = getLengthBytes(props_length);
          pos += props_length_bytes;
          if (props_length > 0) {
            pos += props_length_bytes;
          }
          msgLength = totalLength - pos;
          if (debug) JFLog.log("msg=" + pos + "/" + msgLength);
          msg = new String(packet, pos, msgLength);
          if (debug_msg) JFLog.log("PUBLISH:" + ip + ":" + topic_name + ":" + msg + "!");
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
            events.message(topic_name, msg);
          }
          break;
        }
        case CMD_PUBLISH_ACK:
          //???
          break;
        case CMD_PUBLISH_REC:
          reply = new byte[4];
          reply[0] = (byte)(CMD_PUBLISH_REL << 4);
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
          if (debug_msg) JFLog.log("subscribe_ack");
          break;
        case CMD_PING:
          reply = new byte[2];
          reply[0] = (byte)(CMD_PONG << 4);
//          setPacketLength(reply);  //zero
          if (debug_msg) JFLog.log("PING:" + ip);
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

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage:MQTT server [publish topic msg]");
      System.out.println("Usage:MQTT server [subscribe topic]");
      return;
    }
//    MQTT.debug = true;
//    MQTT.debug_msg = true;
    try {
      MQTT client = new MQTT();
      client.setListener((topic, msg) -> {
        JFLog.log("msg:" + topic + "=" + msg);
        client.unsubscribe(topic);
        resub = topic;
        return true;
      });
      client.connect(args[0]);
      client.connect("user", "pass");
      if (args.length == 4) {
        if (args[1].equals("publish")) {
          client.publish(args[2], args[3]);
        }
      }
      if (args.length == 3) {
        if (args[1].equals("subscribe")) {
          client.subscribe(args[2]);
        }
      }
      while (client.isConnected()) {
        for(int a=0;a<60;a++) {
          JF.sleep(1000);
        }
        client.ping();
        if (resub != null) {
          client.subscribe(resub);
          resub = null;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

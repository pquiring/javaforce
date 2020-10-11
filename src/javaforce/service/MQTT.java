package javaforce.service;

/** MQTTBroker service
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;

public class MQTT extends Thread {
  public void setListener(MQTTEvents events) {
    this.events = events;
  }

  public void cancel() {
    active = false;
  }

  private static class Reader {
    public String ip;
  }

  private boolean active;
  private ArrayList<Reader> readers = new ArrayList<>();
  private MQTTEvents events;

  private ServerSocket ss;

  private static int bufsiz = 4096;

  public void run() {
    active = true;
    try {
      ss = new ServerSocket(1883);  //MQTT Broker port
      while (active) {
        Socket s = ss.accept();
        Worker worker = new Worker(s);
        worker.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private Reader getReader(String ip) {
    for(Reader reader : readers) {
      if (reader.ip.equals(ip)) return reader;
    }
    return null;
  }

  /*

  struct MQTT {
    byte header;  // msg_type(4) / dup(1) / qos(2) / retain (1)
    byte[] length;  // (7 bits per byte) (bit 7 indicates another byte) (max 4 bytes)
    short id1;  //some packets
    short topicLength;  //some packets
    byte[] topic;  //some packets
    short id2;  //some packets
    byte[] msg;  //remainder of packet
  }

   */

  private int getPacketLength(byte[] data, int length) {
    int multi = 1;
    int value = 0;
    int pos = 1;
    int next;
    do {
      if (pos >= length) return -1;
      next = data[pos++] & 0xff;
      value += (next & 0x7f) * multi;
      multi *= 0x80;
    } while (next >= 0x80);
    return value;
  }

  private int getTopicLength(byte[] data, int topicPosition) {
    return BE.getuint16(data, topicPosition);
  }

  private short getPacketID(byte[] data, int idPosition) {
    return (short)BE.getuint16(data, idPosition);
  }

  private void setPacketLength(byte[] in, byte[] out) {
    int value = in.length;
    int pos = 1;
    byte ebyte;
    do {
      ebyte = (byte)(value % 0x80);
      value /= 0x80;
      if (value > 0) {
        ebyte |= 0x80;
      }
      out[pos++] = ebyte;
    } while (value != 0);
  }

  /** max packet size = 127 bytes + 2 for header */
  private void setPacketLength(byte[] packet) {
    int value = packet.length - 2;
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

  private void setPacketID(byte[] data, short id) {
    BE.setuint16(data, 2, id);
  }

  private int getLengthBytes(int length) {
    if (length <= 0x7f) return 1;
    if (length <= 0x3ff) return 2;
    if (length <= 0x1fffff) return 3;
    if (length <= 0xfffffff) return 4;
    return -1;
  }

  private static final byte CMD_CONNECT = 1;
  private static final byte CMD_CONNECT_ACK = 2;
  private static final byte CMD_PUBLISH = 3;
  private static final byte CMD_PUBLISH_ACK = 4;
  private static final byte CMD_PUBLISH_REC = 5;
  private static final byte CMD_PUBLISH_REL = 6;
  private static final byte CMD_PUBLISH_CMP = 7;
  private static final byte CMD_SUBSCRIBE = 8;
  private static final byte CMD_SUBSCRIBE_ACK = 9;
  private static final byte CMD_UNSUBSCRIBE = 10;
  private static final byte CMD_UNSUBSCRIBE_ACK = 11;
  private static final byte CMD_PING = 12;
  private static final byte CMD_PONG = 13;
  private static final byte CMD_DISCONNECT = 14;

  private class Worker extends Thread {
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public String ip;
    public Reader reader;
    public Worker(Socket s) {
      this.s = s;
    }
    public void run() {
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        ip = s.getInetAddress().getHostAddress();
        reader = getReader(ip);
        if (reader == null) {
          throw new Exception("reader not configured:" + ip);
        }
        JFLog.log("connect:" + ip);
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
            JFLog.log("read=" + read);
            if (read == -1) throw new Exception("bad read");
            totalRead += read;
            if (totalRead < 2) continue;
            if (packetLength == -1) {
              packetLength = getPacketLength(buf, totalRead);
              if (packetLength != -1) {
                totalLength = 1 + getLengthBytes(packetLength) + packetLength;
                JFLog.log("totalLength=" + totalLength);
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
      }
      JFLog.log("disconnect:" + ip);
    }
    private void process(byte[] packet, int totalLength, int packetLength) throws Exception {
      byte[] reply = null;
      byte cmd = (byte)((packet[0] & 0xf0) >> 4);
      short id;
      int idPosition;
      int topicPosition;
      int topicLength;
      String topic;
      int msgPosition;
      int msgLength;
      String msg;
      JFLog.log("cmd=" + cmd);
      switch (cmd) {
        case CMD_CONNECT:
          reply = new byte[4];
          //reply = header , size , ack_flags, return_code=0
          reply[0] = (byte)(CMD_CONNECT_ACK << 4);
          setPacketLength(reply);
          break;
        case CMD_PUBLISH:
          //header, size, topic, id, msg
          topicPosition = 1 + getLengthBytes(packetLength);
          topicLength = getTopicLength(packet, topicPosition);
          JFLog.log("topic=" + topicPosition + "/" + topicLength);
          topic = new String(packet, topicPosition + 2, topicLength);
          idPosition = topicPosition + 2 + topicLength;
          id = getPacketID(packet, idPosition);
          JFLog.log("id=" + id);
          msgPosition = idPosition + 2;
          msgLength = totalLength - msgPosition;
          JFLog.log("msg=" + msgPosition + "/" + msgLength);
          msg = new String(packet, msgPosition, msgLength);
          JFLog.log("PUBLISH:" + ip + ":" + topic + ":" + msg + "!");
          reply = new byte[4];
          reply[0] = (byte)(CMD_PUBLISH_ACK << 4);
          //reply = header , size , id_hi, id_lo
          setPacketLength(reply);
          setPacketID(reply, id);
          if (events != null) {
            events.message(topic, msg);
          }
          break;
        case CMD_PUBLISH_ACK:
          //???
          break;
        case CMD_PUBLISH_REC:
          //???
          break;
        case CMD_PUBLISH_REL:
          //???
          break;
        case CMD_PUBLISH_CMP:
          //???
          break;
        case CMD_SUBSCRIBE:
          //cmd, size, id, topic
          idPosition = 1 + getLengthBytes(packetLength);
          id = getPacketID(packet, idPosition);
          JFLog.log("id=" + id);
          topicPosition = idPosition + 2;
          topicLength = getTopicLength(packet, topicPosition);
          JFLog.log("topic=" + topicPosition + "/" + topicLength);
          topic = new String(packet, topicPosition + 2, topicLength);
          msgPosition = topicPosition + 2 + topicLength;
          msgLength = totalLength - msgPosition;
          JFLog.log("msg=" + msgPosition + "/" + msgLength);
          msg = new String(packet, msgPosition, msgLength);
          JFLog.log("SUBSCRIBE:" + ip + ":" + topic + ":" + msg + "!");
          reply = new byte[5];
          //header , size , id_hi, id_lo, return_code=0
          reply[0] = (byte)(CMD_SUBSCRIBE_ACK << 4);
          setPacketLength(reply);
          setPacketID(reply, id);
          break;
        case CMD_UNSUBSCRIBE:
          //cmd, size, id, topic
          idPosition = 1 + getLengthBytes(packetLength);
          id = getPacketID(packet, idPosition);
          JFLog.log("id=" + id);
          topicPosition = idPosition + 2;
          topicLength = getTopicLength(packet, topicPosition);
          JFLog.log("topic=" + topicPosition + "/" + topicLength);
          topic = new String(packet, topicPosition + 2, topicLength);
          msgPosition = topicPosition + 2 + topicLength;
          msgLength = totalLength - msgPosition;
          JFLog.log("msg=" + msgPosition + "/" + msgLength);
          msg = new String(packet, msgPosition, msgLength);
          JFLog.log("UNSUB:" + ip + ":" + topic + ":" + msg);
          reply = new byte[4];
          reply[0] = (byte)(CMD_UNSUBSCRIBE_ACK << 4);
          //header , size , id_hi, id_lo
          setPacketLength(reply);
          setPacketID(reply, id);
          break;
        case CMD_PING:
          reply = new byte[2];
          reply[0] = (byte)(CMD_PONG << 4);
//          setPacketLength(reply);  //zero
          JFLog.log("PING:" + ip);
          break;
        case CMD_DISCONNECT:
          break;
      }
      if (reply == null) {
        throw new Exception("bad cmd:" + cmd);
      }
      send(reply);
    }
    private void send(byte[] reply) throws Exception {
      os.write(reply);
    }
  }
}

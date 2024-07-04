package javaforce.service;

/** MQTTBroker service
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;

public class MQTTServer extends Thread {
  public void setListener(MQTTEvents events) {
    this.events = events;
  }

  public void cancel() {
    server_active = false;
  }

  private static class Topic {
    private String name;
    private byte[] pkt;  //retained publish
    private ArrayList<Worker> subs = new ArrayList<>();
    private Object lock = new Object();
    public static Topic[] TopicArrayType = new Topic[0];
    public Topic(String name) {
      this.name = name;
    }
    public void publish(byte[] pkt, boolean retain) {
      //mask off flags : dup, retain
      pkt[0] &= 0xf9;
      if (retain) {
        this.pkt = pkt.clone();
      }
      synchronized (lock) {
        for(Worker sub : subs.toArray(WorkerArrayType)) {
          try {
            sub.publish(pkt);
          } catch (Exception e) {
            unsubscribe(sub);
          }
        }
      }
    }
    public void subscribe(Worker worker) {
      synchronized (lock) {
        subs.add(worker);
      }
      if (pkt != null) {
        try {worker.publish(pkt);} catch (Exception e) {}
      }
    }
    public void unsubscribe(Worker worker) {
      synchronized (lock) {
        subs.remove(worker);
      }
    }
  }

  private boolean server_active;
  private Object lock = new Object();
  private HashMap<String, Topic> topics = new HashMap<>();
  private MQTTEvents events;

  private ServerSocket ss;

  private static int bufsiz = 4096;

  public static boolean debug = false;
  public static boolean debug_msg = false;

  public void run() {
    server_active = true;
    try {
      ss = new ServerSocket(1883);  //MQTT Broker port
      while (server_active) {
        Socket s = ss.accept();
        Worker worker = new Worker(s);
        worker.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
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

  private Topic getTopic(String name) {
    synchronized (lock) {
      Topic topic = topics.get(name);
      if (topic != null) return topic;
      topic = new Topic(name);
      topics.put(name, topic);
      return topic;
    }
  }

  private void unsubscribeAll(Worker worker) {
    synchronized (lock) {
      Topic[] alltopics = (Topic[])topics.values().toArray(Topic.TopicArrayType);
      for(Topic topic : alltopics) {
        topic.unsubscribe(worker);
      }
    }
  }

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

  private void setPacketID(byte[] data, int offset, short id) {
    BE.setuint16(data, offset, id);
  }

  private int getLengthBytes(int length) {
    if (length <= 0x7f) return 1;
    if (length <= 0x3ff) return 2;
    if (length <= 0x1fffff) return 3;
    if (length <= 0xfffffff) return 4;
    return -1;
  }

  public static final byte CMD_CONNECT = 1;
  public static final byte CMD_CONNECT_ACK = 2;
  public static final byte CMD_PUBLISH = 3;
  public static final byte CMD_PUBLISH_ACK = 4;
  public static final byte CMD_PUBLISH_REC = 5;
  public static final byte CMD_PUBLISH_REL = 6;
  public static final byte CMD_PUBLISH_CMP = 7;
  public static final byte CMD_SUBSCRIBE = 8;
  public static final byte CMD_SUBSCRIBE_ACK = 9;
  public static final byte CMD_UNSUBSCRIBE = 10;
  public static final byte CMD_UNSUBSCRIBE_ACK = 11;
  public static final byte CMD_PING = 12;
  public static final byte CMD_PONG = 13;
  public static final byte CMD_DISCONNECT = 14;

  public static final byte QOS_0 = 0;
  public static final byte QOS_1 = 1;
  public static final byte QOS_2 = 2;
  public static final byte QOS_3 = 3;  //not used

  public static Worker[] WorkerArrayType = new Worker[0];

  private class Worker extends Thread {
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public String ip;
    public boolean active = true;
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
        while (server_active && active) {
          int totalRead = 0;
          int packetLength = -1;  //excluding header + length fields
          int totalLength = -1;  //total packet length
          int read;
          Arrays.fill(buf, (byte)0);
          while (server_active && active) {
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
              packetLength = getPacketLength(buf, totalRead);
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
      }
      unsubscribeAll(this);
      if (debug) JFLog.log("disconnect:" + ip);
    }
    private void process(byte[] packet, int totalLength, int packetLength) throws Exception {
      byte[] reply = null;
      byte cmd = (byte)((packet[0] & 0xf0) >> 4);
      short id = 0;
      int idPosition;
      int topicPosition;
      int topicLength;
      String topic_name;
      int msgPosition;
      int msgLength;
      String msg;
      if (debug) JFLog.log("cmd=" + cmd);
      switch (cmd) {
        case CMD_CONNECT:
          reply = new byte[4];
          //reply = header , size , ack_flags, return_code=0
          reply[0] = (byte)(CMD_CONNECT_ACK << 4);
          setPacketLength(reply);
          break;
        case CMD_PUBLISH: {
          //header, size, topic, id, msg
          boolean dup = (packet[0] & 0x08) != 0;
          byte qos = (byte)((packet[0] & 0x06) >> 1);
          boolean retain = (packet[0] & 0x01) != 0;
          topicPosition = 1 + getLengthBytes(packetLength);
          topicLength = getTopicLength(packet, topicPosition);
          if (debug) JFLog.log("topic=" + topicPosition + "/" + topicLength);
          topic_name = new String(packet, topicPosition + 2, topicLength);
          idPosition = topicPosition + 2 + topicLength;
          msgPosition = idPosition;
          if (qos > 0) {
            id = getPacketID(packet, idPosition);
            if (debug) JFLog.log("id=" + id);
            msgPosition += 2;
          }
          msgLength = totalLength - msgPosition;
          if (debug) JFLog.log("msg=" + msgPosition + "/" + msgLength);
          msg = new String(packet, msgPosition, msgLength);
          if (debug_msg) JFLog.log("PUBLISH:" + ip + ":" + topic_name + ":" + msg + "!");
          Topic topic = getTopic(topic_name);
          topic.publish(packet, retain);
          switch (qos) {
            case QOS_1: {
              //CMD_PUBLISH_ACK
              reply = new byte[4];
              reply[0] = (byte)(CMD_PUBLISH_ACK << 4);
              //reply = header , size , id_hi, id_lo
              setPacketLength(reply);
              setPacketID(reply, 2, id);
              break;
            }
            case QOS_2: {
              //CMD_PUBLISH_REC
              reply = new byte[4];
              reply[0] = (byte)(CMD_PUBLISH_REC << 4);
              //reply = header , size , id_hi, id_lo
              setPacketLength(reply);
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
          //???
          break;
        case CMD_PUBLISH_REL:
          reply = new byte[4];
          reply[0] = (byte)(CMD_PUBLISH_CMP << 4);
          setPacketLength(reply);
          id = getPacketID(packet, 2);
          setPacketID(reply, 2, id);
          break;
        case CMD_PUBLISH_CMP:
          //???
          break;
        case CMD_SUBSCRIBE: {
          //cmd, size, id, topic
          idPosition = 1 + getLengthBytes(packetLength);
          id = getPacketID(packet, idPosition);
          if (debug) JFLog.log("id=" + id);
          topicPosition = idPosition + 2;
          topicLength = getTopicLength(packet, topicPosition);
          if (debug) JFLog.log("topic=" + topicPosition + "/" + topicLength);
          topic_name = new String(packet, topicPosition + 2, topicLength);
          msgPosition = topicPosition + 2 + topicLength;
          msgLength = totalLength - msgPosition;
          if (debug) JFLog.log("msg=" + msgPosition + "/" + msgLength);
          msg = new String(packet, msgPosition, msgLength);
          Topic topic = getTopic(topic_name);
          topic.subscribe(this);
          if (debug_msg) JFLog.log("SUBSCRIBE:" + ip + ":" + topic_name + ":" + msg + "!");
          reply = new byte[5];
          //header , size , id_hi, id_lo, return_code=0
          reply[0] = (byte)(CMD_SUBSCRIBE_ACK << 4);
          setPacketLength(reply);
          setPacketID(reply, 2, id);
          break;
        }
        case CMD_UNSUBSCRIBE: {
          //cmd, size, id, topic
          idPosition = 1 + getLengthBytes(packetLength);
          id = getPacketID(packet, idPosition);
          if (debug) JFLog.log("id=" + id);
          topicPosition = idPosition + 2;
          topicLength = getTopicLength(packet, topicPosition);
          if (debug) JFLog.log("topic=" + topicPosition + "/" + topicLength);
          topic_name = new String(packet, topicPosition + 2, topicLength);
          msgPosition = topicPosition + 2 + topicLength;
          msgLength = totalLength - msgPosition;
          if (debug) JFLog.log("msg=" + msgPosition + "/" + msgLength);
          msg = new String(packet, msgPosition, msgLength);
          Topic topic = getTopic(topic_name);
          topic.unsubscribe(this);
          if (debug_msg) JFLog.log("UNSUB:" + ip + ":" + topic_name + ":" + msg);
          reply = new byte[4];
          reply[0] = (byte)(CMD_UNSUBSCRIBE_ACK << 4);
          //header , size , id_hi, id_lo
          setPacketLength(reply);
          setPacketID(reply, 2, id);
          break;
        }
        case CMD_PING:
          reply = new byte[2];
          reply[0] = (byte)(CMD_PONG << 4);
//          setPacketLength(reply);  //zero
          if (debug_msg) JFLog.log("PING:" + ip);
          break;
        case CMD_DISCONNECT:
          unsubscribeAll(this);
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
    public void publish(byte[] pkt) throws Exception {
      os.write(pkt);
    }
  }
}

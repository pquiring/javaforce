package javaforce.service;

/** MQTTBroker service
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

public class MQTTServer {
  public final static String busPack = "net.sf.jfmqtt";

  private static MQTTServer service;
  private static JBusServer busServer;
  public static void serviceStart(String[] args) {
    service = new MQTTServer();
    service.start();
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    for(String arg : args) {
      switch (arg) {
        case "debug": debug = true; break;
        case "debug_msg": debug_msg = true; break;
      }
    }
  }

  public static void serviceStop() {
    if (service != null) {
      service.stop();
      service = null;
    }
    if (busServer != null) {
      busServer.close();
      busServer = null;
    }
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33014;
    } else {
      return 777;
    }
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfmqtt.log";
  }

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfmqtt.cfg";
  }

  public void setListener(MQTTEvents events) {
    this.events = events;
  }

  public void start() {
    server = new Server();
    server.start();
  }

  public void stop() {
    if (server == null) return;
    server.active = false;
    if (ss != null) {
      try {ss.close();} catch (Exception e) {}
      ss = null;
    }
    if (forwarder != null) {
      forwarder.stop();
      forwarder = null;
    }
    if (busClient != null) {
      busClient.close();
      busClient = null;
    }
    server = null;
  }

  private static class Config {
    public int port = 1883;
    public String user, pass;
    public String forward;
    public int forward_port = 1883;
    public String forward_topic = "#";
    public String forward_user;
    public String forward_pass;
  }

  private static String defaultConfig
    = "port=1883\n"
    + "#user=username\n"
    + "#pass=password\n"
    + "#forward=host\n"
    + "#forward.port=1883\n"
    + "#forward.topic=#\n"
    + "#forward.user=username\n"
    + "#forward.pass=password\n"
  ;

  private static Config loadConfig() {
    try {
      File file = new File(getConfigFile());
      FileInputStream fis = new FileInputStream(file);
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      Config config = new Config();
      String port = props.getProperty("port");
      if (port != null) {
        config.port = JF.atoi(port);
        if (config.port <= 0 || config.port > 65535) {
          config.port = 1883;
        }
      }
      String user = props.getProperty("user");
      if (user != null) {
        config.user = user;
      }
      String pass = props.getProperty("pass");
      if (pass != null) {
        config.pass = pass;
      }
      String forward = props.getProperty("forward");
      if (forward != null) {
        config.forward = forward;
      }
      String forward_port = props.getProperty("forward_port");
      if (forward_port != null) {
        config.forward_port = JF.atoi(forward_port);
        if (config.forward_port <= 0 || config.forward_port > 65535) {
          config.forward_port = 1883;
        }
      }
      String forward_topic = props.getProperty("forward_topic");
      if (forward_topic != null) {
        config.forward_topic = forward_topic;
      }
      String forward_user = props.getProperty("forward_user");
      if (forward_user != null) {
        config.forward_user = forward_user;
      }
      String forward_pass = props.getProperty("forward_pass");
      if (forward_pass != null) {
        config.forward_pass = forward_pass;
      }
      return config;
    } catch (FileNotFoundException e) {
      //create default config
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(defaultConfig.getBytes());
        fos.close();
      } catch (Exception e2) {
        JFLog.log(e2);
      }
      return new Config();
    } catch (Exception e) {
      JFLog.log(e);
      return new Config();
    }
  }

  public static boolean hasWildcard(String topic) {
    if (topic.indexOf('+') != -1) return true;
    if (topic.indexOf('#') != -1) return true;
    return false;
  }

  private static class Topic {
    private String name;
    private byte[] pkt;  //retained publish
    private int pkt_length;
    private ArrayList<Client> subs = new ArrayList<>();
    private Object lock = new Object();
    public static Topic[] TopicArrayType = new Topic[0];
    public Topic(String name) {
      this.name = name;
    }
    public void publish(byte[] pkt, int length, boolean retain) {
      //mask off flags : dup, retain
      pkt[0] &= 0xf6;
      if (retain) {
        this.pkt = pkt.clone();
        this.pkt_length = length;
      }
      synchronized (lock) {
        for(Client sub : subs.toArray(ClientArrayType)) {
          try {
            sub.publish(pkt, length);
          } catch (Exception e) {
            unsubscribe(sub);
          }
        }
      }
    }
    public void subscribe(Client client) {
      synchronized (lock) {
        subs.add(client);
      }
      if (pkt != null) {
        try {client.publish(pkt, pkt_length);} catch (Exception e) {}
      }
    }
    public void unsubscribe(Client client) {
      synchronized (lock) {
        subs.remove(client);
      }
    }
    public boolean matches(String topic) {
      String[] ns = name.split("[/]");
      String[] ws = topic.split("[/]");
      int ni = 0;
      int wi = 0;
      for(;ni<ns.length;ni++) {
        String n = ns[ni];
        if (wi == ws.length) return false;
        String w = ws[wi];
        wi++;
        if (w.equals("+")) {
          //match any section
          continue;
        }
        if (w.equals("#")) {
          //match remainder
          return true;
        }
        if (!n.equals(w)) {
          //section does not match
          return false;
        }
      }
      if (wi != ws.length) {
        String w = ws[wi];
        if (w.equals("#")) return true;
        return false;
      }
      return true;
    }
  }

  private Server server;
  private Config config;
  private Object lock = new Object();
  private HashMap<String, Topic> topics = new HashMap<>();
  private MQTTEvents events;

  private ServerSocket ss;
  private Object forward_lock = new Object();
  private MQTTForward forwarder;
  private JBusClient busClient;

  private static int bufsiz = 4096;

  public static boolean debug = false;
  public static boolean debug_msg = false;

  private class Server extends Thread {
    public boolean active;
    public void run() {
      active = true;
      try {
        JFLog.append(getLogFile(), true);
        config = loadConfig();
        if (config.forward != null) {
          forwarder = new MQTTForward();
          if (config.forward_user != null && config.forward_pass != null) {
            forwarder.start(config.forward, config.forward_port, config.forward_user, config.forward_pass);
          } else {
            forwarder.start(config.forward, config.forward_port);
          }
        }
        busClient = new JBusClient(busPack, new JBusMethods());
        busClient.setPort(getBusPort());
        busClient.start();
        JFLog.log("MQTTServer starting on port " + config.port + "...");
        ss = new ServerSocket(config.port);
        while (active) {
          Socket s = ss.accept();
          Client client = new Client(s);
          client.start();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private Topic getTopic(String name) {
    synchronized (lock) {
      Topic topic = topics.get(name);
      if (topic != null) return topic;
      topic = new Topic(name);
      topics.put(name, topic);
      return topic;
    }
  }

  private Topic[] getTopics(String wc) {
    ArrayList<Topic> topics_sub = new ArrayList<>();
    synchronized (lock) {
      for(Topic topic : topics.values().toArray(Topic.TopicArrayType)) {
        if (topic.matches(wc)) {
          topics_sub.add(topic);
        }
      }
    }
    return topics_sub.toArray(Topic.TopicArrayType);
  }

  private void unsubscribeAll(Client client) {
    synchronized (lock) {
      Topic[] alltopics = (Topic[])topics.values().toArray(Topic.TopicArrayType);
      for(Topic topic : alltopics) {
        topic.unsubscribe(client);
      }
    }
  }

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
  public static final byte CMD_AUTH = 15;

  public static final byte RESERVED = 0;
  public static final byte RESERVED_2 = 2;

  public static final byte QOS_0 = 0;
  public static final byte QOS_1 = 1;
  public static final byte QOS_2 = 2;
  public static final byte QOS_3 = 3;  //not used

  public static final byte FLAG_CLEAN_START = 2;
  public static final byte FLAG_PASS = 0x40;
  public static final byte FLAG_USER = (byte)0x80;

  public static Client[] ClientArrayType = new Client[0];

  private class Client extends Thread {
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public String ip;
    public boolean client_active = true;
    public String client_id;
    public boolean auth;
    public Client(Socket s) {
      this.s = s;
    }
    public void run() {
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        ip = s.getInetAddress().getHostAddress();
        if (debug) JFLog.log("connect:" + ip);
        byte[] buf = new byte[bufsiz];
        if (config.user == null || config.pass == null) {
          auth = true;
        }
        while (server.active && client_active) {
          int totalRead = 0;
          int packetLength = -1;  //excluding header + length fields
          int totalLength = -1;  //total packet length
          int read;
          Arrays.fill(buf, (byte)0);
          while (server.active && client_active) {
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
      } catch (SocketException se) {
      } catch (Exception e) {
        JFLog.log(e);
      }
      unsubscribeAll(this);
      if (debug) JFLog.log("disconnect:" + ip);
    }
    private void process(byte[] packet, int totalLength, int packetLength) throws Exception {
      //totalLength = packet.length
      //packetLength = length excluding header and packet length byte(s)
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
        case CMD_CONNECT: {
          pos = 1 + getLengthBytes(packetLength);
          int ver = packet[pos + 6];  //should be 5
          int flags = packet[pos + 7];
          pos += 10;

          int props_length = getLength(packet, pos, totalLength);
          if (props_length == -1) throw new Exception("malformed packet");
          int props_length_bytes = getLengthBytes(props_length);
          pos += props_length_bytes;
          if (props_length > 0) {
            pos += props_length_bytes;
          }

          int client_id_length = BE.getuint16(packet, pos);
          pos += 2;
          client_id = getString(packet, pos, client_id_length);
          if (debug) {
            JFLog.log("client_id=" + client_id);
          }
          pos += client_id_length;
          String user = null;
          if ((flags & FLAG_USER) != 0) {
            int user_length = BE.getuint16(packet, pos);
            pos += 2;
            user = getString(packet, pos, user_length);
            pos += user_length;
          }
          String pass = null;
          if ((flags & FLAG_PASS) != 0) {
            int pass_length = BE.getuint16(packet, pos);
            pos += 2;
            pass = getString(packet, pos, pass_length);
            pos += pass_length;
          }
          if (config.user != null && config.pass != null) {
            //compare user/pass
            if (user == null || !user.equals(config.user) || pass == null || !pass.equals(config.pass)) {
              if (debug) {
                JFLog.log("auth failed:" + user + ":" + pass);
              }
              disconnect();
              break;
            }
            auth = true;
          }
          reply = new byte[5];
          //reply = header , size , ack_flags, return_code=0, props
          reply[0] = (byte)(CMD_CONNECT_ACK << 4);
          setPacketLength(reply);
          break;
        }
        case CMD_PUBLISH: {
          if (!auth) {
            disconnect();
            break;
          }
          //header, size, topic, id, msg
          boolean dup = (packet[0] & 0x08) != 0;
          byte qos = (byte)((packet[0] & 0x06) >> 1);
          boolean retain = (packet[0] & 0x01) != 0;
          if (qos == QOS_3) throw new Exception("malformed packet");
          pos = 1 + getLengthBytes(packetLength);
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
          if (debug_msg) JFLog.log("PUBLISH:" + ip + ":" + topic_name + ":" + msg);
          Topic topic = getTopic(topic_name);
          topic.publish(packet, totalLength, retain);
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
          if (forwarder != null) {
            forwarder.publish(topic_name, msg);
          }
          break;
        }
        case CMD_PUBLISH_ACK:
          if (!auth) {
            disconnect();
            break;
          }
          //???
          break;
        case CMD_PUBLISH_REC:
          if (!auth) {
            disconnect();
            break;
          }
          //???
          break;
        case CMD_PUBLISH_REL:
          if (!auth) {
            disconnect();
            break;
          }
          reply = new byte[4];
          reply[0] = (byte)(CMD_PUBLISH_CMP << 4);
          setPacketLength(reply);
          id = getPacketID(packet, 2);
          setPacketID(reply, 2, id);
          break;
        case CMD_PUBLISH_CMP:
          if (!auth) {
            disconnect();
            break;
          }
          //???
          break;
        case CMD_SUBSCRIBE: {
          if (!auth) {
            disconnect();
            break;
          }
          //cmd, size, id, topic
          pos = 1 + getLengthBytes(packetLength);
          id = getPacketID(packet, pos);
          if (debug) JFLog.log("id=" + id);
          pos += 2;
          int props_length = getLength(packet, pos, totalLength);
          int props_length_bytes = getLengthBytes(props_length);
          pos += props_length_bytes;
          if (props_length > 0) {
            pos += props_length_bytes;
          }
          while (pos < totalLength) {
            topicLength = getStringLength(packet, pos);
            if (debug) JFLog.log("topic=" + pos + "/" + topicLength);
            pos += 2;
            topic_name = getString(packet, pos, topicLength);
            pos += topicLength;
            if (hasWildcard(topic_name)) {
              Topic[] topics = getTopics(topic_name);
              for(Topic topic : topics) {
                topic.subscribe(this);
              }
            } else {
              Topic topic = getTopic(topic_name);
              topic.subscribe(this);
            }
            if (debug_msg) JFLog.log("SUBSCRIBE:" + ip + ":" + topic_name);
            pos++;  //subscribe options
          }
          reply = new byte[5];
          //header , size , id_hi, id_lo, return_code=0
          reply[0] = (byte)(CMD_SUBSCRIBE_ACK << 4);
          setPacketLength(reply);
          setPacketID(reply, 2, id);
          break;
        }
        case CMD_UNSUBSCRIBE: {
          if (!auth) {
            disconnect();
            break;
          }
          //cmd, size, id, topic
          pos = 1 + getLengthBytes(packetLength);
          id = getPacketID(packet, pos);
          if (debug) JFLog.log("id=" + id);
          pos += 2;
          int props_length = getLength(packet, pos, totalLength);
          if (props_length == -1) throw new Exception("malformed packet");
          int props_length_bytes = getLengthBytes(props_length);
          pos += props_length_bytes;
          if (props_length > 0) {
            pos += props_length_bytes;
          }
          while (pos < totalLength) {
            topicLength = getStringLength(packet, pos);
            pos += 2;
            if (debug) JFLog.log("topic=" + pos + "/" + topicLength);
            topic_name = getString(packet, pos, topicLength);
            pos += topicLength;
            Topic topic = getTopic(topic_name);
            topic.unsubscribe(this);
            if (debug_msg) JFLog.log("UNSUB:" + ip + ":" + topic_name);
          }
          reply = new byte[5];
          reply[0] = (byte)(CMD_UNSUBSCRIBE_ACK << 4);
          //header , size , id_hi, id_lo, reason=0
          setPacketLength(reply);
          setPacketID(reply, 2, id);
          break;
        }
        case CMD_PING:
          if (!auth) {
            disconnect();
            break;
          }
          reply = new byte[2];
          reply[0] = (byte)(CMD_PONG << 4);
//          setPacketLength(reply);  //zero
          if (debug_msg) JFLog.log("PING:" + ip);
          break;
        case CMD_DISCONNECT:
          disconnect();
          break;
      }
      if (reply != null) {
        send(reply);
      }
    }
    private void send(byte[] reply) throws Exception {
      os.write(reply);
    }
    public void publish(byte[] pkt, int length) throws Exception {
      os.write(pkt, 0, length);
    }
    private void disconnect() {
      unsubscribeAll(this);
      client_active = false;
      try {s.close();} catch (Exception e) {}
      s = null;
    }
  }

  public static class JBusMethods {
    public void getConfig(String pack) {
      byte[] cfg = JF.readFile(getConfigFile());
      if (cfg == null) cfg = new byte[0];
      String config = new String(cfg);
      service.busClient.call(pack, "getConfig", JBusClient.quote(JBusClient.encodeString(config)));
    }
    public void setConfig(String cfg) {
      //write new file
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      service.stop();
      service = new MQTTServer();
      service.start();
    }
  }
}

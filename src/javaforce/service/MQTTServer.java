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

import static javaforce.MQTT.*;

public class MQTTServer {
  public final static String busPack = "net.sf.jfmqtt";

  private static MQTTServer service;
  private static JBusServer busServer;
  private ArrayList<ServerWorker> servers = new ArrayList<ServerWorker>();

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
        case "debug": debug = true; JFLog.log("debug enabled"); break;
        case "debug_msg": debug_msg = true; JFLog.log("debug messages enabled");break;
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

  private static String getKeyFile() {
    return JF.getConfigPath() + "/jfmqtt.key";
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
    synchronized(lock) {
      ServerWorker[] sa = servers.toArray(new ServerWorker[0]);
      for(ServerWorker s : sa) {
        s.close();
      }
      servers.clear();
    }
    if (forwarder != null) {
      forwarder.stop();
      forwarder = null;
    }
    if (busClient != null) {
      busClient.close();
      busClient = null;
    }
    synchronized(server) {
      server.notify();
    }
    server = null;
  }

  private static class Config {
    public int port = 1883;
    public int secure = 8883;
    public String user, pass;
    public String forward;
    public int forward_port = 1883;
    public boolean forward_secure;
    public String forward_topic = wildcard_multi_level;
    public String forward_user;
    public String forward_pass;
  }

  private static String defaultConfig
    = "port=1883\n"
    + "secure=8883\n"
    + "#user=username\n"
    + "#pass=password\n"
    + "#forward=host\n"
    + "#forward.port=1883 or 8883\n"
    + "#forward.secure=true\n"
    + "#forward.topic=" + wildcard_multi_level + "\n"
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
      String secure = props.getProperty("secure");
      if (secure != null) {
        config.secure = JF.atoi(secure);
        if (config.secure <= 0 || config.secure > 65535) {
          config.secure = 1883;
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
      String forward_port = props.getProperty("forward.port");
      if (forward_port != null) {
        config.forward_port = JF.atoi(forward_port);
        if (config.forward_port <= 0 || config.forward_port > 65535) {
          config.forward_port = 1883;
        }
      }
      String forward_secure = props.getProperty("forward.secure");
      if (forward_port != null) {
        config.forward_secure = forward_secure.equals("true");
      }
      String forward_topic = props.getProperty("forward.topic");
      if (forward_topic != null) {
        config.forward_topic = forward_topic;
      }
      String forward_user = props.getProperty("forward.user");
      if (forward_user != null) {
        config.forward_user = forward_user;
      }
      String forward_pass = props.getProperty("forward.pass");
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

  private void loadKeys() {
    try {
      keys = new KeyMgmt();
      if (new File(getKeyFile()).exists()) {
        FileInputStream fis = new FileInputStream(getKeyFile());
        keys.open(fis, "password");
        fis.close();
      } else {
        JFLog.log("Warning:Server SSL Keys not generated!");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static boolean hasWildcard(String topic) {
    if (topic.indexOf(wildcard_single_level_char) != -1) return true;
    if (topic.indexOf(wildcard_multi_level_char) != -1) return true;
    return false;
  }

  private static class Topic {
    private String name;
    private String value;  //retained value
    private ArrayList<Client> subs = new ArrayList<>();
    private Object lock = new Object();
    public static Topic[] TopicArrayType = new Topic[0];
    public Topic(String name) {
      this.name = name;
    }
    public void publish(String value, boolean retain) {
      if (retain) {
        this.value = value;
      }
      synchronized (lock) {
        for(Client sub : subs.toArray(ClientArrayType)) {
          try {
            sub.publish(name, value, QOS_0);
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
      if (value != null) {
        try {client.publish(name, value, QOS_0);} catch (Exception e) {}
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
        if (w.equals(wildcard_single_level)) {
          //match any section
          continue;
        }
        if (w.equals(wildcard_multi_level)) {
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
        if (w.equals(wildcard_multi_level)) return true;
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
  private KeyMgmt keys;

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
        loadKeys();
        if (config.forward != null) {
          forwarder = new MQTTForward();
          KeyMgmt forward_keys = null;
          if (config.forward_secure) {
            forward_keys = keys;
          }
          if (config.forward_user != null && config.forward_pass != null) {
            forwarder.start(config.forward, config.forward_port, forward_keys, config.forward_user, config.forward_pass);
          } else {
            forwarder.start(config.forward, config.forward_port, forward_keys);
          }
        }
        busClient = new JBusClient(busPack, new JBusMethods());
        busClient.setPort(getBusPort());
        busClient.start();
        if (config.port > 0) {
          ServerWorker worker = new ServerWorker(config.port, false);
          worker.start();
          servers.add(worker);
        }
        if (config.secure > 0) {
          ServerWorker worker = new ServerWorker(config.secure, true);
          worker.start();
          servers.add(worker);
        }
        while(active) {
          synchronized(this) {
            wait();
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private class ServerWorker extends Thread {
    private ServerSocket ss;
    private int port;
    private boolean secure;

    public boolean worker_active;

    public ServerWorker(int port, boolean secure) {
      this.port = port;
      this.secure = secure;
    }

    public void run() {
      try {
        if (secure) {
          JFLog.log("CreateServerSocketSSL:" + port);
          ss = JF.createServerSocketSSL(port, keys);
        } else {
          JFLog.log("CreateServerSocket:" + port);
          ss = new ServerSocket(port);
        }
        worker_active = true;
        while (worker_active) {
          Socket s = ss.accept();
          Client client = new Client(s);
          client.start();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void close() {
      worker_active = false;
      if (ss != null) {
        try { ss.close(); } catch (Exception e) {}
        ss = null;
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

  /** Get variable sized length. */
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

  public static final byte QOS_0 = 0;  //fire and forget (no reply)
  public static final byte QOS_1 = 1;  //CMD_PUBLISH_ACK reply
  public static final byte QOS_2 = 2;  //CMD_PUBLISH_REC reply, CMD_PUBLISH_REL request, CMD_PUBLISH_CMP reply
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
    public int ver = 4;
    public int keep_alive;
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
      int pos = 1 + getLengthBytes(packetLength);
      int topicLength;
      String topic_name;
      int msgLength;
      String msg;
      if (debug) JFLog.log("cmd=" + cmd);
      switch (cmd) {
        case CMD_CONNECT: {
          //packet[pos + 0] = length_msb = 0x00
          //packet[pos + 1] = length_msb = 0x04
          //packet[pos + 2] thru [pos + 5] = "MQTT"
          ver = packet[pos + 6];  //4 or 5
          int flags = packet[pos + 7];
          keep_alive = BE.getuint16(packet, pos + 8);
          pos += 10;

          if (ver == 5) {
            int props_length = getLength(packet, pos, totalLength);
            if (props_length == -1) throw new Exception("malformed packet");
            int props_length_bytes = getLengthBytes(props_length);
            pos += props_length_bytes;
            if (props_length > 0) {
              pos += props_length_bytes;
            }
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
          switch (ver) {
            case 4: reply = new byte[4]; break;
            case 5: reply = new byte[5]; break;  //adds props
          }
          //reply = header , size , ack_flags, return_code=0, [props]
          reply[0] = (byte)(CMD_CONNECT_ACK << 4);
          setPacketLength(reply);
          if (events != null) {
            events.onConnect();
          }
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
          topicLength = getStringLength(packet, pos);
          if (debug) JFLog.log("topic=" + pos + "/" + topicLength);
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
              pos += props_length_bytes;
            }
          }
          msgLength = totalLength - pos;
          if (debug) JFLog.log("msg=" + pos + "/" + msgLength);
          msg = new String(packet, pos, msgLength);
          if (debug_msg) JFLog.log("PUBLISH:" + ip + ":" + topic_name + ":" + msg);
          Topic topic = getTopic(topic_name);
          topic.publish(msg, retain);
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
            events.onMessage(topic_name, msg);
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
          //??? should not get ???
          break;
        case CMD_PUBLISH_REC:
          if (!auth) {
            disconnect();
            break;
          }
          //??? should not get ???
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
          //cmd, size, id, [props], topic
          id = getPacketID(packet, pos);
          if (debug) JFLog.log("id=" + id);
          pos += 2;
          if (ver == 5) {
            int props_length = getLength(packet, pos, totalLength);
            int props_length_bytes = getLengthBytes(props_length);
            pos += props_length_bytes;
            if (props_length > 0) {
              pos += props_length_bytes;
            }
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
            if (events != null) {
              events.onSubscribe(topic_name);
            }
            pos++;  //subscribe options (QOS)
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
          id = getPacketID(packet, pos);
          if (debug) JFLog.log("id=" + id);
          pos += 2;
          if (ver == 5) {
            int props_length = getLength(packet, pos, totalLength);
            if (props_length == -1) throw new Exception("malformed packet");
            int props_length_bytes = getLengthBytes(props_length);
            pos += props_length_bytes;
            if (props_length > 0) {
              pos += props_length_bytes;
            }
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
          reply = new byte[4];
          reply[0] = (byte)(CMD_UNSUBSCRIBE_ACK << 4);
          //header , size , id_hi, id_lo
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
    private short id = 0x0001;
    public void publish(String topic, String msg, byte qos) throws Exception {
      //each client could require different publish based on version (4 or 5)
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
    private void disconnect() {
      unsubscribeAll(this);
      client_active = false;
      try {s.close();} catch (Exception e) {}
      s = null;
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

    private void setStringLength(byte[] data, int offset, short length) {
      BE.setuint16(data, offset, length);
    }
  }

  public static boolean createKeys() {
    return KeyMgmt.keytool(new String[] {
      "-genkey", "-debug", "-alias", "jfmqtt", "-keypass", "password", "-storepass", "password",
      "-keystore", getKeyFile(), "-validity", "3650", "-dname", "CN=jfmqtt.sourceforge.net, OU=user, O=server, C=CA",
      "-keyalg" , "RSA", "-keysize", "2048"
    });
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
    public void genKeys(String pack) {
      if (createKeys()) {
        JFLog.log("Generated Keys");
        service.busClient.call(pack, "getKeys", service.busClient.quote("OK"));
      } else {
        service.busClient.call(pack, "getKeys", service.busClient.quote("ERROR"));
      }
    }
  }
}

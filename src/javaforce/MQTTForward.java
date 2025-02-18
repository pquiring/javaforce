package javaforce;

/** MQTT Forward
 *
 * Provides a queue of messages to be sent to an MQTT service.
 * MQTTForward will work in the background to send messages.
 *
 * @author peter.quiring
 */

import java.util.*;

import javaforce.*;

public class MQTTForward {
  private String host;
  private int port = 1883;
  private KeyMgmt keys;
  private String user;
  private String pass;
  private int max_queue_size = 1000;
  private int keep_alive = 30;
  private long last_packet = -1;

  private static class Entry {
    public Entry(String topic, String msg) {
      this.topic = topic;
      this.msg = msg;
    }
    public String topic;
    public String msg;
  }

  private ArrayList<Entry> queue = new ArrayList<>();
  private Object queue_lock = new Object();

  private MQTT client;
  private Object client_lock = new Object();

  private Server server;
  private boolean active;

  public void start(String host, int port) {
    start(host, port, null, null, null);
  }

  public void start(String host, int port, KeyMgmt keys) {
    start(host, port, keys, null, null);
  }

  public void start(String host, String user, String pass) {
    start(host, 1883, null, user, pass);
  }

  public void start(String host, KeyMgmt keys, String user, String pass) {
    start(host, 1883, keys, user, pass);
  }

  public void start(String host, int port, String user, String pass) {
    start(host, port, null, user, pass);
  }

  public void start(String host, int port, KeyMgmt keys, String user, String pass) {
    this.host = host;
    this.port = port;
    this.keys = keys;
    this.user = user;
    this.pass = pass;
    active = true;
    server = new Server();
    server.start();
  }

  public void stop() {
    active = false;
  }

  /** Set maximum queue size (default = 1000)
   * Messages are lost when queue reaches this size.
   */
  public void setMaxQueueSize(int size) {
    if (size < 100) size = 100;
    max_queue_size = size;
  }

  public void publish(String topic, String msg) {  //tag:value
    synchronized (queue_lock) {
      if (queue.size() > max_queue_size) {
        JFLog.log("Error:MQTTForward Queue > " + max_queue_size);
        return;
      }
      queue.add(new Entry(topic, msg));
      queue_lock.notify();
    }
  }

  /** Set keep alive interval in seconds (default = 30) (0 = disabled)
   * Send a ping() to maintain connection.
   */
  public void setKeepAlive(int value) {
    keep_alive = value;
  }

  public void reconnect() {
    synchronized (client_lock) {
      if (client != null) {
        client.disconnect();
        client = null;
      }
    }
  }

  private Entry remove() {
    while (active) {
      synchronized (queue_lock) {
        if (queue.size() == 0) {
          try { queue_lock.wait(500); } catch (Exception e) {}
          return null;
        }
        Entry entry = queue.remove(0);
        return entry;
      }
    }
    return null;
  }

  private class Server extends Thread {
    public int count;
    public void run() {
      while (active) {
        Entry entry = remove();
        if (entry == null) {
          JF.sleep(500);
          if (keep_alive > 0) {
            count++;
            if (count >= keep_alive) {
              count = 0;
            } else {
              continue;
            }
          } else {
            continue;
          }
        }
        count = 0;
        try {
          synchronized (client_lock) {
            if (client != null && keep_alive > 0) {
              long timeout = System.currentTimeMillis() - (keep_alive * 1000 * 2);
              if (client.getLastPacketTimestamp() < timeout) {
                reconnect();
              }
            }
            if (client != null && !client.isConnected()) {
              reconnect();
            }
            while (client == null) {
              client = new MQTT();
              if (keys == null) {
                if (!client.connect(host, port)) {
                  client = null;
                }
              } else {
                if (!client.connect(host, port, keys)) {
                  client = null;
                }
              }
              if (client != null) {
                if (user != null && pass != null) {
                  client.connect(user, pass);
                } else {
                  client.connect();
                }
              }
              if (client != null) {
                if (!client.isConnected()) {
                  client = null;
                  JF.sleep(1000);
                } else {
                  //connection okay : TODO : wait for connect() confirmation
                  break;
                }
              }
            }
            if (entry != null) {
              client.publish(entry.topic, entry.msg);
            } else {
              client.ping();
            }
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }
}

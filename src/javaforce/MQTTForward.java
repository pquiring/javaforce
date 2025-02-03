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

  private static class Entry {
    public Entry(String topic, String msg) {
      this.topic = topic;
      this.msg = msg;
    }
    public String topic;
    public String msg;
  }

  private Object lock = new Object();
  private ArrayList<Entry> queue = new ArrayList<>();
  private MQTT client;

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
    synchronized (lock) {
      if (queue.size() > max_queue_size) {
        JFLog.log("Error:MQTTForward Queue > " + max_queue_size);
        return;
      }
      queue.add(new Entry(topic, msg));
      lock.notify();
    }
  }

  private Entry remove() {
    while (active) {
      synchronized (lock) {
        if (queue.size() == 0) {
          try { lock.wait(1000); } catch (Exception e) {}
          return null;
        }
        Entry entry = queue.remove(0);
        return entry;
      }
    }
    return null;
  }

  private class Server extends Thread {
    public void run() {
      while (active) {
        Entry entry = remove();
        if (entry == null) {
          JF.sleep(1000);
          continue;
        }
        try {
          if (client != null && !client.isConnected()) {
            client.disconnect();
            client = null;
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
              break;
            }
            if (!client.isConnected()) {
              client = null;
              JF.sleep(1000);
              continue;
            }
          }
          client.publish(entry.topic, entry.msg);
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }
}

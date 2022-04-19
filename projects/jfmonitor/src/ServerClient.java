/** ServerClient
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;

public class ServerClient extends Thread {
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private boolean ready;
  private String host;
  private int port;
  private boolean active;
  private Object lock = new Object();
  private int version;
  public ArrayList<Storage> stores = new ArrayList<>();

  public long last;

  public ServerClient(Socket s) {
    this.s = s;
    active = true;
  }
  public String getHost() {
    return host;
  }
  public int getVersion() {
    return version;
  }
  public void close(boolean force) {
    active = false;
    if (force) {
      try {s.close();} catch (Exception e) {}
    }
  }
  public void run() {
    try {
      is = s.getInputStream();
      os = s.getOutputStream();
      host = s.getInetAddress().toString();  //temp hostname = ip address
      port = s.getPort();
      if (!version()) {
        JFLog.log("Client : " + host + " : rejected : unsupported version");
        throw new Exception("bad client");
      }
      if (!authenticate()) {
        JFLog.log("Client : " + host + " : rejected : bad password");
        throw new Exception("bad client");
      }
      if (!getHostName()) {
        JFLog.log("Client : " + host + " : rejected : bad hostname");
        throw new Exception("bad client");
      }
      if (!MonitorService.server.addClient(this)) {
        JFLog.log("Client : " + host + " : rejected : already connected");
        throw new Exception("bad client");
      }
      JFLog.log("Client : " + host + ":" + port + " : accepted");
      while (active) {
        String cmd = readString();
        switch (cmd) {
          case "pong":
            break;
          case "pn4":
            String nw_nic = readString();
            String nw_start = readString();
            String nw_stop = readString();
            int len = readLength();
            byte[] map = read(len);
            for(Network network : Config.current.getNetworks()) {
              if (network.host.equals(getHost())) {
                if (network.ip_nic.equals(nw_nic)) {
                  try {
                    network.update(map);
                  } catch (Exception e) {
                    JFLog.log(e);
                  }
                }
              }
            }
            break;
          case "fs":
            String fs = readString();
            String[] fss = fs.split("\n");
            for(int a=0;a<fss.length;a++) {
              String[] f = fss[a].split("[,]");
              if (f.length != 3) continue;
              String name = f[0];
              String total = f[1];
              String free = f[2];
              Storage store = getStorage(name);
              store.size = Long.valueOf(total);
              store.free = Long.valueOf(free);
            }
            last = 0;  //allow next update on next interval
            break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
      JFLog.log("Client : " + host + " : disconnected");
      try { s.close(); } catch (Exception e2) {}
    }
    MonitorService.server.removeClient(this);
  }
  private byte[] read(int size) throws Exception {
    byte[] data = new byte[size];
    int left = size;
    int pos = 0;
    while (left > 0) {
      int read = is.read(data, pos, left);
      if (read == -1) throw new Exception("bad read");
      if (read > 0) {
        pos += read;
        left -= read;
      }
    }
    return data;
  }
  private void writeLength(int len) throws Exception {
    byte[] data = new byte[4];
    LE.setuint32(data, 0, len);
    os.write(data);
  }
  private void writeLength64(long len) throws Exception {
    byte[] data = new byte[8];
    LE.setuint64(data, 0, len);
    os.write(data);
  }
  public void writeString(String str) throws Exception {
    writeLength(str.length());
    os.write(str.getBytes("utf-8"));
  }
  private int readLength() throws Exception {
    byte[] data = read(4);
    return LE.getuint32(data, 0);
  }
  private long readLength64() throws Exception {
    byte[] data = read(8);
    return LE.getuint64(data, 0);
  }
  private String readString() throws Exception {
    int length = readLength();
    byte[] data = read(length);
    return new String(data, "utf-8");
  }
  private boolean version() throws Exception {
    //read client version
    byte data[] = read(4);
    if (data[0] != 'V') return false;
    int idx = 1;
    while (data[idx] == '0') idx++;
    version = Integer.valueOf(new String(data, idx, 4-idx));
    //write server version
    os.write(Config.APIVersion.getBytes());
    return true;
  }
  private boolean authenticate() throws Exception {
    MD5 md5 = new MD5();
    Random r = new Random();
    String key = String.format("%016x", r.nextLong());
    String pwd_key = Config.current.password + key;
    md5.add(pwd_key);
    String challenge = md5.toString();
    //send password challenge
    os.write(key.getBytes());
    //read password reply
    byte[] reply = read(challenge.length());
    if (!challenge.equals(new String(reply))) {
      os.write("NOPE".getBytes());
      os.flush();
      JFLog.log("client password wrong [key=" + key + ":challenge=" + challenge + ":reply=" + reply + "]");
      s.close();
      return false;
    }
    os.write("OKAY".getBytes());
    //client accepted
    return true;
  }
  private boolean getHostName() throws Exception {
    int strlen = readLength();
    if (strlen > 255) {
      JFLog.log(1, "client hostname too long");
      s.close();
      return false;
    }
    byte stringchars[] = new byte[strlen];
    is.read(stringchars);
    host = new String(stringchars);
    ready = true;
    return true;
  }
  private synchronized Storage getStorage(String name) {
    for(Storage store : stores) {
      if (store.name.equals(name)) {
        return store;
      }
    }
    Storage store = new Storage(name);
    stores.add(store);
    return store;
  }
  private void ping() throws Exception {
    writeLength(0);
    read(4);
  }
}

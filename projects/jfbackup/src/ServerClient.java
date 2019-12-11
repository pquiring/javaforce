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
  private ArrayList<Request> queue = new ArrayList<Request>();
  private Request request;
  private int version;
  private static Object hostLock = new Object();

  public ServerClient(Socket s) {
    this.s = s;
    active = true;
  }
  public String getClientName() {
    return host;
  }
  public void run() {
    int pingCount = 0;
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
      synchronized(hostLock) {
        if (Config.current.hosts.contains(host)) {
          JFLog.log("Client : " + host + " : rejected : already connected");
          throw new Exception("bad client");
        }
        JFLog.log("Client : " + host + ":" + port + " : accepted");
        Config.current.hosts.add(host);
        Config.save();
      }
      while (active) {
        if (queue.isEmpty()) {
          pingCount += 250;
          JF.sleep(250);
          if (pingCount > 60 * 1000) {
            pingCount = 0;
            ping();
          }
          continue;
        }
        synchronized(lock) {
          request = queue.remove(0);
        }
        pingCount = 0;
        writeLength(request.cmd.length());
        os.write(request.cmd.getBytes());
        if (request.arg != null) {
          byte data[] = request.arg.getBytes("utf-8");
          writeLength(data.length);
          os.write(data);
        }
        switch (request.cmd) {
          case "listvolumes":  //list volumes
            listvolumes(request);
            break;
          case "mount":  //mount volume
            mount(request);
            break;
          case "unmount":  //unmount volume
            unmount(request);
            break;
          case "listfolder":  //list folder
            listfolder(request);
            break;
          case "readfile":  //read file
            readfile(request);
            break;
        }
        request = null;
      }
      BackupService.server.removeClient(this);
    } catch (Exception e) {
      JFLog.log(e);
      if (request != null) {
        request.notify.notify(null);  //signal an error
      }
      JFLog.log("Client : " + host + " : disconnected");
      if (Config.current.hosts.contains(host)) {
        Config.current.hosts.remove(host);
        Config.save();
      }
      try { s.close(); } catch (Exception e2) {}
    }
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
  private int readLength() throws Exception {
    byte[] data = read(4);
    return LE.getuint32(data, 0);
  }
  private long readLength64() throws Exception {
    byte[] data = read(8);
    return LE.getuint64(data, 0);
  }
  private boolean version() throws Exception {
    //read client version
    byte data[] = read(4);
    if (data[0] != 'V') return false;
    int idx = 1;
    while (data[idx] == '0') idx++;
    version = Integer.valueOf(new String(data, idx, 4-idx));
    if (version < Config.APIVersionMin) {
      throw new Exception("client too old");
    }
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
  private void ping() throws Exception {
    writeLength(0);
    read(4);
  }
  private void listvolumes(Request req) throws Exception {
    int length = readLength();
    if (length > 4 * 1024) {
      JFLog.log(1, "too many volumes");
      throw new Exception("too many volumes");
    }
    byte data[] = read(length);
    req.reply = new String(data);
    req.notify.notify(req);
  }
  private void mount(Request req) throws Exception {
    int length = readLength();
    if (length > 128) {
      JFLog.log(1, "mount reply too large");
      throw new Exception("mount reply too large");
    }
    byte data[] = read(length);
    req.reply = new String(data);
    req.notify.notify(req);
  }
  private void unmount(Request req) throws Exception {
    int length = readLength();
    if (length > 128) {
      JFLog.log(1, "unmount reply too large");
      throw new Exception("unmount reply too large");
    }
    byte data[] = read(length);
    req.reply = new String(data);
    req.notify.notify(req);
  }
  private void listfolder(Request req) throws Exception {
    int length = readLength();
    if (length > 16 * 1024 * 1024) {
      JFLog.log(1, "folder too large");
      throw new Exception("folder too large");
    }
    byte data[] = read(length);
    req.reply = new String(data, "utf-8");
    req.notify.notify(req);
  }
  private byte[] buffer = new byte[64 * 1024];
  private void readfile(Request req) throws Exception {
    long uncompressed = readLength64();
    if (uncompressed == -1) {
      throw new Exception("get file error");
    }
    req.uncompressed = uncompressed;
    long compressed = 0;
    while (Status.active) {
      int chunk = readLength();
      if (chunk == 0x20000) break;  //end of stream
      compressed += chunk;
      int left = chunk;
      while (left > 0) {
        int read = is.read(buffer, 0, left);
        if (read == -1) throw new Exception("bad read");
        if (read > 0) {
          left -= read;
          req.os.write(buffer, 0, read);
        }
      }
    }
    req.compressed = compressed;
    req.os = null;
    req.notify.notify(req);
  }

  public void addRequest(Request req, RequestNotify notify) {
    req.notify = notify;
    synchronized(lock) {
      queue.add(req);
    }
  }
}

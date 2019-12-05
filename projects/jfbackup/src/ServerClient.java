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
  private boolean active;
  private Object lock = new Object();
  private ArrayList<Request> queue = new ArrayList<Request>();
  private Request request;
  private int version;
  public static long localindex;

  public ServerClient(Socket s) {
    this.s = s;
    active = true;
  }
  public String getClientName() {
    return host;
  }
  public static void resetLocalIndex() {
    localindex = 0;
  }
  public void run() {
    int pingCount = 0;
    try {
      is = s.getInputStream();
      os = s.getOutputStream();
      if (!version()) {
        JFLog.log("Client rejected : unsupported version");
        return;
      }
      if (!authenticate()) {
        JFLog.log("Client rejected : bad password");
        return;
      }
      if (!getHostName()) {
        JFLog.log("Client rejected : bad hostname");
        return;
      }
      JFLog.log("Client accepted:" + host);
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
        writeLength(request.cmd.length());
        os.write(request.cmd.getBytes());
        if (request.arg != null) {
          writeLength(request.arg.length());
          os.write(request.arg.getBytes());
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
      if (request != null) {
        request.notify.notify(null);  //signal an error
        if (request.fos != null) {
          try { request.fos.close(); } catch (Exception e2) {}
          request.fos = null;
        }
      }
//      JFLog.log(1, e);
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
      JFLog.log(1, "client password wrong");
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
    req.reply = new String(data);
    req.notify.notify(req);
  }
  private synchronized long nextIndex() {
    return localindex++;
  }
  private void readfile(Request req) throws Exception {
    //this can be very large - need to save to a file
    req.localfile = null;
    long uncompressed = readLength64();
    if (uncompressed == -1) {
      req.notify.notify(req);
      throw new Exception("get file error");
    }
    req.uncompressed = uncompressed;
    long compressed = readLength64();
    req.compressed = compressed;
    req.localfile = Paths.tempPath + "/file-" + nextIndex() + ".dat";
    req.fos = new FileOutputStream(req.localfile);
    byte[] data = new byte[64 * 1024];
    long left = compressed;
    while (left > 0) {
      int read = is.read(data, 0, left > data.length ? data.length : (int)left);
      if (read == -1) throw new Exception("bad read");
      if (read > 0) {
        left -= read;
        req.fos.write(data, 0, read);
      }
    }
    req.fos.close();
    req.fos = null;
    req.notify.notify(req);
  }

  public void addRequest(Request req, RequestNotify notify) {
    req.notify = notify;
    synchronized(lock) {
      queue.add(req);
    }
  }
}

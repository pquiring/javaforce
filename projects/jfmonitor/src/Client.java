/** Client API
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.nio.file.*;

import javaforce.*;

public class Client extends Thread {
  private Socket s;
  private InputStream is;
  private OutputStream os;

  public void run() {
    while (Status.active) {
      try {
        JFLog.log("Client Connecting to:" + Config.current.server_host);
        s = new Socket(Config.current.server_host, 33201);
        is = s.getInputStream();
        os = s.getOutputStream();
        if (!version()) throw new Exception("version not accepted");
        if (!authenticate()) throw new Exception("wrong password");
        sendHost();
        JFLog.log("Client Connected!");
        while (Status.active) {
          int length = readLength();
          if (length == 0) {
            writeLength(0);  //pong
            continue;
          }
          if (length > 11) {
            throw new Exception("bad cmd length");
          }
          byte bytes[] = read(length);
          String cmd = new String(bytes);
          if (Config.debug) {
            JFLog.log("cmd=" + cmd);
          }
          switch (cmd) {
            case "ping":  //keep alive
              writeString("pong");
              break;
            case "fs":  //get file systems
              writeString("fs");
              sendFileSystems();
              break;
            case "pn4":  //ping ip4 network
              if (!Config.pcap) {
                JFLog.log("Error:ping network request but pcap not installed!");
                break;
              }
              String nic = readString();
              String start = readString();
              String stop = readString();
              byte[] result = new ScanNetwork().scan(nic, start, stop);
              writeString("pn4");
              writeString(nic);
              writeString(start);
              writeString(stop);
              writeLength(result.length);
              os.write(result);
              break;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
        JF.sleep(3000);  //try again in 3 seconds
      }
    }
  }
  public boolean test() {
    Socket s;
    try {
      s = new Socket(Config.current.server_host, 33201);
      is = s.getInputStream();
      os = s.getOutputStream();
      if (!version()) throw new Exception("version not accepted");
      if (!authenticate()) throw new Exception("wrong password");
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    is = null;
    os = null;
    return true;
  }
  public void close() {
    try {s.close();} catch (Exception e) {}
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
  private boolean version() throws Exception {
    //send client version
    os.write(Config.APIVersion.getBytes());
    //read server version
    read(4);
    return true;
  }
  private boolean authenticate() throws Exception {
    byte key[] = read(16);
    MD5 md5 = new MD5();
    String pwd_key = Config.current.password + new String(key);
    md5.add(pwd_key);
    String response = md5.toString();
    //send password reply
    os.write(response.getBytes());
    //read reply
    byte[] data = read(4);
    String reply = new String(data);
    if (reply.equals("OKAY")) return true;
//    Config.current.authFailed = true;
//    Config.save();
    return false;
  }
  private void sendHost() throws Exception {
    writeString(Config.current.this_host);
  }
  private static byte[] len4 = new byte[4];
  private void writeLength(int len) throws Exception {
    LE.setuint32(len4, 0, len);
    os.write(len4);
  }
  private static byte[] len8 = new byte[8];
  private void writeLength64(long len) throws Exception {
    LE.setuint64(len8, 0, len);
    os.write(len8);
  }
  private void writeString(String str) throws Exception {
    byte[] data = str.getBytes("utf-8");
    writeLength(data.length);
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
  private String readString() throws Exception {
    int len = readLength();
    byte[] data = read(len);
    return new String(data, "utf-8");
  }

  public void sendFileSystems() throws Exception {
    StringBuilder sb = new StringBuilder();
    for(FileStore fs : FileSystems.getDefault().getFileStores()) {
      if (fs.isReadOnly()) continue;  //do NOT report CD-ROMs, etc.
      if (JF.isUnix()) {
        //only include devices in /dev
        if (!fs.name().startsWith("/dev")) continue;
      }
      sb.append(fs.name());
      sb.append(",");
      sb.append(fs.getTotalSpace());
      sb.append(",");
      sb.append(fs.getUsableSpace());
      sb.append("\n");
    }
    writeString(sb.toString());
  }
}

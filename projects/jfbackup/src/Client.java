/** Client API
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;

public class Client extends Thread {
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private boolean active = true;
  private KeepAlive pinger = new KeepAlive();

  //status
  public static ArrayList<String> mounts = new ArrayList<String>();
  public static Object lock = new Object();
  public static boolean reading_files;

  public void run() {
    cleanMounts();
    while (Status.active && active) {
      try {
        JFLog.log("Client Connecting to:" + Config.current.server_host);
        s = new Socket(Config.current.server_host, 33200);
        is = s.getInputStream();
        os = s.getOutputStream();
        if (!version()) throw new Exception("version not accepted");
        if (!authenticate()) throw new Exception("wrong password");
        sendHost();
        JFLog.log("Connection accepted!");
        pinger = new KeepAlive();
        pinger.start();
        while (Status.active) {
          int length = readLength();
          if (length > 11) {
            throw new Exception("bad cmd length");
          }
          byte req[] = read(length);
          String cmd = new String(req);
          JFLog.log("Request=" + cmd);
          synchronized(lock) {
            switch (cmd) {
              case "listvolumes":  //list volumes
                writeString("listvolumes");
                listvolumes();
                break;
              case "mount":  //mount volume
                writeString("mount");
                mount();
                break;
              case "unmount":  //unmount volume
                writeString("unmount");
                unmount();
                break;
              case "listfolder":  //list folder
                writeString("listfolder");
                listfolder();
                break;
              case "readfile":  //read file
                writeString("readfile");
                reading_files = true;
                readfile();
                reading_files = false;
                break;
              case "readfolders":  //read all files in folder
                writeString("readfolders");
                reading_files = true;
                readfolders();
                reading_files = false;
                break;
              case "ping":
                writeString("pong");
                break;
              case "pong":
                break;
            }
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
        JF.sleep(3000);  //try again in 3 seconds
        cleanMounts();
      }
      if (s != null) {
        try {s.close();} catch (Exception e) {}
        s = null;
      }
      if (pinger != null) {
        try {pinger.active = false;} catch (Exception e) {}
        pinger = null;
      }
    }
  }
  private class KeepAlive extends Thread {
    public boolean active;
    public void run() {
      active = true;
      try {
        while(Status.active && active) {
          //wait 60 seconds
          for(int a=0;a<600;a++) {
            JF.sleep(100);
          }
          synchronized(lock) {
            writeString("ping");
          }
        }
      } catch (Exception e) {
        active = false;
      }
    }
  }
  public boolean test() {
    Socket s;
    try {
      s = new Socket(Config.current.server_host, 33200);
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
    active = false;
    if (s != null) {
      try {s.close();} catch (Exception e) {}
      s = null;
    }
    if (pinger != null) {
      try {pinger.active = false;} catch (Exception e) {}
      pinger = null;
    }
  }
  private byte[] read(int size) throws Exception {
    byte[] data = new byte[size];
    int left = size;
    int pos = 0;
    while (left > 0) {
      int read = is.read(data, pos, left);
      if (read == -1) throw new Exception("bad read:expected=" + size);
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
    JFLog.log("Authenticating");
    byte key[] = read(16);  //16 chars
    MD5 md5 = new MD5();
    String pwd_key = Config.current.password + new String(key);
    md5.add(pwd_key);
    String response = md5.toString();
    //send password reply
    os.write(response.getBytes());  //32 chars
    //read reply
    byte[] data = read(4);
    String reply = new String(data);
    if (reply.equals("OKAY")) return true;
//    Config.current.authFailed = true;
//    Config.save();
    return false;
  }
  private void sendHost() throws Exception {
    JFLog.log("Sending host");
    writeLength(Config.current.this_host.length());
    os.write(Config.current.this_host.getBytes());
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
  private void listvolumes() throws Exception {
    String vols[] = VSS.listVolumes();
    StringBuilder list = new StringBuilder();
    for(int a=0;a<vols.length;a++) {
      list.append(vols[a]);
      list.append("\r\n");
    }
    writeLength(list.length());
    os.write(list.toString().getBytes());
  }
  private void mount() throws Exception {
    //read arg
    int arglen = readLength();
    if (arglen != 2) {
      throw new Exception("bad mount volume length");
    }
    byte[] arg = read(arglen);
    String vol = new String(arg);
    cleanMounts();
    if (!VSS.createShadow(vol)) {
      writeLength(4);
      os.write("FAIL".getBytes());
      return;
    }
    String[] shadows = VSS.listShadows();
    for(String shadow : shadows) {
      if (shadow.startsWith(vol)) {
        if (VSS.mountShadow(Paths.vssPath, shadow.substring(3))) {
          JFLog.log("Note:mountShadow successful:" + shadow);
          writeLength(4);
          os.write("OKAY".getBytes());
          synchronized(lock) {
            mounts.add(vol);
          }
        } else {
          JFLog.log("Error:mountShadow failed:" + shadow);
          writeLength(4);
          os.write("FAIL".getBytes());
        }
        return;
      }
    }
    writeLength(4);
    os.write("FAIL".getBytes());
  }
  private void unmount() throws Exception {
    //read arg
    int arglen = readLength();
    if (arglen != 2) {
      throw new Exception("bad unmount volume length");
    }
    byte[] arg = read(arglen);
    String vol = new String(arg);
    VSS.unmountShadow(Paths.vssPath);
    if (!VSS.deleteShadow(vol)) {
      writeLength(4);
      os.write("FAIL".getBytes());
    } else {
      writeLength(4);
      os.write("OKAY".getBytes());
      synchronized(lock) {
        mounts.remove(vol);
      }
    }
  }
  private void cleanMounts() {
    //remove old mount if exists
    if (new File(Paths.vssPath).exists()) {
      VSS.unmountShadow(Paths.vssPath);
      new File(Paths.vssPath).delete();
    }
    //delete all old shadow copies
    String shadows[] = VSS.listShadows();
    for(String shadow : shadows) {
      VSS.deleteShadow(shadow.substring(0, 2));
    }
    //remove old mount if exists (2nd attempt)
    if (new File(Paths.vssPath).exists()) {
      VSS.unmountShadow(Paths.vssPath);
      new File(Paths.vssPath).delete();
    }
  }
  private boolean isValid(String name) {
    if (name.length() == 0) return false;
    if (name.equals(".") || name.equals("..")) return false;
    if (name.equals("$RECYCLE.BIN")) return false;
    if (name.equals("System Volume Information")) return false;
    //skip files with invalid chars
    if (name.contains("?")) return false;
    if (name.contains("*")) return false;
    if (name.contains(":")) return false;
    if (name.contains("\r\n")) return false;
    return true;
  }
  private void listfolder() throws Exception {
    //read arg
    int arglen = readLength();
    if (arglen > 2 * 1024) {
      throw new Exception("bad folder name length");
    }
    byte[] arg = read(arglen);
    String folder = Paths.vssPath + new String(arg);
    File files[] = new File(folder).listFiles();
    StringBuilder list = new StringBuilder();
    for(File file : files) {
      String name = file.getName();
      if (!isValid(name)) continue;
      if (file.isDirectory()) {
        list.append("\\");
        list.append(name);
      } else {
        if (file.length() == 0) continue;  //omit empty files
        list.append(name);
        list.append("|");
        list.append(file.length());
      }
      list.append("\r\n");
    }
    byte data[] = list.toString().getBytes("utf-8");
    writeLength(data.length);
    os.write(data);
  }
  private void readfile() throws Exception {
    //read arg
    int arglen = readLength();
    if (arglen > 2 * 1024) {
      throw new Exception("bad file name length");
    }
    byte[] arg = read(arglen);
    //compress file and then send it
    String vssFilename = Paths.vssPath + new String(arg, "utf-8");
    File vssFile = new File(vssFilename);
    if (!vssFile.exists()) {
      //should not happen
      JFLog.log(1, "Error:file not found:" + vssFilename);
      writeLength64(-1);
      return;
    }
    long uncompressed = vssFile.length();
    //send compressed file
    writeLength64(uncompressed);
    //send compressed data in "chunks"
    FileInputStream fis = new FileInputStream(vssFile);
    PipedInputStream pis = new PipedInputStream();
    PipedOutputStream pos = new PipedOutputStream(pis);
    Transfer transfer = new Transfer(pis);
    transfer.start();
    transfer.compressed = Compression.compress(fis, pos, uncompressed);
    pos.flush();
    pos.close();
    fis.close();
    transfer.join();
  }
  private void readfolders() throws Exception {
    //read arg
    long backupid = System.currentTimeMillis();
    JFLog.init(Paths.logsPath + "/backup-" + backupid + ".log", true);
    try {
      JFLog.log("Reading folder length");
      int arglen = readLength();
      JFLog.log("Folder Length=" + arglen);
      if (arglen > 2 * 1024) {
        throw new Exception("bad file name length");
      }
      byte[] arg = read(arglen);
      String path = Paths.vssPath + new String(arg, "utf-8");
      JFLog.log("Sending Folder:" + path);
      sendFolder(path);
      writeLength(-1);  //done
      JFLog.log("Done");
    } catch (Exception e) {
      JFLog.log(e);
    }
    JFLog.close();
  }
  private void sendFolder(String path) throws Exception {
    File folder = new File(path);
    if (!folder.exists()) {
      JFLog.log("Error:Path not found:" + path);
      return;
    }
    File files[] = folder.listFiles();
    if (files == null) {
      JFLog.log("Warning:listFiles() returned null:" + path);
      return;
    }
    for(File file : files) {
      if (file.isDirectory()) continue;
      String name = file.getName();
      if (!isValid(name)) continue;
      try {
        sendFile(path, name);
      } catch (Exception e) {
        //log error but continue
        JFLog.log(e);
      }
    }
    for(File file : files) {
      if (!file.isDirectory()) continue;
      String name = file.getName();
      if (!isValid(name)) continue;
      writeString("\\" + name);
      sendFolder(path + "\\" + name);
      writeString("\\..");
    }
  }
  private void sendFile(String path, String name) throws Exception {
    //compress file and then send it
    String vssFilename = path + "\\" + name;
    File vssFile = new File(vssFilename);
    if (!vssFile.exists()) {
      //should not happen
      JFLog.log(1, "Error:file not found:" + vssFilename);
      return;
    }
    long uncompressed = vssFile.length();
    FileInputStream fis = new FileInputStream(vssFile);
    PipedInputStream pis = new PipedInputStream();
    PipedOutputStream pos = new PipedOutputStream(pis);
    //send file name
    writeString(name);
    //send compressed file length
    writeLength64(uncompressed);
    //send compressed data in "chunks"
    Transfer transfer = new Transfer(pis);
    transfer.start();
    transfer.compressed = Compression.compress(fis, pos, uncompressed);
    pos.flush();
    pos.close();
    fis.close();
    transfer.join();
  }
  private static byte buffer[] = new byte[64 * 1024];
  public class Transfer extends Thread {
    private InputStream t_is;
    public long copied;
    public long compressed = -1;
    public Transfer(InputStream is) {
      this.t_is = is;
    }
    public void run() {
      try {
        while (Status.active) {
          if (copied == compressed) break;
          int read = t_is.read(buffer);
          if (read == -1) break;
          if (read == 0) continue;
          writeLength(read);
          os.write(buffer, 0, read);
          copied += read;
        }
        writeLength(0x20000);  //signal end of stream
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
}

package javaforce.service;

/** File Sync Server.
 *
 * Proprietary protocol similar in function to rsync but over SSL.
 *
 * Secure and fast.
 *
 * TCP Port 33203
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;

public class FileSyncServer {

  private static boolean debug = false;

  public static final int VERSION = 1;  //protocol version

  public static final int port = 33203;

  private String passwd;
  private String root;

  private Server server;
  private Object lock = new Object();
  private ArrayList<Client> clients = new ArrayList<>();

  public FileSyncServer(String passwd, String rootFolder) {
    this.passwd = passwd;
    this.root = rootFolder;
  }

  private static String getKeyFile() {
    return JF.getConfigPath() + "/jffilesync.key";
  }

  private static void genKeyFile() {
    //create(String storefile, String storepass, String alias, KeyParams params, String keypass) {
    KeyParams params = new KeyParams();
    params.dname = "CN=javaforce.sourceforge.net, O=server, OU=filesyncserver, C=CA";
    params.exts = new String[0];
    params.validity = "3650";  //10 years
    KeyMgmt.create(getKeyFile(), "password", "filesyncserver", params, "password");
  }

  private void addClient(Client client) {
    JFLog.log("client+=" + client.remote);
    synchronized(lock) {
      clients.add(client);
    }
  }

  private void removeClient(Client client) {
    JFLog.log("client-=" + client.remote);
    synchronized(lock) {
      clients.remove(client);
    }
  }

  private class Server extends Thread {
    public boolean active;
    public ServerSocket ss;
    public IP4Port bind = new IP4Port();
    public void run() {
      active = true;
      JFLog.append(JF.getLogPath() + "/jffilesync.log", true);
      JFLog.setRetention(30);
      JFLog.log("FileSync : Starting service");
      JFLog.log("CreateServerSocketSSL");
      KeyMgmt keys = new KeyMgmt();
      if (!new File(getKeyFile()).exists()) {
        JFLog.log("Warning:Generating self-signed SSL keys");
        genKeyFile();
      }
      try {
        FileInputStream fis = new FileInputStream(getKeyFile());
        keys.open(fis, "password");
        fis.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
      ss = JF.createServerSocketSSL(keys);
      bind.setIP("0.0.0.0");  //all interfaces
      bind.port = port;
      try {
        ss.bind(bind.toInetSocketAddress());
        while(active) {
          Socket s = ss.accept();
          Client client = new Client(s);
          addClient(client);
          client.start();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public void start() {
    stop();
    server = new Server();
    server.start();
  }

  public void stop() {
    if (server == null) return;
    server.active = false;
    try {server.ss.close();} catch (Exception e) {}
    server = null;
    synchronized(lock) {
      Client[] cs = clients.toArray(new Client[0]);
      for(Client c : cs) {
        c.close();
      }
      clients.clear();
    }
  }

  //client requests
  public static final byte REQ_AUTH = 0x01;
  public static final byte REQ_FILE_OPEN = 0x10;  //date, size, filename
  public static final byte REQ_FILE_BLOCK_HASH = 0x11;
  public static final byte REQ_FILE_BLOCK_DATA = 0x12;
  public static final byte REQ_FILE_TRUNCATE = 0x13;
  public static final byte REQ_FILE_CREATE = 0x14;
  public static final byte REQ_FILE_DELETE = 0x15;
  public static final byte REQ_FOLDER_OPEN = 0x20;
  public static final byte REQ_FOLDER_CREATE = 0x21;
  public static final byte REQ_FOLDER_DELETE = 0x22;
  public static final byte REQ_FOLDER_LIST = 0x23;
  //0x30 - 0x70 = reserved

  //server replies
  public static final byte RET_OKAY = 0x00;
  public static final byte RET_ERROR = 0x01;
  public static final byte RET_CHANGE = 0x02;
  public static final byte RET_NO_CHANGE = 0x03;
  public static final byte RET_NO_EXIST = 0x04;

  //auth types
  public static final int AUTH_PASSWD = 0x01;

  //hash types
  public static final int HASH_MD5 = 0x01;  //1 in 2^128 could fail
  public static final int HASH_MD5_SEED = 0x02;  //1 in 2^128^(2^128) could fail repeatedly (inf)

  public static final int max_auth_len = 256;
  public static final int max_data_len = (1024 * 1024) + 64;  //1MB data max + params

  public static void print_hash(byte[] hash) {
    StringBuilder s = new StringBuilder();
    s.append("hash=");
    for(int i=0;i<16;i++) {
      byte b = hash[i];
      if (i > 0) s.append(",");
      s.append(Integer.toHexString(b & 0xff));
    }
    JFLog.log(s.toString());
  }

  private class Client extends Thread {
    public String remote;
    private Socket s;
    private InputStream is;
    private OutputStream os;
    private boolean active;
    private byte[] req = new byte[max_data_len];
    private int req_len;
    private String folder;
    private String filename;
    private byte[] file_data = new byte[max_data_len];
    private RandomAccessFile file_io;
    private long file_date;
    private long file_size;
    private byte[] reply = new byte[max_data_len];
    private int reply_len;
    public Client(Socket s) {
      this.s = s;
      remote = s.getInetAddress().getHostAddress();
      folder = "/";
    }
    public void run() {
      active = true;
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        {
          int cmd = is.read();
          if (cmd == -1) throw new Exception("read error");
          if (cmd != REQ_AUTH) throw new Exception("auth not received");
          if (is.readNBytes(req, 0, 4) != 4) throw new Exception("read error");
          req_len = LE.getuint32(req, 0);
          if (debug) JFLog.log(String.format("c.cmd=%x len=%d", cmd, req_len));
          if (req_len < 8) throw new Exception("auth too small");
          if (req_len > max_auth_len) throw new Exception("auth too long");
          //version(4),auth_type(4)auth(?)
          if (is.readNBytes(req, 0, 8) != 8) throw new Exception("read error");
          int c_version = LE.getuint32(req, 0);
          req_len -= 4;
          int auth_type = LE.getuint32(req, 4);
          req_len -= 4;
          if (is.readNBytes(req, 0, req_len) != req_len) throw new Exception("auth data not received");
          switch (auth_type) {
            case AUTH_PASSWD:
              String client_password = new String(req, 0, req_len);
              if (debug) JFLog.log("client_password={" + client_password + "}");
              if (debug) JFLog.log("server_password={" + passwd + "}");
              if (!passwd.equals(client_password)) throw new Exception("access denied");
              if (debug) JFLog.log("Login granted:" + remote);
              reset();
              write(RET_OKAY);
              write(4);
              write(VERSION);
              if (debug) JFLog.log(String.format("s.cmd=%x len=%d", reply[0], reply_len-5));
              os.write(reply, 0, reply_len);
              break;
            default:
              throw new Exception("unknown auth type");
          }
        }
        while (active) {
          //read command (8)
          //read length (32)
          //read data (length)
          int cmd = is.read();
          if (cmd == -1) throw new Exception("read error");
          if (is.readNBytes(req, 0, 4) != 4) throw new Exception("read error");
          req_len = LE.getuint32(req, 0);
          if (debug) JFLog.log(String.format("c.cmd=%x len=%d", cmd, req_len));
          if (req_len > max_data_len) throw new Exception("data packet too big");
          if (is.readNBytes(req, 0, req_len) != req_len) throw new Exception("read data error");
          reply_len = 0;
          switch (cmd) {
            case REQ_FILE_OPEN: req_file_open(); break;
            case REQ_FILE_BLOCK_HASH: req_file_block_hash(); break;
            case REQ_FILE_BLOCK_DATA: req_file_block_data(); break;
            case REQ_FILE_TRUNCATE: req_file_truncate(); break;
            case REQ_FILE_CREATE: req_file_create(); break;
            case REQ_FILE_DELETE: req_file_delete(); break;
            case REQ_FOLDER_OPEN: req_folder_open(); break;
            case REQ_FOLDER_CREATE: req_folder_create(); break;
            case REQ_FOLDER_DELETE: req_folder_delete(); break;
            case REQ_FOLDER_LIST: req_folder_list(); break;
            default: throw new Exception("unknown cmd");
          }
          if (debug) JFLog.log(String.format("s.cmd=%x len=%d", reply[0], reply_len-5));
          os.write(reply, 0, reply_len);
        }
      } catch (Exception e) {
        if (debug) JFLog.log(e);
      }
      file_close();
      close();
      removeClient(this);
    }
    public void close() {
      active = false;
      try { s.close(); } catch (Exception e) {}
    }

    private void reset() {
      reply_len = 0;
    }
    private boolean write(byte b) {
      reply[reply_len++] = b;
      return true;
    }
    private byte[] buf = new byte[8];
    private boolean write(int len) {
      LE.setuint32(buf, 0, len);
      return write(buf, 0, 4);
    }
    private boolean write(long len) {
      LE.setuint64(buf, 0, len);
      return write(buf, 0, 8);
    }
    private boolean write(byte[] buf, int offset, int length) {
      if (reply_len + length > max_data_len) return error(RET_ERROR);
      System.arraycopy(buf, offset, reply, reply_len, length);
      reply_len += length;
      return true;
    }
    private boolean error(byte code) {
      reset();
      return write(code, null);
    }
    private boolean write(byte code, byte[] data) {
      write(code);
      if (data != null) {
        if (!write(data.length)) return false;
        if (!write(data, 0, data.length)) return false;
      } else {
        if (!write(0)) return false;
      }
      return true;
    }

    private void file_open(File file) throws Exception {
      if (file_io != null) {
        file_close();
      }
      file_io = new RandomAccessFile(file, "rw");
      file_date = file.lastModified();
      file_size = file.length();
    }
    private void file_close() {
      if (file_io == null) return;
      try {file_io.close();} catch (Exception e) {}
      file_io = null;
    }

    private boolean req_file_open() {
      //date(8), size(8), filename(?)
      if (req_len < 17) {
        JFLog.log("Error:req_file_open():req too short");
        return error(RET_ERROR);
      }
      if (file_io != null) {
        file_close();
      }
      long date = LE.getuint64(req, 0); req_len -= 8;
      long size = LE.getuint64(req, 8); req_len -= 8;
      filename = LE.getString(req, 16, req_len);
      if (filename.contains("..") || filename.contains("/")) {
        return error(RET_ERROR);
      }
      try {
        String path = root + folder + filename;
        File file = new File(path);
        if (!file.exists()) {
          write(RET_NO_EXIST, null);
          file_open(file);
          return true;
        }
        file_open(file);
        boolean change = false;
        if (file_date < date) change = true;
        else if (file_size != size) change = true;
        if (!change) {
          write(RET_NO_CHANGE, null);
        } else {
          write(RET_CHANGE);
          write(16);
          write(file_date);
          write(file_size);
        }
      } catch (Exception e) {
        return error(RET_ERROR);
      }
      return true;
    }

    private byte[] hash_md5 = new byte[16];
    private MD5 md5 = new MD5();
    private boolean req_file_block_hash() {
      if (file_io == null) {
        JFLog.log("Error:req_file_block_hash():file not open");
        return error(RET_ERROR);
      }
      if (req_len < 17) {
        JFLog.log("Error:req_file_block_hash():req too short");
        return error(RET_ERROR);
      }
      //hash_type(4)file_offset(8)block_len(4)hash(?)
      int pos = 0;
      int hash_type = LE.getuint32(req, pos); req_len -= 4; pos += 4;
      long file_offset = LE.getuint64(req, pos); req_len -= 8; pos += 8;
      int block_len = LE.getuint32(req, pos); req_len -= 4; pos += 4;
      if (debug) JFLog.log(String.format("block_hash:%d %d %d", hash_type, file_offset, block_len));
      try {
        file_io.seek(file_offset);
        if (file_io.getFilePointer() != file_offset) {
          throw new Exception("bad seek");
        }
        if (file_offset + block_len > file_io.length()) {
          throw new Exception("block beyond eof");
        }
        file_io.readFully(file_data, 0, block_len);
        switch (hash_type) {
          case HASH_MD5: {
            //hash(16)
            if (req_len != 16) throw new Exception("missing hash");
            System.arraycopy(req, pos, hash_md5, 0, 16);
            md5.init();
            md5.add(file_data, 0, block_len);
            byte[] res = md5.done();
            if (debug) print_hash(res);
            if (Arrays.compare(res, hash_md5) == 0) {
              write(RET_NO_CHANGE, null);
            } else {
              write(RET_CHANGE, null);
            }
            break;
          }
          case HASH_MD5_SEED: {
            //hash(16),seed(16)
            if (req_len != 32) throw new Exception("missing hash,seed");
            System.arraycopy(req, pos, hash_md5, 0, 16); pos += 16;
            md5.init();
            md5.add(req, pos, 16);  //random seed
            md5.add(file_data, 0, block_len);
            byte[] res = md5.done();
            if (debug) print_hash(res);
            if (Arrays.compare(res, hash_md5) == 0) {
              write(RET_NO_CHANGE, null);
            } else {
              write(RET_CHANGE, null);
            }
            break;
          }
          default: {
            throw new Exception("unknown hash");
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
        return error(RET_ERROR);
      }
      return true;
    }
    private boolean req_file_block_data() {
      //offset(8),data(?)
      if (file_io == null) return error(RET_ERROR);
      if (req_len < 9) return error(RET_ERROR);
      int pos = 0;
      long file_offset = LE.getuint64(req, pos); req_len -= 8; pos += 8;
      try {
        file_io.seek(file_offset);
        if (file_io.getFilePointer() != file_offset) {
          throw new Exception("seek failed");
        }
        file_io.write(req, pos, req_len);
        write(RET_OKAY, null);
      } catch (Exception e) {
        return error(RET_ERROR);
      }
      return true;
    }
    private boolean req_file_truncate() {
      if (file_io == null) return error(RET_ERROR);
      if (req_len != 8) return error(RET_ERROR);
      int pos = 0;
      long file_size = LE.getuint64(req, pos); req_len -= 8; pos += 8;
      try {
        file_io.setLength(file_size);
        file_size = file_io.length();
        write(RET_OKAY, null);
      } catch (Exception e) {
        return error(RET_ERROR);
      }
      return true;
    }
    private boolean req_file_create() {
      if (file_io != null) {
        file_close();
      }
      if (req_len < 1) return error(RET_ERROR);
      filename = LE.getString(req, 0, req_len);
      if (filename.contains("..") || filename.contains("/")) {
        return error(RET_ERROR);
      }
      String path = root + folder + filename;
      try {
        File file = new File(path);
        file_open(file);
        write(RET_OKAY, null);
      } catch (Exception e) {
        return error(RET_ERROR);
      }
      return true;
    }
    private boolean req_file_delete() {
      if (file_io != null) {
        file_close();
      }
      if (req_len < 1) return error(RET_ERROR);
      filename = LE.getString(req, 0, req_len);
      if (filename.contains("..") || filename.contains("/")) {
        return error(RET_ERROR);
      }
      String path = root + folder + filename;
      try {
        File file = new File(path);
        if (!file.exists()) {
          write(RET_NO_EXIST, null);
          return true;
        }
        if (file.delete()) {
          write(RET_OKAY, null);
        } else {
          write(RET_ERROR, null);
        }
      } catch (Exception e) {
        return error(RET_ERROR);
      }
      return true;
    }
    private boolean req_folder_open() {
      if (file_io != null) {
        file_close();
      }
      if (req_len < 1) return error(RET_ERROR);
      String new_folder = "/" + LE.getString(req, 0, req_len) + "/";
      if (new_folder.contains("..")) {
        return error(RET_ERROR);
      }
      File file = new File(root + new_folder);
      if (file.exists() && file.isDirectory()) {
        folder = new_folder;
        write(RET_OKAY, null);
      } else {
        return error(RET_ERROR);
      }
      return true;
    }
    private boolean req_folder_create() {
      if (file_io != null) {
        file_close();
      }
      if (req_len < 1) return error(RET_ERROR);
      String new_folder = "/" + LE.getString(req, 0, req_len) + "/";
      if (new_folder.contains("..")) {
        return error(RET_ERROR);
      }
      File file = new File(root + new_folder);
      if (file.mkdirs()) {
        folder = new_folder;
        write(RET_OKAY, null);
      } else {
        write(RET_ERROR, null);
      }
      return true;
    }
    private boolean req_folder_delete() {
      if (file_io != null) {
        file_close();
      }
      if (req_len < 1) return error(RET_ERROR);
      String sub_folder = "/" + LE.getString(req, 0, req_len) + "/";
      if (sub_folder.contains("..") || !sub_folder.startsWith("/") || !sub_folder.endsWith("/")) {
        return error(RET_ERROR);
      }
      File file = new File(root + sub_folder);
      if (!file.exists()) {
        return error(RET_NO_EXIST);
      }
      if (!file.isDirectory()) {
        return error(RET_ERROR);
      }
      if (file.delete()) {
        write(RET_OKAY, null);
      } else {
        write(RET_ERROR, null);
      }
      return true;
    }
    private boolean req_folder_list() {
      if (file_io != null) {
        file_close();
      }
      if (req_len < 1) return error(RET_ERROR);
      String sub_folder = LE.getString(req, 0, req_len);
      if (sub_folder.contains("..") || !sub_folder.startsWith("/") || !sub_folder.endsWith("/")) {
        return error(RET_ERROR);
      }
      File file = new File(root + sub_folder);
      if (file.exists() && file.isDirectory()) {
        StringBuilder list = new StringBuilder();
        File[] files = file.listFiles();
        for(File child : files) {
          String name = child.getName();
          if (child.isDirectory()) {
            list.append("/");
          }
          list.append(name);
          list.append("\n");
        }
        byte[] binlist = list.toString().getBytes();
        write(RET_OKAY, binlist);
      } else {
        return error(RET_ERROR);
      }
      return true;
    }
  }

  public static void main(String[] args) {
    FileSyncServer service = new FileSyncServer("password", "/tmp");
    service.start();
    while (true) {
      JF.sleep(1000);
    }
  }
}

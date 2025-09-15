package javaforce.utils;

/** FileSync
 *
 * Sync folder to remote server.
 *
 * Connects to javaforce.service.FileSyncServer
 *
 * Supported:
 *  - recursive sync
 *
 * Not supported:
 *  - mirror sync may be supported in the future
 *
 * @author pquiring
 */

import java.net.*;
import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.service.*;
import static javaforce.service.FileSyncServer.*;

public class FileSync {

  public static boolean debug = false;

  public static final int FLAG_DEFAULT = 0x00;
  public static final int FLAG_RECURSIVE = 0x01;
  public static final int FLAG_MIRROR = 0x02;

  private Socket s;
  private InputStream is;
  private OutputStream os;

  public boolean connect(String server) {
    return connect(server, KeyMgmt.getDefaultClient());
  }

  public boolean connect(String server, KeyMgmt keys) {
    try {
      s = new Socket(server, FileSyncServer.port);
      s = JF.connectSSL(s, keys);
      is = s.getInputStream();
      os = s.getOutputStream();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      s = null;
      is = null;
      os = null;
      return false;
    }
  }

  public boolean disconnect() {
    if (s == null) return true;
    try {s.close();} catch (Exception e) {}
    s = null;
    is = null;
    os = null;
    return true;
  }

  public boolean login(String passwd) {
    int strlen = passwd.length();
    int len = 4 + 4 + strlen;
    reset();
    write(REQ_AUTH);
    write(len);
    write(FileSyncServer.VERSION);
    write(AUTH_PASSWD);
    write(passwd.getBytes(), 0, strlen);
    try {
      if (debug) JFLog.log(String.format("c.cmd=%x len=%d (login)", req[0], req_len-5));
      os.write(req, 0, req_len);
      return read_reply() == RET_OKAY;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean sync(String src_folder, String dest_folder, int flags) {
    return sync(src_folder, null, dest_folder, flags);
  }
  public boolean sync(String src_folder, String[] src_files, String dest_folder, int flags) {
    if ((flags & FLAG_MIRROR) != 0) return false;  //not supported yet
    boolean recursive = (flags & FLAG_RECURSIVE) != 0;
    //using a random seed will ensure a double md5 failure is impossible
    random.setSeed(System.currentTimeMillis());
    random.nextBytes(seed);
    File src_folder_file = new File(src_folder);
    if (src_files == null) {
      src_files = src_folder_file.list();
    }
    if (!folder_open(dest_folder)) {
      if (!folder_create(dest_folder)) {
        return false;
      }
    }
    for(String filename : src_files) {
      if (debug) JFLog.log("file_sync:" + filename);
      if (filename.startsWith(".")) continue;
      File file = new File(src_folder + "/" + filename);
      if (!file.exists()) {
        if (debug) JFLog.log("File not found:" + filename);
        continue;
      }
      if (file.isDirectory()) {
        if (!recursive) continue;
        if (!sync(src_folder + "/" + filename, null, dest_folder + "/" + filename, flags)) {
          return false;
        }
        if (!folder_open(dest_folder)) return false;
      } else {
        if (!file_sync(src_folder, filename, file)) return false;
      }
    }
    return true;
  }

  private long file_date;
  private long file_size;
  private byte[] req = new byte[max_data_len + 16];
  private int req_len = 0;

  private long s_file_date;
  private long s_file_size;

  private byte[] reply = new byte[max_data_len + 16];
  private int reply_len = 0;
  private int reply_offset = 0;

  private MD5 md5 = new MD5();
  private Random random = new Random();
  private byte[] seed = new byte[16];

  private byte[] file_data = new byte[max_data_len];
  private long file_offset;

  private void reset() {
    req_len = 0;
  }
  private boolean write(byte b) {
    req[req_len++] = b;
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
    if (req_len + length > max_data_len) return false;
    System.arraycopy(buf, offset, req, req_len, length);
    req_len += length;
    return true;
  }

  private byte readByte() {
    if (reply_offset + 1 > reply_len) return -1;
    return reply[reply_offset++];
  }
  private short readShort() {
    if (reply_offset + 2 > reply_len) return -1;
    short value = (short)LE.getuint16(reply, reply_offset);
    reply_offset += 2;
    return value;
  }
  private int readInt() {
    if (reply_offset + 4 > reply_len) return -1;
    int value = LE.getuint32(reply, reply_offset);
    reply_offset += 4;
    return value;
  }
  private long readLong() {
    if (reply_offset + 8 > reply_len) return -1;
    long value = LE.getuint64(reply, reply_offset);
    reply_offset += 8;
    return value;
  }
  private boolean readBytes(byte[] out, int off, int len) {
    if (reply_offset + len > reply_len) return false;
    System.arraycopy(reply, reply_offset, out, off, len);
    reply_offset += len;
    return true;
  }
  /* returns cmd */
  private int read_reply() throws Exception {
    int cmd = is.read();
    if (cmd == -1) throw new Exception("read error");
    if (is.readNBytes(reply, 0, 4) != 4) throw new Exception("read error");
    reply_len = LE.getuint32(reply, 0);
    if (debug) JFLog.log(String.format("s.cmd=%x len=%d", cmd, reply_len));
    if (reply_len > 0) {
      if (is.readNBytes(reply, 0, reply_len) != reply_len) throw new Exception("read error");
    }
    reply_offset = 0;
    return cmd;
  }

  private boolean file_sync(String folder, String filename, File file) {
    file_date = file.lastModified();
    file_size = file.length();
    return file_open(folder, filename);
  }

  private boolean file_open(String folder, String filename) {
    //date,size,filename
    int strlen = filename.length();
    int len = 8 + 8 + strlen;
    reset();
    write(REQ_FILE_OPEN);
    write(len);
    write(file_date);
    write(file_size);
    write(filename.getBytes(), 0, strlen);
    try {
      if (debug) JFLog.log(String.format("c.cmd=%x len=%d (file_open:%s)", req[0], req_len-5, filename));
      os.write(req, 0, req_len);
      int cmd = read_reply();
      switch (cmd) {
        case RET_NO_EXIST: {
          //reply = null
          s_file_date = 0;
          s_file_size = 0;
          return file_sync_blocks(folder, filename);
        }
        case RET_CHANGE: {
          //reply = server side date,size
          if (reply_len != 16) throw new Exception("bad reply");
          s_file_date = readLong();
          s_file_size = readLong();
          return file_sync_blocks(folder, filename);
        }
        case RET_NO_CHANGE: {
          return true;
        }
        default: {
          throw new Exception(String.format("file_open:unknown reply:%x", cmd));
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  private boolean file_sync_blocks(String folder, String filename) {
    try {
      RandomAccessFile file_io = new RandomAccessFile(folder + "/" + filename, "r");
      file_offset = 0;
      while (file_offset < file_size) {
        int block_size = 1024*1024;  //1MB
        if (file_offset + block_size > file_size) {
          block_size = (int)(file_size - file_offset);
        }
        if (file_offset >= s_file_size) {
          //just send block
          file_io.readFully(file_data, 0, block_size);
          if (file_block_data(block_size) != RET_OKAY) return false;
          file_offset += block_size;
          continue;
        }
        if (file_offset + block_size > s_file_size) {
          //partial block
          block_size = (int)(s_file_size - file_offset);
        }
        file_io.readFully(file_data, 0, block_size);
        md5.init();
        md5.add(seed, 0, 16);
        md5.add(file_data, 0, block_size);
        int cmd = file_block_hash(block_size, md5.done());
        switch (cmd) {
          case RET_NO_CHANGE: {
            break;
          }
          case RET_CHANGE: {
            if (file_block_data(block_size) != RET_OKAY) return false;
            break;
          }
          default: {
            throw new Exception("unknown return cmd:" + cmd);
          }
        }
        file_offset += block_size;
      }
      file_io.close();
      if (s_file_size > file_size) {
        //truncate file
        if (file_truncate() != RET_OKAY) return false;
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Send file hash, returns server response cmd. */
  private int file_block_hash(int block_len, byte[] hash) throws Exception {
    if (debug) FileSyncServer.print_hash(hash);
    reset();
    //hash_type(4)file_offset(8),block_len(4),hash(16),seed(16)
    int len = 4 + 8 + 4 + 16 + 16;
    write(REQ_FILE_BLOCK_HASH);
    write(len);
    write(HASH_MD5_SEED);
    write(file_offset);
    write(block_len);
    write(hash, 0, 16);
    write(seed, 0, 16);
    if (debug) JFLog.log(String.format("block_hash:%d %d %d", HASH_MD5_SEED, file_offset, block_len));
    if (debug) JFLog.log(String.format("c.cmd=%x len=%d (file_block_hash)", req[0], req_len-5));
    os.write(req, 0, req_len);
    int cmd = read_reply();
    return cmd;
  }

  /** Send file data block. */
  private int file_block_data(int block_len) throws Exception {
    reset();
    //offset(8),data(?)
    int len = 8 + block_len;
    write(REQ_FILE_BLOCK_DATA);
    write(len);
    write(file_offset);
    write(file_data, 0, block_len);
    if (debug) JFLog.log(String.format("c.cmd=%x len=%d (file_block_data)", req[0], req_len-5));
    os.write(req, 0, req_len);
    int cmd = read_reply();
    return cmd;
  }

  private int file_truncate() throws Exception {
    reset();
    //size(8)
    int len = 8;
    write(REQ_FILE_TRUNCATE);
    write(len);
    write(file_size);
    if (debug) JFLog.log(String.format("c.cmd=%x len=%d (file_trunate)", req[0], req_len-5));
    os.write(req, 0, req_len);
    int cmd = read_reply();
    return cmd;
  }

  private boolean folder_open(String folder) {
    int strlen = folder.length();
    reset();
    //folder(?)
    int len = strlen;
    write(REQ_FOLDER_OPEN);
    write(len);
    write(folder.getBytes(), 0, strlen);
    try {
      if (debug) JFLog.log(String.format("c.cmd=%x len=%d (folder_open:%s)", req[0], req_len-5, folder));
      os.write(req, 0, req_len);
      int cmd = read_reply();
      return cmd == RET_OKAY;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private boolean folder_create(String folder) {
    int strlen = folder.length();
    reset();
    //folder(?)
    int len = strlen;
    write(REQ_FOLDER_CREATE);
    write(len);
    write(folder.getBytes(), 0, strlen);
    try {
      if (debug) JFLog.log(String.format("c.cmd=%x len=%d (folder_create:%s)", req[0], req_len-5, folder));
      os.write(req, 0, req_len);
      int cmd = read_reply();
      return cmd == RET_OKAY;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static void main(String[] args) {
    if (args.length < 4) {
      System.out.println("Usage:jfsync src_folder server password dest_folder [-r]");
      return;
    }
    String src_folder = args[0];
    String server = args[1];
    String password = args[2];
    String dst_folder = args[3];
    int flags = FLAG_DEFAULT;
    for(int i=4;i<args.length;i++) {
      String arg = args[i];
      String key, value;
      int idx = arg.indexOf('=');
      if (idx == -1) {
        key = arg;
        value = "";
      } else {
        key = arg.substring(0, idx);
        value = arg.substring(idx + 1);
      }
      switch (key) {
        case "-r": {
          flags |= FLAG_RECURSIVE;
          break;
        }
      }
    }
    FileSync sync = new FileSync();
    if (!sync.connect(server)) {
      JFLog.log("Error:connect failed!");
      return;
    }
    if (!sync.login(password)) {
      JFLog.log("Error:login failed!");
      return;
    }
    if (debug) JFLog.log("Login granted");
    if (!sync.sync(src_folder, dst_folder, flags)) {
      JFLog.log("sync failed!");
      System.exit(1);
    } else {
      JFLog.log("sync complete");
    }
  }
}

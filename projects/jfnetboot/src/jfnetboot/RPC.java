package jfnetboot;

/** RPC / PORTMAP / MOUNT / NFS Service.
 *
 * TCP Port 111
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

import javaforce.*;

public class RPC extends Thread {

  private ServerSocket ss;
  private boolean active;
  private static boolean debug = false;
  private static boolean debugRead = false;
  private static boolean debugWrite = false;
  private static boolean debugCreate = false;
  private static boolean debugLinks = false;

  public void run() {
    JFLog.log("RPC starting on port 111...");
    active = true;
    try {
      ss = new ServerSocket(111);
      while (active) {
        Socket s = ss.accept();
        RPCClient c = new RPCClient();
        c.s = s;
        c.start();
      }
    } catch (Exception e) {
      if (debug) JFLog.log(e);
    }
  }

  public void close() {
    active = false;
    if (ss != null) {
      try {ss.close();} catch (Exception e) {}
    }
  }

  public static class RPCClient extends Thread {
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public boolean active;
    public byte[] request = new byte[256];
    public int request_offset;
    public int request_size;
    public byte[] reply = new byte[256];
    public int reply_offset;
    public int reply_size;

    public void run() {
      active = true;
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        int total_read = 0;
        request_size = 4;
        while (active) {
          int read = is.read(request, total_read, request_size - total_read);
          if (read == -1) break;
          if (read > 0) {
            total_read += read;
            if (total_read == 4) {
              request_size = BE.getuint32(request, 0) & 0x7fffffff;
              request_size += 4;
              while (request_size > request.length) {
                request = new byte[request.length << 1];
              }
            }
            if (total_read == request_size) {
              request_offset = 0;
              reply_offset = 0;
              reply_size = 0;
              writeInt(0);  //packet size (patch later)
              processRequest();
              total_read = 0;
              request_size = 4;
            }
          }
        }
        close();
      } catch (Exception e) {
        if (debug) JFLog.log(e);
        close();
      }
    }

    public void close() {
      active = false;
      if (s != null) {
        try {s.close();} catch (Exception e) {}
        s = null;
      }
    }

    private void reply_grow(int size) {
      while (reply.length < reply_size + size) {
        byte[] new_reply = new byte[reply.length << 1];
        System.arraycopy(reply, 0, new_reply, 0, reply.length);
        reply = new_reply;
      }
    }

    private void writeInt(int value) {
      reply_grow(4);
      BE.setuint32(reply, reply_offset, value);
      reply_offset += 4;
      reply_size += 4;
    }

    private void writeLong(long value) {
      reply_grow(8);
      BE.setuint64(reply, reply_offset, value);
      reply_offset += 8;
      reply_size += 8;
    }

    private void writeBytes(byte[] data, int offset, int length) {
      reply_grow(length);
      System.arraycopy(data, offset, reply, reply_offset, length);
      //pad to ints
      length += 3;
      length &= 0x7ffffffc;
      reply_offset += length;
      reply_size += length;
    }

    private void writeString(String str) {
      int len = str.length();
      writeInt(len);
      writeBytes(str.getBytes(), 0, len);
    }

    private void writeHandle(CHandle handle) {
      writeInt(32);  //size in bytes (8 ints) (4 longs)
      writeLong(0);
      writeLong(0);
      writeInt(handle.serial);
      writeInt(handle.arch);
      writeLong(handle.handle);
    }

    private void writeObj(CHandle handle) {
      writeInt(1);  //handle_follow
      writeHandle(handle);
    }

    private static final int TYPE_REG = 1;  //regular file
    private static final int TYPE_DIR = 2;  //directory
    private static final int TYPE_BLK = 3;  //block
    private static final int TYPE_CHR = 4;  //character
    private static final int TYPE_LNK = 5;  //sym link
    private static final int TYPE_SOCK = 6;  //socket
    private static final int TYPE_FIFO = 7;  //fifo

    private void writeObjAttr(CHandle handle) {
      int type;
      NHandle nhandle = handle.fs.getHandle(handle.handle);
      if (nhandle.symlink != null) {
        type = TYPE_LNK;
      } else if (nhandle.isFolder()) {
        type = TYPE_DIR;
      } else {
        type = TYPE_REG;
      }
      String local = getLocalPath(handle);
      int mode = FileOps.getMode(local);
      if (handle.handle == handle.fs.getRootHandle()) {
        mode |= 0x4000;  //???
      }
      long size = getFileSize(handle);

      writeInt(type);  //file type
      writeInt(mode);  //file mode
      writeInt(FileOps.getNLink(local));  //nlink (hard links)
      writeInt(FileOps.getUID(local));  //uid
      writeInt(FileOps.getGID(local));  //gid
      writeLong(size);  //size
      writeLong(size);  //used (actual space used on disk)
      writeLong(0);  //rdev
      writeLong(0x12345678);  //fsid (file system id)
      writeLong(handle.handle);  //fileid
      writeTime(FileOps.getATime(local));  //atime
      writeTime(FileOps.getMTime(local));  //mtime
      writeTime(FileOps.getCTime(local));  //ctime
    }

    private void writeBefore(CHandle handle) {
      String local = getLocalPath(handle);
      long size = getFileSize(handle);
      writeInt(1);  //follows
      writeLong(size);  //file size
      writeTime(FileOps.getMTime(local));  //mtime
      writeTime(FileOps.getCTime(local));  //ctime
    }

    private void writeAfter(CHandle handle) {
      writeInt(1);  //follows
      writeObjAttr(handle);
    }

    private void writeTime(UnixTime ut) {
      writeInt(ut.secs);
      writeInt(ut.nsecs);
    }

    public int readInt() {
      int value = BE.getuint32(request, request_offset);
      request_offset += 4;
      return value;
    }

    public long readLong() {
      long value = BE.getuint64(request, request_offset);
      request_offset += 8;
      return value;
    }

    private String readString() {
      int len = readInt();
      String str = new String(request, request_offset, len);
      //padding
      len += 3;
      len &= 0x7ffffffc;
      request_offset += len;
      return str;
    }

    private byte[] readBytes(int len) {
      byte[] bs = new byte[len];
      System.arraycopy(request, request_offset, bs, 0, len);
      //padding
      len += 3;
      len &= 0x7ffffffc;
      request_offset += len;
      return bs;
    }

    private CHandle readHandle() {
      int len = readInt();
      if (len != 32) {
        if (debug) JFLog.log("RPC:NFS:Bad handle length");
        return null;
      }
      readLong();
      readLong();
      int serial = readInt();
      int arch = readInt();
      long handle = readLong();
      return new CHandle(serial, arch, handle);
    }

    private void processRequest() {
      int frag_len = readInt();
      int req_xid = readInt();
      int req_msg_type = readInt();
      if (req_msg_type != 0) {
        if (debug) JFLog.log("RPC:Error:Packet not a request");
        return;
      }
      int rpcver = readInt();  // == 2
      if (rpcver != 2) {
        if (debug) JFLog.log("RPC:Error:rpcver != 2");
        return;
      }
      int req_prog = readInt();
      int req_prog_ver = readInt();
      int req_proc = readInt();
      int cred_flavor = readInt();
      int cred_length = readInt();
      while (cred_length > 0) {
        readInt();
        cred_length -= 4;
      }
      int verify_flavor = readInt();
      int verify_length = readInt();
      while (verify_length > 0) {
        readInt();
        verify_length -= 4;
      }

      // write RPC header
      writeInt(req_xid);  //echo XID
      writeInt(1);  //msg_type = 1 (REPLY)
      writeInt(0);  //state = 0 (SUCCESS)
      writeInt(0);  //auth type
      writeInt(0);  //auth length
      writeInt(0);  //rpc executed = 0 (SUCCESS)

      switch (req_prog) {
        case 100000:
          //portmap
          portmap(req_proc);
          break;
        case 100003:
          //NFS
          nfs(req_proc);
          break;
        case 100005:
          //mount
          mount(req_proc);
          break;
        case 100227:
          nfsacl(req_proc);
          break;
        default:
          if (debug) JFLog.log("RPC:Error:Unkown prog request:" + req_prog);
          break;
      }
      if (reply_size <= 4) return;
      try {
        //packets must be multiple of ints
        reply_size += 3;
        reply_size &= 0x7ffffffc;
        //patch packet length
        BE.setuint32(reply, 0, 0x80000000 | (reply_size-4));
        os.write(reply, 0, reply_size);
        os.flush();
      } catch (Exception e) {
        if (debug) JFLog.log(e);
      }
    }

    private void portmap(int req_proc) {
      switch (req_proc) {
        case 0:  //NULL
          break;
        case 3:  //GETPORT
          int req_program = readInt();  //program id
          int req_ver = readInt();  //service version
          int req_proto = readInt();  //proto : TCP=6
          int req_port = readInt();  //port = 0
          switch (req_program) {
            case 100003:  //NFS
              writeInt(111);  //port == 111 (self) (usually 2049)
              break;
            case 100005:  //MOUNT
              writeInt(111);  //port == 111 (self)
              break;
            default:
              if (debug) JFLog.log("RPC:Error:Unknown portmap program:" + req_program);
              break;
          }
          break;
        default:
          if (debug) JFLog.log("RPC:Error:Unkown portmap request:" + req_proc);
          break;
      }
    }

    private void mount(int req_proc) {
      switch (req_proc) {
        case 0: { //NULL
          break;
        }
        case 1: {  //MNT
          String mnt = readString();
          if (!mnt.startsWith("/nfs/")) {
            if (debug) JFLog.log("RPC:mount:Invalid path:" + mnt);
          }
          String[] p = mnt.split("/");
          // 0 / 1=serial / 2=arch
          int serial = Long.valueOf(p[1], 16).intValue();
          int arch = Arch.toInt(p[2]);
          writeInt(0);  //mount = 0 (SUCCESS)
          writeHandle(getRootHandle(serial, arch));
          writeInt(1);  //flavors = 1
          writeInt(1);  //AUTH_UNIX = 1
          break;
        }
        case 3: {  //UMNT
          String mnt = readString();
          writeInt(0);  //unmount = 0 (SUCCESS)
          break;
        }
        case 4: {  //UMNTALL
          writeInt(0);  //unmountall = 0 (SUCCESS)
          break;
        }
        default: {
          if (debug) JFLog.log("RPC:Error:Unkown mount request:" + req_proc);
          break;
        }
      }
    }

    //see https://tools.ietf.org/html/rfc1813#section-2.5 for complete listing
    private static final int ERR_SUCCESS = 0;
    private static final int ERR_PERM = 1;
    private static final int ERR_NOENT = 2;
    private static final int ERR_IO = 5;
    private static final int ERR_EXIST = 17;
    private static final int ERR_STALE = 70;

    private static final int ERR_BAD_HANDLE = 10001;
    private static final int ERR_NOT_SYNC = 10002;
    private static final int ERR_BAD_COOKIE = 10003;

    private static final int CREATE_MODE_UNCHECKED = 0;
    private static final int CREATE_MODE_GUARDED = 1;
    private static final int CREATE_MODE_EXCLUSIVE = 3;

    private static final int ACCESS_READ    = 0x0001;
    private static final int ACCESS_LOOKUP  = 0x0002;
    private static final int ACCESS_MODIFY  = 0x0004;
    private static final int ACCESS_EXTEND  = 0x0008;
    private static final int ACCESS_DELETE  = 0x0010;
    private static final int ACCESS_EXECUTE = 0x0020;

    private void nfs(int req_proc) {
      switch (req_proc) {
        case 0:  //NULL
          break;
        case 1: {  //GETATTR
          CHandle handle = readHandle();
          NHandle nhandle = handle.fs.getHandle(handle.handle);
          if (nhandle == null || !nhandle.exists()) {
            if (debug) JFLog.log("GETATTR:Not Found:" + handle);
            writeInt(ERR_NOENT);
            break;
          }
          if (debug) JFLog.log("GETATTR:" + handle + ":" + getLocalPath(handle));
          writeInt(ERR_SUCCESS);
          writeObjAttr(handle);
          break;
        }
        case 2: {  //SETATTR
          CHandle handle = readHandle();
          FileAttrs fa = readAttrs();
          NHandle nhandle = handle.fs.getHandle(handle.handle);
          boolean exists = nhandle != null && nhandle.exists();
          if (exists) {
            if (debug) JFLog.log("SETATTR:" + handle + ":" + getLocalPath(handle));
            writeInt(ERR_SUCCESS);
            writeBefore(handle);
          } else {
            if (debug) JFLog.log("SETATTR:Not Found:" + handle);
            writeInt(ERR_NOENT);
            writeInt(0);  //before
          }
          if (exists) {
            setattr(handle, fa);
          }
          // read guard
          int has_guard = readInt();
          if (has_guard != 0) {
            int sec = readInt();
            int nsec = readInt();
            if (exists) {
              //handle.fs.setattr_guard(handle.handle, guard);  //TODO
            }
          }
          if (exists) {
            writeAfter(handle);
          } else {
            writeInt(0);  //after
          }
          break;
        }
        case 3: {  //LOOKUP
          CHandle dir = readHandle();
          String name = readString();
          if (!dir.fs.exists(dir.handle, name)) {
            if (debug) JFLog.log("LOOKUP:ERR_NOENT:" + getPath(dir) + "/" + name + ":" + dir);
            writeInt(ERR_NOENT);
            writeInt(0);  //dir attr : no follows
            break;
          }
          CHandle child = getHandle(dir, name);
          if (child == null) {
            if (debug) JFLog.log("LOOKUP:ERR_NOENT:" + getPath(dir) + "/" + name + ":" + dir);
            writeInt(ERR_NOENT);
            writeInt(0);  //dir attr : no follows
            break;
          }
          if (debug) JFLog.log("LOOKUP:" + dir + ":" + child + ":" + getLocalPath(child));
          writeInt(ERR_SUCCESS);
          writeHandle(child);
          writeInt(1);  //value follows = 1
          writeObjAttr(child);  //obj attr
          writeInt(1);  //value follows = 1
          writeObjAttr(dir);  //dir attr
          break;
        }
        case 4: {  //ACCESS
          CHandle handle = readHandle();
          int flags = readInt();

          if (!handle.fs.exists(handle.handle)) {
            if (debug) JFLog.log("ACCESS:Not Found:" + handle);
            writeInt(ERR_NOENT);
            writeInt(0);  //no follows
            break;
          }
          if (debug) JFLog.log("ACCESS:" + handle + ":" + getLocalPath(handle));
          writeInt(ERR_SUCCESS);
          writeInt(1);  //value follows = 1
          writeObjAttr(handle);
          writeInt(flags);  //access rights
          break;
        }
        case 5: {  //READLINK
          CHandle handle = readHandle();
          NHandle nhandle = handle.fs.getHandle(handle.handle);

          if (nhandle == null || !nhandle.exists()) {
            if (debug) JFLog.log("READLINK:Not Found:" + handle);
            writeInt(ERR_NOENT);
            writeInt(0);  //no follows
            break;
          }
          String path = nhandle.symlink;
          if (debug) JFLog.log("READLINK:" + handle + ":" + getLocalPath(handle) + " -> " + path);
          writeInt(ERR_SUCCESS);
          writeInt(1);  //follows
          writeObjAttr(handle);
          writeString(path);
          break;
        }
        case 6: {  //READ
          CHandle handle = readHandle();
          long off = readLong();
          int cnt = readInt();

          if (!handle.fs.exists(handle.handle)) {
            if (debugRead) JFLog.log("READ:Not Found:" + handle + ":off=" + off + ":cnt=" + cnt);
            writeInt(ERR_NOENT);
            writeInt(0);  //no follows
            break;
          }

          if (debugRead) JFLog.log("READ:" + handle + ":" + getLocalPath(handle) + ":off=" + off + ":cnt=" + cnt);
          writeInt(ERR_SUCCESS);
          writeInt(1);  //value follows = 1
          writeObjAttr(handle);  //file_attr
          doReadFile(handle, off, cnt);
          break;
        }
        case 7: {  //WRITE
          CHandle handle = readHandle();
          long off = readLong();
          int cnt = readInt();
          int stable = readInt();  //stable_how
          int len = readInt();
          if (cnt != len) {
            if (debugWrite) JFLog.log("WRITE:size mismatch:" + handle + ":off=" + off + ":cnt=" + cnt);
            writeInt(ERR_IO);
            writeInt(0);  //before
            writeInt(0);  //after
            break;
          }
          byte[] data = readBytes(len);
          if (!handle.fs.exists(handle.handle)) {
            if (debugWrite) JFLog.log("WRITE:Not Found:" + handle + ":off=" + off + ":cnt=" + cnt);
            writeInt(ERR_NOENT);
            writeInt(0);  //before
            writeInt(0);  //after
            break;
          }

          if (debugWrite) JFLog.log("WRITE:" + handle + ":" + getLocalPath(handle) + ":off=" + off + ":cnt=" + cnt);
          writeInt(ERR_SUCCESS);  //status
          writeBefore(handle);
          doWriteFile(handle, off, cnt, data);
          writeAfter(handle);
          writeInt(cnt);  //count
          writeInt(stable);  //stable_how (committed)
          writeLong(0);  //verifier
          break;
        }
        case 8: { //CREATE
          CHandle dir = readHandle();
          String name = readString();
          int create_mode = readInt();

          if (!dir.fs.exists(dir.handle)) {
            writeInt(ERR_NOENT);
            writeInt(0);  //dir before
            writeInt(0);  //dir after
            break;
          }

          long fs_handle = dir.fs.create(dir.handle, name);
          if (debugCreate) JFLog.log("CREATE:" + dir + ":" + getLocalPath(dir) + "/" + name + ":" + Long.toUnsignedString(fs_handle, 16));

          if (fs_handle != -1) {
            CHandle handle = new CHandle(dir.serial, dir.arch, fs_handle);

            switch (create_mode) {
              case 0:  //unchecked
                //no break
              case 1:  //guarded
                //new file attributes
                setattr(handle, readAttrs());
                break;
              case 2:  //exclusive
                long verf = readLong();
                break;
            }

            writeInt(ERR_SUCCESS);
            writeInt(1);  //value follows = 1
            writeHandle(handle);
            writeInt(1);  //value follows = 1
            writeObjAttr(handle);
            writeBefore(dir);
            writeAfter(dir);
          } else {
            switch (create_mode) {
              case 0:  //unchecked
                //no break
              case 1:  //guarded
                //new file attributes
                readAttrs();
                break;
              case 2:  //exclusive
                long verf = readLong();
                break;
            }
            writeInt(ERR_NOENT);
            writeInt(0);  //dir before
            writeInt(0);  //dir after
          }
          break;
        }
        case 9: {  //MKDIR
          CHandle dir = readHandle();
          String name = readString();
          FileAttrs fa = readAttrs();

          if (!dir.fs.exists(dir.handle)) {
            if (debug) JFLog.log("MKDIR:Not Found:" + dir);
            writeInt(ERR_NOENT);
            writeInt(0);  //dir before
            writeInt(0);  //dir after
            break;
          }

          long fs_handle = dir.fs.mkdir(dir.handle, name);
          if (debug) JFLog.log("MKDIR:" + dir + ":" + getLocalPath(dir) + "/" + name + ":" + Long.toUnsignedString(fs_handle));

          if (fs_handle != -1) {
            writeInt(ERR_SUCCESS);
            CHandle handle = new CHandle(dir.serial, dir.arch, fs_handle);
            setattr(handle, fa);
            writeInt(1);  //follows
            writeHandle(handle);
            writeInt(1);  //follows
            writeObjAttr(handle);
            writeBefore(dir);
            writeAfter(dir);
          } else {
            writeInt(ERR_NOENT);
            writeInt(0);  //dir before
            writeInt(0);  //dir after
          }
          break;
        }
        case 10: {  //SYMLINK
          CHandle where = readHandle();
          String where_name = readString();
          FileAttrs fa = readAttrs();
          String to = readString();

          if (!where.fs.exists(where.handle)) {
            if (debug) JFLog.log("SYMLINK:Not Found:" + where);
            writeInt(ERR_NOENT);
            writeInt(0);  //where before
            writeInt(0);  //where after
            break;
          }

          long new_handle = where.fs.create_symlink(where.handle, where_name, to);

          if (new_handle != -1) {
            if (debugLinks) JFLog.log("SYMLINK:" + where + ":" + getLocalPath(where) + "/" + where_name + " -> " + to);
            writeInt(ERR_SUCCESS);
            CHandle handle = new CHandle(where.serial, where.arch, new_handle);
            setattr(handle, fa);
            writeInt(1);  //follows
            writeHandle(handle);
            writeInt(1);  //follows
            writeObjAttr(handle);
            writeBefore(where);
            writeAfter(where);
          } else {
            if (debugLinks) JFLog.log("SYMLINK:Error:" + where + ":" + getLocalPath(where) + "/" + where_name + " -> " + to);
            writeInt(ERR_NOENT);
            writeInt(0);  //where before
            writeInt(0);  //where after
          }
          break;
        }
        case 12: {  //REMOVE
          CHandle dir = readHandle();
          String name = readString();

          if (!dir.fs.exists(dir.handle)) {
            if (debug) JFLog.log("REMOVE:Not Found:" + dir);
            writeInt(ERR_NOENT);
            writeInt(0);  //dir before
            writeInt(0);  //dir after
            break;
          }
          if (debugWrite) JFLog.log("REMOVE:" + dir + ":" + getLocalPath(dir) + "/" + name);

          boolean ok = dir.fs.remove(dir.handle, name);

          if (ok) {
            writeInt(ERR_SUCCESS);
            writeBefore(dir);
            writeAfter(dir);
          } else {
            writeInt(ERR_NOENT);
            writeInt(0);  //where before
            writeInt(0);  //where after
          }
          break;
        }
        case 13: {  //RMDIR
          CHandle dir = readHandle();
          String name = readString();

          if (!dir.fs.exists(dir.handle)) {
            if (debug) JFLog.log("RMDIR:Not Found:" + dir);
            writeInt(ERR_NOENT);
            writeInt(0);  //dir before
            writeInt(0);  //dir after
            break;
          }
          if (debug) JFLog.log("RMDIR:" + dir + ":" + getLocalPath(dir) + "/" + name);

          dir.fs.rmdir(dir.handle, name);

          writeInt(ERR_SUCCESS);
          writeBefore(dir);
          writeAfter(dir);
          break;
        }
        case 14: {  //RENAME
          CHandle from_dir = readHandle();
          String from_name = readString();
          CHandle to_dir = readHandle();
          String to_name = readString();

          if (!from_dir.fs.exists(from_dir.handle)) {
            if (debug) JFLog.log("RENAME:from_dir not found:" + from_dir);
            writeInt(ERR_NOENT);
            writeInt(0);  //from before
            writeInt(0);  //from after
            writeInt(0);  //to before
            writeInt(0);  //to after
            break;
          }

          if (!to_dir.fs.exists(to_dir.handle)) {
            if (debug) JFLog.log("RENAME:to_dir not found:" + to_dir);
            writeInt(ERR_NOENT);
            writeInt(0);  //from before
            writeInt(0);  //from after
            writeInt(0);  //to before
            writeInt(0);  //to after
            break;
          }

          if (debugWrite) JFLog.log("RENAME:" + from_dir + ":" + getLocalPath(from_dir) + "/" + from_name + " to " + to_dir + ":" + getLocalPath(to_dir) + "/" + to_name);
          boolean ok = from_dir.fs.rename(from_dir.handle, from_name, to_dir.handle, to_name);

          writeInt(ok ? ERR_SUCCESS : ERR_EXIST);
          writeBefore(from_dir);
          writeAfter(from_dir);
          writeBefore(to_dir);
          writeAfter(to_dir);
          break;
        }
        case 15: {  //LINK
          CHandle target = readHandle();  //existing file (target)
          CHandle where_dir = readHandle();  //new link dir
          String where_name = readString();  //new link name
          //create hard link from link_dir/link_name to file
          if (debugLinks) JFLog.log("LINK:" + where_dir + ":" + getLocalPath(where_dir) + "/" + where_name + " to " + target + ":" + getLocalPath(target));
          boolean ok;

          //BUG : link may have issues - a derived file system would make multiple copies of file (this bug is benign and would just waste disk space)
          ok = where_dir.fs.create_link(target.handle, where_dir.handle, where_name);

          writeInt(ok ? ERR_SUCCESS : ERR_IO);
          writeInt(1);  //follows
          writeObjAttr(target);
          writeBefore(where_dir);
          writeAfter(where_dir);
          break;
        }
        case 16:  //READDIR
          //no break
        case 17: {  //READDIRPLUS
          boolean plus = req_proc == 17;
          CHandle dir = readHandle();
          long cookie = readLong();
          long cookieverf = readLong();
          int count = readInt();
          int max;
          if (plus) {
            max = readInt();
          } else {
            max = 0x7fff;
          }
          if (debug) JFLog.log("READDIR:folder=" + dir + ":" + getLocalPath(dir));

          writeInt(ERR_SUCCESS);
          writeInt(1);  //follows = 1
          writeObjAttr(dir);
          writeLong(0x1234);  //cookieverf to restart this request (not used)
          FileSystem fs = Clients.getClient(dir.serial, Arch.toString(dir.arch)).getFileSystem();
          long[] files = fs.getAllFiles(dir.handle);
          boolean done = cookie >= files.length;
          for(int a=(int)cookie;a<files.length;a++) {
            long file = files[a];
            NHandle nhandle = dir.fs.getHandle(file);
            if (nhandle == null) {
              JFLog.log("BUG:getAllFiles() returned a null entry");
              continue;
            }
            if (debug) JFLog.log("READIR:entry=" + nhandle.local + ":" + nhandle.toString());
            writeInt(1);  //follows
            writeLong(file);  //fileid
            if (a == 0) {
              writeString(".");
            } else if (a == 1) {
              writeString("..");
            } else {
              writeString(fs.getName(file));  //filename
            }
            writeLong(a+1);  //cookie to restart from this entry
            if (plus) {
              CHandle handle = new CHandle(dir.serial, dir.arch, file);
              writeInt(1);  //follows
              writeObjAttr(handle);
              writeInt(1);  //follows
              writeHandle(handle);
            }
            if (a == files.length - 1) {
              done = true;
            }
            if (reply_size > count) {
              break;
            }
          }
          writeInt(0);  //no follows
          writeInt(done ? 1 : 0);  //eof
          break;
        }
        case 18: {  //FSSTAT
          CHandle handle = readHandle();
          if (debug) JFLog.log("FSSTAT:" + handle + ":" + getLocalPath(handle));

          writeInt(ERR_SUCCESS);
          writeInt(1);  //value follows = 1
          writeObjAttr(handle);
          writeLong(0x40000000);  //total bytes
          writeLong(0x20000000);  //free bytes
          writeLong(0x20000000);  //avail free bytes
          writeLong(0x2000);  //total file slots
          writeLong(0x1000);  //free file slots
          writeLong(0x1000);  //avail free file slots
          writeInt(0);  //invarsec
          break;
        }
        case 19: {  //FSINFO
          CHandle handle = readHandle();
          if (debug) JFLog.log("FSINFO:" + handle + ":" + getLocalPath(handle));
          boolean exists = handle.fs.exists(handle.handle);

          if (exists) {
            writeInt(ERR_SUCCESS);
            writeInt(0);  //value follows = 0
//            writeObjAttr(handle);
            writeInt(32768);  //rtmax
            writeInt(32768);  //rtpref
            writeInt(1);  //rtmult
            writeInt(32768);  //wtmax
            writeInt(32768);  //wtpref
            writeInt(1);  //wtmult
            writeInt(1024);  //dtpref
            writeLong(0x7fffffffffffffffL);  //maxfilesize
            writeInt(1);  //time delta : seconds
            writeInt(0);  //time delta : nano seconds
            writeInt(0x1b);  //props bits
          } else {
            writeInt(ERR_BAD_HANDLE);
            writeInt(0);  //value follows = 0
          }
          break;
        }
        case 20: {  //PATHCONF
          CHandle handle = readHandle();
          if (debug) JFLog.log("PATHCONF:" + handle + ":" + getLocalPath(handle));

          writeInt(ERR_SUCCESS);
          writeInt(1);  //value follows = 1
          writeObjAttr(handle);
          writeInt(32767);  //linkmax
          writeInt(255);  //name_max
          writeInt(1);  //no_trunc
          writeInt(0);  //chown_restrict
          writeInt(0);  //case_insensitive
          writeInt(1);  //case_preserving
          break;
        }
        case 21: {  //COMMIT
          CHandle handle = readHandle();
          long offset = readLong();
          int count = readInt();
          String path = getLocalPath(handle);
          if (debug) JFLog.log("COMMIT:" + handle + ":" + path);
          boolean ok = path != null;

          writeInt(ok ? ERR_SUCCESS : ERR_IO);
          if (path == null) {
            handle = getRootHandle(handle.serial, handle.arch);
          }
          writeBefore(handle);
          writeAfter(handle);
          if (ok) {
            writeLong(0);  //verifier
          }
          break;
        }
        default: {
          if (debug) JFLog.log("RPC:NFS:Error:Unknown proc:" + req_proc);
          break;
        }
      }
    }

    private void nfsacl(int req_proc) {
      switch (req_proc) {
        case 0:  //NULL
          break;
        case 1: {  //GETACL
          CHandle handle = readHandle();
          int mask = readInt();

          writeInt(ERR_SUCCESS);
          writeInt(0);  //attr no follows = 0
          writeInt(mask);  //mask (echo back)
          writeInt(0);  //acl count
          writeInt(0);  //total acl entries
          writeInt(0);  //default acl count
          writeInt(0);  //total default acl entries
          break;
        }
        case 2: {  //SETACL
          CHandle handle = readHandle();
          int mask = readInt();
          int cnt = readInt();
          int cnt2 = readInt();
          for(int a=0;a<cnt2;a++) {
            int type = readInt();
            int uid = readInt();
            int perms = readInt();
          }
          int defcnt = readInt();
          int defcnt2 = readInt();
          for(int a=0;a<defcnt2;a++) {
            int type = readInt();
            int uid = readInt();
            int perms = readInt();
          }

          writeInt(ERR_SUCCESS);
          writeInt(0);  //follows : 0 = none
          break;
        }
        default:
          if (debug) JFLog.log("RPC:NFSACL:Error:Unknown proc:" + req_proc);
          break;
      }
    }

    private CHandle getRootHandle(int serial, int arch) {
      FileSystem fs = Clients.getClient(serial, Arch.toString(arch)).getFileSystem();
      return new CHandle(serial, arch, fs.getRootHandle());
    }

    private CHandle getHandle(CHandle dir, String path) {
      FileSystem fs = Clients.getClient(dir.serial, Arch.toString(dir.arch)).getFileSystem();
      long handle = fs.getHandle(dir.handle, path);
      if (handle == -1) return null;
      return new CHandle(dir.serial, dir.arch, handle);
    }

    private String getPath(CHandle handle) {
      return handle.fs.getPath(handle.handle);
    }

    private String getLocalPath(CHandle handle) {
      return handle.fs.getLocalPath(handle.handle);
    }

    private long getFileSize(CHandle chandle) {
      NHandle nhandle = chandle.fs.getHandle(chandle.handle);
      if (nhandle == null) {
        return 0;
      }
      if (nhandle.isFolder()) {
        return 4096;
      }
      if (nhandle.symlink != null) {
        return nhandle.symlink.length();
      }
      NFile file = (NFile)nhandle;
      return new File(file.local).length();
    }

    private RandomAccessFile openFile(CHandle handle, boolean write) {
      NFile file = handle.fs.getFile(handle.handle);
      if (file == null) {
        return null;
      }
      return handle.fs.openFile(file, write);
    }

    private void doReadFile(CHandle handle, long offset, int length) {
      RandomAccessFile raf = openFile(handle, false);
      try {
        long size = raf.length();
        byte[] data = new byte[length];
        raf.seek(offset);
        int read = raf.read(data);
        if (read == -1) read = 0;
        long pos = raf.getFilePointer();
        writeInt(read);  //count
        writeInt(pos == size ? 1 : 0);  //eof
        writeInt(read);  //data.length
        writeBytes(data, 0, read);
      } catch (Exception e) {
        if (debug) JFLog.log(e);
        writeInt(0);  //count
        writeInt(0);  //eof
        writeInt(0);  //data.length
      }
    }

    private int doWriteFile(CHandle handle, long offset, int length, byte[] data) {
      RandomAccessFile raf = openFile(handle, true);
      try {
        raf.seek(offset);
        raf.write(data);
        return length;
      } catch (Exception e) {
        if (debug) JFLog.log(e);
        return 0;
      }
    }

    private FileAttrs readAttrs() {
      FileAttrs fa = new FileAttrs();
      // read attributes changes (sattr3)
      int has_mode = readInt();
      if (has_mode != 0) {
        fa.mode = readInt();
      }
      int has_uid = readInt();
      if (has_uid != 0) {
        fa.uid = readInt();
      }
      int has_gid = readInt();
      if (has_gid != 0) {
        fa.gid = readInt();
      }
      int has_size = readInt();
      if (has_size != 0) {
        fa.size = readLong();
      }
      int has_atime = readInt();
      //has_atime = 0 (no value)
      //has_atime = 1 (use server time)
      if (has_atime > 0) {
        int secs, nsecs;
        if (has_atime == 1) {
          fa.atime = System.currentTimeMillis();
        } else {
          secs = readInt();
          nsecs = readInt();
          fa.atime = secs * 1000L;  // + nsecs / 1000000L;
        }
      }
      int has_mtime = readInt();
      //has_mtime = 0 (no value)
      //has_mtime = 1 (use server time)
      if (has_mtime > 0) {
        int secs, nsecs;
        if (has_mtime == 1) {
          fa.mtime = System.currentTimeMillis();
        } else {
          secs = readInt();
          nsecs = readInt();
          fa.mtime = secs * 1000L;  // + nsecs / 1000000L;
        }
      }
      return fa;
    }

    private void setattr(CHandle handle, FileAttrs fa) {
      // read attributes changes (sattr3)
      if (fa.mode != -1) {
        if (debug) JFLog.log("SETATTR:" + handle + ":mode=0" + Integer.toString(fa.mode, 8));
        handle.fs.setattr_mode(handle.handle, fa.mode);
      }
      if (fa.uid != -1) {
        if (debug) JFLog.log("SETATTR:" + handle + ":uid=" + fa.uid);
        handle.fs.setattr_uid(handle.handle, fa.uid);
      }
      if (fa.gid != -1) {
        if (debug) JFLog.log("SETATTR:" + handle + ":gid=" + fa.gid);
        handle.fs.setattr_gid(handle.handle, fa.gid);
      }
      if (fa.size != -1) {
        if (debug) JFLog.log("SETATTR:" + handle + ":size=" + fa.size);
        handle.fs.setattr_size(handle.handle, fa.size);
      }
      if (fa.atime != -1) {
        handle.fs.setattr_atime(handle.handle, fa.atime);
      }
      if (fa.mtime != -1) {
        handle.fs.setattr_mtime(handle.handle, fa.mtime);
      }
    }
  }
}

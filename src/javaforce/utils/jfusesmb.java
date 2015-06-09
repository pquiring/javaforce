package javaforce.utils;

/**
 * Fuse SMB/CIFS
 *
 * Created : Nov 2, 2013
 */

import java.io.*;

import jcifs.smb.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.jni.lnx.*;

public class jfusesmb extends Fuse {
  private NtlmPasswordAuthentication auth;
  private SmbFile baseFile;
  private String user, server, share;  //user@server/share
  private String base;

  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage : jfuse-smb user@server/share mount");
      return;
    }
    new jfusesmb().main2(args);
  }

  public boolean auth(String args[], String passwd) {
    try {
      if (args[0].startsWith("smb://")) {
        args[0] = args[0].substring(6);
      }
      int idx1 = args[0].indexOf("@");
      int idx2 = args[0].indexOf("/");
      if (idx1 == -1 || idx2 == -1) throw new Exception("Usage : jfuse-smb user@server/share mount");
      user = args[0].substring(0, idx1);
      server = args[0].substring(idx1+1, idx2);
      share = args[0].substring(idx2+1);
      base = "smb://" + server + "/" + share;
      auth = new NtlmPasswordAuthentication("domain", user, passwd);
      baseFile = new SmbFile(base + "/", auth);
      baseFile.listFiles();
      return true;
    } catch (Exception e) {
      JFLog.log("Error:" + e);
      return false;
    }
  }

  public void main2(String args[]) {
    try {
      //auth first
      System.out.print("Password:");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String pass = br.readLine();
      if (!auth(args, pass)) throw new Exception("bad password");
      System.out.println("Ok");
      System.out.flush();
      LnxNative.fuse(args, this);
    } catch (Exception e) {
      JFLog.log("Error:" + e);
    }
  }

  public int getattr(String path, FuseStat stat) {
//    JFLog.log("getattr:" + path);
    try {
//      SmbFile file = new SmbFile(base + path, auth);
      SmbFile file = new SmbFile(baseFile, path);
      if (!file.exists()) {
        return -ENOENT;  //not found
      }
      if (file.isDirectory()) {
        stat.folder = true;
      } else {
        stat.size = file.length();
      }
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int mkdir(String path, int mode) {
//    JFLog.log("mkdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
//      SmbFile file = new SmbFile(base + path, auth);
      SmbFile file = new SmbFile(baseFile, path);
      file.mkdir();
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int unlink(String path) {
//    JFLog.log("unlink:" + path);
    try {
//      SmbFile file = new SmbFile(base + path, auth);
      SmbFile file = new SmbFile(baseFile, path);
      file.delete();
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int rmdir(String path) {
//    JFLog.log("rmdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
//      SmbFile file = new SmbFile(base + path, auth);
      SmbFile file = new SmbFile(baseFile, path);
      file.delete();
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int symlink(String target, String link) {
//    JFLog.log("symlink:" + link + "->" + target);
    return -1;
  }

  public int link(String target, String link) {
//    JFLog.log("link:" + link + "->" + target);
    return -1;
  }

  public int chmod(String path, int mode) {
//    JFLog.log("chmod:" + path);
    return -1;
  }

  public int chown(String path, int mode) {
//    JFLog.log("chown:" + path);
    return -1;
  }

  public int truncate(String path, long size) {
//    JFLog.log("truncate:" + path);
    try {
//      SmbFile file = new SmbFile(base + path, auth);
      SmbFile file = new SmbFile(baseFile, path);
      if (!file.exists()) return -1;
      if (file.isDirectory()) return -1;
      new SmbRandomAccessFile(file, "w").setLength(size);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return -1;
  }

  private class FileState {
    SmbFile file;
    SmbRandomAccessFile raf;
    boolean canWrite;
    boolean canRead;
  }

  public int open(String path, int _mode, int fd) {
//    JFLog.log("open:" + path);
    try {
//      SmbFile file = new SmbFile(base + path, auth);
      SmbFile file = new SmbFile(baseFile, path);
      if (!file.exists()) return -1;
      if (file.isDirectory()) return -1;
      FileState fs = new FileState();
      String mode = "";
      fs.canRead = file.canRead();
      if (fs.canRead) mode += "r";
      fs.canWrite = file.canWrite();
      if (fs.canWrite) mode += "w";
      if (mode.length() == 0) {
//        JFLog.log("open:access denied");
        return -1;
      }
      fs.file = file;
      fs.raf = new SmbRandomAccessFile(file, mode);
      attachObject(fd, fs);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int read(String path, byte buf[], long offset, int fd) {
    int size = buf.length;
//    JFLog.log("read:" + path);
    FileState fs = (FileState)getObject(fd);
    if (fs == null) {
//      JFLog.log("no fs");
      return -1;
    }
    if (!fs.canRead) {
//      JFLog.log("!read");
      return -1;
    }
    byte data[] = new byte[size];
    try {
      fs.raf.seek(offset);
      int read = 0;
      int pos = 0;
      while (read != size) {
        int left = size - read;
        int amt = fs.raf.read(data, 0, left);
        if (amt <= 0) break;
        System.arraycopy(data, 0, buf, pos, amt);
        read += amt;
        pos += amt;
      }
//      JFLog.log("read=" + read);
      return read;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int write(String path, byte buf[], long offset, int fd) {
    int size = buf.length;
//    JFLog.log("write:" + path);
    FileState fs = (FileState)getObject(fd);
    if (fs == null) return -1;
    if (!fs.canWrite) return -1;
    try {
      fs.raf.seek(offset);
      fs.raf.write(buf);
      return size;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int statfs(String path) {
//    JFLog.log("statfs:" + path);
    return -1;
  }

  public int close(String path, int fd) {
//    JFLog.log("close:" + path);
    detachObject(fd);
    return 0;
  }

  public String[] readdir(String path) {
//    JFLog.log("readdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
//      SmbFile folder = new SmbFile(base + path, auth);
      SmbFile folder = new SmbFile(baseFile, path);
      SmbFile files[] = folder.listFiles();
      int cnt = files.length;
      String dir[] = new String[cnt];
      for(int a=0;a<files.length;a++) {
//        JFLog.log("invokeFiller:" + files[a].getName());
        dir[a] = files[a].getName();
      }
//      JFLog.log("readdir done");
      return dir;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public int create(String path, int mode, int fd) {
//    JFLog.log("create:" + path);
    try {
//      SmbFile file = new SmbFile(base + path, auth);
      SmbFile file = new SmbFile(baseFile, path);
      if (file.exists()) return -1;
      file.createNewFile();
      return open(path, mode, fd);
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }
}

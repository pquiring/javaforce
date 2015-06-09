package javaforce.utils;

/**
 * Fuse SMB/CIFS
 *
 * Created : Nov 2, 2013
 */

import java.io.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.jni.lnx.*;


public class jfuseftp extends Fuse {
  protected String user, server;  //user@server
  private String pass;
  public FTP ftp;

  private void connect(FTP ftp) throws Exception {
    ftp.connect(server, 21);
  }

  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage : jfuse-smb user@server mount");
      return;
    }
    new jfuseftp().main2(args);
  }

  public boolean auth(String args[], String passwd) {
    this.pass = passwd;
    try {
      int idx1 = args[0].indexOf("@");
      if (idx1 == -1) throw new Exception("Usage : jfuse-smb user@server mount");
      user = args[0].substring(0, idx1);
      server = args[0].substring(idx1+1);
      ftp = new FTP();
      connect(ftp);
      if (!ftp.login(user, passwd)) throw new Exception("login failed");
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
      String passwd = br.readLine();
      if (!auth(args, passwd)) throw new Exception("bad password");
      System.out.println("Ok");
      System.out.flush();
      LnxNative.fuse(args, this);
    } catch (Exception e) {
      JFLog.log("Error:" + e);
    }
  }

  //drwxrwxrwx   ? <user  > <user  > <size       > MMM DD  YYYY <name...>
  //0123456789012345678901234567890123456789012345678901234567890

  public int getattr(String path, FuseStat stat) {
//    JFLog.log("getattr:" + path);
    try {
      String ln = ftp.ls(path);
      if (ln.length() < 10) throw new Exception("bad dir");
      if (ln.charAt(0) == 'd') {
        stat.folder = true;
      } else if (ln.charAt(0) == 'l') {
        stat.symlink = true;
      } else {
        stat.size = JF.atoi(ln.substring(33,46).trim());
      }
      return 0;
    } catch (Exception e) {
//      JFLog.log(e);
      return -1;
    }
  }

  public int mkdir(String path, int mode) {
//    JFLog.log("mkdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
      ftp.mkdir(path);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int unlink(String path) {
//    JFLog.log("unlink:" + path);
    try {
      ftp.rm(path);
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
      ftp.rmdir(path);
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
    try {
      ftp.chmod(mode, path);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int chown(String path, int mode) {
//    JFLog.log("chown:" + path);
    return -1;
  }

  public int truncate(String path, long size) {
//    JFLog.log("truncate:" + path);
    return -1;
  }

  private class FileState {
    FTP ftp;  //create a new session for file transfers
    boolean reading;
    boolean writing;
    long pos;
    OutputStream os;
    InputStream is;
  }

  public int open(String path, int mode, int fd) {
//    JFLog.log("open:" + path);
    try {
      String ln = ftp.ls(path);
      if (ln.charAt(0) == 'd') throw new Exception("not a file");
      FileState fs = new FileState();
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
      JFLog.log("no fs");
      return -1;
    }
    if (fs.writing) {
      JFLog.log("read after write");
      return -1;
    }
    try {
      if (!fs.reading) {
        if (offset != 0) throw new Exception("read not from start of file");
        fs.reading = true;
        fs.ftp = new FTP();
        fs.pos = 0;
        connect(fs.ftp);
        fs.ftp.login(user, pass);
        fs.is = fs.ftp.getStart(path);
      } else {
        if (fs.pos != offset) throw new Exception("download not continuous");
      }
      byte data[] = new byte[size];
      int read = 0;
      int pos = 0;
      while (read != size) {
        int amt = fs.is.read(data, pos, size - read);
        if (amt <= 0) throw new Exception("read failed");
        read += amt;
        pos += amt;
      }
      fs.pos += size;
      return size;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int write(String path, byte buf[], long offset, int fd) {
    int size = buf.length;
//    JFLog.log("write:" + path);
    FileState fs = (FileState)getObject(fd);
    if (fs == null) {
//      JFLog.log("fs null");
      return -1;
    }
    if (fs.reading) {
//      JFLog.log("write after read");
      return -1;
    }
    try {
      if (!fs.writing) {
        if (offset != 0) throw new Exception("write not from start of file");
        fs.writing = true;
        fs.ftp = new FTP();
        fs.pos = 0;
        connect(fs.ftp);
        fs.ftp.login(user, pass);
        fs.os = fs.ftp.putStart(path);
      } else {
        if (fs.pos != offset) throw new Exception("write not sequential");
      }
      fs.os.write(buf);
      fs.pos += size;
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
//    JFLog.log("release:" + path);
    FileState fs = (FileState)getObject(fd);
    try {
      if (fs != null) {
        if (fs.reading) {
          fs.ftp.getFinish();
          fs.reading = false;
        }
        if (fs.writing) {
          fs.ftp.putFinish();
          fs.writing = false;
        }
        if (fs.ftp != null) {
          fs.ftp.disconnect();
          fs.ftp = null;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    detachObject(fd);
    return 0;
  }

  public String[] readdir(String path) {
//    JFLog.log("readdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
      String dir = ftp.ls(path);
      String lns[] = dir.split("\n");
      String ret[] = new String[lns.length];
      for(int a=0;a<lns.length;a++) {
        ret[a] = lns[a].substring(100);
      }
      return ret;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public int create(String path, int mode, int fd) {
//    JFLog.log("create:" + path);
    return 0;
  }
}

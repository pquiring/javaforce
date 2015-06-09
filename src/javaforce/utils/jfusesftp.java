package javaforce.utils;

/**
 * Fuse SSH (sftp)
 *
 * Created : Nov 2, 2013
 */

import java.io.*;
import java.util.*;

import com.jcraft.jsch.*;

import javaforce.*;
import javaforce.jni.*;
import javaforce.jni.lnx.*;

public class jfusesftp extends Fuse {
  private String user, share, server;  //user@server/share

  private ChannelSftp channel;
  private Session jschsession;
  private JSch jsch;

  private class MyUserInfo implements UserInfo {
    public String password;
    public MyUserInfo(String password) {this.password = password;}
    public String getPassword(){
      return password;
    }
    public boolean promptYesNo(String str){
      return true;
    }
    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ return true; }
    public boolean promptPassword(String message){ return true; }
    public void showMessage(String message){
      JFLog.log(message);
    }
  }

  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage : jfuse-smb user@server/share mount");
      return;
    }
    new jfusesftp().main2(args);
  }

  public boolean auth(String args[], String passwd) {
    try {
      int idx1 = args[0].indexOf("@");
      int idx2 = args[0].indexOf("/");
      if (idx1 == -1 || idx2 == -1) throw new Exception("Usage : jfuse-smb user@server/share mount");
      user = args[0].substring(0, idx1);
      server = args[0].substring(idx1+1, idx2);
      share = args[0].substring(idx2+1);
      jsch = new JSch();
      jschsession = jsch.getSession(user, server, 22);
      jschsession.setPassword(passwd);
      jschsession.setUserInfo(new MyUserInfo(passwd));
      jschsession.connect(30000);
      channel = (ChannelSftp) jschsession.openChannel("sftp");
      channel.connect(30000);
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
//      Vector<ChannelSftp.LsEntry> list = channel.ls(path);
//      ChannelSftp.LsEntry file = list.get(0);
      SftpATTRS attrs = channel.lstat(path);
      if (attrs.isDir()) {
        stat.folder = true;
      } else {
        stat.size = attrs.getSize();
      }
      stat.atime = attrs.getATime();
      stat.mtime = attrs.getMTime();
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
      channel.mkdir(path);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int unlink(String path) {
//    JFLog.log("unlink:" + path);
    try {
      channel.rm(path);
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
      channel.rmdir(path);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int symlink(String target, String link) {
//    JFLog.log("symlink:" + link + "->" + target);
    try {
      channel.symlink(target, link);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int link(String target, String link) {
//    JFLog.log("link:" + link + "->" + target);
    return -1;
  }

  public int chmod(String path, int mode) {
//    JFLog.log("chmod:" + path);
    try {
      channel.chmod(mode, path);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int chown(String path, int mode) {
//    JFLog.log("chown:" + path);
    try {
      channel.chown(mode, path);
      return 0;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public int truncate(String path, long size) {
//    JFLog.log("truncate:" + path);
    return -1;
  }

  private static class FileState {
    boolean reading;
    boolean writing;
    long pos;
    OutputStream os;
    InputStream is;
  }

  public int open(String path, int mode, int fd) {
//    JFLog.log("open:" + path);
    try {
      SftpATTRS attrs = channel.lstat(path);
      if (attrs.isDir()) throw new Exception("not a file");
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
//      JFLog.log("no fs");
      return -1;
    }
    if (fs.writing) {
//      JFLog.log("read after write");
      return -1;
    }
    try {
      if (!fs.reading) {
        if (offset != 0) throw new Exception("read not from start of file");
        fs.reading = true;
        fs.pos = 0;
        fs.is = channel.get(path);
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
        fs.pos = 0;
        fs.os = channel.put(path);
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
//    JFLog.log("close:" + path);
    detachObject(fd);
    return 0;
  }

  @SuppressWarnings("unchecked")
  public String[] readdir(String path) {
//    JFLog.log("readdir:" + path);
    if (!path.endsWith("/")) path += "/";
    try {
      Vector<ChannelSftp.LsEntry> files = channel.ls(path);  //unchecked conversion
      int cnt = files.size();
      String dir[] = new String[cnt];
      for(int a=0;a<cnt;a++) {
        dir[a] = files.get(a).getFilename();
      }
      return dir;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public int create(String path, int mode, int fd) {
//    JFLog.log("create:" + path);
    return 0;
  }

  private void disconnect() {
    channel.disconnect();
    jschsession.disconnect();
  }
}

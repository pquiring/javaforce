package javaforce;

/** FTP over SSH
 *
 * Using Apache SSHD.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.awt.*;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;

public class SFTP {
  private SshClient client;  //ssh
  private ClientSession session;
  private SftpClient channel;
  private String path;
  private boolean aborted;

  public boolean connect(String host, int port, String username, String password, String key) {
    try {
      client = SshClient.setUpDefaultClient();
      client.start();
      ConnectFuture cf = client.connect(username, host, port);
      session = cf.verify().getSession();
//System.out.println("session = " + jschsession);
      if (key == null || key.length() == 0) {
        session.addPasswordIdentity(password);
      } else {
        JFLog.log("using key:" + key);
        JFLog.log("TODO : set ssh key");
        //session.addPublicKeyIdentity(sshKey);
      }
      session.auth().verify(30000);
      channel = DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
    } catch (Exception e) {
      JFAWT.showMessage("Error", "Error:" + e);
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public void disconnect() {
    try {
      channel.close();
      session.close();
    } catch (Exception e) {}
  }

  public String remote_pwd() {
    String wd;
    try {
      wd = channel.canonicalPath(path);
      int i1 = wd.indexOf("\"");
      if (i1 != -1) {
        //the reply is ["path" is your current location]
        int i2 = wd.lastIndexOf("\"");
        if (i1 == i2) return "/";
        wd = wd.substring(i1+1, i2);
      }
    } catch (Exception e) {
      return "/";
    }
    return wd;
  }

  @SuppressWarnings("unchecked")
  public String[] remote_ls() {
    try {
      Iterable<SftpClient.DirEntry> ls;
      ls = channel.readDir(path);
      ArrayList<String> files = new ArrayList<>();
      for(SftpClient.DirEntry e : ls) {
        files.add(e.getFilename());
      }
      return files.toArray(JF.StringArrayType);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public void remote_chdir(String newpath) {
    try {
      if (newpath.startsWith("/")) {
        path = channel.canonicalPath(newpath);  //absolute
      } else {
        path = channel.canonicalPath(path + "/" + newpath);  //relative
      }
      remote_ls();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static final int bufsiz = (1024 * 64);
  private void copy(InputStream is, OutputStream os) throws Exception {
    byte[] buf = new byte[bufsiz];
    while (is.available() > 0) {
      int toread = is.available();
      if (toread > bufsiz) toread = bufsiz;
      int read = is.read(buf, 0, toread);
      if (read == -1) throw new Exception("read error");
      if (read > 0) {
        os.write(buf, 0, read);
      }
    }
  }

  public void download_file(File remote, File local) {
    total = 0;
    try {
      FileOutputStream fos = new FileOutputStream(local);
      InputStream is = channel.read(path + "/" +remote.getName());
      copy(is, fos);
      is.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void upload_file(File local, File remote) {
    total = 0;
    try {
      FileInputStream fis = new FileInputStream(local);
      OutputStream os = channel.write(path + "/" +remote.getName());
      copy(fis, os);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void abort() {
    aborted = true;
    //TODO : channel.abort() ?
  }

  public void remote_mkdir(String file) {
    try {
      channel.mkdir(file);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void remote_delete_file(String file) {
    try {
      channel.remove(file);
   } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void remote_rename(String from, String to) {
    try {
      channel.rename(from, to);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void setPerms(int value, String remoteFile) {
    try {
      SftpClient.Attributes attr = new SftpClient.Attributes();
      attr.setPermissions(value);
      channel.setStat(remoteFile, attr);
      remote_chdir(".");  //refresh
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private int total;

  //TODO : progress monitor

  public void progress_init() {
    total = 0;
  }

  public void progress_count(long l) {
    total += l;
  }
}

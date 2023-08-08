/** FTP over SSH
 *
 * Requires jcraft/jsch
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

public class SiteSFTP extends SiteFTP {
  private SshClient client;  //ssh
  private ClientSession session;
  private SftpClient channel;
  private String path;

  public boolean connect(SiteDetails sd) {
    try {
      setStatus("Connecting...");
      client = SshClient.setUpDefaultClient();
      client.start();
      ConnectFuture cf = client.connect(sd.username, sd.host, JF.atoi(sd.port));
      session = cf.verify().getSession();
//System.out.println("session = " + jschsession);
      if (sd.sshKey.length() == 0) {
        session.addPasswordIdentity(sd.password);
      } else {
        JFLog.log("using key:" + sd.sshKey);
        JFLog.log("TODO : set ssh key");
        //session.addPublicKeyIdentity(sd.sshKey);
      }
      setStatus("Login...");
      session.auth().verify(30000);
      channel = DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
//System.out.println("channel = " + channel);
      if (sd.remoteDir.length() > 0) {
        path = channel.canonicalPath(sd.remoteDir);
      } else {
        path = channel.canonicalPath(".");
      }
      remote_ls();
      setStatus(null);
    } catch (Exception e) {
      JFAWT.showMessage("Error", "Error:" + e);
      JFLog.log(e);
      closeSite();
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
  public void remote_ls() {
    try {
      String wd = remote_pwd();
      remoteDir.setText(wd);
      remoteDir.setText(wd);
      Iterable<SftpClient.DirEntry> ls;
      ls = channel.readDir(path);
      StringBuilder lsstr = new StringBuilder();
      for(SftpClient.DirEntry e : ls) {
        lsstr.append(e.getFilename());
        lsstr.append("\n");
      }
      parseRemote(wd, lsstr.toString());
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
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
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
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
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void upload_file(File local, File remote) {
    total = 0;
    try {
      FileInputStream fis = new FileInputStream(local);
      OutputStream os = channel.write(path + "/" +remote.getName());
      copy(fis, os);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
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
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_delete_file(String file) {
    try {
      channel.remove(file);
   } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_delete_folder(String file) {
    try {
      channel.rmdir(file);
      //update remoteTree
      int idx = file.lastIndexOf("/");
      if (idx != -1) file = file.substring(idx+1);
      for(int a=0;a<remoteTag.getChildCount();a++) {
        XMLTree.XMLTag child = remoteTag.getChildAt(a);
        if (child.getName().equals(file)) {
          remoteFolders.deleteTag(child);
          break;
        }
      }
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_rename(String from, String to) {
    try {
      channel.rename(from, to);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void setPerms(int value, String remoteFile) {
    try {
      SftpClient.Attributes attr = new SftpClient.Attributes();
      attr.setPermissions(value);
      channel.setStat(remoteFile, attr);
      remote_chdir(".");  //refresh
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  private int total;

  //TODO : progress monitor

  public void progress_init() {
    total = 0;
  }

  public void progress_count(long l) {
    total += l;
    setProgress(total);
  }
}

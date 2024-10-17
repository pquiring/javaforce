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

public class SiteSFTP extends SiteFTP {
  private SFTP sftp;

  public boolean connect(SiteDetails sd) {
    try {
      setStatus("Connecting...");
      sftp = new SFTP();
      sftp.connect(sd.host, JF.atoi(sd.port), sd.username, sd.password, null);
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
    sftp.disconnect();
    sftp = null;
  }

  public String remote_pwd() {
    return sftp.remote_pwd();
  }

  public void remote_ls() {
    try {
      String wd = sftp.remote_pwd();
      String[] files = sftp.remote_ls();
      String ls = JF.join("\n", files);
      parseRemote(wd, ls);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_chdir(String newpath) {
    try {
      sftp.remote_chdir(newpath);
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
    try {
      sftp.download_file(remote, local);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void upload_file(File local, File remote) {
    try {
      sftp.upload_file(local, remote);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void abort() {
    aborted = true;
    sftp.abort();
  }

  public void remote_mkdir(String file) {
    try {
      sftp.remote_mkdir(file);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_delete_file(String file) {
    try {
      sftp.remote_delete_file(file);
   } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_delete_folder(String file) {
    try {
      sftp.remote_delete_folder(file);
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
      sftp.remote_rename(from, to);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void setPerms(int value, String remoteFile) {
    try {
      sftp.setPerms(value, remoteFile);
      remote_chdir(".");  //refresh
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }
}

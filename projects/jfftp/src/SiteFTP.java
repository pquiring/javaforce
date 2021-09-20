/**
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class SiteFTP extends Site implements FTP.ProgressListener {
  public FTP ftp;

  @Override
  public boolean connect(SiteDetails sd) {
    try {
      ftp = new FTP();
      ftp.setLogging(true);
      ftp.addProgressListener(this);
      setStatus("Connecting...");
      if (!ftp.connect(sd.host, Integer.valueOf(sd.port))) {
        throw new Exception("Connection failed");
      }
      setStatus("Login...");
      if (!ftp.login(sd.username, sd.password)) {
        throw new Exception("Login denied");
      }
      if (sd.remoteDir.length() > 0) {
        ftp.cd(sd.remoteDir);
      }
      remote_ls();
      if (!ftp.setBinary()) {
        throw new Exception("Binary mode not supported");
      }
      setStatus(null);
    } catch (Exception e) {
      JFAWT.showMessage("Error", "Error:" + e);
      JFLog.log(e);
      closeSite();
      return false;
    }
    return true;
  }

  @Override
  public void disconnect() {
    try {ftp.disconnect();} catch (Exception e) {}
  }

  public String remote_pwd() {
    String wd;
    try {
      wd = ftp.pwd();
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

  public void remote_ls() {
    try {
      String wd = remote_pwd();
      remoteDir.setText(wd);
      parseRemote(wd, ftp.ls("."));
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
      remoteFilesTableModel.setRowCount(0);
      remoteFilesTableModel.addRow(new Object[] {"Error"});
    }
  }

  @Override
  public void remote_chdir(String path) {
    path = path.replaceAll("\\\\", "/");
    try {
      ftp.cd(path);
      remote_ls();
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  @Override
  public void download() {
    if (JF.isWindows() && localDir.getText().equals("/")) return;
    aborted = false;
    localFolderStack = new ArrayList<String>();
    remoteFolderStack = new ArrayList<String>();
    new Thread() {
      @Override
      public void run() {
        setStatus("Downloading");
        download_files();
        setStatus(null);
      }
      public void download_files() {
        try {
          int rows[] = remoteFiles.getSelectedRows();
          if (rows.length == 0) return;
          SiteFile sf[] = new SiteFile[rows.length];
          for(int idx=0;idx<rows.length;idx++) {
            sf[idx] = (SiteFile)remoteFilesTableModel.getValueAt(rows[idx], 0);
          }
          for(int idx=0;idx<sf.length;idx++) {
            if (sf[idx].getText().equals(".")) continue;
            if (sf[idx].getText().equals("..")) continue;
            totalFileSize = sf[idx].filesize;
            if (sf[idx].isDir) {
              remoteFolderStack.add(remoteDir.getText());
              remote_chdir(sf[idx].getText());
            }
            if (sf[idx].isLink && !sf[idx].isDir) {
              //test if dir
              String d1 = remoteDir.getText();
              remote_chdir(sf[idx].getText());
              String d2 = remoteDir.getText();
              if (!d1.equals(d2)) {
                remoteFolderStack.add(d1);
                sf[idx].isDir = true;
              }
            }
            if (sf[idx].isDir) {
              //already remote_chdir()
              localFolderStack.add(localDir.getText());
              local_mkdir(localDir.getText() + "/" + sf[idx].getText());
              local_chdir(sf[idx].getText());
              remoteFiles.selectAll();
              download_files();
              remote_chdir(remoteFolderStack.remove(remoteFolderStack.size() - 1));
              local_chdir(localFolderStack.remove(localFolderStack.size() - 1));
              continue;
            }
            File remoteFile = new File(remoteDir.getText() + "/" + sf[idx].getText());
            File localFile = new File(localDir.getText() + "/" + sf[idx].getText());
            JFLog.log("download:" + remoteFile + "->" + localFile);
            addLog("download:" + remoteFile + "->" + localFile);
            setStatus("download:" + remoteFile + "->" + localFile);
            setTotalFileSize(sf[idx].filesize);
            download_file(remoteFile, localFile);
            if (aborted) break;
          }
        } catch (Exception e) {
          setStatus(null);
          JFLog.log(e);
          addLog("Error:" + e);
          addLog(ftp.getLastResponse());
        }
        local_chdir(".");  //refresh
      }
    }.start();
  }

  public void download_file(File remote, File local) {
    try {
      ftp.get(remote, local);
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  @Override
  public void upload() {
    aborted = false;
    localFolderStack = new ArrayList<String>();
    remoteFolderStack = new ArrayList<String>();
    new Thread() {
      @Override
      public void run() {
        setStatus("Uploading");
        upload_files();
        setStatus(null);
      }
      public void upload_files() {
        try {
          int rows[] = localFiles.getSelectedRows();
          if (rows.length == 0) return;
          SiteFile sf[] = new SiteFile[rows.length];
          for(int idx=0;idx<rows.length;idx++) {
            sf[idx] = (SiteFile)localFilesTableModel.getValueAt(rows[idx], 0);
          }
          for(int idx=0;idx<sf.length;idx++) {
            if (sf[idx].getText().equals(".")) continue;
            if (sf[idx].getText().equals("..")) continue;
            totalFileSize = sf[idx].filesize;
            if (sf[idx].isDir) {
              localFolderStack.add(localDir.getText());
              local_chdir(sf[idx].getText());
            }
            if (sf[idx].isLink && !sf[idx].isDir) {
              //test if dir
              String d1 = localDir.getText();
              local_chdir(sf[idx].getText());
              String d2 = localDir.getText();
              if (!d1.equals(d2)) {
                localFolderStack.add(d1);
                sf[idx].isDir = true;
              }
            }
            if (sf[idx].isDir) {
              //already local_chdir()
              remoteFolderStack.add(remoteDir.getText());
              remote_mkdir(sf[idx].getText());
              remote_chdir(sf[idx].getText());
              localFiles.selectAll();
              upload_files();
              remote_chdir(remoteFolderStack.remove(remoteFolderStack.size() - 1));
              local_chdir(localFolderStack.remove(localFolderStack.size() - 1));
              continue;
            }
            File remoteFile = new File(remoteDir.getText() + "/" + sf[idx].getText());
            File localFile = new File(localDir.getText() + "/" + sf[idx].getText());
            JFLog.log("upload:" + localFile + "->" + remoteFile);
            addLog("upload:" + localFile + "->" + remoteFile);
            setStatus("upload:" + localFile + "->" + remoteFile);
            setTotalFileSize((int)localFile.length());
            upload_file(localFile, remoteFile);
            if (aborted) break;
          }
        } catch (Exception e) {
          setStatus(null);
          JFLog.log(e);
          addLog("Error:" + e);
          addLog(ftp.getLastResponse());
        }
        remote_chdir(".");  //refresh
      }
    }.start();
  }

  public void upload_file(File local, File remote) {
    try {
      ftp.put(local, remote);
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  public void abort() {
    aborted = true;
    ftp.abort();
  }

  @Override
  public void remote_mkdir() {
    String fn = JFAWT.getString("Enter new folder name", "");
    if ((fn == null) || (fn.length() == 0)) return;
    if (fn.indexOf(":") != -1) return;
    if (fn.indexOf("/") != -1) return;
    if (fn.indexOf("?") != -1) return;
    if (fn.indexOf("*") != -1) return;
    String wd = remoteDir.getText();
    if (!wd.endsWith("/")) wd += "/";
    String file = wd + fn;
    remote_mkdir(file);
    remote_chdir(".");  //refresh
  }

  public void remote_mkdir(String file) {
    try {
      ftp.mkdir(file);
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  @Override
  public void remote_delete() {
    remoteFolderStack = new ArrayList<String>();
    if (!JFAWT.showConfirm("Delete", "Are you sure you want to delete file(s)?")) return;
    aborted = false;
    setStatus("Deleting");
    new Thread() {
      @Override
      public void run() {
        remote_delete_files();
        remote_chdir(".");  //refresh
        setStatus(null);
      }
    }.start();
  }

  private void remote_delete_files() {
    int rows[] = remoteFiles.getSelectedRows();
    if (rows.length == 0) return;
    SiteFile sf[] = new SiteFile[rows.length];
    for(int idx=0;idx<rows.length;idx++) {
      sf[idx] = (SiteFile)remoteFilesTableModel.getValueAt(rows[idx], 0);
    }
    for(int idx=0;idx<sf.length;idx++) {
      if (sf[idx].getText().equals(".")) continue;
      if (sf[idx].getText().equals("..")) continue;
      totalFileSize = sf[idx].filesize;
      if (sf[idx].isDir) {
        remoteFolderStack.add(remoteDir.getText());
        remote_chdir(sf[idx].getText());
      }
      if (sf[idx].isLink && !sf[idx].isDir) {
        //test if dir
        String d1 = remoteDir.getText();
        remote_chdir(sf[idx].getText());
        String d2 = remoteDir.getText();
        if (!d1.equals(d2)) {
          remoteFolderStack.add(d1);
          sf[idx].isDir = true;
        }
      }
      if (sf[idx].isDir) {
        //already remote_chdir();
        remoteFiles.selectAll();
        remote_delete_files();
        remote_chdir(remoteFolderStack.remove(remoteFolderStack.size() - 1));
      }
      String remoteFile = remoteDir.getText() + "/" + sf[idx].getText();
      if (sf[idx].isDir) {
        remote_delete_folder(remoteFile);
      } else {
        remote_delete_file(remoteFile);
      }
      if (aborted) break;
    }
  }

  public void remote_delete_file(String file) {
    try {
      ftp.rm(file);
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  public void remote_delete_folder(String file) {
    try {
      ftp.rmdir(file);
      //update remoteTree
      int idx = file.lastIndexOf("/");
      if (idx != -1) file = file.substring(idx+1);
      for(int a=0;a<remoteTag.getChildCount();a++) {
        XML.XMLTag child = remoteTag.getChildAt(a);
        if (child.getName().equals(file)) {
          remoteFolders.deleteTag(child);
          break;
        }
      }
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  @Override
  public void remote_rename() {
    int rows[] = remoteFiles.getSelectedRows();
    if ((rows == null) || (rows.length != 1)) return;
    SiteFile sf = (SiteFile)remoteFilesTableModel.getValueAt(rows[0], 0);
    String remoteFile = remoteDir.getText() + "/" + sf.getText();
    String newName = JFAWT.getString("Rename File", sf.getText());
    if ((newName == null) || (newName.length() == 0)) return;
    File newFile = new File(remoteDir.getText() + "/" + newName);
    remote_rename(remoteFile, newName);
    //update remoteTree
    for(int a=0;a<remoteTag.getChildCount();a++) {
      XML.XMLTag child = remoteTag.getChildAt(a);
      if (child.getName().equals(sf.getText())) {
        child.setName(newName);
        break;
      }
    }
    remote_chdir(".");  //refresh
  }

  public void remote_rename(String from, String to) {
    try {
      ftp.rename(from, to);
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  @Override
  public void remote_props() {
    int rows[] = remoteFiles.getSelectedRows();
    if ((rows == null) || (rows.length != 1)) return;
    SiteFile sf = (SiteFile)remoteFilesTableModel.getValueAt(rows[0], 0);
    String remoteFile = remoteDir.getText() + "/" + sf.getText();
    String perms = (String)remoteFilesTableModel.getValueAt(rows[0], 3);
    //drwxrwxrwx
    int octal = 0;
    if (perms.charAt(1) == 'r') octal += 0x100;
    if (perms.charAt(2) == 'w') octal += 0x80;
    if (perms.charAt(3) == 'x') octal += 0x40;
    if (perms.charAt(4) == 'r') octal += 0x20;
    if (perms.charAt(5) == 'w') octal += 0x10;
    if (perms.charAt(6) == 'x') octal += 0x08;
    if (perms.charAt(7) == 'r') octal += 0x04;
    if (perms.charAt(8) == 'w') octal += 0x02;
    if (perms.charAt(9) == 'x') octal += 0x01;
    Permissions dialog = new Permissions(null, true, remoteFile, octal);
    dialog.setVisible(true);
    if (dialog.value == -1) return;
    try {
      setPerms(dialog.value, remoteFile);
      remote_chdir(".");  //refresh
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  public void setPerms(int value, String remoteFile) {
    try {
      ftp.chmod(value, remoteFile);
      remote_chdir(".");  //refresh
    } catch (Exception e) {
      setStatus(null);
      JFLog.log(e);
      addLog("Error:" + e);
      addLog(ftp.getLastResponse());
    }
  }

  public void closeSite() {
    JPanel panel = (JPanel)this.getClientProperty("panel");
    JTabbedPane tabs = (JTabbedPane)this.getClientProperty("tabs");
    tabs.remove(panel);
  }
}

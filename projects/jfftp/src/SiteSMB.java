import java.io.*;
import java.util.*;
import javax.swing.*;

//jcifs.samba.org
import jcifs.smb.*;

import javaforce.*;
import javaforce.awt.*;

/**
 *
 * @author pquiring
 */

public class SiteSMB extends Site {
  private String url;
  private NtlmPasswordAuthentication auth;

  public boolean connect(SiteDetails sd) {
    try {
      setStatus("Connecting...");
      url = "smb://" + sd.host;
      if (!sd.remoteDir.startsWith("/"))
        remoteDir.setText("/" + sd.remoteDir);
      else
        remoteDir.setText(sd.remoteDir);
      auth = new NtlmPasswordAuthentication("domain", sd.username, sd.password);
      SmbFile file = new SmbFile(url + remoteDir.getText(), auth);
      if (!file.exists()) throw new Exception("Resource not found");
      if (!remote_ls()) throw new Exception("Connection failed");
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
  }

  public String remote_pwd() {
    return remoteDir.getText();
  }

  public String getListing() {
    //attr       node owner    group    filesize mth dy time  filename  //time=year if older than 1 year
    StringBuffer str = new StringBuffer();
    try {
      setStatus("Listing");
      SmbFile folder = new SmbFile(url + remoteDir.getText() + "/", auth);
      SmbFile lst[] = folder.listFiles();
      for(int a=0;a<lst.length;a++) {
        String name = lst[a].getName();
        if (lst[a].isDirectory()) {
          str.append("drwx------ 1 smb smb 0 1 1 12:00 " + name.substring(0, name.length() - 1) + "\n");
        } else {
          str.append("-rwx------ 1 smb smb " + lst[a].length() + " 1 1 12:00 " + name + "\n");
        }
      }
      setStatus(null);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
      return null;
    }
    return str.toString();
  }

  public boolean remote_ls() {
    try {
      String ls = getListing();
      if (ls == null) return false;
      parseRemote(remote_pwd(), ls);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
    return true;
  }

  public void remote_chdir(String path) {
    String wd = remoteDir.getText();
    path = path.replaceAll("\\\\", "/");
    if (path.equals(".")) {
      //do nothing
    } else if (path.equals("..")) {
      //remove last path element
      int idx = wd.lastIndexOf('/');
      if (idx == -1) {
        wd = "/";
      } else {
        wd = wd.substring(0, idx);
      }
    } else {
      if (path.indexOf("/") != -1) {
        //absolute path
        wd = path;
      } else {
        //add path element
        if (!wd.endsWith("/")) wd += "/";
        wd += path;
      }
    }
    remoteDir.setText(wd);
    parseRemote(wd, getListing());
  }

  public void download() {
    aborted = false;
    localFolderStack = new ArrayList<String>();
    remoteFolderStack = new ArrayList<String>();
    new Thread() {
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
          setStatus("Error:" + e);
          JFLog.log(e);
          addLog("" + e);
        }
        local_chdir(".");  //refresh
      }
    }.start();
  }

  public void download_file(File remote, File local) {
    SmbFile file;
    try {
      file = new SmbFile(url + remoteDir.getText() + "/" + remote.getName(), auth);
      InputStream fis = file.getInputStream();
      int len = (int)file.length();
      byte buf[] = new byte[64 * 1024];
      OutputStream fos = new FileOutputStream(local);
      int copied = 0;
      while ((len > 0) && (!aborted)) {
        int read = fis.read(buf);
        if (read == -1) throw new Exception("file i/o error");
        if (read > 0) {
          fos.write(buf, 0, read);
          len -= read;
          copied += read;
          setProgress(copied);
        }
      }
      fos.close();
      fis.close();
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("" + e);
    }
  }

  public void upload() {
    aborted = false;
    localFolderStack = new ArrayList<String>();
    remoteFolderStack = new ArrayList<String>();
    new Thread() {
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
          setStatus("Error:" + e);
          JFLog.log(e);
          addLog("" + e);
        }
        remote_chdir(".");  //refresh
      }
    }.start();
  }

  public void upload_file(File local, File remote) {
    SmbFile file;
    try {
      file = new SmbFile(url + remoteDir.getText() + "/" + remote.getName(), auth);
      if (file.exists()) {
        file.delete();
      }
      file.createNewFile();
      OutputStream fos = file.getOutputStream();
      byte buf[] = new byte[64 * 1024];
      InputStream fis = new FileInputStream(local);
      int len = fis.available();
      int copied = 0;
      while ((len > 0) && (!aborted)) {
        int read = fis.read(buf);
        if (read == -1) throw new Exception("file i/o error");
        if (read > 0) {
          fos.write(buf, 0, read);
          len -= read;
          copied += read;
          setProgress(copied);
        }
      }
      fos.close();
      fis.close();
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("" + e);
    }
  }

  public void abort() {
    aborted = true;
  }

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
      if (!file.startsWith("/")) file = remoteDir.getText() + "/" + file;
      setStatus("mkdir");
      SmbFile smbfile = new SmbFile(url + file, auth);
      smbfile.mkdir();
      setStatus(null);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_delete() {
    remoteFolderStack = new ArrayList<String>();
    if (!JFAWT.showConfirm("Delete", "Are you sure you want to delete file(s)?")) return;
    aborted = false;
    setStatus("Deleting");
    new Thread() {
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
      if (!file.startsWith("/")) file = remoteDir.getText() + "/" + file;
      SmbFile smbfile = new SmbFile(url + file, auth);
      smbfile.delete();
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void remote_delete_folder(String file) {
    try {
      if (!file.endsWith("/"))
        remote_delete_file(file + "/");
      else
        remote_delete_file(file);
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
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

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
      SmbFile smbfrom, smbto;
      if (from.startsWith("/"))
        smbfrom = new SmbFile(url + from, auth);
      else
        smbfrom = new SmbFile(url + remoteDir.getText() + "/" + from, auth);
      if (to.startsWith("/"))
        smbto = new SmbFile(url + to, auth);
      else
        smbto = new SmbFile(url + remoteDir.getText() + "/" + to, auth);
      smbfrom.renameTo(smbto);
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

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
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void setPerms(int value, String remoteFile) {
    try {
      //TODO
      remote_chdir(".");  //refresh
    } catch (Exception e) {
      setStatus("Error:" + e);
      JFLog.log(e);
      addLog("Error:" + e);
    }
  }

  public void closeSite() {
    JPanel panel = (JPanel)this.getClientProperty("panel");
    JTabbedPane tabs = (JTabbedPane)this.getClientProperty("tabs");
    tabs.remove(panel);
  }
}

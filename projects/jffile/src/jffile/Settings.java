package jffile;

/*
 * Settings.java
 *
 * Created on August 2, 2007, 7:56 PM
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;

public class Settings {
  public static class Site {
    public String name, host, protocol, port, username, password, localDir, remoteDir;
  }
  public static class Folder {
    public Folder() {
      name = "My Sites";
      site = new Site[0];
      folder = new Folder[0];
    }
    public String name;
    public Site site[];
    public Folder folder[];
  }
  public static Settings settings = new Settings();
  public int WindowXSize = 640;
  public int WindowYSize = 480;
  public int WindowXPos = 0;
  public int WindowYPos = 0;
  public boolean bWindowMax = true;
  public int defaultView = JFileBrowser.VIEW_ICONS;
  public Folder sites = new Folder();

  public static void loadSettings() {
    String fn = JF.getUserPath() + "/.jfile.xml";
    XML xml = new XML();
    xml.read(fn);
    xml.writeClass(xml.root, settings);
  }
  public static void saveSettings() {
    String fn = JF.getUserPath() + "/.jfile.xml";
    XML xml = new XML();
    xml.root.setName("jfile");
    xml.readClass(xml.root, settings);
    xml.write(fn);
  }
  private static int cnt;
  public static void importSettings() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(JF.getCurrentPath()));
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

    try {
      XML fz = new XML();
      fz.read(chooser.getSelectedFile().getAbsolutePath());
      //process fz and insert into settings
      cnt = 0;
      importTag(fz.root, settings.sites);
    } catch (Exception e) {
      JFLog.log(e);
    }
    //save back to xml
    saveSettings();
    FileApp.inDialog = true;
    JF.showMessage("Import", "Imported " + cnt + " entries.");
    FileApp.inDialog = false;
  }
  private static void importTag(XML.XMLTag tag, Folder folder) {
    for(int a=0;a<tag.getChildCount();a++) {
      XML.XMLTag child = tag.getChildAt(a);
      if (child.getXMLName().equalsIgnoreCase("Servers")) {
        importTag(child, folder);
        continue;
      }
      if (child.getXMLName().equalsIgnoreCase("Folder")) {
        String name = child.getContent();
        int idx = name.indexOf("&");
        if (idx != -1) name = name.substring(0, idx);
        folder.folder = Arrays.copyOf(folder.folder, folder.folder.length + 1);
        importTag(child, folder.folder[folder.folder.length-1]);
        continue;
      }
      if (child.getXMLName().equalsIgnoreCase("Server")) {
        cnt++;
        String name = "untitled", host = "", port = "21", protocol = "ftp", user = "", pass = "", localDir = "", remoteDir = "";
        XML.XMLTag field;
        for(int b=0;b<child.getChildCount();b++) {
          field = child.getChildAt(b);
          if (field.getName().equalsIgnoreCase("name")) {name = field.getContent(); continue;}
          if (field.getName().equalsIgnoreCase("host")) {host = field.getContent(); continue;}
          if (field.getName().equalsIgnoreCase("port")) {port = field.getContent(); continue;}
          if (field.getName().equalsIgnoreCase("protocol")) {
            if (field.getContent().equals("0")) protocol = "ftp";
            if (field.getContent().equals("1")) protocol = "sftp";
            if (field.getContent().equals("4")) protocol = "ftps";
            continue;
          }
          if (field.getName().equalsIgnoreCase("user")) {user = field.getContent(); continue;}
          if (field.getName().equalsIgnoreCase("pass")) {pass = field.getContent(); continue;}
          if (field.getName().equalsIgnoreCase("localDir")) {localDir = field.getContent(); continue;}
          if (field.getName().equalsIgnoreCase("remoteDir")) {remoteDir = decodeDir(field.getContent()); continue;}
        }
        folder.site = Arrays.copyOf(folder.site, folder.site.length + 1);
        Site site = folder.site[folder.site.length-1];
        site.name = name;
        site.host = host;
        site.port = port;
        site.protocol = protocol;
        site.username = user;
        site.password = SiteMgr.encodePassword(pass);
        site.localDir = localDir;
        site.remoteDir = remoteDir;
        continue;
      }
    }
  }
  private static String decodeDir(String epath) {
    //decodes FileZilla3 remoteDir
    //Format : 1 0 [<len> element] ...
    String path = "";
    if (!epath.startsWith("1 0 ")) return "";
    epath = epath.substring(3);
    while (epath.length() > 0) {
      if (epath.charAt(0) != ' ') break;
      epath = epath.substring(1);
      int idx = epath.indexOf(" ");
      if (idx == -1) break;
      int len = JF.atoi(epath.substring(0, idx));
      if (len <= 0) break;
      if (epath.length() < idx + len) break;  //approx
      path += "/";
      path += epath.substring(idx+1, idx+1+len);
      epath = epath.substring(idx+1+len);
    }
    return path;
  }
  public static void exportSettings() {
/*
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(JF.getCurrentPath()));
    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;
*/
    FileApp.inDialog = true;
    JF.showMessage("TODO", "Not implemented yet");
    FileApp.inDialog = false;
    //TODO
 }
}

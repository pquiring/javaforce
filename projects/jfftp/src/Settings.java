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
import javaforce.awt.*;

public class Settings {
  public static int WindowXSize = 800;
  public static int WindowYSize = 600;
  public static int WindowXPos = 0;
  public static int WindowYPos = 0;
  public static boolean bWindowMax = false;

  public static void loadSettings() {
    String fn = JF.getUserPath() + "/.jfftp.xml";
    XML xml = new XML();
    xml.read(fn);
    XML.XMLTag tag = xml.getTag(new Object[] {"jfftp", "settings"});
    if (tag == null) return;  //no settings found
    xml.writeClass(tag, new Settings());
  }
  public static void saveSettings() {
    String fn = JF.getUserPath() + "/.jfftp.xml";
    XML xml = new XML();
    xml.read(fn);
    xml.root.setName("jfftp");
    XML.XMLTag tag = xml.addSetTag(xml.root, "settings", "", "");
    xml.readClass(tag, new Settings());
    xml.write(fn);
  }
  private static int cnt;
  public static void importSettings() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(JF.getCurrentPath()));
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

    XML xml = new XML();
    String fn = JF.getUserPath() + "/.jfftp.xml";
    try {
      xml.read(fn);
      xml.root.setName("jfftp");  //in case .jfftp.xml doesn't exist
      xml.addSetTag(xml.root, "sites", "", "");  //in case .jfftp.xml doesn't exist
      XML fz = new XML();
      fz.read(chooser.getSelectedFile().getAbsolutePath());
      //process fz and insert into xml
      folder = new ArrayList<String>();
      folder.add("jfftp");
      folder.add("sites");
      cnt = 0;
      importTag(fz.root, xml);
    } catch (Exception e) {
      JFLog.log(e);
    }
    folder = null;
    //save back to xml
    xml.write(fn);
    JFAWT.showMessage("Import", "Imported " + cnt + " entries.");
  }
  private static ArrayList<String> folder;
  private static void importTag(XML.XMLTag tag, XML xml) {
    for(int a=0;a<tag.getChildCount();a++) {
      XML.XMLTag child = tag.getChildAt(a);
      if (child.getName().equalsIgnoreCase("Servers")) {
        importTag(child, xml);
        continue;
      }
      if (child.getName().equalsIgnoreCase("Folder")) {
        XML.XMLTag parent = xml.getTag(folder.toArray());
        String name = child.getContent();
        int idx = name.indexOf("&");
        if (idx != -1) name = name.substring(0, idx);
        folder.add(name);
        xml.addSetTag(parent, "folder", "name=\"" + name + "\"", "");
        importTag(child, xml);
        folder.remove(folder.size() - 1);
        continue;
      }
      if (child.getName().equalsIgnoreCase("Server")) {
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
        XML.XMLTag parent = xml.getTag(folder.toArray());
        parent = xml.addTag(parent, "site", "name=\"" + name + "\"", "");
        xml.addSetTag(parent, "host", "", host);
        xml.addSetTag(parent, "port", "", port);
        xml.addSetTag(parent, "protocol", "", protocol);
        xml.addSetTag(parent, "username", "", user);
        xml.addSetTag(parent, "password", "", SiteMgr.encodePassword(pass));
        xml.addSetTag(parent, "localDir", "", localDir);
        xml.addSetTag(parent, "remoteDir", "", remoteDir);
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
    JFAWT.showMessage("TODO", "Not implemented yet");
    //TODO
 }
}

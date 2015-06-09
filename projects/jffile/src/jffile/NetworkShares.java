package jffile;

/**
 * Created : May 1, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class NetworkShares {
  public static class Share {
    public String name;
    public String folder;
  }
  public static class Shares {
    public Share share[];
  }

  private static Shares shares;
  private static String sharesFile = ".shares.xml";

  public static void addShare(String name, String folder) {
    Share newShare = new Share();
    newShare.name = name;
    newShare.folder = folder;
    shares.share = Arrays.copyOf(shares.share, shares.share.length + 1);
    shares.share[shares.share.length-1] = newShare;
    saveShares();
  }

  public static void delShare(String name) {
    int idx = -1;
    for(int a=0;a<shares.share.length;a++) {
      if (shares.share[a].name.equals(name)) {idx = a; break;}
    }
    if (idx == -1) return;
    int len = shares.share.length;
    Share newList[] = new Share[len-1];
    System.arraycopy(shares.share, 0, newList, 0, idx);
    System.arraycopy(shares.share, idx+1, newList, idx, len - idx - 1);
    shares.share = newList;
    saveShares();
  }

  public static ArrayList<String> getSharedFolders() {
    ArrayList<String> ret = new ArrayList<String>();
    for(int a=0;a<shares.share.length;a++) {
      ret.add(shares.share[a].folder);
    }
    return ret;
  }

  public static boolean isShared(String folder) {
    if (shares == null) return false;
    for(int a=0;a<shares.share.length;a++) {
      if (shares.share[a].folder.equals(folder)) return true;
    }
    return false;
  }

  public static String getShareName(String folder) {
    for(int a=0;a<shares.share.length;a++) {
      if (shares.share[a].folder.equals(folder)) return shares.share[a].name;
    }
    return null;
  }

  public static void loadShares() {
    defaultSharesConfig();
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(JF.getUserPath() + "/" + sharesFile);
      xml.read(fis);
      xml.writeClass(shares);
    } catch (FileNotFoundException e1) {
      defaultSharesConfig();
    } catch (Exception e2) {
      JFLog.log(e2);
      defaultSharesConfig();
    }
  }

  private static void defaultSharesConfig() {
    shares = new Shares();
    shares.share = new Share[0];
  }

  public static void saveShares() {
    try {
      XML xml = new XML();
      FileOutputStream fos = new FileOutputStream(JF.getUserPath() + "/" + sharesFile);
      xml.readClass("shares", shares);
      xml.write(fos);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

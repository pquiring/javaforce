package jffile;

/**
 * Created : Apr 28, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Mappings {
  public static class Map {
    public String mount;
    public String uri;
    public String wineDrive;
    public String passwd;
  }
  public static class Maps {
    public Map map[];
  }

  private static Maps maps;
  private static String mapsFile = "/.maps.xml";

  public static void addMap(String uri, String mount, String passwd, String wine) {
    Map newMap = new Map();
    newMap.mount = mount;
    newMap.uri = uri;
    newMap.wineDrive = wine;
    newMap.passwd = passwd;
    maps.map = Arrays.copyOf(maps.map, maps.map.length + 1);
    maps.map[maps.map.length-1] = newMap;
  }

  public static void delMap(String uri) {
    int idx = -1;
    for(int a=0;a<maps.map.length;a++) {
      if (maps.map[a].uri.equals(uri)) {idx = a; break;}
    }
    if (idx == -1) return;
    int len = maps.map.length;
    Map newList[] = new Map[len-1];
    System.arraycopy(maps.map, 0, newList, 0, idx);
    System.arraycopy(maps.map, idx+1, newList, idx, len - idx - 1);
    maps.map = newList;
    saveMaps();
  }

  public static Maps getMaps() {
    return maps;
  }

  public static ArrayList<String> getMapsList() {
    ArrayList<String> ret = new ArrayList<String>();
    for(int a=0;a<maps.map.length;a++) {
      ret.add(maps.map[a].wineDrive);
    }
    return ret;
  }

  public static String getMount(String wineDrive) {
    for(int a=0;a<maps.map.length;a++) {
      if (maps.map[a].wineDrive.equals(wineDrive)) return maps.map[a].mount;
    }
    return null;
  }

  public static void loadMaps() {
    defaultMapsConfig();
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(JF.getUserPath() + mapsFile);
      xml.read(fis);
      xml.writeClass(maps);
    } catch (FileNotFoundException e1) {
      defaultMapsConfig();
    } catch (Exception e2) {
      JFLog.log(e2);
      defaultMapsConfig();
    }
  }

  private static void defaultMapsConfig() {
    maps = new Maps();
    maps.map = new Map[0];
  }

  public static void saveMaps() {
    try {
      XML xml = new XML();
      FileOutputStream fos = new FileOutputStream(JF.getUserPath() + mapsFile);
      xml.readClass("maps", maps);
      xml.write(fos);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

}

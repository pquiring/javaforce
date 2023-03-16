/**
 * Torrent file.
 *
 * Created : May 27, 2012
 *
 * @author pquiring
 */

import java.util.Arrays;
import javaforce.JFLog;

public class TorrentFile {
  private static boolean debug = false;
  private byte metaData[];
  private MetaTag root;  //root tag
  private int metaIdx;
  public void read(byte data[]) throws Exception {
    metaData = data;
    metaIdx = 0;
    root = readTag();
  }
  private MetaTag readTag() throws Exception {
    int start = metaIdx;
    if (Character.isDigit(metaData[metaIdx])) {
      MetaData tag = new MetaData();
      //length:byte[length]
      StringBuilder str = new StringBuilder();
      while (Character.isDigit(metaData[metaIdx])) {
        str.append((char)metaData[metaIdx++]);
      }
      metaIdx++;  //':'
      tag.pos1 = start;
      int len = Integer.valueOf(str.toString());
      tag.data = Arrays.copyOfRange(metaData, metaIdx, metaIdx + len);
      metaIdx += len;
      tag.pos2 = metaIdx;
      if (debug) JFLog.log("read byte[]:" + tag.data.length);
      return tag;
    }
    switch (metaData[metaIdx++]) {
      case 'i': {  //integer
        MetaInt tag = new MetaInt();
        tag.pos1 = start;
        StringBuilder str = new StringBuilder();
        while (Character.isDigit(metaData[metaIdx])) {
          str.append((char)metaData[metaIdx++]);
        }
        metaIdx++;  //'e'
        tag.val = Long.valueOf(str.toString());
        tag.pos2 = metaIdx;
        if (debug) JFLog.log("readInt:" + tag.val);
        return tag;
      }
      case 'd': {  //dict
        MetaDict tag = new MetaDict();
        tag.pos1 = start;
        if (debug) JFLog.log("readDict {");
        do {
          MetaDictEntry de = new MetaDictEntry();
          if (debug) JFLog.log("readDictEntry {");
          de.key = readTag();
          if (de.key == null) break;  //'e'
          if (debug) JFLog.log("key=" + de.key);
          de.value = readTag();
          if (debug) JFLog.log("} //dictEntry");
          tag.list.add(de);
        } while(true);
        if (debug) JFLog.log("} //dict");
        tag.pos2 = metaIdx;
        return tag;
      }
      case 'l': { //list
        MetaList tag = new MetaList();
        tag.pos1 = start;
        if (debug) JFLog.log("readList {");
        do {
          MetaTag entry = readTag();
          if (entry == null) break;  //'e'
          tag.list.add(tag);
        } while(true);
        if (debug) JFLog.log("} //list");
        tag.pos2 = metaIdx;
        return tag;
      }
      case 'e': {
        if (debug) JFLog.log("read:end");
        return null;
      }
      default: {
        throw new Exception("bad torrent");
      }
    }
  }
  public String getString(String path[], MetaTag list) throws Exception {
    if (list == null) list = root;
    MetaTag tag = getTag(path, list);
    if (tag instanceof MetaData) {
      MetaData str = (MetaData)tag;
      return str.toString();
    }
    return null;
  }
  public byte[] getData(String path[], MetaTag list) throws Exception {
    if (list == null) list = root;
    MetaTag tag = getTag(path, list);
    if (tag instanceof MetaData) {
      MetaData str = (MetaData)tag;
      return str.data;
    }
    return null;
  }
  public MetaList getList(String path[], MetaTag list) throws Exception {
    if (list == null) list = root;
    MetaTag tag = getTag(path, list);
    if (tag instanceof MetaList) {
      MetaList sublist = (MetaList)tag;
      return sublist;
    }
    return null;
  }
  public long getValue(String path[], MetaTag list) throws Exception {
    if (list == null) list = root;
    MetaTag tag = getTag(path, list);
    if (tag instanceof MetaInt) {
      MetaInt val = (MetaInt)tag;
      return val.val;
    }
    return -1;
  }
  public MetaDict getDict(String path[], MetaTag list) throws Exception {
    if (list == null) list = root;
    return (MetaDict)getTag(path, list);
  }
  public MetaTag getDictEntry(String path[], MetaTag list) throws Exception {
    if (list == null) list = root;
    return getTag(path, list);
  }
  private MetaTag getTag(String path[], MetaTag list) throws Exception {
    if (list == null) list = root;
    int pathIdx = 0;
    MetaDict dict = (MetaDict)list;
    while (pathIdx < path.length) {
      if (path[pathIdx].equals("d")) {
        //root is always the main dict
        pathIdx++;
        continue;
      }
      if (path[pathIdx].startsWith("d:")) {
        String find = path[pathIdx].substring(2);
        boolean found = false;
        for(int a=0;a<dict.list.size();a++) {
          MetaDictEntry de = dict.list.get(a);
          MetaTag key = de.key;
          if (key instanceof MetaData) {
            MetaData str = (MetaData)key;
            if (debug) JFLog.log("compare?" + str.toString() + "==" + find);
            if (str.toString().equals(find)) {
              found = true;
              dict = (MetaDict)de.value;
              break;
            }
          } else {
            if (debug) JFLog.log("unknown dictEntry key?" + de.key);
          }
        }
        if (!found) {
          JFLog.log("Error:dict not found:" + find);
          return null;
        }
        pathIdx++;
        continue;
      }
      if (path[pathIdx].startsWith("l:")) {
        String find = path[pathIdx].substring(2);
        for(int a=0;a<dict.list.size();a++) {
          MetaDictEntry de = dict.list.get(a);
          MetaTag key = de.key;
          if (key instanceof MetaData) {
            MetaData str = (MetaData)key;
            if (debug) JFLog.log("compare?" + str.toString() + "==" + find);
            if (str.toString().equals(find)) {
              return de.value;
            }
          }
        }
        JFLog.log("Error:list not found:" + find);
        return null;
      }
      if (path[pathIdx].startsWith("s:")) {
        String find = path[pathIdx].substring(2);
        for(int a=0;a<dict.list.size();a++) {
          MetaDictEntry de = dict.list.get(a);
          MetaTag key = de.key;
          if (key instanceof MetaData) {
            MetaData str = (MetaData)key;
            if (debug) JFLog.log("compare?" + str.toString() + "==" + find);
            if (str.toString().equals(find)) {
              return de.value;
            }
          }
        }
        if (debug) JFLog.log("Error:string not found:" + find);
        return null;
      }
      if (path[pathIdx].startsWith("i:")) {
        String find = path[pathIdx].substring(2);
        for(int a=0;a<dict.list.size();a++) {
          MetaDictEntry de = dict.list.get(a);
          MetaTag key = de.key;
          if (key instanceof MetaData) {
            MetaData str = (MetaData)key;
            if (str.toString().equals(find)) {
              return de.value;
            }
          }
        }
        JFLog.log("Error:int not found:" + find);
        return null;
      }
    }
    return dict;
  }
}

/** Example layout:
 *
 * 'd'ict
 *  - str("announce") str("URL-IP4")
 *  - str("announce-list") 'l'ist
 *    - 'l'ist
 *      - str("URL-IP4")
 *      - 'e'nd
 *    - 'l'ist
 *      - str("URL-IP6")
 *      - 'e'nd
 *    - 'e'nd
 *  - str("comment") str("...")
 *  - str("created by") str("...")
 *  - str("creation date") int("timestamp") 'e'nd
 *  - str("info") 'd'ict
 *    - str("length") int("...") 'e'nd
 *    - str("name") str("...filename...")
 *    - str("piece length") int("...") 'e'nd
 *    - str("pieces") int("...") ':'
 *      byte [pieces];
 *    - 'e'nd
 *  - 'e'nd
 */

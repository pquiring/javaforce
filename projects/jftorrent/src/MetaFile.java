
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created : May 27, 2012
 *
 * @author pquiring
 */

public class MetaFile {
  private byte metaData[];
  private ArrayList<MetaTag> metaList;  //root tag
  public int tagBegin, tagEnd;  //set by getTag()
  private int metaIdx;
  public void read(byte data[]) throws Exception {
    metaData = data;
    metaIdx = 0;
    metaList = new ArrayList<MetaTag>();
    readMeta(metaList);
  }
  private void readMeta(ArrayList<MetaTag> list) throws Exception {
    while (metaIdx < metaData.length) {
      int tagIdx = metaIdx;
      if ((metaData[metaIdx] >= '0') && (metaData[metaIdx] <= '9')) {
        MetaString tag = new MetaString();
        tag.pos = tagIdx;
        //string length:string data
        String str = "";
        while (metaData[metaIdx] != ':') {
          str += (char)metaData[metaIdx++];
        }
        metaIdx++;
        int strLength = Integer.valueOf(str);
        tag.str = new String(Arrays.copyOfRange(metaData, metaIdx, metaIdx + strLength));
        list.add(tag);
        metaIdx += strLength;
        if (strLength < 128) {
//          if (log) JFLog.log("  create:s:" + new String((byte[])list.get(list.size()-1).obj));
        } else {
//          if (log) JFLog.log("  create:s: ...long string...");
        }
        continue;
      }
      switch (metaData[metaIdx++]) {
        case 'i': {  //integer
          MetaValue tag = new MetaValue();
          tag.pos = tagIdx;
          String str = "";
          while (metaData[metaIdx] != 'e') {
            str += (char)metaData[metaIdx++];
          }
          metaIdx++;
          tag.val = Long.valueOf(str);
          list.add(tag);
//          if (log) JFLog.log("  create:i:" + tag.obj);
          break;
        }
        case 'd': {  //dict
//          if (log) JFLog.log("  create:d");
          MetaList tag = new MetaList();
          list.add(tag);
          readMeta(tag.list);
          tag.endpos = metaIdx-1;
          break;
        }
        case 'l':  //list (handled same as dict)
//          if (log) JFLog.log("  create:l");
          MetaList tag = new MetaList();
          list.add(tag);
          readMeta(tag.list);
          tag.endpos = metaIdx-1;
          break;
        case 'e':
//          if (log) JFLog.log("  end");
          return;
        default:
          throw new Exception("bad torrent");
      }
    }
  }
  public String getString(String path[], ArrayList<MetaTag> list) throws Exception {
    if (list == null) list = metaList;
    MetaTag tag = getTag(path, list);
    if (tag instanceof MetaString) {
      MetaString str = (MetaString)tag;
      return str.str;
    }
    return null;
  }
  public MetaList getList(String path[], ArrayList<MetaTag> list) throws Exception {
    if (list == null) list = metaList;
    MetaTag tag = getTag(path, list);
    if (tag instanceof MetaList) {
      MetaList sublist = (MetaList)tag;
      return sublist;
    }
    return null;
  }
  public long getValue(String path[], ArrayList<MetaTag> list) throws Exception {
    if (list == null) list = metaList;
    MetaTag tag = getTag(path, list);
    if (tag instanceof MetaValue) {
      MetaValue val = (MetaValue)tag;
      return val.val;
    }
    return -1;
  }
  public MetaTag getTag(String path[], ArrayList<MetaTag> list) throws Exception {
    int pathIdx = 0;
    while (pathIdx < path.length) {
      int listLength = list.size();
      if (path[pathIdx].equals("d")) {
//        if (log) JFLog.log("getTag:d0:length=" + listLength);
        MetaList sublist = (MetaList)list.get(0);
        list = (ArrayList<MetaTag>)sublist.list;
        pathIdx++;
        continue;
      }
      if (path[pathIdx].startsWith("d:")) {
//        if (log) JFLog.log("getTag:d:length=" + listLength);
        boolean found = false;
        for(int a=0;a<listLength;a++) {
          Object o = list.get(a);
          if (o instanceof MetaString) {
            MetaString str = (MetaString)o;
            if (str.str.equals(path[pathIdx].substring(2))) {
              if (pathIdx == path.length-1) {
                return list.get(a+1);
              } else {
                MetaList sublist = (MetaList)list.get(a+1);
                list = sublist.list;
                pathIdx++;
                found = true;
                break;
              }
            }
          }
        }
        if (!found) return null;
        continue;
      }
      if (path[pathIdx].startsWith("s:")) {
//        if (log) JFLog.log("getTag:s");
        for(int a=0;a<listLength;a++) {
          Object o = list.get(a);
          if (o instanceof MetaString) {
            MetaString str = (MetaString)o;
            if (str.str.equals(path[pathIdx].substring(2))) {
              tagBegin = list.get(a+1).pos;
              tagEnd = list.get(a+1).endpos;
              return list.get(a+1);
            }
          }
        }
        return null;
      }
      if (path[pathIdx].startsWith("i:")) {
//        if (log) JFLog.log("getTag:i");
        for(int a=0;a<listLength;a++) {
          Object o = list.get(a);
          if (o instanceof MetaString) {
            MetaString str = (MetaString)o;
            if (str.str.equals(path[pathIdx].substring(2))) {
              return list.get(a+1);
            }
          }
        }
        return null;
      }
      if (path[pathIdx].startsWith("l:")) {
//        if (log) JFLog.log("getTag:l:length=" + listLength);
        boolean found = false;
        for(int a=0;a<listLength;a++) {
          Object o = list.get(a);
          if (o instanceof MetaString) {
            MetaString str = (MetaString)o;
            if (str.str.equals(path[pathIdx].substring(2))) {
              if (pathIdx == path.length-1) {
                return list.get(a+1);
              } else {
                MetaList sublist = (MetaList)list.get(a+1);
                list = sublist.list;
                pathIdx++;
                found = true;
                break; //out of for loop
              }
            }
          }
        }
        if (!found) return null;
        continue;
      }
    }
    return null;
  }
}

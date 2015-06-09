
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created : May 27, 2012
 *
 * @author pquiring
 */

public class MetaFile {
  private byte metaData[];
  private ArrayList<MetaTag> metaList;
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
      MetaTag tag = new MetaTag();
      tag.pos = metaIdx;
      if ((metaData[metaIdx] >= '0') && (metaData[metaIdx] <= '9')) {
        //string length:string data
        String str = "";
        while (metaData[metaIdx] != ':') {
          str += (char)metaData[metaIdx++];
        }
        metaIdx++;
        int strLength = Integer.valueOf(str);
        tag.obj = Arrays.copyOfRange(metaData, metaIdx, metaIdx + strLength);
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
        case 'i':  //integer
          String str = "";
          while (metaData[metaIdx] != 'e') {
            str += (char)metaData[metaIdx++];
          }
          metaIdx++;
          tag.obj = Long.valueOf(str);
          list.add(tag);
//          if (log) JFLog.log("  create:i:" + tag.obj);
          break;
        case 'd':  //dict
//          if (log) JFLog.log("  create:d");
          ArrayList<MetaTag> dict = new ArrayList<MetaTag>();
          tag.obj = dict;
          list.add(tag);
          readMeta(dict);
          tag.endpos = metaIdx-1;
          break;
        case 'l':  //list (handled same as dict)
//          if (log) JFLog.log("  create:l");
          ArrayList<MetaTag> sublist = new ArrayList<MetaTag>();
          tag.obj = sublist;
          list.add(tag);
          readMeta(sublist);
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
  public Object getTag(String path[]) throws Exception {
    return getTag(path, metaList);
  }
  public Object getTag(String path[], ArrayList<MetaTag> list) throws Exception {
    int pathIdx = 0;
    while (pathIdx < path.length) {
      int listLength = list.size();
      if (path[pathIdx].equals("d")) {
//        if (log) JFLog.log("getTag:d0:length=" + listLength);
        list = (ArrayList<MetaTag>)list.get(0).obj;
        pathIdx++;
        continue;
      }
      if (path[pathIdx].startsWith("d:")) {
//        if (log) JFLog.log("getTag:d:length=" + listLength);
        boolean found = false;
        for(int a=0;a<listLength;a++) {
          Object o = list.get(a).obj;
          if (o instanceof byte[]) {
            String s = new String((byte[])o, "UTF-8");
            if (s.equals(path[pathIdx].substring(2))) {
              if (pathIdx == path.length-1) {
                return list.get(a+1).obj;
              } else {
                list = (ArrayList<MetaTag>)list.get(a+1).obj;
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
          Object o = list.get(a).obj;
          if (o instanceof byte[]) {
            String s = new String((byte[])o, "UTF-8");
            if (s.equals(path[pathIdx].substring(2))) {
              tagBegin = list.get(a+1).pos;
              tagEnd = list.get(a+1).endpos;
              return list.get(a+1).obj;
            }
          }
        }
        return null;
      }
      if (path[pathIdx].startsWith("i:")) {
//        if (log) JFLog.log("getTag:i");
        for(int a=0;a<listLength;a++) {
          Object o = list.get(a).obj;
          if (o instanceof byte[]) {
            String s = new String((byte[])o, "UTF-8");
            if (s.equals(path[pathIdx].substring(2))) {
              return list.get(a+1).obj;
            }
          }
        }
        return null;
      }
      if (path[pathIdx].startsWith("l:")) {
//        if (log) JFLog.log("getTag:l:length=" + listLength);
        boolean found = false;
        for(int a=0;a<listLength;a++) {
          Object o = list.get(a).obj;
          if (o instanceof byte[]) {
            String s = new String((byte[])o, "UTF-8");
            if (s.equals(path[pathIdx].substring(2))) {
              if (pathIdx == path.length-1) {
                return list.get(a+1).obj;
              } else {
                list = (ArrayList<MetaTag>)list.get(a+1).obj;
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

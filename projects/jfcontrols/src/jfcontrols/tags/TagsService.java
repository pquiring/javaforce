package jfcontrols.tags;

/** Tags Service
 *
 * @author pquiring
*/

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.db.*;
import javaforce.io.*;
import javaforce.controls.*;

import jfcontrols.db.*;
import jfcontrols.app.*;

public class TagsService extends Thread {
  private static Object lock_main = new Object();
  private static Object lock_signal = new Object();
  private static Object lock_tags = new Object();
  private static Object lock_done_reads = new Object();
  private static Object lock_done_writes = new Object();
  private static TagsService service;
  private static volatile boolean active;
  private static volatile boolean doReads;
  private static volatile boolean doWrites;

  public static final int MAX_TAGS = 4096;
  public static TagBase tags[] = new TagBase[MAX_TAGS];  //all tags (local & remote)
  private static HashMap<String, TagBase> map = new HashMap<String, TagBase>();  //local tags only

  public static TagBase getTag(String name) {
    if (name == null || name.length() == 0) return null;
    int cidx = name.indexOf('#');
    if (cidx != -1) {
      //c{id}#tag
      if (name.charAt(0) != 'c') {
        JFLog.log("Error:Invalid Tag Format:" + name);
        return null;
      }
      String cidstr = name.substring(1, cidx);
      int cid = Integer.valueOf(cidstr);
      name = name.substring(cidx + 1);
      return RemoteControllers.getTag(cid, name);
    }
    int tagidx = 0;
    int fieldidx = 0;
    String field = null;
    int idx = name.indexOf(".");
    if (idx != -1) {
      //UDT access
      field = name.substring(idx + 1);
      name = name.substring(0, idx);
      idx = field.indexOf("[");
      if (idx != -1) {
        //field array access (ignore here)
        fieldidx = Integer.valueOf(field.substring(idx + 1, field.length() - 1));
        field = field.substring(0, idx);
      }
    }
    idx = name.indexOf("[");
    if (idx != -1) {
      //array access
      tagidx = Integer.valueOf(name.substring(idx + 1, name.length() - 1));
      name = name.substring(0, idx);
      JFLog.log("tag=" + name + " @ " + tagidx);
    }
    TagBase tag = map.get(name);
    if (tag == null) {
      JFLog.log("Error:getTag(" + name + ") == null");
      return tag;
    }
    if (field != null) {
      TagUDT udt = (TagUDT)tag;
      tag = udt.getField(tagidx, field);
      if (tag == null) {
        JFLog.log("Error:getTag(" + name + ")." + field + " == null");
      }
    } else if (tagidx != -1) {
      if (tag instanceof TagUDT) {
        tag = new TagUDTIndex((TagUDT)tag, tagidx);
      } else {
        tag = new TagIndex(tag, tagidx);
      }
      tag.init(tag);
    }
    return tag;
  }

  public static void main() {
    synchronized(lock_main) {
      new TagsService().start();
      try {lock_main.wait();} catch (Exception e) {}
    }
    //monitor alarms
    TagBase alarms = getTag("alarms");
    alarms.addListener((tag, oldValue, newValue) -> {
      if (oldValue.equals("0") && newValue.equals("1")) {
        Database.logAlarm(-1);  //BUG : need alarm id ???
      }
    });
  }

  private void saveTag(TagBase tag) {
//    JFLog.log("saveTag:" + tag.type + ":" + tag.tid);
    if (tag == null) return;
    try {
      String filename = Paths.tagsPath + "/" + tag.tid + ".dat";
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectWriter oos = new ObjectWriter(baos);
      if (tag.type == -1) {
        JFLog.log("Error:tag.type==-1");
      }
      oos.writeObject(tag, tag.type);
      RandomAccessFile raf = new RandomAccessFile(filename, "rw");
      raf.write(baos.toByteArray());
      raf.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private TagBase loadTag(int tid) {
    try {
      String filename = Paths.tagsPath + "/" + tid + ".dat";
      File file = new File(filename);
      if (!file.exists()) return null;
      RandomAccessFile raf = new RandomAccessFile(filename, "rw");
      int size = (int)raf.length();
      byte data[] = new byte[size];
      int done = 0;
      while (done != size) {
        int read = raf.read(data, done, size - done);
        if (read > 0) {
          done += read;
        }
      }
      raf.close();
      ObjectReader ois = new ObjectReader(new ByteArrayInputStream(data));
      ois.readHeader();
      int type = ois.getType();
      TagBase tag = createTag(type);
      ois.readObject(tag);
      tag.init(tag);
      return tag;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static TagBase createTag(TagBase parent, int cid, int id, int type, int length, String name, String comment) {
//    JFLog.log("createTag:" + cid + "," + id + "," + type + "," + length + "," + name + "," + comment);
    TagBase tag = null;
    if (type > 0xff) {
      //String fields[][] = sql.select("select type, arraysize, name, comment from jfc_udtmems where uid=" + type);
      UDTMember fields[] = Database.getUDTMembersById(type);
      tag = new TagUDT(cid, id, type, length, fields.length);
      //create fields
      if (length == 0) length = 1;
      for(int idx=0;idx<length;idx++) {
        TagBase tagFields[] = tag.getFields(idx);
        for(int fidx=0;fidx<fields.length;fidx++) {
          int ftype = fields[fidx].type;
          int flength = fields[fidx].length;
          String fname = fields[fidx].name;
          String fcomment = fields[fidx].comment;
          tagFields[fidx] = createTag(tag, cid, -1, ftype, flength, fname, fcomment);
        }
      }
    } else {
      switch (type) {
        case TagType.char8: tag = new TagChar8(cid, id, "", length); break;
        case TagType.char16: tag = new TagChar16(cid, id, "", length); break;
        case TagType.bit: tag = new TagByte(cid, id, "", false, length); break;
        case TagType.int8: tag = new TagByte(cid, id, "", false, length); break;
        case TagType.int16: tag = new TagShort(cid, id, "", false, length); break;
        case TagType.int32: tag = new TagInt(cid, id, "", false, length); break;
        case TagType.int64: tag = new TagLong(cid, id, "", false, length); break;
        case TagType.uint8: tag = new TagByte(cid, id, "", true, length); break;
        case TagType.uint16: tag = new TagShort(cid, id, "", true, length); break;
        case TagType.uint32: tag = new TagInt(cid, id, "", true, length); break;
        case TagType.uint64: tag = new TagLong(cid, id, "", true, length); break;
        case TagType.float32: tag = new TagFloat(cid, id, "", length); break;
        case TagType.float64: tag = new TagDouble(cid, id, "", length); break;
        case TagType.string: tag = new TagString(cid, id, "", length); break;
        default: JFLog.log("TagsService.createTag() unknown type:" + type); break;
      }
    }
    tag.name = name;
    tag.comment = comment;
    tag.isArray = length > 0 || type == TagType.string;
    tag.init(tag);
    return tag;
  }

  public static void deleteTag(int id) {
    TagBase tag = tags[id];
    tags[tag.tid] = null;
    if (tag.cid == 0) {
      map.put(tag.name, null);
    } else {
      RemoteControllers.deleteTag(tag);
    }
  }

  public void run() {
    JFLog.log("TagServer starting...");
    service = this;
    registerAll();
    TagRow tagList[] = Database.getTags();
    for(int a=0;a<tagList.length;a++) {
      int tid = tagList[a].id;
      int cid = tagList[a].cid;
      int type = tagList[a].type;
      int length = tagList[a].length;
      String name = tagList[a].name;
      String comment = tagList[a].comment;
      TagBase tag = loadTag(tid);
      if (tag == null) {
        tag = createTag(null, cid, tid, type, length, name, comment);
      }
      JFLog.log("loadTag:" + tid + ":c" + tag.cid + ":" + tag.name);
      tags[tid] = tag;
      if (cid == 0) {
        map.put(tag.name, tag);
      }
    }
    active = true;
    synchronized(lock_main) {
      lock_main.notify();  //signal main to continue
    }
    synchronized(lock_signal) {
      while (active) {
        try {lock_signal.wait();} catch (Exception e) {}
        if (doReads) {
          processReads();
        }
        if (doWrites) {
          processWrites();
        }
        processSaves();
      }
    }
    service = null;
  }
  private void processReads() {
    for(int a=0;a<tags.length;a++) {
      TagBase tag = tags[a];
      if (tag == null) continue;
      if (tag.cid > 0) {
        if (tag.remoteTag == null) continue;
        tag.updateValue(tag.remoteTag.getValue());
      }
    }
    synchronized(lock_done_reads) {
      doReads = false;
      lock_done_reads.notify();
    }
  }
  private void processWrites() {
    for(int a=0;a<tags.length;a++) {
      TagBase tag = tags[a];
      if (tag == null) continue;
      if (tag.cid > 0) {
        if (tag.remoteTag == null) continue;
        if (tag.dirty) {
          JFLog.log("RemoteTag:c" + tag.cid + ":" + tag.name + ":setValue:" + tag.getValue());
          tag.remoteTag.setValue(tag.getValue());
        }
      }
    }
    synchronized(lock_done_writes) {
      doWrites = false;
      lock_done_writes.notify();
    }
  }
  private void processSaves() {
    for(int a=0;a<tags.length;a++) {
      TagBase tag = tags[a];
      if (tag == null) continue;
//      if (tag.tid == 2) JFLog.log("check:" + tag.cid + ":" + tag.tid + ":" + tag.name + ":" + tag.type + ":" + tag.dirty);
      if (tag.dirty) {
        JFLog.log("SetClean:" + tag.name);
        tag.dirty = false;
        saveTag(tag);
      }
    }
  }
  public static void doReads() {
    synchronized(lock_done_reads) {
      synchronized(lock_signal) {
        doReads = true;
        lock_signal.notify();
      }
      try {lock_done_reads.wait();} catch (Exception e) {}
    }
  }
  public static void doWrites() {
    synchronized(lock_done_writes) {
      synchronized(lock_signal) {
        doWrites = true;
        lock_signal.notify();
      }
      try {lock_done_writes.wait();} catch (Exception e) {}
    }
  }
  public static void cancel() {
    synchronized(lock_signal) {
      active = false;
      lock_signal.notify();
    }
  }
  public static boolean validTagName(int cid, String tag) {
    //first letter must be a-z or A-Z or _
    //other chars may also include : 0-9
    tag = tag.toLowerCase();
    if (tag.length() == 0) return false;
    if (cid != 0) return true;  //controller tags
    char ch = tag.charAt(0);
    if ((ch < 'a' || ch > 'z') && (ch != '_')) return false;
    char ca[] = tag.toCharArray();
    for(int a=0;a<ca.length;a++) {
      ch = ca[a];
      if (ch >= 'a' && ch <= 'z') continue;
      if (ch == '_') continue;
      if (ch >= '0' && ch <= '9') continue;
      return false;
    }
    return true;
  }
  private static HashMap<Integer, TagBase.Creator> tagTypes = new HashMap<>();
  public static TagBase createTag(int id) {
    TagBase.Creator creator = tagTypes.get(id);
    if (creator == null) {
      if (id < IDs.uid_sdt || id > IDs.uid_user_end) {
        JFLog.log("Error:Unknown tag type:" + id);
        return null;
      }
      return new TagUDT(id);
    }
    return creator.create();
  }
  private static void registerTag(int id, TagBase.Creator creator) {
    tagTypes.put(id, creator);
  }
  private static void registerAll() {
    registerTag(TagType.int8, () -> {return new TagByte(false);});
    registerTag(TagType.char16, () -> {return new TagChar16();});
    registerTag(TagType.char8, () -> {return new TagChar8();});
    registerTag(TagType.int16, () -> {return new TagShort(false);});
    registerTag(TagType.int32, () -> {return new TagInt(false);});
    registerTag(TagType.int64, () -> {return new TagLong(false);});
    registerTag(TagType.float32, () -> {return new TagFloat();});
    registerTag(TagType.float64, () -> {return new TagDouble();});
    registerTag(TagType.string, () -> {return new TagString();});
    registerTag(TagType.uint8, () -> {return new TagByte(true);});
    registerTag(TagType.uint16, () -> {return new TagShort(true);});
    registerTag(TagType.uint32, () -> {return new TagInt(true);});
    registerTag(TagType.uint64, () -> {return new TagLong(true);});
  }
}

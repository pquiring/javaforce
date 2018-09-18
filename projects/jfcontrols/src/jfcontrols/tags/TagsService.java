package jfcontrols.tags;

/** Tags Service
 *
 * @author pquiring
*/

import java.io.*;
import java.util.*;

import javaforce.*;
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
  public static TagBase tags[] = new TagBase[MAX_TAGS];
  private static HashMap<String, TagBase> map = new HashMap<String, TagBase>();

  public static TagBase getTag(String name) {
    //TODO : c# // controller #
    if (name == null) return null;
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
    }
    TagBase tag = map.get(name);
    if (field != null) {
      TagUDT udt = (TagUDT)tag;
      tag = udt.getField(tagidx, field);
    }
    if (tag == null) {
      JFLog.log("Error:getTag(" + name + ") == null");
    }
    return tag;
  }

  public static TagBase getTag(int idx) {
    return tags[idx];
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
    JFLog.log("saveTag:" + tag.tid);
    if (tag == null) return;
    try {
      String filename = Paths.tagsPath + "/" + tag.tid + ".dat";
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(tag);
      RandomAccessFile raf = new RandomAccessFile(filename, "rw");
      raf.writeInt(baos.size());
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
      int size = raf.readInt();
      if (size > 4 * 1024 * 1024) throw new Exception("tag too large");
      byte data[] = new byte[size];
      int done = 0;
      while (done != size) {
        int read = raf.read(data, done, size - done);
        if (read > 0) {
          done += read;
        }
      }
      raf.close();
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
      TagBase tag = (TagBase)ois.readObject();
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

  public static void addTag(TagBase tag) {
    if (tag.cid > 0) {
      tag.remoteTag = RemoteControllers.getTag(tag);
    }
    tags[tag.tid] = tag;
    map.put(tag.name, tag);
  }

  public static void deleteTag(int id) {
    TagBase tag = tags[id];
    tags[tag.tid] = null;
    map.put(tag.name, null);
  }

  public void run() {
    service = this;
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
      tags[tid] = tag;
      map.put(tag.name, tag);
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
        tag.setValue(tag.remoteTag.getValue());
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
        tag.remoteTag.setValue(tag.getValue());
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
  public static boolean validTagName(String tag) {
    //first letter must be a-z or A-Z
    //other chars : 0-9 _
    tag = tag.toLowerCase();
    if (tag.length() == 0) return false;
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
}

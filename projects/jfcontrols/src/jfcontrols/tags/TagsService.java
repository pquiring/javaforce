package jfcontrols.tags;

/** Tags Service
 *
 * @author pquiring
*/

import java.util.*;

import javaforce.*;
import javaforce.controls.TagType;

import jfcontrols.sql.*;

public class TagsService extends Thread {
  private static Object lock = new Object();
  private static Object done = new Object();
  private static TagsService service;
  private static volatile boolean active;
  private static volatile boolean doReads;
  private static volatile boolean doWrites;

  private HashMap<String, TagBase> localTags = new HashMap<>();
  private HashMap<String, TagBase> remoteTags = new HashMap<>();

  public static String read(String tag) {
    TagAddr ta = TagAddr.decode(tag);
    return getTag(ta).getValue(ta);
  }
  public static void write(String tag, String value) {
    TagAddr ta = TagAddr.decode(tag);
    getTag(ta).setValue(ta, value);
  }
  public static TagBase getTag(TagAddr ta) {
    return service.get_Tag(ta);
  }
  public static void main() {
    synchronized(lock) {
      new TagsService().start();
      try {lock.wait();} catch (Exception e) {}
    }
  }
  public void run() {
    service = this;
    SQL sql = SQLService.getSQL();
    String tags[][] = sql.select("select name,type,cid,unsigned,array from tags");
    for(int a=0;a<tags.length;a++) {
      if (tags[a][2].equals("0")) {
//  public LocalTag(int cid, String name, int type, boolean unsigned, boolean array, boolean udt, int uid, SQL sql) {
        int type = Integer.valueOf(tags[a][1]);
        boolean udt = type >= 0x100;
        localTags.put(tags[a][0], new LocalTag(0, tags[a][0], type, tags[a][3].equals("true"), tags[a][4].equals("true"), udt, sql));
      } else {
        remoteTags.put("c" + tags[a][2] + "#" + tags[a][0], new RemoteTag(Integer.valueOf(tags[a][2]), tags[a][0], Integer.valueOf(tags[a][1]), tags[a][3].equals("true"), false, sql));
      }
    }
    active = true;
    synchronized(lock) {
      lock.notify();  //signal main to continue
    }
    while (active) {
      synchronized(lock) {
        try {lock.wait();} catch (Exception e) {}
        if (doReads) {
          processReads(sql);
          doReads = false;
        }
        if (doWrites) {
          processWrites(sql);
          doWrites = false;
        }
      }
    }
    service = null;
    sql.close();
  }
  public void processReads(SQL sql) {
    MonitoredTag localtags[] = (MonitoredTag[])localTags.values().toArray(new MonitoredTag[0]);
    for(int a=0;a<localtags.length;a++) {
      localtags[a].updateRead(sql);
    }
    MonitoredTag remotetags[] = (MonitoredTag[])remoteTags.values().toArray(new MonitoredTag[0]);
    for(int a=0;a<remotetags.length;a++) {
      remotetags[a].updateRead(sql);
    }
    synchronized(done) {
      done.notify();
    }
  }
  public void processWrites(SQL sql) {
    MonitoredTag localtags[] = (MonitoredTag[])localTags.values().toArray(new MonitoredTag[0]);
    for(int a=0;a<localtags.length;a++) {
      localtags[a].updateWrite(sql);
    }
    MonitoredTag remotetags[] = (MonitoredTag[])remoteTags.values().toArray(new MonitoredTag[0]);
    for(int a=0;a<remotetags.length;a++) {
      remotetags[a].updateWrite(sql);
    }
    synchronized(done) {
      done.notify();
    }
  }
  public TagBase get_Tag(TagAddr ta) {
    if (ta.tempValue != null) {
      return new TagTemp(ta.tempValue);
    }
    synchronized(lock) {
      if (ta.cid == 0) {
        TagBase tag = localTags.get(ta.name);
        return tag;
      } else {
        return remoteTags.get(ta.name);
      }
    }
  }
  public static void doReads() {
    synchronized(done) {
      doReads = true;
      synchronized(lock) {
        lock.notify();
      }
      try {done.wait();} catch (Exception e) {}
    }
  }
  public static void doWrites() {
    synchronized(done) {
      doWrites = true;
      synchronized(lock) {
        lock.notify();
      }
      try {done.wait();} catch (Exception e) {}
    }
  }
  public static void cancel() {
    synchronized(lock) {
      active = false;
      lock.notify();
    }
  }
}

package jfcontrols.tags;

/** Tags Service
 *
 * @author pquiring
*/

import java.util.*;

import javaforce.*;

import jfcontrols.sql.*;

public class TagsService extends Thread {
  private static Object lock = new Object();
  private static Object done = new Object();
  private static TagsService service;
  private static volatile boolean active;
  private static volatile boolean doReads;
  private static volatile boolean doWrites;

  private HashMap<String, Tag> localTags;
  private HashMap<String, Tag> remoteTags;

  public static String read(String tag) {
    return getTag(tag).getValue();
  }
  public static void write(String tag, String value) {
    getTag(tag).setValue(value);
  }
  public static Tag getTag(String name) {
    return service.get_Tag(name);
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
    String tags[][] = sql.select("select name,type from tags where cid=0");
    for(int a=0;a<tags.length;a++) {
      localTags.put(tags[a][0], new LocalTag(tags[a][0], Integer.valueOf(tags[a][1]), sql));
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
    Tag tags[] = localTags.values().toArray(new Tag[0]);
    for(int a=0;a<tags.length;a++) {
      tags[a].updateRead(sql);
    }
    synchronized(done) {
      done.notify();
    }
  }
  public void processWrites(SQL sql) {
    Tag tags[] = localTags.values().toArray(new Tag[0]);
    for(int a=0;a<tags.length;a++) {
      tags[a].updateWrite(sql);
    }
    synchronized(done) {
      done.notify();
    }
  }
  public Tag get_Tag(String name) {
    synchronized(lock) {
      return localTags.get(name);
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

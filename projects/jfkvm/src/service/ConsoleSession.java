package service;

/** Console Session
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.vm.*;

public class ConsoleSession {
  public String id;
  public VirtualMachine vm;
  public long ts;

  private static HashMap<String, ConsoleSession> sessions = new HashMap<>();

  public static ConsoleSession get(String id) {
    return sessions.remove(id);
  }

  public void put() {
    sessions.put(id, this);
  }

  private static final long _24_hrs = 24 * 60 * 60 * 1000;

  public static void clean() {
    long ts = System.currentTimeMillis() - _24_hrs;
    String[] keys = sessions.keySet().toArray(JF.StringArrayType);
    if (keys == null || keys.length == 0) return;
    for(String key : keys) {
      ConsoleSession sess = sessions.get(key);
      if (sess == null) continue;
      if (sess.ts < ts) {
        sessions.remove(key);
      }
    }
  }
}

package service;

/** Terminal Session
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.lxc.*;

public class TerminalSession {
  public String id;
  public LxcContainer c;
  public long ts;

  private static HashMap<String, TerminalSession> sessions = new HashMap<>();

  public static TerminalSession get(String id) {
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
      TerminalSession sess = sessions.get(key);
      if (sess == null) continue;
      if (sess.ts < ts) {
        sessions.remove(key);
      }
    }
  }
}

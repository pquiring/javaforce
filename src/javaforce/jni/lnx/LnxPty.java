package javaforce.jni.lnx;

/** Linux PTY support.
 *
 * @author pquiring
 *
 * Created : Jan 17, 2014
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.jni.*;

public class LnxPty {

  private static final boolean debug = true;

  public static boolean init() {
    return true;
  }

  /** Spawns cmd with args and env (both must be null terminated arrays) and returns new pty. */
  public static LnxPty exec(String cmd, String args[], String env[]) {
    LnxPty pty = new LnxPty();
    if (!pty.fork(cmd, args, env)) return null;
    return pty;
  }

  /** Spawns cmd with args and env (both must be null terminated arrays).
   * Captures all output (stdout and stderr) and returns it.
   * Does not return until child process exits. */
  public static String execOutput(String cmd, String args[], String env[]) {
    LnxPty pty = new LnxPty();
    if (!pty.fork(cmd, args, env)) return null;
    StringBuilder sb = new StringBuilder();
    byte data[] = new byte[1024];
    while (true) {
      int read = pty.read(data);
      if (read == -1) break;
      sb.append(new String(data, 0, read));
    }
    pty.close();
    return sb.toString();
  }

  public boolean isClosed() {
    return closed;
  }

  private boolean closed = false;
  private long ctx;

  private Process p;

  /** Spawns a new process with a new pty.
   * Note:args, env MUST be null terminated.
   */
  private boolean fork(String cmd, String args[], String env[]) {
    ctx = LnxNative.ptyAlloc();
    if (debug) JFLog.log("LnxPty:ctx=0x" + Long.toHexString(ctx));
    String slaveName = LnxNative.ptyOpen(ctx);
    if (debug) JFLog.log("LnxPty:slaveName=" + slaveName);
    if (slaveName == null) return false;

    ArrayList<String> cmdline = new ArrayList<String>();
    cmdline.add("/usr/bin/jfpty");
    cmdline.add(slaveName);
    cmdline.add(cmd);
    cmdline.add(Integer.toString(args.length-1));  //# args
    for(int a=0;a<args.length;a++) {
      if (args[a] == null) break;
      cmdline.add(args[a]);
    }
    for(int a=0;a<env.length;a++) {
      if (env[a] == null) break;
      cmdline.add(env[a]);
    }
    String cl[] = cmdline.toArray(new String[0]);
    if (debug) {
      for(int a=0;a<cl.length;a++) {
        JFLog.log("cmd=" + cl[a]);
      }
    }
    try {
      ProcessBuilder pb = new ProcessBuilder(cl);
      String user = System.getenv("USER");
      if (user != null) {
        if (user.equals("root")) {
          pb.directory(new File("/root"));
        } else {
          pb.directory(new File("/home/" + user));
        }
      }
      if (debug) pb.redirectOutput(new File("debug.txt"));
      p = pb.start();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }

    new Thread() {
      public void run() {
        try {p.waitFor();} catch (Exception e) {}
        close();
      }
    }.start();
    return true;
  }

  /** This is the child process for fork() implementation.
   */
  public static void main(String args[]) {
    if (args == null || args.length < 3) {
      System.out.println("Usage : LnxPty slaveName, cmd, #args, [args...], [env...]");
      return;
    }
    init();

    String slaveName = args[0];
    String cmd = args[1];
    int noArgs = JF.atoi(args[2]);
    int p = 3;
    ArrayList<String> process_args = new ArrayList<String>();
    ArrayList<String> process_env = new ArrayList<String>();
    for(int a=0;a<noArgs;a++) {
      process_args.add(args[p++]);
    }
    while (p < args.length) {
      process_env.add(args[p++]);
    }

    try {
      //JFNative.load();
      LnxNative.ptyChildExec(slaveName, cmd, process_args.toArray(new String[0]), process_env.toArray(new String[0]));
      System.exit(0);  //should not happen
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  /** Frees resources */
  public synchronized void close() {
    if (closed) return;
    if (debug) JFLog.log("LnxPty:close()");
    LnxNative.ptyClose(ctx);
    closed = true;
  }

  /** Writes to child process (max 1024 bytes) */
  public void write(byte[] buf) {
    LnxNative.ptyWrite(ctx, buf);
  }

  /** Reads from child process (max 1024 bytes) */
  public int read(byte[] buf) {
    if (closed) return -1;
    return LnxNative.ptyRead(ctx, buf);
  }

  /** Sets the size of the pty */
  public void setSize(int x, int y) {
    LnxNative.ptySetSize(ctx, x, y);
  }

  /** Returns current processes environment variables plus those passed in extra.
   * extra overrides current variables.
   */
  public static String[] makeEnvironment(String[] extra) {
    ArrayList<String> env = new ArrayList<String>();
    Map<String, String> old = System.getenv();
    Set<String> set = old.keySet();
    String[] keys = (String[])set.toArray(new String[0]);
    for(int a=0;a<keys.length;a++) {
      String key = keys[a];
      env.add(key + "=" + old.get(key));
    }
    if (extra != null) {
      for(int a=0;a<extra.length;a++) {
        int idx = extra[a].indexOf("=");
        if (idx == -1) continue;  //bad string
        String key = extra[a].substring(0, idx+1);
        int cnt = env.size();
        boolean found = false;
        for(int b=0;b<cnt;b++) {
          if (env.get(b).startsWith(key)) {
            env.set(b, extra[a]);
            found = true;
            break;
          }
        }
        if (!found) {
          env.add(extra[a]);
        }
      }
    }
    env.add(null);
    return env.toArray(new String[0]);
  }
  public static String[] makeEnvironment() {
    return makeEnvironment(null);
  }
}

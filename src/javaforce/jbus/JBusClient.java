package javaforce.jbus;

/**
 * JBusClient is the client side of inter-process communications (RPC).
 *
 * Each client has a package name in dot notation (ie: client001.example.net)
 * The client will have methods (functions) that other clients can invoke.
 * Arguments must be "strings" or integers (ie: "\"test\",123")
 *
 * Created : Apr 9, 2012
 *
 * @author pquiring
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import javaforce.*;

public class JBusClient extends Thread {

  public String pack;
  private Object obj;
  private Class<?> cls;
  private Dispatch dispatch;
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private volatile boolean ready;
  private boolean debug = false;
  private int port = 777;

  /** Creates new client.
   * @param pack = client name in dot notation
   * @param obj = object with methods to invoke for RPC calls
   */
  public JBusClient(String pack, Object obj) {
    if ((pack == null) || (obj == null)) {
      return;
    }
    this.pack = pack;
    this.obj = obj;
    cls = obj.getClass();
  }

  /** Setup alternative message dispatch. */
  public void setDispatch(Dispatch dispatch) {
    this.dispatch = dispatch;
  }

  /** Enable logging exceptions to console. */
  public void setDebug(boolean state) {
    debug = state;
  }

  /** Set server port.  Must be called before start(). */
  public void setPort(int port) {
    this.port = port;
  }

  public void run() {
    try {
      JFLog.log("JBusClient:connecting to 127.0.0.1:" + port);
      s = new Socket(InetAddress.getByName("127.0.0.1"), port);
      is = s.getInputStream();
      os = s.getOutputStream();
      if (pack != null) {
        os.write(("cmd.package=" + pack + "\n").getBytes());
        os.flush();
        if (debug) JFLog.log("JBus Client registered as : " + pack);
      }
      ready = true;
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      while (s.isConnected()) {
        String cmd = br.readLine();
        if (cmd == null) {
          break;
        }
        try {
          doCmd(cmd);
        } catch (InvocationTargetException ite) {
          Throwable cause = ite.getCause();
          if (cause == null) {
            JFLog.log(ite);
          } else {
            JFLog.log(cause);
          }
        } catch (Exception e1) {
          JFLog.log(e1);
        }
      }
    } catch (SocketException e2) {
      if (debug) JFLog.log("JBus Client closed : " + pack);
    } catch (Exception e3) {
      if (debug) JFLog.log(e3);
    }
  }

  private void doCmd(String cmd) throws Exception {
    if (cmd.startsWith("cmd.")) {
      //unknown cmd
      return;
    }
    //must be a remote function call
    //general format : org.package.func(args)
    //supported args : "String", int
    if (cls == null) {
      if (dispatch != null) {
        dispatch.onMessage(cmd);
      }
      return;  //not accepting calls
    }
    int b1 = cmd.indexOf("(");
    int b2 = cmd.length() - 1;
    String argsString = cmd.substring(b1 + 1, b2);
    String packFunc = cmd.substring(0, b1);
    int idx = packFunc.lastIndexOf('.');
    String call_pack = packFunc.substring(0, idx);
    if (!call_pack.equals(pack)) {
      throw new Exception("package mismatch");  //error : should not happen
    }
    String call_func = packFunc.substring(idx + 1);
    if (debug) JFLog.log("call:" + call_pack + "." + call_func + "(" + argsString + ")");
    Object[] args = parseArgs(argsString);
    int argsLength = args.length;
    Class[] types = new Class[argsLength];
    for (int a = 0; a < argsLength; a++) {
      if (args[a] instanceof Integer) {
        types[a] = int.class;  //not the same as Integer.class
      } else {
        types[a] = args[a].getClass();  //String.class
      }
    }
    Method method = cls.getMethod(call_func, types);
    method.invoke(obj, args);
  }

  private Object[] parseArgs(String args) throws Exception {
    ArrayList<Object> ret = new ArrayList<Object>();
    //"string", int, ...
    int pos = 0;
    int len = args.length();
    while (pos < len) {
      if (args.charAt(pos) == '\"') {
        //decode String
        int q1 = pos;
        int q2 = args.indexOf('\"', q1 + 1);
        String str = args.substring(q1 + 1, q2);
        ret.add(str);
        pos = q2 + 1;
      } else {
        //decode Integer
        int nxt = args.indexOf(',', pos);
        if (nxt == -1) {
          nxt = len;
        }
        Integer i = Integer.valueOf(args.substring(pos, nxt));
        ret.add(i);
        pos = nxt;
      }
      if (pos >= len) {
        break;
      }
      if (args.charAt(pos) != ',') {
        throw new Exception("bad args");
      }
      pos++;
    }
    return ret.toArray();
  }

  /** Invoke function in another client.
   * @param pack = other clients name (package)
   * @param func = method to invoke
   * @param args = comma list of arguments to pass ("strings" or integers only)
   */
  public boolean call(String pack, String func, String args) {
    return call(pack + "." + func + "(" + args + ")\n");
  }

  /** Invoke function in another client using condensed package.function.args format.
   * @param pfa = package.function(args)
   * There should be no quote around args (just around strings if there are any)
   */
  public boolean call(String pfa) {
    int cnt = 0;
    while (!ready) {
      cnt++;
      if (cnt == 30) {
        return false;  //failed
      }
      JF.sleep(100);
    }
    try {
      os.write(pfa.getBytes());
      os.flush();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Invoke function in all client matching package.
   * @param pack = other clients name (package) if client.package.startsWith(pack) it will receive message
   * @param func = method to invoke
   * @param args = comma list of arguments to pass ("strings" or integers only)
   */
  public boolean broadcast(String pack, String func, String args) {
    return call("cmd.broadcast=" + pack + "." + func + "(" + args + ")\n");
  }

  /** Returns if client is connected to server and ready. */
  public boolean ready() {
    return ready;
  }

  /** Closes connection to server. */
  public void close() {
    try {
      s.close();
    } catch (Exception e) {
    }
    s = null;
  }

  /** Quotes a string. */
  public static String quote(String str) {
    return "\"" + str + "\"";
  }

  /** Encode a string if it contains quotes, etc. */
  public static String encodeString(String in) {
    try {
      return URLEncoder.encode(in, "UTF-8");
    } catch (Exception e) {
      return null;
    }
  }

  /** Decode a string if it contains quotes, etc. */
  public static String decodeString(String in) {
    try {
      return URLDecoder.decode(in, "UTF-8");
    } catch (Exception e) {
      return null;
    }
  }

  /** Encode byte[] as a String. */
  public static String encodeByteArray(byte[] ba) {
    StringBuilder sb = new StringBuilder();
    sb.append("\"");
    for(int a=0;a<ba.length;a++) {
      sb.append(String.format("%02x", ba[a] & 0xff));
    }
    sb.append("\"");
    return sb.toString();
  }

  /** Decode String as byte[] */
  public static byte[] decodeByteArray(String str) {
    if (str.length() == 0) return new byte[0];
    if (str.charAt(0) == '\"') {
      str = str.substring(1, str.length() - 1);
    }
    int len = str.length() / 2;
    if (len * 2 != str.length()) {
      JFLog.log("JBusClient.decodeByteArray:Error:str.length()=" + str.length());
      return new byte[0];
    }
    byte[] ba = new byte[len];
    for(int a=0;a<len;a++) {
      int pos = a*2;
      ba[a] = (byte)(Integer.parseInt(str.substring(pos, pos+2), 16));
    }
    return ba;
  }
}

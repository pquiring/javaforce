package javaforce.jbus;

/**
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
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private volatile boolean ready;
  private int readyCnt = 0;

  public JBusClient(String pack, Object obj) {
    if ((pack == null) || (obj == null)) {
      return;
    }
    this.pack = pack;
    this.obj = obj;
    cls = obj.getClass();
  }

  public void run() {
    try {
      s = new Socket(InetAddress.getByName("127.0.0.1"), JBusServer.port);
      is = s.getInputStream();
      os = s.getOutputStream();
      if (pack != null) {
        os.write(("cmd.package=" + pack + "\n").getBytes());
        os.flush();
//        JFLog.log("JBus Client registered as : " + pack);
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
      JFLog.log("JBus Client closed : " + pack);
    } catch (Exception e3) {
      JFLog.log(e3);
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
//    JFLog.log("call:" + call_pack + "." + call_func + "(" + argsString + ")");  //test
    Object args[] = parseArgs(argsString);
    int argsLength = args.length;
    Class types[] = new Class[argsLength];
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

  public boolean call(String pack, String func, String args) {
    return call(pack + "." + func + "(" + args + ")\n");
  }

  public boolean call(String pfa) {
    while (!ready) {
      readyCnt++;
      if (readyCnt == 30) {
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

  private boolean ready() {
    return ready;
  }

  public void close() {
    try {
      s.close();
    } catch (Exception e) {
    }
    s = null;
  }

  public static String quote(String str) {
    return "\"" + str + "\"";
  }

  public static String encodeSafe(String in) {
    return in.replaceAll("&", "&amp;").replaceAll("\"", "&quot;");  //order matters here
  }

  public static String decodeSafe(String in) {
    return in.replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
  }
}

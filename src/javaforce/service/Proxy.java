package javaforce.service;

/**
 * A simple Proxy Server
 *
 * Supports : SSL (CONNECT)
 *
 * jfproxy.cfg example:
 *   [global]
 *   port=3128
 *   allow=0.0.0.0/0
 *   [blockdomain]
 *   .*youtube[.]com
 *   [urlchange]
 *   url = newURL
 *
 * Note : is [blockdomain] section the domains are in Regular Expression format
 * see : http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
 *
 * Note : in [urlchange] section the url is a regular expression
 *   and the = MUST have a space before and after it.
 *
 * @author pquiring
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.jbus.*;

public class Proxy extends Thread {

  public final static String busPack = "net.sf.jfproxy";

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfproxy.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfproxy.log";
  }

  private static class URLChange {
    public String url, newurl;
  }

  private ServerSocket ss;
  private Vector<Session> list = new Vector<Session>();
  private ArrayList<String> blockedDomain = new ArrayList<String>();
  private ArrayList<URLChange> urlChanges = new ArrayList<URLChange>();
  private ArrayList<Integer> allow_net = new ArrayList<Integer>();
  private ArrayList<Integer> allow_mask = new ArrayList<Integer>();
  private int port = 3128;

  public void close() {
    JFLog.logTrace("proxy.close()");
    try {
      ss.close();
    } catch (Exception e) {}
    busClient.close();
    //close list
    Session sess;
    while (list.size() > 0) {
      sess = list.get(0);
      sess.close();
    }
  }

  public void run() {
    JFLog.init(getLogFile(), true);
    Socket s;
    Session sess;
    loadConfig();
    busClient = new JBusClient(busPack, new JBusMethods());
    busClient.setPort(getBusPort());
    busClient.start();
    //try to bind to port 5 times (in case restart() takes a while)
    for(int a=0;a<5;a++) {
      try {
        ss = new ServerSocket(port);
      } catch (Exception e) {
        if (a == 4) return;
        JF.sleep(1000);
        continue;
      }
      break;
    }
    try {
      JFLog.log("Starting proxy on port " + port);
      //read newJS
      while (!ss.isClosed()) {
        s = ss.accept();
        sess = new Session(s);
        sess.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private static enum Section {None, Global, BlockDomain, URLChange};

  private final static String defaultConfig
    = "[global]\r\n"
    + "port=3128\r\n"
    + "allow=0.0.0.0/0 #allow all\r\n"
    + "#allow=192.168.0.0/24 #allow subnet\r\n"
    + "#allow=10.1.2.3/32 #allow single ip\r\n"
    + "\r\n"
    + "[blockdomain]\r\n"
    + ".*youtube[.]com\r\n"
    + "\r\n"
    + "[urlchange]\r\n"
    + "#www.example.com/test = www.google.com\r\n";

  private void loadConfig() {
    Section section = Section.None;
    try {
      StringBuilder cfg = new StringBuilder();
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\r\n");
        ln = ln.trim().toLowerCase();
        int idx = ln.indexOf('#');
        if (idx != -1) ln = ln.substring(0, idx).trim();
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        if (ln.equals("[blockdomain]")) {
          section = Section.BlockDomain;
          continue;
        }
        if (ln.equals("[urlchange]")) {
          section = Section.URLChange;
          continue;
        }
        switch (section) {
          case Global:
            if (ln.startsWith("port=")) {
              port = JF.atoi(ln.substring(5));
            }
            if (ln.startsWith("allow=")) {
              String net_mask = ln.substring(6);
              idx = net_mask.indexOf("/");
              String net = net_mask.substring(0, idx);
              int addr = getIP(net);
              allow_net.add(addr);
              String mask = net_mask.substring(idx+1);
              int maskBits = getMask(mask);
              allow_mask.add(maskBits);
            }
            break;
          case BlockDomain:
            blockedDomain.add(ln);
            break;
          case URLChange:
            int eq = ln.indexOf(" = ");
            if (eq == -1) {
              JFLog.log("Bad URLChange:" + ln);
              break;
            }
            URLChange uc = new URLChange();
            uc.url = ln.substring(0, eq);
            uc.newurl = ln.substring(eq+3);
            urlChanges.add(uc);
            break;
        }
      }
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(defaultConfig.getBytes());
        fos.close();
        config = defaultConfig;
      } catch (Exception e2) {
        JFLog.log(e2);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private int ba2int(byte ba[]) {
    int ret = 0;
    for(int a=0;a<4;a++) {
      ret <<= 8;
      ret += ((int)ba[a]) & 0xff;
    }
    return ret;
  }

  private int getIP(String ip) {
    String p[] = ip.split("[.]");
    byte o[] = new byte[4];
    for(int a=0;a<4;a++) {
      o[a] = (byte)JF.atoi(p[a]);
    }
    return ba2int(o);
  }

  private int getMask(String mask) {
    int bits = JF.atoi(mask);
    if (bits == 0) return 0;
    int ret = 0x80000000;
    bits--;
    while (bits > 0) {
      ret >>= 1;  //signed shift will repeat the sign bit (>>>=1 would not)
      bits--;
    }
    return ret;
  }

  private class Session extends Thread {
    private Socket p, i;  //proxy, internet
    private InputStream pis, iis;
    private OutputStream pos, ios;
    private boolean disconn = false;
    private int client_port;
    private String client_ip;
    public synchronized void close() {
      try {
        if ((p!=null) && (p.isConnected())) p.close();
        p = null;
      } catch (Exception e1) {}
      try {
        if ((i!=null) && (i.isConnected())) i.close();
        i = null;
      } catch (Exception e2) {}
      list.remove(this);
    }
    public Session(Socket s) {
      p = s;
    }
    public String toString(int ip) {
      long ip64 = ((long)ip) & 0xffffffffL;
      return Long.toString(ip64, 16);
    }
    private void log(String s) {
      JFLog.log(client_ip + ":" + client_port + ":" + s);
    }
    private void log(Exception e) {
      String s = e.toString();
      StackTraceElement stack[] = e.getStackTrace();
      for(int a=0;a<stack.length;a++) {
        s += "\r\n" + stack[a].toString();
      }
      log(s);
    }
    public void run() {
      String req = "";
      int ch;
      list.add(this);
      client_port = p.getPort();
      client_ip = p.getInetAddress().getHostAddress();
      log("Session Start");
      try {
        boolean allowed = false;
        for(int a=0;a<allow_net.size();a++) {
          int net = allow_net.get(a);
          int mask = allow_mask.get(a);
          int host = getIP(p);
          if ((net & mask) == (host & mask)) {
            allowed = true;
            break;
          }
        }
        if (!allowed) throw new Exception("client not allowed");
        pis = p.getInputStream();
        pos = p.getOutputStream();
        while (true) {
          req = "";
          log("reading request");
          do {
            ch = pis.read();
            if (ch == -1) throw new Exception("read error");
            req += (char)ch;
          } while (!req.endsWith("\r\n\r\n"));
          proxy(req);
          if (disconn) {
            log("disconn");
            break;
          }
        }
        p.close();
      } catch (Exception e) {
        if (req.length() > 0) log(e);
      }
      close();
      log("Session Stop");
    }
    private int getIP(Socket s) {
      if (s.getInetAddress().isLoopbackAddress()) return 0x7f000001;  //loopback may return IP6 address
      byte o[] = s.getInetAddress().getAddress();
      return ba2int(o);
    }
    private void proxy(String req) throws Exception {
      String ln[] = req.split("\r\n");
      log("Proxy:" + ln[0]);
      int hostidx = -1;
      if (ln[0].endsWith("1.0")) disconn = true;  //HTTP/1.0
      for(int a=0;a<ln.length;a++) {
        if (ln[a].regionMatches(true, 0, "Host: ", 0, 6)) hostidx = a;
      }
      if (hostidx == -1) {
        log("ERROR : No host specified : " + req);
        replyError(505, "No host specified");
        return;
      }
      String hostln = ln[hostidx].substring(6);
      String host;
      int port = 80;
      int portidx = hostln.indexOf(':');
      try {
        if (portidx != -1) {
          host = hostln.substring(0, portidx);
          port = Integer.valueOf(hostln.substring(portidx+1));
        } else {
          host = hostln;
        }
        //check if host is blocked
        host = host.trim().toLowerCase();
        for(int a=0;a<blockedDomain.size();a++) {
          if (host.matches(blockedDomain.get(a))) {
            replyError(505, "Access Denied");
            return;
          }
        }
        if (ln[0].regionMatches(true, 0, "CONNECT ", 0, 8)) {
          connectCommand(host, ln[0]);
          return;
        }
        String method = null, proto = null, url = null, http = null;
        if (ln[0].regionMatches(true, 0, "POST ", 0, 5)) {
          method = "POST";
          String fn = ln[0].substring(5);
          int idx = fn.indexOf(" ");
          url = fn.substring(0, idx);
          http = fn.substring(idx+1);
        }
        else if (ln[0].regionMatches(true, 0, "GET ", 0, 4)) {
          method = "GET";
          String fn = ln[0].substring(4);
          int idx = fn.indexOf(" ");
          url = fn.substring(0, idx);
          http = fn.substring(idx+1);
        }
        else {
          replyError(505, "Unknown request");
          return;
        }
        if (url.startsWith("http://")) {
          proto = "http://";
          url = url.substring(7);
        } else {
          proto = "";
        }
        //check if url is changed
        for(int a=0;a<urlChanges.size();a++) {
          URLChange uc = urlChanges.get(a);
          if (url.matches(uc.url)) {
            url = uc.newurl;
            ln[0] = method + " " + proto + url + " " + http;
            int iport = url.indexOf(":");
            int iurl = url.indexOf("/");
            if (iurl == -1) iurl = url.length();
            if (iport == -1) {
              port = 80;
              host = url.substring(0, iurl);
              ln[hostidx] = "Host: " + host;
            } else {
              port = JF.atoi(url.substring(iport+1, iurl));
              host = url.substring(0, iport);
              ln[hostidx] = "Host: " + host + ":" + port;
            }
            break;
          }
        }
        connect(host, port);
        sendRequest(ln);
        if (method.equals("POST")) sendPost(ln);
        relayReply(proto + url);
        return;
      } catch (UnknownHostException uhe) {
        replyError(404, "Domain not found");
        log(uhe);
      } catch (IOException ioe) {
        /*do nothing*/
        log(ioe);
      } catch (Exception e) {
        replyError(505, "Exception:" + e);
        log(e);
      }
    }
    private void connect(String host, int port) throws UnknownHostException, IOException {
      log("connect:" + host + ":" + port);
      i = new Socket(host, port);
      iis = i.getInputStream();
      ios = i.getOutputStream();
    }
    private void replyError(int code, String msg) throws Exception {
      log("Error:" + code);
      String content = "<h1>Error : " + code + " : " + msg + "</h1>";
      String headers = "HTTP/1.1 " + code + " " + msg + "\r\nContent-Length: " + content.length() + "\r\n\r\n";
      pos.write(headers.getBytes());
      pos.write(content.getBytes());
      pos.flush();
    }
    private void sendRequest(String ln[]) throws Exception {
      String req = "";
      for(int a=0;a<ln.length;a++) {
        if (a == 0) ln[a] = removeHost(ln[a]);
        req += ln[a];
        req += "\r\n";
      }
      req += "\r\n";
      ios.write(req.getBytes());
      ios.flush();
    }
    private void sendPost(String ln[]) throws Exception {
      int length = -1;
      for(int a=0;a<ln.length;a++) {
        if (ln[a].regionMatches(true, 0, "Content-Length: ", 0, 16)) {
          length = Integer.valueOf(ln[a].substring(16, ln[a].length()));
        }
      }
      if (length == -1) throw new Exception("unknown post size");
      log("sendPost data len=" + length);
      byte post[] = JF.readAll(pis, length);
      ios.write(post);
      ios.flush();
    }
    private void relayReply(String fn) throws Exception {
      log("relayReply:" + fn);
      String tmp[];
      String line = "";
      String headers = "";
      int length = -1;
      int contentLength = -1;
      int ch;
      boolean first = true;
      int code;
      String encoding = "";
      do {
        ch = iis.read();
        if (ch == -1) throw new Exception("read error");
        line += (char)ch;
        if (!line.endsWith("\r\n")) continue;
        if (line.regionMatches(true, 0, "Content-Length: ", 0, 16)) {
          length = Integer.valueOf(line.substring(16, line.length() - 2));
          contentLength = length;
        }
        if (line.regionMatches(true, 0, "Connection: Close", 0, 17)) {
          disconn = true;
        }
        if (line.regionMatches(true, 0, "Transfer-Encoding:", 0, 18)) {
          encoding = line.substring(18).trim().toLowerCase();
        }
        if (first == true) {
          //HTTP/1.1 CODE MSG
          if (line.startsWith("HTTP/1.0")) disconn = true;
          tmp = line.split(" ");
          code = Integer.valueOf(tmp[1]);
          log("reply=" + code + ":" + line);
          first = false;
        }
        headers += line;
        if (line.length() == 2) break;  //blank line (double enter)
        line = "";
      } while (true);
      pos.write(headers.getBytes());
      pos.flush();
      if (length == 0) {
        log("reply:done:content.length=0:headers.length=" + headers.length());
        return;
      }
      if (length == -1) {
        if (encoding.equals("chunked")) {
          //read chunked format
          contentLength = 0;
          while (true) {
            //read chunk size followed by \r\n
            String chunkSize = "";
            while (true) {
              ch = iis.read();
              if (ch == -1) throw new Exception("read error");
              chunkSize += (char)ch;
              if (chunkSize.endsWith("\r\n")) break;
            }
            contentLength += chunkSize.length();
            int idx = chunkSize.indexOf(";");  //ignore extensions
            if (idx == -1) idx = chunkSize.length() - 2;
            int chunkLength = Integer.valueOf(chunkSize.substring(0, idx), 16);
            pos.write(chunkSize.getBytes());
            boolean zero = chunkLength == 0;
            //read chunk
            chunkLength += 2;  // \r\n
            contentLength += chunkLength;
            int read , off = 0;
            byte buf[] = new byte[chunkLength];
            while (chunkLength != 0) {
              read = iis.read(buf, off, chunkLength);
              if (read == -1) throw new Exception("read error");
              if (read > 0) {
                chunkLength -= read;
                off += read;
              }
            }
            pos.write(buf);
            pos.flush();
            if (zero) break;
          }
        } else {
          contentLength = 0;
          //read until disconnected (HTTP/1.0)
          int read;
          byte buf[] = new byte[64 * 1024];
          while (true) {
            read = iis.read(buf, 0, 64 * 1024);
            if (read == -1) break;
            if (read > 0) {
              contentLength += read;
              pos.write(buf, 0, read);
              pos.flush();
            }
          }
        }
      } else {
        //read content (length known)
        int read, off = 0;
        byte buf[] = new byte[length];
        while (length != 0) {
          read = iis.read(buf, off, length);
          if (read == -1) break;
          if (read > 0) {
            length -= read;
            off += read;
          }
        }
        pos.write(buf);
        pos.flush();
      }
      log("reply:done:content.length=" + contentLength + ":headers.length=" + headers.length());
    }
    private void connectCommand(String host, String req) throws Exception {
      String ln[] = req.split(" ");
      if (ln.length != 3) {
        replyError(505, "Bad CONNECT syntax");
        return;
      }
      int portidx = ln[1].indexOf(':');
      if (portidx != -1) {
        int port = Integer.valueOf(ln[1].substring(portidx+1));
        if (port != 443) {
          replyError(505, "CONNECT is for port 443 only");
          return;
        }
      }
      connect(host, 443);
      pos.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      pos.flush();
      ConnectRelay i2p = new ConnectRelay(iis, pos);
      ConnectRelay p2i = new ConnectRelay(pis, ios);
      i2p.start();
      p2i.start();
      i2p.join();
      p2i.join();
      disconn = true;  //not HTTP/1.1 compatible?
    }
    private String removeHost(String req) throws Exception {
      //older webserver don't like the host in the request line
      //in fact I didn't even know that some would accept it
      String p[] = req.split(" ");
      if (p.length != 3) return req;
      URL url = new URL(p[1]);
      return p[0] + " " + url.getFile() + " " + p[2];
    }
    private class ConnectRelay extends Thread {
      private InputStream is;
      private OutputStream os;
      private byte buf[] = new byte[4096];
      private final int buflen = 4096;
      public ConnectRelay(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
      }
      public void run() {
        int read;
        try {
          while (true) {
            read = is.read(buf, 0, buflen);
            if (read == -1) break;
            if (read > 0) {os.write(buf, 0, read); os.flush();}
          }
        } catch (Exception e) {}
      }
    }
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public class JBusMethods {
    public void getConfig(String pack) {
      busClient.call(pack, "getConfig", busClient.quote(busClient.encodeString(config)));
    }
    public void setConfig(String cfg) {
      //write new file
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      proxy.close();
      proxy = new Proxy();
      proxy.start();
    }
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33003;
    } else {
      return 777;
    }
  }

  public static void main(String args[]) {
    serviceStart(args);
  }

  //Win32 Service

  private static Proxy proxy;

  public static void serviceStart(String args[]) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    proxy = new Proxy();
    proxy.start();
  }

  public static void serviceStop() {
    JFLog.log("Stopping service");
    busServer.close();
    proxy.close();
  }
}

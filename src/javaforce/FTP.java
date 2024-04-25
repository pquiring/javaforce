package javaforce;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

/**
 * FTP client class. Supports Passive and Active mode.
 */
public class FTP {

  public static interface ProgressListener {
    public void setProgress(int value);
  }

  private Socket s;
  private Socket ds;
  private InputStream is;
  private OutputStream os;
  private BufferedReader br;
  private boolean passive = true;
  private String host;  //passive host
  private int pasvport; //passive port
  private ServerSocket ss;  //active socket
  private boolean log = false;
  private ProgressListener progress;
  private boolean aborted = false;
  private boolean active = true;
  private Reader reader;

  public boolean debug = false;
  /**
   * Holds the repsonse strings from the last executed command
   */
  public ArrayList<String> response = new ArrayList<String>();

  public int getResponseLength() {
    synchronized(response) {
      return response.size();
    }
  }

  public void addResponse(String r) {
    synchronized(response) {
      response.add(r);
    }
  }

  public String getNextResponse() {
    synchronized(response) {
      if (response.size() == 0) return null;
      return response.remove(0);
    }
  }

  public String getLastResponse() {
    while (!aborted) {
      synchronized(response) {
        if (response.size() == 0) return null;
        String res = response.remove(0);
        if (res.charAt(3) == ' ') {
          return res;
        }
      }
    }
    return null;
  }

  public boolean connect(String host, int port) throws Exception {
    active = true;
    s = new Socket(host, port);
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    reader = new Reader();
    reader.start();
    wait4Response();
    if (getLastResponse().startsWith("220")) {
      return true;
    }
    disconnect();  //not valid FTP site
    return false;
  }

  public boolean connectSSL(String host, int port) throws Exception {
    active = true;
    TrustManager[] trustAllCerts = new TrustManager[]{
      new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
//      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();  //this method will only work with trusted certs
    SSLSocketFactory sslsocketfactory = (SSLSocketFactory) sc.getSocketFactory();  //this method will work with untrusted certs
    SSLSocket ssl = (SSLSocket) sslsocketfactory.createSocket(host, port);
    s = (Socket) ssl;
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    reader = new Reader();
    reader.start();
    wait4Response();
    if (getLastResponse().startsWith("220")) {
      return true;
    }
    disconnect();  //not valid FTP site
    return false;
  }

  public void abort() {
    aborted = true;
  }

  public void disconnect() throws Exception {
    active = false;
    if (s != null) {
      s.close();
    }
    s = null;
    is = null;
    os = null;
    reader = null;
  }

  public void addProgressListener(ProgressListener progress) {
    this.progress = progress;
  }

  public void setLogging(boolean state) {
    log = state;
  }

  public void setPassiveMode(boolean mode) {
    passive = mode;
  }

  public boolean setBinary() throws Exception {
    cmd("type i");
    wait4Response();
    if (!getLastResponse().startsWith("200")) {
      return false;
    }
    return true;
  }

  public boolean setAscii() throws Exception {
    cmd("type a");
    wait4Response();
    if (!getLastResponse().startsWith("200")) {
      return false;
    }
    return true;
  }

  public boolean login(String user, String pass) throws Exception {
    cmd("user " + user);
    wait4Response();
    if (getLastResponse().startsWith("230")) {
      //no password required
      return true;
    }
    if (!getLastResponse().startsWith("331")) {
      return false;
    }
    cmd("pass " + pass);
    wait4Response();
    if (!getLastResponse().startsWith("230")) {
      return false;
    }
    return true;
  }

  public void logout() throws Exception {
    cmd("quit");
    wait4Response();  //should be "221" but ignored
    getLastResponse();
    active = false;
  }

  public void cmd(String cmd) throws Exception {
    if ((s == null) || (s.isClosed())) {
      throw new Exception("not connected");
    }
    aborted = false;
    if (log) {
      if (cmd.startsWith("pass ")) {
        JFLog.log("pass ****");
      } else {
        JFLog.log(cmd);
      }
    }
    synchronized(response) {
      response.clear();
    }
    cmd += "\r\n";
    os.write(cmd.getBytes());
  }

  public void get(String filename, String out) throws Exception {
    getPort();
    cmd("retr " + filename);
    FileOutputStream fos = new FileOutputStream(out);
    getData(fos);
    fos.close();
    wait4Response();
    getLastResponse();
  }

  public void get(String filename, OutputStream os) throws Exception {
    getPort();
    cmd("retr " + filename);
    getData(os);
    wait4Response();
    getLastResponse();
  }

  public InputStream getStart(String filename) throws Exception {
    getPort();
    cmd("retr " + filename);
    return getData();
  }

  public void get(File remote, File local) throws Exception {
    get(remote.getAbsolutePath(), local.getAbsolutePath());
  }

  public void getFinish() throws Exception {
    wait4Response();
    getLastResponse();
  }

  public void put(InputStream is, String filename) throws Exception {
    getPort();
    cmd("stor " + filename);
    putData(is);
    wait4Response();
    if (!getLastResponse().startsWith("226")) {
      throw new Exception("bad listing");
    }
  }

  public void put(String in, String filename) throws Exception {
    getPort();
    cmd("stor " + filename);
    FileInputStream fis = new FileInputStream(in);
    putData(fis);
    fis.close();
    wait4Response();
    if (!getLastResponse().startsWith("226")) {
      throw new Exception("bad listing");
    }
  }

  public void put(File local, File remote) throws Exception {
    put(local.getAbsolutePath(), remote.getName());
  }


  public OutputStream putStart(String filename) throws Exception {
    getPort();
    cmd("stor " + filename);
    return putData();
  }

  public void putFinish() throws Exception {
    wait4Response();
    getLastResponse();
  }

  public void cd(String path) throws Exception {
    cmd("cwd " + path);
    wait4Response();
    getLastResponse();
  }

  public void chmod(int mode, String path) throws Exception {
    cmd("site chmod " + Integer.toString(mode, 8) + " " + path);
    wait4Response();
    getLastResponse();
  }

  public void mkdir(String path) throws Exception {
    cmd("mkd " + path);
    wait4Response();
    getLastResponse();
  }

  public void rename(String oldpath, String newpath) throws Exception {
    cmd("rnfr " + oldpath);
    wait4Response();
    getLastResponse();
    cmd("rnto " + newpath);
    wait4Response();
    getLastResponse();
  }

  public void rm(String path) throws Exception {
    cmd("dele " + path);
    wait4Response();
    getLastResponse();
  }

  public void rmdir(String path) throws Exception {
    cmd("rmd " + path);
    wait4Response();
    getLastResponse();
  }

  public String ls(String path) throws Exception {
    getPort();
    cmd("list " + path);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    getData(baos);
    wait4Response();
    if (!getLastResponse().startsWith("226")) {
      throw new Exception("bad listing");
    }
    return baos.toString();
  }

  public String pwd() throws Exception {
    cmd("pwd");
    wait4Response();
    String str = getLastResponse();
    if (!str.startsWith("257")) {
      throw new Exception("pwd failed");
    }
    if ((str.charAt(4) == '\"') && (str.charAt(str.length() - 1) == '\"')) {
      return str.substring(5, str.length() - 1);
    } else {
      return str.substring(4);
    }
  }
  private final int BUFSIZ = 64 * 1024;

  private void getPort() throws Exception {
    if (passive) {
      cmd("pasv");
      wait4Response();
      String str = getLastResponse();
      if (!str.startsWith("227")) {
        throw new Exception("pasv failed");
      }
      //227 Entering Passive Mode (ip,ip,ip,ip,PORTHI,PORTLO).
      String[] strs = str.split(",");
      if (strs.length != 6) {
        throw new Exception("pasv failed - bad response");
      }
      int hi = Integer.valueOf(strs[4]);
      int lo = Integer.valueOf(strs[5].substring(0, strs[5].indexOf(")")));
      pasvport = (hi << 8) + lo;
    } else {
      ss = new ServerSocket();
      int hi = ss.getLocalPort() >> 8;
      int lo = ss.getLocalPort() & 0xff;
      cmd("port " + s.getLocalAddress().getHostAddress().replaceAll("[.]", ",") + "," + hi + "," + lo);
      wait4Response();
      String str = getLastResponse();
      if (!str.startsWith("200")) {
        throw new Exception("port failed");
      }
    }
    if (passive) {
      ds = new Socket(host, pasvport);
    } else {
      ds = ss.accept();
    }
  }

  private InputStream getData() throws Exception {
    aborted = false;
    InputStream dis = ds.getInputStream();
    wait4Response();
    if (!getLastResponse().startsWith("150")) {
      throw new Exception("bad get");
    }
    return dis;
  }

  private void getData(OutputStream os) throws Exception {
    aborted = false;
    byte[] data = new byte[BUFSIZ];
    InputStream dis = ds.getInputStream();
    wait4Response();
    if (!getLastResponse().startsWith("150")) {
      throw new Exception("bad get");
    }
    int read, total = 0;
    startTimeoutThread();
    while (!ds.isClosed() && !aborted) {
      timeoutCounter = 0;
      read = dis.read(data);
      if (read == -1) {
        break;
      }
      if (read > 0) {
        os.write(data, 0, read);
        total += read;
        if (progress != null) {
          progress.setProgress(total);
        }
      }
    }
    //read any remaining data left in buffers
    if (aborted) {
      return;
    }
    do {
      timeoutCounter = 0;
      read = dis.read(data);
      if (read > 0) {
        os.write(data, 0, read);
        total += read;
        if (progress != null) {
          progress.setProgress(total);
        }
      }
    } while ((read > 0) && (!aborted));
    stopTimeoutThread();
  }

  private OutputStream putData() throws Exception {
    aborted = false;
    OutputStream dos = ds.getOutputStream();
    wait4Response();
    if (!getLastResponse().startsWith("150")) {
      throw new Exception("bad put");
    }
    return dos;
  }

  private void putData(InputStream is) throws Exception {
    aborted = false;
    byte[] data = new byte[BUFSIZ];
    OutputStream dos = ds.getOutputStream();
    wait4Response();
    if (!getLastResponse().startsWith("150")) {
      throw new Exception("bad put");
    }
    int total = 0;
    startTimeoutThread();
    while ((!ds.isClosed()) && (is.available() > 0) && (!aborted)) {
      timeoutCounter = 0;
      int read = is.read(data);
      if (read > 0) {
        dos.write(data, 0, read);
        total += read;
        if (progress != null) {
          progress.setProgress(total);
        }
      }
    }
    stopTimeoutThread();
    dos.flush();
    ds.close();
    ds = null;
  }

  private void wait4Response() throws Exception {
    while (!aborted) {
      synchronized(response) {
        int size = response.size();
        if (size > 0) {
          String last = response.get(size-1);
          if (last.charAt(3) == ' ') return;
        }
        response.wait();
      }
    }
  }

  private class Reader extends Thread {
    public void run() {
      while (active) {
        try {
          String res = br.readLine();
          if (res == null) throw new Exception("FTP:invalid reply:null");
          if (res.length() < 3) throw new Exception("FTP:invalid reply:" + res);
          JFLog.log(res);
          addResponse(res);
          if (res.charAt(3) == ' ') {
            synchronized(response) {
              response.notify();
            }
          }
          if (res.startsWith("5")) {
            JFLog.log("Aborted");
            aborted = true;
            if (ds != null) {
              ds.close();
              ds = null;
            }
          }
        } catch (SocketException se) {
          break;
        } catch (Exception e) {
          e.printStackTrace();
          break;
        }
      }
    }
  }

  private Timer timer;
  private int timeoutCounter;

  private void startTimeoutThread() {
    timeoutCounter = 0;
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      public void run() {
        timeoutCounter++;
        if (timeoutCounter == 20) {
          JFLog.log("Error:Transfer Timeout");
          aborted = true;
          if (ds != null) {
            try {ds.close();} catch (Exception e) {}
            ds = null;
          }
          timer.cancel();
          timer = null;
        }
      }
    }, 1000, 1000);
  }

  private void stopTimeoutThread() {
    timer.cancel();
    timer = null;
  }
}

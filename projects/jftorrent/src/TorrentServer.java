/**
 * Created : May 26, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;

/** Listens on a port and send peers to registered TorrentClients */
public class TorrentServer extends Thread {
  private static Vector<TorrentClient> clients = new Vector<TorrentClient>();
  private String status = "?";
  public static int port = 6881;
  private ServerSocket ss;

  public static void register(TorrentClient client) {
    clients.add(client);
  }

  public static void unregister(TorrentClient client) {
    clients.remove(client);
  }

  public TorrentServer(int port) {
    TorrentServer.port = port;
  }

  public void run() {
    while (true) {
      try {
        ss = new ServerSocket(port);
        while (true) {
          Socket s = ss.accept();
          new PeerListener(s).start();
        }
      } catch (Exception e) {
        JF.sleep(1000);
      }
    }
  }

  public String getStatus() {
    return status;
  }

  public void changePort(int port) {
    TorrentServer.port = port;
    status = "?";
    try {ss.close();} catch (Exception e) {}
  }

  private class PeerListener extends Thread {
    private Socket s;
    private InputStream is;
    private OutputStream os;
    private String ip, id;

    public PeerListener(Socket s) {
      this.s = s;
    }

    public void run() {
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        ip = s.getInetAddress().toString().substring(1);
      } catch (Exception e) {
        close();
        return;
      }
      if (!getHandshake()) {
        close();
        return;
      }
      if (status.equals("?")) {
        status = "Ok";
        MainPanel.This.setStatus(status);
      }
    }

    private void close() {
      try { s.close(); } catch (Exception e) {}
    }

    private boolean getHandshake() {
      byte buf[] = new byte[1024];
      byte handshake[] = new byte[68];
      byte info_hash[] = new byte[20];
      int toRead = 68;
      int pos = 0;
      JFLog.log("Waiting for handshake:" + ip);
      try {
        while (toRead > 0) {
          int read = is.read(buf, 0, toRead);
          if (read <= 0) throw new Exception("read error:" + ip);
          System.arraycopy(buf, 0, handshake, pos, read);
          pos += read;
          toRead -= read;
        }
        if (handshake[0] != 19) throw new Exception("bad handshake (protocol.len!=19):" + ip);
        if (!new String(handshake, 1, 19).equals("BitTorrent protocol")) throw new Exception("bad handshake (unknown protocol):" + ip);
        id = new String(handshake, 48, 20);
        System.arraycopy(handshake, 28, info_hash, 0, 20);
        JFLog.log("info_hash=" + escape(info_hash));
        for(int a=0;a<clients.size();a++) {
          TorrentClient client = clients.get(a);
          if (Arrays.equals(info_hash, client.getHash())) {
            client.addPeer(s, id);
            return true;
          }
        }
        JFLog.log("unknown info_hash:drop peer");
        return false;
      } catch (Exception e) {
//        JFLog.log("getHandshake Exception:" + ip);
        JFLog.log(e);
      }
      return false;
    }
  }
  private static String escape(byte[] hash) {
    StringBuilder str = new StringBuilder();
    for(int a=0;a<hash.length;a++) {
      if ((hash[a] >= '0') && (hash[a] <= '9')) {
        str.append((char)hash[a]);
        continue;
      }
      if ((hash[a] >= 'a') && (hash[a] <= 'z')) {
        str.append((char)hash[a]);
        continue;
      }
      if ((hash[a] >= 'A') && (hash[a] <= 'Z')) {
        str.append((char)hash[a]);
        continue;
      }
      switch (hash[a]) {
        case '.':
        case '-':
        case '_':
        case '~':
          str.append((char)hash[a]);
          continue;
      }
      str.append("%" + String.format("%02x", (((int)hash[a]) & 0xff)));
    }
    return str.toString();
  }
}

/**
 * Created : May 27, 2012
 *
 * @author pquiring
 */

import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javaforce.*;

/** The real "server" part of torrents that keeps "track" of clients. */
public class TorrentTracker extends Thread {
  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage:tracker [--port=PORT] torrents...");
      System.out.println(" Default port = 6969");
      System.exit(1);
    }
    JFLog.init(".jftorrentTracker.log", true);
    for(int a=0;a<args.length;a++) {
      if (args[a].startsWith("--port=")) {
        port = JF.atoi(args[a].substring(7));
      } else {
        addTorrent(args[a]);
      }
    }
    new TorrentTracker().start();
  }

  private ServerSocket ss;
  private static int port = 6969;
  private boolean active = true;
  private static ArrayList<Torrent> torrents = new ArrayList<Torrent>();

  private static class Torrent {
    byte[] info_hash;
    ArrayList<byte[]> peers = new ArrayList<byte[]>();
    Object peersLock = new Object();
    int complete;
    int incomplete;
  }

  public static void addTorrent(String file) {
    Torrent torrent = new Torrent();
    //read file and get info_hash
    TorrentFile metaFile = new TorrentFile();
    try {
      FileInputStream fis = new FileInputStream(file);
      byte metaData[] = JF.readAll(fis);
      fis.close();
      metaFile.read(metaData);
      MetaDict info = metaFile.getDict(new String[] {"d", "s:info"}, null);
      torrent.info_hash = SHA1sum(Arrays.copyOfRange(metaData, info.pos1, info.pos2));
      torrents.add(torrent);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static byte[] SHA1sum(byte[] data) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    return md.digest(data);
  }

  public void run() {
    try {
      ss = new ServerSocket(port);
      JFLog.log("TorrentTracker Listening on port " + port);
      while (active) {
        Socket s = ss.accept();
        Client client = new Client(s);
        client.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  private byte[] decode_info_hash(String hashStr) {
    byte hash[] = new byte[20];
    int pos = 0;
    char hashChars[] = hashStr.toCharArray();
    for(int a=0;a<20;a++) {
      if (hashChars[pos] == '%') {
        pos++;
        String hexStr = "" + hashChars[pos++] + hashChars[pos++];
        hash[a] = (byte)Integer.valueOf(hexStr, 16).intValue();
      } else {
        hash[a] = (byte)hashChars[pos++];
      }
    }
    return hash;
  }
  private class Client extends Thread {
    private Socket s;
    private InputStream is;
    private OutputStream os;
    public Client(Socket s) {
      this.s = s;
    }
    public void run() {
      process();
      try { s.close(); } catch (Exception e) {}
    }
    public void process() {
      try {
        is = s.getInputStream();
        os = s.getOutputStream();
        //read until we get a double CR+LF
        byte[] req = new byte[1024];
        int pos = 0;
        int read;
        while (true) {
          read = is.read(req, pos, 1024-pos);
          if (read == 0) continue;
          if (read == -1) return;
          pos += read;
          if (pos < 4) continue;
          if (new String(req, pos-4, 4).equals("\r\n\r\n")) break;
        }
        String lns[] = new String(req, 0, pos).split("\r\n");
        if (!lns[0].startsWith("GET ")) throw new Exception("bad request - no GET");
        //GET /?args
        int idxArgsStart = lns[0].indexOf("?");
        int idxArgsEnd = lns[0].indexOf(" ", idxArgsStart);
        String args[] = lns[0].substring(idxArgsStart+1, idxArgsEnd).split("&");
        String info_hashStr = null, peer_id = null, portStr = null, compact = null, numwantStr = null, event = null;
        String uploaded = null, downloaded = null, left = null;
        for(int a=0;a<args.length;a++) {
          int idx = args[a].indexOf("=");
          if (idx == -1) continue;
          if (args[a].startsWith("info_hash=")) info_hashStr = args[a].substring(idx+1);
          if (args[a].startsWith("peer_id=")) peer_id = args[a].substring(idx+1);
          if (args[a].startsWith("port=")) portStr = args[a].substring(idx+1);
          if (args[a].startsWith("compact=")) compact = args[a].substring(idx+1);
          if (args[a].startsWith("numwant=")) numwantStr = args[a].substring(idx+1);
          if (args[a].startsWith("event=")) event = args[a].substring(idx+1);
          if (args[a].startsWith("uploaded=")) uploaded = args[a].substring(idx+1);
          if (args[a].startsWith("downloaded=")) downloaded = args[a].substring(idx+1);
          if (args[a].startsWith("left=")) left = args[a].substring(idx+1);
        }
        byte peer[] = new byte[6];
        InetAddress ia = s.getInetAddress();
        byte ip[] = ia.getAddress();
        System.arraycopy(ip, 0, peer, 0, 4);
        int peerport = JF.atoi(portStr);
        peer[4] = (byte)((peerport >> 8) & 0xff);
        peer[5] = (byte)(peerport & 0xff);
        if (event == null) event = "";
        byte info_hash[] = decode_info_hash(info_hashStr);
        if (numwantStr == null) numwantStr = "50";
        int numwant = JF.atoi(numwantStr);
        if (numwant < 50) numwant = 50;
        if (numwant > 100) numwant = 100;
        if (left == null) left = "1";
        for(int a=0;a<torrents.size();a++) {
          Torrent t = torrents.get(a);
          if (Arrays.equals(t.info_hash, info_hash)) {
            ByteArrayOutputStream bb = new ByteArrayOutputStream();
            if (!compact.equals("1")) {
              JFLog.log("bad request");
              bb.write("d14:failure reason12:need compacte".getBytes());
              os.write(("HTTP/1.0 500 Bad Request\r\nContent-Length: " + bb.toByteArray().length + "\r\n\r\n").getBytes());
              os.write(bb.toByteArray());
              os.flush();
              return;
            }
            JFLog.log("event=" + event);
            if (event.equals("completed")) {
              t.complete++;
              t.incomplete--;
            }
            if (event.equals("stopped")) {
              synchronized(t.peersLock) {
                int size = t.peers.size();
                for(int b=0;b<size;b++) {
                  if (Arrays.equals(peer, t.peers.get(b))) {
                    t.peers.remove(b);
                    JFLog.log("remove peer:" + ia.getHostAddress());
                    break;
                  }
                }
              }
              numwant = 0;
            }
            if (event.equals("started")) {
              if (left.equals("0")) t.complete++; else t.incomplete++;
              synchronized(t.peersLock) {
                t.peers.add(peer);
                JFLog.log("new peer:" + ia.getHostAddress());
              }
            }
            bb.write("d".getBytes());
            bb.write(("8:completei" + t.complete + "e").getBytes());
            bb.write(("10:incompletei" + t.incomplete + "e").getBytes());
            bb.write("8:intervali1800e".getBytes());
            byte peers[];
            synchronized(t.peersLock) {
              int numpeers = t.peers.size();
              if (numwant >= numpeers) numwant = numpeers-1;
              if (numwant == -1) numwant = 0;
              peers = new byte[numwant * 6];
              boolean used[] = new boolean[numpeers];
              Random r = new Random();
              for(int b=0;b<numwant;b++) {
                int idx = Math.abs(r.nextInt(numpeers));
                while (used[idx] || Arrays.equals(t.peers.get(idx), peer)) {
                  idx++;
                  if (idx == numpeers) idx = 0;
                }
                used[idx] = true;
                System.arraycopy(t.peers.get(idx), 0, peers, b*6, 6);
              }
            }
            bb.write("5:peers".getBytes());
            if (numwant == 0) {
              bb.write("0:".getBytes());
            } else {
              bb.write(((numwant*6) + ":").getBytes());
              bb.write(peers);
            }
            bb.write("e".getBytes());  //end main dict
            os.write(("HTTP/1.0 200 OK\r\nContent-Length: " + bb.toByteArray().length + "\r\n\r\n").getBytes());
            os.write(bb.toByteArray());
            os.flush();
            return;
          }
        }
        ByteArrayOutputStream bb = new ByteArrayOutputStream();
        bb.write("d14:failure reason16:torrent not heree".getBytes());
        os.write(("HTTP/1.0 404 Not Found\r\nContent-Length: " + bb.toByteArray().length + "\r\n\r\n").getBytes());
        os.write(bb.toByteArray());
        os.flush();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
}

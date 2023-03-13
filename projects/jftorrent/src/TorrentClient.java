/**
 * Created : May 23, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

import javaforce.*;

public class TorrentClient extends Thread {
  public static final String clientVersion = "0002";  //must be 4 digits

  private static final int FRAGSIZE = 16 * 1024;  //16k
  private static final int MAXPEERS = 30;  //max # active peers
  private static final int NUMWANT = 80;  //# peers requested from tracker (advisory)

  public static boolean debug = true;  //lots of debugging info

  public String torrent, dest;
  public boolean done = false;
  public boolean active = true;
  public boolean paused = false;
  public int downSpeed = 0;
  public int upSpeed = 0;
  public long downAmount = 1;  //must not be zero - causes / by zero
  public long upAmount = 0;
  public long downAmountCnt = 0;
  public long upAmountCnt = 0;
  public int seeders;
  public float available = 0.0f;
  //metainfo data
  private byte metaData[];
  private TorrentFile metaFile;
  public String status;
  private boolean have[];
  private int haveCnt = 0;
  private String announce;
  private ArrayList<String> announceList = new ArrayList<String>();
  private byte info_hash[];
  public long totalLength = 1;  //must not be zero - causes / by zero
  private long pieceLength;
  private long lastPieceLength;  //need to calc
  public String name;
  private int noFiles;
  private ArrayList<MetaFile> files = new ArrayList<MetaFile>();
  private byte pieces[][];  //SHA1
  private int numFragsPerPiece;
  private final int FRAGSTACK = 3;

  //client info
  private String peer_id;

  //tracker data
  private int complete;
  private int incomplete;
  private int interval, intervalCounter = 0;
  private int pruneCounter = 120;
  private byte[] peers;
  private ArrayList<Peer> peerList = new ArrayList<Peer>();
  private int peerIdx = 0;
  private int peerActiveCount = 0;
  private final Object peerListLock = new Object();
  private Timer timer = new Timer();
  private boolean registered = false;
  private long now;

  private final int CHOKE = 0;
  private final int UNCHOKE = 1;
  private final int INTERESTED = 2;
  private final int NOTINTERESTED = 3;
  private final int HAVE = 4;
  private final int BITFIELD = 5;
  private final int REQUEST = 6;  //request fragment
  private final int FRAGMENT = 7;  //deliver fragment - piece of a piece (I call them fragments)
  private final int CANCEL = 8;
  private final int DHT_PORT = 9;

  /** Peer for this torrent */
  private static class Peer {
    public boolean inuse = false;
    public boolean seeder = false;
    public int available = 0;
    public String ip, id;
    public int port;
    public Socket s;
    public InputStream is;
    public OutputStream os;
    public final Object lock = new Object();
    public boolean am_choking = true, am_interested = false;
    public boolean peer_choking = true, peer_interested = false;
    public boolean haveHandshake = false;
    public PeerListener listener;
    public PeerDownloader downloader;
    public long lastMsg;
    public boolean have[];
    public byte piece[];
    public boolean haveFrags[];
    public int gotFragCnt, numFrags;
    public int downloadingPieceIdx = -1;
    public final Object chokeLock = new Object();
    public final Object fragLock = new Object();
    public int lastFragLength;
    public int pendingFrags;
  }

  public static void main(String args[]) {
    if (args.length != 2) {
      System.out.println("Usage : TorrentClient torrent destination");
      System.exit(1);
    }
    JFLog.init(JF.getUserPath() + "/.jftorrent.log", true);
    JF.initHttps();
    TorrentClient client = new TorrentClient(args[0], args[1], true, false);
    client.start();
  }

  public byte[] getHash() {
    return info_hash;
  }

  public int getNumPeers() {
    return peerList.size();
  }

  public TorrentClient(String torrent, String dest, boolean active, boolean paused) {
    this.active = active;
    this.paused = paused;
    this.torrent = torrent;
    this.dest = dest;
  }
  public void run() {
    try {
      status = "Reading torrent file";
      //generate peer id (random)
      peer_id = "-JT" + clientVersion + "-";
      Random r = new Random();
      while (peer_id.length() != 20) {
        peer_id += (char)(r.nextBoolean() ? 'a' : 'A' + r.nextInt(26));
      }
      //read torrent
      if (debug) JFLog.log("Reading torrent:" + torrent);
      readMeta();
      if (debug) JFLog.log("Getting info...");
      MetaDict info = metaFile.getDict(new String[] {"d", "d:info"}, null);
      info_hash = SHA1sum(Arrays.copyOfRange(metaData, info.pos1, info.pos2));
      if (debug) JFLog.log("info_hash = " + escape(info_hash));
      announce = metaFile.getString(new String[] {"d", "s:announce"}, null);
      if (debug) JFLog.log("announce=" + announce);
      MetaList aList = metaFile.getList(new String[] {"d", "s:announce-list"}, null);
      if (aList != null) {
        for(int a=0;a<aList.list.size();a++) {
          MetaTag tag = aList.list.get(a);
          if (tag instanceof MetaData) {
            MetaData str = (MetaData)tag;
            announceList.add(str.toString());
          }
        }
      }
      totalLength = metaFile.getValue(new String[] {"d", "d:info", "i:length"}, null);
      if (debug) JFLog.log("length=" + totalLength);
      pieceLength = metaFile.getValue(new String[] {"d", "d:info", "i:piece length"}, null);
      if (debug) JFLog.log("piece_length=" + pieceLength);
      numFragsPerPiece = (int)(pieceLength / FRAGSIZE);
      if (numFragsPerPiece * FRAGSIZE != pieceLength) throw new Exception("bad piece_length (not multiple of FRAGSIZE)");
      name = metaFile.getString(new String[] {"d", "d:info", "s:name"}, null);
      if (debug) JFLog.log("name=" + name);
      MetaData piecesTag = (MetaData)metaFile.getDictEntry(new String[] {"d", "d:info", "s:pieces"}, null);
      byte[] piecesArray = piecesTag.data;
      int noPieces = piecesArray.length / 20;
      if (debug) JFLog.log("# pieces=" + noPieces);
      have = new boolean[noPieces];
      pieces = new byte[noPieces][];
      for(int a=0;a<noPieces;a++) {
        pieces[a] = new byte[20];
        System.arraycopy(piecesArray, a * 20, pieces[a], 0, 20);
      }
      MetaList filesList = metaFile.getList(new String[] {"d", "d:info", "l:files"}, null);
      if (filesList != null) {
        noFiles = filesList.list.size();
        if (debug) JFLog.log("filesList:" + noFiles);
        long filesLength = 0;
        for(int a=0;a<noFiles;a++) {
          MetaList fileDict = (MetaList)filesList.list.get(a);
          MetaList fileName = (MetaList)metaFile.getList(new String[]{"s:path"}, fileDict);
          MetaFile tfile = new MetaFile();
          tfile.name = concatList(fileName.list, true);
          tfile.length = (Long)metaFile.getValue(new String[] {"i:length"}, fileDict);
          filesLength += tfile.length;
          files.add(tfile);
          if (debug) JFLog.log("file[]=" + tfile.name + ":length=" + tfile.length);
        }
        if (totalLength == -1) {
          totalLength = filesLength;
        }
      } else {
        if (debug) JFLog.log("filesList=null");
        noFiles = 1;
        MetaFile tfile = new MetaFile();
        tfile.name = name;
        tfile.length = totalLength;
        files.add(tfile);
      }
      if (debug) JFLog.log("noFiles=" + noFiles);
      lastPieceLength = totalLength % pieceLength;
      if (lastPieceLength == 0) lastPieceLength = pieceLength;
      if (debug) JFLog.log("lastPieceLength=" + lastPieceLength);
      //what are we doing?
      checkFiles();
      if (debug) JFLog.log("haveCnt=" + haveCnt);
      //create a timer to get info from announce[-list]
      status = "Contacting tracker";
      timer.schedule(new TimerTask() {
        public void run() {
          now = System.currentTimeMillis();
          try {
            if (intervalCounter <= 0) {
              contactTracker(peerList.size() == 0 ? "started" : "");
              intervalCounter = interval;
            } else {
              intervalCounter--;
            }
            if (!active) return;
            if (paused) return;
            pruneCounter--;
            if (pruneCounter == 0) {
              pruneCounter = 120;
              prunePeers();
            }
            if (done) return;
            addPeer();
          } catch (Exception e) {
            if (debug) JFLog.log(e);
          }
        }
      }, 1000, 1000);
    } catch (Exception e) {
      status = "Error";
      if (debug) JFLog.log(e);
    }
  }
  private void readMeta() throws Exception {
    FileInputStream fis = new FileInputStream(torrent);
    metaData = JF.readAll(fis);
    fis.close();
    metaFile = new TorrentFile();
    metaFile.read(metaData);
  }
  private String concatList(ArrayList<MetaTag> list, boolean isFilePath) throws Exception {
    int size = list.size();
    StringBuilder str = new StringBuilder();
    for(int a=0;a<size;a++) {
      if ((a > 0) || (isFilePath)) str.append("/");
      MetaData s = (MetaData)list.get(a);
      str.append(s.toString());
    }
    return str.toString();
  }
  private void checkFiles() {
    byte piece[] = new byte[(int)pieceLength];
    long poff = 0;
    int pidx = 0;
    boolean bad = false;  //part of piece is missing
    try {
      for(int a=0;a<noFiles;a++) {
        MetaFile tfile = files.get(a);
        String filename = dest + "/" + tfile.name;
        File file = new File(filename);
        if (file.exists()) {
          tfile.file = new RandomAccessFile(filename, "rw");
          long torrentLength = tfile.length;  //file length according to torrent info
          long realLength = tfile.file.length();  //current file's real length
          int toRead = (int)(pieceLength - poff);  //length to read rest of current piece
          if (pidx == pieces.length-1) {
            toRead = (int)(lastPieceLength - poff);
          }
          if ((torrentLength < toRead) || (realLength < toRead)) {
            if (torrentLength == realLength) {
              JF.readAll(tfile.file, piece, (int)poff, (int)torrentLength);
              poff += torrentLength;
            } else {
              JFLog.log("Warning : piece bad : " + pidx + " : it will be redownloaded.");
              bad = true;
              poff += tfile.length;
              while (poff >= pieceLength) {
                bad = false;
                poff -= pieceLength;
                pidx++;
              }
            }
          } else {
            boolean part = false;
            while ((torrentLength > 0) && (realLength > 0)) {
              toRead = (int)(pieceLength - poff);
              if (pidx == pieces.length-1) {
                toRead = (int)(lastPieceLength - poff);
              }
              if (toRead > torrentLength) {toRead = (int)torrentLength; part = true;}
              if (toRead > realLength) {toRead = (int)realLength; part = true;}
              JF.readAll(tfile.file, piece, (int)poff, toRead);
              if (part) {
                poff += toRead;
                break;
              }
              if (bad) {
                bad = false;
              } else {
                int thisPieceLength = (int)pieceLength;
                if (pidx == pieces.length-1) {
                  thisPieceLength = (int)lastPieceLength;
                }
                byte hash[] = SHA1sum(Arrays.copyOfRange(piece,0,thisPieceLength));
                if (Arrays.equals(hash, pieces[pidx])) {
                  have[pidx] = true;
                  haveCnt++;
                  if (pidx == pieces.length-1) {
                    downAmount += lastPieceLength;
                  } else {
                    downAmount += pieceLength;
                  }
                }
              }
              poff = 0;
              pidx++;
              torrentLength -= toRead;
              realLength -= toRead;
            }
            if (torrentLength > 0) {
              poff += torrentLength;
              while (poff >= pieceLength) {
                poff -= pieceLength;
                pidx++;
              }
              bad = poff != 0;
            }
          }
        } else {
          poff += tfile.length;
          while (poff >= pieceLength) {
            poff -= pieceLength;
            pidx++;
          }
          bad = poff != 0;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    done = haveCnt == pieces.length;
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        MainPanel.This.updateList();
      }
    });
    if (done) status = "Seeding";
  }

  public static byte[] SHA1sum(byte[] data) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    return md.digest(data);
  }

  //URLEncoder only works with String's
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
  private String getip(byte data[], int pos) {
    return "" + ((int)data[pos++] & 0xff) + "." + ((int)data[pos++] & 0xff) + "."
      + ((int)data[pos++] & 0xff) + "." + ((int)data[pos++] & 0xff);
  }
  private int getport(byte data[], int pos) {
    int ret = ((int)data[pos]) & 0xff;
    ret <<= 8;
    ret += ((int)data[pos+1]) & 0xff;
    return ret;
  }
  private void contactTracker(String event) throws Exception {
    if (announce.startsWith("http://") || announce.startsWith("https://")) {
      URL url = new URL(announce + "?info_hash=" + escape(info_hash) + "&peer_id=" + peer_id + "&port=" + TorrentServer.port +
        "&uploaded=" + upAmount + "&downloaded=" + downAmount + "&left=" + (totalLength - downAmount) + "&compact=1&numwant=" + NUMWANT + "&event=" + event);
      if (debug) JFLog.log("url=" + url.toExternalForm());
      HttpURLConnection uc = (HttpURLConnection)url.openConnection();
      uc.setReadTimeout(30 * 1000);
      uc.connect();
      InputStream is = uc.getInputStream();
      int length = uc.getContentLength();
      metaData = JF.readAll(is, length);
      uc.disconnect();
      if (debug) JFLog.log("response=" + new String(metaData));
      metaFile.read(metaData);
      String failure = metaFile.getString(new String[] {"d", "s:failure reason"}, null);
      if (failure != null) {
        throw new Exception(failure);
      }
      if (event.equals("stopped") || event.equals("completed")) return;
      complete = (int)metaFile.getValue(new String[] {"d", "i:complete"}, null);
      incomplete = (int)metaFile.getValue(new String[] {"d", "i:incomplete"}, null);
      interval = (int)metaFile.getValue(new String[] {"d", "i:interval"}, null);
      peers = metaFile.getString(new String[] {"d", "s:peers"}, null).getBytes();
      if (peers == null) throw new Exception("no peers");
      int noPeers = peers.length/6;
      for(int a=0;a<noPeers;a++) {
        Peer p = new Peer();
        p.ip = getip(peers, a*6);
        p.port = getport(peers, a*6 + 4);
        boolean found = false;
        int size = peerList.size();
        for(int b=0;b<size;b++) {
          Peer pp = peerList.get(b);
          if ((pp.ip.equals(p.ip)) && (pp.port == p.port)) {
            found = true;
            break;
          }
        }
        if (!found) {
          peerList.add(p);
          if (debug) JFLog.log("peer[]=" + p.ip + ":" + p.port);
        }
      }
      if (!registered) {
        TorrentServer.register(this);
        registered = true;
      }
      if (done) {
        //may be 1st client seeding this torrent
        status = "Seeding";
        return;
      }
      status = "Downloading";
      if (peerList.isEmpty()) throw new Exception("no peers");
    } else if (announce.startsWith("udp://")) {
      throw new Exception("UDP protocol not supported yet");
    } else {
      throw new Exception("bad announce URL:" + announce);
    }
  }
  private void addPeer() throws Exception {
    if (peerActiveCount >= MAXPEERS) return;
    Peer peer;
    synchronized(peerListLock) {
      peer = peerList.get(peerIdx++);
      if (peerIdx >= peerList.size()) peerIdx = 0;
      if (peer.inuse) return;
      if (debug) JFLog.log("Connecting to peer:" + peer.ip + ":" + peer.port);
      peer.lastMsg = now;
      peer.inuse = true;
      peerActiveCount++;
    }
    peer.haveHandshake = false;
    peer.listener = new PeerListener(peer);
    peer.listener.start();
  }
  public void addPeer(Socket s, String id) throws Exception {
    //inbound peer from TorrentServer
    if (peerActiveCount >= MAXPEERS) {
      s.close();
      return;
    }
    Peer peer = null;
    String ip = s.getInetAddress().toString().substring(1);
    int port = s.getPort();
    synchronized(peerListLock) {
      for(int a=0;a<peerList.size();a++) {
        Peer pp = peerList.get(a);
        if (pp.ip.equals(ip) && pp.port == port) {
          peer = peerList.get(a);
          break;
        }
      }
      if ((peer == null) || (peer.inuse)) {
        peer = new Peer();
        peerList.add(peer);
      }
      peer.lastMsg = now;
      peer.inuse = true;
    }
    peer.haveHandshake = true;
    peer.ip = ip;
    peer.s = s;
    peer.port = port;
    peer.id = id;
    peer.is = s.getInputStream();
    peer.os = s.getOutputStream();
    peer.listener = new PeerListener(peer);
    peer.listener.start();
  }
  private void prunePeers() {
    long now_2mins = System.currentTimeMillis() - 120 * 1000;
    synchronized(peerListLock) {
      int size = peerList.size();
      for(int a=0;a<size;a++) {
        Peer peer = peerList.get(a);
        if (!peer.inuse) continue;
        if (peer.listener == null) continue;
        if (peer.lastMsg < now_2mins) {
          peer.listener.close();
        }
      }
    }
  }
  private class PeerListener extends Thread {
    private Peer peer;
    public volatile boolean listenerActive = true;
    public PeerListener(Peer peer) {
      this.peer = peer;
    }
    public void run() {
      try {
        if (debug) JFLog.log("Starting PeerListener:" + peer.ip + ":" + peer.port);
        if (peer.s == null) {
          peer.s = new Socket(peer.ip, peer.port);
          peer.is = peer.s.getInputStream();
          peer.os = peer.s.getOutputStream();
//          if (log) JFLog.log("send handshake:" + peer.ip);
        }
        //send handshake
        byte handshake[] = new byte[68];
        handshake[0] = 19;
        System.arraycopy("BitTorrent protocol".getBytes(),0,handshake,1,19);
        //8 reserved bytes
        System.arraycopy(info_hash,0,handshake,28,20);
        System.arraycopy(peer_id.getBytes(),0,handshake,48,20);
        peer.os.write(handshake);
        peer.os.flush();
        if (!peer.haveHandshake) {
          if (!getHandshake()) {
            throw new Exception("handshake failed");
          }
        }
        sendBitField();
//        if (log) JFLog.log("Pooling PeerListener:" + peer.ip);
        while (peer.s.isConnected()) {
          byte msg[] = getMessage();
          if (msg == null) break;
          processMessage(msg);
        }
      } catch (ConnectException e1) {
        if (debug) JFLog.log("Lost connection:" + peer.ip);
      } catch (Exception e2) {
        if (debug) JFLog.log(e2);
      }
      try { if (peer.s.isConnected()) peer.s.close(); } catch (Exception e1) { }
//      if (log) JFLog.log("Stopping peer:" + peer.ip);
      if (peer.downloader != null) {
        peer.downloader.downloaderActive = false;
        try { peer.downloader.join(); } catch (Exception e2) {}
        peer.downloader = null;
      }
      peer.s = null;
      peer.inuse = false;
      synchronized (peerListLock) {
        peerActiveCount--;
      }
    }
    public void close() {
      listenerActive = false;
      try { peer.s.close(); } catch (Exception e) {}
    }
    private boolean getHandshake() {
      byte buf[] = new byte[1024];
      byte handshake[] = new byte[68];
      byte peer_info_hash[] = new byte[20];
      int toRead = 68;
      int pos = 0;
//      if (log) JFLog.log("Waiting for handshake:" + peer.ip);
      try {
        while (toRead > 0) {
          int read = peer.is.read(buf, 0, toRead);
          if (read <= 0) throw new Exception("read error:" + peer.ip);
          System.arraycopy(buf, 0, handshake, pos, read);
          pos += read;
          toRead -= read;
        }
        if (handshake[0] != 19) throw new Exception("bad handshake (len!=19):" + peer.ip);
        if (!new String(handshake, 1, 19).equals("BitTorrent protocol")) throw new Exception("bad handshake (unknown protocol):" + peer.ip);
        System.arraycopy(handshake, 28, peer_info_hash, 0, 20);
        if (!Arrays.equals(peer_info_hash, info_hash)) throw new Exception("not my torrent:" + peer.ip);
        peer.id = new String(handshake, 48, 20);
        //TODO : validate handshake more???
        peer.haveHandshake = true;
        return true;
      } catch (Exception e) {
//        if (log) JFLog.log("getHandshake Exception:" + peer.ip);
//        if (log) JFLog.log(e);
      }
      return false;
    }
    private void sendBitField() throws Exception {
      int bytes = have.length / 8;
      if (bytes * 8 != have.length) bytes++;
      byte bits[] = new byte[bytes + 1];  //+1 for header
      bits[0] = BITFIELD;
      int Bo = 1;  //byte offset
      int bo = 128;  //bit offset
      int pidx = 0;
      int value = 0;
      while (pidx < have.length) {
        if (have[pidx++]) value |= bo;
        bo >>= 1;
        if (bo == 0) {bits[Bo++] = (byte)value; bo = 128; value = 0;}
      }
      if (bo != 128) {
        bits[Bo] = (byte)value;
      }
      writePacket(peer.os, bits);
    }
    private static final int BUFSIZ = 2048;
    private byte[] getMessage() {
      byte buf[] = new byte[BUFSIZ];
      byte len[] = new byte[4];
      byte msg[];
      int toRead = 4;
      int pos = 0;
      //read message length
      try {
        while (toRead > 0) {
          int read = peer.is.read(buf, 0, toRead);
          if (read == 0) continue;
          if (read == -1) throw new Exception("read error:" + peer.ip);
          System.arraycopy(buf, 0, len, pos, read);
          pos += read;
          toRead -= read;
        }
        int msglen = BE.getuint32(len, 0);
        if ((msglen > FRAGSIZE + 1 + 4 + 4) && (msglen > (have.length / 8) + 1 + 1)) throw new Exception("msg too large:" + peer.ip);
        msg = new byte[msglen];
        toRead = msglen;
        pos = 0;
        while (toRead > 0) {
          int read = peer.is.read(buf, 0, toRead > BUFSIZ ? BUFSIZ : toRead);
          if (read == 0) continue;
          if (read == -1) throw new Exception("read error:" + peer.ip);
          System.arraycopy(buf, 0, msg, pos, read);
          pos += read;
          toRead -= read;
        }
        return msg;
      } catch (Exception e) {
//        if (log) JFLog.log("getMessage Exception:" + peer.ip);
//        if (log) JFLog.log(e);
      }
      return null;
    }
    private void processMessage(byte msg[]) throws Exception {
      peer.lastMsg = now;
      if (msg.length == 0) {
        //keep alive
        return;
      }
      switch (msg[0]) {
        case CHOKE:
          peer.peer_choking = true;
          break;
        case UNCHOKE:
          peer.peer_choking = false;
          synchronized(peer.chokeLock) {peer.chokeLock.notify();}
          break;
        case INTERESTED:
          peer.peer_interested = true;
          peer.am_choking = false;
          writePacket(peer.os, new byte[] {UNCHOKE});
          break;
        case NOTINTERESTED:
          peer.peer_interested = false;
          peer.am_choking = true;
          writePacket(peer.os, new byte[] {CHOKE});
          break;
        case HAVE:
          have(BE.getuint32(msg, 1));
          break;
        case BITFIELD: bitfield(msg); break;
        case REQUEST: request(msg); break;
        case FRAGMENT: fragment(msg); break;
        case CANCEL: cancel(msg); break;
        case DHT_PORT: dht_port(msg); break;
      }
    }
    private void have(int pidx) {
      if (peer.have[pidx]) return;
      peer.have[pidx] = true;
      if (!have[pidx]) synchronized(peer.chokeLock) {peer.chokeLock.notify();}
      peer.available++;
      if (peer.available == have.length) {
        peer.seeder = true;
      }
    }
    private void bitfield(byte msg[]) {
      if (peer.downloader != null) return;  //already got bitfield
      peer.have = new boolean[pieces.length];
      int Bo = 1;
      int bo = 128;
      int idx = 0;
      int available = 0;
      while (idx < peer.have.length) {
        peer.have[idx] = (msg[Bo] & bo) == bo;
        if (peer.have[idx]) available++;
        idx++;
        bo >>= 1;
        if (bo == 0) {Bo++; bo = 128;}
      }
      peer.seeder = available == have.length;
      peer.available = available;
      if (done) return;
      peer.downloader = new PeerDownloader(peer);
      peer.downloader.start();
    }
    private void request(byte msg[]) throws Exception {
      if (peer.am_choking) return;
      int pidx = BE.getuint32(msg, 1);
      int begin = BE.getuint32(msg, 5);
      int length = BE.getuint32(msg, 9);
      if (length > 65536) return;
      upAmount += length;
      upAmountCnt += length;
      sendFragment(peer.os, pidx, begin, length);
    }
    private void fragment(byte msg[]) throws Exception {
      // FRAGMENT PIDX(4) BEGIN(4)
      int pidx = BE.getuint32(msg, 1);
      if (pidx != peer.downloadingPieceIdx) {if (debug) JFLog.log("frag:bad pidx:"+pidx);return;}
      int begin = BE.getuint32(msg, 5);
      int fidx = begin / FRAGSIZE;
      if (fidx >= peer.numFrags) {if (debug) JFLog.log("frag:bad fidx:"+fidx);return;}
      int length = msg.length - 9;
      if (peer.piece == null) {if (debug) JFLog.log("frag:not ready(1)");return;}
      if (begin + length > peer.piece.length) {if (debug) JFLog.log("frag:bad length:"+fidx);return;}
      synchronized(peer.fragLock) {
        if (peer.haveFrags[fidx]) {if (debug) JFLog.log("frag:already have fidx:"+fidx);return;}
        System.arraycopy(msg, 9, peer.piece, begin, length);
        if (debug) JFLog.log("gotFrag:" + pidx + "," + fidx);
        peer.haveFrags[fidx] = true;
        peer.gotFragCnt++;
        peer.pendingFrags--;
        peer.fragLock.notify();
      }
    }
    private void cancel(byte msg[]) {
    }
    private void dht_port(byte msg[]) {
    }
  }
  private class PeerDownloader extends Thread {
    private Peer peer;
    public volatile boolean downloaderActive = true;
    public PeerDownloader(Peer peer) {
      this.peer = peer;
    }
    public void run() {
      try {
        if (debug) JFLog.log("Starting PeerDownloader:" + peer.ip);
        synchronized(peer.chokeLock) {
          while (true) {
            if (done) return;
            if (!downloaderActive) return;
            if (!peer.inuse) return;
            //is there a piece we can get from them
            int startIdx = Math.abs(new Random().nextInt(have.length));  //pure random baby
            int pidx = startIdx;
            boolean ok = false;
            do {
              if (!have[pidx] && peer.have[pidx]) {
                ok = true;
                break;
              }
              pidx++;
              if (pidx == have.length) pidx = 0;
            } while (pidx != startIdx);
            if (!ok) {
              if (peer.am_interested) {
                writePacket(peer.os, new byte[] {NOTINTERESTED});
                peer.am_interested = false;
              }
              peer.chokeLock.wait();
              continue;
            }
            if (debug) JFLog.log("want " + pidx + " from " + peer.ip);
            if (!peer.am_interested) {
              writePacket(peer.os, new byte[] {INTERESTED});
              peer.am_interested = true;
            }
            while (peer.peer_choking) {
              peer.chokeLock.wait();
            }
            //calc fragments
            peer.downloadingPieceIdx = pidx;
            peer.pendingFrags = 0;
            if (pidx == have.length - 1) {
              //last piece
              peer.numFrags = (int)lastPieceLength / FRAGSIZE;
              if (peer.numFrags * FRAGSIZE != pieceLength) {
                peer.lastFragLength = (int)(lastPieceLength - (FRAGSIZE * peer.numFrags));
                peer.numFrags++;
              } else {
                peer.lastFragLength = FRAGSIZE;
              }
              peer.piece = new byte[(int)lastPieceLength];
            } else {
              peer.numFrags = numFragsPerPiece;
              peer.piece = new byte[(int)pieceLength];
              peer.lastFragLength = FRAGSIZE;
            }
            peer.haveFrags = new boolean[peer.numFrags];
            peer.gotFragCnt = 0;
            int nextFrag = 0;
            //now send frag requests (stack them up to FRAGSTACK)
            if (debug) JFLog.log("downloading fragments:#frags=" + peer.numFrags + ":from=" + peer.ip);
            while (peer.gotFragCnt != peer.numFrags) {
              synchronized (peer.fragLock) {
                if ((peer.pendingFrags < FRAGSTACK) && (nextFrag < peer.numFrags)) {
                  requestFragment(nextFrag++);
                  peer.pendingFrags++;
                }
                if ((peer.pendingFrags >= FRAGSTACK) || (peer.gotFragCnt + FRAGSTACK >= peer.numFrags)) {
                  peer.fragLock.wait();
                }
              }
            }
            byte sha[] = SHA1sum(peer.piece);
            if (debug) {
              JFLog.log("sha1.downloaded=" + escape(sha));
              JFLog.log("sha1.peice     =" + escape(pieces[peer.downloadingPieceIdx]));
            }
            if (Arrays.equals(sha, pieces[peer.downloadingPieceIdx])) {
              savePiece(peer.downloadingPieceIdx, peer.piece);
              if (done) return;
              broadcastHave();
            } else {
              JFLog.log("bad piece downloaded from peer:" + peer.ip);
            }
            peer.downloadingPieceIdx = -1;
            peer.piece = null;
          }
        }
      } catch (Exception e) {
      }
    }
    private void requestFragment(int fidx) throws Exception {
      byte msg[] = new byte[1 + 4 + 4 + 4];
      msg[0] = REQUEST;
      BE.setuint32(msg, 1, peer.downloadingPieceIdx);
      BE.setuint32(msg, 5, fidx * FRAGSIZE);
      if (fidx == peer.haveFrags.length-1)
        BE.setuint32(msg, 9, peer.lastFragLength);
      else
        BE.setuint32(msg, 9, FRAGSIZE);
      writePacket(peer.os, msg);
      if (debug) JFLog.log("requestFrag:" + peer.downloadingPieceIdx + "," + fidx);
    }
    private void broadcastHave() {
      byte msg[] = new byte[5];
      msg[0] = HAVE;
      BE.setuint32(msg, 1, peer.downloadingPieceIdx);
      synchronized(peerListLock) {
        for(int a=0;a<peerList.size();a++) {
          Peer p = peerList.get(a);
          if (!p.inuse) continue;
          try {writePacket(p.os, msg);} catch (Exception e) {}
        }
      }
    }
  }
  private void sendFragment(OutputStream os, int pidx, int fbegin, int flength) throws Exception {
    //TODO : throttle bandwidth
    byte frag[] = new byte[flength];
    long begin = pidx * pieceLength + fbegin;
    int fragOff = 0;
    long pos = 0;
    int toRead;
    for(int a=0;a<files.size();a++) {
      MetaFile tfile = files.get(a);
      if (pos + tfile.length <= begin) {
        pos += tfile.length;
        continue;
      }
      long filePos = begin + fragOff - pos;
      if (tfile.length - filePos < frag.length - fragOff) {
        //read to end of file
        synchronized(tfile.lock) {
          if (tfile.file == null) {
            tfile.file = new RandomAccessFile(dest + "/" + tfile.name, "rw");
          }
          tfile.file.seek(filePos);
          toRead = (int)(tfile.length - filePos);
          JF.readAll(tfile.file, frag, fragOff, toRead);
        }
        pos += tfile.length;
        fragOff += toRead;
        continue;
      }
      //read rest of fragment
      synchronized(tfile.lock) {
        if (tfile.file == null) {
          tfile.file = new RandomAccessFile(dest + "/" + tfile.name, "rw");
        }
        tfile.file.seek(filePos);
        toRead = (int)(frag.length - fragOff);
        JF.readAll(tfile.file, frag, fragOff, toRead);
      }
//      pos += tfile.length;
//      fragOff += toRead;
      break;
    }
    byte msg[] = new byte[1 + 4 + 4 + frag.length];
    msg[0] = FRAGMENT;
    msg[1] = (byte)((pidx & 0xff000000) >>> 24);
    msg[2] = (byte)((pidx & 0xff0000) >> 16);
    msg[3] = (byte)((pidx & 0xff00) >> 8);
    msg[4] = (byte)(pidx & 0xff);
    msg[5] = (byte)((fbegin & 0xff000000) >>> 24);
    msg[6] = (byte)((fbegin & 0xff0000) >> 16);
    msg[7] = (byte)((fbegin & 0xff00) >> 8);
    msg[8] = (byte)(fbegin & 0xff);
    System.arraycopy(frag, 0, msg, 9, frag.length);
    writePacket(os, msg);
  }
  private synchronized void savePiece(int pidx, byte piece[]) throws Exception {
    //piece already validated
    //piece may span files
    if (have[pidx]) return;  //already have this piece
    if (debug) JFLog.log(" --- savePiece --- idx=" + pidx);
    long begin = pidx * pieceLength;
    int pieceOff = 0;
    long pos = 0;
    int write;
    for(int a=0;a<files.size();a++) {
      MetaFile tfile = files.get(a);
      if (pos + tfile.length < begin) {
        pos += tfile.length;
        continue;
      }
      long filePos = begin + pieceOff - pos;
      if (tfile.length - filePos < piece.length - pieceOff) {
        //write to end of file
        synchronized(tfile.lock) {
          if (tfile.file == null) {
            tfile.mkdirs(dest);
            tfile.file = new RandomAccessFile(dest + "/" + tfile.name, "rw");
          }
          tfile.file.seek(filePos);
          write = (int)(tfile.length - filePos);
          tfile.file.write(piece, pieceOff, write);
        }
        pos += tfile.length;
        pieceOff += write;
        continue;
      }
      //write rest of piece
      synchronized(tfile.lock) {
        if (tfile.file == null) {
          tfile.mkdirs(dest);
          tfile.file = new RandomAccessFile(dest + "/" + tfile.name, "rw");
        }
        tfile.file.seek(filePos);
        write = (int)(piece.length - pieceOff);
        tfile.file.write(piece, pieceOff, write);
      }
      break;
    }
    have[pidx] = true;
    haveCnt++;
    downAmount += piece.length;
    downAmountCnt += piece.length;
    if (haveCnt == pieces.length) {
      status = "Seeding";
      done = true;
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          MainPanel.This.updateList();
        }
      });
      contactTracker("completed");
    }  //all downloaders will stop now
  }
  public void reset() {
    downAmountCnt = 0;
    upAmountCnt = 0;
  }
  public void update() {
    seeders = 0;
    available = 0.0f;
    int size = peerList.size();
    for(int a=0;a<size;a++) {
      Peer peer = peerList.get(a);
      if (peer.seeder) seeders++;
      available += (float)peer.available / (float)have.length;
    }
    available = available * 100 / (float)size;  //average availablity ???
    //BUG : this available is not right - should not be an average ???
    //what if all peers are missing the same piece - then available would be < 1.0 ???
    downSpeed = (int)(downAmountCnt / 5);
    downAmountCnt = 0;
    upSpeed = (int)(upAmountCnt / 5);
    upAmountCnt = 0;
  }
  private void writePacket(OutputStream os, byte msg[]) throws Exception {
    byte packet[] = new byte[msg.length + 4];
    packet[0] = (byte)((msg.length & 0xff000000) >>> 24);
    packet[1] = (byte)((msg.length & 0xff0000) >> 16);
    packet[2] = (byte)((msg.length & 0xff00) >> 8);
    packet[3] = (byte)(msg.length & 0xff);
    System.arraycopy(msg, 0, packet, 4, msg.length);
    os.write(packet);  //BUG : can you write large packets???
    os.flush();
  }
  /** Closes the torrent, can not be re-started. */
  public void close() {
    if (timer == null) return;
    timer.cancel();
    timer = null;
    if (registered) {
      TorrentServer.unregister(this);
      registered = false;
    }
    new Thread() {
      public void run() {
        try {contactTracker("stopped");} catch (Exception e) {}
      }
    }.start();
    active = false;
  }
  public void pause() {
    status = "Paused";
    paused = true;
  }
  public void unpause() {
    status = done ? "Seeding" : "Downloading";
    paused = false;
  }
  /** Stops the torrent, but can be started again. */
  public void _stop() {
    if (!active) return;
    active = false;
    status = "Stopped";
    //close all Peers
    synchronized(peerListLock) {
      for(int a=0;a<peerList.size();a++) {
        Peer peer = peerList.get(a);
        if (peer.listener != null) peer.listener.close();
      }
    }
  }
  public void _start() {
    if (timer == null) return;  //torrent was close()d
    if (paused) {unpause(); return;}
    status = done ? "Seeding" : "Downloading";
    if (active) return;
    active = true;
  }
  public void recontactTracker() {
    //do this if TorrentServer.port changes
    intervalCounter = 0;
  }
}

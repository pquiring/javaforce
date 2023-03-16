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
  public static final String clientVersion = "0003";  //must be 4 digits

  private static final int FRAGSIZE = 16 * 1024;  //16k
  private static final int MAXPEERS = 30;  //max # active peers
  private static final int NUMWANT = 80;  //# peers requested from tracker (advisory)

  private static final boolean enable_dht = true;

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
  private byte[] metaData;
  private TorrentFile metaFile;
  public String status;
  private boolean[] have;
  private int haveCnt = 0;
  private String announce;
  private ArrayList<String> announceList = new ArrayList<String>();
  private byte[] info_hash;
  public long totalLength = 1;  //must not be zero - causes / by zero
  private long pieceLength;
  private long lastPieceLength;  //need to calc
  public String name;
  private int noFiles;
  private ArrayList<MetaFile> files = new ArrayList<MetaFile>();
  private byte[][] pieces;  //SHA1
  private int numFragsPerPiece;
  private final int FRAGSTACK = 3;

  //client info
  private String peer_id;  //Torrent protocol
  private String node_id;  //DHT protocol

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

  //DHT support
  private DatagramSocket local_dht_ds;
  private int local_dht_port;
  private DHTListener dht_listener;
  private ArrayList<Node> nodeList = new ArrayList<Node>();
  private final Object nodeListLock = new Object();

  //Torrent protocol commands
  private final int CHOKE = 0;
  private final int UNCHOKE = 1;
  private final int INTERESTED = 2;
  private final int NOTINTERESTED = 3;
  private final int HAVE = 4;
  private final int BITFIELD = 5;
  private final int REQUEST = 6;  //request fragment
  private final int FRAGMENT = 7;  //deliver fragment - piece of a piece (I call them fragments)
  private final int CANCEL = 8;
  private final int PORT = 9;  //DHT port

  /** Peer for this torrent */
  private static class Peer {
    public boolean inuse = false;
    public boolean seeder = false;
    public int available = 0;
    public String ip, id;
    public int port;
    public InetAddress ipaddr;
    public InetSocketAddress ipaddrport;
    public boolean dht;
    public int dhtport;
//    public int extdhtport;

    //Unicast TCP
    public Socket s;
    public InputStream is;
    public OutputStream os;
    //Multicast UDP
    public MulticastSocket ms;
    public NetworkInterface netif;

    public final Object lock = new Object();
    public boolean am_choking = true, am_interested = false;
    public boolean peer_choking = true, peer_interested = false;
    public boolean haveHandshake = false;
    public PeerListener listener;
    public PeerDownloader downloader;
    public long lastMsg;
    public boolean[] have;
    public byte[] piece;
    public boolean[] haveFrags;
    public int gotFragCnt, numFrags;
    public int downloadingPieceIdx = -1;
    public final Object chokeLock = new Object();
    public final Object fragLock = new Object();
    public int lastFragLength;
    public int pendingFrags;
    public void write(byte[] buf) throws Exception {
//      log("writeTo:" + ip + ":" + port + ":Len=" + buf.length);
      if (s != null) {
        os.write(buf);
        os.flush();
        return;
      }
      if (ms != null) {
        DatagramPacket packet = new DatagramPacket(buf, buf.length, ipaddrport);
        ms.send(packet);
        return;
      }
      log("Error:write but not connected");
    }
    private byte[] overflow;
    private byte[] datagram;
    public int read(byte[] buf, int pos, int len) throws Exception {
//      log("readFrom:" + ip + ":" + port + ":Len=" + len);
      if (s != null) {
        int ret = is.read(buf, pos, len);
//        log("unicast.read.Len=" + ret);
        return ret;
      }
      if (ms != null) {
        //with UDP when a packet is read any overflow is lost
        if (overflow != null) {
          if (len >= overflow.length) {
            //return all of overflow
            System.arraycopy(overflow, 0, buf, pos, overflow.length);
            int ret = overflow.length;
            overflow = null;
//            log("read.Len=" + ret + " (overflow-all)");
            return ret;
          } else {
            //return part of overflow
            System.arraycopy(overflow, 0, buf, pos, len);
            overflow = Arrays.copyOfRange(overflow, len, overflow.length);
//            log("read.Len=" + len + " (overflow-partial)");
            return len;
          }
        }
        DatagramPacket packet = new DatagramPacket(datagram, datagram.length);
        ms.receive(packet);
        int packLen = packet.getLength();
        log("multicast.read.len=" + packLen);
        if (packLen > len) {
          //save overflow
          int overlen = packLen - len;
          overflow = new byte[overlen];
          System.arraycopy(datagram, len, overflow, 0, overlen);
          packLen = len;
        }
        System.arraycopy(datagram, 0, buf, pos, packLen);
//        log("read.Len=" + packLen);
        return packLen;
      }
      log("Error:read but not connected");
      return 0;
    }
    public boolean connect() throws Exception {
      ipaddr = InetAddress.getByName(ip);
      ipaddrport = new InetSocketAddress(ipaddr, port);
      if (ipaddr.isMulticastAddress()) {
        ms = new MulticastSocket(port);
        netif = NetworkInterface.getByInetAddress(ipaddr);
        ms.joinGroup(ipaddrport, netif);
        datagram = new byte[65527];  //largest UDP packet size (IPv6)
      } else {
        s = new Socket(ip, port);
        is = s.getInputStream();
        os = s.getOutputStream();
      }
      return true;
    }
    public void close() throws Exception {
      if (s != null) {
        try { s.close(); } catch (Exception e) {}
        s = null;
      }
      if (ms != null) {
        ms.leaveGroup(ipaddrport, netif);
        ms = null;
        datagram = null;
        overflow = null;
      }
    }
    public boolean isConnected() {
      if (s != null) {
        return s.isConnected();
      }
      if (ms != null) {
        return true;  //connectionless
      }
      return false;
    }
    public void log(String msg) {
      if (debug) JFLog.log("peer:" + ip + ":" + port + ":" + msg);
    }
  }

  /** DHT Node. */
  public class Node {
    public String node_id;
    public String ip;
    public InetAddress ipaddr;
    public int port;

    public void init(String ip, int port) {
      this.ip = ip;
      try { this.ipaddr = InetAddress.getByName(ip); } catch (Exception e) {}
      this.port = port;
    }

    public void write(byte[] msg) {
      DatagramPacket packet = new DatagramPacket(msg, msg.length, ipaddr, port);
      try {
        local_dht_ds.send(packet);
      } catch (Exception e) {
        if (debug) JFLog.log(e);
      }
    }
    public void log(String msg) {
      if (debug) JFLog.log("node:" + ip + ":" + port + ":" + msg);
    }
  }

  public static void main(String[] args) {
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
      node_id = "-JD" + clientVersion + "-";
      Random r = new Random();
      while (peer_id.length() != 20) {
        char ch = (char)(r.nextBoolean() ? 'a' : 'A' + r.nextInt(26));
        peer_id += ch;
        node_id += ch;
      }
      //read torrent
      if (debug) JFLog.log("Reading torrent:" + torrent);
      readMeta();
      if (debug) JFLog.log("Getting info...");
      MetaDict info = metaFile.getDict(new String[] {"d", "d:info"}, null);
      info_hash = SHA_1(Arrays.copyOfRange(metaData, info.pos1, info.pos2));
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
      if (enable_dht) {
        initDHT();
      }
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
                byte hash[] = SHA_1(Arrays.copyOfRange(piece,0,thisPieceLength));
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

  public static String hexToString(byte[] hex) {
    StringBuilder str = new StringBuilder();
    for(int a=0;a<hex.length;a++) {
      if (a > 0) str.append(",");
      str.append(String.format("%02x", hex[a] & 0xff));
    }
    return str.toString();
  }

  public static byte[] digest(byte[] data, String type) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance(type);
    byte[] digest = md.digest(data);
    if (debug) {
      JFLog.log("digest/" + type + ":" + hexToString(digest));
    }
    return digest;
  }

  public static byte[] SHA_1(byte[] data) throws NoSuchAlgorithmException {
    return digest(data, "SHA-1");
  }

  public static byte[] SHA_256(byte[] data) throws NoSuchAlgorithmException {
    return digest(data, "SHA-256");
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
      str.append("%" + String.format("%02X", (((int)hash[a]) & 0xff)));
    }
    return str.toString();
  }
  private String getip(byte[] data, int pos) {
    return "" + ((int)data[pos++] & 0xff) + "." + ((int)data[pos++] & 0xff) + "."
      + ((int)data[pos++] & 0xff) + "." + ((int)data[pos++] & 0xff);
  }
  private int getport(byte[] data, int pos) {
    return BE.getuint16(data, pos);
  }
  private void contactTracker(String event) throws Exception {
    if (announce.startsWith("http://") || announce.startsWith("https://")) {
      URL url = new URL(announce + "?info_hash=" + escape(info_hash) + "&peer_id=" + peer_id + "&port=" + MainPanel.config.port +
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
      peers = metaFile.getData(new String[] {"d", "s:peers"}, null);
      if (peers == null) throw new Exception("no peers");
      int noPeers = peers.length/6;
      if (debug) JFLog.log("# peers=" + noPeers);
      for(int a=0;a<noPeers;a++) {
        Peer p = new Peer();
        p.ip = getip(peers, a*6);
        p.port = getport(peers, a*6 + 4);
        p.ipaddr = InetAddress.getByName(p.ip);
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
      if (peer.ipaddr.isMulticastAddress()) return;
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
    String ip = s.getInetAddress().getHostAddress();
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
    peer.ipaddr = InetAddress.getByName(ip);
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
          try {
            peer.listener.close();
          } catch (Exception e) {}
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
        peer.log("Starting PeerListener");
        if (!peer.isConnected()) {
          peer.connect();
        }
        peer.log("Sending handshake");
        //send handshake
        byte handshake[] = new byte[68];
        handshake[0] = 19;  //magic length (0)
        System.arraycopy("BitTorrent protocol".getBytes(),0,handshake,1,19);  //magic (1-19)
        //8 reserved bytes (20-27)
        if (enable_dht) {
          handshake[27] = (byte)0x01;  //set last bit to let tracker know DHT is requested
        }
        System.arraycopy(info_hash,0,handshake,28,20);  //info_hash (28-47)
        System.arraycopy(peer_id.getBytes(),0,handshake,48,20);  //peer_id (48-67)
        peer.write(handshake);
        if (!peer.haveHandshake) {
          if (!getHandshake()) {
            throw new Exception("handshake failed");
          }
        }
        if (peer.dht) {
          sendDHTPort();
        }
        sendBitField();
        while (peer.isConnected()) {
          byte msg[] = getMessage();
          if (msg == null) break;
          processMessage(msg);
        }
      } catch (ConnectException e1) {
        peer.log("Lost connection");
      } catch (Exception e2) {
        if (debug) JFLog.log(e2);
      }
      try { peer.close(); } catch (Exception e1) { }
      peer.log("Stopping peer");
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
    public void close() throws Exception {
      listenerActive = false;
      peer.close();
    }
    private boolean getHandshake() {
      byte buf[] = new byte[1024];
      byte handshake[] = new byte[68];
      byte peer_info_hash[] = new byte[20];
      int toRead = 68;
      int pos = 0;
      peer.log("Waiting for handshake");
      try {
        while (toRead > 0) {
          int read = peer.read(buf, 0, toRead);
          if (read <= 0) throw new Exception("read error:" + peer.ip);
          System.arraycopy(buf, 0, handshake, pos, read);
          pos += read;
          toRead -= read;
        }
        if (handshake[0] != 19) throw new Exception("bad handshake (len!=19):" + peer.ip);
        if (!new String(handshake, 1, 19).equals("BitTorrent protocol")) throw new Exception("bad handshake (unknown protocol):" + peer.ip);
        peer.dht = (handshake[27] & 0x01) == 0x01;
        peer.log("Supports DHT:" + peer.dht);
        peer.log("Handshake=" + hexToString(handshake));
        System.arraycopy(handshake, 28, peer_info_hash, 0, 20);
        if (!Arrays.equals(peer_info_hash, info_hash)) throw new Exception("not my torrent:" + peer.ip);
        peer.id = new String(handshake, 48, 20);
        //TODO : validate handshake more???
        peer.haveHandshake = true;
        peer.log("Got valid handshake");
        return true;
      } catch (Exception e) {
        peer.log("getHandshake Exception");
        if (debug) JFLog.log(e);
      }
      return false;
    }
    private void sendBitField() throws Exception {
      peer.log("sendBitField");
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
      writeMessage(peer, bits);
    }
    private void sendDHTPort() throws Exception {
      peer.log("sendDHTPort");
      byte[] msg = new byte[3];
      msg[0] = PORT;
      BE.setuint16(msg, 1, local_dht_port);
      writeMessage(peer, msg);
    }
    private void sendDHTPing(Node node) throws Exception {
      if (debug) node.log("Sending ping");
      StringBuilder msg = new StringBuilder();
      msg.append("d");
        msg.append("1:a");
          msg.append("d");
            msg.append("2:id");
            msg.append("20:"); msg.append(node_id);
          msg.append("e");
        msg.append("1:q");  //query
          msg.append("4:ping");
        msg.append("1:t");
          msg.append("2:aa");
        msg.append("1:y");
          msg.append("1:q");
        msg.append("e");
      node.write(msg.toString().getBytes());
    }
    private static final int BUFSIZ = 2048;
    private byte[] getMessage() {
      byte[] buf = new byte[BUFSIZ];
      byte[] len = new byte[4];
      byte[] msg;
      int toRead = 4;
      int pos = 0;
      try {
        //read message length
        while (toRead > 0) {
          int read = peer.read(buf, 0, toRead);
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
        //read message content
        while (toRead > 0) {
          int read = peer.read(buf, 0, toRead > BUFSIZ ? BUFSIZ : toRead);
          if (read == 0) {
            peer.log("read=0");
            continue;
          }
          if (read == -1) throw new Exception("read error");
          System.arraycopy(buf, 0, msg, pos, read);
          pos += read;
          toRead -= read;
        }
        peer.log("message received");
        return msg;
      } catch (Exception e) {
        peer.log("getMessage Exception");
        if (debug) JFLog.log(e);
      }
      return null;
    }
    private void processMessage(byte[] msg) throws Exception {
      peer.lastMsg = now;
      if (msg.length == 0) {
        //keep alive
        if (debug) peer.log("received keepAlive");
        return;
      }
      peer.log("message=" + (msg[0] & 0xff));
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
          writeMessage(peer, new byte[] {UNCHOKE});
          break;
        case NOTINTERESTED:
          peer.peer_interested = false;
          peer.am_choking = true;
          writeMessage(peer, new byte[] {CHOKE});
          break;
        case HAVE:
          have(BE.getuint32(msg, 1));
          break;
        case BITFIELD: bitfield(msg); break;
        case REQUEST: request(msg); break;
        case FRAGMENT: fragment(msg); break;
        case CANCEL: cancel(msg); break;
        case PORT: port(msg);
          break;
        default: {
          if (debug) {
            peer.log("Unknown message:" + hexToString(msg));
          }
        }
      }
    }
    private void have(int pidx) {
      if (debug) peer.log("have:" + pidx);
      if (peer.have == null) {
        peer.have = new boolean[pieces.length];
      }
      if (peer.have[pidx]) return;
      peer.have[pidx] = true;
      if (!have[pidx]) synchronized(peer.chokeLock) {peer.chokeLock.notify();}
      peer.available++;
      if (peer.available == have.length) {
        peer.seeder = true;
      }
      if (peer.downloader == null) {
        peer.log("received have:starting downloader:available=" + available);
        peer.downloader = new PeerDownloader(peer);
        peer.downloader.start();
      }
    }
    private void bitfield(byte[] msg) {
      if (peer.downloader != null) {
        peer.log("bitfield() : already downloading");
        return;
      }
      if (peer.have == null) {
        peer.have = new boolean[pieces.length];
      }
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
      if (done) {
        peer.log("Peer.bitfield() : already done");
        return;
      }
      if (peer.downloader == null) {
        peer.log("received bit field:starting downloader:available=" + available);
        peer.downloader = new PeerDownloader(peer);
        peer.downloader.start();
      }
    }
    private void request(byte[] msg) throws Exception {
      if (peer.am_choking) return;
      int pidx = BE.getuint32(msg, 1);
      int begin = BE.getuint32(msg, 5);
      int length = BE.getuint32(msg, 9);
      if (length > 65536) return;
      upAmount += length;
      upAmountCnt += length;
      sendFragment(peer, pidx, begin, length);
    }
    private void fragment(byte[] msg) throws Exception {
      // FRAGMENT PIDX(4) BEGIN(4)
      int pidx = BE.getuint32(msg, 1);
      if (pidx != peer.downloadingPieceIdx) {peer.log("frag:bad pidx:"+pidx);return;}
      int begin = BE.getuint32(msg, 5);
      int fidx = begin / FRAGSIZE;
      if (fidx >= peer.numFrags) {peer.log("frag:bad fidx:"+fidx);return;}
      int length = msg.length - 9;
      if (peer.piece == null) {peer.log("frag:not ready(1)");return;}
      if (begin + length > peer.piece.length) {peer.log("frag:bad length:"+fidx);return;}
      synchronized(peer.fragLock) {
        if (peer.haveFrags[fidx]) {peer.log("frag:already have fidx:"+fidx);return;}
        System.arraycopy(msg, 9, peer.piece, begin, length);
        peer.log("gotFrag:" + pidx + "," + fidx);
        peer.haveFrags[fidx] = true;
        peer.gotFragCnt++;
        peer.pendingFrags--;
        peer.fragLock.notify();
      }
    }
    private void cancel(byte[] msg) {
      peer.log("cancel not supported yet");
    }
    private void port(byte[] msg) {
      if (msg.length != 3) {
        peer.log("invalid PORT command:len=" + msg.length);
        return;
      }
      peer.dhtport = getport(msg, 1);
      peer.log("DHT Port=" + peer.dhtport);
      Node node = new Node();
      node.init(peer.ip, peer.dhtport);
      synchronized (nodeListLock) {
        nodeList.add(node);
      }
      try {
        sendDHTPing(node);
      } catch (Exception e) {
        if (debug) JFLog.log(e);
      }
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
        peer.log("Starting PeerDownloader");
        synchronized(peer.chokeLock) {
          while (true) {
            if (done) {
              peer.log("download done, stopping");
              break;
            }
            if (!downloaderActive) {
              peer.log("downloader not active, stopping");
              break;
            }
            if (!peer.inuse) {
              peer.log("not in use, stopping");
              break;
            }
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
                writeMessage(peer, new byte[] {NOTINTERESTED});
                peer.am_interested = false;
              }
              peer.log("NOTINTERESTED:wait()ing");
              peer.chokeLock.wait();
              continue;
            }
            peer.log("wants " + pidx);
            if (!peer.am_interested) {
              writeMessage(peer, new byte[] {INTERESTED});
              peer.am_interested = true;
            }
            while (peer.peer_choking) {
              peer.log("CHOKING:wait()ing");
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
            peer.log("downloading fragments:#frags=" + peer.numFrags);
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
            byte sha[] = SHA_1(peer.piece);
            if (debug) {
              peer.log("sha1.downloaded=" + escape(sha));
              peer.log("sha1.peice     =" + escape(pieces[peer.downloadingPieceIdx]));
            }
            if (Arrays.equals(sha, pieces[peer.downloadingPieceIdx])) {
              savePiece(peer.downloadingPieceIdx, peer.piece);
              if (done) {
                peer.log("download done, stopping");
                break;
              }
              broadcastHave();
            } else {
              peer.log("bad piece downloaded");
            }
            peer.downloadingPieceIdx = -1;
            peer.piece = null;
          }
        }
      } catch (Exception e) {
      }
      peer.log("Downloader stopping");
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
      writeMessage(peer, msg);
      peer.log("requestFrag:" + peer.downloadingPieceIdx + "," + fidx);
    }
    private void broadcastHave() {
      byte msg[] = new byte[5];
      msg[0] = HAVE;
      BE.setuint32(msg, 1, peer.downloadingPieceIdx);
      synchronized(peerListLock) {
        for(int a=0;a<peerList.size();a++) {
          Peer peer = peerList.get(a);
          if (!peer.inuse) continue;
          try {writeMessage(peer, msg);} catch (Exception e) {}
        }
      }
    }
  }
  private void sendFragment(Peer peer, int pidx, int fbegin, int flength) throws Exception {
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
    writeMessage(peer, msg);
  }
  private synchronized void savePiece(int pidx, byte[] piece) throws Exception {
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
  private void writeMessage(Peer peer, byte[] msg) throws Exception {
    byte packet[] = new byte[msg.length + 4];
    packet[0] = (byte)((msg.length & 0xff000000) >>> 24);
    packet[1] = (byte)((msg.length & 0xff0000) >> 16);
    packet[2] = (byte)((msg.length & 0xff00) >> 8);
    packet[3] = (byte)(msg.length & 0xff);
    System.arraycopy(msg, 0, packet, 4, msg.length);
    peer.write(packet);
  }
  private void sendQuery(Node node, byte[] msg) throws Exception {
    node.write(msg);
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
        if (peer.listener != null) {
          try {
            peer.listener.close();
          } catch (Exception e) {}
        }
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
  public void initDHT() {
    try {
      local_dht_ds = new DatagramSocket();
      local_dht_port = local_dht_ds.getLocalPort();
      if (debug) JFLog.log("DHT.localport=" + local_dht_port);
      dht_listener = new DHTListener(local_dht_ds);
      dht_listener.start();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public class DHTListener extends Thread {
    private DatagramSocket ds;
    private byte[] data = new byte[65527];  //max UDP packet

    public DHTListener(DatagramSocket ds) {
      this.ds = ds;
    }
    public void run() {
      if (debug) JFLog.log("Starting DHTListener");
      while (active) {
        try {
          if (debug) JFLog.log("Listening for DHT packet");
          DatagramPacket packet = new DatagramPacket(data, data.length);
          ds.receive(packet);
          int len = packet.getLength();
          if (debug) {
            JFLog.log("DHT.recv.len=" + len);
            JFLog.log("DHT.recv.str=" + new String(data, 0, len));
          }
        } catch (Exception e) {
          JFLog.log(e);
          JF.sleep(100);
        }
      }
      if (debug) JFLog.log("Stopping DHTListener");
    }
  }
}

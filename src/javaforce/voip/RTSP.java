package javaforce.voip;

import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Base class for RTSP communications (Real Time Streaming Protocol).
 * Opens the TCP port and passes any received packets thru the RTSPInterface.
 * Direct Known subclasses : RTSPClient
 *
 * RFC : http://tools.ietf.org/html/rfc2326.html - RTSP
 */

public abstract class RTSP implements TransportInterface {
  static {
    RTSPURL.register();
  }
  private WorkerReader worker_reader;
  private WorkerPacket worker_packet;
  private PacketPool pool;
  private RTSPInterface iface;
  private boolean active = true;
  private String rinstance;
  private String tupleid;
  private Random r = new Random();
  private boolean server;
  protected Transport transport;
  protected String localhost;
  protected int localport;
  protected static String useragent = "JavaForce/" + JF.getVersion();
  public int log;
  public static boolean debug = false;

  /**
   * Opens the transport and sets the RTSPInterface callback.
   */
  protected boolean init(String localhost, int localport, RTSPInterface iface, boolean server, TransportType type) throws Exception {
    JFLog.log(log, "RTSP:local=" + localhost + ":" + localport);
    rinstance = null;
    this.iface = iface;
    this.server = server;
    switch (type) {
      case UDP:
        transport = new TransportUDP();
        break;
      case TCP:
        if (server)
          transport = new TransportTCPServer();
        else
          transport = new TransportTCPClient();
        break;
      case TLS:
        if (server)
          transport = new TransportTLSServer();
        else
          transport = new TransportTLSClient();
        break;
    }
    if (!transport.open(localhost, localport, this)) return false;
    this.localhost = localhost;
    this.localport = localport;
    pool = new PacketPool(mtu);
    worker_reader = new WorkerReader();
    worker_reader.start();
    if (server) {
      worker_packet = new WorkerPacket();
      worker_packet.start();
    }
    return true;
  }

  /**
   * Closes the UDP port and frees resources.
   */
  protected void uninit() {
    if (debug) JFLog.log(log, "RTSP.uninit() start:" + this);
    if (transport == null) {
      return;
    }
    active = false;
    transport.close();
    try {
      if (worker_reader != null) {
        if (debug) JFLog.log(log, "RTSP.uninit() waiting for reader:" + this);
        worker_reader.join();
      }
    } catch (Exception e) {
      JFLog.log(log, e);
    }
    worker_reader = null;
    if (server) {
      if (worker_packet != null) {
        worker_packet.cancel();
        try {
          worker_packet.join();
        } catch (Exception e) {
          JFLog.log(log, e);
        }
      }
      worker_packet = null;
    }
    pool = null;
    transport = null;
    worker_reader = null;
    if (debug) JFLog.log(log, "RTSP.uninit() done:" + this);
  }

  public void setLog(int id) {
    log = id;
  }

  /**
   * Sends a packet out on the UDP port.
   */
  protected boolean send(InetAddress remote, int remoteport, String datastr) {
    byte[] data = datastr.getBytes();
    if (debug) JFLog.log(log, "RTSP:send:remote=" + remote.getHostAddress() + ":" + remoteport);
    return transport.send(data, 0, data.length, remote, remoteport);
  }

  /**
   * Joins a To: or From: field after it was split into parts.
   */
  public static String join(String[] x) {
    //x = "display name" <sip:user@host ;...  > ;...
    //return:    [0]          [1]  [2]  [...][:][...]
    if (x == null) {
      return "\"null\"<sip:null@null>";
    }
    StringBuilder buf = new StringBuilder();
    if (x[0].length() > 0) {
      buf.append('\"');
      buf.append(x[0]);
      buf.append('\"');
      buf.append('<');
    }
    buf.append("sip:");
    if (x[1].length() > 0) {
      buf.append(x[1]);
      buf.append('@');
    }
    buf.append(x[2]);
    int i = 3;
    for (; (i < x.length) && (!x[i].equals(":")); i++) {
      buf.append(';');
      buf.append(x[i]);
    }
    i++;  //skip ':' seperator
    if (x[0].length() > 0) buf.append('>');
    for (; i < x.length; i++) {
      buf.append(';');
      buf.append(x[i]);
    }
    return buf.toString();
  }

  /**
   * Returns a flag in a To: From: field.
   */
  public static String getFlag2(String[] fields, String flg) {
    flg += "=";
    int i;
    for (i = 0; i < fields.length; i++) {
      if (fields[i].equals(":")) {
        break;
      }
    }
    if (i == fields.length) {
      return "";
    }
    i++;
    for (; i < fields.length; i++) {
      if (fields[i].startsWith(flg)) {
        return fields[i].substring(flg.length());
      }
    }
    return "";  //do not return null
  }

  /**
   * Sets/adds a flag in a To: From: field.
   */
  public static String[] setFlag2(String[] fields, String flg, String value) {
    flg += "=";
    boolean seperator = false;
    for (int i = 3; i < fields.length; i++) {
      if (!seperator) {
        if (fields[i].equals(":")) {
          seperator = true;
        }
        continue;
      }
      if (fields[i].startsWith(flg)) {
        fields[i] = flg + value;
        return fields;
      }
    }
    //need to add an element to fields and append "flg=value"
    String[] newfields = new String[fields.length + 1];
    for (int j = 0; j < fields.length; j++) {
      newfields[j] = fields[j];
    }
    newfields[fields.length] = flg + value;
    return newfields;
  }

  /**
   * Returns a random SIP branch id.
   * TODO : Implement RFC 3261 section 8.1.1.7 (z9hG4bK)
   */
  public String getbranch() {
    return String.format("z123456-y12345-%x%x-1--d12345-", r.nextInt(), r.nextInt());
  }

  /** Returns branch in first Via line */
  protected String getbranch(String[] msg) {
    String[] vias = getvialist(msg);
    if (vias == null || vias.length == 0) return null;
    //Via: SDP/2.0/UDP host:port;branch=...;rport;...
    String[] f = vias[0].split(";");
    for(int a=0;a<f.length;a++) {
      if (f[a].startsWith("branch=")) {
        return f[a].substring(7);
      }
    }
    return null;
  }

  /**
   * Determines if a SIP message is on hold.
   */
  protected boolean ishold(String[] msg) {
    //does msg contain "a=sendonly"?
    for (int a = 0; a < msg.length; a++) {
      if (msg[a].equalsIgnoreCase("a=sendonly")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the Via: list in a SIP message as an array.
   */
  protected String[] getvialist(String[] msg) {
    ArrayList<String> vialist = new ArrayList<String>();
    for (int a = 0; a < msg.length; a++) {
      String ln = msg[a];
      if (ln.regionMatches(true, 0, "Via:", 0, 4)) {
        vialist.add(ln);
        continue;
      }
      if (ln.regionMatches(true, 0, "v:", 0, 2)) {
        vialist.add(ln);
        continue;
      }
    }
    return vialist.toArray(JF.StringArrayType);
  }

  /**
   * Returns the Record-Route: list in a SIP message as an array.
   */
  protected String[] getroutelist(String[] msg) {
    ArrayList<String> routelist = new ArrayList<String>();
    for (int a = 0; a < msg.length; a++) {
      String ln = msg[a];
      if (ln.regionMatches(true, 0, "Record-Route:", 0, 13)) {
        routelist.add("Route:" + ln.substring(13));
        continue;
      }
    }
    return routelist.toArray(JF.StringArrayType);
  }

  /**
   * Returns a random generated rinstance id.
   */
  protected String getrinstance() {
    if (rinstance != null) {
      return rinstance;
    }
    rinstance = String.format("%x%x", r.nextInt(), r.nextInt());
    return rinstance;
  }

  /**
   * Returns a random generated tuple id.
   */
  protected String gettupleid() {
    if (tupleid != null) {
      return tupleid;
    }
    tupleid = String.format("%08x", r.nextInt());
    return tupleid;
  }

  /**
   * Returns the URI part of a SIP message.
   */
  protected String geturi(String[] msg) {
    //cmd uri SIP/2.0\r\n
    int idx1 = msg[0].indexOf(' ');
    if (idx1 == -1) {
      return null;
    }
    int idx2 = msg[0].substring(idx1 + 1).indexOf(' ');
    if (idx2 == -1) {
      return null;
    }
    return msg[0].substring(idx1 + 1).substring(0, idx2);
  }

  /**
   * Returns a random generated tag for the To: or From: parts of a SIP message.
   * This function is used by replacetag() so it must resemble a To: or From:
   * field.
   */
  public static String generatetag() {
    Random r = new Random();
    return String.format("null<sip:null@null>;tag=%x%x", r.nextInt(), r.nextInt());
  }

  /**
   * Removes the 'tag' field from 'fields'.
   */
  public static String[] removetag(String[] fields) {
    boolean seperator = false;
    for (int i = 3; i < fields.length; i++) {
      if (!seperator) {
        if (fields[i].equals(":")) {
          seperator = true;
        }
        continue;
      }
      if (fields[i].startsWith("tag=")) {
        //remove fields[i]
        String[] newfields = new String[fields.length - 1];
        for (int j = 0; j < i; j++) {
          newfields[j] = fields[j];
        }
        for (int j = i + 1; j < fields.length; j++) {
          newfields[j - 1] = fields[j];
        }
        return newfields;
      }
    }
    return fields;  //no tag found
  }

  /**
   * Returns the 'tag' field from 'fields'.
   */
  public static String gettag(String[] fields) {
    boolean seperator = false;
    for (int i = 3; i < fields.length; i++) {
      if (!seperator) {
        if (fields[i].equals(":")) {
          seperator = true;
        }
        continue;
      }
      if (fields[i].startsWith("tag=")) {
        return fields[i].substring(4);
      }
    }
    return null;  //no tag found
  }

  /**
   * Returns a random callid for a SIP message (a unique id for each call, not
   * to be confused with caller id).
   */
  public String getcallid() {
    return String.format("%x%x", r.nextInt(), System.currentTimeMillis());
  }

  /**
   * Returns current time in seconds.
   */
  protected long getNow() {
    return System.currentTimeMillis() / 1000;
  }

  /**
   * Returns a random nonce variable used in SIP authorization.
   */
  protected String getnonce() {
    return String.format("%x%x%x%x", r.nextInt(), r.nextInt(), System.currentTimeMillis(), r.nextInt());
  }

  /**
   * Returns string name of codec based on payload id (except dynamic ids 96-127).
   */
  private static String getCodecName(int id) {
    switch (id) {
      case 0:
        return "PCMU";
      case 3:
        return "GSM";
      case 8:
        return "PCMA";
      case 9:
        return "G722";
      case 26:
        return "JPEG";
      case 18:
        return "G729";
      case 34:
        return "H263";
    }
    return "?";
  }

  /**
   * Parses the SDP content.
   *
   * @param msg = SDP text
   */
  public SDP getSDP(String[] msg) {
    return SDP.getSDP(msg, log);
  }

  /**
   * Determines if codecs[] contains codec.
   * NOTE:This checks the name field, not the id which could by dynamic.
   */
  public static boolean hasCodec(Codec[] codecs, Codec codec) {
    for (int a = 0; a < codecs.length; a++) {
      if (codecs[a].name.equals(codec.name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a codec to a list of codecs.
   */
  public static Codec[] addCodec(Codec[] codecs, Codec codec) {
    Codec[] newCodecs = new Codec[codecs.length + 1];
    for (int a = 0; a < codecs.length; a++) {
      newCodecs[a] = codecs[a];
    }
    newCodecs[codecs.length] = codec;
    return newCodecs;
  }

  /**
   * Removes a codec from a list of codecs.
   */
  public static Codec[] delCodec(Codec[] codecs, Codec codec) {
    if (!hasCodec(codecs, codec)) {
      return codecs;
    }
    Codec[] newCodecs = new Codec[codecs.length - 1];
    int pos = 0;
    for (int a = 0; a < codecs.length; a++) {
      if (codecs[a].name.equals(codec.name)) {
        continue;
      }
      newCodecs[pos++] = codecs[a];
    }
    return newCodecs;
  }

  /**
   * Returns a codec from a list of codecs. Comparison is done by name only. The
   * returned codec 'id' may be different than provided codec.
   */
  public static Codec getCodec(Codec[] codecs, Codec codec) {
    for (int a = 0; a < codecs.length; a++) {
      if (codecs[a].name.equals(codec.name)) {
        return codecs[a];
      }
    }
    return null;
  }

  /**
   * Returns the requested operation of a SIP message. (INVITE, BYE, etc.)
   */
  protected String getRequest(String[] msg) {
    int idx = msg[0].indexOf(" ");
    if (idx == -1) {
      return null;
    }
    return msg[0].substring(0, idx);
  }

  /** Returns URI in SIP msg. (INVITE "uri" SIP/2.0) */
  protected String getURI(String[] msg) {
    String[] parts = msg[0].split(" ");
    return parts[1];
  }

  /**
   * Returns the response number from a SIP reply message. (100, 200, 401, etc.)
   */
  protected int getResponseType(String[] msg) {
    if (msg[0].length() < 11) {
      return -1;  //bad msg
    }
    if (!msg[0].regionMatches(true, 0, "RTSP/1.0 ", 0, 9)) {
      return -1;  //not a response
    }    //RTSP/1.0 ### ...
    return Integer.valueOf(msg[0].substring(9, 12));
  }

  /**
   * Returns the cseq of a SIP message.
   */
  protected int getcseq(String[] msg) {
    String cseqstr = HTTP.getParameter(msg, "CSeq");
    if (cseqstr == null) {
      return -1;
    }
    String[] parts = cseqstr.split(" ");
    return Integer.valueOf(parts[0]);
  }

  /**
   * Returns the command at the end of the cseq header in a SIP message.
   */
  protected String getcseqcmd(String[] msg) {
    String cseqstr = HTTP.getParameter(msg, "CSeq");
    if (cseqstr == null) {
      return null;
    }
    String[] parts = cseqstr.split(" ");
    if (parts.length < 2) {
      return null;
    }
    return parts[1];
  }

  /**
   * Generates a response to a SIP authorization challenge.
   */
  protected String getResponse(String user, String pass, String realm, String cmd, String uri, String nonce, String qop, String nc, String cnonce) {
    MD5 md5 = new MD5();
    String H1 = user + ":" + realm + ":" + pass;
    md5.init();
    md5.add(H1.getBytes(), 0, H1.length());
    H1 = new String(md5.byte2char(md5.done()));
    String H2 = cmd + ":" + uri;
    md5.init();
    md5.add(H2.getBytes(), 0, H2.length());
    H2 = new String(md5.byte2char(md5.done()));
    String H3 = H1 + ":" + nonce + ":";
    if ((qop != null) && (qop.length() > 0)) {
      H3 += nc + ":" + cnonce + ":" + qop + ":";
    }
    H3 += H2;
    md5.init();
    md5.add(H3.getBytes(), 0, H3.length());
    return new String(md5.byte2char(md5.done()));
  }

  /** Split an Authenticate line into parts. */
  private String[] split(String in, char delimit) {
    ArrayList<String> strs = new ArrayList<String>();
    boolean inquote = false;
    char[] ca = in.toCharArray();
    int p1 = 0, p2 = 0;
    for(int a=0;a<ca.length;a++) {
      char ch = ca[a];
      if (ch == delimit && !inquote) {
        strs.add(in.substring(p1,p2).trim());
        p2++;
        p1 = p2;
        continue;
      } else if (ch == '\"') {
        inquote = !inquote;
      }
      p2++;
    }
    if (p2 > p1) {
      strs.add(in.substring(p1, p2).trim());
    }
/*
    System.out.println("auth=" + in);
    for(int a=0;a<strs.size();a++) {
      System.out.println("str[]=" + strs.get(a));
    }
*/
    return strs.toArray(new String[strs.size()]);
  }

  /**
   * Generates a complete header response to a SIP authorization challenge.
   */
  protected String getAuthResponse(RTSPSession sess, String user, String pass, String remote, String cmd, String header) {
    //request = ' Digest algorithm=MD5, realm="asterisk", nonce="value", etc.'
    String request = sess.authstr;
    if (!request.regionMatches(true, 0, "Digest ", 0, 7)) {
      JFLog.log(log, "err:no digest");
      return null;
    }
    String[] tags = SIP.convertParameters(request.substring(7), ',');
    String algo, nonce = null, qop = null, cnonce = null, nc = null, stale = null;
    String realm = null;
    algo = HTTP.getParameter(tags, "algorithm");
    if (algo != null) {
      if (!algo.equalsIgnoreCase("MD5")) {
        JFLog.log(log, "err:only MD5 auth supported");
        return null;
      }  //unsupported auth type
    }
    realm = HTTP.getParameter(tags, "realm");
    nonce = HTTP.getParameter(tags, "nonce");
    qop = HTTP.getParameter(tags, "qop");  //auth or auth-int
    stale = HTTP.getParameter(tags, "stale");  //true|false ???
    if (nonce == null) {
      JFLog.log(log, "err:no nonce");
      return null;
    }  //no nonce found
    if (realm == null) {
      JFLog.log(log, "err:no realm");
      return null;
    }  //no realm found
    if (qop != null) {
      String[] qops = qop.split(",");  //server could provide multiple options
      qop = null;
      for (int a = 0; a < qops.length; a++) {
        if (qops[a].trim().equals("auth")) {
          qop = "auth";
          break;
        }
      }
      if (qop != null) {
        //generate cnonce and nc
        cnonce = getnonce();
        if (sess.nonce != null && sess.nonce.equals(nonce)) {
          sess.nonceCount++;
        } else {
          sess.nonceCount = 1;
        }
        nc = String.format("%08x", sess.nonceCount);
      }
    }
    sess.nonce = nonce;
    String response = getResponse(user, pass, realm, cmd, sess.uri, nonce, qop, nc, cnonce);
    StringBuilder ret = new StringBuilder();
    ret.append(header);
    ret.append(":");
    ret.append(" Digest username=\"" + user + "\", realm=\"" + realm + "\"" + ", nonce=\"" + nonce + "\"" + ", uri=\"" + sess.uri + "\"");
    if (cnonce != null) {
      ret.append(", cnonce=\"" + cnonce + "\"");
    }
    //NOTE:Do NOT quote qop or nc
    if (qop != null) {
      ret.append(", nc=" + nc);
      ret.append(", qop=" + qop);
    }
    ret.append(", response=\"" + response + "\"");
    if (algo != null) {
      ret.append(", algorithm=MD5");
    }
    ret.append("\r\n");
    return ret.toString();
  }

  /**
   * Returns the remote RTP host in a SIP/SDP packet.
   */
  protected String getremotertphost(String[] msg) {
    String c = HTTP.getParameter(msg, "c");
    if (c == null) {
      return null;
    }
    int idx = c.indexOf("IP4 ");
    if (idx == -1) {
      return null;
    }
    return c.substring(idx + 4);
  }

  /**
   * Returns the remote RTP port in a SIP/SDP packet.
   */
  protected int getremote_audio_rtp_port(String[] msg) {
    // m=audio PORT RTP/AVP ...
    String ms[] = HTTP.getParameters(msg, "m");
    for(String m : ms) {
      if (!m.startsWith("audio")) continue;
      String[] p = m.split(" ");
      return Integer.valueOf(p[1]);
    }
    return -1;
  }

  /**
   * Returns the remote Video RTP port in a SIP/SDP packet.
   */
  protected int getremote_video_rtp_port(String[] msg) {
    // m=video PORT RTP/AVP ...
    // m=audio PORT RTP/AVP ...
    String ms[] = HTTP.getParameters(msg, "m");
    for(String m : ms) {
      if (!m.startsWith("video")) continue;
      String[] p = m.split(" ");
      return Integer.valueOf(p[1]);
    }
    return -1;
  }

  /**
   * Returns the 'o' counts in a SIP/SDP packet. idx can be 1 or 2.
   */
  protected long geto(String[] msg, int idx) {
    //o=blah o1 o2 ...
    String o = HTTP.getParameter(msg, "o");
    if (o == null) {
      return 0;
    }
    String[] os = o.split(" ");
    return Long.valueOf(os[idx]);
  }

  public abstract String getlocalRTPhost(RTSPSession sess);

  /**
   * Builds SDP packet. (RFC 2327)
   */
  public String[] buildsdp(RTSPSession sess, CallDetails.SideDetails cdsd) {
    //build SDP content
    SDP sdp = cdsd.sdp;
    if (sdp.ip == null) {
      sdp.ip = getlocalRTPhost(sess);
    }
    return sdp.build(sess.localhost);
  }

  private static HashMap<String, String> dnsCache = new HashMap<String, String>();

  /**
   * Resolve hostname to IP address. Keeps a cache to improve performance.
   */
  public String resolve(String host) {
    //uses a small DNS cache
    //TODO : age and delete old entries (SIP servers should always have static IPs so this is not critical)
    String ip = dnsCache.get(host);
    if (ip != null) {
      return ip;
    }
    try {
      ip = InetAddress.getByName(host).getHostAddress();
    } catch (Exception e) {
      JFLog.log(log, e);
      return null;
    }
    JFLog.log(log, "dns:" + host + "=" + ip);
    dnsCache.put(host, ip);
    return ip;
  }
  private final int mtu = 1460;  //max size of packet

  /**
   * This thread handles reading incoming SIP packets and dispatches them thru
   * RTSPInterface.
   */
  private class WorkerReader extends Thread {
    public void run() {
      setName("RTSP.WorkerReader");
      if (debug) JFLog.log(log, "RTSP.Worker:start:local=" + localport);
      while (active) {
        if (transport.error()) {
          JFLog.log(log, "RTSP:Transport:Error:server=" + server + ":localport=" + localport);
          active = false;
          break;
        }
        Packet pack = null;
        try {
          pack = pool.alloc();
          if (pack == null) {
            JFLog.log(log, "RTSP:PacketPool full");
            JF.sleep(1000);
            continue;
          }
          if (debug) JFLog.log(log, "RTSP:receiving:...");
          if (!transport.receive(pack)) {
            pool.free(pack);
            continue;
          }
          if (debug) JFLog.log(log, "RTSP:received:host=" + pack.host);
          if (pack.length <= 4) {
            pool.free(pack);
            continue;  //keep alive
          }
          if (server) {
            //RTSPServer
            worker_packet.add(pack);
          } else {
            //RTSPClient
            if (debug) JFLog.log(log, "RTSP:packet recv:host=" + pack.host);
            String[] msg = new String(pack.data, 0, pack.length).split("\r\n", -1);
            iface.onPacket(RTSP.this, msg, pack.host, pack.port);
            if (debug) JFLog.log(log, "RTSP:packet free:host=" + pack.host);
            pool.free(pack);
            if (debug) JFLog.log(log, "RTSP:packet done:host=" + pack.host);
          }
        } catch (Exception e) {
          if (debug) JFLog.log(log, e);
          if (!server && pack != null) {
            pool.free(pack);
          }
        }
      }
      if (debug) JFLog.log(log, "RTSP.Worker:stop:local=" + localport);
    }
  }

  /**
   * This thread dispatches SIP packets in a separate thread for server mode.
   */
  private class WorkerPacket extends Thread {

    private Object queueLock = new Object();
    private ArrayList<Packet> queue = new ArrayList<>();

    public void run() {
      setName("RTSP.WorkerPacket");
      while (active) {
        synchronized (queueLock) {
          if (queue.size() > 0) {
            Packet packet = queue.remove(0);
            String[] msg = new String(packet.data, 0, packet.length).split("\r\n", -1);

            iface.onPacket(RTSP.this, msg, packet.host, packet.port);
            pool.free(packet);
          } else {
            try {queueLock.wait();} catch (Exception e) {}
          }
        }
      }
    }

    public void add(Packet packet) {
      synchronized (queueLock) {
        queue.add(packet);
        queueLock.notify();
      }
    }

    public void cancel() {
      active = false;
      synchronized (queueLock) {
        queueLock.notify();
      }
    }
  }

  public void onConnect(String host, int port) {
    iface.onConnect(this, host, port);
  }

  public void onDisconnect(String host, int port) {
    iface.onDisconnect(this, host, port);
  }

  public int getPacketPoolSize() {
    return pool.count();
  }
}

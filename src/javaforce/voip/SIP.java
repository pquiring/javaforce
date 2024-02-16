package javaforce.voip;

import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Base class for SIP communications (Session Initiation Protocol).
 * Opens the UDP port and passes any received packets thru the SIPInterface.
 * Direct Known subclasses : SIPClient, SIPServer.
 * RFC 3261 (2543) - SIP
 * See also:
 * http://www.iana.org/assignments/sip-parameters/sip-parameters.xhtml#sip-parameters-2
 */

public abstract class SIP implements TransportInterface {
  private Worker worker;
  private SIPInterface iface;
  private boolean active = true;
  private String rinstance;
  private String tupleid;
  private Random r = new Random();
  private boolean server;
  private static boolean dns_system = true;
  private static int dns_transport = 0;
  private static String dns_server = "8.8.8.8";
  protected Transport transport;
  protected static String useragent = "JavaForce/" + JF.getVersion();

  /**
   * Opens the transport and sets the SIPInterface callback.
   */
  protected boolean init(String localhost, int localport, SIPInterface iface, boolean server, TransportType type) throws Exception {
    JFLog.log("SIP:local=" + localhost + ":" + localport);
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
    worker = new Worker();
    worker.start();
    return true;
  }

  /**
   * Closes the UDP port and frees resources.
   */
  protected void uninit() {
    if (transport == null) {
      return;
    }
    active = false;
    transport.close();
    try {
      worker.join();
    } catch (Exception e) {
    }
    transport = null;
    worker = null;
  }

  /**
   * Sends a packet out on the UDP port.
   */
  protected boolean send(InetAddress remote, int remoteport, String datastr) {
    byte[] data = datastr.getBytes();
    return transport.send(data, 0, data.length, remote, remoteport);
  }

  /**
   * Splits a To: or From: field in a SIP message into parts.
   */
  public static String[] split(String x) {
    //x = "display name" <sip:user@host ;...   >  ;...
    //return:    [0]          [1]  [2] [flgs1][:][flgs2]
    if (x == null) {
      return new String[] {"null", "null", "null"};
    }
    ArrayList<String> parts = new ArrayList<String>();
    int i1, i2;
    String x1, x2;
    i1 = x.indexOf('<');
    if (i1 == -1) {
      parts.add("");
      x1 = x;
      x2 = "";
    } else {
      if (i1 == 0) {
        parts.add("Unknown Name");
      } else {
        parts.add(x.substring(0, i1).trim().replaceAll("\"", ""));
      }
      i1++;
      i2 = x.substring(i1).indexOf('>');
      if (i2 == -1) {
        return null;
      }
      x1 = x.substring(i1, i1 + i2);
      x2 = x.substring(i1 + i2 + 1).trim();
    }
    i1 = x1.indexOf(':');
    if (i1 == -1) {
      return null;
    }
    x1 = x1.substring(i1 + 1);  //remove sip:
    i1 = x1.indexOf('@');
    if (i1 == -1) {
      parts.add("");  //no user
    } else {
      parts.add(x1.substring(0, i1).trim());  //userid
      x1 = x1.substring(i1 + 1).trim();
    }
    if ((x1.length() > 0) && (x1.charAt(0) == ';')) {
      x1 = x1.substring(1);
    }
    do {
      i1 = x1.indexOf(';');
      if (i1 == -1) {
        x1 = x1.trim();
        if (x1.length() > 0) {
          parts.add(x1);
        }
        break;
      }
      parts.add(x1.substring(0, i1).trim());
      x1 = x1.substring(i1 + 1).trim();
    } while (true);
    if (parts.size() == 2) {
      parts.add("");  //no host ???
    }
    parts.add(":");  //this seperates fields outside of <>
    if ((x2.length() > 0) && (x2.charAt(0) == ';')) {
      x2 = x2.substring(1);
    }
    do {
      i1 = x2.indexOf(';');
      if (i1 == -1) {
        x2 = x2.trim();
        if (x2.length() > 0) {
          parts.add(x2);
        }
        break;
      }
      parts.add(x2.substring(0, i1).trim());
      x2 = x2.substring(i1 + 1).trim();
    } while (true);
    String[] ret = new String[parts.size()];
    for (int a = 0; a < parts.size(); a++) {
      ret[a] = parts.get(a);
    }
    return ret;
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
   * Replaces the 'tag' field from 'newfield' into 'fields'.
   */
  public static String[] replacetag(String[] fields, String newfield) {
    //x = "display name" <sip:user@host;tag=...>;tag=...
    //           [0]          [1]  [2]  [...] [:][...]
    if (newfield == null) {
      return fields;
    }
    String[] newfields = split(newfield);
    int oldtagidx = -1;
    boolean seperator = false;
    for (int i = 3; i < fields.length; i++) {
      if (!seperator) {
        if (fields[i].equals(":")) {
          seperator = true;
        }
        continue;
      }
      if (fields[i].startsWith("tag=")) {
        oldtagidx = i;
        break;
      }
    }
    seperator = false;
    for (int i = 3; i < newfields.length; i++) {
      if (!seperator) {
        if (newfields[i].equals(":")) {
          seperator = true;
        }
        continue;
      }
      if (newfields[i].startsWith("tag=")) {
        if (oldtagidx != -1) {
          fields[oldtagidx] = newfields[i];
          return fields;
        } else {
          //need to add an element to fields and append newfields[i]
          String[] retfields = new String[fields.length + 1];
          for (int j = 0; j < fields.length; j++) {
            retfields[j] = fields[j];
          }
          retfields[fields.length] = newfields[i];
          return retfields;
        }
      }
    }
    return fields;
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
  public static String getCodecName(int id) {
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

  public static SDP getSDP(String[] msg) {
    return SDP.getSDP(msg, 0);
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
    if (!msg[0].regionMatches(true, 0, "SIP/2.0 ", 0, 8)) {
      return -1;  //not a response
    }    //SIP/2.0 ### ...
    return Integer.valueOf(msg[0].substring(8, 11));
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
  protected String getAuthResponse(CallDetails cd, String user, String pass, String remote, String cmd, String header) {
    //request = 'Digest algorithm=MD5, realm="asterisk", nonce="value", etc.'
    String request = cd.authstr;
    if (!request.regionMatches(true, 0, "Digest ", 0, 7)) {
      JFLog.log("err:no digest");
      return null;
    }
    String[] tags = convertParameters(request.substring(7), ',');
    String auth, nonce = null, qop = null, cnonce = null, nc = null,stale = null;
    String realm = null;
    auth = HTTP.getParameter(tags, "algorithm");
    if (auth != null) {
      if (!auth.equalsIgnoreCase("MD5")) {
        JFLog.log("err:only MD5 auth supported:auth=" + auth);
        return null;
      }  //unsupported auth type
    }
    realm = HTTP.getParameter(tags, "realm");
    nonce = HTTP.getParameter(tags, "nonce");
    qop = HTTP.getParameter(tags, "qop");  //auth or auth-int
    stale = HTTP.getParameter(tags, "stale");  //true|false ???
    if (nonce == null) {
      JFLog.log("err:no nonce");
      return null;
    }  //no nonce found
    if (realm == null) {
      JFLog.log("err:no realm");
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
        if (cd.nonce != null && cd.nonce.equals(nonce)) {
          cd.nonceCount++;
        } else {
          cd.nonceCount = 1;
        }
        nc = String.format("%08x", cd.nonceCount);
      }
    }
    cd.nonce = nonce;
    String response = getResponse(user, pass, realm, cmd, "sip:" + remote, nonce, qop, nc, cnonce);
    StringBuilder ret = new StringBuilder();
    ret.append(header);
    ret.append(" Digest username=\"" + user + "\", realm=\"" + realm + "\", uri=\"sip:" + remote + "\", nonce=\"" + nonce + "\"");
    if (cnonce != null) {
      ret.append(", cnonce=\"" + cnonce + "\"");
    }
    //NOTE:Do NOT quote qop or nc
    if (qop != null) {
      ret.append(", nc=" + nc);
      ret.append(", qop=" + qop);
    }
    ret.append(", response=\"" + response + "\"");
    ret.append(", algorithm=MD5\r\n");
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
   * Returns "expires" field from SIP headers.
   */
  public int getexpires(String[] msg) {
    //check Expires field
    String expires = HTTP.getParameter(msg, "Expires");
    if (expires != null) {
      return JF.atoi(expires);
    }
    //check Contact field
    String contact = HTTP.getParameter(msg, "Contact");
    if (contact == null) {
      contact = HTTP.getParameter(msg, "c");
    }
    if (contact == null) {
      return -1;
    }
    int idx = contact.indexOf('>');
    if (idx == -1) return -1;
    String[] tags = convertParameters(contact.substring(idx+1), ';');
    expires = HTTP.getParameter(tags, "expires");
    if (expires == null) {
      return -1;
    }
    return JF.atoi(expires);
  }

  public abstract String getlocalRTPhost(CallDetails cd);

  /**
   * Builds SDP packet. (RFC 2327)
   */
  public String[] buildsdp(CallDetails cd, CallDetails.SideDetails cdsd) {
    //build SDP content
    SDP sdp = cdsd.sdp;
    if (sdp.ip == null) {
      sdp.ip = getlocalRTPhost(cd);
    }
    return sdp.build(cd.localhost);
  }

  /** Set resolver to system DNS client. */
  public static void setResolver() {
    dns_system = true;
    dns_transport = DNS.TRANSPORT_UDP;
    dns_server = "8.8.8.8";
  }

  /** Set resolver to custom DNS Server.
   * @param transport = javaforce.DNS.TRANSPORT_...
   * @param server = DNS Server host/IP.
   */
  public static void setResolver(int transport, String server) {
    dns_system = false;
    dns_transport = transport;
    dns_server = server;
  }

  public void setremoteport(int port) {}

  private static Object dnsCacheLock = new Object();
  private static HashMap<String, String> dnsCache = new HashMap<String, String>();

  private static boolean contains(String[] list, String find) {
    for(String item : list) {
      if (item.equals(find)) return true;
    }
    return false;
  }

  private String resolve(DNS dns, String host) throws Exception {
    //try A records
    String[] reply = dns.resolve(DNS.TYPE_A, host);
    if (reply == null) {
      //try NAPTR records
      reply = dns.resolve(DNS.TYPE_NAPTR, host);
      //find _sip._udp service and change host to that
      if (reply != null) {
        String service = null;
        switch (transport.getName()) {
          case "UDP": service = "_sip._udp"; break;
          case "TCP": service = "_sip._tcp"; break;
          case "TLS": service = "_sips._tcp"; break;
        }
        for(String newhost : reply) {
          if (newhost.startsWith(service)) {
            if (!newhost.equals(host)) {
              JFLog.log("DNS.resolve:using NAPTR:" + newhost);
              return resolve(dns, newhost);
            }
          }
        }
        throw new Exception("SIP.resolve:no valid NAPTR record found:" + host);
      }
    }
    if (reply == null) {
      //try SRV records
      reply = dns.resolve(DNS.TYPE_SRV, host);
      if (reply != null) {
        String newhost = reply[0];
        int idx = newhost.indexOf(':');
        if (idx != -1) {
          String port = newhost.substring(idx + 1);
          setremoteport(JF.atoi(port));
          newhost = newhost.substring(0, idx);
        }
        if (!newhost.equals(host)) {
          JFLog.log("SIP.resolve:using SRV:" + newhost);
          return resolve(dns, newhost);
        }
      }
    }
    if (reply == null) {
      //try NS records
      reply = dns.resolve(DNS.TYPE_NS, host);
      if (reply == null || contains(reply, host)) {
        throw new Exception("SIP.resolve:host not found:" + host);
      }
      JFLog.log("DNS.resolve:using NS:" + reply[0]);
      dns = new DNS(dns_transport, reply[0]);
      return resolve(dns, host);
    }
    if (reply == null) {
      //try SOA records
      reply = dns.resolve(DNS.TYPE_SOA, host);
      if (reply == null || contains(reply, host)) {
        throw new Exception("SIP.resolve:host not found:" + host);
      }
      JFLog.log("DNS.resolve:using SOA:" + reply[0]);
      dns = new DNS(dns_transport, reply[0]);
      return resolve(dns, host);
    }
    String ip = reply[0];
    int idx = ip.indexOf(':');
    if (idx != -1) {
      String port = ip.substring(idx + 1);
      //TODO : use port from SRV records
      JFLog.log("SIP.resolve:discarding port:" + port);
      ip = ip.substring(0, idx);
    }
    return ip;
  }

  /**
   * Resolve hostname to IP address. Keeps a cache to improve performance.
   */
  public String resolve(String host) {
    //uses a small DNS cache
    //TODO : age and delete old entries (SIP servers should always have static IPs so this is not critical)
    String ip = null;
    synchronized (dnsCacheLock) {
      ip = dnsCache.get(host);
    }
    if (ip != null) {
      return ip;
    }
    if (dns_system || javaforce.net.IP4.isIP(host) || javaforce.net.IP6.isIP(host)) {
      try {
        ip = InetAddress.getByName(host).getHostAddress();
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    } else {
      try {
        DNS dns = new DNS(dns_transport, dns_server);
        ip = resolve(dns, host);
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }
    JFLog.log("dns:" + host + "=" + ip);
    synchronized (dnsCacheLock) {
      dnsCache.put(host, ip);
    }
    return ip;
  }
  private static final int mtu = 1460;  //max size of packet

  /**
   * This thread handles reading incoming SIP packets and dispatches them thru
   * SIPInterface.
   */
  private class Worker extends Thread {

    public void run() {
      while (active) {
        try {
          byte[] data = new byte[mtu];
          Packet pack = new Packet();
          pack.data = data;
          if (!transport.receive(pack)) continue;
          if (pack.length <= 4) {
            continue;  //keep alive
          }
          String[] msg = new String(pack.data, 0, pack.length).replaceAll("\r", "").split("\n", -1);
          if (server) {
            WorkerPacket wp = new WorkerPacket(msg, pack.host, pack.port);
            wp.start();
          } else {
            iface.packet(msg, pack.host, pack.port);
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }

  /**
   * This thread dispatches SIP packets in a separate thread for server mode.
   */
  private class WorkerPacket extends Thread {

    String[] x1;
    String x2;
    int x3;

    public WorkerPacket(String[] x1, String x2, int x3) {
      this.x1 = x1;
      this.x2 = x2;
      this.x3 = x3;
    }

    public void run() {
      iface.packet(x1, x2, x3);
    }
  }

  /** Converts xml style parameters to HTTP style parameters while removing quotes around value.
   * example in: key1="value1" key2="value2" ...
   * example out: [key1: value1] [key2: value2]
   */
  public static String[] convertParameters(String ln, char delimit) {
    String[] params = JF.splitQuoted(ln, delimit);
    for(int idx = 0;idx<params.length;idx++) {
      String param = params[idx];
      int eq = param.indexOf('=');
      if (eq == -1) continue;
      String key = param.substring(0, eq).trim();
      String value = param.substring(eq+1).replaceAll("\"", "").trim();
      params[idx] = key + ": " + value;
    }
    return params;
  }

  public void onConnect(String host, int port) {}
  public void onDisconnect(String host, int port) {}
}

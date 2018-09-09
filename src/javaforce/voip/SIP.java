package javaforce.voip;

import java.net.*;
import java.util.*;
import javaforce.*;

/**
 * Base class for SIP communications. Opens the UDP port and passes any received
 * packets thru the SIPInterface.
 * Direct Known subclasses : SIPClient, SIPServer.
 * RFC 3261 (2543) - SIP
 * See also:
 * http://www.iana.org/assignments/sip-parameters/sip-parameters.xhtml#sip-parameters-2
 */

public abstract class SIP {

  public static class Packet {
    public byte data[];
    public int length;
    public int port;
    public String host;
  }

  public enum Transport {UDP, TCP, TLS};

  private Worker worker;
  private SIPInterface iface;
  private boolean active = true;
  private String rinstance;
  private String tupleid;
  private Random r = new Random();
  private boolean server;
  protected String protocol = "SIP/2.0";  //SIP/2.0 or RTSP/1.0
  protected SIPTransport transport;
  protected static String useragent = "JavaForce/" + JF.getVersion();

  /**
   * Opens the transport and sets the SIPInterface callback.
   */
  protected boolean init(int port, SIPInterface iface, boolean server, Transport type) throws Exception {
    rinstance = null;
    this.iface = iface;
    this.server = server;
    switch (type) {
      case UDP:
        transport = new SIPUDPTransport();
        break;
      case TCP:
        if (server)
          transport = new SIPTCPTransportServer();
        else
          transport = new SIPTCPTransportClient();
        break;
      case TLS:
        if (server)
          transport = new SIPTLSTransportServer();
        else
          transport = new SIPTLSTransportClient();
        break;
    }
    if (!transport.open(port)) return false;
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
    byte data[] = datastr.getBytes();
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
    String ret[] = new String[parts.size()];
    for (int a = 0; a < parts.size(); a++) {
      ret[a] = parts.get(a);
    }
    return ret;
  }

  /**
   * Joins a To: or From: field after it was split into parts.
   */
  public static String join(String x[]) {
    //x = "display name" <sip:user@host ;...  > ;...
    //return:    [0]          [1]  [2]  [...][:][...]
    if (x == null) {
      return "\"null\"<sip:null@null>";
    }
    StringBuffer buf = new StringBuffer();
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
  public static String getFlag2(String fields[], String flg) {
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
  public static String[] setFlag2(String fields[], String flg, String value) {
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
    String newfields[] = new String[fields.length + 1];
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
  protected String getbranch(String msg[]) {
    String vias[] = getvialist(msg);
    if (vias == null || vias.length == 0) return null;
    //Via: SDP/2.0/UDP host:port;branch=...;rport;...
    String f[] = vias[0].split(";");
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
  protected boolean ishold(String msg[]) {
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
  protected String[] getvialist(String msg[]) {
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
    return vialist.toArray(new String[0]);
  }

  /**
   * Returns the Record-Route: list in a SIP message as an array.
   */
  protected String[] getroutelist(String msg[]) {
    ArrayList<String> routelist = new ArrayList<String>();
    for (int a = 0; a < msg.length; a++) {
      String ln = msg[a];
      if (ln.regionMatches(true, 0, "Record-Route:", 0, 13)) {
        routelist.add("Route:" + ln.substring(13));
        continue;
      }
    }
    return routelist.toArray(new String[0]);
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
  protected String geturi(String msg[]) {
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
  public static String[] replacetag(String fields[], String newfield) {
    //x = "display name" <sip:user@host;tag=...>;tag=...
    //           [0]          [1]  [2]  [...] [:][...]
    if (newfield == null) {
      return fields;
    }
    String newfields[] = split(newfield);
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
          String retfields[] = new String[fields.length + 1];
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
  public static String[] removetag(String fields[]) {
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
        String newfields[] = new String[fields.length - 1];
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
  public static String gettag(String fields[]) {
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
   */
  public static SDP getSDP(String msg[]) {
    String type = getHeader("Content-Type:", msg);
    if (type == null) type = getHeader("c:", msg);  //short form
    if (type == null || type.indexOf("application/sdp") == -1) return null;
    SDP sdp = new SDP();
    SDP.Stream stream = null;
    int idx;
    int start = -1;
    for(int a=0;a<msg.length;a++) {
      if (msg[a].length() == 0) {start = a+1; break;}
    }
    if (start == -1) {
      JFLog.log("SIP.getSDP() : No SDP found");
      return null;
    }
    int acnt = 1;
    int vcnt = 1;
    for(int a=start;a<msg.length;a++) {
      String ln = msg[a];
      if (ln.startsWith("c=")) {
        //c=IN IP4 1.2.3.4
        idx = ln.indexOf("IP4 ");
        if (idx == -1) {JFLog.log("SIP.getSDP() : Unsupported c field:" + ln); continue;}
        String ip = ln.substring(idx+4);
        if (stream == null) {
          sdp.ip = ip;
        } else {
          stream.ip = ip;
        }
      } else if (ln.startsWith("m=")) {
        //m=audio <port> RTP/<profile> <codecs>
        if (stream != null) {
          if (stream.content == null) {
            switch (stream.type) {
              case audio: stream.content = "audio" + (acnt++); break;
              case video: stream.content = "video" + (vcnt++); break;
            }
          }
        }
        if (ln.startsWith("m=audio")) {
          stream = sdp.addStream(SDP.Type.audio);
        } else if (ln.startsWith("m=video")) {
          stream = sdp.addStream(SDP.Type.video);
        } else {
          JFLog.log("SIP.getSDP() : Unsupported m field:" + ln);
          stream = sdp.addStream(SDP.Type.other);
          continue;
        }
        //parse static codecs
        String f[] = ln.split(" ");
        String p[] = f[2].split("/");
        if (p[1].equals("AVP")) {
          stream.profile = SDP.Profile.AVP;
        } else if (p[1].equals("AVPF")) {
          stream.profile = SDP.Profile.AVPF;
        } else if (p[1].equals("SAVP")) {
          stream.profile = SDP.Profile.SAVP;
        } else if (p[1].equals("SAVPF")) {
          stream.profile = SDP.Profile.SAVPF;
        } else {
          stream.profile = SDP.Profile.UNKNOWN;
          JFLog.log("SIP.getSDP() : Unsupported profile:" + p[1]);
        }
        stream.port = JF.atoi(f[1]);
        for(int b=3;b<f.length;b++) {
          int id = JF.atoi(f[b]);
          if (id < 96) {
            stream.addCodec(new Codec(getCodecName(id), id));
          }
        }
      } else if (ln.startsWith("a=")) {
        if (ln.startsWith("a=rtpmap:")) {
          //a=rtpmap:<id> <name>/<bitrate>
          String f[] = ln.substring(9).split(" ");
          int id = JF.atoi(f[0]);
          String n[] = f[1].split("/");
          if (id >= 96) {
            stream.addCodec(new Codec(n[0], id));
          }
        }
        else if (ln.startsWith("a=sendrecv")) {
          if (stream != null) {
            stream.mode = SDP.Mode.sendrecv;
          }
        }
        else if (ln.startsWith("a=sendonly")) {
          if (stream != null) {
            stream.mode = SDP.Mode.sendonly;
          }
        }
        else if (ln.startsWith("a=recvonly")) {
          if (stream != null) {
            stream.mode = SDP.Mode.sendonly;
          }
        }
        else if (ln.startsWith("a=inactive")) {
          if (stream != null) {
            stream.mode = SDP.Mode.inactive;
          }
        }
        else if (ln.startsWith("a=content:")) {
          stream.content = ln.substring(10);
        }
        else if (ln.startsWith("a=candidate:")) {
          //            0 1 2   3          4          5     6   7     8     9          10    11
          //a=candidate:0 1 UDP 2128609535 10.1.1.100 60225 typ host
          //a=candidate:1 1 UDP 1692467199 x.x.x.x    60225 typ srflx raddr 10.1.1.100 rport 60225
          //a=candidate:0 2 UDP 2128609534 10.1.1.100 60226 typ host
          //a=candidate:1 2 UDP 1692467198 x.x.x.x    60226 typ srflx raddr 10.1.1.100 rport 60226
          String f[] = ln.substring(12).split(" ");
          if (stream != null && f.length >= 8 && f[0].equals("0") && f[1].equals("1")) {
            //override ip
            stream.ip = f[4];
          }
        }
        else if (ln.startsWith("a=ice-ufrag:")) {
          sdp.iceufrag = ln.substring(12);
        }
        else if (ln.startsWith("a=ice-pwd:")) {
          sdp.icepwd = ln.substring(10);
        }
        else if (ln.startsWith("a=fingerprint:sha-256 ")) {
          sdp.fingerprint = ln.substring(22);
        }
        else if (ln.startsWith("a=crypto:")) {
          //SRTP Keys (replaced by DTLS method)
          //a=crypto:1 AES_CM_128_HMAC_SHA1_80 inline:PS1uQCVeeCFCanVmcjkpPywjNWhcYD0mXXtxaVBR|2^20|1:32
          //         # crypto                         base64_key_salt                          life mki
          stream.keyExchange = SDP.KeyExchange.SDP;
          String f[] = ln.split(" ");
          if (!f[2].startsWith("inline:")) {
            JFLog.log("a=crypto:bad keys(1)");
            continue;
          }
          String base64 = f[2].substring(7);
          int pipe = base64.indexOf("|");
          if (pipe != -1) {
            base64 = base64.substring(0, pipe);
          }
          byte keys[] = javaforce.Base64.decode(base64.getBytes());
          if (keys == null || keys.length != 30) {
            JFLog.log("a=crypto:bad keys(2)");
            continue;
          }
          byte key[] = Arrays.copyOfRange(keys, 0, 16);
          byte salt[] = Arrays.copyOfRange(keys, 16, 16 + 14);
          stream.addKey(f[1], key, salt);
        }
      }
    }
    if ((stream != null) && (stream.content == null)) {
      switch (stream.type) {
        case audio: stream.content = "audio" + (acnt++); break;
        case video: stream.content = "video" + (vcnt++); break;
      }
    }
    return sdp;
  }

  /**
   * Determines if codecs[] contains codec.
   * NOTE:This checks the name field, not the id which could by dynamic.
   */
  public static boolean hasCodec(Codec codecs[], Codec codec) {
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
  public static Codec[] addCodec(Codec codecs[], Codec codec) {
    Codec newCodecs[] = new Codec[codecs.length + 1];
    for (int a = 0; a < codecs.length; a++) {
      newCodecs[a] = codecs[a];
    }
    newCodecs[codecs.length] = codec;
    return newCodecs;
  }

  /**
   * Removes a codec from a list of codecs.
   */
  public static Codec[] delCodec(Codec codecs[], Codec codec) {
    if (!hasCodec(codecs, codec)) {
      return codecs;
    }
    Codec newCodecs[] = new Codec[codecs.length - 1];
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
  public static Codec getCodec(Codec codecs[], Codec codec) {
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
  protected String getRequest(String msg[]) {
    int idx = msg[0].indexOf(" ");
    if (idx == -1) {
      return null;
    }
    return msg[0].substring(0, idx);
  }

  /** Returns URI in SIP msg. (INVITE "uri" SIP/2.0) */
  protected String getURI(String msg[]) {
    String parts[] = msg[0].split(" ");
    return parts[1];
  }

  /**
   * Returns the response number from a SIP reply message. (100, 200, 401, etc.)
   */
  protected int getResponseType(String msg[]) {
    if (msg[0].length() < 11) {
      return -1;  //bad msg
    }
    if (!msg[0].regionMatches(true, 0, protocol + " ", 0, 8)) {
      return -1;  //not a response
    }    //SIP/2.0 ### ...
    return Integer.valueOf(msg[0].substring(8, 11));
  }

  /**
   * Returns a specific header (field) from a SIP message.
   */
  public static String getHeader(String header, String msg[]) {
    int sl = header.length();
    for (int a = 0; a < msg.length; a++) {
      String ln = msg[a];
      if (ln.length() < sl) {
        continue;
      }
      if (ln.substring(0, sl).equalsIgnoreCase(header)) {
        return ln.substring(sl).trim().replaceAll("\"", "");
      }
    }
    return null;
  }

  /**
   * Returns a set of specific headers (fields) from a SIP message.
   */
  public static String[] getHeaders(String header, String msg[]) {
    ArrayList<String> lst = new ArrayList<String>();
    int sl = header.length();
    for (int a = 0; a < msg.length; a++) {
      String ln = msg[a];
      if (ln.length() < sl) {
        continue;
      }
      if (ln.substring(0, sl).equalsIgnoreCase(header)) {
        lst.add(ln.substring(sl).trim().replaceAll("\"", ""));
      }
    }
    return lst.toArray(new String[0]);
  }

  /**
   * Returns the cseq of a SIP message.
   */
  protected int getcseq(String msg[]) {
    String cseqstr = getHeader("CSeq:", msg);
    if (cseqstr == null) {
      return -1;
    }
    String parts[] = cseqstr.split(" ");
    return Integer.valueOf(parts[0]);
  }

  /**
   * Returns the command at the end of the cseq header in a SIP message.
   */
  protected String getcseqcmd(String msg[]) {
    String cseqstr = getHeader("CSeq:", msg);
    if (cseqstr == null) {
      return null;
    }
    String parts[] = cseqstr.split(" ");
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
    char ca[] = in.toCharArray();
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
    //request = ' Digest algorithm=MD5, realm="asterisk", nonce="value", etc.'
    String request = cd.authstr;
    if (!request.regionMatches(true, 0, "Digest ", 0, 7)) {
      JFLog.log("err:no digest");
      return null;
    }
    String tags[] = split(request.substring(7), ',');
    String auth, nonce = null, qop = null, cnonce = null, nc = null,stale = null;
    String realm = null;
    auth = getHeader("algorithm=", tags);
    if (auth != null) {
      if (!auth.equalsIgnoreCase("MD5")) {
        JFLog.log("err:only MD5 auth supported");
        return null;
      }  //unsupported auth type
    }
    realm = getHeader("realm=", tags);
    nonce = getHeader("nonce=", tags);
    qop = getHeader("qop=", tags);  //auth or auth-int
    stale = getHeader("stale=", tags);  //true|false ???
    if (nonce == null) {
      JFLog.log("err:no nonce");
      return null;
    }  //no nonce found
    if (realm == null) {
      JFLog.log("err:no realm");
      return null;
    }  //no realm found
    if (qop != null) {
      String qops[] = qop.split(",");  //server could provide multiple options
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
    StringBuffer ret = new StringBuffer();
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
  protected String getremotertphost(String msg[]) {
    String c = getHeader("c=", msg);
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
  protected int getremotertpport(String msg[]) {
    // m=audio PORT RTP/AVP ...
    String m = getHeader("m=audio ", msg);
    if (m == null) {
      return -1;
    }
    int idx = m.indexOf(' ');
    if (idx == -1) {
      return -1;
    }
    return Integer.valueOf(m.substring(0, idx));
  }

  /**
   * Returns the remote Video RTP port in a SIP/SDP packet.
   */
  protected int getremoteVrtpport(String msg[]) {
    // m=video PORT RTP/AVP ...
    String m = getHeader("m=video ", msg);
    if (m == null) {
      return -1;
    }
    int idx = m.indexOf(' ');
    if (idx == -1) {
      return -1;
    }
    return Integer.valueOf(m.substring(0, idx));
  }

  /**
   * Returns the 'o' counts in a SIP/SDP packet. idx can be 1 or 2.
   */
  protected long geto(String msg[], int idx) {
    //o=blah o1 o2 ...
    String o = getHeader("o=", msg);
    if (o == null) {
      return 0;
    }
    String os[] = o.split(" ");
    return Long.valueOf(os[idx]);
  }

  /**
   * Returns "expires" field from SIP headers.
   */
  public int getexpires(String msg[]) {
    //check Expires field
    String expires = getHeader("Expires:", msg);
    if (expires != null) {
      return JF.atoi(expires);
    }
    //check Contact field
    String contact = getHeader("Contact:", msg);
    if (contact == null) {
      contact = getHeader("c:", msg);
    }
    if (contact == null) {
      return -1;
    }
    String tags[] = split(contact);
    expires = getHeader("expires=", tags);
    if (expires == null) {
      return -1;
    }
    return JF.atoi(expires);
  }

  public abstract String getlocalRTPhost(CallDetails cd);

  /**
   * Builds SDP packet. (RFC 2327)
   */
  public void buildsdp(CallDetails cd, CallDetails.SideDetails cdsd) {
    //build SDP content
    SDP sdp = cdsd.sdp;
    String ip = sdp.ip;
    if (ip == null) {
      ip = getlocalRTPhost(cd);
    }
    StringBuffer content = new StringBuffer();
    content.append("v=0\r\n");
    content.append("o=- " + cdsd.o1 + " " + cdsd.o2 + " IN IP4 " + cd.localhost + "\r\n");
    content.append("s=" + useragent + "\r\n");
    content.append("c=IN IP4 " + ip + "\r\n");
    content.append("t=0 0\r\n");
    if (sdp.iceufrag != null) content.append("a=ice-ufrag:" + sdp.iceufrag + "\r\n");
    if (sdp.icepwd != null) content.append("a=ice-pwd:" + sdp.icepwd + "\r\n");
    if (sdp.fingerprint != null) content.append("a=fingerprint:sha-256 " + sdp.fingerprint + "\r\n");
    for(int a=0;a<sdp.streams.length;a++) {
      SDP.Stream stream = sdp.streams[a];
      if (stream.codecs.length == 0) continue;
      Codec rfc2833 = getCodec(stream.codecs, RTP.CODEC_RFC2833);
      content.append("m=" + stream.getType() + " " + stream.port + " RTP/" + stream.profile);
      for(int b=0;b<stream.codecs.length;b++) {
        content.append(" " + stream.codecs[b].id);
      }
      if (stream.type == SDP.Type.audio && rfc2833 == null) {
        rfc2833 = RTP.CODEC_RFC2833;
        content.append(" " + rfc2833.id);
      }
      content.append("\r\n");
      if (stream.keyExchange == SDP.KeyExchange.SDP && stream.keys != null) {
        for(int c=0;c<stream.keys.length;c++) {
          SDP.Key keys = stream.keys[c];
          byte key_salt[] = new byte[16 + 14];
          System.arraycopy(keys.key, 0, key_salt, 0, 16);
          System.arraycopy(keys.salt, 0, key_salt, 16, 14);
          String keystr = new String(javaforce.Base64.encode(key_salt));
                                               //keys          | lifetime     | mki:length
          String ln = keys.crypto + " inline:" + keystr; // + "|2^20";  // + "|1:32";
          content.append("a=crypto:" + (c+1) + " ");
          content.append(ln);
          content.append("\r\n");
        }
      }
      if (stream.content != null) {
        content.append("a=content:" + stream.content + "\r\n");
      }
      content.append("a=" + stream.getMode() + "\r\n");
      if (stream.ip != null) {
        content.append("c=IN IP4 " + stream.ip + "\r\n");
      }
      content.append("a=ptime:20\r\n");
      if (hasCodec(stream.codecs, RTP.CODEC_G711u)) {
        content.append("a=rtpmap:0 PCMU/8000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_G711a)) {
        content.append("a=rtpmap:8 PCMA/8000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_GSM)) {
        content.append("a=rtpmap:3 GSM/8000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_G722)) {
        content.append("a=rtpmap:9 G722/8000\r\n");  //NOTE:It's really 16000 but an error in RFC claims it as 8000
      }
      if (hasCodec(stream.codecs, RTP.CODEC_G729a)) {
        content.append("a=rtpmap:18 G729/8000\r\n");
        content.append("a=fmtp:18 annexb=no\r\n");
        content.append("a=silenceSupp:off - - - -\r\n");
      }
      if (stream.type == SDP.Type.audio) {
        content.append("a=rtpmap:" + rfc2833.id + " telephone-event/8000\r\n");
        content.append("a=fmtp:" + rfc2833.id + " 0-15\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_JPEG)) {
        content.append("a=rtpmap:26 JPEG/90000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_H263)) {
        content.append("a=rtpmap:34 H263/90000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_H263_1998)) {
        content.append("a=rtpmap:" + getCodec(stream.codecs, RTP.CODEC_H263_1998).id + " H263-1998/90000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_H263_2000)) {
        content.append("a=rtpmap:" + getCodec(stream.codecs, RTP.CODEC_H263_2000).id + " H263-2000/90000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_H264)) {
        content.append("a=rtpmap:" + getCodec(stream.codecs, RTP.CODEC_H264).id + " H264/90000\r\n");
      }
      if (hasCodec(stream.codecs, RTP.CODEC_VP8)) {
        content.append("a=rtpmap:" + getCodec(stream.codecs, RTP.CODEC_VP8).id + " VP8/90000\r\n");
      }
      if (stream.keyExchange == SDP.KeyExchange.DTLS) {
        content.append("a=rtcp-mux");  //http://tools.ietf.org/html/rfc5761
      }
    }
    cd.sdp = content.toString();
  }
  private static HashMap<String, String> dnsCache = new HashMap<String, String>();

  /**
   * Resolve hostname to IP address. Keeps a cache to improve performance.
   */
  public static String resolve(String host) {
    //uses a small DNS cache
    //TODO : age and delete old entries (SIP servers should always have static IPs so this is not critical)
    String ip = dnsCache.get(host);
    if (ip != null) {
      return ip;
    }
    try {
      ip = InetAddress.getByName(host).getHostAddress();
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
    JFLog.log("dns:" + host + "=" + ip);
    dnsCache.put(host, ip);
    return ip;
  }
  private final int mtu = 1460;  //max size of packet

  /**
   * This thread handles reading incoming SIP packets and dispatches them thru
   * SIPInterface.
   */
  private class Worker extends Thread {

    public void run() {
      while (active) {
        try {
          byte data[] = new byte[mtu];
          Packet pack = new Packet();
          pack.data = data;
          if (!transport.receive(pack)) continue;
          if (pack.length <= 4) {
            continue;  //keep alive
          }
          String msg[] = new String(data, 0, pack.length).replaceAll("\r", "").split("\n", -1);
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

    String x1[];
    String x2;
    int x3;

    public WorkerPacket(String x1[], String x2, int x3) {
      this.x1 = x1;
      this.x2 = x2;
      this.x3 = x3;
    }

    public void run() {
      iface.packet(x1, x2, x3);
    }
  }
}

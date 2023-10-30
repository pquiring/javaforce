package jfpbx.core;

/** WebRTC
 *
 * @author pquiring
 *
 * Created : Jan 4, 2014
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.voip.*;

public class WebRTC implements WebSocketHandler, SIPClientInterface {
  private static byte crt[], privateKey[];
  private static String fingerprintSHA256;

  public void init() {
    try {
      char password[] = "password".toCharArray();
      FileInputStream fis = new FileInputStream(Paths.etc + "jfpbx.key");
      KeyMgmt key = new KeyMgmt();
      key.open(fis, password);
      fis.close();
      crt = key.getCRT("jfpbx").getEncoded();
      fingerprintSHA256 = KeyMgmt.fingerprintSHA256(crt);
      ArrayList<byte[]> chain = new ArrayList<byte[]>();
      chain.add(crt);
      java.security.cert.Certificate root = key.getCRT("root");
      if (root != null) {
        chain.add(root.getEncoded());
      }
      privateKey = key.getKEY("jfpbx", password).getEncoded();
//      SRTPChannel.initDTLS(chain, privateKey, false);
      if (false) {
        //see bouncy castle DTLSClientTest
        RTP testRTP = new RTP();
        testRTP.init(null, 5556);
        testRTP.start();
        SDP sdp = new SDP();
        SDP.Stream stream = sdp.addStream(SDP.Type.audio);
        stream.addCodec(RTP.CODEC_G711u);
        stream.ip = "10.1.1.2";
        stream.port = -1;
        RTPChannel testCH = testRTP.createChannel(stream);
        testCH.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void doWebRTC1(WebRequest req, WebResponse res) throws Exception {
    String args[] = req.getQueryString().split("&");
    String user = "", room = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("user=")) user = args[a].substring(x);
      if (args[a].startsWith("room=")) room = args[a].substring(x);
    }
    RTC rtc = rooms.get(room);
    JFLog.log("room:" + room + ":rtc=" + rtc);
    if (rtc != null) {
      if (rtc.inuse) {
        StringBuilder html = new StringBuilder();
        html.append("Room is full");
        res.getOutputStream().write(html.toString().getBytes());
        return;
      } else {
        JFLog.log("Room has one other guest:" + room);
      }
    } else {
      JFLog.log("room is empty:" + room);
    }
    Random r = new Random();
    if (user.length() == 0) {
      user = "1010";  //Integer.toString(Math.abs(r.nextInt()));
    }
    if (room.length() == 0) {
      room = Integer.toString(Math.abs(r.nextInt()));
    }
    StringBuilder html = new StringBuilder();
    html.append("<html>");
    html.append("<head>");
    html.append("  <title>jfPBX</title>");
    html.append("  <link rel=stylesheet href='/static/webrtc.css' type='text/css'>");
    html.append("  <script src='/static/adapter.js'></script>");
    html.append("  <script type='text/javascript' src='/static/webrtc1.js'></script>");
    html.append("</head>");
    html.append("<body leftmargin=1 rightmargin=1 topmargin=1 bottommargin=1 vlink=ffffff link=ffffff alink=ffffff width=100% height=100%>");
    html.append("<div id='container' ondblclick='enterFullScreen()' style='width: 608px; height: 456px; left: 336px; top: 0px;'>");
    html.append("  <div id='card'>");
    html.append("    <div id='local'>");
    html.append("      <video id='localVideo' muted='true' autoplay='autoplay' style='opacity: 1;'></video>");
    html.append("    </div>");
    html.append("    <div id='remote'>");
    html.append("      <video id='remoteVideo' autoplay='autoplay'></video>");
    html.append("      <div id='mini'>");
    html.append("        <video id='miniVideo' muted='true' autoplay='autoplay'></video>");
    html.append("      </div>");
    html.append("    </div>");
    html.append("  </div>");
    html.append("</div>");
    html.append("<footer id='status'></footer>");
    html.append("<div id='infoDiv'></div>");
    html.append("<script>");
    html.append("  var errorMessages = [];");
    html.append("  var user = '" + user + "';");
    html.append("  var roomKey = '" + room + "';");
    html.append("  var roomLink = 'http://" + req.getHost() + "/webrtc1?room=" + room + "';");
    html.append("  var initiator = " + (rtc == null ? "0" : "1") + ";");
    html.append("  var pcConfig = null;");  //'iceServers': [{'url': 'stun:stun.services.mozilla.com'}]};");
    html.append("  var pcConstraints = {};");  //'optional': [{'DtlsSrtpKeyAgreement': true}]};");
    html.append("  var offerConstraints = {'optional': [], 'mandatory': {}};");
    html.append("  var mediaConstraints = {'audio': true, 'video': true};");
    html.append("  var stereo = false;");
    html.append("  var audio_send_codec = 'PCMU/8000';");
    html.append("  var audio_receive_codec = 'PCMU/8000';");
    html.append("  var webSocketURL = 'ws://" + req.getHost() + ":" + WebConfig.http_port + "/webrtcsocket';");
    html.append("  setTimeout(initialize, 1);");
    html.append("</script>");
    res.getOutputStream().write(html.toString().getBytes());
  }

  public void doWebRTC2(WebRequest req, WebResponse res) throws Exception {
    int postLength = JF.atoi(req.getHeader("Content-Length"));
    byte post[] = JF.readAll(req.getInputStream(), postLength);
    String query = new String(post);
    String args[] = query.split("&");
    String user = "", pass = "", dial = "";
    for(int a=0;a<args.length;a++) {
      int x = args[a].indexOf("=") + 1;
      if (args[a].startsWith("user=")) user = args[a].substring(x);
      if (args[a].startsWith("pass=")) pass = args[a].substring(x);
      if (args[a].startsWith("dial=")) dial = args[a].substring(x);
    }
    StringBuilder html = new StringBuilder();
    html.append("<html>");
    html.append("<head>");
    html.append("  <title>jfPBX</title>");
    html.append("  <link rel=stylesheet href='/static/webrtc.css' type='text/css'>");
    html.append("  <script src='/static/adapter.js'></script>");
    html.append("  <script type='text/javascript' src='/static/webrtc2.js'></script>");
    html.append("</head>");
    html.append("<body leftmargin=1 rightmargin=1 topmargin=1 bottommargin=1 vlink=ffffff link=ffffff alink=ffffff width=100% height=100%>");
    html.append("<div id='container' ondblclick='enterFullScreen()' style='width: 608px; height: 456px; left: 336px; top: 0px;'>");
    html.append("  <div id='card'>");
    html.append("    <div id='local'>");
    html.append("      <video id='localVideo' muted='true' autoplay='autoplay' style='opacity: 1;'></video>");
    html.append("    </div>");
    html.append("    <div id='remote'>");
    html.append("      <video id='remoteVideo' autoplay='autoplay'></video>");
    html.append("      <div id='mini'>");
    html.append("        <video id='miniVideo' muted='true' autoplay='autoplay'></video>");
    html.append("      </div>");
    html.append("    </div>");
    html.append("  </div>");
    html.append("</div>");
    html.append("<footer id='status'></footer>");
    html.append("<div id='infoDiv'></div>");
    html.append("<script>");
    html.append("  var errorMessages = [];");
    html.append("  var user = '" + user + "';");
    html.append("  var pass = '" + pass + "';");
    html.append("  var dial = '" + dial + "';");
    html.append("  var initiator = 1;");
    html.append("  var pcConfig = null;");
    html.append("  var pcConstraints = {};");
    html.append("  var offerConstraints = {'optional': [], 'mandatory': {}};");
    html.append("  var mediaConstraints = {'audio': true, 'video': true};");
    html.append("  var stereo = false;");
    html.append("  var audio_send_codec = 'PCMU/8000';");
    html.append("  var audio_receive_codec = 'PCMU/8000';");
    html.append("  var webSocketURL = 'wss://" + req.getHost() + ":" + WebConfig.https_port + "/webrtcsocket';");
    html.append("  setTimeout(initialize, 1);");
    html.append("</script>");
    res.getOutputStream().write(html.toString().getBytes());
  }

  private class RTC {
    public WebSocket sock;
    public SIPClient sip;
    public String callid;
    public boolean talking;
    public boolean inuse;
    public RTC other;
  }

  private static HashMap<String, RTC>rooms = new HashMap<String, RTC>();  //share between http and https

  private int sipmin = 6000;
  private int sipmax = 7000;
  private int sipnext = sipmin;

  private static boolean use_sip = true;  //enable webRTC sip support (experimental)

  private synchronized int getlocalport() {
    int port = sipnext++;
    if (sipnext == sipmax) sipnext = sipmin;
    return port;
  }

  public boolean doWebSocketConnect(WebSocket sock) {
    if (!sock.getURL().equals("/webrtcsocket")) return false;
    RTC rtc = new RTC();
    rtc.sock = sock;
    sock.userobj = rtc;
    return true;
  }

  public void doWebSocketClosed(WebSocket sock) {
    if (sock.userobj == null) return;
    RTC rtc = (RTC)sock.userobj;
    if (rtc.talking) {
      rtc.sip.bye(rtc.callid);
      rtc.talking = false;
    }
    if (use_sip) {
      rtc.sip.uninit();
    }
  }

  public void doWebSocketMessage(WebSocket sock, byte data[], int msg_type) {
    if (sock.userobj == null) return;
    //parse JSON message
//    JFLog.log("WebSocketMessage:" + new String(data));
    JFLog.log("RTC:JSON=" + new String(data));
    try {
      RTC rtc = (RTC)sock.userobj;
      JSON.Element json = JSON.parse(new String(data));
      //find type
      String type = "", sdp = "", room = "", user = "", pass = "", dial = "";
      for(int a=0;a<json.children.size();a++) {
        JSON.Element e = json.children.get(a);
        String key = e.key;
        String val = e.value;
        if (key.equals("type")) type = val;
        else if (key.equals("sdp")) sdp = val;
        else if (key.equals("room")) room = val;
        else if (key.equals("user")) user = val;
        else if (key.equals("pass")) pass = val;
        else if (key.equals("dial")) dial = val;
      }
      if (type.equals("hello")) {
        JFLog.log("hello:" + room);
        rooms.put(room, rtc);
      } else if (type.equals("register")) {
        rtc.sip = new SIPClient();
        rtc.sip.userobj = rtc;
        rtc.sip.init("127.0.0.1", WebConfig.sip_port, getlocalport(), this, TransportType.UDP);
        rtc.sip.register(user, user, null, pass);
      } else if (type.equals("offer")) {
        RTC rtc2 = rooms.get(room);
        StringBuilder json2 = new StringBuilder();
        json2.append("{");
        json2.append("\"type\":" + "\"offer\"");
        json2.append(",\"sdp\":\"" + sdp + "\"");
        json2.append("}");
        rtc2.sock.write(json2.toString().getBytes(), WebSocket.TYPE_TEXT);
        rtc2.other = rtc;
        rtc2.inuse = true;
      } else if (type.equals("invite")) {
        sdp = "SIP/2.0 ???\r\n\r\n" + sdp.replaceAll("\\\\r\\\\n", "\r\n");
        String lns[] = sdp.split("\r\n");
/*        for(int a=0;a<lns.length;a++) {
          //save some params
          String ln = lns[a];
          if (ln.startsWith("a=ice-ufrag:")) rtc.iceufrag = ln;
          else if (ln.startsWith("a=ice-pwd:")) rtc.icepwd = ln;
          else if (ln.startsWith("a=fingerprint:")) rtc.fingerprint = ln;
        }*/
        //send an invite to room (IVR)
        JFLog.log("RTC:INVITE:SDP=" + sdp);
        rtc.sip.invite(dial, SIP.getSDP(lns));
      } else if (type.equals("bye")) {
        if (rtc.sip != null) {
          if (rtc.callid != null) {
            rtc.sip.bye(rtc.callid);
            rtc.callid = null;
          }
        } else {
          rooms.remove(room);
        }
      } else if (type.equals("answer")) {
        RTC rtc2 = rooms.get(room);
        StringBuilder json2 = new StringBuilder();
        json2.append("{");
        json2.append("\"type\":" + "\"answer\"");
        json2.append(",\"sdp\":\"" + sdp + "\"");
        json2.append("}");
        rtc2.other.sock.write(json2.toString().getBytes(), WebSocket.TYPE_TEXT);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void onRegister(SIPClient sip, boolean success) {
  }

  public void onTrying(SIPClient sip, String callid) {
  }

  public void onRinging(SIPClient sip, String callid) {
  }

  public void onSuccess(SIPClient sip, String callid, SDP sdp, boolean complete) {
    if (complete) {
      RTC rtc = (RTC)sip.userobj;
      String sdpstr = convertSDPtoWebSDP(rtc,sip.getSDP(callid));
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"type\":" + "\"answer\"");
      json.append(",\"sdp\":\"" + sdpstr + "\"");
      json.append("}");
      JFLog.log("RTC:onSuccess:SDP=" + sdpstr.replaceAll("\\\\r\\\\n", "\r\n"));
      rtc.sock.write(json.toString().getBytes(), WebSocket.TYPE_TEXT);
    }
  }

  public void onBye(SIPClient sip, String callid) {
  }

  public int onInvite(SIPClient sip, String callid, String toName, String toNumber, SDP sdp) {
    RTC rtc = (RTC)sip.userobj;
    if (rtc.talking) {
      //reinvite
      StringBuilder json = new StringBuilder();
      json.append("{");
      json.append("\"type\":" + "\"answer\"");
      json.append(",\"sdp\":\"" + convertSDPtoWebSDP(rtc, sip.getSDP(callid)) + "\"");
      json.append("}");
      rtc.sock.write(json.toString().getBytes(), WebSocket.TYPE_TEXT);
      return 200;
    }
    return 486;  //busy
  }

  public void onCancel(SIPClient sip, String callid, int code) {
  }

  public void onRefer(SIPClient sip, String callid) {
  }

  public void onNotify(SIPClient sip, String callid, String event, String msg) {
  }

  public void onAck(SIPClient sip, String callid, SDP sdp) {
  }

  public void onMessage(SIPClient sip, String callid, String fromid, String fromnumber, String msg) {
  }

  //http://tools.ietf.org/html/rfc5763 - Secure RTP (fingerprint)

  private String convertSDPtoWebSDP(RTC rtc, String sdp) {
    String lns[] = sdp.split("\r\n");
    StringBuilder out = new StringBuilder();
    boolean fingerprint = false;
    boolean m = false;
    String ip = null;
    int port = -1;
    int id = 1234567890;
    for(int a=0;a<lns.length;a++) {
      String ln = lns[a];
      if (ln.startsWith("a=content:")) continue;  //unrecognized
      if (ln.startsWith("c=")) {
        //c=IN IP4 <ip>
        ip = ln.substring(9);
      }
      if (ln.startsWith("m=")) {
        ln = ln.replaceAll("RTP/AVP", "RTP/SAVPF");  //???
        //m=audio port ...
        if (!fingerprint) {
          //add a bogus ice params and DTLS fingerprint before first m=
          out.append("a=ice-ufrag:12345678");
          out.append("\\r\\n");
          out.append("a=ice-pwd:javaforce");
          out.append("\\r\\n");
          out.append("a=fingerprint:sha-256 " + fingerprintSHA256);
          out.append("\\r\\n");
          fingerprint = true;
        }
        if (m) {
          //output candidate for last stream
          out.append("a=setup:actpass");  //http://tools.ietf.org/html/rfc4145
          out.append("\\r\\n");
          out.append("a=candidate: 0 1 UDP " + (id++) + " " + ip + " " + port + " typ host"); // raddr " + ip + " rport " + port);
          out.append("\\r\\n");
          out.append("a=candidate: 0 2 UDP " + (id++) + " " + ip + " " + (port+1) + " typ host"); // raddr " + ip + " rport " + port);
          out.append("\\r\\n");
          out.append("a=rtcp-mux");
          out.append("\\r\\n");
        }
        m = true;
        port = JF.atoi(ln.split(" ")[1]);
      }
      out.append(ln);
      out.append("\\r\\n");
    }
    if (m) {
      //output candidate for last stream
      out.append("a=setup:actpass");
      out.append("\\r\\n");
      out.append("a=candidate: 0 1 UDP " + (id++) + " " + ip + " " + port + " typ host"); // raddr " + ip + " rport " + port);
      out.append("\\r\\n");
      out.append("a=candidate: 0 2 UDP " + (id++) + " " + ip + " " + (port+1) + " typ host"); // raddr " + ip + " rport " + port);
      out.append("\\r\\n");
      out.append("a=rtcp-mux");
      out.append("\\r\\n");
    }
    return out.toString();
  }
}

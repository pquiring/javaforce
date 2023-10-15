package jfpbx.core;

import jfpbx.db.Database;
import jfpbx.db.RouteRow;
import jfpbx.db.TrunkRow;
import jfpbx.db.ExtensionRow;
import java.util.*;
import java.io.*;

import javaforce.*;
import javaforce.voip.*;
import javaforce.jbus.*;


/** Handles main SIP messages and dispatches them to low-level plugins. */

public class Service implements SIPServerInterface, PBXAPI {

  public static String getVersion() {
    return "0.32";
  }

  private SIPServer ss;
  private Vector<DialChain> dialChainList;
  private Hashtable<String, Extension> extList;
  private JBusClient jbusClient;
  private boolean relayAudio, relayVideo;
  private Timer timer;

  //plugins
  public Trunks pluginTrunks;
  public Extensions pluginExtensions;
  public VoiceMail pluginVoicemail;
  public IVR pluginIvr;
  public Queues pluginQueues;

  public static int sip_port;

  public boolean init() {
    if (JF.isUnix()) {
      jbusClient = new JBusClient("org.jflinux.service.jfpbx", new JBusMethods());
      jbusClient.start();
    }
    //start service
    ss = new SIPServer();
    int rtpmin, rtpmax;
    try {
      sip_port = Integer.valueOf(getCfg("port"));
      rtpmin = Integer.valueOf(getCfg("rtpmin"));
      rtpmax = Integer.valueOf(getCfg("rtpmax"));
      if ((rtpmin < 1024) || (rtpmin > 65535-1000)) rtpmin = 32768;
      if ((rtpmax < 1024) || (rtpmax > 65535)) rtpmax = 65535;
      if ((rtpmin > rtpmax) || (rtpmax - rtpmin < 1000)) {
        rtpmin = 32768;
        rtpmax = 65535;
      }
    } catch (Exception e) {
      sip_port = 5060;
      rtpmin = 32768;
      rtpmax = 65535;
    }
    JFLog.log("Setting RTP Range:" + rtpmin + "-" + rtpmax);
    RTP.setPortRange(rtpmin, rtpmax);

    dialChainList = new Vector<DialChain>();
    extList = new Hashtable<String, Extension>();
    relayAudio = getCfg("relayAudio").equals("true");
    relayVideo = getCfg("relayVideo").equals("true");
    String moh = getCfg("moh");
    if (moh.length() > 0) {
      MusicOnHold.loadWav(Paths.sounds + Paths.lang + "/" + moh + ".wav");
    }
    WebConfig.http_port = JF.atoi(getCfg("http"));
    if (WebConfig.http_port == 0) WebConfig.http_port = 80;
    WebConfig.https_port = JF.atoi(getCfg("https"));
    if (WebConfig.https_port == 0) WebConfig.https_port = 443;
    WebConfig.hideAdmin = getCfg("hideAdmin").equals("true");
    WebConfig.disableWebRTC = getCfg("disableWebRTC").equals("true");
    WebConfig.sip_port = sip_port;
    new File(Paths.lib + "voicemail").mkdirs();
    new File(Paths.logs).mkdirs();

    initPlugins();

    ss.init(sip_port, this, TransportType.UDP);
//    ss.enableQOP(true);  //test!
    //create timer to register trunks (every 110 seconds)
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {public void run() {registerTrunks();}}, 0, 110 * 1000);
    return true;
  }
  public void uninit() {
    if (ss != null) {
      ss.uninit();
      ss = null;
    }
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    uninitPlugins();
  }
  private void initPlugins() {
    pluginTrunks = new Trunks();
    pluginTrunks.init(this);
    pluginExtensions = new Extensions();
    pluginExtensions.init(this);
    pluginVoicemail = new VoiceMail();
    pluginVoicemail.init(this);
    pluginIvr = new IVR();
    pluginIvr.init(this);
    pluginQueues = new Queues();
    pluginQueues.init(this);
  }

  public void uninitPlugins() {
    if (pluginTrunks != null) {
      pluginTrunks.uninit(this);
      pluginTrunks = null;
    }
    if (pluginExtensions != null) {
      pluginExtensions.uninit(this);
      pluginExtensions = null;
    }
    if (pluginVoicemail != null) {
      pluginVoicemail.uninit(this);
      pluginVoicemail = null;
    }
    if (pluginIvr != null) {
      pluginIvr.uninit(this);
      pluginIvr = null;
    }
    if (pluginQueues != null) {
      pluginQueues.uninit(this);
      pluginQueues = null;
    }
  }
  public void restart() {
    uninit();
    JF.sleep(1000);
    init();
  }
  public String getCfg(String id) {
    return Database.getConfig(id);
  }
  private long getNow() {
    return System.currentTimeMillis() / 1000;
  }
//interface SIPServerInterface
  public CallDetailsServer createCallDetailsServer() {
    return new CallDetailsPBX();
  }
  public String getPassword(String number) {
    ExtensionRow ext = Database.getExtension(number);
    return ext.password;
  }
  public void onRegister(String ext, int expires, String remoteip, int remoteport) {
    if (expires == 0) {
      extList.remove(ext);
    } else {
      extList.put(ext, new Extension(ext, getNow() + expires, remoteip, remoteport));
    }
  }
  public void onOptions(CallDetailsServer cd, boolean src) {
    //send 200 and ignore
    reply(cd, 200, "OK", null, false, src);
  }
  public void onInvite(CallDetailsServer cd, boolean src) {
    cd.pid = onInvite(cd, src, cd.pid);
  }
  public int onInvite(CallDetailsServer cd, boolean src, int pid) {
    DialChain chain;
    int newpid;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if ((pid == 0) || (pid == chain.getPriority())) {
        newpid = chain.onInvite((CallDetailsPBX)cd, src);
        if (newpid != -1) {
          return newpid;
        }
      }
    }
    JFLog.log("Dest not found:" + cd.dialed);
    reply(cd, 404, "NOT ONLINE", null, false, true);
    return -1;
  }
  public void onCancel(CallDetailsServer cd, boolean src) {
    onCancel(cd, src, cd.pid);
  }
  public void onCancel(CallDetailsServer cd, boolean src, int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (pid == chain.getPriority()) {
        chain.onCancel((CallDetailsPBX)cd, src);
        return;
      }
    }
  }
  public void onBye(CallDetailsServer cd, boolean src) {
    onBye(cd, src, cd.pid);
  }
  public void onBye(CallDetailsServer cd, boolean src, int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (pid == chain.getPriority()) {
        chain.onBye((CallDetailsPBX)cd, src);
        return;
      }
    }
  }
  public void onSuccess(CallDetailsServer cd, boolean src) {
    onSuccess(cd, src, cd.pid);
  }
  public void onSuccess(CallDetailsServer cd, boolean src, int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (pid == chain.getPriority()) {
        chain.onSuccess((CallDetailsPBX)cd, src);
        return;
      }
    }
  }
  public void onRinging(CallDetailsServer cd, boolean src) {
    onRinging(cd, src, cd.pid);
  }
  public void onRinging(CallDetailsServer cd, boolean src, int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (pid == chain.getPriority()) {
        chain.onRinging((CallDetailsPBX)cd, src);
        return;
      }
    }
  }
  public void onError(CallDetailsServer cd, int code, boolean src) {
    onError(cd, code, src, cd.pid);
  }
  public void onError(CallDetailsServer cd, int code, boolean src, int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (pid == chain.getPriority()) {
        chain.onError((CallDetailsPBX)cd, code, src);
        return;
      }
    }
  }
  public void onTrying(CallDetailsServer cd, boolean src) {
    onTrying(cd, src, cd.pid);
  }
  public void onTrying(CallDetailsServer cd, boolean src, int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (pid == chain.getPriority()) {
        chain.onTrying((CallDetailsPBX)cd, src);
        return;
      }
    }
  }
  public void onFeature(CallDetailsServer cd, String cmd, String cmddata, boolean src) {
    onFeature(cd, cmd, cmddata, src, cd.pid);
  }
  public void onFeature(CallDetailsServer cd, String cmd, String cmddata, boolean src, int pid) {
    DialChain chain;
    if (cmd.equalsIgnoreCase("REFER")) {
      if (src) {
        reply(cd, 202, "REFER OK", null, false, true);
        cd.cmd = "NOTIFY";
        cd.sdp = "SIP/2.0 200 OK";
        issue(cd, "Event: refer\r\n", true, true);
        cd.cmd = "BYE";
        issue(cd, null, false, true);
        String tmp[] = SIP.split(cmddata);
        transfer_dst((CallDetailsPBX)cd, tmp[1]);
      } else {
        reply(cd, 202, "REFER OK", null, false, false);
        cd.cmd = "NOTIFY";
        cd.sdp = "SIP/2.0 200 OK";
        issue(cd, "Event: refer\r\n", true, false);
        cd.cmd = "BYE";
        issue(cd, null, false, false);
        String tmp[] = SIP.split(cmddata);
        transfer_src((CallDetailsPBX)cd, tmp[1]);
      }
      return;
    }
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if ((pid == 0) || (pid == chain.getPriority())) {
        chain.onFeature((CallDetailsPBX)cd, cmd, cmddata, src);
        return;
      }
    }
  }
  public void onMessage(CallDetailsServer cd, String from, String to, String msg, boolean src) {
    onMessage(cd, from, to, msg, src, cd.pid);
  }
  public void onMessage(CallDetailsServer cd, String from, String to, String msg, boolean src, int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (chain.onMessage((CallDetailsPBX)cd, from, to, msg, src)) {
        return;
      }
    }
    JFLog.log("Dest not found:" + cd.dialed);
    reply(cd, 404, "NOT ONLINE", null, false, true);
  }
//interface PBXAPI
  public void hookDialChain(DialChain chain) {
    DialChain chainEntry;
    int priority = chain.getPriority();
    int idx = 0;
    for(;idx < dialChainList.size();) {
      chainEntry = dialChainList.get(idx);
      if (chainEntry.getPriority() < priority) {idx++; continue;}
      dialChainList.add(idx, chain);
      return;
    }
    dialChainList.add(idx, chain);
  }
  public void unhookDialChain(DialChain chain) {
    dialChainList.remove(chain);
  }
  public void schedule(Runnable plugin, int minutes) {}
  public void unschedule(Runnable plugin) {}
  public SIPClient createClient() {return null;}
  public boolean isRegistered(String ext) {
    if (extList.get(ext) != null) return true;
    return false;
  }
  public boolean reply(CallDetailsServer cd, int code, String msg, String header, boolean sdp, boolean src) {
    if (cd.xfer_dst && src) {
      JFLog.log("reply:refer(to src):" + code);
      switch (code) {
        case 200:
          //now send reinvite back to src (if PBX doesn't relay data)
          cd.xfer_dst = false;
          if (!relayAudio || !relayVideo) {
            cd.xfer_src = true;
            cd.cmd = "INVITE";
            connect((CallDetailsPBX)cd);
            ss.buildsdp(cd, cd.pbxsrc);
            issue(cd, null, true, true);
          }
          return true;
        case 100:
        case 180:
        case 183:
          return true;
        default:
          //xfer failed - term call
          cd.xfer_dst = false;
          cd.cmd = "BYE";
          issue(cd, null, false, true);
          disconnect((CallDetailsPBX)cd);
          return true;
      }
    }
    if (cd.xfer_src && !src) {
      cd.xfer_src = false;
      //BUG : What is src changes SDP details???  Do "another" reinvite to dst???
      return true;
    }
    if (src) {
      cd.pbxsrc.host = cd.src.host;
      cd.pbxsrc.port = cd.src.port;
    } else {
      cd.pbxdst.host = cd.dst.host;
      cd.pbxdst.port = cd.dst.port;
    }
    JFLog.log("reply:" + cd.cmd + ":" + code + " to:" + (src ? cd.src.host : cd.dst.host) + ":" + (src ? cd.src.port : cd.dst.port));
    return ss.reply(cd,code,msg,header,sdp,src);
  }
  public boolean issue(CallDetailsServer cd, String header, boolean sdp, boolean src) {
/*
    if ((cd.xfer_src) && (src)) {
      return true;
    }
*/
    if (cd.cmd.equals("INVITE") && !sdp) {
      try {
        throw new Exception("error");
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
    if (src) {
      cd.src.host = cd.pbxsrc.host;
      cd.src.port = cd.pbxsrc.port;
    } else {
      cd.dst.host = cd.pbxdst.host;
      cd.dst.port = cd.pbxdst.port;
    }
    JFLog.log("issue:" + cd.cmd + " to:" + (src ? cd.src.host : cd.dst.host) + ":" + (src ? cd.src.port : cd.dst.port));
    return ss.issue(cd,header,sdp,src);
  }
  /** transfers source (call originator) to a new dest */
  public boolean transfer_src(CallDetailsPBX cd, String number) {
    JFLog.log("transfer_src");
    cd.dialed = number;
    cd.src.to[0] = number;
    cd.src.to[1] = number;
    cd.xfer_dst = true;
    cd.cmd = "INVITE";
    //reset values
    cd.invited = false;
    cd.connected = false;
    cd.cancelled = false;
    cd.pid = 0;
    cd.dst = new CallDetails.SideDetails();
    cd.pbxdst = new CallDetails.SideDetails();
    JFLog.log("transfer_src:from="+cd.src.from[0]+":"+cd.src.from[1]+":"+cd.src.contact+":"+cd.uri);
    //do reinvites
    onInvite(cd, true);
    return true;
  }
  /** transfers dest (call terminator) to a new dest */
  public boolean transfer_dst(CallDetailsPBX cd, String number) {
    JFLog.log("transfer_dst");
    //copy dst to src
    cd.src = cd.dst;
    cd.pbxsrc = cd.pbxdst;
    //swap RTP connections
    if (cd.audioRelay != null) {
      cd.audioRelay.swap();
    }
    if (cd.videoRelay != null) {
      cd.videoRelay.swap();
    }
    //now do a normal transfer
    transfer_src(cd, number);
    return true;
  }
  public Extension getExtension(String ext) {
    if (ext == null) {
      JFLog.log("getExtension:null");
      return null;
    }
    return extList.get(ext);
  }
  public String getlocalhost(CallDetailsPBX cd) {
    return cd.localhost;
  }
  public int getlocalport() {
    return sip_port;
  }
  /** Returns new dial string in [0] and trunks in [1-n] */
  public TrunkRow[] getTrunks(Dial dialed, String number) {
    //look for a route that matches dialed and return its trunks
    String routetable = null;
    if (number != null) {
      ExtensionRow ext = Database.getExtension(number);
      if (ext != null) {
        routetable = ext.routetable;
      }
    }
    if (routetable == null) {
      routetable = "default";
    }
    RouteRow[] routes = Database.getOutRoutes(routetable);
    if (routes == null) {
      JFLog.log("Service:routetable not found:" + routetable);
      return null;
    }
    String patterns[];
    String newdialed;
    for(int a=0;a<routes.length;a++) {
      patterns = routes[a].patterns.split(",");
      for(int b=0;b<patterns.length;b++) {
        newdialed = patternMatches(patterns[b], dialed.number);
        if (newdialed == null) continue;
        JFLog.log("PatternMatched:" + patterns[b]);
        dialed.number = newdialed;
        return Database.getTrunks(routes[a]);
      }
    }
    return null;
  }
  /** Resolve hostname to IP address. */
  public String resolve(String host) {
    return ss.resolve(host);
  }
  public void releaseCallDetails(CallDetails cd) {
    ss.setCallDetailsServer(cd.callid, null);
  }
  /** Returns new dial number if it matches pattern, else returns null. */
  public String patternMatches(String pattern, String dialed) {
/* pattern:
 #=match exact digit (0-9)
 X=0-9
 Z=1-9
 N=2-9
 [123-9]=range (TODO)
 .=wildcard (last char only)
 x|y = removes 'x' if 'xy' matches
 x+y = add 'x' if 'y' matches
*/
    JFLog.log("PatternMatch:pattern=" + pattern + ":dialed=" + dialed);
    char cap[] = pattern.toCharArray();
    char cad[] = dialed.toCharArray();
    int plus = pattern.indexOf('+');
    int pipe = pattern.indexOf('|');
    int pi = 0;
    int di = 0;
    if ((plus != -1) && (pipe != -1)) {
      if (plus > pipe) {
        //swap around plus/pipe strings
        pattern = pattern.substring(0, pipe+1) + pattern.substring(pipe+1, plus+1) + pattern.substring(plus+1);
        plus = pattern.indexOf('+');
        pipe = pattern.indexOf('|');
      }
    }
    if (plus != -1) {
      pi = plus+1;
    }
    int pl = pattern.length();
    int dl = dialed.length();
    while (pi < pl) {
      if ((cap[pi] >= '0') && (cap[pi] <= '9')) {
        if (di >= dl) return null;
        if (cap[pi] != cad[di]) return null;
        pi++;
        di++;
        continue;
      }
      switch (Character.toUpperCase(cap[pi])) {
        case 'X':
          if (di >= dl) return null;
          if ((cad[di] < '0') || (cad[di] > '9')) return null;
          break;
        case 'Z':
          if (di >= dl) return null;
          if ((cad[di] < '1') || (cad[di] > '9')) return null;
          break;
        case 'N':
          if (di >= dl) return null;
          if ((cad[di] < '2') || (cad[di] > '9')) return null;
          break;
        case '.':
          pi = pl-1;
          di = dl-1;
          break;
        case '|':
          pi++;
          continue;
      }
      pi++;
      di++;
    }
    if (di < dl) return null;
    if ((plus != -1) && (pipe != -1)) {
      return pattern.substring(0, plus) + dialed.substring(pipe-1-plus);
    } else if (plus != -1) {
      return pattern.substring(0, plus) + dialed;
    } else if (pipe != -1) {
      return dialed.substring(pipe);
    }
    return dialed;
  }
  public void log(CallDetailsPBX cd, String msg) {
    JFLog.log("To:" + cd.src.to[1] + " From:" + cd.src.from[1] + " " + msg);
  }
  public void log(CallDetailsPBX cd, Exception e) {
    JFLog.log("To:" + cd.src.to[1] + " From:" + cd.src.from[1] + " exception:");
    JFLog.log(e);
  }
  public DialChain getDialChain(int pid) {
    DialChain chain;
    for(int a=0;a<dialChainList.size();a++) {
      chain = dialChainList.get(a);
      if (pid == chain.getPriority()) return chain;
    }
    return null;
  }
  public void makePath(String path) {
    try {
      File file = new File(path);
      file.mkdirs();
    } catch (Exception e) {
    }
  }
  public String convertString(String instr) {
    //convert WebString to normal strings (expand %## codes, '+'->' ')
    String outstr = "";
    char ca[] = instr.toCharArray();
    int h1, h2;
    char ch;
    for(int a=0;a<ca.length;a++) {
      switch (ca[a]) {
        case '%':
          if (a+2>=ca.length) return outstr;
          if ((ca[a+1] >= '0') && (ca[a+1] <= '9')) h1 = ca[a+1] - '0';
          else if ((ca[a+1] >= 'a') && (ca[a+1] <= 'f')) h1 = ca[a+1] - ('a' - 10);
          else if ((ca[a+1] >= 'A') && (ca[a+1] <= 'F')) h1 = ca[a+1] - ('A' - 10);
          else h1 = 0;
          if ((ca[a+2] >= '0') && (ca[a+2] <= '9')) h2 = ca[a+2] - '0';
          else if ((ca[a+2] >= 'a') && (ca[a+2] <= 'f')) h2 = ca[a+2] - ('a' - 10);
          else if ((ca[a+2] >= 'A') && (ca[a+2] <= 'F')) h2 = ca[a+2] - ('A' - 10);
          else h2 = 0;
          ch = (char)(h1 * 0x10 + h2);
          if (ch == '\"') ch = '\'';
          outstr += ch;
          a+=2;
          break;
        case '+': outstr += " "; break;
        default: outstr += ca[a]; break;
      }
    }
    return outstr;
  }
  private void registerTrunks() {
    try {
      TrunkRow trunks[] = Database.getTrunks();
      for(int trunk=0;trunk<trunks.length;trunk++) {
        if (!trunks[trunk].doRegister()) continue;
        String register = trunks[trunk].register;  //user : password @ domain[:sip_port] [/ did]
        int idx = register.indexOf(":");
        if (idx == -1) continue;  //bad string
        String user = register.substring(0, idx);
        register = register.substring(idx+1);
        idx = register.indexOf("@");
        if (idx == -1) continue;  //bad string
        String pass = register.substring(0, idx);
        register = register.substring(idx+1);
        idx = register.indexOf("/");
        String host = null;
        String did = "s";
        if (idx == -1) {
          host = register;
        } else {
          host = register.substring(0, idx);
          did = register.substring(idx+1);
        }
        int port = 5060;
        idx = host.indexOf(":");
        if (idx != -1) {
          port = Integer.valueOf(host.substring(idx+1));
          host = host.substring(0, idx);
        }
//        JFLog.log("Register Trunk:" + trunks[trunk]);
        ss.register(user, pass, host, port, 120, did, host + "---jfpbx---");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public String getTrunkRegister(String ip) {
    String reg = null;
    try {
      TrunkRow trunks[] = Database.getTrunks();
      //user : pass @ host [:sip_port] / did
      int idx;
      String host;
      for(int a=0;a<trunks.length;a++) {
        String register = trunks[a].register;
        idx = register.indexOf("@");
        if (idx == -1) continue;
        host = register.substring(idx+1);
        idx = host.indexOf(":");
        if (idx == -1) {
          idx = host.indexOf("/");
        }
        if (idx != -1) {
          host = host.substring(0, idx);
        }
        String trunkip = resolve(host);
        if (trunkip == null) continue;
        if (trunkip.equals(ip)) {
          reg = register;
          break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
    return reg;
  }
  public SIPServerInterface getSIPServerInterface() {
    return this;
  }
  public static void test() {
    Service s = new Service();
    JFLog.log(s.patternMatches("123", "123"));
    JFLog.log(s.patternMatches("1+23", "23"));
    JFLog.log(s.patternMatches("1|123", "1123"));
    JFLog.log(s.patternMatches("1+1|23", "123"));
    JFLog.log(s.patternMatches("NXXNXXX", "2342345"));
    JFLog.log(s.patternMatches("011.", "0111231234"));
  }
  public class JBusMethods {
    public void stop() {
      System.exit(0);
    }
    public void status(String pack) {
      jbusClient.call(pack, "serviceStatus", "\"jfpbx running\"");
    }
  }
  /**
   * Connects two end-points by creating and changing the RTPRelay's as needed.
   */
  public void connect(CallDetailsPBX cd) {
    SDP.Stream pbx_astream, pbx_vstream;
    SDP.Stream other_astream, other_vstream;
    //determine what needs to be connected and do it
    if (cd.audioRelay == null && relayAudio) {
      cd.audioRelay = new RTPRelay();
      cd.audioRelay.init();
      cd.audioRelay.setRawMode(true);
    }
    if (cd.videoRelay == null && relayVideo) {
      cd.videoRelay = new RTPRelay();
      cd.videoRelay.init();
      cd.videoRelay.setRawMode(true);
    }
JFLog.log("connect:" + relayAudio +","+ relayVideo);
    //copy src -> pbxdst
    if (cd.src.sdp != null) {
      cd.pbxdst.sdp = new SDP();
      //copy SRTP-DTLS details
      cd.pbxdst.sdp.fingerprint = cd.src.sdp.fingerprint;
      cd.pbxdst.sdp.iceufrag = cd.src.sdp.iceufrag;
      cd.pbxdst.sdp.icepwd = cd.src.sdp.icepwd;
      other_astream = cd.src.sdp.getFirstAudioStream();
      if (relayAudio) {
        cd.pbxdst.sdp.ip = getlocalhost(cd);
        pbx_astream = cd.pbxdst.sdp.addStream(SDP.Type.audio);
        pbx_astream.codecs = other_astream.codecs;
        pbx_astream.port = cd.audioRelay.getPort_dst();
      } else {
        cd.pbxdst.sdp.ip = other_astream.getIP();
        pbx_astream = cd.pbxdst.sdp.addStream(SDP.Type.audio);
        pbx_astream.ip = other_astream.getIP();
        pbx_astream.codecs = other_astream.codecs;
        pbx_astream.port = other_astream.port;
        pbx_astream.mode = other_astream.mode;
      }
      //copy SRTP-SDP details
      pbx_astream.profile = other_astream.profile;
      if (cd.src.sdp.hasVideo()) {
        other_vstream = cd.src.sdp.getFirstVideoStream();
        if (relayVideo) {
          pbx_vstream = cd.pbxdst.sdp.addStream(SDP.Type.video);
          if (!relayAudio) {
            pbx_vstream.ip = getlocalhost(cd);
          }
          pbx_vstream.codecs = other_vstream.codecs;
          pbx_vstream.port = cd.videoRelay.getPort_dst();
        } else {
          pbx_vstream = cd.pbxdst.sdp.addStream(SDP.Type.video);
          pbx_vstream.ip = other_vstream.getIP();
          pbx_vstream.codecs = other_vstream.codecs;
          pbx_vstream.port = other_vstream.port;
          pbx_vstream.mode = other_vstream.mode;
        }
        //copy SRTP-SDP details
        pbx_vstream.profile = other_vstream.profile;
      }
    }
    //copy dst -> pbxsrc
    if (cd.dst.sdp != null) {
      cd.pbxsrc.sdp = new SDP();
      //copy SRTP-DTLS details
      cd.pbxsrc.sdp.fingerprint = cd.dst.sdp.fingerprint;
      cd.pbxsrc.sdp.iceufrag = cd.dst.sdp.iceufrag;
      cd.pbxsrc.sdp.icepwd = cd.dst.sdp.icepwd;
      other_astream = cd.dst.sdp.getFirstAudioStream();
      if (relayAudio) {
        cd.pbxsrc.sdp.ip = getlocalhost(cd);
        pbx_astream = cd.pbxsrc.sdp.addStream(SDP.Type.audio);
        pbx_astream.codecs = other_astream.codecs;
        pbx_astream.port = cd.audioRelay.getPort_src();
      } else {
        cd.pbxsrc.sdp.ip = other_astream.getIP();
        pbx_astream = cd.pbxsrc.sdp.addStream(SDP.Type.audio);
        pbx_astream.ip = other_astream.getIP();
        pbx_astream.codecs = other_astream.codecs;
        pbx_astream.port = other_astream.port;
        pbx_astream.mode = other_astream.mode;
      }
      //copy SRTP-SDP details
      pbx_astream.profile = other_astream.profile;
      if (cd.dst.sdp.hasVideo()) {
        other_vstream = cd.dst.sdp.getFirstVideoStream();
        if (relayVideo) {
          pbx_vstream = cd.pbxsrc.sdp.addStream(SDP.Type.video);
          if (!relayAudio) {
            pbx_vstream.ip = getlocalhost(cd);
          }
          pbx_vstream.codecs = other_vstream.codecs;
          pbx_vstream.port = cd.videoRelay.getPort_src();
        } else {
          pbx_vstream = cd.pbxsrc.sdp.addStream(SDP.Type.video);
          pbx_vstream.ip = other_vstream.getIP();  //TODO : check if needed
          pbx_vstream.codecs = other_vstream.codecs;
          pbx_vstream.port = other_vstream.port;
          pbx_vstream.mode = other_vstream.mode;
        }
        //copy SRTP-SDP details
        pbx_vstream.profile = other_vstream.profile;
      }
    }

    if (cd.src.sdp == null || cd.dst.sdp == null) return;
    //both ends have offers, try to connect
    if (relayAudio) {
      //start audio
      cd.audioRelay.start(cd.src.sdp.getFirstAudioStream(), cd.dst.sdp.getFirstAudioStream());
    }
    if (relayVideo && cd.src.sdp.hasVideo() && cd.dst.sdp.hasVideo()) {
      //start video
      cd.videoRelay.start(cd.src.sdp.getFirstVideoStream(), cd.dst.sdp.getFirstVideoStream());
    }
  }
  /**
   * Connects two ACTIVE end-points.
   * Swaps the dst RTP in each audioRelay.
   */
  public void connect(CallDetailsPBX cd1, CallDetailsPBX cd2) {
    RTPRelay.swap(cd1.audioRelay, cd2.audioRelay);
  }
  public void disconnect(CallDetailsPBX cd) {
    if (cd.audioRelay != null) {
      cd.audioRelay.uninit();
      cd.audioRelay = null;
    }
    if (cd.videoRelay != null) {
      cd.videoRelay.uninit();
      cd.videoRelay = null;
    }
  }
}

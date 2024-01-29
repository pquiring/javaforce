package jfpbx.core;

import jfpbx.db.ExtensionRow;
import jfpbx.db.Database;
import jfpbx.core.Conference;
import java.util.*;

import javaforce.*;
import javaforce.voip.*;


/** Low-level plugin for handling INVITEs to IVR. */

public class IVR implements Plugin, DialChain, PBXEventHandler {
  public final static int pid = 15;  //priority
  public static enum State {
    IVR_NONE,
    IVR_PLAY_MSG,
    IVR_GET_CHAR,
    IVR_GET_STRING,
    IVR_GOODBYE,
    IVR_TRANSFER,
    IVR_CONF
  }
  private PBXAPI api;

//interface Plugin

  public void init(PBXAPI api) {
    this.api = api;
    api.hookDialChain(this);
    JFLog.log("IVR plugin init");
  }

  public void uninit(PBXAPI api) {
    api.unhookDialChain(this);
    JFLog.log("IVR plugin uninit");
  }

  public void install(PBXAPI api) {
    //nothing to do
  }

  public void uninstall(PBXAPI api) {
    //nothing to do
  }

//interface DialChain
  public int getPriority() {return pid;}

  public int onInvite(CallDetailsPBX cd, boolean src) {
    //NOTE:There is no dst used in IVR (it's a one-sided call)
    if (!cd.authorized) {
      if (!cd.anon) return -1;
    }
    ExtensionRow ivr = Database.getIVR(cd.dialed);
    if (ivr == null) return -1;  //an IVR is not being dialed
    if (cd.invited) {
      //reINVITE
      api.log(cd, "IVR : reINVITE");
      SDP.Stream astream = cd.src.sdp.getFirstAudioStream();
      cd.audioRelay.change_src(astream);
      cd.pbxsrc.sdp.getFirstAudioStream().codecs = astream.codecs;
      cd.sip.buildsdp(cd, cd.pbxsrc);
      api.reply(cd, 200, "OK", null, true, true);
      return pid;
    }
    String script = api.convertString(ivr.script);
    cd.ivrscript = script.replaceAll("\r", " ").replaceAll("\n", " ").replaceAll("\t", " ").replaceAll(">", " > ").replaceAll("<", " < ").replaceAll("=", " = ")
      .replaceAll("!", " ! ").replaceAll("[+]", " + ").split(" ");
    cd.ivrtag = 0;
    cd.pbxsrc.to = cd.src.to.clone();
    cd.pbxsrc.from = cd.src.from.clone();
    if (cd.audioRelay == null) {
      cd.audioRelay = new RTPRelay();
      cd.audioRelay.init();
    }
    cd.audioRelay.init(cd, this, api);
    cd.pbxsrc.host = cd.src.host;
    cd.pbxsrc.sdp = new SDP();
    cd.pbxsrc.sdp.ip = api.getlocalhost(cd);
    SDP.Stream astream = cd.pbxsrc.sdp.addStream(SDP.Type.audio);
    astream.codecs = cd.src.sdp.getFirstAudioStream().codecs;
    astream.port = cd.audioRelay.getPort_src();
    if (cd.src.sdp.hasVideo()) {
      if (cd.videoRelay == null) {
        cd.videoRelay = new RTPRelay();
        cd.videoRelay.init();
      }
      //do a test with video
      SDP.Stream vstream = cd.pbxsrc.sdp.addStream(SDP.Type.video);
      vstream.codecs = cd.src.sdp.getFirstVideoStream().codecs;
      vstream.port = cd.videoRelay.getPort_src();
      vstream.mode = SDP.Mode.inactive;
    }
    cd.sip.buildsdp(cd, cd.pbxsrc);
    cd.invited = true;
    cd.connected = true;
    cd.pbxsrc.to = SIP.replacetag(cd.pbxsrc.to, SIP.generatetag());  //assign tag
    api.reply(cd, 200, "OK", null, true, true);
    start(cd);
    return pid;
  }

  public void onRinging(CallDetailsPBX cd, boolean src) {
  }

  public void onSuccess(CallDetailsPBX cd, boolean src) {
  }

  public void onCancel(CallDetailsPBX cd, boolean src) {
  }

  public void onError(CallDetailsPBX cd, int code, boolean src) {
  }

  public void onTrying(CallDetailsPBX cd, boolean src) {
  }

  public void onBye(CallDetailsPBX cd, boolean src) {
    if (!src) return;
    api.reply(cd, 200, "OK", null, false, true);
    if (cd.audioRelay != null) {
      cd.audioRelay.uninit();
      cd.audioRelay = null;
    }
    if (cd.ivrstate == State.IVR_CONF) {
      delMember(cd);
    }
  }

  public void onFeature(CallDetailsPBX cd, String cmd, String cmddata, boolean src) {
  }
  public boolean onMessage(CallDetailsPBX cd, String from, String to, String[] msg, boolean src) {
    return false;
  }

//interface PBXEventHandler
  public void event(CallDetailsPBX cd, int type, char digit, boolean interrupted) {
    //type = DTMF digit or end of playSound()
    JFLog.log("event:type=" + type + ":digit=" + digit + ":ivrstate=" + cd.ivrstate);
    switch (type) {
      case PBXEventHandler.DIGIT:
        cd.ivrstring.append(digit);
        switch (cd.ivrstate) {
          case IVR_PLAY_MSG:
            if (interrupted) {
              if (!cd.ivrint) break;
            }
            runScript(cd);
            break;
          case IVR_GET_CHAR:
            if (cd.ivrstring.length() > 0) {
              setvar(cd, cd.ivrvar, cd.ivrstring.substring(0, 1));
              cd.ivrstring.delete(0,cd.ivrstring.length());
            } else {
              setvar(cd, cd.ivrvar, " ");
            }
            runScript(cd);
            break;
          case IVR_GET_STRING:
            if ((digit == '#') || (digit == '*')) {
              int idx = cd.ivrstring.indexOf("#");
              //assign it to $var
              setvar(cd, cd.ivrvar, cd.ivrstring.substring(0, idx));
              flush(cd.ivrstring);
              runScript(cd);
              break;
            }
            break;
        }
        break;
      case PBXEventHandler.SOUND:
        switch (cd.ivrstate) {
          case IVR_PLAY_MSG:
            if (!interrupted) {
              runScript(cd);
            }
            break;
          case IVR_GET_CHAR:
            if (!interrupted) {
              setvar(cd, cd.ivrvar, " ");
              runScript(cd);
            }
            break;
          case IVR_GET_STRING:
            if (!interrupted) {
              setvar(cd, cd.ivrvar, cd.ivrstring.toString());
              flush(cd.ivrstring);
              runScript(cd);
            }
            break;
          case IVR_CONF:
            switch (cd.confstate) {
              case CONF_DROP:
                hangup(cd);  //end of 'conf-admin-left.wav'
                break;
            }
        }
        break;
    }
  }

  public void samples(CallDetailsPBX cd, short sam[]) {
    if (cd.ivrstate != State.IVR_CONF) {
      System.arraycopy(RTPRelay.silence, 0, sam, 0, 160);
      return;
    }
    Conference.Member member, myMember = cd.confmember;
    int actcnt = 0;
    int admincnt = 0;
    synchronized(Conference.lock) {
      Vector<Conference.Member> memberList = Conference.list.get(cd.dialed);
      for(int a=0;a<memberList.size();a++) {
        member = memberList.get(a);
        if (!member.dropped) {
          actcnt++;
          if (member.admin) admincnt++;
        }
      }
      //record samples for mixing
      System.arraycopy(sam, 0, myMember.buf[inc(myMember.idx)], 0, 160);
      myMember.idx = inc(myMember.idx);
      switch (cd.confstate) {
        case CONF_WAIT:
          System.arraycopy(RTPRelay.silence, 0, sam, 0, 160);
          //check if admin is present
          if (admincnt > 0) {
            cd.confstate = Conference.State.CONF_TALK;
            //reset all jitter idxs
            for(int a=0;a<memberList.size();a++) {
              member = memberList.get(a);
              myMember.idxs[a] = member.idx;
            }
            //add video if enabled
            if (cd.confVideo) {
              reInviteVideo(cd, myMember, memberList);
              for(int a=0;a<memberList.size();a++) {
                member = memberList.get(a);
                if (!member.cd.confVideo) continue;
                if (member.dropped) continue;
                reInviteVideo(member.cd, member, memberList);
              }
            }
            break;
          }
          if (cd.conftimer <= 0) {
            cd.audioRelay.playSound("conf-no-admin");
            cd.conftimer = 60 * 8000 * 160;  //1min of silence
          } else {
            cd.conftimer -= 160;
          }
          break;
        case CONF_TALK:
          if (admincnt == 0) {
            cd.confstate = Conference.State.CONF_DROP;
          }
          //mix samples
          System.arraycopy(RTPRelay.silence, 0, sam, 0, 160);
          for(int a=0;a<memberList.size();a++) {
            member = memberList.get(a);
            if (member == myMember) continue;  //don't want to hear yourself
            if (myMember.idxs[a] == inc(member.idx)) {
              myMember.idxs[a] = dec(member.idx);  //reset to head-1
            }
            mix(sam, member.buf[myMember.idxs[a]]);
            myMember.idxs[a] = inc(myMember.idxs[a]);
          }
          break;
        case CONF_DROP:
          if (!myMember.dropped) {
            myMember.dropped = true;
            cd.audioRelay.playSound("conf-admin-left");
          }
          break;
      }
    }
  }

  /* Increment buffer index */
  private int inc(int in) {
    in++;
    if (in == Conference.bufs) {
      in = 0;
    }
    return in;
  }

  /* Decrement buffer index */
  private int dec(int in) {
    in--;
    if (in == -1) {
      in = Conference.bufs-1;
    }
    return in;
  }

  private void mix(short out[], short in[]) {
    //mix 'in' into 'out'
    for(int a=0;a<160;a++) {
      out[a] += in[a];
    }
  }

//private code
  protected void start(CallDetailsPBX cd) {
    String lang = "en";  //TODO : query for ext
    cd.ivrstate = State.IVR_NONE;
    cd.ivrstring = new StringBuffer();
    cd.ivrvars = new HashMap<String,String>();
    cd.audioRelay.setLang(lang);
    cd.audioRelay.setRawMode(false);
    SDP.Stream stream = cd.src.sdp.getFirstAudioStream();
    if (api.getExtension(cd.fromnumber) != null) {
      //NAT src
      stream.port = -1;
    }
    cd.audioRelay.start_src(stream);
    while (runScript(cd));
  }

  private String gettag(CallDetailsPBX cd) {
    int len = cd.ivrscript.length;
    if (cd.ivrtag == -1) return null;
    String tag;
    do {
      if (cd.ivrtag == len) return null;
      tag = cd.ivrscript[cd.ivrtag++];
    } while (tag.length() == 0);
    return tag;
  }

  private String peektag(CallDetailsPBX cd) {
    String tag = gettag(cd);
    cd.ivrtag--;
    return tag;
  }

  private void setvar(CallDetailsPBX cd, String key, String value) {
    api.log(cd, "setvar:" + key + "=" + value);
    cd.ivrvars.put(key, value);
  }

  private String getvar(CallDetailsPBX cd, String key) {
    return cd.ivrvars.get(key);
  }

  private boolean runScript(CallDetailsPBX cd) {
    String tag = gettag(cd);
    if (tag.length() == 0) {
      api.log(cd, "IVR error : tag = zero length");  //should never happen
      return true;
    }
    if (tag == null) {
      api.log(cd, "IVR reached end of script");
      hangup(cd);
      return false;
    }
    api.log(cd, "IVR:tag=" + tag);
    if (tag.charAt(0) == '$') {
      //assignment : $var = value
      String op = gettag(cd);
      if (op.equals("=")) {
        String value = gettag(cd);
        if (value.charAt(0) == '$') value = getvar(cd, value);
        String op2 = peektag(cd);
        if (op2.equals("+")) {
          op2 = gettag(cd);
          String value2 = gettag(cd);
          if (value2.charAt(0) == '$') value2 = getvar(cd, value2);
          value += value2;
        }
        setvar(cd, tag, value);
      } else {
        api.log(cd, "IVR:Unknown operation:"+tag+" "+op);
        hangup(cd);
        return false;
      }
      return true;
    }
    if (tag.equalsIgnoreCase("playmsg")) {
      cd.ivrstate = State.IVR_PLAY_MSG;
      cd.ivrint = true;
      String msg = gettag(cd);
      if (msg.charAt(0) == '$') msg = getvar(cd, msg);
      playSound(cd, msg);
      return false;
    }
    if (tag.equalsIgnoreCase("playmsgnoint")) {
      cd.ivrstate = State.IVR_PLAY_MSG;
      cd.ivrint = false;
      String msg = gettag(cd);
      if (msg.charAt(0) == '$') msg = getvar(cd, msg);
      playSound(cd, msg);
      return false;
    }
    if (tag.equalsIgnoreCase("getchar")) {
      cd.ivrvar = gettag(cd);
      if (cd.ivrstring.length() > 0) {
        //remove 1 char and assign it $var
        setvar(cd, cd.ivrvar, cd.ivrstring.substring(0, 1));
        cd.ivrstring.delete(0,cd.ivrstring.length());
        return true;
      }
      cd.ivrint = true;
      cd.ivrstate = State.IVR_GET_CHAR;
      playSound(cd, "vm-pause");
      return false;
    }
    if (tag.equalsIgnoreCase("getstring")) {
      cd.ivrvar = gettag(cd);
      int idx = cd.ivrstring.indexOf("#");
      if (idx != -1) {
        //assign it to $var
        setvar(cd, cd.ivrvar, cd.ivrstring.substring(0, idx));
        flush(cd.ivrstring);
        return true;
      }
      cd.ivrint = true;
      cd.ivrstate = State.IVR_GET_STRING;
      playSound(cd, "vm-pause");
      return false;
    }
    if (tag.equalsIgnoreCase("hangup")) {
      hangup(cd);
      return false;
    }
    if (tag.equalsIgnoreCase("goto")) {
      String target = gettag(cd);
      if (target.charAt(0) == '$') target = getvar(cd, target);
      cd.ivrtag = 0;
      do {
        tag = gettag(cd);
        if (tag.equals("label")) {
          tag = gettag(cd);
          if (tag.equalsIgnoreCase(target)) break;
        }
      } while (true);
      return true;
    }
    if (tag.equalsIgnoreCase("label")) {
      gettag(cd);  //ignore next tag
      return true;
    }
    if (tag.equalsIgnoreCase("if")) {
      String v1 = gettag(cd);
      if (v1.charAt(0) == '$') v1 = getvar(cd, v1);
      String op = gettag(cd);
      String v2 = gettag(cd);
      if (v2.equals("=")) {
        op += v2;
        v2 = gettag(cd);
      }
      if (v2.charAt(0) == '$') v2 = getvar(cd, v2);
      boolean res = false;
      int i1, i2;
      if ((op.equals("==")) || (op.equals("="))) {
        res = v1.equalsIgnoreCase(v2);
      } else if (op.equals("!=")) {
        res = !v1.equalsIgnoreCase(v2);
      } else if (op.equals("<")) {
        i1 = Integer.valueOf(v1);
        i2 = Integer.valueOf(v2);
        res = i1 < i2;
      } else if (op.equals(">")) {
        i1 = Integer.valueOf(v1);
        i2 = Integer.valueOf(v2);
        res = i1 > i2;
      } else if (op.equals("<=")) {
        i1 = Integer.valueOf(v1);
        i2 = Integer.valueOf(v2);
        res = i1 <= i2;
      } else if (op.equals(">=")) {
        i1 = Integer.valueOf(v1);
        i2 = Integer.valueOf(v2);
        res = i1 >= i2;
      } else {
        api.log(cd, "IVR:Unknown operation:"+op);
        hangup(cd);
        return false;
      }
      if (!res) {
        //look for ENDIF
        while (!(gettag(cd).equalsIgnoreCase("endif")));
      }
      return true;
    }
    if (tag.equalsIgnoreCase("endif")) {
      return true;
    }
    if (tag.equalsIgnoreCase("transfer")) {
      String target = gettag(cd);
      if (target.charAt(0) == '$') target = getvar(cd, target);
      cd.ivrstate = State.IVR_TRANSFER;
      api.transfer_src(cd, target);
      return false;
    }
    if (tag.equalsIgnoreCase("conf")) {
      //user enters conference mode
      tag = gettag(cd);
      if (tag.equals("admin")) {
        addMember(cd, true);
      } else if (tag.equals("user")) {
        addMember(cd, false);
      } else {
        api.log(cd, "IVR:conf command bad user type:"+tag);
        hangup(cd);
        return false;
      }
      return false;
    }
    if (tag.equalsIgnoreCase("enable")) {
      tag = gettag(cd);
      if (tag.equals("video")) {
        if (cd.src.sdp.getFirstVideoStream() != null) {
          cd.confVideo = true;
        }
      }
      return true;
    }
    if (tag.equalsIgnoreCase("disable")) {
      tag = gettag(cd);
      if (tag.equals("video")) cd.confVideo = false;
      return true;
    }
    api.log(cd, "IVR:Unknown command:"+tag);
    hangup(cd);
    return false;
  }

  private void hangup(CallDetailsPBX cd) {
    try {
      cd.ivrtag = -1;
      cd.cmd = "BYE";
      cd.pbxsrc.cseq++;
      JFLog.log("IVR:issuing BYE to:" + cd.user);
      api.issue(cd, null, false, true);
      JFLog.log("IVR:hangup:" + cd.user + ":state=" + cd.ivrstate);
      if (cd.ivrstate == State.IVR_CONF) {
        delMember(cd);
      }
      if (cd.audioRelay != null) {
        cd.audioRelay.uninit();
        cd.audioRelay = null;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void flush(StringBuffer sb) {
    sb.delete(0,sb.length());
  }

  private void playSound(CallDetailsPBX cd, String name) {
    cd.audioRelay.cleanup();  //stop
    cd.audioRelay.playSound(name);
  }

  /* Conference Code */

  private void addMember(CallDetailsPBX cd, boolean admin) {
    JFLog.log("Conf:" + cd.dialed + ":add member:" + cd.user + ":admin:" + admin);
    Conference.Member member = new Conference.Member();
    cd.confmember = member;
    member.cd = cd;
    member.admin = admin;
    int siz;
    synchronized(Conference.lock) {
      //check conf list for cd.dialed
      Vector<Conference.Member> memberList = Conference.list.get(cd.dialed);
      if (memberList == null) {
        //create new list
        memberList = new Vector<Conference.Member>();
        Conference.list.put(cd.dialed, memberList);
      }
      member.memberList = memberList;
      memberList.add(member);
      //add new member to all members jitter idxs buffers
      siz = memberList.size();
      for(int a=0;a<siz;a++) {
        member = memberList.get(a);
        if (member.idxs == null) {
          member.idxs = new int[siz];
        } else {
          member.idxs = Arrays.copyOf(member.idxs, siz);
        }
      }
    }
    cd.confstate = Conference.State.CONF_WAIT;
    cd.ivrstate = State.IVR_CONF;
    JFLog.log("Conf:" + cd.dialed + ":add member:" + cd.user + ":admin:" + admin + ":size=" + siz);
  }

  private void delMember(CallDetailsPBX cd) {
    JFLog.log("Conf:" + cd.dialed + ":del member:" + cd.user + ":admin:" + cd.confmember.admin);
    if (cd.confmember.dropped) {
      JFLog.log("Conf:" + cd.dialed + ":del member:" + cd.user + ":admin:" + cd.confmember.admin + ":already done");
      return;  //already done
    }
    cd.confmember.dropped = true;
    boolean admindrop = cd.confmember.admin;
    Conference.Member member;
    int actcnt = 0;
    int admincnt = 0;
    synchronized(Conference.lock) {
      Vector<Conference.Member> memberList = Conference.list.get(cd.dialed);
      int idx = memberList.indexOf(cd.confmember);
      if (idx == -1) {
        JFLog.log("Conf:del member:failed:idx==-1");
        return;
      }
      memberList.remove(cd.confmember);
      int size = memberList.size();
      for(int a=0;a<size;a++) {
        member = memberList.get(a);
        if (!member.dropped) {
          actcnt++;
          if (member.admin) admincnt++;
          if (member.cd.confVideo) {
            reInviteVideo(member.cd, member, memberList);
          }
        }
        if (member.idxs == null) continue;
        member.idxs = JF.copyOfExcluding(member.idxs, idx);
      }
      if (actcnt == 0) {
        //last member dropped
        Conference.list.remove(cd.dialed);
        JFLog.log("Conf:" + cd.dialed + ":del member:last member dropped");
        return;
      }
      if (admindrop && admincnt == 0) {
        //last admin dropped - drop all other users (CONF_DROP)
        JFLog.log("Conf:" + cd.dialed + ":del member:last admin dropped:dropping other users");
        for(int a=0;a<memberList.size();a++) {
          member = memberList.get(a);
          member.cd.confstate = Conference.State.CONF_DROP;
        }
      }
    }
    JFLog.log("Conf:" + cd.dialed + ":del member:" + cd.user + ":admin:" + cd.confmember.admin + ":actcnt=" + actcnt);
  }

  private void reInviteVideo(CallDetailsPBX cd, Conference.Member myMember, Vector<Conference.Member> memberList) {
    //NOTE:already have Conference.lock
    //need to send a reINVITE to user to add video streams with other members
    SDP sdp = new SDP();
    sdp.streams = new SDP.Stream[1];
    sdp.streams[0] = cd.pbxsrc.sdp.getFirstAudioStream();  //keep audio stream
    sdp.ip = cd.pbxsrc.sdp.ip;
    cd.pbxsrc.sdp = sdp;
    //now add video streams
    SDP.Stream stream;
    int size = memberList.size();
    for(int a=0;a<size;a++) {
      Conference.Member member = memberList.get(a);
      if (member != myMember && (member.cd.src.sdp == null || !member.cd.src.sdp.hasVideo())) continue;
      stream = sdp.addStream(SDP.Type.video);
      if (member == myMember) {
        stream.mode = SDP.Mode.inactive;  //so there is at least one stream to start video camera
        stream.ip = "0.0.0.0";
        stream.port = 0;
      } else {
        stream.mode = SDP.Mode.sendrecv;
        stream.ip = member.cd.src.sdp.getFirstVideoStream().getIP();
        stream.port = member.cd.src.sdp.getFirstVideoStream().getPort();
      }
      stream.codecs = getVideoCodecs();
      stream.content = member.cd.user;
    }
    JFLog.log("IVR:Video Conference:Reinvite " + cd.user + " with " + (sdp.streams.length - 1) + " video streams");
    cd.cmd = "INVITE";
    cd.sip.buildsdp(cd, cd.pbxsrc);
    api.issue(cd, null, true, true);
  }

  private Codec[] getVideoCodecs() {
    String cfg = api.getCfg("videoCodecs");
    if (cfg == null || cfg.length() == 0) cfg = "H264,VP8";
    String cs[] = cfg.split(",");
    Codec codecs[] = new Codec[cs.length];
    for(int a=0;a<cs.length;a++) {
      if (cs[a].equals("JPEG")) {codecs[a] = RTP.CODEC_JPEG; continue;}
      if (cs[a].equals("H263")) {codecs[a] = RTP.CODEC_H263; continue;}
      if (cs[a].equals("H263-1998")) {codecs[a] = RTP.CODEC_H263_1998; continue;}
      if (cs[a].equals("H263-2000")) {codecs[a] = RTP.CODEC_H263_2000; continue;}
      if (cs[a].equals("H264")) {codecs[a] = RTP.CODEC_H264; continue;}
      if (cs[a].equals("VP8")) {codecs[a] = RTP.CODEC_VP8; continue;}
      codecs[a] = RTP.CODEC_UNKNOWN;
    }
    return codecs;
  }
}

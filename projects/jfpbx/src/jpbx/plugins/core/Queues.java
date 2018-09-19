package jpbx.plugins.core;

/** Queues (ACD : Automatic Call Distributor)
 *
 * @author pquiring
 *
 * Created : Jun 27, 2014
 */

import java.util.*;

import javaforce.*;
import javaforce.voip.*;

import jpbx.core.*;

public class Queues implements Plugin, DialChain, PBXEventHandler {
  public final static int pid = 25;  //priority
  private static PBXAPI api;

  public enum MemberState {
    WELCOME, WAITING, CALLING, CONNECTED
  }

  public static class Queue {
    public ArrayList<CallDetailsPBX> queue = new ArrayList<CallDetailsPBX>();
    public String agentList[];
    public String ext;
    public void add(CallDetailsPBX cd) {
      synchronized(this) {
        queue.add(cd);
      }
    }
    public void remove(CallDetailsPBX cd) {
      synchronized(this) {
        queue.remove(cd);
      }
    }
    public void process() {
      synchronized(this) {
        if (queue.isEmpty()) {
          return;
        }
        callAgents();
      }
    }
    private void callAgents() {
      CallDetailsPBX member = queue.get(0);
      switch (member.qstate) {
        case WELCOME:
          return;
        case WAITING:
          member.qstate = MemberState.CALLING;
          break;
      }
      if (member.agents == null) {
        member.agents = new CallDetailsPBX[agentList.length];
      }
      if (member.pids == null) {
        member.pids = new int[agentList.length];
      }
      SQL sql = new SQL();
      if (!sql.connect(Service.jdbc)) return;
      for(int a=0;a<agentList.length;a++) {
        if (agentList[a].length() == 0) continue;
        JFLog.log("Calling agent:" + agentList[a]);
        if (member.agents[a] != null) continue;
        //call agents using dial chain (see Extension/Trunks)
        String callid = member.sip.getcallid();  //generate a new callid
        CallDetailsPBX agent = (CallDetailsPBX)member.sip.getCallDetailsServer(callid);
        agent.isAgent = true;
        agent.member = member;
        agent.user = member.user;
        agent.dialed = agentList[a];
        agent.src.contact = member.src.contact;
        agent.src.branch = member.src.branch;
        agent.src.to = new String[] {"Agent", agentList[a], "127.0.0.1"};
        agent.src.from = member.src.from.clone();
        agent.src.host = "127.0.0.1";
        agent.src.port = api.getlocalport();
        agent.src.sdp = (SDP)member.src.sdp.clone();
        agent.pbxsrc = (CallDetails.SideDetails)agent.src.clone();
        agent.authorized = true;
        agent.queue = member.queue;
        agent.cmd = "INVITE";
        member.pids[a] = api.onInvite(agent, sql, true, 0);  //simulate call from PBX to agent
        //BUG : another inbound msg here could bypass Queues control
        agent.pid = pid;
        member.agents[a] = agent;
      }
      sql.close();
    }
  }

  private HashMap<String, Queue> queues = new HashMap<String, Queue>();
  private Timer timer;

  public void init(PBXAPI api) {
    this.api = api;
    api.hookDialChain(this);
    timer = new Timer();
    timer.schedule(new TimerTask() {public void run() {
      synchronized(queues) {
        Queue list[] = queues.values().toArray(new Queue[queues.size()]);
        for(int a=0;a<list.length;a++) {
          list[a].process();
        }
      }
    }}, 30 * 1000, 30 * 1000);
    JFLog.log("Queues plugin init");
  }

  public void uninit(PBXAPI api) {
    timer.cancel();
    timer = null;
    api.unhookDialChain(this);
    JFLog.log("Queues plugin uninit");
  }

  public void install(PBXAPI api) {
  }

  public void uninstall(PBXAPI api) {
  }

  public int getPriority() {
    return pid;
  }

  public int onInvite(CallDetailsPBX cd, SQL sql, boolean src) {
    //NOTE : there is no dst - it's a one-sided call
    if (!src) return -1;
/*
    if (!cd.authorized) {
      if (!cd.anon) return -1;
    }
*/
    String ext = sql.select1value("SELECT ext FROM queues WHERE ext=" + sql.quote(cd.dialed));
    if (ext == null) return -1;  //a queue is not being dialed
    if (cd.invited) {
      //reINVITE (just get new codecs)
      api.log(cd, "ACD : reINVITE");
      SDP.Stream astream = cd.src.sdp.getFirstAudioStream();
      cd.audioRelay.change_src(astream);
      cd.pbxsrc.sdp.getFirstAudioStream().codecs = cd.src.sdp.getFirstAudioStream().codecs;
      cd.sip.buildsdp(cd, cd.pbxsrc);
      api.reply(cd, 200, "OK", null, true, true);
      return pid;
    }
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
    astream.port = cd.audioRelay.getPort_src();
    astream.codecs = cd.src.sdp.getFirstAudioStream().codecs;
    cd.sip.buildsdp(cd, cd.pbxsrc);
    cd.invited = true;
    cd.connected = true;
    cd.pbxsrc.to = SIP.replacetag(cd.pbxsrc.to, SIP.generatetag());  //assign tag
    api.reply(cd, 200, "OK", null, true, true);
    start(cd, sql);
    return pid;
  }

  public void onRinging(CallDetailsPBX cd, SQL sql, boolean src) {
  }

  public void onSuccess(CallDetailsPBX cd, SQL sql, boolean src) {
    try {
      if (cd.isAgent) {
        int idx = -1;
        CallDetailsPBX agent = cd;
        CallDetailsPBX member = cd.member;
        synchronized(member.queue) {
          for(int a=0;a<member.agents.length;a++) {
            if (member.agents[a] == agent) {
              idx = a;
              break;
            }
          }
          if (idx == -1) {
            JFLog.log(pid, "Lost track of agents???");
            return;
          }

          if (member.agent == agent) {
            //dup msg
            return;
          }
          api.onSuccess(cd, sql, src, member.pids[idx]);  //this will generate dup msg
          if (member.qstate == MemberState.CALLING) {
            //connect current member to agent
            member.qstate = MemberState.CONNECTED;
            member.audioRelay.setRawMode(true);  //switch to relay mode
            api.connect(member, agent);
            member.audioRelay.setMOH(false);
            member.queue.remove(member);
            member.agent = agent;
            //send cancel to remaining agents
            for(int a=0;a<member.agents.length;a++) {
              if (a == idx) continue;
              agent = member.agents[a];
              if (agent == null) continue;
              agent.cmd = "CANCEL";
              agent.dst.cseq++;
              api.issue(agent, null, false, false);
              member.agents[a] = null;
            }
          } else {
            //agent too late - send bye
            member.agents[idx] = null;
            cd.cmd = "BYE";
            cd.dst.cseq++;
            api.issue(cd, null, false, false);
          }
          member.queue.process();
        }
      } else {
        JFLog.log("Unknown???");
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void onCancel(CallDetailsPBX cd, SQL sql, boolean src) {
    onBye(cd, sql, src);
  }

  public void onBye(CallDetailsPBX cd, SQL sql, boolean src) {
    if (cd.queue == null) return;
    synchronized(cd.queue) {
      if (src) {
        if (cd.isAgent) {
          JFLog.log("Assertion : Queue Agent must be dst");
          return;
        }
        //member bye
        api.reply(cd, 200, "OK", null, false, true);
        if (cd.agent != null) {
          cd.agent.cmd = "BYE";
          cd.agent.dst.cseq++;
          api.issue(cd.agent, null, false, false);
          api.disconnect(cd.agent);
          cd.agent = null;
        }
        api.disconnect(cd);
      } else {
        if (!cd.isAgent) {
          JFLog.log("Assertion : Queue Member must be src");
          return;
        }
        //agent bye
        api.reply(cd, 200, "OK", null, false, false);
        api.disconnect(cd);
        for(int a=0;a<cd.member.agents.length;a++) {
          if (cd.member.agents[a] == cd) {
            cd.member.agents[a] = null;
            break;
          }
        }
        if (cd.member.qstate != MemberState.CONNECTED) return;
        if (cd.member.agent != cd) return;
        cd.member.cmd = "BYE";
        cd.member.src.cseq++;
        api.issue(cd.member, null, false, true);
        api.disconnect(cd.member);
        cd = cd.member;
      }
      //now disconnect all remaining agents and remove from queue
      if (cd.agents == null) return;
      for(int a=0;a<cd.agents.length;a++) {
        CallDetailsPBX agent = cd.agents[a];
        if (agent == null) continue;
        agent.cmd = "CANCEL";
        agent.dst.cseq++;
        api.issue(agent, null, false, false);
        cd.agents[a] = null;
      }
      cd.queue.remove(cd);
    }
  }

  public void onError(CallDetailsPBX cd, SQL sql, int code, boolean src) {
    onBye(cd, sql, src);
  }

  public void onTrying(CallDetailsPBX cd, SQL sql, boolean src) {
  }

  public void onFeature(CallDetailsPBX cd, SQL sql, String cmd, String cmddata, boolean src) {
  }

  //PBXEventHandler
  public void event(CallDetailsPBX cd, int type, char digit, boolean interrupted) {
    try {
      switch (type) {
        case PBXEventHandler.SOUND:
          if (cd.qstate == MemberState.WELCOME) {
            cd.qstate = MemberState.WAITING;
            cd.audioRelay.setMOH(true);  //play MOH until an agent picks up
            cd.queue.process();
          }
          break;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void samples(CallDetailsPBX cd, short[] sam) {}

  //private code
  private void start(CallDetailsPBX cd, SQL sql) {
    String lang = "en";  //TODO : query for ext
    cd.qstate = MemberState.WELCOME;
    String message = null;
    synchronized(queues) {
      Queue q = queues.get(cd.dialed);
      String list[] = sql.select1value("SELECT agents FROM queues WHERE ext=" + sql.quote(cd.dialed)).split(",");
      message = sql.select1value("SELECT message FROM queues WHERE ext=" + sql.quote(cd.dialed));
      if (q == null) {
        q = new Queue();
        q.ext = cd.dialed;
        q.agentList = list;
        queues.put(cd.dialed, q);
      } else {
        q.agentList = list;  //update
      }
      q.queue.add(cd);
      cd.queue = q;
    }
    cd.audioRelay.setLang(lang);
    cd.audioRelay.setRawMode(false);
    if (api.getExtension(cd.fromnumber) != null) {
      //NAT src
      cd.src.sdp.getFirstAudioStream().port = -1;
    }
    cd.audioRelay.start_src(cd.src.sdp.getFirstAudioStream());
    cd.audioRelay.playSound(message);
  }
}

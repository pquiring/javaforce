package jfpbx.core;

import jfpbx.db.TrunkRow;
import javaforce.*;
import javaforce.voip.*;


/** Low-level and high-level APIs used by Plugins for processing SIP messages. */

public interface PBXAPI {
  public void hookDialChain(DialChain chain);
  public void unhookDialChain(DialChain chain);
  public void schedule(Runnable plugin, int minutes);
  public void unschedule(Runnable plugin);
  public boolean isRegistered(String ext);
  public boolean issue(CallDetailsServer cd, String header, boolean sdp, boolean src);
  public boolean reply(CallDetailsServer cd, int code, String msg, String header, boolean sdp, boolean src);
  public boolean transfer_src(CallDetailsPBX cd, String number);
  public boolean transfer_dst(CallDetailsPBX cd, String number);
  public Extension getExtension(String ext);
  public String getlocalhost(CallDetailsPBX cd);
  public int getlocalport();
  public TrunkRow[] getTrunks(Dial dialed, String ext);
  public String resolve(String host);  //resolve host to ip address
  public CallDetailsServer createCallDetailsServer();
  public void releaseCallDetails(CallDetails cd);
  public String patternMatches(String pattern, String dialed);
  public void log(CallDetailsPBX cd, String msg);
  public void log(CallDetailsPBX cd, Exception e);
  public DialChain getDialChain(int pid);
  public void makePath(String path);
  public String convertString(String str);  //convert web string (%##) (+ -> ' ')
  public String getCfg(String id);
  public SIPServerInterface getSIPServerInterface();

  public int onInvite(CallDetailsServer cd, boolean src, int pid);
  public void onCancel(CallDetailsServer cd, boolean src, int pid);
  public void onError(CallDetailsServer cd, int code, boolean src, int pid);
  public void onBye(CallDetailsServer cd, boolean src, int pid);
  public void onSuccess(CallDetailsServer cd, boolean src, int pid);
  public void onRinging(CallDetailsServer cd, boolean src, int pid);
  public void onTrying(CallDetailsServer cd, boolean src, int pid);
  public void onFeature(CallDetailsServer cd, String cmd, String data, boolean src, int pid);
  public void onMessage(CallDetailsServer cd, String from, String to, String[] msg, boolean src, int pid);

  /** Connects src with dst end points.
   * Creates RTP relays as needed.
   */
  public void connect(CallDetailsPBX cd);
  /** Disconnects src and dst end points.
   * Removes RTP relays.
   */
  public void disconnect(CallDetailsPBX cd);
  /** Connects cd1.src with cd2.dst end points.
   * It does this by swapping cd1.dst and cd2.dst.
   */
  public void connect(CallDetailsPBX cd1, CallDetailsPBX cd2);
}

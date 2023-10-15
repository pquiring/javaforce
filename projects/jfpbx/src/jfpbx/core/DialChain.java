package jfpbx.core;

import javaforce.*;

public interface DialChain {
  public int getPriority();  //pid value from 1-100
  /** Invoked for each inbound INVITE message.
   * Return -1 if not related, or pid to mark call.
   * If marked then all subsequent messages for this call leg are directly called.
   */
  public int onInvite(CallDetailsPBX cd, boolean src);
  public void onRinging(CallDetailsPBX cd, boolean src);
  public void onSuccess(CallDetailsPBX cd, boolean src);
  public void onCancel(CallDetailsPBX cd, boolean src);
  public void onBye(CallDetailsPBX cd, boolean src);
  public void onError(CallDetailsPBX cd, int code, boolean src);
  public void onTrying(CallDetailsPBX cd, boolean src);
  public void onFeature(CallDetailsPBX cd, String cmd, String cmddata, boolean src);
  public boolean onMessage(CallDetailsPBX cd, String from, String to, String msg, boolean src);
}

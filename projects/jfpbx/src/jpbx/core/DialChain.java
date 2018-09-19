package jpbx.core;

import javaforce.*;

public interface DialChain {
  public int getPriority();  //pid value from 1-100
  /** Invoked for each inbound INVITE message.
   * Return -1 if not related, or pid to mark call.
   * If marked then all subsequent messages for this call leg are directly called.
   */
  public int onInvite(CallDetailsPBX cd, SQL sql, boolean src);
  public void onRinging(CallDetailsPBX cd, SQL sql, boolean src);
  public void onSuccess(CallDetailsPBX cd, SQL sql, boolean src);
  public void onCancel(CallDetailsPBX cd, SQL sql, boolean src);
  public void onBye(CallDetailsPBX cd, SQL sql, boolean src);
  public void onError(CallDetailsPBX cd, SQL sql, int code, boolean src);
  public void onTrying(CallDetailsPBX cd, SQL sql, boolean src);
  public void onFeature(CallDetailsPBX cd, SQL sql, String cmd, String cmddata, boolean src);
}

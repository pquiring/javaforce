package javaforce.voip;

/**
 * Callback interface for handling SIP messages for a SIP server.
 */
public interface SIPServerInterface {

  public CallDetailsServer createCallDetailsServer();  //allow expanding class

  public String getPassword(String user);

  public String getTrunkRegister(String ip);

  public void onRegister(String user, int expires, String remoteip, int remoteport);

  public void onOptions(CallDetailsServer cd, boolean src);

  public void onInvite(CallDetailsServer cd, boolean src);

  public void onCancel(CallDetailsServer cd, boolean src);

  public void onError(CallDetailsServer cd, int code, boolean src);

  public void onBye(CallDetailsServer cd, boolean src);

  public void onSuccess(CallDetailsServer cd, boolean src);

  public void onRinging(CallDetailsServer cd, boolean src);

  public void onTrying(CallDetailsServer cd, boolean src);

  public void onFeature(CallDetailsServer cd, String cmd, String data, boolean src);
}

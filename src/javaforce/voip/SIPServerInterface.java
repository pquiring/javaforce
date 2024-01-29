package javaforce.voip;

/**
 * Callback interface for handling SIP messages for a SIP server.
 */
public interface SIPServerInterface {

  /** Create a new CallDetailsServer instance.
   * Allows server to return a derived class of CallDetailsServer.
   */
  public CallDetailsServer createCallDetailsServer();

  /** Get password for user. */
  public String getPassword(String user);

  /** Get trunk register string for IP. */
  public String getTrunkRegister(String ip);

  /** Client registration. */
  public void onRegister(String user, int expires, String remoteip, int remoteport);

  /** Client requests options. */
  public void onOptions(CallDetailsServer cd, boolean src);

  /** Client initiates a call. */
  public void onInvite(CallDetailsServer cd, boolean src);

  /** Client cancels operation. */
  public void onCancel(CallDetailsServer cd, boolean src);

  /** Client indicates error occurred. */
  public void onError(CallDetailsServer cd, int code, boolean src);

  /** Client ends call. */
  public void onBye(CallDetailsServer cd, boolean src);

  /** Client indicates a success*/
  public void onSuccess(CallDetailsServer cd, boolean src);

  /** Client indicates a call is ringing. */
  public void onRinging(CallDetailsServer cd, boolean src);

  /** Client indicates attempt to complete call. */
  public void onTrying(CallDetailsServer cd, boolean src);

  /** Client issues a feature.
   * Supported : REFER, SHUTDOWN. */
  public void onFeature(CallDetailsServer cd, String cmd, String data, boolean src);

  /** Client issues an instant message. */
  public void onMessage(CallDetailsServer cd, String from, String to, String[] msg, boolean src);
}

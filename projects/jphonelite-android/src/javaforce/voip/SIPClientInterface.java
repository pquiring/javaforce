package javaforce.voip;

/**
 * Callback interface for handling SIP messages for a SIP client.
 */
public interface SIPClientInterface {

  /**
   * Registration status.
   */
  public void onRegister(SIPClient client, boolean status);

  /**
   * Trunk is trying call. (100)
   */
  public void onTrying(SIPClient client, String callid);

  /**
   * Call is ringing. (180/183)
   */
  public void onRinging(SIPClient client, String callid);

  /**
   * Call is successful.
   */
  public void onSuccess(SIPClient client, String callid, String remotertphost, int remotertpport, int remoteVrtpport, Codec codecs[]);

  /**
   * Call was terminated from other side.
   */
  public void onBye(SIPClient client, String callid);

  /**
   * Incoming invite, must return SIP code : 180 (ringing), 200 (connect),
   * 486(busy), or -1 to do nothing.
   */
  public int onInvite(SIPClient client, String callid, String fromid, String fromnumber, String remotertphost, int remotertpport, int remoteVrtpport, Codec codecs[]);

  /**
   * Call was cancelled/failed with SIP status code.
   */
  public void onCancel(SIPClient client, String callid, int code);

  /**
   * Call refer (transfer) was successful.
   */
  public void onRefer(SIPClient client, String callid);

  /**
   * Server send notify command. (event="message-summary" or "presence")
   */
  public void onNotify(SIPClient client, String event, String content);

  /**
   * Returns MD5 authorization response if password was not given to SIPClient
   * during init.
   */
  public String getResponse(SIPClient client, String realm, String cmd, String uri, String nonce, String qop, String nc, String cnonce);
}

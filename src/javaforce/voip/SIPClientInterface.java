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
   * Call is ringing. (180)
   */
  public void onRinging(SIPClient client, String callid);

  /**
   * Call is successful or making progress.
   * if complete=true then call is successful (SIP 200)
   * if complete=false then call is making progress (SIP 183) (usually for ringback tones)
   */
  public void onSuccess(SIPClient client, String callid, SDP sdp, boolean complete);

  /**
   * Call was terminated from other side.
   */
  public void onBye(SIPClient client, String callid);

  /**
   * Incoming invite, must return SIP code : 180 (ringing), 200 (connect),
   * 486(busy), or -1 to do nothing.
   */
  public int onInvite(SIPClient client, String callid, String fromid, String fromnumber, SDP sdp);

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
  public void onNotify(SIPClient client, String callid, String event, String[] content);

  /**
   * Ack message (may contain SDP data)
   */
  public void onAck(SIPClient client, String callid, SDP sdp);

  /**
   * Instant message received.
   */
  public void onMessage(SIPClient client, String callid, String fromid, String fromnumber, String[] msg);
}

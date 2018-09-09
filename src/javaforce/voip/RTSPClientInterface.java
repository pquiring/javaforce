package javaforce.voip;

/** RTSP Client Interface
 *
 * @author User
 */

public interface RTSPClientInterface {
  public void onOptions(SIPClient client);
  public void onDescribe(SIPClient client, SDP sdp);
  public void onSetup(SIPClient client);
  public void onPlay(SIPClient client);
  public void onTeardown(SIPClient client);
}

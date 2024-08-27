package javaforce.voip;

/** RTSP Client Interface
 *
 * @author User
 */

public interface RTSPClientInterface {
  public void onOptions(RTSPClient client);
  public void onDescribe(RTSPClient client, SDP sdp);
  public void onSetup(RTSPClient client);
  public void onPlay(RTSPClient client);
  public void onTeardown(RTSPClient client);
  public void onGetParameter(RTSPClient client, String[] params);
  public void onSetParameter(RTSPClient client, String[] params);
}

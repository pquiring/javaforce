package javaforce.voip;

/** RTSP Server Interface
 *
 * @author User
 */

public interface RTSPServerInterface {
  public String getPassword(String user);
  public void onOptions(RTSPServer client);
  public void onDescribe(RTSPServer client);
  public void onSetup(RTSPServer client);
  public void onPlay(RTSPServer client);
  public void onTeardown(RTSPServer client);
}

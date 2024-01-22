package javaforce.voip;

/** RTSP Server Interface
 */

public interface RTSPServerInterface {
  public String getPassword(String user);
  public void onOptions(RTSPServer server, RTSPSession session);
  public void onDescribe(RTSPServer server, RTSPSession session);
  public void onSetup(RTSPServer server, RTSPSession session);
  public void onPlay(RTSPServer server, RTSPSession session);
  public void onTeardown(RTSPServer server, RTSPSession session);
}

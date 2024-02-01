package javaforce.voip;

/**
 * Handles RTSP packets directly.
 */
public interface RTSPInterface {
  public void onPacket(RTSP rtsp, String[] msg, String remoteip, int remoteport);
  public void onConnect(RTSP rtsp, String remoteip, int remoteport);
  public void onDisconnect(RTSP rtsp, String remoteip, int remoteport);
}

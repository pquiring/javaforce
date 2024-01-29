package javaforce.voip;

/**
 * Handles RTSP packets directly.
 */
public interface RTSPInterface {
  public void packet(String[] msg, String remoteip, int remoteport);
}

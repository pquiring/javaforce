package javaforce.voip;

/**
 * Handles SIP packets directly.
 */
public interface SIPInterface {
  public void packet(String[] msg, String remoteip, int remoteport);
}

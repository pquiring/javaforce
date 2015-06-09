package javaforce.voip;

/**
 * Handles SIP packets directly.
 */
public interface SIPInterface {

  public void packet(String msg[], String remoteip, int remoteport);

  public String getResponse(String realm, String cmd, String uri, String nonce, String qop, String nc, String cnonce);
}

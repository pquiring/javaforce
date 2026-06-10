package javaforce.service;

/** WebSocketHandler
 *
 * @author pquiring
 *
 * Created : Jan 4, 2014
 */

public interface WebSocketHandler {

  /** Accept inbound socket connection. */
  public static final int REJECT = 0;
  /** Reject inbound socket connection. */
  public static final int ACCEPT = 1;
  /** Accept but detach from inbound socket connection. */
  public static final int DETACH = 2;

  /** Return ACCEPT, REJECT or DETACH. Always return REJECT if not using web sockets. */
  public int doWebSocketConnect(WebSocket sock);
  /** Triggered when a WebSocket connection has closed. */
  public void doWebSocketClosed(WebSocket sock);
  /** Process a WebSocket message. */
  public void doWebSocketMessage(WebSocket sock, byte[] data, int type);
}

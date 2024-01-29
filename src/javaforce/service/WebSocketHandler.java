package javaforce.service;

/** WebSocketHandler
 *
 * @author pquiring
 *
 * Created : Jan 4, 2014
 */

public interface WebSocketHandler {
  /** Return true/false to accept connection.  Always return false if not using WebSockets. */
  public boolean doWebSocketConnect(WebSocket sock);
  /** Triggered when a WebSocket connection has closed. */
  public void doWebSocketClosed(WebSocket sock);
  /** Process a WebSocket message. */
  public void doWebSocketMessage(WebSocket sock, byte[] data, int type);
}

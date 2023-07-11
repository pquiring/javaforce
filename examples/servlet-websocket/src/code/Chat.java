package code;

/**
 * This chat example shows how to "push" data to web clients using WebSockets.
 * Very powerful.
 *
 */

import java.io.*;
import java.util.*;

import javax.websocket.*;
import javax.websocket.server.*;

@ServerEndpoint(value = "/ws")
public class Chat {

  private static final String GUEST_PREFIX = "Guest";
  private static final ArrayList<Chat> connections = new ArrayList<>();

  private final String nickname;
  private Session session;
  private static Object lock = new Object();
  private static int id;

  public Chat() {
    synchronized (lock) {
      nickname = GUEST_PREFIX + id++;
    }
  }

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
    connections.add(this);
    String message = String.format("* %s %s", nickname, "has joined.");
    broadcast(message);
  }


  @OnClose
  public void onClose() {
    connections.remove(this);
    String message = String.format("* %s %s", nickname, "has disconnected.");
    broadcast(message);
  }


  @OnMessage
  public void onMessage(String message) {
    broadcast(nickname + ":" + message);
  }


  @OnError
  public void onError(Throwable t) throws Throwable {
    //log.error("Chat Error: " + t.toString(), t);
  }

  private static void broadcast(String msg) {
    for (Chat client : connections) {
      try {
        synchronized (client) {
          client.session.getBasicRemote().sendText(msg);
        }
      } catch (Exception e) {
        //log.debug("Chat Error: Failed to send message to client", e);
        connections.remove(client);
        try {
          client.session.close();
        } catch (IOException e1) {
          // Ignore
        }
        String message = String.format("* %s %s", client.nickname, "has been disconnected.");
        broadcast(message);
      }
    }
  }
}

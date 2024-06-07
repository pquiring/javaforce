package javaforce;

/** SSH Client
 *
 * @author peterq.admin
 */

import java.io.*;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;

public class SSH {
  private SshClient client;
  private ClientSession session;
  private ClientChannel channel;

  private InputStream in;
  private OutputStream out;

  public boolean connect(String host, int port, String username, String password) {
    Object[] pipes;
    try {
      client = SshClient.setUpDefaultClient();
      client.start();
      ConnectFuture cf = client.connect(username, host, port);
      session = cf.verify().getSession();
      if (true) {
        session.addPasswordIdentity(password);
      } else {
        //TODO : support key
        //session.addPublicKeyIdentity(sd.sshKey);
      }
      session.auth().verify(30000);
      channel = session.createChannel(ClientChannel.CHANNEL_SHELL);
      if (false) {
        // Enable X11 forwarding
        //channel.setXForwarding(true);
        JFLog.log("TODO : enable x11 forwarding");
      }
      if (false) {
        // Enable agent-forwarding
        //channel.setAgentForwarding(true);
        JFLog.log("TODO : enable agent forwarding");
      }
      pipes = createPipes();
      if (pipes == null) return false;
      out = (OutputStream)pipes[1];
      channel.setIn((InputStream)pipes[0]);
      pipes = createPipes();
      if (pipes == null) return false;
      in = (InputStream)pipes[0];
      channel.setOut((OutputStream)pipes[1]);
      channel.open();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public void disconnect() {
    try {if (client != null) {client.close(); client = null;}} catch(Exception e) {}
    try {if (out != null) {out.close(); out = null;}} catch(Exception e) {}
    try {if (in != null) {in.close(); in = null;}} catch(Exception e) {}
  }

  public OutputStream getOutputStream() {
    return out;
  }

  public InputStream getInputStream() {
    return in;
  }

  private Object[] createPipes() {
    Object[] ret = new Object[2];
    try {
      ret[0] = new PipedInputStream();
      ret[1] = new PipedOutputStream((PipedInputStream)ret[0]);
      return ret;
    } catch (Exception e) {
      return null;
    }
  }
}

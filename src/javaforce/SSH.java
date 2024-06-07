package javaforce;

/** SSH Client
 *
 * @author peterq.admin
 */

import java.io.*;
import java.util.*;

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
  private Object[] pipes;

  public boolean connect(String host, int port, String username, String password) {
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
      JFLog.log(e);
      return false;
    }
  }

  public void disconnect() {
    try {if (client != null) {client.close(); client = null;}} catch(Exception e) {}
    try {if (out != null) {out.close(); out = null;}} catch(Exception e) {}
    try {if (in != null) {in.close(); in = null;}} catch(Exception e) {}
  }

  public boolean connected() {
    return client.isOpen();
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

  public static void usage() {
    System.out.println("jfssh [user@]host[:port] [-p port]");
    System.exit(1);
  }

  public static void error(String msg) {
    System.out.println("Error:" + msg);
    System.exit(2);
  }

  /** SSH cli client */
  public static void main(String[] args) {
    String dest = null;
    int port = 22;
    ArrayList<String> cmd = new ArrayList<>();
    String argtype = null;
    for(String arg : args) {
      if (argtype != null) {
        switch (argtype) {
          case "-p": port = Integer.valueOf(arg); break;
        }
        argtype = null;
        continue;
      }
      if (arg.startsWith("-")) {
        switch (arg) {
          case "-p": argtype = arg; break;
          default: usage();
        }
      } else {
        if (dest == null) {
          dest = arg;
        } else {
          cmd.add(arg);
        }
      }
    }
    if (dest == null) usage();
    String user = null;
    String host = null;
    int idx = dest.indexOf('@');
    if (idx == -1) {
      host = dest;
    } else {
      user = dest.substring(0, idx);
      host = dest.substring(idx + 1);
    }
    idx = host.indexOf(':');
    if (idx != -1) {
      port = Integer.valueOf(host.substring(idx + 1));
      host = host.substring(0, idx);
    }
    System.out.print("Enter Password:");
    String pass = new String(System.console().readPassword());
    SSH ssh = new SSH();
    if (!ssh.connect(host, port, user, pass)) {
      error("Connection failed");
    }
    //connect input/output relay agents
    Condition connected = () -> {return ssh.connected();};
    RelayStream rs1 = new RelayStream(Console.getInputStream(), ssh.getOutputStream(), connected);
    RelayStream rs2 = new RelayStream(ssh.getInputStream(), Console.getOutputStream(), connected);
    rs1.start();
    rs2.start();
    try {
      rs1.join();
      rs2.join();
    } catch (Exception e) {
      //JFLog.log(e);
    }
  }
}

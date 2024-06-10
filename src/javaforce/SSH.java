package javaforce;

/** SSH Client
 *
 * @author peterq.admin
 */

import java.io.*;
import java.util.*;
import java.security.*;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;

public class SSH {

  /** SSH Connection options. */
  public static class Options {
    public int type;
    public String username;
    public String password;
    public KeyMgmt keys;
    public String keyalias;
    public String keypass;
    public String command;  //if type == EXEC
  }

  public static final int TYPE_SHELL = 0;
  public static final int TYPE_EXEC = 1;
  public static final int TYPE_SUBSYSTEM = 2;

  private SshClient client;
  private ClientSession session;
  private ClientChannel channel;

  private InputStream in;
  private OutputStream out;
  private Object[] pipes;

  public boolean connect(String host, int port, Options options) {
    try {
      client = SshClient.setUpDefaultClient();
      client.start();
      ConnectFuture cf = client.connect(options.username, host, port);
      session = cf.verify().getSession();
      if (options.password != null) {
        session.addPasswordIdentity(options.password);
      } else {
        session.addPublicKeyIdentity((KeyPair)options.keys.getKeyPair(options.keyalias, options.keypass));
      }
      session.auth().verify(30000);
      switch (options.type) {
        case TYPE_SHELL:
          channel = session.createChannel(ClientChannel.CHANNEL_SHELL);
          //https://github.com/apache/mina-sshd/blob/master/docs/port-forwarding.md
          if (false) {
            // Enable X11 forwarding
            //session.createRemotePortForwardingTracker(SshdSocketAddress.LOCALHOST_ADDRESS, SshdSocketAddress.LOCALHOST_ADDRESS);
            JFLog.log("TODO : enable x11 forwarding");
          }
          if (false) {
            // Enable agent-forwarding
            //session.createRemotePortForwardingTracker(SshdSocketAddress.LOCALHOST_ADDRESS, SshdSocketAddress.LOCALHOST_ADDRESS);
            JFLog.log("TODO : enable agent forwarding");
          }
          break;
        case TYPE_EXEC:
          channel = session.createExecChannel(options.command);
          break;
        case TYPE_SUBSYSTEM:
          break;
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

  /** Get output from exec command. */
  public String getOutput() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Condition connected = () -> {return connected();};
    RelayStream rs2 = new RelayStream(getInputStream(), baos, connected);
    rs2.start();
    try {
      rs2.join();
    } catch (Exception e) {
      //JFLog.log(e);
    }
    return new String(baos.toByteArray());
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
    String out = null;
    ArrayList<String> cmd = new ArrayList<>();
    String argtype = null;
    for(String arg : args) {
      if (argtype != null) {
        switch (argtype) {
          case "-p": port = Integer.valueOf(arg); break;
          case "-o": out = arg; break;
        }
        argtype = null;
        continue;
      }
      if (arg.startsWith("-")) {
        switch (arg) {
          case "-p": argtype = arg; break;
          case "-o": argtype = arg; break;
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
    Options opts = new Options();
    opts.username = user;
    opts.password = pass;
    if (cmd.size() > 0) {
      StringBuilder command = new StringBuilder();
      for(String str : cmd) {
        if (command.length() > 0) {
          command.append(" ");
        }
        command.append(str);
      }
      opts.command = command.toString();
      opts.type = TYPE_EXEC;
    }
    if (!ssh.connect(host, port, opts)) {
      error("Connection failed");
    }
    //connect input/output relay agents
    Condition connected = () -> {return ssh.connected();};
//    RelayStream.debug = true;
    switch (opts.type) {
      case TYPE_SHELL:
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
        break;
      case TYPE_EXEC:
        String output = ssh.getOutput();
        if (out != null) {
          try {
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(output.getBytes());
            fos.close();
          } catch (Exception e) {
            JFLog.log(e);
          }
        }
        System.out.print(output);
        break;
    }
  }
}

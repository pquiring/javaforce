package javaforce.service;

/** SSH Server
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.sshd.server.*;
import org.apache.sshd.server.keyprovider.*;
import org.apache.sshd.common.file.virtualfs.*;
import org.apache.sshd.sftp.server.*;
import org.apache.sshd.server.subsystem.*;
import org.apache.sshd.server.shell.*;
import org.apache.sshd.scp.server.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.jbus.*;

public class SSH extends Thread {
  public final static String busPack = "net.sf.jfssh";

  public static boolean debug = false;

  private static SSH ssh;
  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;
  private static String domain = null;
  private static String ldap_server = null;
  private static ArrayList<String> user_pass_list;
  private static IP4Port bind = new IP4Port();

  private boolean active;
  private SshServer sshd;

  //config
  private String root = null;

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfssh.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfssh.log";
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33012;
    } else {
      return 777;
    }
  }


  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    ssh = new SSH();
    ssh.start();
  }

  public static void serviceStop() {
    ssh.close();
  }

  public static void main(String[] args) {
    serviceStart(args);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        serviceStop();
      }
    });
  }

  public void run() {
    JFLog.append(JF.getLogPath() + "/jfssh.log", true);
    JFLog.setRetention(30);
    JFLog.log("jfSSH starting...");

    try {
      loadConfig();
      busClient = new JBusClient(busPack, new JBusMethods());
      busClient.setPort(getBusPort());
      busClient.start();

      sshd = SshServer.setUpDefaultServer();
      sshd.setPort(bind.port);
      sshd.setHost(bind.toIP4String());
      sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

      //Accept all keys for authentication
      sshd.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);

      //Allow username/password authentication using pre-defined credentials
      sshd.setPasswordAuthenticator((username, password, serverSession) -> {
        for(String u_p : user_pass_list) {
          int idx = u_p.indexOf(':');
          if (idx == -1) continue;
          String user = u_p.substring(0, idx);
          String pass = u_p.substring(idx + 1);
          if (user.equals(username) && pass.equals(password)) {
            return true;
          }
        }
        return false;
      });

      //Setup Virtual File System (VFS)
      Path dir = Paths.get(root);
      VirtualFileSystemFactory vfs = new VirtualFileSystemFactory(dir.toAbsolutePath());
      sshd.setFileSystemFactory(vfs);

      //Add SFTP support
      List<SubsystemFactory> sftpCommandFactory = new ArrayList<>();
      SftpSubsystemFactory sftp = new SftpSubsystemFactory();
      sftpCommandFactory.add(sftp);
      sshd.setSubsystemFactories(sftpCommandFactory);

      //Add SCP support
      ScpCommandFactory scp = new ScpCommandFactory.Builder().build();
      sshd.setCommandFactory(scp);

      //Add Shell support
      if (JF.isWindows()) {
        sshd.setShellFactory(new ProcessShellFactory("cmd.exe", new String[] {"cmd.exe"}));
      } else {
        sshd.setShellFactory(new ProcessShellFactory("/bin/bash", new String[] {"/bin/bash"}));
      }

      sshd.start();
      active = true;
      while (sshd.isStarted() && active) {
        JF.sleep(1000);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void close() {
    active = false;
    if (sshd != null) {
      try {sshd.stop();} catch (Exception e) {}
      sshd = null;
    }
  }

  enum Section {None, Global};

  private final static String defaultConfig
    = "[global]\n"
    + "port=22\n"
    + "#root=/\n"
    + "#bind=192.168.100.2\n"
    + "#domain=example.com\n"
    + "#ldap_server=192.168.200.2\n"
    + "#account=user:pass\n"
    ;

  private void loadConfig() {
    JFLog.log("loadConfig");
    user_pass_list = new ArrayList<String>();
    Section section = Section.None;
    bind.setIP("0.0.0.0");  //bind to all interfaces
    bind.port = 22;
    if (JF.isWindows()) {
      root = "c:/";
    } else {
      root = "/";
    }
    try {
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      StringBuilder cfg = new StringBuilder();
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim();
        int cmt = ln.indexOf('#');
        if (cmt != -1) ln = ln.substring(0, cmt).trim();
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        int idx = ln.indexOf("=");
        if (idx == -1) continue;
        String key = ln.substring(0, idx);
        String value = ln.substring(idx + 1);
        switch (section) {
          case None:
          case Global:
            switch (key) {
              case "port":
                bind.port = JF.atoi(value);
                break;
              case "bind":
                if (!bind.setIP(value)) {
                  JFLog.log("SMTP:bind:Invalid IP:" + value);
                  break;
                }
                break;
              case "account":
                user_pass_list.add(value);
                break;
              case "domain":
                domain = value;
                break;
              case "root":
                root = value;
                break;
              case "ldap_server":
                ldap_server = value;
                break;
              case "debug":
                debug = value.equals("true");
                break;
            }
            break;
        }
      }
      br.close();
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
      JFLog.log("config not found, creating defaults.");
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(defaultConfig.getBytes());
        fos.close();
        config = defaultConfig;
      } catch (Exception e2) {
        JFLog.log(e2);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static class JBusMethods {
    public void getConfig(String pack) {
      ssh.busClient.call(pack, "getConfig", ssh.busClient.quote(ssh.busClient.encodeString(ssh.config)));
    }
    public void setConfig(String cfg) {
      //write new file
      JFLog.log("setConfig");
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      JFLog.log("restart");
      ssh.close();
      ssh = new SSH();
      ssh.start();
    }
  }
}

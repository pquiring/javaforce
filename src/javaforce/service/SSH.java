package javaforce.service;

/** SSH Server
 *
 * @author pquiring
 */

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

public class SSH extends Thread {
  private static SSH service;

  private SshServer sshd;
  private String username = "bob";
  private String password = "secret";

  public static void main(String[] args) {
    service = new SSH();
    service.start();
  }

  public void run() {
    //setup debug logging to console
    System.setProperty("log4j.logger.org.apache.sshd", "DEBUG");
    System.setProperty("log4j.rootLogger", "DEBUG,console");
    System.setProperty("log4j.appender.console", "org.apache.log4j.ConsoleAppender");
    System.setProperty("log4j2.simplelogStatusLoggerLevel", "WARN");
//    System.setProperty("", "");

    sshd = SshServer.setUpDefaultServer();
    sshd.setPort(22);
    sshd.setHost("localhost");
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

    //Accept all keys for authentication
    sshd.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);

    //Allow username/password authentication using pre-defined credentials
    sshd.setPasswordAuthenticator((username, password, serverSession) ->  username.equals(username) && password.equals(password));

    //Setup Virtual File System (VFS)
    //Ensure VFS folder exists
    Path dir;
    if (JF.isWindows())
      dir = Paths.get("c:/");
    else
      dir = Paths.get("/");
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

    try {
      sshd.start();
      while (sshd.isStarted()) {
        JF.sleep(1000);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

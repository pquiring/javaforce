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
    Path dir = Paths.get(".");
    sshd.setFileSystemFactory(new VirtualFileSystemFactory(dir.toAbsolutePath()));

    //Add SFTP support
    List<SubsystemFactory> sftpCommandFactory = new ArrayList<>();
    sftpCommandFactory.add(new SftpSubsystemFactory());
    sshd.setSubsystemFactories(sftpCommandFactory);

    //Add Shell support
    if (JF.isWindows()) {
      sshd.setShellFactory(new ProcessShellFactory("cmd.exe", new String[] {"cmd.exe"}));
    } else {
      sshd.setShellFactory(new ProcessShellFactory("/bin/bash", new String[] {"/bin/bash"}));
    }

    //setup debug logging
    System.setProperty("log4j.logger.org.apache.sshd", "DEBUG");
    System.setProperty("log4j.rootLogger", "DEBUG,console");
    System.setProperty("log4j.appender.console", "org.apache.log4j.ConsoleAppender");

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

package javaforce.utils;

/*
 * Created : Apr 13, 2012
 */
import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;

public class Sudo {

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage : jfsudo command [args]");
      System.exit(1);
    }
    if (args[0].equals("--ask")) {
      String pass = GetPassword.getPassword(null);
      if (pass == null) {
        pass = "";
      }
      System.out.println(pass);
      System.exit(0);
    }
    try {
      //add "sudo -A"
      String[] cmd = new String[args.length + 2];
      cmd[0] = "sudo";
      cmd[1] = "-A";
      System.arraycopy(args, 0, cmd, 2, args.length);
      ProcessBuilder pb = new ProcessBuilder(cmd);
      Map<String, String> env = pb.environment();
      env.put("SUDO_ASKPASS", "/usr/bin/jfsudo-ask");
      Process p = pb.start();
      OutputStream os = p.getOutputStream();
      InputStream err = p.getErrorStream();
      InputStream is = p.getInputStream();
      //System.in -> os
      new Relay(System.in, os).start();
      //is -> System.out
      new Relay(is, System.out).start();
      //err -> System.err
      new Relay(err, System.err).start();
      p.waitFor();
      //BUG : wait for io completion???
      System.exit(p.exitValue());
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static class Relay extends Thread {

    private InputStream is;
    private OutputStream os;

    public Relay(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
    }

    public void run() {
      byte[] buf = new byte[1024];
      try {
        while (true) {
          int len = is.read(buf);
          if (len > 0) {
            os.write(buf, 0, len);
          }
        }
      } catch (Exception e) {
      }
    }
  }
}

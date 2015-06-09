package javaforce.utils;

/**
 * Created : May 10, 2012
 *
 * @author pquiring
 */
import javaforce.*;

public class smbget {

  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage:jsmbget smb://server/share/file [--user=user] [--pass=pass]");
      System.out.println("Note:DOMAINNAME and PASSWORD environment variables are used by default");
      System.exit(0);
    }
    String user = System.getenv("DOMAINNAME");  //username on domain
    String pass = System.getenv("PASSWORD");
    for (int a = 1; a < args.length; a++) {
      if (args[a].startsWith("--user=")) {
        user = args[a].substring(7);
      }
      if (args[a].startsWith("--pass=")) {
        pass = args[a].substring(7);
      }
    }
    if ((user == null) || (pass == null)) {
      user = "";
      pass = "";
    }
    ShellProcess sp = new ShellProcess();
    sp.addRegexResponse("Username.+", user + "\n", false);
    sp.addRegexResponse("Password.+", pass + "\n", false);
    //NOTE : smbget buffers output strangely preventing ShellProcess from getting it, but openpty fixes that
    sp.run(new String[]{"openpty", "/usr/bin/smbget", args[0]}, false);
    System.exit(sp.getErrorLevel());
  }
}

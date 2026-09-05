package javaforce.api.linux;

import javaforce.ffm.*;
import javaforce.linux.*;

/** Linux OS specific API.
 *
 * @author pquiring
 */

@NativeLibrary("pam,ncurses")
public interface LinuxAPI {

  public static LinuxAPI getInstance() {
    return LinuxFFM.getInstance();
  }

  //pty
  public long ptyAlloc();
  public void ptyFree(long ctx);  //free resources on parent side
  public String ptyOpen(long ctx);  //creates a pty and returns the slaveName (one use per ctx)
  public void ptyClose(long ctx);  //close pty
  public int ptyRead(long ctx, byte[] data, int offset, int length);  //read child output on parent side
  public void ptyWrite(long ctx, byte[] data, int offset, int length);  //write to child on parent side
  public void ptySetSize(long ctx, int x, int y);  //set child term size
  public long ptyChildExec(String slaveName, String cmd, String[] args, String[] env);  //spawn child process

  //PAM (Pluggable Authentication Modules)
  public long pamOpen(String user, String pass, String backend);
  public boolean pamClose(long ctx);
  public boolean pamSetItem(long ctx, int type, String value);
  public boolean pamOpenSession(long ctx);
  public boolean pamCloseSession(long ctx);
  public String pamGetEnv(long ctx, String name);

  public static final int PAM_SERVICE = 1;
  public static final int PAM_USER = 2;
  public static final int PAM_TTY = 3;

  public static String pamGetBackend() {
    String backend = "passwd";
    Linux.detectDistro();
    // see /etc/pam.d/ for available back ends
    switch (Linux.distro) {
      case Debian: backend = "passwd"; break;
      case Fedora: backend = "password-auth"; break;
      case Arch: backend = "system-auth"; break;
    }
    return backend;
  }

  //setenv
  public void setEnv(String name, String value);

  //console
  public void enableConsoleMode();
  public void disableConsoleMode();
  public int[] getConsoleSize();
  public int[] getConsolePos();
  public char readConsole();
  public boolean peekConsole();
  public void writeConsole(int ch);
  public void writeConsoleArray(byte[] ch, int off, int len);

  //file
  public int fileGetMode(String path);
  public void fileSetMode(String path, int mode);
  public void fileSetAccessTime(String path, long ts);
  public void fileSetModifiedTime(String path, long ts);
  public long fileGetID(String path);

  //user
  public int getUID();
  public int geteUID();
  public int getGID();
  public int geteGID();
  public int setUID(int uid);
  public int setGID(int gid);
}

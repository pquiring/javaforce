package javaforce.api;

/** Linux OS specific API.
 *
 * @author pquiring
 */

public interface LinuxAPI {
  //init
  public boolean lnxInit(String libX11, String libGL, String libv4l2, String pam, String ncurses);

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
  public boolean authUser(String user, String pass, String backend);

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
}

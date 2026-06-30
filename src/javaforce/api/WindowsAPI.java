package javaforce.api;

/** Windows OS specific API.
 *
 * @author pquiring
 */

public interface WindowsAPI {

  public static WindowsAPI getInstance() {
    return null;  //TODO
  }

  //Windows
  public boolean getWindowRect(String name, int[] rect);  //returns x,y,width,height
  public String getLog();
  public long executeSession(String cmd, String[] args);  //execute child process in current session id
  public void simulateCtrlAltDel();
  public void setInputDesktop();
  public int getSessionID();
  public boolean setSessionID(long token, int sid);  //update session ID of process
  public void closeSession(long token);

  //WinPE resources
  public long peBegin(String file);  //returns handle
  public void peAddIcon(long handle, byte[] data);
  public void peAddString(long handle, int name, int idx, byte[] data);
  public void peEnd(long handle);

  //Impersonate User
  public boolean impersonateUser(String domain, String user, String passwd);
  public boolean revertToSelf();
  public boolean createProcessAsUser(String domain, String user, String passwd, String app, String cmdline, int flags);
  public boolean shellExecute(String op, String app, String cmdline);

  public final int FLAG_LIMIT = 1;
  public final int FLAG_ELEVATE = 2;

  //JDK
  public String findJDKHome();

  //Console
  public void enableConsoleMode();
  public void disableConsoleMode();
  public int[] getConsoleSize();
  public int[] getConsolePos();
  public char readConsole();
  public boolean peekConsole();
  public void writeConsole(int ch);
  public void writeConsoleArray(byte[] ch, int off, int len);

  //Tape drive
  public long tapeOpen(String name);
  public void tapeClose(long handle);
  public boolean tapeFormat(long handle, int blocksize);
  public int tapeRead(long handle, byte[] buf, int offset, int length);
  public int tapeWrite(long handle, byte[] buf, int offset, int length);
  public boolean tapeSetpos(long handle, long pos);
  public long tapeGetpos(long handle);
  public boolean tapeMedia(long handle);
  public long tapeMediaSize();
  public int tapeMediaBlockSize();
  public boolean tapeMediaReadOnly();
  public boolean tapeDrive(long handle);
  public int tapeDriveMinBlockSize();
  public int tapeDriveMaxBlockSize();
  public int tapeDriveDefaultBlockSize();
  public int tapeLastError();

  //Tape changer
  public long changerOpen(String name);
  public void changerClose(long handle);
  public String[] changerList(long handle);
  public boolean changerMove(long handle, String src, String transport, String dst);  //transport is optional

  //VSS (Volume Shadow Services)
  public boolean vssInit();
  public String[] vssListVols();
  public String[][] vssListShadows();  //ret = GUID, shadow volume, org volume
  public boolean vssCreateShadow(String drv);  // { return vssCreateShadow(drv, null); }
  public boolean vssCreateShadow(String drv, String mount);
  public boolean vssDeleteShadow(String shadowID);
  public boolean vssDeleteShadowAll();
  public boolean vssMountShadow(String mount, String shadowVol);
  public boolean vssUnmountShadow(String mount);
}

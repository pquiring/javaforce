package javaforce.jni;

/** Windows Native API
 *
 * @author pquiring
 */

public class WinNative {
  //com port
  public static native long comOpen(String name, int baud);  //assumes 8 data bits, 1 stop bit, no parity, etc.
  public static native void comClose(long handle);
  public static native int comRead(long handle, byte[] buf);
  public static native int comWrite(long handle, byte[] buf);

  //Windows
  public static native boolean getWindowRect(String name, int[] rect);  //returns x,y,width,height

  //WinPE resources
  public static native long peBegin(String file);  //returns handle
  public static native void peAddIcon(long handle, byte[] data);
  public static native void peAddString(long handle, int name, int idx, byte[] data);
  public static native void peEnd(long handle);

  //Impersonate User
  public static native boolean impersonateUser(String domain, String user, String passwd);

  //JDK
  public static native String findJDKHome();

  //Console
  public static native void enableConsoleMode();
  public static native void disableConsoleMode();
  public static native int[] getConsoleSize();
  public static native int[] getConsolePos();
  public static native char readConsole();
  public static native boolean peekConsole();
  public static native void writeConsole(int ch);
  public static native void writeConsoleArray(byte[] ch, int off, int len);

  //Tape drive
  public static native long tapeOpen(String name);
  public static native void tapeClose(long handle);
  public static native boolean tapeFormat(long handle, int blocksize);
  public static native int tapeRead(long handle, byte[] buf, int offset, int length);
  public static native int tapeWrite(long handle, byte[] buf, int offset, int length);
  public static native boolean tapeSetpos(long handle, long pos);
  public static native long tapeGetpos(long handle);
  public static native boolean tapeMedia(long handle);
  public static native long tapeMediaSize();
  public static native int tapeMediaBlockSize();
  public static native boolean tapeMediaReadOnly();
  public static native boolean tapeDrive(long handle);
  public static native int tapeDriveMinBlockSize();
  public static native int tapeDriveMaxBlockSize();
  public static native int tapeDriveDefaultBlockSize();
  public static native int tapeLastError();

  //Tape changer
  public static native long changerOpen(String name);
  public static native void changerClose(long handle);
  public static native String[] changerList(long handle);
  public static native boolean changerMove(long handle, String src, String transport, String dst);  //transport is optional

  //VSS (Volume Shadow Services)
  public static native boolean vssInit();
  public static native String[] vssListVols();
  public static native String[][] vssListShadows();  //ret = GUID, shadow volume, org volume
  public static boolean vssCreateShadow(String drv) {
    return vssCreateShadow(drv, null);
  }
  public static native boolean vssCreateShadow(String drv, String mount);
  public static native boolean vssDeleteShadow(String shadowID);
  public static native boolean vssDeleteShadowAll();
  public static native boolean vssMountShadow(String mount, String shadowVol);
  public static native boolean vssUnmountShadow(String mount);

  //test
  public static native int add(int x, int y);
  public static native void hold(int[] a, int ms);
}

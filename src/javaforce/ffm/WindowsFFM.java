package javaforce.ffm;

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

/** WindowsAPI FFM implementation.
 *
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */


public class WindowsFFM implements WindowsAPI {

  private FFM ffm;

  private static WindowsFFM instance;
  public static WindowsFFM getInstance() {
    if (instance == null) {
      instance = new WindowsFFM();
      if (!instance.ffm_init()) {
        JFLog.log("WindowsFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle getWindowRect;
  public boolean getWindowRect(String name,int[] rect) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_rect = FFM.toMemory(arena, rect);boolean _ret_value_ = (boolean)getWindowRect.invokeExact(arena.allocateFrom(name),_array_rect);FFM.copyBack(_array_rect,rect);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle getLog;
  public String getLog() { try { String _ret_value_ = FFM.getString((MemorySegment)getLog.invokeExact());return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle executeSession;
  public long executeSession(String cmd,String[] args) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_args = FFM.toMemory(arena, args);long _ret_value_ = (long)executeSession.invokeExact(arena.allocateFrom(cmd),_array_args);FFM.copyBack(_array_args,args);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle simulateCtrlAltDel;
  public void simulateCtrlAltDel() { try { simulateCtrlAltDel.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle setInputDesktop;
  public void setInputDesktop() { try { setInputDesktop.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle getSessionID;
  public int getSessionID() { try { int _ret_value_ = (int)getSessionID.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle setSessionID;
  public boolean setSessionID(long token,int sid) { try { boolean _ret_value_ = (boolean)setSessionID.invokeExact(token,sid);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle closeSession;
  public void closeSession(long token) { try { closeSession.invokeExact(token); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle peBegin;
  public long peBegin(String file) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)peBegin.invokeExact(arena.allocateFrom(file));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle peAddIcon;
  public void peAddIcon(long handle,byte[] data) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);peAddIcon.invokeExact(handle,_array_data);FFM.copyBack(_array_data,data); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle peAddString;
  public void peAddString(long handle,int name,int idx,byte[] data) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);peAddString.invokeExact(handle,name,idx,_array_data);FFM.copyBack(_array_data,data); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle peEnd;
  public void peEnd(long handle) { try { peEnd.invokeExact(handle); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle impersonateUser;
  public boolean impersonateUser(String domain,String user,String passwd) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)impersonateUser.invokeExact(arena.allocateFrom(domain),arena.allocateFrom(user),arena.allocateFrom(passwd));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle revertToSelf;
  public boolean revertToSelf() { try { boolean _ret_value_ = (boolean)revertToSelf.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle createProcessAsUser;
  public boolean createProcessAsUser(String domain,String user,String passwd,String app,String cmdline,int flags) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)createProcessAsUser.invokeExact(arena.allocateFrom(domain),arena.allocateFrom(user),arena.allocateFrom(passwd),arena.allocateFrom(app),arena.allocateFrom(cmdline),flags);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle shellExecute;
  public boolean shellExecute(String op,String app,String cmdline) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)shellExecute.invokeExact(arena.allocateFrom(op),arena.allocateFrom(app),arena.allocateFrom(cmdline));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle findJDKHome;
  public String findJDKHome() { try { String _ret_value_ = FFM.getString((MemorySegment)findJDKHome.invokeExact());return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle enableConsoleMode;
  public void enableConsoleMode() { try { enableConsoleMode.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle disableConsoleMode;
  public void disableConsoleMode() { try { disableConsoleMode.invokeExact(); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle getConsoleSize;
  public int[] getConsoleSize() { try { FFM.createFFMArray();getConsoleSize.invokeExact();return (int[])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle getConsolePos;
  public int[] getConsolePos() { try { FFM.createFFMArray();getConsolePos.invokeExact();return (int[])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle readConsole;
  public char readConsole() { try { char _ret_value_ = (char)readConsole.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return (char)-1;} }

  private MethodHandle peekConsole;
  public boolean peekConsole() { try { boolean _ret_value_ = (boolean)peekConsole.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle writeConsole;
  public void writeConsole(int ch) { try { writeConsole.invokeExact(ch); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle writeConsoleArray;
  public void writeConsoleArray(byte[] ch,int off,int len) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_ch = FFM.toMemory(arena, ch);writeConsoleArray.invokeExact(_array_ch,off,len);FFM.copyBack(_array_ch,ch); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle tapeOpen;
  public long tapeOpen(String name) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)tapeOpen.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeClose;
  public void tapeClose(long handle) { try { tapeClose.invokeExact(handle); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle tapeFormat;
  public boolean tapeFormat(long handle,int blocksize) { try { boolean _ret_value_ = (boolean)tapeFormat.invokeExact(handle,blocksize);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle tapeRead;
  public int tapeRead(long handle,byte[] buf,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_buf = FFM.toMemory(arena, buf);int _ret_value_ = (int)tapeRead.invokeExact(handle,_array_buf,offset,length);FFM.copyBack(_array_buf,buf);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeWrite;
  public int tapeWrite(long handle,byte[] buf,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_buf = FFM.toMemory(arena, buf);int _ret_value_ = (int)tapeWrite.invokeExact(handle,_array_buf,offset,length);FFM.copyBack(_array_buf,buf);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeSetpos;
  public boolean tapeSetpos(long handle,long pos) { try { boolean _ret_value_ = (boolean)tapeSetpos.invokeExact(handle,pos);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle tapeGetpos;
  public long tapeGetpos(long handle) { try { long _ret_value_ = (long)tapeGetpos.invokeExact(handle);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeMedia;
  public boolean tapeMedia(long handle) { try { boolean _ret_value_ = (boolean)tapeMedia.invokeExact(handle);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle tapeMediaSize;
  public long tapeMediaSize() { try { long _ret_value_ = (long)tapeMediaSize.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeMediaBlockSize;
  public int tapeMediaBlockSize() { try { int _ret_value_ = (int)tapeMediaBlockSize.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeMediaReadOnly;
  public boolean tapeMediaReadOnly() { try { boolean _ret_value_ = (boolean)tapeMediaReadOnly.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle tapeDrive;
  public boolean tapeDrive(long handle) { try { boolean _ret_value_ = (boolean)tapeDrive.invokeExact(handle);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle tapeDriveMinBlockSize;
  public int tapeDriveMinBlockSize() { try { int _ret_value_ = (int)tapeDriveMinBlockSize.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeDriveMaxBlockSize;
  public int tapeDriveMaxBlockSize() { try { int _ret_value_ = (int)tapeDriveMaxBlockSize.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeDriveDefaultBlockSize;
  public int tapeDriveDefaultBlockSize() { try { int _ret_value_ = (int)tapeDriveDefaultBlockSize.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle tapeLastError;
  public int tapeLastError() { try { int _ret_value_ = (int)tapeLastError.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle changerOpen;
  public long changerOpen(String name) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)changerOpen.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle changerClose;
  public void changerClose(long handle) { try { changerClose.invokeExact(handle); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle changerList;
  public String[] changerList(long handle) { try { FFM.createFFMArray();changerList.invokeExact(handle);return (String[])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle changerMove;
  public boolean changerMove(long handle,String src,String transport,String dst) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)changerMove.invokeExact(handle,arena.allocateFrom(src),arena.allocateFrom(transport),arena.allocateFrom(dst));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vssInit;
  public boolean vssInit() { try { boolean _ret_value_ = (boolean)vssInit.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vssListVols;
  public String[] vssListVols() { try { FFM.createFFMArray();vssListVols.invokeExact();return (String[])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vssListShadows;
  public String[][] vssListShadows() { try { FFM.createFFMArray();vssListShadows.invokeExact();return (String[][])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vssCreateShadow;
  public boolean vssCreateShadow(String drv) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vssCreateShadow.invokeExact(arena.allocateFrom(drv));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  public boolean vssCreateShadow(String drv,String mount) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vssCreateShadow.invokeExact(arena.allocateFrom(drv),arena.allocateFrom(mount));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vssDeleteShadow;
  public boolean vssDeleteShadow(String shadowID) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vssDeleteShadow.invokeExact(arena.allocateFrom(shadowID));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vssDeleteShadowAll;
  public boolean vssDeleteShadowAll() { try { boolean _ret_value_ = (boolean)vssDeleteShadowAll.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vssMountShadow;
  public boolean vssMountShadow(String mount,String shadowVol) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vssMountShadow.invokeExact(arena.allocateFrom(mount),arena.allocateFrom(shadowVol));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vssUnmountShadow;
  public boolean vssUnmountShadow(String mount) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vssUnmountShadow.invokeExact(arena.allocateFrom(mount));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("WindowsAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    getWindowRect = ffm.getFunctionPtr("_getWindowRect", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS));
    getLog = ffm.getFunctionPtr("_getLog", ffm.getFunctionDesciptor(ADDRESS));
    executeSession = ffm.getFunctionPtr("_executeSession", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,ADDRESS));
    simulateCtrlAltDel = ffm.getFunctionPtr("_simulateCtrlAltDel", ffm.getFunctionDesciptorVoid());
    setInputDesktop = ffm.getFunctionPtr("_setInputDesktop", ffm.getFunctionDesciptorVoid());
    getSessionID = ffm.getFunctionPtr("_getSessionID", ffm.getFunctionDesciptor(JAVA_INT));
    setSessionID = ffm.getFunctionPtr("_setSessionID", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT));
    closeSession = ffm.getFunctionPtr("_closeSession", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    peBegin = ffm.getFunctionPtr("_peBegin", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS));
    peAddIcon = ffm.getFunctionPtr("_peAddIcon", ffm.getFunctionDesciptorVoid(JAVA_LONG,ADDRESS));
    peAddString = ffm.getFunctionPtr("_peAddString", ffm.getFunctionDesciptorVoid(JAVA_LONG,JAVA_INT,JAVA_INT,ADDRESS));
    peEnd = ffm.getFunctionPtr("_peEnd", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    impersonateUser = ffm.getFunctionPtr("_impersonateUser", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,ADDRESS));
    revertToSelf = ffm.getFunctionPtr("_revertToSelf", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    createProcessAsUser = ffm.getFunctionPtr("_createProcessAsUser", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,ADDRESS,ADDRESS,ADDRESS,JAVA_INT));
    shellExecute = ffm.getFunctionPtr("_shellExecute", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,ADDRESS));
    findJDKHome = ffm.getFunctionPtr("_findJDKHome", ffm.getFunctionDesciptor(ADDRESS));
    enableConsoleMode = ffm.getFunctionPtr("_enableConsoleMode", ffm.getFunctionDesciptorVoid());
    disableConsoleMode = ffm.getFunctionPtr("_disableConsoleMode", ffm.getFunctionDesciptorVoid());
    getConsoleSize = ffm.getFunctionPtr("_getConsoleSize", ffm.getFunctionDesciptorVoid());
    getConsolePos = ffm.getFunctionPtr("_getConsolePos", ffm.getFunctionDesciptorVoid());
    readConsole = ffm.getFunctionPtr("_readConsole", ffm.getFunctionDesciptor(JAVA_CHAR));
    peekConsole = ffm.getFunctionPtr("_peekConsole", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    writeConsole = ffm.getFunctionPtr("_writeConsole", ffm.getFunctionDesciptorVoid(JAVA_INT));
    writeConsoleArray = ffm.getFunctionPtr("_writeConsoleArray", ffm.getFunctionDesciptorVoid(ADDRESS,JAVA_INT,JAVA_INT));
    tapeOpen = ffm.getFunctionPtr("_tapeOpen", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS));
    tapeClose = ffm.getFunctionPtr("_tapeClose", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    tapeFormat = ffm.getFunctionPtr("_tapeFormat", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT));
    tapeRead = ffm.getFunctionPtr("_tapeRead", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    tapeWrite = ffm.getFunctionPtr("_tapeWrite", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    tapeSetpos = ffm.getFunctionPtr("_tapeSetpos", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG));
    tapeGetpos = ffm.getFunctionPtr("_tapeGetpos", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_LONG));
    tapeMedia = ffm.getFunctionPtr("_tapeMedia", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    tapeMediaSize = ffm.getFunctionPtr("_tapeMediaSize", ffm.getFunctionDesciptor(JAVA_LONG));
    tapeMediaBlockSize = ffm.getFunctionPtr("_tapeMediaBlockSize", ffm.getFunctionDesciptor(JAVA_INT));
    tapeMediaReadOnly = ffm.getFunctionPtr("_tapeMediaReadOnly", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    tapeDrive = ffm.getFunctionPtr("_tapeDrive", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    tapeDriveMinBlockSize = ffm.getFunctionPtr("_tapeDriveMinBlockSize", ffm.getFunctionDesciptor(JAVA_INT));
    tapeDriveMaxBlockSize = ffm.getFunctionPtr("_tapeDriveMaxBlockSize", ffm.getFunctionDesciptor(JAVA_INT));
    tapeDriveDefaultBlockSize = ffm.getFunctionPtr("_tapeDriveDefaultBlockSize", ffm.getFunctionDesciptor(JAVA_INT));
    tapeLastError = ffm.getFunctionPtr("_tapeLastError", ffm.getFunctionDesciptor(JAVA_INT));
    changerOpen = ffm.getFunctionPtr("_changerOpen", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS));
    changerClose = ffm.getFunctionPtr("_changerClose", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    changerList = ffm.getFunctionPtr("_changerList", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    changerMove = ffm.getFunctionPtr("_changerMove", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,ADDRESS,ADDRESS,ADDRESS));
    vssInit = ffm.getFunctionPtr("_vssInit", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    vssListVols = ffm.getFunctionPtr("_vssListVols", ffm.getFunctionDesciptorVoid());
    vssListShadows = ffm.getFunctionPtr("_vssListShadows", ffm.getFunctionDesciptorVoid());
    vssCreateShadow = ffm.getFunctionPtr("_vssCreateShadow", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vssDeleteShadow = ffm.getFunctionPtr("_vssDeleteShadow", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vssDeleteShadowAll = ffm.getFunctionPtr("_vssDeleteShadowAll", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    vssMountShadow = ffm.getFunctionPtr("_vssMountShadow", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS));
    vssUnmountShadow = ffm.getFunctionPtr("_vssUnmountShadow", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    return true;
  }
}

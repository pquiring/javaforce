package javaforce.ffm;

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

/** LinuxAPI FFM implementation.
 *
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */


public class LinuxFFM implements LinuxAPI {

  private FFM ffm;

  private static LinuxFFM instance;
  public static LinuxFFM getInstance() {
    if (instance == null) {
      instance = new LinuxFFM();
      if (!instance.ffm_init()) {
        JFLog.log("LinuxFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle lnxInit;
  public boolean lnxInit(String libX11,String libGL,String libv4l2,String pam,String ncurses) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)lnxInit.invokeExact(arena.allocateFrom(libX11),arena.allocateFrom(libGL),arena.allocateFrom(libv4l2),arena.allocateFrom(pam),arena.allocateFrom(ncurses));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle ptyAlloc;
  public long ptyAlloc() { try { long _ret_value_ = (long)ptyAlloc.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle ptyFree;
  public void ptyFree(long ctx) { try { ptyFree.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle ptyOpen;
  public String ptyOpen(long ctx) { try { String _ret_value_ = FFM.getString((MemorySegment)ptyOpen.invokeExact(ctx));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle ptyClose;
  public void ptyClose(long ctx) { try { ptyClose.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle ptyRead;
  public int ptyRead(long ctx,byte[] data,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);int _ret_value_ = (int)ptyRead.invokeExact(ctx,_array_data,offset,length);FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle ptyWrite;
  public void ptyWrite(long ctx,byte[] data,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);ptyWrite.invokeExact(ctx,_array_data,offset,length);FFM.copyBack(_array_data,data); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle ptySetSize;
  public void ptySetSize(long ctx,int x,int y) { try { ptySetSize.invokeExact(ctx,x,y); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle ptyChildExec;
  public long ptyChildExec(String slaveName,String cmd,String[] args,String[] env) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_args = FFM.toMemory(arena, args);MemorySegment _array_env = FFM.toMemory(arena, env);long _ret_value_ = (long)ptyChildExec.invokeExact(arena.allocateFrom(slaveName),arena.allocateFrom(cmd),_array_args,_array_env);FFM.copyBack(_array_args,args);FFM.copyBack(_array_env,env);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle authUser;
  public boolean authUser(String user,String pass,String backend) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)authUser.invokeExact(arena.allocateFrom(user),arena.allocateFrom(pass),arena.allocateFrom(backend));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle setEnv;
  public void setEnv(String name,String value) { try { Arena arena = Arena.ofAuto(); setEnv.invokeExact(arena.allocateFrom(name),arena.allocateFrom(value)); } catch (Throwable t) { JFLog.log(t); } }

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

  private MethodHandle fileGetMode;
  public int fileGetMode(String path) { try { Arena arena = Arena.ofAuto(); int _ret_value_ = (int)fileGetMode.invokeExact(arena.allocateFrom(path));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle fileSetMode;
  public void fileSetMode(String path,int mode) { try { Arena arena = Arena.ofAuto(); fileSetMode.invokeExact(arena.allocateFrom(path),mode); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle fileSetAccessTime;
  public void fileSetAccessTime(String path,long ts) { try { Arena arena = Arena.ofAuto(); fileSetAccessTime.invokeExact(arena.allocateFrom(path),ts); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle fileSetModifiedTime;
  public void fileSetModifiedTime(String path,long ts) { try { Arena arena = Arena.ofAuto(); fileSetModifiedTime.invokeExact(arena.allocateFrom(path),ts); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle fileGetID;
  public long fileGetID(String path) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)fileGetID.invokeExact(arena.allocateFrom(path));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle getUID;
  public int getUID() { try { int _ret_value_ = (int)getUID.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("LinuxAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    lnxInit = ffm.getFunctionPtr("_lnxInit", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,ADDRESS,ADDRESS,ADDRESS));
    ptyAlloc = ffm.getFunctionPtr("_ptyAlloc", ffm.getFunctionDesciptor(JAVA_LONG));
    ptyFree = ffm.getFunctionPtr("_ptyFree", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    ptyOpen = ffm.getFunctionPtr("_ptyOpen", ffm.getFunctionDesciptor(ADDRESS,JAVA_LONG));
    ptyClose = ffm.getFunctionPtr("_ptyClose", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    ptyRead = ffm.getFunctionPtr("_ptyRead", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    ptyWrite = ffm.getFunctionPtr("_ptyWrite", ffm.getFunctionDesciptorVoid(JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    ptySetSize = ffm.getFunctionPtr("_ptySetSize", ffm.getFunctionDesciptorVoid(JAVA_LONG,JAVA_INT,JAVA_INT));
    ptyChildExec = ffm.getFunctionPtr("_ptyChildExec", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,ADDRESS,ADDRESS,ADDRESS));
    authUser = ffm.getFunctionPtr("_authUser", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,ADDRESS));
    setEnv = ffm.getFunctionPtr("_setEnv", ffm.getFunctionDesciptorVoid(ADDRESS,ADDRESS));
    enableConsoleMode = ffm.getFunctionPtr("_enableConsoleMode", ffm.getFunctionDesciptorVoid());
    disableConsoleMode = ffm.getFunctionPtr("_disableConsoleMode", ffm.getFunctionDesciptorVoid());
    getConsoleSize = ffm.getFunctionPtr("_getConsoleSize", ffm.getFunctionDesciptorVoid());
    getConsolePos = ffm.getFunctionPtr("_getConsolePos", ffm.getFunctionDesciptorVoid());
    readConsole = ffm.getFunctionPtr("_readConsole", ffm.getFunctionDesciptor(JAVA_CHAR));
    peekConsole = ffm.getFunctionPtr("_peekConsole", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    writeConsole = ffm.getFunctionPtr("_writeConsole", ffm.getFunctionDesciptorVoid(JAVA_INT));
    writeConsoleArray = ffm.getFunctionPtr("_writeConsoleArray", ffm.getFunctionDesciptorVoid(ADDRESS,JAVA_INT,JAVA_INT));
    fileGetMode = ffm.getFunctionPtr("_fileGetMode", ffm.getFunctionDesciptor(JAVA_INT,ADDRESS));
    fileSetMode = ffm.getFunctionPtr("_fileSetMode", ffm.getFunctionDesciptorVoid(ADDRESS,JAVA_INT));
    fileSetAccessTime = ffm.getFunctionPtr("_fileSetAccessTime", ffm.getFunctionDesciptorVoid(ADDRESS,JAVA_LONG));
    fileSetModifiedTime = ffm.getFunctionPtr("_fileSetModifiedTime", ffm.getFunctionDesciptorVoid(ADDRESS,JAVA_LONG));
    fileGetID = ffm.getFunctionPtr("_fileGetID", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS));
    getUID = ffm.getFunctionPtr("_getUID", ffm.getFunctionDesciptor(JAVA_INT));
    return true;
  }
}

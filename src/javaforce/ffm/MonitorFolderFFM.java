package javaforce.ffm;

/** MonitorFolderAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;
import javaforce.io.*;

public class MonitorFolderFFM implements MonitorFolderAPI {

  private FFM ffm;

  private static MonitorFolderFFM instance;
  public static MonitorFolderFFM getInstance() {
    if (instance == null) {
      instance = new MonitorFolderFFM();
      if (!instance.ffm_init()) {
        JFLog.log("MonitorFolderFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle monitorFolderCreate;
  public long monitorFolderCreate(String folder) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)monitorFolderCreate.invokeExact(arena.allocateFrom(folder));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle monitorFolderPoll;
  public void monitorFolderPoll(long handle,FolderListener listener) { try { Arena arena = Arena.ofAuto(); monitorFolderPoll.invokeExact(handle,ffm.getFunctionUpCall(listener, "folderChangeEvent", void.class, new Class[] {MemorySegment.class, MemorySegment.class}, arena)); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle monitorFolderClose;
  public void monitorFolderClose(long handle) { try { monitorFolderClose.invokeExact(handle); } catch (Throwable t) { JFLog.log(t); } }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("MonitorFolderAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    monitorFolderCreate = ffm.getFunctionPtr("_monitorFolderCreate", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS));
    monitorFolderPoll = ffm.getFunctionPtr("_monitorFolderPoll", ffm.getFunctionDesciptorVoid(JAVA_LONG,ADDRESS));
    monitorFolderClose = ffm.getFunctionPtr("_monitorFolderClose", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    return true;
  }
}

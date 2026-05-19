package javaforce.ffm;

/** ComPortAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

public class ComPortFFM implements ComPortAPI {

  private FFM ffm;

  private static ComPortFFM instance;
  public static ComPortFFM getInstance() {
    if (instance == null) {
      instance = new ComPortFFM();
      if (!instance.ffm_init()) {
        JFLog.log("ComPortFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle comOpen;
  public long comOpen(String name,int baud) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)comOpen.invokeExact(arena.allocateFrom(name),baud);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle comRead;
  public int comRead(long handle,byte[] data,int size) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);int _ret_value_ = (int)comRead.invokeExact(handle,_array_data,size);FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle comWrite;
  public int comWrite(long handle,byte[] data,int size) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);int _ret_value_ = (int)comWrite.invokeExact(handle,_array_data,size);FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle comClose;
  public void comClose(long handle) { try { comClose.invokeExact(handle); } catch (Throwable t) { JFLog.log(t); } }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("ComPortAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    comOpen = ffm.getFunctionPtr("_comOpen", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,JAVA_INT));
    comRead = ffm.getFunctionPtr("_comRead", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,ADDRESS,JAVA_INT));
    comWrite = ffm.getFunctionPtr("_comWrite", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,ADDRESS,JAVA_INT));
    comClose = ffm.getFunctionPtr("_comClose", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    return true;
  }
}

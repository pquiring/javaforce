package javaforce.ffm;

/** WinPipeAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

public class WinPipeFFM implements WinPipeAPI {

  private FFM ffm;

  private static WinPipeFFM instance;
  public static WinPipeFFM getInstance() {
    if (instance == null) {
      instance = new WinPipeFFM();
      if (!instance.ffm_init()) {
        JFLog.log("WinPipeFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle pipeCreate;
  public long pipeCreate(String name,boolean first) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)pipeCreate.invokeExact(arena.allocateFrom(name),first);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle pipeClose;
  public void pipeClose(long ctx) { try { pipeClose.invokeExact(ctx); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle pipeRead;
  public int pipeRead(long ctx,byte[] buf,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_buf = FFM.toMemory(arena, buf);int _ret_value_ = (int)pipeRead.invokeExact(ctx,_array_buf,offset,length);FFM.copyBack(_array_buf,buf);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle pipeWrite;
  public int pipeWrite(String name,byte[] buf,int offset,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_buf = FFM.toMemory(arena, buf);int _ret_value_ = (int)pipeWrite.invokeExact(arena.allocateFrom(name),_array_buf,offset,length);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("WinPipeAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    pipeCreate = ffm.getFunctionPtr("_pipeCreate", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,JAVA_BOOLEAN));
    pipeClose = ffm.getFunctionPtr("_pipeClose", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    pipeRead = ffm.getFunctionPtr("_pipeRead", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    pipeWrite = ffm.getFunctionPtr("_pipeWrite", ffm.getFunctionDesciptor(JAVA_INT,ADDRESS,ADDRESS,JAVA_INT,JAVA_INT));
    return true;
  }
}

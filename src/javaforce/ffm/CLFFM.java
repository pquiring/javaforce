package javaforce.ffm;

/** CLAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

public class CLFFM implements CLAPI {

  private FFM ffm;

  private static CLFFM instance;
  public static CLFFM getInstance() {
    if (instance == null) {
      instance = new CLFFM();
      if (!instance.ffm_init()) {
        JFLog.log("CLFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle clLoadLibrary;
  public boolean clLoadLibrary(String file) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)clLoadLibrary.invokeExact(arena.allocateFrom(file));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clCreate;
  public long clCreate(String src,int type) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)clCreate.invokeExact(arena.allocateFrom(src),type);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle clKernel;
  public long clKernel(long ctx,String func) { try { Arena arena = Arena.ofAuto(); long _ret_value_ = (long)clKernel.invokeExact(ctx,arena.allocateFrom(func));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle clCreateBuffer;
  public long clCreateBuffer(long ctx,int size,int type) { try { long _ret_value_ = (long)clCreateBuffer.invokeExact(ctx,size,type);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle clSetArg;
  public boolean clSetArg(long ctx,long kernel,int idx,byte[] value,int size) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_value = FFM.toMemory(arena, value);boolean _ret_value_ = (boolean)clSetArg.invokeExact(ctx,kernel,idx,_array_value,size);FFM.copyBack(_array_value,value);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clWriteBufferi8;
  public boolean clWriteBufferi8(long ctx,long buffer,byte[] value,int size) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_value = FFM.toMemory(arena, value);boolean _ret_value_ = (boolean)clWriteBufferi8.invokeExact(ctx,buffer,_array_value,size);FFM.copyBack(_array_value,value);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clWriteBufferf32;
  public boolean clWriteBufferf32(long ctx,long buffer,float[] value,int size) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_value = FFM.toMemory(arena, value);boolean _ret_value_ = (boolean)clWriteBufferf32.invokeExact(ctx,buffer,_array_value,size);FFM.copyBack(_array_value,value);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clExecute;
  public boolean clExecute(long ctx,long kernel,int count) { try { boolean _ret_value_ = (boolean)clExecute.invokeExact(ctx,kernel,count);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clExecute2;
  public boolean clExecute2(long ctx,long kernel,int count1,int count2) { try { boolean _ret_value_ = (boolean)clExecute2.invokeExact(ctx,kernel,count1,count2);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clExecute3;
  public boolean clExecute3(long ctx,long kernel,int count1,int count2,int count3) { try { boolean _ret_value_ = (boolean)clExecute3.invokeExact(ctx,kernel,count1,count2,count3);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clExecute4;
  public boolean clExecute4(long ctx,long kernel,int count1,int count2,int count3,int count4) { try { boolean _ret_value_ = (boolean)clExecute4.invokeExact(ctx,kernel,count1,count2,count3,count4);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clReadBufferi8;
  public boolean clReadBufferi8(long ctx,long buffer,byte[] value,int size) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_value = FFM.toMemory(arena, value);boolean _ret_value_ = (boolean)clReadBufferi8.invokeExact(ctx,buffer,_array_value,size);FFM.copyBack(_array_value,value);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clReadBufferf32;
  public boolean clReadBufferf32(long ctx,long buffer,float[] value,int size) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_value = FFM.toMemory(arena, value);boolean _ret_value_ = (boolean)clReadBufferf32.invokeExact(ctx,buffer,_array_value,size);FFM.copyBack(_array_value,value);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clFreeKernel;
  public boolean clFreeKernel(long ctx,long kernel) { try { boolean _ret_value_ = (boolean)clFreeKernel.invokeExact(ctx,kernel);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clFreeBuffer;
  public boolean clFreeBuffer(long ctx,long buffer) { try { boolean _ret_value_ = (boolean)clFreeBuffer.invokeExact(ctx,buffer);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle clClose;
  public boolean clClose(long ctx) { try { boolean _ret_value_ = (boolean)clClose.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("CLAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    clLoadLibrary = ffm.getFunctionPtr("_clLoadLibrary", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    clCreate = ffm.getFunctionPtr("_clCreate", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,JAVA_INT));
    clKernel = ffm.getFunctionPtr("_clKernel", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_LONG,ADDRESS));
    clCreateBuffer = ffm.getFunctionPtr("_clCreateBuffer", ffm.getFunctionDesciptor(JAVA_LONG,JAVA_LONG,JAVA_INT,JAVA_INT));
    clSetArg = ffm.getFunctionPtr("_clSetArg", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,JAVA_INT,ADDRESS,JAVA_INT));
    clWriteBufferi8 = ffm.getFunctionPtr("_clWriteBufferi8", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,ADDRESS,JAVA_INT));
    clWriteBufferf32 = ffm.getFunctionPtr("_clWriteBufferf32", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,ADDRESS,JAVA_INT));
    clExecute = ffm.getFunctionPtr("_clExecute", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,JAVA_INT));
    clExecute2 = ffm.getFunctionPtr("_clExecute2", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,JAVA_INT,JAVA_INT));
    clExecute3 = ffm.getFunctionPtr("_clExecute3", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT));
    clExecute4 = ffm.getFunctionPtr("_clExecute4", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    clReadBufferi8 = ffm.getFunctionPtr("_clReadBufferi8", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,ADDRESS,JAVA_INT));
    clReadBufferf32 = ffm.getFunctionPtr("_clReadBufferf32", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG,ADDRESS,JAVA_INT));
    clFreeKernel = ffm.getFunctionPtr("_clFreeKernel", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG));
    clFreeBuffer = ffm.getFunctionPtr("_clFreeBuffer", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_LONG));
    clClose = ffm.getFunctionPtr("_clClose", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    return true;
  }
}

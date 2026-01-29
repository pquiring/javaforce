package javaforce.ffm;

/** PCapAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

public class PCapFFM implements PCapAPI {

  private Arena arena;
  private FFM ffm;

  private static PCapFFM instance;
  public static PCapFFM getInstance() {
    if (instance == null) {
      instance = new PCapFFM();
      if (!instance.ffm_init()) {
        JFLog.log("PCapFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle pcapInit;
  public boolean pcapInit(String lib1,String lib2) { try { boolean _ret_value_ = (boolean)pcapInit.invokeExact(arena.allocateFrom(lib1),arena.allocateFrom(lib2));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle pcapListLocalInterfaces;
  public String[] pcapListLocalInterfaces() { try { String[] _ret_value_ = FFM.toArrayString((MemorySegment)pcapListLocalInterfaces.invokeExact());return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle pcapStart;
  public long pcapStart(String local_interface,boolean nonblocking) { try { long _ret_value_ = (long)pcapStart.invokeExact(arena.allocateFrom(local_interface),nonblocking);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle pcapStop;
  public void pcapStop(long id) { try { pcapStop.invokeExact(id); } catch (Throwable t) { JFLog.log(t); } }

  private MethodHandle pcapCompile;
  public boolean pcapCompile(long handle,String program) { try { boolean _ret_value_ = (boolean)pcapCompile.invokeExact(handle,arena.allocateFrom(program));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle pcapRead;
  public byte[] pcapRead(long handle) { try { byte[] _ret_value_ = FFM.toArrayByte((MemorySegment)pcapRead.invokeExact(handle));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle pcapWrite;
  public boolean pcapWrite(long handle,byte[] packet,int offset,int length) {MemorySegment _array_packet = FFM.toMemory(arena, packet); try { boolean _ret_value_ = (boolean)pcapWrite.invokeExact(handle,_array_packet,offset,length);FFM.copyBack(_array_packet,packet);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    arena = Arena.ofAuto();
    init = ffm.getFunction("PCapAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    pcapInit = ffm.getFunctionPtr("_pcapInit", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS));
    pcapListLocalInterfaces = ffm.getFunctionPtr("_pcapListLocalInterfaces", ffm.getFunctionDesciptor(ADDRESS));
    pcapStart = ffm.getFunctionPtr("_pcapStart", ffm.getFunctionDesciptor(JAVA_LONG,ADDRESS,JAVA_BOOLEAN));
    pcapStop = ffm.getFunctionPtr("_pcapStop", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    pcapCompile = ffm.getFunctionPtr("_pcapCompile", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,ADDRESS));
    pcapRead = ffm.getFunctionPtr("_pcapRead", ffm.getFunctionDesciptor(ADDRESS,JAVA_LONG));
    pcapWrite = ffm.getFunctionPtr("_pcapWrite", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,ADDRESS,JAVA_INT,JAVA_INT));
    return true;
  }
}

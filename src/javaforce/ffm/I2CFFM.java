package javaforce.ffm;

/** I2CAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

public class I2CFFM implements I2CAPI {

  private FFM ffm;

  private static I2CFFM instance;
  public static I2CFFM getInstance() {
    if (instance == null) {
      instance = new I2CFFM();
      if (!instance.ffm_init()) {
        JFLog.log("I2CFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle i2cSetup;
  public boolean i2cSetup() { try { boolean _ret_value_ = (boolean)i2cSetup.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle i2cSetSlave;
  public boolean i2cSetSlave(int addr) { try { boolean _ret_value_ = (boolean)i2cSetSlave.invokeExact(addr);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle i2cWrite;
  public boolean i2cWrite(byte[] data,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);boolean _ret_value_ = (boolean)i2cWrite.invokeExact(_array_data,length);FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle i2cRead;
  public int i2cRead(byte[] data,int length) { try { Arena arena = Arena.ofAuto(); MemorySegment _array_data = FFM.toMemory(arena, data);int _ret_value_ = (int)i2cRead.invokeExact(_array_data,length);FFM.copyBack(_array_data,data);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("I2CAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    i2cSetup = ffm.getFunctionPtr("_i2cSetup", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    i2cSetSlave = ffm.getFunctionPtr("_i2cSetSlave", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT));
    i2cWrite = ffm.getFunctionPtr("_i2cWrite", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,JAVA_INT));
    i2cRead = ffm.getFunctionPtr("_i2cRead", ffm.getFunctionDesciptor(JAVA_INT,ADDRESS,JAVA_INT));
    return true;
  }
}

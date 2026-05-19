package javaforce.ffm;

/** GPIOAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

public class GPIOFFM implements GPIOAPI {

  private FFM ffm;

  private static GPIOFFM instance;
  public static GPIOFFM getInstance() {
    if (instance == null) {
      instance = new GPIOFFM();
      if (!instance.ffm_init()) {
        JFLog.log("GPIOFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle gpioSetup;
  public boolean gpioSetup(int addr) { try { boolean _ret_value_ = (boolean)gpioSetup.invokeExact(addr);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle gpioConfigOutput;
  public boolean gpioConfigOutput(int idx) { try { boolean _ret_value_ = (boolean)gpioConfigOutput.invokeExact(idx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle gpioConfigInput;
  public boolean gpioConfigInput(int idx) { try { boolean _ret_value_ = (boolean)gpioConfigInput.invokeExact(idx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle gpioWrite;
  public boolean gpioWrite(int idx,boolean state) { try { boolean _ret_value_ = (boolean)gpioWrite.invokeExact(idx,state);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle gpioRead;
  public boolean gpioRead(int idx) { try { boolean _ret_value_ = (boolean)gpioRead.invokeExact(idx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("GPIOAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    gpioSetup = ffm.getFunctionPtr("_gpioSetup", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT));
    gpioConfigOutput = ffm.getFunctionPtr("_gpioConfigOutput", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT));
    gpioConfigInput = ffm.getFunctionPtr("_gpioConfigInput", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT));
    gpioWrite = ffm.getFunctionPtr("_gpioWrite", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT,JAVA_BOOLEAN));
    gpioRead = ffm.getFunctionPtr("_gpioRead", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT));
    return true;
  }
}

package javaforce.ffm;

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;

/** CameraAPI FFM implementation.
 *
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */


public class CameraFFM implements CameraAPI {

  private FFM ffm;

  private static CameraFFM instance;
  public static CameraFFM getInstance() {
    if (instance == null) {
      instance = new CameraFFM();
      if (!instance.ffm_init()) {
        JFLog.log("CameraFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle cameraInit;
  public long cameraInit() { try { long _ret_value_ = (long)cameraInit.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle cameraUninit;
  public boolean cameraUninit(long ctx) { try { boolean _ret_value_ = (boolean)cameraUninit.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle cameraListDevices;
  public String[] cameraListDevices(long ctx) { try { FFM.createFFMArray();cameraListDevices.invokeExact(ctx);return (String[])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle cameraListModes;
  public String[] cameraListModes(long ctx,int deviceIdx) { try { FFM.createFFMArray();cameraListModes.invokeExact(ctx,deviceIdx);return (String[])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle cameraStart;
  public boolean cameraStart(long ctx,int deviceIdx,int width,int height) { try { boolean _ret_value_ = (boolean)cameraStart.invokeExact(ctx,deviceIdx,width,height);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle cameraStop;
  public boolean cameraStop(long ctx) { try { boolean _ret_value_ = (boolean)cameraStop.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle cameraGetFrame;
  public int[] cameraGetFrame(long ctx) { try { FFM.createFFMArray();cameraGetFrame.invokeExact(ctx);return (int[])FFM.getArray(); } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle cameraGetWidth;
  public int cameraGetWidth(long ctx) { try { int _ret_value_ = (int)cameraGetWidth.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle cameraGetHeight;
  public int cameraGetHeight(long ctx) { try { int _ret_value_ = (int)cameraGetHeight.invokeExact(ctx);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("CameraAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    cameraInit = ffm.getFunctionPtr("_cameraInit", ffm.getFunctionDesciptor(JAVA_LONG));
    cameraUninit = ffm.getFunctionPtr("_cameraUninit", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    cameraListDevices = ffm.getFunctionPtr("_cameraListDevices", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    cameraListModes = ffm.getFunctionPtr("_cameraListModes", ffm.getFunctionDesciptorVoid(JAVA_LONG,JAVA_INT));
    cameraStart = ffm.getFunctionPtr("_cameraStart", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG,JAVA_INT,JAVA_INT,JAVA_INT));
    cameraStop = ffm.getFunctionPtr("_cameraStop", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_LONG));
    cameraGetFrame = ffm.getFunctionPtr("_cameraGetFrame", ffm.getFunctionDesciptorVoid(JAVA_LONG));
    cameraGetWidth = ffm.getFunctionPtr("_cameraGetWidth", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    cameraGetHeight = ffm.getFunctionPtr("_cameraGetHeight", ffm.getFunctionDesciptor(JAVA_INT,JAVA_LONG));
    return true;
  }
}

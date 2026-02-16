package javaforce.ffm;

/** VMAPI FFM implementation.
 * NON-AI MACHINE GENERATED CODE - DO NOT EDIT
 */

import java.lang.foreign.*;
import java.lang.invoke.*;
import static java.lang.foreign.ValueLayout.*;

import javaforce.*;
import javaforce.ffm.*;
import javaforce.api.*;
import javaforce.vm.*;

public class VMFFM implements VMAPI {

  private FFM ffm;

  private static VMFFM instance;
  public static VMFFM getInstance() {
    if (instance == null) {
      instance = new VMFFM();
      if (!instance.ffm_init()) {
        JFLog.log("VMFFM init failed!");
        instance = null;
      }
    }
    return instance;
  }

  private MethodHandle vmDeviceList;
  public String[] vmDeviceList(int type) { try { String[] _ret_value_ = FFM.toArrayString((MemorySegment)vmDeviceList.invokeExact(type));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmDiskCreate;
  public boolean vmDiskCreate(String pool_name,String xml) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmDiskCreate.invokeExact(arena.allocateFrom(pool_name),arena.allocateFrom(xml));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmNetworkListPhys;
  public String[] vmNetworkListPhys() { try { String[] _ret_value_ = FFM.toArrayString((MemorySegment)vmNetworkListPhys.invokeExact());return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmSecretCreate;
  public boolean vmSecretCreate(String xml,String passwd) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmSecretCreate.invokeExact(arena.allocateFrom(xml),arena.allocateFrom(passwd));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmStorageList;
  public String[] vmStorageList() { try { String[] _ret_value_ = FFM.toArrayString((MemorySegment)vmStorageList.invokeExact());return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmStorageRegister;
  public boolean vmStorageRegister(String xml) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmStorageRegister.invokeExact(arena.allocateFrom(xml));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmStorageUnregister;
  public boolean vmStorageUnregister(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmStorageUnregister.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmStorageStart;
  public boolean vmStorageStart(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmStorageStart.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmStorageStop;
  public boolean vmStorageStop(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmStorageStop.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmStorageGetState;
  public int vmStorageGetState(String name) { try { Arena arena = Arena.ofAuto(); int _ret_value_ = (int)vmStorageGetState.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle vmStorageGetUUID;
  public String vmStorageGetUUID(String name) { try { Arena arena = Arena.ofAuto(); String _ret_value_ = FFM.getString((MemorySegment)vmStorageGetUUID.invokeExact(arena.allocateFrom(name)));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmInit;
  public boolean vmInit() { try { boolean _ret_value_ = (boolean)vmInit.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmStart;
  public boolean vmStart(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmStart.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmStop;
  public boolean vmStop(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmStop.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmPowerOff;
  public boolean vmPowerOff(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmPowerOff.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmRestart;
  public boolean vmRestart(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmRestart.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmSuspend;
  public boolean vmSuspend(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmSuspend.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmResume;
  public boolean vmResume(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmResume.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmGetState;
  public int vmGetState(String name) { try { Arena arena = Arena.ofAuto(); int _ret_value_ = (int)vmGetState.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle vmList;
  public String[] vmList() { try { String[] _ret_value_ = FFM.toArrayString((MemorySegment)vmList.invokeExact());return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmGet;
  public String vmGet(String name) { try { Arena arena = Arena.ofAuto(); String _ret_value_ = FFM.getString((MemorySegment)vmGet.invokeExact(arena.allocateFrom(name)));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmRegister;
  public boolean vmRegister(String xml) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmRegister.invokeExact(arena.allocateFrom(xml));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmUnregister;
  public boolean vmUnregister(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmUnregister.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmMigrate;
  public boolean vmMigrate(String name,String desthost,boolean live) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmMigrate.invokeExact(arena.allocateFrom(name),arena.allocateFrom(desthost),live);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmSnapshotCreate;
  public boolean vmSnapshotCreate(String name,String xml,int flags) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmSnapshotCreate.invokeExact(arena.allocateFrom(name),arena.allocateFrom(xml),flags);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmSnapshotList;
  public String[] vmSnapshotList(String name) { try { Arena arena = Arena.ofAuto(); String[] _ret_value_ = FFM.toArrayString((MemorySegment)vmSnapshotList.invokeExact(arena.allocateFrom(name)));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmSnapshotExists;
  public boolean vmSnapshotExists(String name) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmSnapshotExists.invokeExact(arena.allocateFrom(name));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmSnapshotGetCurrent;
  public String vmSnapshotGetCurrent(String name) { try { Arena arena = Arena.ofAuto(); String _ret_value_ = FFM.getString((MemorySegment)vmSnapshotGetCurrent.invokeExact(arena.allocateFrom(name)));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return null;} }

  private MethodHandle vmSnapshotRestore;
  public boolean vmSnapshotRestore(String name,String snapshot) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmSnapshotRestore.invokeExact(arena.allocateFrom(name),arena.allocateFrom(snapshot));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmSnapshotDelete;
  public boolean vmSnapshotDelete(String name,String snapshot) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmSnapshotDelete.invokeExact(arena.allocateFrom(name),arena.allocateFrom(snapshot));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmTotalMemory;
  public long vmTotalMemory() { try { long _ret_value_ = (long)vmTotalMemory.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle vmFreeMemory;
  public long vmFreeMemory() { try { long _ret_value_ = (long)vmFreeMemory.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle vmCpuLoad;
  public long vmCpuLoad() { try { long _ret_value_ = (long)vmCpuLoad.invokeExact();return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return -1;} }

  private MethodHandle vmGetAllStats;
  public boolean vmGetAllStats(int year,int month,int day,int hour,int sample) { try { boolean _ret_value_ = (boolean)vmGetAllStats.invokeExact(year,month,day,hour,sample);return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }

  private MethodHandle vmConnect;
  public boolean vmConnect(String remote) { try { Arena arena = Arena.ofAuto(); boolean _ret_value_ = (boolean)vmConnect.invokeExact(arena.allocateFrom(remote));return _ret_value_; } catch (Throwable t) { JFLog.log(t);  return false;} }


  private boolean ffm_init() {
    MethodHandle init;
    ffm = FFM.getInstance();
    init = ffm.getFunction("VMAPIinit", ffm.getFunctionDesciptor(ValueLayout.JAVA_BOOLEAN));
    if (init == null) return false;
    try {if (!(boolean)init.invokeExact()) return false;} catch (Throwable t) {JFLog.log(t); return false;}

    vmDeviceList = ffm.getFunctionPtr("_vmDeviceList", ffm.getFunctionDesciptor(ADDRESS,JAVA_INT));
    vmDiskCreate = ffm.getFunctionPtr("_vmDiskCreate", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS));
    vmNetworkListPhys = ffm.getFunctionPtr("_vmNetworkListPhys", ffm.getFunctionDesciptor(ADDRESS));
    vmSecretCreate = ffm.getFunctionPtr("_vmSecretCreate", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS));
    vmStorageList = ffm.getFunctionPtr("_vmStorageList", ffm.getFunctionDesciptor(ADDRESS));
    vmStorageRegister = ffm.getFunctionPtr("_vmStorageRegister", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmStorageUnregister = ffm.getFunctionPtr("_vmStorageUnregister", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmStorageStart = ffm.getFunctionPtr("_vmStorageStart", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmStorageStop = ffm.getFunctionPtr("_vmStorageStop", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmStorageGetState = ffm.getFunctionPtr("_vmStorageGetState", ffm.getFunctionDesciptor(JAVA_INT,ADDRESS));
    vmStorageGetUUID = ffm.getFunctionPtr("_vmStorageGetUUID", ffm.getFunctionDesciptor(ADDRESS,ADDRESS));
    vmInit = ffm.getFunctionPtr("_vmInit", ffm.getFunctionDesciptor(JAVA_BOOLEAN));
    vmStart = ffm.getFunctionPtr("_vmStart", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmStop = ffm.getFunctionPtr("_vmStop", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmPowerOff = ffm.getFunctionPtr("_vmPowerOff", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmRestart = ffm.getFunctionPtr("_vmRestart", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmSuspend = ffm.getFunctionPtr("_vmSuspend", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmResume = ffm.getFunctionPtr("_vmResume", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmGetState = ffm.getFunctionPtr("_vmGetState", ffm.getFunctionDesciptor(JAVA_INT,ADDRESS));
    vmList = ffm.getFunctionPtr("_vmList", ffm.getFunctionDesciptor(ADDRESS));
    vmGet = ffm.getFunctionPtr("_vmGet", ffm.getFunctionDesciptor(ADDRESS,ADDRESS));
    vmRegister = ffm.getFunctionPtr("_vmRegister", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmUnregister = ffm.getFunctionPtr("_vmUnregister", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmMigrate = ffm.getFunctionPtr("_vmMigrate", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,JAVA_BOOLEAN));
    vmSnapshotCreate = ffm.getFunctionPtr("_vmSnapshotCreate", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS,JAVA_INT));
    vmSnapshotList = ffm.getFunctionPtr("_vmSnapshotList", ffm.getFunctionDesciptor(ADDRESS,ADDRESS));
    vmSnapshotExists = ffm.getFunctionPtr("_vmSnapshotExists", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    vmSnapshotGetCurrent = ffm.getFunctionPtr("_vmSnapshotGetCurrent", ffm.getFunctionDesciptor(ADDRESS,ADDRESS));
    vmSnapshotRestore = ffm.getFunctionPtr("_vmSnapshotRestore", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS));
    vmSnapshotDelete = ffm.getFunctionPtr("_vmSnapshotDelete", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS,ADDRESS));
    vmTotalMemory = ffm.getFunctionPtr("_vmTotalMemory", ffm.getFunctionDesciptor(JAVA_LONG));
    vmFreeMemory = ffm.getFunctionPtr("_vmFreeMemory", ffm.getFunctionDesciptor(JAVA_LONG));
    vmCpuLoad = ffm.getFunctionPtr("_vmCpuLoad", ffm.getFunctionDesciptor(JAVA_LONG));
    vmGetAllStats = ffm.getFunctionPtr("_vmGetAllStats", ffm.getFunctionDesciptor(JAVA_BOOLEAN,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT,JAVA_INT));
    vmConnect = ffm.getFunctionPtr("_vmConnect", ffm.getFunctionDesciptor(JAVA_BOOLEAN,ADDRESS));
    return true;
  }
}

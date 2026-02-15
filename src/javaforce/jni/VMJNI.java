package javaforce.jni;

/** VM JNI
 *
 * @author pquiring
 */

import javaforce.webui.tasks.*;
import javaforce.api.*;
import javaforce.vm.*;

public class VMJNI implements VMAPI {

  private static VMAPI api;
  public static VMAPI getInstance() {
    if (api == null) {
      api = new VMJNI();
    }
    return api;
  }

  //Device
  public native String[] vmDeviceList(int type);

  //Disk
  public native boolean vmDiskCreate(String pool_name, String xml);

  //NetworkInterface
  public native String[] vmNetworkListPhys();

  //Secret
  public native boolean vmSecretCreate(String xml, String passwd);

  //Storage
  public native String[] vmStorageList();
  public native boolean vmStorageRegister(String xml);
  public native boolean vmStorageUnregister(String name);
  public native boolean vmStorageStart(String name);
  public native boolean vmStorageStop(String name);
  public native int vmStorageGetState(String name);
  public native String vmStorageGetUUID(String name);

  //VirtualMachine
  public native boolean vmInit();
  public native boolean vmStart(String name);
  public native boolean vmStop(String name);
  public native boolean vmPowerOff(String name);
  public native boolean vmRestart(String name);
  public native boolean vmSuspend(String name);
  public native boolean vmResume(String name);
  public native int vmGetState(String name);
  public native String[] vmList();
  public native String vmGet(String name);
  public native boolean vmRegister(String xml);
  public native boolean vmUnregister(String name);
  public native boolean vmMigrate(String name, String desthost, boolean live);
  public native boolean vmSnapshotCreate(String name, String xml, int flags);
  public native String[] vmSnapshotList(String name);
  public native boolean vmSnapshotExists(String name);
  public native String vmSnapshotGetCurrent(String name);
  public native boolean vmSnapshotRestore(String name, String snapshot);
  public native boolean vmSnapshotDelete(String name, String snapshot);

  //VMHost
  public native long vmTotalMemory();
  public native long vmFreeMemory();
  public native long vmCpuLoad();
  public native boolean vmGetAllStats(int year, int month, int day, int hour, int sample);
  public native boolean vmConnect(String remote);
}

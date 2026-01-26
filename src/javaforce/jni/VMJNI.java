package javaforce.jni;

/** VM JNI
 *
 * @author pquiring
 */

import javaforce.webui.tasks.*;
import javaforce.api.*;
import javaforce.vm.*;

public class VMJNI implements VMAPI {

  //Device
  public native String[] deviceList(int type);

  //Disk
  public native boolean diskCreate(String pool_name, String xml);

  //NetworkInterface
  public native String[] networkListPhys();

  //Secret
  public native boolean secretCreate(String xml, String passwd);

  //Storage
  public native String[] storageList();
  public native boolean storageRegister(String xml);
  public native boolean storageUnregister(String name);
  public native boolean storageStart(String name);
  public native boolean storageStop(String name);
  public native int storageGetState(String name);
  public native String storageGetUUID(String name);

  //VirtualMachine
  public native boolean init();
  public native boolean start(String name);
  public native boolean stop(String name);
  public native boolean poweroff(String name);
  public native boolean restart(String name);
  public native boolean suspend(String name);
  public native boolean resume(String name);
  public native int getState(String name);
  public native String[] list();
  public native String get(String name);
  public native boolean register(String xml);
  public native boolean unregister(String name);
  public native boolean migrate(String name, String desthost, boolean live, Status status);
  public native boolean snapshotCreate(String name, String xml, int flags);
  public native String[] snapshotList(String name);
  public native boolean snapshotExists(String name);
  public native String snapshotGetCurrent(String name);
  public native boolean snapshotRestore(String name, String snapshot);
  public native boolean snapshotDelete(String name, String snapshot);

  //VMHost
  public native long totalMemory();
  public native long freeMemory();
  public native long cpuLoad();
  public native boolean getAllStats(int year, int month, int day, int hour, int sample);
  public native boolean connect(String remote);
}

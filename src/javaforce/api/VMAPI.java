package javaforce.api;

/** VM Native API
 *
 * @author pquiring
 */

import javaforce.jni.*;
import javaforce.ffm.*;

public interface VMAPI {
  public static VMAPI getInstance() {
    if (FFM.enabled()) {
      return VMFFM.getInstance();
    } else {
      return VMJNI.getInstance();
    }
  }

  //Device
  public String[] vmDeviceList(int type);

  //Disk
  public boolean vmDiskCreate(String pool_name, String xml);

  //NetworkInterface
  public String[] vmNetworkListPhys();

  //Secret
  public boolean vmSecretCreate(String xml, String passwd);

  //Storage
  public String[] vmStorageList();
  public boolean vmStorageRegister(String xml);
  public boolean vmStorageUnregister(String name);
  public boolean vmStorageStart(String name);
  public boolean vmStorageStop(String name);
  public int vmStorageGetState(String name);
  public String vmStorageGetUUID(String name);

  //VirtualMachine
  public boolean vmInit();
  public boolean vmStart(String name);
  public boolean vmStop(String name);
  public boolean vmPowerOff(String name);
  public boolean vmRestart(String name);
  public boolean vmSuspend(String name);
  public boolean vmResume(String name);
  public int vmGetState(String name);
  public String[] vmList();
  public String vmGet(String name);
  public boolean vmRegister(String xml);
  public boolean vmUnregister(String name);
  public boolean vmMigrate(String name, String desthost, boolean live);
  public boolean vmSnapshotCreate(String name, String xml, int flags);
  public String[] vmSnapshotList(String name);
  public boolean vmSnapshotExists(String name);
  public String vmSnapshotGetCurrent(String name);
  public boolean vmSnapshotRestore(String name, String snapshot);
  public boolean vmSnapshotDelete(String name, String snapshot);

  //VMHost
  public long vmTotalMemory();
  public long vmFreeMemory();
  public long vmCpuLoad();
  public boolean vmGetAllStats(int year, int month, int day, int hour, int sample);
  public boolean vmConnect(String remote);

}

package javaforce.vm;

/** VM Native API
 *
 * @author pquiring
 */

import javaforce.webui.tasks.*;
import javaforce.jni.*;

public interface VMAPI {
  public static VMAPI getInstance() {
    return new VMJNI();
  }

  //Device
  public String[] deviceList(int type);

  //Disk
  public boolean diskCreate(String pool_name, String xml);

  //NetworkInterface
  public String[] networkListPhys();

  //Secret
  public boolean secretCreate(String xml, String passwd);

  //Storage
  public String[] storageList();
  public boolean storageRegister(String xml);
  public boolean storageUnregister(String name);
  public boolean storageStart(String name);
  public boolean storageStop(String name);
  public int storageGetState(String name);
  public String storageGetUUID(String name);

  //VirtualMachine
  public boolean init();
  public boolean start(String name);
  public boolean stop(String name);
  public boolean poweroff(String name);
  public boolean restart(String name);
  public boolean suspend(String name);
  public boolean resume(String name);
  public int getState(String name);
  public String[] list();
  public String get(String name);
  public boolean register(String xml);
  public boolean unregister(String name);
  public boolean migrate(String name, String desthost, boolean live, Status status);
  public boolean snapshotCreate(String name, String xml, int flags);
  public String[] snapshotList(String name);
  public boolean snapshotExists(String name);
  public String snapshotGetCurrent(String name);
  public boolean snapshotRestore(String name, String snapshot);
  public boolean snapshotDelete(String name, String snapshot);

  //VMHost
  public long totalMemory();
  public long freeMemory();
  public long cpuLoad();
  public boolean getAllStats(int year, int month, int day, int hour, int sample);
  public boolean connect(String remote);

}

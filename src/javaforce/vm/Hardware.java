package javaforce.vm;

/** Hardware setup for a Virtual Machine
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Hardware implements Serializable {
  private static final long serialVersionUID = 1L;

  public String pool;
  public String name;
  public String uuid;
  public String genid;
  public int os;
  public int cores;
  public Size memory;
  public Disk[] disks;
  public Network[] networks;
  public Device[] devices;

  public boolean auto_start;
  public boolean bios_efi;
  public boolean bios_secure;

  public static final int OS_LINUX = 1;
  public static final int OS_WINDOWS = 2;

  public Hardware() {
    //TODO
  }

  public Hardware(String pool, String name, int os, int cores, Size memory) {
    this.pool = pool;
    this.name = name;
    this.uuid = UUID.generate();
    this.genid = UUID.generate();
    this.os = os;
    this.cores = cores;
    this.memory = memory;
    disks = new Disk[0];
    networks = new Network[0];
    devices = new Device[0];
  }

  public static Hardware load(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      Hardware hardware = (Hardware)Compression.deserialize(fis, new File(file).length());
      fis.close();
      return hardware;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public synchronized boolean save(String file) {
    try {
      FileOutputStream fos = new FileOutputStream(file);
      boolean res = Compression.serialize(fos, this);
      fos.close();
      return res;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}

package jfnetboot;

/** FileSystem
 *
 * File Systems in /var/netboot/filesystems are read only
 * File Systems in /var/netboot/clients are read-write
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class FileSystem implements Cloneable {
  public String name;
  public String local;
  public String arch;  //x86 or arm
  public Client client;  //client file system

  private static boolean debug = false;

  /** File System
   * @param name = file system path
   */
  public FileSystem(String name, String arch) {
    this.name = name;
    this.arch = arch;
    this.local = Paths.filesystems + "/" + name + "-" + arch;
    new File(local).mkdir();
    new File(getRootPath()).mkdir();
  }

  public FileSystem(String name, String arch, Client client) {
    this.name = name;
    this.arch = arch;
    this.local = Paths.clients + "/" + name + "-" + arch;
    this.client = client;
    new File(local).mkdir();
    new File(getRootPath()).mkdir();
  }

  /** Creates a clone of a Client. */
  public FileSystem clone(String name, Runnable notify) {
    String dest = Paths.filesystems + "/" + name + "-" + arch;
    if (new File(dest).exists()) return null;
    //this will copy all folders/files
    FileSystem clone = new FileSystem(name, arch);
    JF.exec(new String[] {"cp", "-a", this.getRootPath(), clone.getLocalPath()});
    FileSystems.add(clone);
    if (notify != null) {
      notify.run();
    }
    return clone;
  }

  public String getName() {
    return name;
  }

  public String getArch() {
    return arch;
  }

  public void delete() {
    if (!isClientFileSystem()) {
      FileSystems.remove(name, arch);
    }
    new Thread() {
      public void run() {
        try {
          Runtime.getRuntime().exec(new String[] {"rm", "-rf", local});
        } catch (Exception e) {
        }
      }
    }.start();
  }

  private void deleteFiles(File path) {
    File files[] = path.listFiles();
    for(File file : files) {
      if (file.isDirectory()) {
        deleteFiles(file);
      } else {
        file.delete();
      }
    }
  }

  public void purge() {
    //delete all 'files' recursively
    if (!isClientFileSystem()) return;
    deleteFiles(new File(getRootPath()));
  }

  public String getLocalPath() {
    return local;
  }

  public String getRootPath() {
    return local + "/root";
  }

  private boolean isClientFileSystem() {
    return client != null;
  }

  public boolean archiving;
  public Runnable notify;

  public void archive(Runnable notify) {
    if (archiving) return;
    this.notify = notify;
    new Thread() {
      public void run() {
        archiving = true;
        Calendar c = Calendar.getInstance();
        String file = String.format("%s/archive-%04d-%02d-%02d_%02d-%02d-%02d.tar.xz"
          , local
          , c.get(Calendar.YEAR)
          , c.get(Calendar.MONTH + 1)
          , c.get(Calendar.DAY_OF_MONTH)
          , c.get(Calendar.HOUR_OF_DAY)
          , c.get(Calendar.MINUTE)
          , c.get(Calendar.SECOND)
        );
        JF.exec(new String[] {"tar", "cf", file, local + "/root"});
        archiving = false;
        if (notify != null) {
          notify.run();
        }
      }
    }.start();
  }
}

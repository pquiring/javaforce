/** Catalog
 *
 * The Catalog is stored on server (default retention = 1 year).
 *
 * filename = catalog-timestamp.dat
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;

public class Catalog implements Serializable {
  private static final long serialVersionUID = 1L;

  public ArrayList<EntryVolume> volumes = new ArrayList<EntryVolume>();
  public boolean haveChanger;
  public long backup;

  public boolean save() {
    String base = Paths.catalogsPath + "/catalog-" + backup;
    String raw = base + ".raw";
    String dat = base + ".dat";
    try {
      FileOutputStream fos = new FileOutputStream(raw);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(this);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    //compress raw -> dat
    try {
      FileInputStream fis = new FileInputStream(raw);
      FileOutputStream fos = new FileOutputStream(dat);
      Compression.compress(fis, fos, new File(raw).length());
      fis.close();
      fos.close();
      new File(raw).delete();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public static Catalog load(long backupid) {
    Catalog catalog;
    String base = Paths.catalogsPath + "/catalog-" + backupid;
    String raw = base + ".raw";
    String dat = base + ".dat";
    //decompress dat -> raw
    try {
      FileInputStream fis = new FileInputStream(dat);
      FileOutputStream fos = new FileOutputStream(raw);
      Compression.decompress(fis, fos, new File(dat).length());
      fis.close();
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
    try {
      FileInputStream fis = new FileInputStream(raw);
      ObjectInputStream ois = new ObjectInputStream(fis);
      catalog = (Catalog)ois.readObject();
      fis.close();
      new File(raw).delete();
      return catalog;
    } catch (FileNotFoundException e) {
      return null;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public static void deleteOld(long now) {
    //list backups
    File folder = new File(Paths.catalogsPath);
    File files[] = folder.listFiles();
    if (files == null) files = new File[0];

    for(File file : files) {
      String name = file.getName();  //catalog-###.nfo
      if (!name.startsWith("catalog-")) continue;
      if (!name.endsWith(".nfo")) continue;
      name = name.substring(8);  //remove catalog-
      name = name.substring(0, name.length() - 4);  //remove .nfo
      long backup = Long.valueOf(name);
      CatalogInfo info = CatalogInfo.load(backup);
      if (info.retention < now) {
        //delete old backup
        file.delete();
        new File(Paths.catalogsPath + "/catalog-" + backup + ".dat").delete();
      }
    }
  }
}

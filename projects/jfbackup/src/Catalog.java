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

  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(Paths.catalogsPath + "/catalog-" + backup + ".dat");
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(this);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static Catalog load(long backupid) {
    Catalog catalog;
    try {
      FileInputStream fis = new FileInputStream(Paths.catalogsPath + "/catalog-" + backupid + ".dat");
      ObjectInputStream ois = new ObjectInputStream(fis);
      catalog = (Catalog)ois.readObject();
      fis.close();
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

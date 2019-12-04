/** CatalogInfo
 *
 * The CatalogInfo is stored with the catalog.
 *
 * filename = catalog-timestamp.nfo
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;

public class CatalogInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  public ArrayList<EntryTape> tapes = new ArrayList<EntryTape>();
  public long backup;
  public long retention;
  public String name;  //backup name

  public void save() {
    try {
      FileOutputStream fos = new FileOutputStream(Paths.catalogsPath + "/catalog-" + backup + ".nfo");
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(this);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static CatalogInfo load(long backupid) {
    CatalogInfo catalog;
    try {
      FileInputStream fis = new FileInputStream(Paths.catalogsPath + "/catalog-" + backupid + ".nfo");
      ObjectInputStream ois = new ObjectInputStream(fis);
      catalog = (CatalogInfo)ois.readObject();
      fis.close();
      return catalog;
    } catch (FileNotFoundException e) {
      return null;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
}

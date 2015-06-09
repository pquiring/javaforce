
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import javaforce.JF;
import javaforce.JFLog;

/** Holds all library files.
 *
 * @author pquiring
 *
 * Created : Mar 14, 2014
 */

public class Library {
  public enum Type {WAV, DLS};

  public static class Entry {
    public Type type;
    public String name;
    public String fullPath;  //WAV only
    public int dls_idx;
  }

  private static ArrayList<Entry> libraryList = new ArrayList<Entry>();
  private static ArrayList<DLS> dlsList = new ArrayList<DLS>();

  public static void load() {
    if (!libraryList.isEmpty()) return;  //already done (must restart app to reload)
    File folder = new File("Library");
    if (folder.exists() && folder.isDirectory()) {
      File files[] = folder.listFiles();
      for(int a=0;a<files.length;a++) {
        if (!files[a].isFile()) continue;
        String name = files[a].getName().toLowerCase();
        if (name.endsWith(".wav")) {
          loadWAV(files[a]);
        }
        if (name.endsWith(".dls")) {
          loadDLS(files[a].getAbsolutePath());
        }
      }
    }
    File userFolder = new File(JF.getUserPath() + "/Music/Library");
    if (userFolder.exists() && userFolder.isDirectory()) {
      File files[] = userFolder.listFiles();
      for(int a=0;a<files.length;a++) {
        if (!files[a].isFile()) continue;
        String name = files[a].getName().toLowerCase();
        if (name.endsWith(".wav")) {
          loadWAV(files[a]);
        }
        if (name.endsWith(".dls")) {
          loadDLS(files[a].getAbsolutePath());
        }
      }
    }
    //add gm.dls (windows only)
    if (JF.isWindows()) {
      loadDLS(System.getenv("windir") + "\\System32\\Drivers\\gm.dls");
    }
  }

  public static void loadWAV(File file) {
    String name = file.getName();
    Library.Entry entry = new Library.Entry();
    entry.type = Library.Type.WAV;
    entry.name = name;
    entry.fullPath = file.getAbsolutePath();
    libraryList.add(entry);
  }

  public static void loadDLS(String filename) {
    try {
      DLS dls = new DLS();
      dls.load(new FileInputStream(filename));
      String names[] = dls.getInstrumentNames();
      int dls_idx = dlsList.size();
      for(int a=0;a<names.length;a++) {
        Library.Entry entry = new Library.Entry();
        entry.type = Library.Type.DLS;
        entry.name = names[a];
        entry.dls_idx = dls_idx;
        libraryList.add(entry);
      }
      dlsList.add(dls);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static ArrayList<Entry> getList() {
    return libraryList;
  }

  public static Entry get(int idx) {
    return libraryList.get(idx);
  }

  public static DLS getDLS(int idx) {
    return dlsList.get(idx);
  }
}

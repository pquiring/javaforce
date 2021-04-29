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
  public String derived_from;

  private NFolder rootFolder;
  private String boot_path;
  private String root_path;
  private HashMap<Long, NFile> all_files = new HashMap();
  private HashMap<Long, NFolder> all_folders = new HashMap();
  private HashMap<Long, NHandle> all_handles = new HashMap();
  private HashMap<String, Long> all_names = new HashMap();
  private Object lock = new Object();
  private HashMap<Long, RandomAccessFile> files = new HashMap<>();

  private static boolean debug = false;

  /** File System
   * @param name = file system path
   */
  public FileSystem(String name, String arch) {
    this.name = name;
    this.arch = arch;
    local = Paths.filesystems + "/" + name + "-" + arch;
    boot_path = local + "/boot";
    root_path = local + "/root";
    mkdirs();
    this.rootFolder = new NFolder();
  }

  /** File System : ctor to create derived copy.
   * @param that = FileSystem to clone
   * @param derived_local = derived file system
   */
  public FileSystem(String local, String name, FileSystem that, boolean copy) {
    this.name = name;
    this.arch = that.arch;
    this.derived_from = that.name;
    this.local = local;
    boot_path = that.boot_path;  //read-only
    root_path = this.local + "/root";
    mkdirs();
    this.rootFolder = that.rootFolder.clone(root_path);
    this.base_id = that.base_id + clone_id;
    this.next_id = base_id;
    if (copy) {
      JFLog.log("cp -r " + that.getRootPath() + " " + this.getRootPath());
      JF.exec(new String[] {"cp", "-r", that.getRootPath(), this.getRootPath()});
    }
    cloneBuildFolder(this.rootFolder);
  }

  private void mkdirs() {
    new File(local).mkdir();
    new File(getRootPath()).mkdir();
    if (true) {
      //symlink : boot -> root/boot
      new File(getRootPath() + "/boot").mkdir();
      if (!new File(getBootPath()).exists()) {
        FileOps.createSymbolicLink(getBootPath(), getRootPath() + "/boot");
      }
    } else {
      //seperate boot folder (deprecated)
      new File(getBootPath()).mkdir();
    }
  }

  /** Build hashes after a clone. */
  private void cloneBuildFolder(NFolder pfolder) {
    all_folders.put(pfolder.handle, pfolder);
    all_handles.put(pfolder.handle, pfolder);
    all_names.put(pfolder.path, pfolder.handle);
    for(NFolder cfolder : pfolder.cfolders) {
      cloneBuildFolder(cfolder);
    }
    for(NFile cfile : pfolder.cfiles) {
      all_files.put(cfile.handle, cfile);
      all_handles.put(cfile.handle, cfile);
      all_names.put(cfile.path, cfile.handle);
    }
  }

  /** Creates a clone for a Client. */
  public FileSystem clone(Client client) {
    String name = client.getSerial();
    //this will only copy folders
    return new FileSystem(Paths.clients + "/" + name + "-" + arch, name, this, false);
  }

  /** Creates a clone of a Client. */
  public FileSystem clone(String name, FileSystem derived, Runnable notify) {
    String dest = Paths.filesystems + "/" + name + "-" + arch;
    if (new File(dest).exists()) return null;
    //this will copy all folders/files
    FileSystem clone = new FileSystem(Paths.filesystems + "/" + name + "-" + arch, name, this, true);
    clone.derived_from = derived_from;  //does NOT derive from Client FileSystem
    clone.save();
    FileSystems.add(clone);
    if (notify != null) {
      notify.run();
    }
    return clone;
  }

  public boolean canIndex() {
    if (derived_from == null) return true;
    return FileSystems.get(derived_from, arch) != null;
  }

  public String getName() {
    return name;
  }

  public String getArch() {
    return arch;
  }

  public FileSystem getDerived() {
    if (derived_from == null) return null;
    return FileSystems.get(derived_from, arch);
  }

  public FileSystem getBase() {
    FileSystem fs = this;
    FileSystem derived = this;
    do {
      derived = derived.getDerived();
      if (derived != null) {
        fs = derived;
      }
    } while (derived != null);
    return fs;
  }

  public static String getConfigFile(String name, String arch) {
    return Paths.filesystems + "/" + name + "-" + arch + ".cfg";
  }

  public static FileSystem load(String name, String arch) {
    try {
      FileInputStream fis = new FileInputStream(getConfigFile(name, arch));
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      FileSystem fs = new FileSystem(name, arch);
      fs.derived_from = props.getProperty("derived-from");
      fs.next_id = Long.valueOf(props.getProperty("next-id"), 16);
      fs.base_id = Long.valueOf(props.getProperty("base-id"), 16);
      return fs;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public void save() {
    if (isClientFileSystem()) {
      //Client filesystem - do not save
      return;
    }
    try {
      Properties props = new Properties();
      if (derived_from != null) {
        props.setProperty("derived-from", derived_from);
      }
      props.setProperty("next-id", Long.toUnsignedString(next_id, 16));
      props.setProperty("base-id", Long.toUnsignedString(base_id, 16));
      FileOutputStream fos = new FileOutputStream(getConfigFile(name, arch));
      props.store(fos, "FileSystem Settings");
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void delete() {
    if (!isClientFileSystem()) {
      new File(getConfigFile(name, arch)).delete();
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

  public String getBootPath() {
    return boot_path;
  }

  public String getRootPath() {
    return root_path;
  }

  public NFolder getRootFolder() {
    return rootFolder;
  }

  private boolean isClientFileSystem() {
    return local.startsWith(Paths.clients);
  }

  /** Index a file system.
   */
  public void index() {
    JFLog.log("FileSystem.index():name=" + name + ":derived_from=" + derived_from + ":arch=" + arch);
    if (derived_from == null) {
      synchronized(lock) {
        doFolder(getRootPath(), "", rootFolder);
      }
    } else {
      synchronized(lock) {
        doFolderDerived(getRootPath(), "", rootFolder, false);
      }
    }
    save();
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

  private Object hash_lock = new Object();
  private static final long clone_id = 0x0001000000000000L;
  private long base_id = clone_id;
  private long next_id = clone_id;

  /** Creates unique id for path. */
  private long hash(String path) {
    //BUG : during index() and rename() can not use hash() to generate a new handle (fileid) : client expects handle to not change
    //    : but if the server is restarted the handles are all regenerated and the clients will most likely need to restart as well
    //    : or else the client will most likely complain that fileid's have changed
    synchronized(hash_lock) {
      return next_id++;
    }
  }

  private void doFolder(String pfull, String pname, NFolder pfolder) {
    pfolder.name = pname;
    if (pfolder.parent != null) {
      //sub folder
      pfolder.path = pfolder.parent.path + "/" + pname;
    } else {
      //root folder
      pfolder.path = pname;
    }
    pfolder.local = pfull;
    pfolder.handle = hash(pfolder.path);
    if (all_handles.get(pfolder.handle) == null) {
      all_folders.put(pfolder.handle, pfolder);
      all_handles.put(pfolder.handle, pfolder);
      all_names.put(pfolder.path, pfolder.handle);
    } else {
      JFLog.log("Warning:duplicate hash code folder:" + pfolder.path);
      return;
    }
    File file = new File(pfull);
    File[] children = file.listFiles();
    for(File child : children) {
      String cname = child.getName();
      boolean folder = child.isDirectory();
      boolean symlink = FileOps.isSymlink(child);
      if (folder && !symlink) {
        String cfull = pfull + "/" + cname;
        NFolder cfolder = new NFolder();
        cfolder.parent = pfolder;
        pfolder.cfolders.add(cfolder);
        doFolder(cfull, cname, cfolder);
      } else {
        String cfull = pfull + "/" + cname;
        NFile cfile = new NFile();
        cfile.parent = pfolder;
        cfile.name = cname;
        cfile.path = pfolder.path + "/" + cname;
        cfile.local = cfull;
        long chandle = hash(cfile.path);
        if (symlink) {
          cfile.symlink = FileOps.readSymlink(child);
        }
        if (all_handles.get(chandle) == null) {
          cfile.handle = chandle;
          all_files.put(chandle, cfile);
          all_handles.put(chandle, cfile);
          all_names.put(cfile.path, chandle);
          pfolder.cfiles.add(cfile);
        } else {
          JFLog.log("Warning:duplicate hash code file:" + cfile.path);
        }
      }
    }
  }

  private void doFolderDerived(String pfull, String pname, NFolder pfolder, boolean newFolder) {
    pfolder.local = pfull;  //MUST use derived folder
    new File(pfolder.local).mkdir();  //MUST create all folders in derived file system so new files can be created and folders can be renamed
    pfolder.converted = true;
    for(NFolder cfolder : pfolder.cfolders) {
      cfolder.converted = false;
    }
    if (newFolder) {
      pfolder.name = pname;
      if (pfolder.parent != null) {
        //sub folder
        pfolder.path = pfolder.parent.path + "/" + pname;
      } else {
        //root folder
        pfolder.path = pname;
      }
      pfolder.handle = hash(pfolder.path);
      if (all_handles.get(pfolder.handle) == null) {
        all_folders.put(pfolder.handle, pfolder);
        all_handles.put(pfolder.handle, pfolder);
        all_names.put(pfolder.path, pfolder.handle);
      } else {
        JFLog.log("Warning(2):duplicate hash code folder:" + pfolder.path);
        return;
      }
    }
    File file = new File(pfull);
    File[] children = file.listFiles();
    for(File child : children) {
      String cname = child.getName();
      boolean folder = child.isDirectory();
      boolean symlink = FileOps.isSymlink(child);
      if (folder && !symlink) {
        String cfull = pfull + "/" + cname;
        NFolder cfolder;
        boolean childNewFolder;
        NFile cfile = pfolder.getFile(cname);
        if (cfile != null) {
          if (cfile.symlink != null) {
            JFLog.log("Warning:symlink in base filesystem has real folder in derived system:" + cfull + " (ignoring)");
          }
          continue;
        }
        cfolder = pfolder.getFolder(cname);
        if (cfolder == null) {
          childNewFolder = true;
          cfolder = new NFolder();
          pfolder.cfolders.add(cfolder);
        } else {
          childNewFolder = false;
        }
        cfolder.parent = pfolder;
        doFolderDerived(cfull, cname, cfolder, childNewFolder);
      } else {
        String cfull = pfull + "/" + cname;
        NFile cfile;
        cfile = pfolder.getFile(cname);
        if (cfile == null) {
          cfile = new NFile();
          cfile.name = cname;
          cfile.path = pfolder.path + "/" + cname;
          cfile.local = cfull;
          long chandle = hash(cfile.path);
          if (symlink) {
            cfile.symlink = FileOps.readSymlink(child);
          }
          if (all_handles.get(chandle) == null) {
            cfile.handle = chandle;
            all_files.put(chandle, cfile);
            all_handles.put(chandle, cfile);
            all_names.put(cfile.path, chandle);
            pfolder.cfiles.add(cfile);
          } else {
            JFLog.log("Warning(2):duplicate hash code file:" + cfile.path);
          }
        } else {
          cfile.local = cfull;
        }
        cfile.rw = true;
        cfile.parent = pfolder;
      }
    }
    //convert remaining folders to derived folders
    for(NFolder cfolder : pfolder.cfolders) {
      if (cfolder.converted) continue;
      String cfull = pfull + "/" + cfolder.name;
      doFolderDerived(cfull, cfolder.name, cfolder, false);
    }
  }

  /** Return root handle. */
  public long getRootHandle() {
    return rootFolder.handle;
  }

  /** Checks if a file/folder exists. */
  public boolean exists(long handle) {
    return all_handles.get(handle) != null;
  }

  /** Checks if a file/folder exists in provided folder. */
  public boolean exists(long handle, String name) {
    NFolder folder = getFolder(handle);
    if (folder == null) return false;
    return folder.getHandle(name) != null;
  }

  /** Get handle relative to dir handle. */
  public long getHandle(long dir, String filename) {
    String fullPath = getPath(dir) + "/" + filename;
    Long handle = all_names.get(fullPath);
    if (handle == null) {
      return -1;
    }
    return handle;
  }

  /** Return local path for handle. */
  public String getLocalPath(long handle) {
    synchronized(lock) {
      NHandle nhandle = all_handles.get(handle);
      if (nhandle == null) {
        return null;
      }
      if (nhandle.local == null) {
        JFLog.log("FileSystem:Error:local==null:" + nhandle.path + "/" + nhandle.name);
        if (debug) System.exit(1);
        return null;
      }
      return nhandle.local;
    }
  }

  /** Return relative path for handle. */
  public String getPath(long handle) {
    synchronized(lock) {
      NHandle nhandle = all_handles.get(handle);
      if (nhandle == null) {
        return null;
      }
      return nhandle.path;
    }
  }

  /** Return name for handle. */
  public String getName(long handle) {
    synchronized(lock) {
      NHandle nhandle = all_handles.get(handle);
      if (nhandle == null) {
        return null;
      }
      return nhandle.name;
    }
  }

  public NFile getFile(long handle) {
    return all_files.get(handle);
  }

  public NFolder getFolder(long handle) {
    return all_folders.get(handle);
  }

  public NHandle getHandle(long handle) {
    return all_handles.get(handle);
  }

  /** Return sub-folders for folder handle. */
  public long[] getFolders(long dir) {
    synchronized(lock) {
      NFolder folder = all_folders.get(dir);
      if (folder == null) return null;
      long[] list = new long[folder.cfolders.size()];
      int idx = 0;
      for(NFolder child : folder.cfolders) {
        list[idx++] = child.handle;
      }
      return list;
    }
  }

  /** Return files for folder handle. */
  public long[] getFiles(long dir) {
    synchronized(lock) {
      NFolder folder = all_folders.get(dir);
      if (folder == null) return null;
      long[] list = new long[folder.cfiles.size()];
      int idx = 0;
      for(NFile child : folder.cfiles) {
        list[idx++] = child.handle;
      }
      return list;
    }
  }

  /** Returns all files/folders for handle (including "." and "..") */
  public long[] getAllFiles(long dir) {
    if (!isDirectory(dir)) return null;
    NFolder folder = getFolder(dir);
    long[] folders = getFolders(dir);
    long[] files = getFiles(dir);
    long[] all = new long[2 + folders.length + files.length];
    System.arraycopy(folders, 0, all, 2, folders.length);
    System.arraycopy(files, 0, all, 2 + folders.length, files.length);
    all[0] = dir;  //"."
    if (folder.parent == null) {
      all[1] = dir;  //".." = root
    } else {
      all[1] = folder.parent.handle;  //".." = parent
    }
    return all;
  }

  private long createHandle(long dir, String file) {
    String path = getPath(dir);
    String full = path + "/" + file;
    return hash(full);
  }

  public boolean isDirectory(long handle) {
    synchronized(lock) {
      NHandle folder = all_folders.get(handle);
      if (folder == null) return false;
      return folder.isFolder();
    }
  }

  //not really needed - the client will resolve symlinks on it's own
  private String resolveSymlink(NFile file) {
    if (file.symlink == null) return null;
    String symlink = file.symlink;
    String[] parts = symlink.split("[/]");
    NHandle target;
    if (symlink.startsWith("/")) {
      target = rootFolder;
    } else {
      target = file.parent;
      if (target == null) {
        JFLog.log("FileSystem:Failed to resolve symlink(1):" + file.symlink);
        return null;
      }
    }
    for(int a=0;a<parts.length;a++) {
      String part = parts[a];
      if (part.length() == 0) continue;
      if (part.equals(".")) continue;
      if (part.equals("..")) {
        target = target.parent;
        if (target == null) {
          JFLog.log("FileSystem:Failed to resolve symlink(2):" + file.symlink);
          return null;
        }
        continue;
      }
      if (!target.isFolder()) {
        JFLog.log("FileSystem:Failed to resolve symlink(3):" + file.symlink);
        return null;
      }
      NFolder folder = (NFolder)target;
      target = folder.getHandle(part);
      if (target == null) {
        JFLog.log("FileSystem:Failed to resolve symlink(4):" + file.symlink);
        return null;
      }
    }
    return target.local;
  }

  public RandomAccessFile openFile(NFile file, boolean write) {
    synchronized(lock) {
      RandomAccessFile raf = getOpenFile(file.handle);
      if (write && !file.rw) {
        if (debug) JFLog.log("MakeCopy:" + file.local);
        if (file.symlink != null) {
          JFLog.log("MakeCopy:Error:symlink???");
        }
        file.makeCopy(getRootPath());
        if (raf != null) {
          try { raf.close(); } catch (Exception e) {}
          removeOpenFile(file.handle);
          raf = null;
        }
      }
      if (raf != null) return raf;
      String path;
      if (file.symlink == null) {
        path = file.local;
      } else {
        path = resolveSymlink(file);
        if (path == null) return null;
        if (debug) JFLog.log("ResolveSymLink=" + path);
      }
      if (!new File(path).exists()) return null;
      try {
        raf = new RandomAccessFile(path, write | file.rw ? "rw" : "r");
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
      setOpenFile(file.handle, raf);
      return raf;
    }
  }

  private RandomAccessFile getOpenFile(long handle) {
    synchronized(lock) {
      return files.get(handle);
    }
  }

  private void setOpenFile(long handle, RandomAccessFile raf) {
    synchronized(lock) {
      files.put(handle, raf);
    }
  }

  private void removeOpenFile(long handle) {
    synchronized(lock) {
      RandomAccessFile raf = getOpenFile(handle);
      if (raf != null) {
        try {raf.close();} catch (Exception e) {}
      }
      files.remove(handle);
    }
  }

  private void closeOpenFile(NFile file) {
    RandomAccessFile raf = getOpenFile(file.handle);
    if (raf != null) {
      removeOpenFile(file.handle);
    }
  }

  public long create(long dir, String filename) {
    NFolder pfolder = getFolder(dir);
    if (pfolder == null) return -1;
    long handle = createHandle(dir, filename);
    NFile cfile = new NFile(handle, getRootPath() + pfolder.path + "/" + filename, pfolder.path + "/" + filename, filename);
    cfile.rw = true;
    FileOps.create(cfile.local);
    cfile.parent = pfolder;
    pfolder.cfiles.add(cfile);
    all_files.put(handle, cfile);
    all_names.put(cfile.path, handle);
    all_handles.put(handle, cfile);
    return handle;
  }

  public long mkdir(long dir, String filename) {
    NFolder pfolder = getFolder(dir);
    if (pfolder == null) return -1;
    long handle = createHandle(dir, filename);
    NFolder cfolder = new NFolder(handle, getRootPath() + pfolder.path + "/" + filename, pfolder.path + "/" + filename, filename);
    new File(cfolder.local).mkdir();
    cfolder.rw = true;
    pfolder.cfolders.add(cfolder);
    all_folders.put(handle, cfolder);
    all_names.put(cfolder.path, handle);
    all_handles.put(handle, cfolder);
    return handle;
  }

  public long create_symlink(long where, String where_name, String to) {
    NFolder pfolder = getFolder(where);
    if (pfolder == null) return -1;
    if (pfolder.getFile(where_name) != null) return -1;
    String link_local = getLocalPath(where) + "/" + where_name;
    if (!FileOps.createSymbolicLink(link_local, to)) return -1;
    long handle = createHandle(where, where_name);
    NFile cfile = new NFile(handle, getRootPath() + pfolder.path + "/" + where_name, pfolder.path + "/" + where_name, where_name);
    cfile.rw = true;
    cfile.parent = pfolder;
    cfile.symlink = to;
    pfolder.cfiles.add(cfile);
    all_files.put(handle, cfile);
    all_names.put(cfile.path, cfile.handle);
    all_handles.put(handle, cfile);
    return handle;
  }

  public boolean remove(long where, String name) {
    NFolder pfolder = getFolder(where);
    NFile file = pfolder.getFile(name);
    if (file == null) return false;
    if (file.rw) {
      closeOpenFile(file);
      FileOps.delete(file.local);
    }
    pfolder.cfiles.remove(file);
    all_files.remove(file.handle);
    all_names.remove(file.path);
    all_handles.remove(file.handle);
    return true;
  }

  public boolean rmdir(long where, String name) {
    NFolder pfolder = getFolder(where);
    NFolder cfolder = pfolder.getFolder(name);
    if (cfolder == null) return false;
    if (cfolder.cfolders.size() > 0) return false;
    if (cfolder.cfiles.size() > 0) return false;
    FileOps.rmdir(cfolder.local);
    pfolder.cfolders.remove(cfolder);
    all_files.remove(cfolder.handle);
    all_names.remove(cfolder.path);
    all_handles.remove(cfolder.handle);
    return true;
  }

  public boolean rename(long from_dir, String from_name, long to_dir, String to_name) {
    NFolder from_folder = getFolder(from_dir);
    if (from_folder == null) {
      if (debug) JFLog.log("RENAME:from_folder==null");
      return false;
    }
    NHandle from = from_folder.getHandle(from_name);
    if (from == null) {
      if (debug) JFLog.log("RENAME:from==null");
      return false;
    }
    NFolder to_folder = getFolder(to_dir);
    if (to_folder == null) {
      if (debug) JFLog.log("RENAME:to_folder==null");
      return false;
    }
    NHandle to = to_folder.getHandle(to_name);

    //remove from
    from_folder.remove(from);
    all_handles.remove(from.handle);
    all_names.remove(from.path);
    long to_handle = from.handle;
    if (from.isFile()) {
      all_files.remove(from.handle);
      //remove to if it exists
      if (to != null) {
        if (to.isFolder()) {
          if (debug) JFLog.log("RENAME:not compatible:file->folder");
          return false;
        }
        if (to.rw) {
          closeOpenFile((NFile)to);
          FileOps.delete(to.local);
        }
        to_folder.cfiles.remove(to);
        all_files.remove(to.handle);
        all_names.remove(to.path);
        all_handles.remove(to.handle);
      }
      to = new NFile(to_handle, getRootPath() + to_folder.path + "/" + to_name, to_folder.path + "/" + to_name, to_name);
    } else {
      all_folders.remove(from.handle);
      //remove to if it exists (and is empty)
      if (to != null) {
        if (to.isFile()) {
          if (debug) JFLog.log("RENAME:not compatible:folder->file");
          return false;
        }
        NFolder to_to_folder = (NFolder)to;
        if (to_to_folder.cfiles.size() > 0 || to_to_folder.cfolders.size() > 0) {
          if (debug) JFLog.log("RENAME:not compatible:dest not empty");
          return false;
        }
        FileOps.rmdir(to.local);
        to_folder.cfolders.remove(to);
        all_folders.remove(to.handle);
        all_names.remove(to.path);
        all_handles.remove(to.handle);
      }
      to = new NFolder(to_handle, getRootPath() + to_folder.path + "/" + to_name, to_folder.path + "/" + to_name, to_name);
    }
    to.rw = true;

    //close from
    if (from.isFile() && from.rw) {
      closeOpenFile((NFile)from);
    }

    //rename/copy file
    if (from.isFile()) {
      if (from.rw) {
        //from is on read-write file system so just actually rename it
        FileOps.renameFile(from.local, to.local);
      } else {
        //from is on read-only file system, need to make a read-write copy
        if (from.symlink != null) {
          FileOps.createSymbolicLink(to.local, from.symlink);
        } else {
          FileOps.copyFile(from.local, to.local);
        }
      }
      if (from.symlink != null) {
        to.symlink = from.symlink;
      }
    } else {
      FileOps.renameFile(from.local, to.local);
    }

    //add to
    to_folder.add(to);
    all_handles.put(to.handle, to);
    all_names.put(to.path, to.handle);
    if (from.isFile()) {
      all_files.put(to.handle, (NFile)to);
    } else {
      all_folders.put(to.handle, (NFolder)to);
    }
    return true;
  }

  /** create hard link */
  public boolean create_link(long target, long link_dir, String link_name) {
    NHandle ntarget = getHandle(target);
    if (ntarget.isFile() && !ntarget.rw) {
      NFile nfile = (NFile)ntarget;
      nfile.makeCopy(getRootPath());
    }
    NFolder linkdir = getFolder(link_dir);
    String link_local = linkdir.local + "/" + link_name;
    String link_path = linkdir.path + "/" + link_name;
    if (!FileOps.createLink(link_local, ntarget.local)) return false;
    long handle = createHandle(link_dir, link_name);
    NHandle nlink;
    if (ntarget.isFile()) {
      //add file
      nlink = new NFile(handle, link_local, link_path, link_name);
    } else {
      //add folder
      nlink = new NFolder(handle, link_local, link_path, link_name);
    }
    nlink.rw = true;
    nlink.parent = linkdir;
    linkdir.add(nlink);

    all_handles.put(nlink.handle, nlink);
    all_names.put(nlink.path, nlink.handle);
    if (ntarget.isFile()) {
      all_files.put(nlink.handle, (NFile)nlink);
    } else {
      all_folders.put(nlink.handle, (NFolder)nlink);
    }

    return true;
  }

  //SETATTR methods

  public boolean setattr_size(long handle, long size) {
    NFile file = getFile(handle);
    RandomAccessFile raf = openFile(file, true);
    try {
      raf.getChannel().truncate(size);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public boolean setattr_mode(long handle, int mode) {
    NHandle nhandle = getHandle(handle);
    if (!nhandle.rw) {
      if (nhandle.isFile()) {
        NFile file = (NFile)nhandle;
        file.makeCopy(getRootPath());
      }
    }
    return FileOps.setMode(nhandle.local, mode);
  }

  public boolean setattr_uid(long handle, int id) {
    NHandle nhandle = getHandle(handle);
    if (!nhandle.rw) {
      if (nhandle.isFile()) {
        NFile file = (NFile)nhandle;
        file.makeCopy(getRootPath());
      }
    }
    return FileOps.setUID(nhandle.local, id);
  }

  public boolean setattr_gid(long handle, int id) {
    NHandle nhandle = getHandle(handle);
    if (!nhandle.rw) {
      if (nhandle.isFile()) {
        NFile file = (NFile)nhandle;
        file.makeCopy(getRootPath());
      }
    }
    return FileOps.setUID(nhandle.local, id);
  }

  public boolean setattr_atime(long handle, long ts) {
    NHandle nhandle = getHandle(handle);
    if (!nhandle.rw) {
      if (nhandle.isFile()) {
        NFile file = (NFile)nhandle;
        file.makeCopy(getRootPath());
      }
    }
    return FileOps.setATime(nhandle.local, ts);
  }

  public boolean setattr_mtime(long handle, long ts) {
    NHandle nhandle = getHandle(handle);
    if (!nhandle.rw) {
      if (nhandle.isFile()) {
        NFile file = (NFile)nhandle;
        file.makeCopy(getRootPath());
      }
    }
    return FileOps.setMTime(nhandle.local, ts);
  }
}

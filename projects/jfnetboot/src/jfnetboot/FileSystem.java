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

  private NFolder root;
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
    this.local = Paths.filesystems + "/" + name + "-" + arch;
    this.root = new NFolder();
    new File(local).mkdir();
    new File(getRootPath()).mkdir();
  }

  public FileSystem(String name, String arch, Client client) {
    this.name = name;
    this.arch = arch;
    this.local = Paths.clients + "/" + name + "-" + arch;
    this.root = new NFolder();
    this.client = client;
    new File(local).mkdir();
    new File(getRootPath()).mkdir();
  }

  /** Creates a clone of a Client. */
  public FileSystem clone(String name, Runnable notify) {
    String dest = Paths.filesystems + "/" + name + "-" + arch;
    if (new File(dest).exists()) return null;
    //this will copy all folders/files
    FileSystem clone = new FileSystem(Paths.filesystems + "/" + name + "-" + arch, name);
    JFLog.log("cp -a " + this.getRootPath() + " " + clone.getRootPath());
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

  public String getRootPath() {
    return local + "/root";
  }

  public NFolder getRootFolder() {
    return root;
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

  /** Index a file system.
   */
  public void index() {
    JFLog.log("FileSystem.index():name=" + name + ":arch=" + arch);
    if (Settings.current.nfs_server) return;
    synchronized(lock) {
      indexFolder(getRootPath(), "", root);
    }
  }

  private void indexFolder(String pfull, String pname, NFolder pfolder) {
    pfolder.name = pname;
    if (pfolder.parent != null) {
      //sub folder
      pfolder.path = pfolder.parent.path + "/" + pname;
    } else {
      //root folder
      pfolder.path = pname;
    }
    pfolder.local = pfull;
    pfolder.handle = FileOps.getID(pfolder.local);
    while (all_handles.get(pfolder.handle) != null) {
      pfolder.handle += NHandle.HARD_LINK;
    }
    all_folders.put(pfolder.handle, pfolder);
    all_handles.put(pfolder.handle, pfolder);
    all_names.put(pfolder.path, pfolder.handle);
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
        indexFolder(cfull, cname, cfolder);
      } else {
        String cfull = pfull + "/" + cname;
        NFile cfile = new NFile();
        cfile.parent = pfolder;
        cfile.name = cname;
        cfile.path = pfolder.path + "/" + cname;
        cfile.local = cfull;
        cfile.handle = FileOps.getID(cfile.local);
        while (all_handles.get(cfile.handle) != null) {
          cfile.handle += NHandle.HARD_LINK;
        }
        cfile.handle = cfile.handle;
        all_files.put(cfile.handle, cfile);
        all_handles.put(cfile.handle, cfile);
        all_names.put(cfile.path, cfile.handle);
        pfolder.cfiles.add(cfile);
      }
    }
  }

  /** Return root handle. */
  public long getRootHandle() {
    return root.handle;
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

  public boolean isDirectory(long handle) {
    synchronized(lock) {
      NHandle folder = all_folders.get(handle);
      if (folder == null) return false;
      return folder.isFolder();
    }
  }

  public RandomAccessFile openFile(NFile file, boolean write) {
    synchronized(lock) {
      RandomAccessFile raf = getOpenFile(file.handle);
      if (raf != null) return raf;
      if (!new File(file.local).exists()) return null;
      try {
        raf = new RandomAccessFile(file.local, "rw");
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

  public void closeAllFiles() {
    synchronized(lock) {
      RandomAccessFile[] rafs = files.values().toArray(new RandomAccessFile[0]);
      for(RandomAccessFile raf : rafs) {
        try {raf.close();} catch (Exception e) {JFLog.log(e);}
      }
      files.clear();
    }
  }

  public long create(long dir, String filename) {
    NFolder pfolder = getFolder(dir);
    if (pfolder == null) return -1;
    String nlocal = getRootPath() + pfolder.path + "/" + filename;
    long handle = FileOps.create(nlocal);
    while (all_handles.get(handle) != null) {
      handle += NHandle.HARD_LINK;
    }
    NFile cfile = new NFile(handle, nlocal, pfolder.path + "/" + filename, filename);
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
    String nlocal = getRootPath() + pfolder.path + "/" + filename;
    new File(nlocal).mkdir();
    long handle = FileOps.getID(nlocal);
    while (all_handles.get(handle) != null) {
      handle += NHandle.HARD_LINK;
    }
    NFolder cfolder = new NFolder(handle, nlocal, pfolder.path + "/" + filename, filename);
    pfolder.cfolders.add(cfolder);
    all_folders.put(handle, cfolder);
    all_names.put(cfolder.path, handle);
    all_handles.put(handle, cfolder);
    return handle;
  }

  public long create_symlink(long where, String where_name, String to) {
    NFolder pfolder = getFolder(where);
    if (pfolder == null) {
      JFLog.log("Error:FileSystem.create_symlink():folder not found:" + where_name);
      return -1;
    }
    if (pfolder.getFile(where_name) != null) {
      JFLog.log("Error:FileSystem.create_symlink():file not found:" + where_name);
      return -1;
    }
    String link_local = getLocalPath(where) + "/" + where_name;
    if (!FileOps.createSymbolicLink(link_local, to)) {
      return -1;
    }
    long handle = FileOps.getID(link_local);
    while (all_handles.get(handle) != null) {
      handle += NHandle.HARD_LINK;
    }
    NFile cfile = new NFile(handle, getRootPath() + pfolder.path + "/" + where_name, pfolder.path + "/" + where_name, where_name);
    cfile.parent = pfolder;
    pfolder.cfiles.add(cfile);
    all_files.put(handle, cfile);
    all_names.put(cfile.path, cfile.handle);
    all_handles.put(handle, cfile);
    return handle;
  }

  public boolean remove(long where, String name) {
    NFolder pfolder = getFolder(where);
    if (pfolder == null) {
      JFLog.log("Error:FileSystem.remove():folder not found:" + pfolder.path);
      return false;
    }
    NFile file = pfolder.getFile(name);
    if (file == null) {
      JFLog.log("Error:FileSystem.remove():file not found:" + pfolder.path + "/" + name);
      return false;
    }
    FileOps.delete(file.local);
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
        closeOpenFile((NFile)to);
        FileOps.delete(to.local);
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

    //close from
    if (from.isFile()) {
      closeOpenFile((NFile)from);
    }

    //rename it
    FileOps.renameFile(from.local, to.local);

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
    NFolder linkdir = getFolder(link_dir);
    String link_local = linkdir.local + "/" + link_name;
    String link_path = linkdir.path + "/" + link_name;
    if (!FileOps.createLink(link_local, ntarget.local)) return false;
    long handle = FileOps.getID(link_local);
    while (all_handles.get(handle) != null) {
      handle += NHandle.HARD_LINK;
    }
    NHandle nlink;
    if (ntarget.isFile()) {
      //add file
      nlink = new NFile(handle, link_local, link_path, link_name);
    } else {
      //add folder
      nlink = new NFolder(handle, link_local, link_path, link_name);
    }
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
    return FileOps.setMode(nhandle.local, mode);
  }

  public boolean setattr_uid(long handle, int id) {
    NHandle nhandle = getHandle(handle);
    return FileOps.setUID(nhandle.local, id);
  }

  public boolean setattr_gid(long handle, int id) {
    NHandle nhandle = getHandle(handle);
    return FileOps.setUID(nhandle.local, id);
  }

  public boolean setattr_atime(long handle, long ts) {
    NHandle nhandle = getHandle(handle);
    return FileOps.setATime(nhandle.local, ts);
  }

  public boolean setattr_mtime(long handle, long ts) {
    NHandle nhandle = getHandle(handle);
    return FileOps.setMTime(nhandle.local, ts);
  }
}

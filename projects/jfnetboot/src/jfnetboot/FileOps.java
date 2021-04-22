package jfnetboot;

/** File Ops
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

import javaforce.*;
import javaforce.jni.*;

public class FileOps {

  private static boolean debug = false;

  public static boolean exists(String path) {
    return Files.exists(new File(path).toPath(), LinkOption.NOFOLLOW_LINKS);
  }

  public static void copyFile(String src, String dst) {
    if (debug) JFLog.log("CopyFile:" + src + " to " + dst);
    try {
      File fsrc = new File(src);
      File fdst = new File(dst);
      Files.copy(fsrc.toPath(), fdst.toPath(), StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void renameFile(String src, String dst) {
    if (debug) JFLog.log("RenameFile:" + src + " to " + dst);
    try {
      File fsrc = new File(src);
      File fdst = new File(dst);
      Files.move(fsrc.toPath(), fdst.toPath(), StandardCopyOption.ATOMIC_MOVE, LinkOption.NOFOLLOW_LINKS);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void delete(String filename) {
    new File(filename).delete();
  }

  public static void create(String filename) {
    try {
      File file = new File(filename);
      if (file.exists()) {
        file.delete();
      }
      Path path = file.toPath();
      Files.createFile(path);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static void mkdir(String folder) {
    new File(folder).mkdir();
  }

  public static void rmdir(String folder) {
    new File(folder).delete();
  }

  public static int getMode(String local) {
    if (true) {
      return LnxNative.fileGetMode(local);
    } else {
      try {
        int mode = 0;

        Set<PosixFilePermission> perms = (Set<PosixFilePermission>)Files.getAttribute(new File(local).toPath(), "unix:permissions", LinkOption.NOFOLLOW_LINKS);

        //BUG : SUID SGID and STICKY bits not supported : means passwd will not work

        if (perms.contains(PosixFilePermission.OWNER_READ)) {
          mode += 0400;
        }
        if (perms.contains(PosixFilePermission.OWNER_WRITE)) {
          mode += 0200;
        }
        if (perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
          mode += 0100;
        }

        if (perms.contains(PosixFilePermission.GROUP_READ)) {
          mode += 040;
        }
        if (perms.contains(PosixFilePermission.GROUP_WRITE)) {
          mode += 020;
        }
        if (perms.contains(PosixFilePermission.GROUP_EXECUTE)) {
          mode += 010;
        }

        if (perms.contains(PosixFilePermission.OTHERS_READ)) {
          mode += 04;
        }
        if (perms.contains(PosixFilePermission.OTHERS_WRITE)) {
          mode += 02;
        }
        if (perms.contains(PosixFilePermission.OTHERS_EXECUTE)) {
          mode += 01;
        }

        return mode;
      } catch (Exception e) {
        JFLog.log(e);
        return 0777;
      }
    }
  }

  public static boolean setMode(String local, int mode) {
    if (true) {
      LnxNative.fileSetMode(local, mode);
      return true;
    } else {
      try {
        EnumSet<PosixFilePermission> set = EnumSet.noneOf(PosixFilePermission.class);

        if ((mode & 0400) != 0) {
          set.add(PosixFilePermission.OWNER_READ);
        }
        if ((mode & 0200) != 0) {
          set.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((mode & 0100) != 0) {
          set.add(PosixFilePermission.OWNER_EXECUTE);
        }

        if ((mode & 040) != 0) {
          set.add(PosixFilePermission.GROUP_READ);
        }
        if ((mode & 020) != 0) {
          set.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((mode & 010) != 0) {
          set.add(PosixFilePermission.GROUP_EXECUTE);
        }

        if ((mode & 04) != 0) {
          set.add(PosixFilePermission.OTHERS_READ);
        }
        if ((mode & 02) != 0) {
          set.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((mode & 01) != 0) {
          set.add(PosixFilePermission.OTHERS_EXECUTE);
        }

        Files.setAttribute(new File(local).toPath(), "unix:permissions", set, LinkOption.NOFOLLOW_LINKS);
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
  }

  public static int getNLink(String local) {
    try {
      return (Integer)Files.getAttribute(new File(local).toPath(), "unix:nlink", LinkOption.NOFOLLOW_LINKS);
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  public static int getUID(String local) {
    try {
      return (Integer)Files.getAttribute(new File(local).toPath(), "unix:uid", LinkOption.NOFOLLOW_LINKS);
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  public static boolean setUID(String local, int id) {
    try {
      Files.setAttribute(new File(local).toPath(), "unix:uid", id, LinkOption.NOFOLLOW_LINKS);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static int getGID(String local) {
    try {
      return (Integer)Files.getAttribute(new File(local).toPath(), "unix:gid", LinkOption.NOFOLLOW_LINKS);
    } catch (Exception e) {
      JFLog.log(e);
      return 0;
    }
  }

  public static boolean setGID(String local, int id) {
    try {
      Files.setAttribute(new File(local).toPath(), "unix:gid", id, LinkOption.NOFOLLOW_LINKS);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static UnixTime getMTime(String local) {
    try {
      FileTime ft = (FileTime)Files.getAttribute(new File(local).toPath(), "basic:lastModifiedTime", LinkOption.NOFOLLOW_LINKS);
      long ts = ft.toMillis();
      int secs = (int)(ts / 1000L);
      int nsecs = (int)((ts % 1000L) * 1000000L);
      return new UnixTime(secs, nsecs);
    } catch (Exception e) {
      JFLog.log(e);
      return new UnixTime(0,0);
    }
  }

  public static boolean setMTime(String local, long ts) {
    try {
      if (true) {
        LnxNative.fileSetModifiedTime(local, ts);
      } else {
        //fails with symlinks
        FileTime ft = FileTime.fromMillis(ts);
        Files.setAttribute(new File(local).toPath(), "basic:lastModifiedTime", ft, LinkOption.NOFOLLOW_LINKS);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static UnixTime getATime(String local) {
    try {
      FileTime ft = (FileTime)Files.getAttribute(new File(local).toPath(), "basic:lastAccessTime", LinkOption.NOFOLLOW_LINKS);
      long ts = ft.toMillis();
      int secs = (int)(ts / 1000L);
      int nsecs = (int)((ts % 1000L) * 1000000L);
      return new UnixTime(secs, nsecs);
    } catch (Exception e) {
      JFLog.log(e);
      return new UnixTime(0,0);
    }
  }

  public static boolean setATime(String local, long ts) {
    try {
      if (true) {
        LnxNative.fileSetAccessTime(local, ts);
      } else {
        //fails with symlinks
        FileTime ft = FileTime.fromMillis(ts);
        Files.setAttribute(new File(local).toPath(), "basic:lastAccessTime", ft, LinkOption.NOFOLLOW_LINKS);
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static UnixTime getCTime(String local) {
    try {
      FileTime ft = (FileTime)Files.getAttribute(new File(local).toPath(), "basic:creationTime", LinkOption.NOFOLLOW_LINKS);
      long ts = ft.toMillis();
      int secs = (int)(ts / 1000L);
      int nsecs = (int)((ts % 1000L) * 1000000L);
      return new UnixTime(secs, nsecs);
    } catch (Exception e) {
      JFLog.log(e);
      return new UnixTime(0,0);
    }
  }

  public static boolean isSymlink(File file) {
    try {
      return Files.isSymbolicLink(file.toPath());
    } catch (Exception e) {
      return false;
    }
  }

  public static String readSymlink(File file) {
    try {
      Path link = Files.readSymbolicLink(file.toPath());
      String path = link.toString();
      return path;
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean createSymbolicLink(String link, String target) {
    try {
      Files.createSymbolicLink(new File(link).toPath(), new File(target).toPath());
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public static boolean createLink(String link, String target) {
    try {
      Files.createLink(new File(link).toPath(), new File(target).toPath());
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
}

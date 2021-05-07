package javaforce.utils;

/** Processes a git repo as a package repo.
 *
 * Uses bfg.jar to delete old packages.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class GitRepo {
  private static char d1 = '-';
  private static char d2 = '-';
  public static void main(String[] args) {
    if (args.length != 1) {
      JFLog.log("Usage: GitRepo package_ext");
      System.exit(1);
    }
    String ext = args[0];
    //deb : app - version _ arch . deb
    //rpm : app - version - 1.arch .rpm
    //pac : app - version - arch .pkg.tar.gz
    if (args[0].equals("deb")) {
      d2 = '_';
    }
    //git add *.ext
    JF.exec(new String[] {"git", "add", "*." + ext});
    String[] files = new File(".").list();
    Arrays.sort(files);
    String last = null;
    for(String file : files) {
      if (!file.endsWith(args[0])) continue;
      if (last == null) {last = file; continue;}
      if (same(last, file)) {
        if (older(last, file)) {
          delete(last);
        }
        if (older(file, last)) {
          delete(file);
        }
      }
      last = file;
    }
    //files were deleted - update repo
    JF.exec(new String[] {"git", "commit" , "-m", getDate()});
    //git reflog expire --expire=now --all
    JF.exec(new String[] {"git", "reflog", "expire", "--expire=now", "--all"});
    //git gc --prune=now --aggressive
    JF.exec(new String[] {"git", "gc", "--prune=now", "--aggressive"});
    //update.sh
    JF.exec(new String[] {"bash", "update.sh"});
  }

  private static String getDate() {
    Calendar c = Calendar.getInstance();
    return String.format("\"%04d/%02d/%02d\"",
      c.get(Calendar.YEAR),
      c.get(Calendar.MONTH) + 1,
      c.get(Calendar.DAY_OF_MONTH)
    );
  }

  private static boolean same(String p1, String p2) {
    int i1 = p1.indexOf(d1);
    if (i1 == -1) return false;
    int i2 = p2.indexOf(d1);
    if (i2 == -1) return false;
    return p1.substring(0, i1).equals(p2.substring(0, i2));
  }

  private static int[] getVersion(String p) {
    int i1 = p.indexOf(d1);
    int i2 = p.indexOf(d2);
    String[] ps = p.substring(i1 + 1, i2).split("[.]");
    int[] ver = new int[ps.length];
    for(int a=0;a<ver.length;a++) {
      ver[a] = Integer.valueOf(ps[a]);
    }
    return ver;
  }

  private static boolean older(String p1, String p2) {
    int[] ver1 = getVersion(p1);
    int[] ver2 = getVersion(p2);
    int len1 = ver1.length;
    int len2 = ver2.length;
    int len = len1;
    if (len2 > len) len = len2;
    for(int i=0;i<len;i++) {
      if (len1 == i) return true;
      if (len2 == i) return false;
      if (ver1[i] < ver2[i]) return true;
    }
    return false;
  }

  private static void delete(String p) {
    new File(p).delete();
    JF.exec(new String[] {"java", "-jar", "bfg.jar", "-D", p});
  }
}

package javaforce.utils;

/** GenPAC
 *
 * Generates Arch Linux .pac files.
 *
 * @author pquiring
 */

import java.io.*;
import javaforce.JF;

public class GenPAC {
  public static void main(String args[]) {
    if (args.length < 2) {
      System.out.println("Usage:GenPAC output.pac arch");
      System.exit(1);
    }
    String files = "files.lst";
    if (!new File(files).exists()) {
      System.out.println("Error:files.lst not found");
      System.exit(1);
    }
    String out = args[0];
    String arch = args[1];
    String archext = null;
    if (archext == null) {
      System.out.println("Error:Unknown arch type");
      System.exit(1);
    }

    String files_tmp = ".files.tmp";
    String data = ".MTREE";

    Runtime rt = Runtime.getRuntime();
    try {
      GenPkgInfo.main(new String[] {"arch", arch, files});
      new File(files_tmp).delete();
      JF.echoAppend(".PKGINFO\n", files_tmp);
      JF.echoAppend(".MTREE\n", files_tmp);
      JF.copyAllAppend(files, files_tmp);
      rt.exec(new String[] {
        "bsdtar",
        "-czf",
        data,
        "--format=mtree",
        "--options='!all,use-set,type,uid,gid,mode,time,size,md5,sha256,link'",
        "-T",
        files
      }).waitFor();
      rt.exec(new String[] {"tar", "cf", out, "-T", files_tmp}).waitFor();
      new File(data).delete();
      new File(".PKGINFO").delete();
      new File(data).delete();
      new File(files_tmp).delete();
      System.out.println(out + " created!");
      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}

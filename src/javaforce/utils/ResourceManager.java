package javaforce.utils;

/**
 *  Resource Manager.
 *
 * Adds resource files to any file (such as Linux executables)
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class ResourceManager {
  public static void main(String[] args) {
    if (args == null || args.length < 2) {
      System.out.println("jfresmgr/" + JF.getVersion());
      System.out.println("Desc : Adds files to target file");
      System.out.println("Usage : jfresmgr target infile[...]");
      return;
    }
    try {
      RandomAccessFile target = new RandomAccessFile(args[0], "rw");
      target.seek(target.length());
      byte[] header = new byte[8];
      for(int a=1;a<args.length;a++) {
        FileInputStream fis = new FileInputStream(args[a]);
        byte[] data = JF.readAll(fis);
        fis.close();
        System.arraycopy(args[a].getBytes(), args[a].length() - 4, header, 0, 4);
        LE.setuint32(header, 4, data.length);
        target.write(data);
        target.write(header);  //actually a trailer
        System.out.println("Added : " + args[a]);
      }
      target.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

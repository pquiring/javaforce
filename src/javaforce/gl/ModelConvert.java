package javaforce.gl;

/**
 * Converts 3d model files to JF3D.
 *
 * @author pquiring
 */

import java.io.*;
import javaforce.JFLog;

public class ModelConvert {
  public static void usage() {
    System.out.println("Desc : ModelConvert");
    System.out.println("  Usage : infile outfile");
    System.out.println("  Usage : infolder outfolder");
    System.out.println("In formats: .3ds .blend");
    System.out.println("Out format: .jf3d");
    System.exit(0);
  }
  public static void main(String args[]) {
    if (args == null || args.length  != 2) usage();
    String in = args[0];
    String out = args[1];
    File inf = new File(in);
    File outf = new File(out);
    if (inf.isDirectory() && inf.isDirectory() ) {
      doFolder(in ,out);
    } else {
      if (inf.isDirectory() || outf.isDirectory()) {
        usage();
      }
      doFile(in, out);
    }
  }
  private static void doFile(String in, String out) {
    File inf = new File(in);
    File outf = new File(out);
    if (outf.exists()) {
      long inTime = inf.lastModified();
      long outTime = outf.lastModified();
      if (outTime >= inTime) {
        System.out.println(out + " is up-to-date");
        return;
      }
    }
    GLModel model = null;
    if (in.toLowerCase().endsWith(".3ds")) {
      GL_3DS _3ds = new GL_3DS();
      model = _3ds.load(in);
    } else
    if (in.toLowerCase().endsWith(".blend")) {
      GL_BLEND blend = new GL_BLEND();
      model = blend.load(in);
    } else usage();
    if (model == null) {
      JFLog.log("ModelConvert:Error:Load mesh failed:" + in);
      return;
    }
    GL_JF3D jf3d = new GL_JF3D();
    jf3d.save(model, out);
    System.out.println("Converted " + in + " to " + out);
  }
  private static void doFolder(String in, String out) {
    File ins[] = new File(in).listFiles();
    for(int a=0;a<ins.length;a++) {
      File f = ins[a];
      if (f.isDirectory()) continue;
      if (!f.getName().endsWith(".blend")) continue;
      String _in = f.getAbsolutePath();
      String fn = f.getName();
      String _out = out + "/" + fn.substring(0, fn.length() - 6) + ".jf3d";
      doFile(_in, _out);
    }
  }
}

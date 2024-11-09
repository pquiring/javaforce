package javaforce.gl.model;

/**
 * Converts 3d model files to JF3D.
 *
 * @author pquiring
 */

import javaforce.gl.model.ModelBLEND;
import javaforce.gl.model.ModelJF3D;
import javaforce.gl.model.Model3DS;
import java.io.*;
import javaforce.JFLog;
import javaforce.gl.Model;

public class Convert {
  public static void usage() {
    System.out.println("Desc : Convert");
    System.out.println("  Usage : infile outfile");
    System.out.println("  Usage : infolder outfolder");
    System.out.println("In formats: .3ds .blend .obj");
    System.out.println("Out format: .jf3d");
    System.exit(0);
  }
  public static void main(String[] args) {
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
    Model model = null;
    if (in.toLowerCase().endsWith(".3ds")) {
      Model3DS _3ds = new Model3DS();
      model = _3ds.load(in);
    } else if (in.toLowerCase().endsWith(".blend")) {
      ModelBLEND blend = new ModelBLEND();
      model = blend.load(in);
    } else if (in.toLowerCase().endsWith(".obj")) {
      ModelOBJ obj = new ModelOBJ();
      model = obj.load(in);
    } else {
      usage();
    }
    if (model == null) {
      JFLog.log("ModelConvert:Error:Load mesh failed:" + in);
      return;
    }
    ModelJF3D jf3d = new ModelJF3D();
    jf3d.save(model, out);
    System.out.println("Converted " + in + " to " + out);
  }
  private static void doFolder(String in, String out) {
    File[] ins = new File(in).listFiles();
    for(int a=0;a<ins.length;a++) {
      File f = ins[a];
      if (f.isDirectory()) continue;
      String fn = f.getName();
      if (!fn.endsWith(".3ds") && !fn.endsWith(".blend") && !fn.endsWith(".obj")) continue;
      int extlen = 4;
      if (fn.endsWith(".blend")) extlen = 6;
      String _in = f.getAbsolutePath();
      String _out = out + "/" + fn.substring(0, fn.length() - extlen) + ".jf3d";
      doFile(_in, _out);
    }
  }
}

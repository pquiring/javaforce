package javaforce;

import java.io.*;
import java.util.*;

/** MetaData.
 *
 * Provides extended meta data:
 *  - field order
 *  - method order
 *
 * @author pquiring
 */
public class Meta {
  //generate meta data
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: Meta source_folder_in class_folder_out");
      return;
    }
    String src = args[0];
    String dst = args[1];
    int cnt = 0;
    try {
      File[] files = new File(src).listFiles();
      for(File srcfile : files) {
        if (!srcfile.getName().endsWith(".java")) continue;
        String srcfilename = src + "/" + srcfile.getName();
        String dstfilename = dst + "/" + srcfile.getName().replace(".java", ".md");
        File dstFile = new File(dstfilename);
        long srcts = srcfile.lastModified();
        long dstts = dstFile.exists() ? dstFile.lastModified() : 0;
        if (dstts > srcts) continue;
        FileInputStream fis = new FileInputStream(srcfile);
        String[] lns = new String(fis.readAllBytes()).split("\n");
        fis.close();
        boolean struct = false;
        StringBuilder methods = new StringBuilder();
        StringBuilder fields = new StringBuilder();
        for(String ln : lns) {
          //find "public class ... extends FFMStruct ..."
          if (!struct) {
            if (ln.indexOf(" class ") != -1 && ln.indexOf(" FFMStruct") != -1) {
              struct = true;
            }
            continue;
          }
          //find "public T field [=value];
          String[] fs = ln.trim().replace(";", "").split("[ ]+");
          if (fs.length < 3) continue;
          if (!fs[0].equals("public")) continue;
          String type = fs[1];
          String name = fs[2];
          if (name.contains("(")) {
            //method
            int idx = name.indexOf('(');
            name = name.substring(0, idx);
            methods.append(name);
            methods.append("\n");
          } else {
            //field
            fields.append(name);
            fields.append("\n");
          }
        }
        if (struct) {
          //output meta data
          FileOutputStream fos = new FileOutputStream(dstfilename);
          if (fields.length() > 0) {
            fos.write("[fields]\n".getBytes());
            fos.write(fields.toString().getBytes());
          }
          if (methods.length() > 0) {
            fos.write("[methods]\n".getBytes());
            fos.write(methods.toString().getBytes());
          }
          fos.close();
          cnt++;
        }
      }
      if (cnt > 0) System.out.println("Generated meta data for " + cnt + " classes.");
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  public static Meta getMetaData(String className) {
    String md = "/" + className.replace(".", "/") + ".md";
    InputStream is = Meta.class.getResourceAsStream(md);
    if (is == null) {
      JFLog.log("Error:meta data not found:" + md);
      return null;
    }
    Meta meta = new Meta();
    ArrayList<String> fields = new ArrayList<>();
    ArrayList<String> methods = new ArrayList<>();
    ArrayList<String> section = null;
    try {
      String[] lns = new String(is.readAllBytes()).split("\n");
      for(String ln : lns) {
        if (ln.equals("[fields]")) {
          section = fields;
          continue;
        }
        if (ln.equals("[methods]")) {
          section = methods;
          continue;
        }
        if (section == null) continue;
        section.add(ln);
      }
      meta.fields = fields.toArray(JF.StringArrayType);
      meta.methods = methods.toArray(JF.StringArrayType);
    } catch (Exception e) {
      JFLog.log(e);
    }
    return meta;
  }

  public String[] fields;
  public String[] methods;
}

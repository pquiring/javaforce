package javaforce;

import java.io.*;
import java.util.*;

/** MetaData.
 *
 * Provides extended meta data:
 *  - field order
 *  - method order
 *
 * @see javaforce.utils.GenMeta
 *
 * @author pquiring
 */

public class Meta {
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

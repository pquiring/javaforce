package jfcontrols.functions;

/** Compiler
 *
 * @author pquiring
 */

import javaforce.*;

public class Compiler {
  public String generateFunction(int fid, SQL sql) {
    StringBuilder sb = new StringBuilder();
    sb.append("public class test {\r\n");
    sb.append("  public boolean func(boolean enabled, Tag args[]) {\r\n");
    sb.append("    boolean enabled;\r\n");
    sb.append("    boolean estack[] = new boolean[256];\r\n");
    sb.append("    int eidx = 0;\r\n");
    sb.append("    Tag tags[] = new Tag[33];\r\n");
    sb.append("    Tag temp[] = new Tag[33];\r\n");
    sb.append("    \r\n");

    //append code from rungs
    String rungs[][] = sql.select("select * from rungs where fid=" + fid);
    int norungs = rungs.length;
    for(int rid=0;rid<norungs;rid++) {
      String rung[] = rungs[rid];
      String blocks[][] = sql.select("select * from blocks where rid=" + rid);
      //...
    }

    sb.append("    return enabled;\r\n");
    sb.append("  }\r\n");
    sb.append("}\r\n");
    return sb.toString();
  }
}

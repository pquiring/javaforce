package jfcontrols.functions;

/** Compiler
 *
 * @author pquiring
 */

public class Compiler {
  public String generateFunction(int fid) {
    StringBuilder sb = new StringBuilder();
    sb.append("public class test {\r\n");
    sb.append("  public boolean func(Tag args[]) {\r\n");
    sb.append("    boolean en[] = new boolean[256];\r\n");
    sb.append("    int enidx = 0;\r\n");
    sb.append("    en[enidx] = true;\r\n");
    sb.append("    Tag tags[] = new Tag[32];\r\n");
    sb.append("    \r\n");

    //append code from rungs

    sb.append("  }\r\n");
    sb.append("}\r\n");
    return sb.toString();
  }
}

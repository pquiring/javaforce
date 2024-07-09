package javaforce.utils;

/** xml
 *
 * CLI to view xml tags.
 *
 * @author peter.quiring
 */

import java.util.*;

import javaforce.*;

public class xml {
  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
    }
    switch (args[0]) {
      case "read":
        if (args.length < 3) {
          usage();
        }
        XML xml = new XML();
        if (!xml.read(args[1])) {
          error("read file failed");
        }
        XML.XMLTag tag = xml.getTag(makePath(args[2]));
        if (tag == null) {
          error("tag not found");
        }
        if (args.length < 4) {
          System.out.print(tag.content);
        } else {
          String param = tag.getArg(args[3]);
          if (param == null) error("param not found");
          System.out.print(param);
        }
        break;
      default:
        System.out.println("Error:Unknown command");
        System.exit(1);
        break;
    }
  }
  private static void usage() {
    System.out.println("Usage:xml {cmd} [args]");
    System.out.println(" cmd:read {xml-file} {tag-path,...} [{param}]");
    System.exit(1);
  }
  private static void error(String msg) {
    System.out.println("Error:" + msg);
    System.exit(1);
  }
  private static String[] makePath(String[] args, int start, int end) {
    ArrayList<String> list = new ArrayList<>();
    if (end == -1) end = args.length;
    for(int i=start;i<end;i++) {
      list.add(args[i]);
    }
    return list.toArray(JF.StringArrayType);
  }
  private static String[] makePath(String arg) {
    return makePath(arg.split(","), 0, -1);
  }
}

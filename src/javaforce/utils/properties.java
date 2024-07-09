package javaforce.utils;

/** properties
 *
 * CLI to view properties (works with yaml files too)
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class properties {
  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
    }
    try {
      switch (args[0]) {
        case "read":
          if (args.length < 3) {
            usage();
          }
          Properties props = new Properties();
          props.load(new FileInputStream(args[1]));
          String prop = (String)props.get(args[2]);
          if (prop == null) {
            error("property not found");
          }
          System.out.print(prop);
          break;
        default:
          System.out.println("Error:Unknown command");
          System.exit(1);
          break;
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  private static void usage() {
    System.out.println("Usage:properties {cmd} [args]");
    System.out.println(" cmd:read {file} {property}");
    System.exit(1);
  }
  private static void error(String msg) {
    System.out.println("Error:" + msg);
    System.exit(1);
  }
}

package javaforce.tests;

/** Test : Print System Properties
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class TestSysProps {
  public static void list(Properties p) {
    Enumeration keys = p.keys();
    while (keys.hasMoreElements()) {
        String key = (String)keys.nextElement();
        String value = (String)p.get(key);
        System.out.println(key + ": " + value);
    }
  }
  public static void list(String file) {
    if (!new File(file).exists()) return;
    try {
      Properties p = new Properties();
      p.load(new FileInputStream(file));
      list(p);
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }
  public static void main(String args[]) {
    list(System.getProperties());
    list("/etc/os-release");
  }
}

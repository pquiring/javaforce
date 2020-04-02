package javaforce.utils;

/** Generates GraalVM JNI config file.
 *
 * @author pquiring
 */

/*

//TODO

  {
    "name" : "javaforce.media.MediaIO",
    "allDeclaredMethods" : true
  },
  {
    "name" : "javaforce.media.MediaCoder",
    "fields" : [
      { "name" : "ctx" }
    ]
  }

*/

import java.io.*;

public class GenGraal {
  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("usage:GenGraal mainclass");
      System.exit(1);
    }
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    sb.append("{");
    sb.append("\"name\":\"javaforce.JF\",");
    sb.append("\"methods\":");
    sb.append("[");
    sb.append("{\"name\":\"expandArgs\"}");
    sb.append("]");
    sb.append("},{");
    sb.append("\"name\":\"java.lang.System\",");
    sb.append("\"methods\":");
    sb.append("[");
    sb.append("{\"name\":\"setProperty\"}");
    sb.append("]");
    sb.append("},{");
    sb.append("\"name\":\"" + args[0] + "\",");
    sb.append("\"methods\":");
    sb.append("[");
    sb.append("{\"name\":\"main\"}");
    sb.append("]");
    sb.append("}");
    sb.append("]");
    try {
      FileOutputStream fos = new FileOutputStream("graal.json");
      fos.write(sb.toString().getBytes());
      fos.close();
      System.out.println("graal.json created");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

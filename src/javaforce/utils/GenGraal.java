package javaforce.utils;

/** Generates GraalVM JNI config file.
 *
 * @author pquiring
 */

import java.io.*;

public class GenGraal {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("usage:GenGraal mainclass");
      System.exit(1);
    }
    jni(args);
    resources(args);
  }
  public static void jni(String[] args) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    sb.append("{");
    sb.append("  \"name\":\"javaforce.JF\",");
    sb.append("  \"methods\":");
    sb.append("  [");
    sb.append("    {\"name\":\"expandArgs\"}");
    sb.append("  ]");
    sb.append("},{");
    sb.append("  \"name\":\"java.lang.System\",");
    sb.append("  \"methods\":");
    sb.append("  [");
    sb.append("    {\"name\":\"setProperty\"}");
    sb.append("  ]");
    sb.append("},{");
    sb.append("  \"name\":\"" + args[0] + "\",");
    sb.append("  \"allDeclaredMethods\" : true");
    sb.append("},{");
    sb.append("  \"name\":\"javaforce.gl.GLWindow\",");
    sb.append("  \"allDeclaredMethods\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.gl.GL\",");
    sb.append("  \"allDeclaredConstructors\" : true,");
    sb.append("  \"allDeclaredMethods\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.media.MediaIO\",");
    sb.append("  \"allDeclaredMethods\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.media.MediaCoder\",");
    sb.append("  \"allDeclaredConstructors\" : true,");
    sb.append("  \"fields\" : [");
    sb.append("    { \"name\" : \"ctx\" }");
    sb.append("  ]");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.media.Camera\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.media.MediaDecoder\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.media.MediaVideoDecoder\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.media.MediaEncoder\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.media.VideoBuffer\",");
    sb.append("  \"allDeclaredMethods\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.jni.JFNative\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.jni.WinNative\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.jni.LnxNative\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("},{");
    sb.append("  \"name\" : \"javaforce.controls.ni.DAQmx\",");
    sb.append("  \"allDeclaredConstructors\" : true");
    sb.append("}");
    sb.append("]");
    try {
      FileOutputStream fos = new FileOutputStream("graal_jni.json");
      fos.write(sb.toString().getBytes());
      fos.close();
      System.out.println("graal_jni.json created");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static void resources(String[] args) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("  \"resources\": {");
    sb.append("    \"includes\": [");
    sb.append("      {\"pattern\": \"javaforce/webui/static/.*\"},");
    sb.append("      {\"pattern\": \"javaforce/icons/.*\"}");
    sb.append("    ]");
    sb.append("  }");
    sb.append("}");
    try {
      FileOutputStream fos = new FileOutputStream("graal_res.json");
      fos.write(sb.toString().getBytes());
      fos.close();
      System.out.println("graal_res.json created");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

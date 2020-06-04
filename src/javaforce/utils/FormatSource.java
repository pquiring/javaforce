package javaforce.utils;

/** Format Source code to assist in Java to C# conversion.
 *
 * @author pquiring
 */

import java.io.*;

public class FormatSource {
  public static void main(String[] args) {
    new FormatSource().run(args);
  }
  public void run(String[] args) {
    if (args.length == 0) {
      System.out.println("usage:FormatSource folder");
      System.exit(0);
    }
    doFolder(args[0]);
  }
  public boolean isID(byte ch) {
    if ((ch >= 'a') && (ch <= 'z')) return true;
    if ((ch >= 'A') && (ch <= 'Z')) return true;
    if ((ch >= '0') && (ch <= '9')) return true;
    if (ch == '_') return true;
    return false;
  }
  public boolean isType(byte[] data, int offset, int length) {
    char first = (char)data[offset];
    if ((first >= 'A') && (first <= 'Z')) return true;  //assume capitalized name is a type
    String type = new String(data, offset, length);
    switch (type) {
      case "byte": return true;
      case "short": return true;
      case "int": return true;
      case "long": return true;
      case "float": return true;
      case "double": return true;
      case "boolean": return true;
      case "char": return true;
    }
    return false;
  }
  public int doSource(byte[] data) {
    //type name[] -> type[] name
    int count = 0;
    int offset = 0;
    while (offset < data.length) {
      int length = 0;
      while (data[offset + length] == '[' && data[offset + length + 1] == ']') {
        length += 2;
      }
      if (length > 0) {
        int nameStart, nameLength;
        //scan back to name id
        nameStart = offset - 1;
        nameLength = 0;
        while (data[nameStart] == ' ') {
          nameStart--;
        }
        while (isID(data[nameStart])) {
          nameStart--;
          nameLength++;
        }
        nameStart++;
        if (nameLength > 0 && !isType(data, nameStart, nameLength)) {
          //scan back to type id
          int typeStart, typeLength;
          typeStart = nameStart - 1;
          typeLength = 0;
          while (data[typeStart] == ' ') {
            typeStart--;
          }
          while (isID(data[typeStart])) {
            typeStart--;
            typeLength++;
          }
          typeStart++;
          if (typeLength > 0) {
            //type name[] -> type[] name
            int src = typeStart + typeLength;
            int dst = src + length;
            System.arraycopy(data, src, data, dst, offset - src);
            int pairs = length / 2;
            for(int a=0;a<pairs;a++) {
              data[src++] = '[';
              data[src++] = ']';
            }
            count++;
          }
        }
        offset += length;
      } else {
        offset++;
      }
    }
    return count;
  }
  public void doFile(String file) {
    try {
      FileInputStream fis = new FileInputStream(file);
      byte[] data = fis.readAllBytes();
      fis.close();
      int count = doSource(data);
      if (count == 0) return;
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(data);
      fos.close();
      System.out.println("Fixed " + count + " arrays in " + file);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  public void doFolder(String folder) {
    File[] files = new File(folder).listFiles();
    for(File file : files) {
      if (file.isDirectory()) {
        doFolder(file.getAbsolutePath());
      } else {
        String name = file.getAbsolutePath();
        if (name.endsWith(".java")) {
          doFile(name);
        }
      }
    }
  }
}

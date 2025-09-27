package javaforce.utils;

/** Generate icons
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenIcons {
  public static void main(String[] args) {
    try {
      File[] files = new File("src/javaforce/icons/svg").listFiles();
      for(File svg : files) {
        gen(svg);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  private static void gen(File svg) {
    String name = svg.getName();
    int idx = name.indexOf('.');
    String base = name.substring(0, idx);
    String i16 = "src/javaforce/icons/16/" + base + ".png";
    File f16 = new File(i16);
    String i32 = "src/javaforce/icons/32/" + base + ".png";
    File f32 = new File(i32);
    long svg_date = svg.lastModified();
    long i16_date = f16.lastModified();
    long i32_date = f32.lastModified();
    if (svg_date < i16_date || svg_date < i32_date) return;
    String temp = "GenIcons-64.png";
    ImageConvert.main(new String[] {svg.getPath(), temp, "size=64,64", "fill=00ffffff"});
    ImageConvert.main(new String[] {temp, i32, "scale=50,50"});
    ImageConvert.main(new String[] {temp, i16, "scale=25,25"});
    new File(temp).delete();
  }
}

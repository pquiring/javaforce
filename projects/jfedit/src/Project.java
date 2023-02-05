/** Project Settings
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Project {
  public static String filename = ".jfedit.project";
  public static Project project = new Project();

  private enum Section {Unknown, Files};

  public ArrayList<String> fileMasks = new ArrayList<String>();

  public Project() {
    fileMasks.add("*.txt");
  }

  public void load() {
    Section section = Section.Unknown;
    fileMasks.clear();
    try {
      FileInputStream fis = new FileInputStream(filename);
      String[] lns = new String(fis.readAllBytes()).replaceAll("\r", "").split("\n");
      fis.close();
      for(String ln : lns) {
        ln = ln.trim();
        if (ln.length() == 0) continue;
        switch (ln) {
          case "[files]":
            section = Section.Files;
            continue;
        }
        if (ln.startsWith("[")) continue;  //unknown section
        switch (section) {
          case Files:
            fileMasks.add(ln);
            break;
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void save() {
    StringBuilder cfg = new StringBuilder();
    String eol = JF.isWindows() ? "\r\n" : "\n";
    cfg.append("[files]" + eol);
    for(String mask : fileMasks) {
      cfg.append(mask + eol);
    }
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(cfg.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

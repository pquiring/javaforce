package javaforce.ansi.server;

/** FileDialog
 *
 * @author pquiring
 */

/*
 *  Layout:
 * ************ Open/Save **************
 * * Filename: [.....................] *
 * * { current_path                  } *
 * * ******** Files/Folders ********** *
 * * * [folder]                      * *
 * * * file.txt                      * *
 * * *  ...                          * *
 * * ********************************* *
 * * < Ok >   < Cancel >               *
 * *************************************
 */

import java.io.*;
import java.util.*;
import java.awt.event.KeyEvent;

import javaforce.*;

public class FileDialog implements Dialog {
  private ANSI ansi;
  private boolean closed = false;
  private boolean cancel = false;
  private boolean load;
  private String filename;
  private static String path;
  private String[] files;
  private Field[] fields;
  private TextField file;
  private int pathx, pathy;
  private List list;
  private int field;

  public FileDialog(ANSI ansi, boolean load, String path, String filename) {
    this.ansi = ansi;
    this.load = load;
    if (path == null) {
      if (this.path == null) {
        this.path = JF.getCurrentPath();
      }
    } else {
      this.path = path;
    }
    this.path = this.path.replaceAll("\\\\", "/");
    if (filename == null) {
      filename = "";
    }
    this.filename = filename;
    listFiles();
  }

/*
 *  Layout:
 * ************ Open/Save **************
 * * Filename: [.....................] *
 * * { current_path                  } *
 * * ******** Files/Folders ********** *
 * * * [folder]                      * *
 * * * file.txt                      * *
 * * *  ...                          * *
 * * ********************************* *
 * * < Ok >   < Cancel >               *
 * *************************************
 */

  public void draw() {
    int width = 42;
    int height = ansi.height - 3;
    int x = (ansi.width - width) / 2;
    int y = 3;
    //draw box
    String title = load ? "Open" : "Save";
    String lns[] = new String[height-2];
    lns[0] = "Filename: [" + ansi.pad(filename, 28) + "]";
    lns[1] = "";  //path
    lns[2] = ansi.pad("{list}", 40);  //place holder for list
    lns[height-3] = "<Ok> <Cancel>";
    pathx = x + 1;
    pathy = y + 2;
    fields = ansi.drawWindow(x, y, width, height, lns);
    ansi.gotoPos(x + (width - title.length()) / 2, y);
    System.out.print(title);
    title = "Files/Folders";
    ansi.gotoPos(x + (width - title.length()) / 2, y+3);
    System.out.print(title);
    drawPath();
    file = (TextField)fields[0];
    list = (List)fields[1];
    list.width = 40;
    list.height = height - 5;
    list.items = files;
    list.draw();
    fields[0].gotoCurrentPos();
  }

  public void drawPath() {
    ansi.gotoPos(pathx, pathy);
    ansi.setDialogColor();
    System.out.print(ansi.pad(path, 40));
  }

  public void drawFiles() {
    list.draw();
  }

  public void keyPressed(int keyCode, int keyMods) {
    switch (keyMods) {
      case 0:
        switch (keyCode) {
          case KeyEvent.VK_ESCAPE:
            closed = true;
            cancel = true;
            break;
          default:
            fields[field].keyPressed(keyCode, keyMods);
            break;
        }
        break;
    }
  }

  public void keyTyped(char key) {
    switch (key) {
      case 9:
        //tab
        field++;
        if (field == fields.length) field = 0;
        fields[field].gotoCurrentPos();
        break;
      case 10:
        //enter
        if (field == 1) {
          //list
          String item = list.getItem();
          if (item.startsWith("[") && item.endsWith("]")) {
            String folder = item.substring(1, item.length() - 1);
            if (folder.equals("..")) {
              int lastIdx = path.lastIndexOf('/');
              if (lastIdx == -1) {
                path = "/";
              } else {
                path = path.substring(0, lastIdx);
              }
            } else {
              if (!path.endsWith("/")) path += "/";
              if (JF.isWindows() && path.equals("/")) {
                path = "";
              }
              path += folder;
            }
            listFiles();
            list.items = files;
            list.cy = 0;
            list.wy = 0;
            drawPath();
            drawFiles();
          } else {
            file.setText(item);
            closed = true;
            cancel = false;
          }
        } else {
          closed = true;
          cancel = field == 3;
        }
        break;
      default:
        fields[field].keyTyped(key);
        break;
    }
  }

  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  private void listFiles() {
    File tmp[];
    if (JF.isWindows() && path.equals("/")) {
      ArrayList<String> drives = new ArrayList<String>();
      for(char drv='A';drv<='Z';drv++) {
        if (new File("" + drv + ":/").exists()) {
          drives.add("[" + drv + ":]");
        }
      }
      files = drives.toArray(new String[0]);
    } else {
      tmp = new File(path).listFiles();
      if (tmp == null) tmp = new File[0];
      int start;
      if (path.equals("/")) {
        files = new String[tmp.length];
        start = 0;
      } else {
        files = new String[tmp.length + 1];
        files[0] = "[..]";
        start = 1;
      }
      for(int a=0;a<tmp.length;a++) {
        if (tmp[a].isDirectory()) {
          files[a + start] = "[" + tmp[a].getName() + "]";
        } else {
          files[a + start] = tmp[a].getName();
        }
      }
      Arrays.sort(files, new Comparator<String>() {
        public int compare(String o1, String o2) {
          boolean f1 = o1.startsWith("[");
          boolean f2 = o2.startsWith("[");
          if (f1 != f2) {
            return f2 ? 1 : -1;
          }
          return o1.compareTo(o2);
        }
      });
    }
  }

  public String getPath() {
    return path;
  }

  public String getFilename() {
    if (cancel) return null;
    return file.getText();
  }

  public boolean isLoading() {
    return load;
  }
}

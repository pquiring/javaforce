/** TEdit
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.awt.event.KeyEvent;

import javaforce.*;
import javaforce.ansi.*;
import javaforce.ansi.games.*;
import javaforce.jni.*;

public class TEdit implements KeyEvents {
  public static String args[];
  public ArrayList<Tab> tabs = new ArrayList<Tab>();
  public int tabidx;
  public ANSI ansi;
  public boolean active;
  public boolean ask_save;
  public MessageDialog message;
  public InputDialog input;
  public FileDialog file;
  public Menu menu;
  public KeyEvents game;

  public int menuIdx;

  public String menus[][] = {
    {"New", "Open", "Close", "Save", "Save As", "-", "Properties", "-", "Exit"},
    {"Cut", "Copy", "Paste", "-", "Find", "Replace", "-", "Options"},
    {"Prev", "Next", "-"},
    {"Keys", "About"},
  };

  public int menusPos[] = {2, 7, 12, 19};

  public enum InputType {none, gotoline, find, replace, options, properties, games};
  public InputType inputType = InputType.none;

  public String find, replace;
  public boolean found;

  public void keyPressed(int keyCode, int keyMods) {
//    System.err.println("keyCode=" + keyCode + ",keyMods=" + keyMods);
    if (keyMods == 0 && keyCode == KeyEvent.VK_F5) {
      refresh();
    }
    if (game != null) {
      game.keyPressed(keyCode, keyMods);
      return;
    }
    if (input != null) {
      input.keyPressed(keyCode, keyMods);
      if (input.isClosed()) {
        if (!input.isCancelled()) {
          doInput();
        } else {
          input = null;
        }
        refresh();
      }
      return;
    }
    if (message != null) {
      message.keyPressed(keyCode, keyMods);
      if (message.isClosed()) {
        if (ask_save) {
          ask_save = false;
          String action = message.getAction();
          switch (action) {
            case "Save":
              save();
              break;
            case "Discard":
              close(true);
              break;
          }
        }
        message = null;
        refresh();
      }
      return;
    }
    if (file != null) {
      file.keyPressed(keyCode, keyMods);
      if (file.isClosed()) {
        doFile();
        refresh();
      }
      return;
    }
    if (menu != null) {
      menu.keyPressed(keyCode, keyMods);
      if (menu.isClosed()) {
        doMenu();
        refresh();
      }
      return;
    }
    switch (keyMods) {
      case 0:
        switch (keyCode) {
          case KeyEvent.VK_UP:
            moveUp(false);
            break;
          case KeyEvent.VK_DOWN:
            moveDown(false);
            break;
          case KeyEvent.VK_LEFT:
            moveLeft(false);
            break;
          case KeyEvent.VK_RIGHT:
            moveRight(false);
            break;
          case KeyEvent.VK_PAGE_UP:
            movePageUp(false);
            break;
          case KeyEvent.VK_PAGE_DOWN:
            movePageDown(false);
            break;
          case KeyEvent.VK_DELETE:
            delete(true);
            break;
          case KeyEvent.VK_ESCAPE:
            break;
          case KeyEvent.VK_HOME:
            moveHome(false);
            break;
          case KeyEvent.VK_END:
            moveEnd(false, true);
            break;
          case KeyEvent.VK_F1:
            createHelp();
            refresh();
            break;
          case KeyEvent.VK_F3:
            findAgain();
            break;
        }
        break;
      case KeyEvent.CTRL_MASK:
        switch (keyCode) {
          case KeyEvent.VK_UP:
            //scrollUp(false);
            break;
          case KeyEvent.VK_DOWN:
            //scrollDown(false);
            break;
          case KeyEvent.VK_LEFT:
            //moveNextLeft(false);
            break;
          case KeyEvent.VK_RIGHT:
            //moveNextRight(false);
            break;
          case KeyEvent.VK_HOME:
            moveTop(false, true);
            break;
          case KeyEvent.VK_END:
            moveBottom(false);
            break;
        }
        break;
      case KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK:
        switch (keyCode) {
          case KeyEvent.VK_UP:
            //scrollUp(true);
            break;
          case KeyEvent.VK_DOWN:
            //scrollDown(true);
            break;
          case KeyEvent.VK_LEFT:
            //moveNextLeft(true);
            break;
          case KeyEvent.VK_RIGHT:
            //moveNextRight(true);
            break;
          case KeyEvent.VK_HOME:
            moveTop(true, true);
            break;
          case KeyEvent.VK_END:
            moveBottom(true);
            break;
        }
        break;
      case KeyEvent.ALT_MASK:
        switch (keyCode) {
          case 'f':
            //ALT+F = File Menu
            menuIdx = 0;
            createMenu();
            refresh();
            break;
          case 'e':
            //ALT+E = Edit Menu
            menuIdx = 1;
            createMenu();
            refresh();
            break;
          case 'w':
            //ALT+W = Window Menu
            menuIdx = 2;
            createMenu();
            refresh();
            break;
          case 'h':
            //ALT+H = Help Menu
            menuIdx = 3;
            createMenu();
            refresh();
            break;
          case 'z':
            //ALT+Z = debug popup
            createDebug();
            refresh();
            break;
          case 'g':
            createGames();
            refresh();
            break;
        }
        break;
      case KeyEvent.SHIFT_MASK:
        switch (keyCode) {
          case KeyEvent.VK_UP:
            moveUp(true);
            break;
          case KeyEvent.VK_DOWN:
            moveDown(true);
            break;
          case KeyEvent.VK_LEFT:
            moveLeft(true);
            break;
          case KeyEvent.VK_RIGHT:
            moveRight(true);
            break;
          case KeyEvent.VK_PAGE_UP:
            movePageUp(true);
            break;
          case KeyEvent.VK_PAGE_DOWN:
            movePageDown(true);
            break;
          case KeyEvent.VK_HOME:
            moveHome(true);
            break;
          case KeyEvent.VK_END:
            moveEnd(true, true);
            break;
        }
        break;
    }
  }

  public void keyTyped(char key) {
//    System.err.println("keyTyped:" + (int)key);
    if (game != null) {
      game.keyTyped(key);
      return;
    }
    if (input != null) {
      input.keyTyped(key);
      if (input.isClosed()) {
        if (!input.isCancelled()) {
          doInput();
        } else {
          input = null;
        }
        refresh();
      }
      return;
    }
    if (message != null) {
      message.keyTyped(key);
      if (message.isClosed()) {
        message = null;
        refresh();
      }
      return;
    }
    if (file != null) {
      file.keyTyped(key);
      if (file.isClosed()) {
        doFile();
        refresh();
      }
      return;
    }
    if (menu != null) {
      menu.keyTyped(key);
      if (menu.isClosed()) {
        doMenu();
        refresh();
      }
      return;
    }
    Tab tab = tabs.get(tabidx);
    switch (key) {
      case 3:
        //CTRL+C
        copy();
        break;
      case 6:
        //CTRL+F
        find();
        break;
      case 8:
      case 127:
        backspace();
        break;
      case 10:
        enter();
        break;
      case 12:
        gotoLine();
        break;
      case 15:
        //CTRL+O
        open();
        break;
      case 18:
        replace();
        break;
      case 19:
        //CTRL+S
        save();
        break;
      case 22:
        //CTRL+V
        paste();
        break;
      case 23:
        //CTRL+W
        saveAs();
        break;
      case 24:
        //CTRL+X
        cut();
        break;
      default:
        if (key < 32 && key != 9) return;
        tab.insert(key);
        moveRight(false);
        break;
    }
  }

  public static class Tab {
    public Tab() {
      display = "Untitled";
      lines.add(new StringBuilder(""));
    }
    public Tab(String name) {
      filename = name;
      display = name;
      lines.add(new StringBuilder(""));
    }
    public String filename;
    public String display;
    public ArrayList<StringBuilder> lines = new ArrayList<StringBuilder>();
    public boolean crlf;  //else lf
    public int dx, dy;  //display (window) position
    public int cx, cy;  //cursor position (relative to lines)
    public boolean dirty;
    public int sel_begin_line = -1, sel_begin_char = -1;
    public int sel_end_line = -1, sel_end_char = -1;
    public boolean sel_at_begin;
    public boolean sel_at_end;
    /** Returns cx adjusted with tab size. */
    public int adjustX(int x, int line) {
      StringBuilder sb = lines.get(line);
      int tab_1 = Settings.settings.tabSize - 1;
      if (tab_1 == 0) return x;
      int ox = x;
      if (ox > sb.length()) {
        ox = sb.length();
      }
      for(int a=0;a<ox;a++) {
        if (sb.charAt(a) == '\t') {
          x += tab_1;
        }
      }
      return x;
    }
    public boolean hasSelection() {
      return sel_begin_line != -1;
    }
    public void clearSelection() {
      sel_begin_line = -1;
      sel_begin_char = -1;
      sel_end_line = -1;
      sel_end_char = -1;
    }
    public void startSelection() {
      sel_begin_line = cy;
      sel_begin_char = cx;
      sel_end_line = cy;
      sel_end_char = cx;
      sel_at_begin = true;
      sel_at_end = true;
    }
    public void adjustSelectionCheck() {
      sel_at_begin = sel_begin_line == cy && sel_begin_char == cx;
      sel_at_end = sel_end_line == cy && sel_end_char == cx;
    }
    public void adjustSelection(boolean right_down) {
      if (sel_at_begin && sel_at_end) {
        if (right_down) {
          adjustEndSelection();
        } else {
          adjustBeginSelection();
        }
      } else {
        if (sel_at_begin) {
          adjustBeginSelection();
        } else {
          adjustEndSelection();
        }
      }
    }
    public void adjustBeginSelection() {
      sel_begin_line = cy;
      sel_begin_char = cx;
      checkSelection();
    }
    public void adjustEndSelection() {
      sel_end_line = cy;
      sel_end_char = cx;
      checkSelection();
    }
    private void checkSelection() {
      if (sel_begin_line == sel_end_line && sel_begin_char == sel_end_char) {clearSelection(); return;}
      if (sel_begin_line > sel_end_line || (sel_begin_line == sel_end_line && sel_begin_char > sel_end_char)) swapSelection();
    }
    private void swapSelection() {
      int _line = sel_begin_line;
      int _char = sel_begin_char;
      sel_begin_line = sel_end_line;
      sel_begin_char = sel_end_char;
      sel_end_line = _line;
      sel_end_char = _char;
    }
    public boolean pastSelection() {
      if (cy > sel_end_line) return true;
      if (cy == sel_end_line && cx > sel_end_char) return true;
      return false;
    }
    public void deleteSelection(int height) {
      if (!hasSelection()) return;
      int full_lines = 0;
      for(int ln=sel_begin_line + 1;ln<sel_end_line;ln++) {
        lines.remove(sel_begin_line + 1);
        full_lines++;
      }
      sel_end_line -= full_lines;
      if (sel_begin_line == sel_end_line) {
        //delete partial line
        lines.get(sel_begin_line).delete(sel_begin_char, sel_end_char);
      } else {
        //combine begin and end lines
        String end = lines.get(sel_end_line).substring(sel_end_char);
        lines.remove(sel_end_line);
        StringBuilder begin = lines.get(sel_begin_line);
        begin.delete(sel_begin_char, begin.length());
        begin.append(end);
      }
      cy = sel_begin_line;
      cx = sel_begin_char;
      int len = lines.get(cy).length();
      if (cx >= len) {
        cx = len;
        dx = 0;
      }
      //move dy if needed to show cy
      height -= 3;
      int y1 = cy;
      int y2 = y1 + height;
      int pos = cy + dy;
      if (pos < y1) {
        //move up
        dy = cy;
      }
      else if (pos > y2) {
        //move down
        dy = cy - height;
      }
      clearSelection();
    }
    public String getSelection() {
      if (!hasSelection()) return null;
      StringBuilder sb = new StringBuilder();
      for(int ln=sel_begin_line;ln <= sel_end_line;ln++) {
        if (ln == sel_begin_line) {
          if (sel_begin_line != sel_end_line) {
            sb.append(lines.get(ln).substring(sel_begin_char));
            sb.append("\n");
          } else {
            sb.append(lines.get(ln).substring(sel_begin_char, sel_end_char));
          }
        } else if (ln == sel_end_line) {
          sb.append(lines.get(ln).substring(0, sel_end_char));
        } else {
          sb.append(lines.get(ln));
          sb.append("\n");
        }
      }
      return sb.toString();
    }
    public void setSelection(int begin_line, int begin_char, int end_line, int end_char) {
      sel_begin_line = begin_line;
      sel_begin_char = begin_char;
      sel_end_line = end_line;
      sel_end_char = end_char;
    }
    public boolean saveAs(String path, String filename) {
      String _display = this.display;
      String _filename = this.filename;
      this.filename = path + "/" + filename;
      this.display = filename;
      try {
        return save();
      } catch (Exception e) {
        this.display = _display;
        this.filename = _filename;
        return false;
      }
    }
    public boolean save() throws Exception {
      StringBuilder sb = new StringBuilder();
      int cnt = lines.size();
      String eol;
      if (crlf) {
        eol = "\r\n";
      } else {
        eol = "\n";
      }
      for(int ln = 0;ln < cnt;ln++) {
        sb.append(lines.get(ln));
        sb.append(eol);
      }
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(sb.toString().getBytes("UTF-8"));
      fos.close();
      dirty = false;
      return true;
    }
    public boolean load(String pathfile) {
      File file = new File(pathfile);
      pathfile = file.getAbsolutePath().replaceAll("\\\\", "/");
      String path = null;
      String filename = null;
      int idx = pathfile.lastIndexOf('/');
      if (idx == -1) {
        //relative file, use current path
        path = JF.getCurrentPath();
        filename = pathfile;
      } else {
        path = pathfile.substring(0, idx);
        filename = pathfile.substring(idx+1);
      }
      if (!file.exists()) {
        //allow command line files that do not exist to be created
        this.display = filename;
        this.filename = path + "/" + filename;
        return false;
      }
      return load(path, filename);
    }
    public boolean load(String path, String filename) {
      this.display = filename;
      this.filename = path + "/" + filename;
      try {
        File file = new File(this.filename);
        if (!file.exists()) throw new Exception("file not found");
        FileInputStream fis = new FileInputStream(this.filename);
        byte data[] = JF.readAll(fis);
        fis.close();
        crlf = false;
        for(int a=0;a<data.length;a++) {
          if (data[a] == 13) {
            crlf = true;
            break;
          }
        }
        String str = new String(data);
        if (crlf) {
          str = str.replaceAll("\r", "");
        }
        String lns[] = str.split("\n");
        lines.clear();
        for(int a=0;a<lns.length;a++) {
          lines.add(new StringBuilder(lns[a]));
        }
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    public void insert(char ch) {
      StringBuilder ln = lines.get(cy);
      ln.insert(cx, ch);
      dirty = true;
    }
    public void insert(String str) {
      StringBuilder ln = lines.get(cy);
      ln.insert(cx, str);
      dirty = true;
    }
  }

  public static void main(String args[]) {
    ANSI.enableConsoleMode();
    TEdit.args = args;
    new TEdit().run();
    ANSI.disableConsoleMode();
  }

  public void run() {
    ansi = new ANSI(this);
    loadConfig();
    processArgs();
    if (tabs.size() == 0) {
      newTab();
      createAbout();
    }
    refresh();
    active = true;
    while (active) {
      try {
        ansi.process();
        if (ansi.getConsoleSize()) {
          //screen size changed
          refresh();
        }
      } catch (Exception e) {
        ansi.setForeColor(0xffffff);
        ansi.setBackColor(0x0c0c0c);
        ansi.clrscr();
        e.printStackTrace();
        return;
      }
    }
    ansi.setForeColor(0xffffff);
    ansi.setBackColor(0x0c0c0c);
    ansi.clrscr();
  }

  //display critical failure
  public void showError(String msg) {
    ansi.setForeColor(0xffffff);
    ansi.setBackColor(0x0c0c0c);
    ansi.clrscr();
    System.out.println(msg);
    System.exit(1);
  }

  public void createAbout() {
    message = new MessageDialog(ansi, new String[] {
      "",
      "Welcome to MS-DOS Editor 6.22",
      "",
      "Press <ESC> to close this dialog box",
      ""
    });
  }

  public void createHelp() {
    message = new MessageDialog(ansi, new String[] {
      "F1 = Help",
      "F3 = Find Again",
      "",
      "CTRL+F = Find",
      "CTRL+R = Replace",
      "CTRL+L = Goto Line",
      "",
      "CTRL+O = Open",
      "CTRL+S = Save",
      "CTRL+W = Save As",
      "",
      "CTRL+X = Cut",
      "CTRL+C = Copy",
      "CTRL+V = Paste",
      "",
      "Press <ESC> to close this dialog box",
      ""
    });
  }

  public void createSave() {
    message = new MessageDialog(ansi, new String[] {
      "File is not saved",
      "You can <Save> or <Discard>",
      "Press <ESC> to cancel"
    });
  }

  public void createDebug() {
    message = new MessageDialog(ansi, new String[] {
      "Size:" + ansi.width + "x" + ansi.height,
      "TabSize:" + Settings.settings.tabSize
    });
  }

  public void createGames() {
    inputType = InputType.games;
    input = new InputDialog(ansi, "games", new String[] {"<T> Tetris"}, null, "<ESC> to cancel");
    refresh();
  }

  public void loadConfig() {
    JEdit.loadcfg(false);
  }

  public void processArgs() {
    if (args == null) return;
    for(int a=0;a<args.length;a++) {
      Tab tab = new Tab();
      tab.load(args[a]);
      tabs.add(tab);
    }
  }

  public void refresh() {
    StringBuilder header = new StringBuilder();
    header.append(' ');
    header.append("File");
    header.append(' ');
    header.append("Edit");
    header.append(' ');
    header.append("Window");
    header.append(' ');
    header.append("Help");
    header.append(' ');
    while (header.length() < ansi.width) {
      header.append(' ');
    }
    ansi.gotoPos(1, 1);
    ansi.setForeColor(0x000000);
    ansi.setBackColor(0xffffff);
    System.out.print(header.toString());
    ansi.setForeColor(0xffffff);
    ansi.setBackColor(0x0000ff);
    ansi.drawBox1(1, 2, ansi.width, ansi.height-1);
    drawTab();
    if (file != null) {
      file.draw();
    }
    if (input != null) {
      input.draw();
    }
    if (message != null) {
      message.draw();
    }
    if (menu != null) {
      menu.draw();
    }
  }

  public void drawTab() {
    Tab tab = tabs.get(tabidx);
    //print title
    ansi.setNormalTextColor();
    ansi.gotoPos((ansi.width - tab.display.length()) / 2, 2);
    System.out.print(tab.display);
    int w2 = ansi.width-2;
    int h3 = ansi.height-3;
    int dx = tab.dx;
    int dy = tab.dy;
    String blank = ANSI.repeat(w2, ' ');
    String tab2spaces = ANSI.repeat(Settings.settings.tabSize, ' ');
    String ln;
    for(int y=0;y<h3;y++,dy++) {
      ansi.gotoPos(2, 3 + y);
      if (dy < tab.lines.size()) {
        ln = tab.lines.get(dy).toString().replace("\t", tab2spaces);
        if (dx > 0) {
          if (ln.length() <= dx) {
            ln = "";
          } else {
            ln = ln.substring(dx);
          }
        }
        int len = ln.length();
        if (len > w2) {
          ln = ln.substring(0, w2);
        } else if (len < w2) {
          ln = ln + ANSI.repeat(w2 - len, ' ');
        }
      } else {
        ln = blank;
      }
      if (dy >= tab.sel_begin_line && dy <= tab.sel_end_line) {
        //highlight selection
        if (dy == tab.sel_begin_line) {
          if (dy == tab.sel_end_line) {
            //single line selection
            int idxstart = tab.adjustX(tab.sel_begin_char, dy) - dx;
            int idxend = tab.adjustX(tab.sel_end_char, dy) - dx;
            String s1, s2, s3;
            if (idxstart < 0) {
              idxstart = 0;
            }
            if (idxstart >= ln.length()) {
              idxstart = ln.length() - 1;
            }
            if (idxend < 0) {
              idxend = 0;
            }
            if (idxend >= ln.length()) {
              idxend = ln.length() - 1;
            }
            s1 = ln.substring(0, idxstart);
            s2 = ln.substring(idxstart, idxend);
            s3 = ln.substring(idxend);
            System.out.print(s1);
            ansi.setSelectedTextColor();
            System.out.print(s2);
            ansi.setNormalTextColor();
            System.out.print(s3);
          } else {
            //start of multi line selection
            int idx = tab.adjustX(tab.sel_begin_char, dy) - dx;
            String s1, s2;
            if (idx < 0) {
              s1 = "";
              s2 = ln;
            } else if (idx >= ln.length()) {
              s1 = ln;
              s2 = "";
            } else {
              s1 = ln.substring(0, idx);
              s2 = ln.substring(idx);
            }
            System.out.print(s1);
            ansi.setSelectedTextColor();
            System.out.print(s2);
          }
        } else if (dy == tab.sel_end_line) {
          //last line of multi line selection
          int idx = tab.adjustX(tab.sel_end_char, dy) - dx;
          String s1, s2;
          if (idx < 0) {
            s1 = "";
            s2 = ln;
          } else if (idx >= ln.length()) {
            s1 = ln;
            s2 = "";
          } else {
            s1 = ln.substring(0, idx);
            s2 = ln.substring(idx);
          }
          System.out.print(s1);
          ansi.setNormalTextColor();
          System.out.print(s2);
        } else {
          //midline of multi line selection
          System.out.print(ln);
        }
      } else {
        System.out.print(ln);
      }
    }
    gotoCursor();
  }

  public void gotoCursor() {
    Tab tab = tabs.get(tabidx);
    ansi.gotoPos(2+tab.adjustX(tab.cx, tab.cy)-tab.dx, 3+tab.cy-tab.dy);
  }

  public void newTab() {
    Tab tab = new Tab();
    tabs.add(tab);
    tabidx = tabs.size() - 1;
  }

  public void splitLine() {
    Tab tab = tabs.get(tabidx);
    tab.dirty = true;
    StringBuilder ln = tab.lines.get(tab.cy);
    if (tab.cx < ln.length()) {
      String newln = ln.substring(tab.cx);
      ln.delete(tab.cx, ln.length());
      tab.lines.add(tab.cy+1, new StringBuilder(newln));
    } else {
      tab.lines.add(tab.cy+1, new StringBuilder());
    }
    tab.cx = 0;
    tab.dx = 0;
  }

  public void enter() {
    splitLine();
    moveDown(false);
  }

  public void backspace() {
    Tab tab = tabs.get(tabidx);
    tab.dirty = true;
    StringBuilder ln = tab.lines.get(tab.cy);
    if (tab.cx > 0) {
      tab.lines.get(tab.cy).deleteCharAt(tab.cx-1);
      moveLeft(false);
    } else {
      if (tab.cy == 0) return;
      tab.lines.remove(tab.cy);
      tab.cy--;
      moveEnd(false, false);
      tab.lines.get(tab.cy).append(ln);
      drawTab();
    }
  }

  public void delete(boolean draw) {
    Tab tab = tabs.get(tabidx);
    if (tab.hasSelection()) {
      tab.deleteSelection(ansi.height);
    } else {
      StringBuilder ln = tab.lines.get(tab.cy);
      if (tab.cx < ln.length()) {
        tab.lines.get(tab.cy).deleteCharAt(tab.cx);
      } else {
        if (tab.cy == tab.lines.size()-1) return;
        String ln2 = tab.lines.get(tab.cy+1).toString();
        tab.lines.remove(tab.cy+1);
        tab.lines.get(tab.cy).append(ln2);
      }
    }
    if (draw) drawTab();
  }

  public void checkBeyondEOL() {
    Tab tab = tabs.get(tabidx);
    int len = tab.lines.get(tab.cy).length();
    if (tab.cx > len) {
      tab.cx = len;
    }
  }

  public void showCursor() {
    Tab tab = tabs.get(tabidx);
    int dx1 = tab.dx;
    int dx2 = tab.dx + (ansi.width - 3);
    if (tab.adjustX(tab.cx, tab.cy) > dx2) {
      tab.dx = tab.adjustX(tab.cx, tab.cy) - (ansi.width - 3);
    }
    if (tab.adjustX(tab.cx, tab.cy) < dx1) {
      tab.dx = tab.adjustX(tab.cx, tab.cy);
    }
    int dy1 = tab.dy;
    int dy2 = tab.dy + (ansi.height - 4);
    if (tab.cy > dy2) {
      tab.dy = tab.cy - (ansi.height - 4);
    }
    if (tab.cy < dy1) {
      tab.dy = tab.cy;
    }
  }

  public void moveHome(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cx == 0) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cx = 0;
    tab.dx = 0;
    if (shift) {
      tab.adjustSelection(false);
    }
    drawTab();
  }

  public void moveEnd(boolean shift, boolean draw) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    int end = tab.lines.get(tab.cy).length();
    if (tab.cx == end) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cx = end;
    showCursor();
    if (shift) {
      tab.adjustSelection(true);
    }
    if (draw) drawTab();
  }

  public void moveTop(boolean shift, boolean draw) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cx == 0 && tab.cy == 0) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cx = 0;
    tab.dx = 0;
    tab.cy = 0;
    tab.dy = 0;
    if (shift) {
      tab.adjustSelection(false);
    }
    if (draw) drawTab();
  }

  public void moveBottom(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    int end_line = tab.lines.size() - 1;
    int end_char = tab.lines.get(tab.cy).length();
    if (tab.cx == end_char && tab.cy == end_line) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cx = end_char;
    tab.cy = end_line;
    showCursor();
    if (shift) {
      tab.adjustSelection(true);
    }
    drawTab();
  }

  public void moveToLine(int ln) {
    Tab tab = tabs.get(tabidx);
    tab.clearSelection();
    if (ln >= tab.lines.size()) {
      ln = tab.lines.size() - 1;
    }
    if (tab.cy == ln) return;
    tab.cy = ln;
    tab.cx = 0;
    showCursor();
    drawTab();
  }

  public void moveUp(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cy == 0) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cy--;
    checkBeyondEOL();
    showCursor();
    if (shift) {
      tab.adjustSelection(false);
    }
    drawTab();
  }

  public void moveDown(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cy == tab.lines.size()-1) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cy++;
    checkBeyondEOL();
    showCursor();
    if (shift) {
      tab.adjustSelection(true);
    }
    drawTab();
  }

  public void moveLeft(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cx == 0) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cx--;
    showCursor();
    if (shift) {
      tab.adjustSelection(false);
    }
    drawTab();
  }

  public void moveRight(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cx == tab.lines.get(tab.cy).length()) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cx++;
    showCursor();
    if (shift) {
      tab.adjustSelection(true);
    }
    drawTab();
  }

  public void movePageUp(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cy == 0) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cy -= ansi.height - 3;
    if (tab.cy < 0) tab.cy = 0;
    checkBeyondEOL();
    showCursor();
    if (shift) {
      tab.adjustSelection(false);
    }
    drawTab();
  }

  public void movePageDown(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cy == tab.lines.size()-1) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cy += ansi.height - 3;
    if (tab.cy > tab.lines.size() - 1) tab.cy = tab.lines.size() - 1;
    checkBeyondEOL();
    showCursor();
    if (shift) {
      tab.adjustSelection(true);
    }
    drawTab();
  }

  public void open() {
    //need to create open file dialog
    file = new FileDialog(ansi, true, null, null);
    file.draw();
  }

  public void close(boolean discard) {
    Tab tab = tabs.get(tabidx);
    if (tab.dirty && !discard) {
      ask_save = true;
      createSave();
    }
  }

  public boolean save() {
    Tab tab = tabs.get(tabidx);
    if (tab.filename == null) {
      return saveAs();
    }
    try {
      return tab.save();
    } catch (Exception e) {
      message = new MessageDialog(ansi, new String[] {"Error", "Unable to save file", "Press <ESC> to close"});
      return false;
    }
  }

  public boolean saveAs() {
    file = new FileDialog(ansi, false, null, null);
    file.draw();
    return true;
  }

  /**
  * get string from Clipboard
  */
  public String getClipboardText() {
    String ret = "";
    Clipboard sysClip = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
    Transferable clipTf = sysClip.getContents(null);

    if (clipTf != null) {
      if (clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        try {
          ret = (String) clipTf.getTransferData(DataFlavor.stringFlavor);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    return ret;
  }

  /**
  * put string into Clipboard
  */
  public void setClipboardText(String writeMe) {
    Clipboard clip = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
    Transferable tText = new StringSelection(writeMe);
    clip.setContents(tText, null);
  }


  public void copy() {
    //copy selection to clipboard
    Tab tab = tabs.get(tabidx);
    String str = tab.getSelection();
    if (str == null) return;
    setClipboardText(str);
  }

  public void paste(String str) {
    Tab tab = tabs.get(tabidx);
    char ca[] = str.toCharArray();
    for(int a=0;a<ca.length;a++) {
      char ch = ca[a];
      if (ch == 10) {
        splitLine();
        tab.cy++;
      }
      if (ch < 32) continue;
      tab.insert(ch);
      tab.cx++;
    }
    tab.dirty = true;
    showCursor();
  }

  public void paste() {
    //paste clipboard
    String str = getClipboardText();
    if (str == null) return;
    paste(str);
  }

  public void cut() {
    //delete to clipboard
    Tab tab = tabs.get(tabidx);
    tab.dirty = true;
    String str = tab.getSelection();
    if (str == null) return;
    delete(true);
    setClipboardText(str);
  }

  public void doFile() {
    String path = file.getPath();
    String filename = file.getFilename();
    if (filename != null) {
      if (file.isLoading()) {
        Tab newTab = new Tab();
        if (newTab.load(path, filename)) {
          tabs.add(newTab);
          tabidx = tabs.size() - 1;
          file = null;
        } else {
          message = new MessageDialog(ansi, new String[] {"Error", "Unable to open file", "Press <ESC> to close"});
          file.setClosed(false);
        }
      } else {
        Tab tab = tabs.get(tabidx);
        if (tab.saveAs(path, filename)) {
          file = null;
        } else {
          message = new MessageDialog(ansi, new String[] {"Error", "Unable to save file", "Press <ESC> to close"});
          file.setClosed(false);
        }
      }
    } else {
      //cancelled
      file = null;
    }
    refresh();
  }

  public void doMenu() {
    String action = menu.getAction();
    int widx = action.indexOf('.');
    if (widx != -1) {
      //window selection
      tabidx = Integer.valueOf(action.substring(0, widx)) - 1;
      menu = null;
      return;
    }
    switch (action) {
      case "New":
        newTab();
        break;
      case "Open":
        open();
        break;
      case "Close":
        close(false);
        break;
      case "Save":
        save();
        break;
      case "Save As":
        saveAs();
        break;
      case "Properties":
        properties();
        break;
      case "Exit":
        active = false;
        break;
      case "Cut":
        cut();
        break;
      case "Copy":
        copy();
        break;
      case "Paste":
        paste();
        break;
      case "Find":
        find();
        break;
      case "Replace":
        replace();
        break;
      case "Options":
        options();
        break;
      case "Prev":
        tabidx--;
        if (tabidx == -1) tabidx = tabs.size() - 1;
        break;
      case "Next":
        tabidx++;
        if (tabidx == tabs.size()) tabidx = 0;
        break;
      case "Keys":
        createHelp();
        break;
      case "About":
        createAbout();
        break;
      case "@Left":
        menuIdx--;
        if (menuIdx == -1) menuIdx = menus.length - 1;
        createMenu();
        return;
      case "@Right":
        menuIdx++;
        if (menuIdx == menus.length) menuIdx = 0;
        createMenu();
        return;
    }
    menu = null;
  }

  public void doInput() {
    switch (inputType) {
      case gotoline:
        String str = input.getText(0);
        input = null;
        if (str == null) return;
        int ln = JF.atoi(str);
        if (ln <= 0) ln = 1;
        moveToLine(ln-1);
        break;
      case find:
        doFind(true);
        input = null;
        break;
      case replace:
        doReplace();
        break;
      case options:
        doOptions();
        break;
      case properties:
        doProperties();
        break;
      case games:
        String action = input.getAction();
        input = null;
        switch (action) {
          case "T":
            game = new Tetris();
            ((Tetris)game).run(ansi);
            refresh();
            game = null;
            break;
        }
        break;
    }
  }

  public void doFind(boolean showError) {
    if (input != null) {
      find = input.getText(0);
    }
    found = false;
    Tab tab = tabs.get(tabidx);
    int cnt = tab.lines.size();
    int start_line, start_char;
    if (tab.hasSelection()) {
      start_line = tab.sel_end_line;
      start_char = tab.sel_end_char;
    } else {
      start_line = tab.cy;
      start_char = tab.cx;
    }
    for(int ln=start_line;ln<cnt;ln++) {
      int idx = tab.lines.get(ln).indexOf(find, (ln == start_line) ? start_char : 0);
      if (idx != -1) {
        found = true;
        tab.setSelection(ln, idx, ln, idx+find.length());
        tab.cx = idx;
        tab.cy = ln;
        showCursor();
        break;
      }
    }
    if (inputType == InputType.find) {
      input = null;
    }
    if (!found) {
      if (showError) message = new MessageDialog(ansi, new String[] {"Error", "Find text not found", "Press <ESC> to close"});
    }
  }

  public void doReplace() {
    find = input.getText(0);
    replace = input.getText(1);
    String action = input.getAction();
    if (action == null) {
      if (found) {
        action = "Replace";
      } else {
        action = "Find";
      }
    }
    switch (action) {
      case "Find":
        doFind(true);
        break;
      case "Replace":
        doReplaceOne();
        break;
      case "ReplaceAll":
        doReplaceAll();
        input = null;
        break;
      case "ESC":
        input = null;
        break;
    }
    if (input != null) {
      input.setAction(null);
      input.setClosed(false);
    }
    refresh();
  }

  private void doReplaceOne() {
    if (!found) return;
    found = false;
    delete(false);
    paste(replace);
  }

  private void doReplaceAll() {
    moveTop(false, false);
    int cnt = 0;
    do {
      doFind(false);
      if (!found) break;
      doReplaceOne();
      cnt++;
    } while (true);
    input = null;
    message = new MessageDialog(ansi, new String[] {"Replaced " + cnt + " occurances", "Press <ESC> to close"});
  }

  public void createMenu() {
    String opts[] = menus[menuIdx];
    if (menuIdx == 2) {
      //list windows
      String newopts[] = new String[opts.length + tabs.size()];
      for(int a=0;a<opts.length;a++) {
        newopts[a] = opts[a];
      }
      for(int a=0;a<tabs.size();a++) {
        newopts[a + opts.length] = String.format("%d.%c%s", a+1, a == tabidx ? '*' : ' ', tabs.get(a).display);
      }
      opts = newopts;
    }
    menu = new Menu(ansi, opts, menusPos[menuIdx], 2);
  }

  public void find() {
    inputType = InputType.find;
    input = new InputDialog(ansi, "title", new String[] {"Enter text to find"}, new String[] {find}, "Press <Enter> to find or <ESC> to cancel");
    input.draw();
  }

  public void findAgain() {
    if (find == null) {
      find();
    } else {
      doFind(true);
    }
    refresh();
  }

  public void replace() {
    inputType = InputType.replace;
    input = new InputDialog(ansi, "title", new String[] {"Enter text to find", "Enter text to replace with"}, new String[] {find, replace}, "<Find>, <Replace>, <ReplaceAll> or <ESC>");
    input.draw();
  }

  public void gotoLine() {
    inputType = InputType.gotoline;
    input = new InputDialog(ansi, "title", new String[] {"Enter line number"}, null, InputDialog.ENTER_ESC);
    input.draw();
  }

  public void options() {
    inputType = InputType.options;
    input = new InputDialog(ansi, "title"
      , new String[] {
        "{checkbox} Preserve line endings",
        "{checkbox} Unix line endings"
      }
      , new String[] {
        Boolean.toString(Settings.settings.bPreserve),
        Boolean.toString(Settings.settings.bUnix)
      }, InputDialog.ENTER_ESC);
    input.draw();
  }

  public void doOptions() {
    Settings.settings.bPreserve = input.isChecked(0);
    Settings.settings.bUnix = input.isChecked(1);
    JEdit.savecfg();
    input = null;
    refresh();
  }

  public void properties() {
    Tab tab = tabs.get(tabidx);
    inputType = InputType.properties;
    input = new InputDialog(ansi, "title"
      , new String[] {
        "{checkbox} Unix line endings"
      }
      , new String[] {
        Boolean.toString(!tab.crlf)
      }, InputDialog.ENTER_ESC);
    input.draw();
  }

  public void doProperties() {
    Tab tab = tabs.get(tabidx);
    tab.crlf = !input.isChecked(0);
    input = null;
    refresh();
  }
}

/** THex - text/ansi hex editor
 *
 * @author Peter Quiring
 */

import java.util.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.awt.event.KeyEvent;

import javaforce.*;
import javaforce.ansi.*;
import javaforce.jni.*;

public class THex implements KeyEvents {
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

  public int menuIdx;

  public String menus[][] = {
    {"New", "Open", "Close", "Save", "Save As", "-", "Exit"},
    {"Cut", "Copy", "Paste", "-", "Find", "Replace"},
    {"Prev", "Next", "-"},
    {"Keys", "About"},
  };

  public int menusPos[] = {2, 7, 12, 19};

  public enum InputType {none, gotoOffset, find, replace};
  public InputType inputType = InputType.none;

  public String find, replace;
  public boolean found;

  public void keyPressed(int keyCode, int keyMods) {
//    System.err.println("keyCode=" + keyCode + ",keyMods=" + keyMods);
    if (keyMods == 0 && keyCode == KeyEvent.VK_F5) {
      refresh();
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
      case 9:
        switchSides();
        break;
      case 10:
        enter();
        break;
      case 12:
        gotoOffset();
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
        if (tab.insertMode)
          tab.insert(key);
        else
          tab.overwrite(key);
        moveRight(false);
        break;
    }
  }

  public static class Tab {
    public Tab() {
      display = "Untitled";
    }
    public Tab(String name) {
      filename = name;
      display = name;
    }
    public String filename;
    public String display;
    public StringBuilder txt = new StringBuilder();
    public int dx, dy;  //display (window) position
    public int cx, cy;  //cursor position (cx=octet(0-31))
    public boolean leftSide = true;
    public boolean leftNibble = true;
    public boolean insertMode = false;
    public boolean dirty;
    public int sel_begin_y = -1;
    public int sel_end_y = -1;
    public int sel_begin_x = -1;
    public int sel_end_x = -1;
    public boolean sel_at_begin;
    public boolean sel_at_end;
    public boolean hasSelection() {
      return sel_begin_y != -1;
    }
    public void clearSelection() {
      sel_begin_y = -1;
      sel_end_y = -1;
    }
    public void startSelection() {
      sel_begin_y = cy;
      sel_begin_x = cx;
      sel_end_y = cy;
      sel_end_x = cx;
      sel_at_begin = true;
      sel_at_end = true;
    }
    public void adjustSelectionCheck() {
      sel_at_begin = sel_begin_y == cy && sel_begin_x == cx;
      sel_at_end = sel_end_y == cy && sel_end_x == cx;
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
      sel_begin_y = cy;
      sel_begin_x = cx;
      checkSelection();
    }
    public void adjustEndSelection() {
      sel_end_y = cy;
      sel_end_x = cx;
      checkSelection();
    }
    private void checkSelection() {
      if (sel_begin_y == sel_end_y && sel_begin_x == sel_end_x) {clearSelection(); return;}
      if (sel_begin_y > sel_end_y || (sel_begin_y == sel_end_y && sel_begin_x > sel_end_x)) swapSelection();
    }
    private void swapSelection() {
      int _line = sel_begin_y;
      int _char = sel_begin_x;
      sel_begin_y = sel_end_y;
      sel_begin_x = sel_end_x;
      sel_end_y = _line;
      sel_end_x = _char;
    }
    public boolean pastSelection() {
      if (cy > sel_end_y) return true;
      if (cy == sel_end_y && cx > sel_end_x) return true;
      return false;
    }
    public void deleteSelection(int height) {
      if (!hasSelection()) return;
      int full_lines = 0;
      for(int ln=sel_begin_y + 1;ln<sel_end_y;ln++) {
        int pos = (sel_begin_y + 1) * 16;
        txt.delete(pos, pos + 16);
        full_lines++;
      }
      sel_end_y -= full_lines;
      if (sel_begin_y == sel_end_y) {
        //delete partial line
        int pos = sel_begin_y * 16;
        txt.delete(pos + sel_begin_x, pos + sel_end_x);
      } else {
        txt.delete(sel_end_y * 16, sel_end_y * 16 + sel_end_x);
        txt.delete(sel_begin_y * 16 + sel_begin_x, sel_begin_y * 16 + 16);
      }
      cy = sel_begin_y;
      cx = sel_begin_x;
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
      for(int ln=sel_begin_y;ln <= sel_end_y;ln++) {
        if (ln == sel_begin_y) {
          int pos = sel_begin_y * 16;
          if (sel_begin_y != sel_end_y) {
            sb.append(txt.substring(pos + sel_begin_x, pos + 16));
          } else {
            sb.append(txt.substring(pos + sel_begin_x, pos + sel_end_x));
          }
        } else if (ln == sel_end_y) {
          int pos = sel_end_y * 16;
          sb.append(txt.substring(pos, pos + sel_end_x));
        } else {
          int pos = ln * 16;
          sb.append(txt.subSequence(pos, pos + 16));
        }
      }
      return sb.toString();
    }
    public void setSelection(int begin_line, int begin_char, int end_line, int end_char) {
      sel_begin_y = begin_line;
      sel_begin_x = begin_char;
      sel_end_y = end_line;
      sel_end_x = end_char;
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
      FileOutputStream fos = new FileOutputStream(filename);
      byte data[] = new byte[txt.length()];
      txt.toString().getBytes(0, txt.length(), data, 0);  //getBytes() will not encode bytes
      fos.write(data);
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
        txt.setLength(0);
        txt.append(new String(data));
        return true;
      } catch (Exception e) {
        return false;
      }
    }
    public void insert(char ch) {
      if (leftSide) {
        int value = -1;
        ch = Character.toLowerCase(ch);
        if (ch >= '0' && ch <= '9') {
          value = ch - '0';
        } else if (ch >= 'a' && ch <= 'f') {
          value = ch - 'a' + 10;
        } else {
          value = 0;
        }
        int pos = cy * 16 + cx;
        if (leftNibble) {
          txt.insert(pos, (char)(value << 4));
        } else {
          if (pos == txt.length()) {
            txt.insert(pos, (char)0);
          }
          ch = txt.charAt(pos);
          ch &= 0xf0;
          ch |= value;
          txt.setCharAt(pos, ch);
        }
      } else {
        txt.insert(cy * 16 + cx, ch);
      }
      dirty = true;
    }
    public void insert(String str) {
      if (leftSide) {
        char chs[] = str.toCharArray();
        for(int a=0;a<chs.length;a++) {
          insert(chs[a]);
        }
      } else {
        txt.insert(cy * 16 + cx, str);
      }
      dirty = true;
    }
    public void overwrite(char ch) {
      if (cy * 16 + cx == txt.length()) {
        insert(ch);
        return;
      }
      if (leftSide) {
        int value = -1;
        ch = Character.toLowerCase(ch);
        if (ch >= '0' && ch <= '9') {
          value = ch - '0';
        } else if (ch >= 'a' && ch <= 'f') {
          value = ch - 'a' + 10;
        } else {
          value = 0;
        }
        int pos = cy * 16 + cx;
        if (leftNibble) {
          ch = txt.charAt(pos);
          ch &= 0x0f;
          ch |= value << 4;
          txt.setCharAt(pos, ch);
        } else {
          if (pos == txt.length()) {
            txt.insert(pos, (char)0);
          }
          ch = txt.charAt(pos);
          ch &= 0xf0;
          ch |= value;
          txt.setCharAt(pos, ch);
        }
      } else {
        txt.setCharAt(cy * 16 + cx, ch);
      }
      dirty = true;
    }
    public void overwrite(String str) {
      if (leftSide) {
        char chs[] = str.toCharArray();
        for(int a=0;a<chs.length;a++) {
          overwrite(chs[a]);
        }
      } else {
        txt.replace(cy * 16 + cx, cy * 16 + cx + str.length(), str);
      }
      dirty = true;
    }
  }

  public static void main(String args[]) {
    THex.args = args;
    ANSI.enableConsoleMode();
    new THex().run();
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
      "Welcome to MS-DOS Hex Editor 6.22",
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
      "CTRL+L = Goto Hex Offset",
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
      "Size:" + ansi.width + "x" + ansi.height
    });
  }

  public void loadConfig() {
    jfhex.loadcfg(false);
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

  private String text_normal = ANSI.makeBackColor(0x0000ff) + ANSI.makeForeColor(0xffffff);
  private String text_select = ANSI.makeBackColor(0xffffff) + ANSI.makeForeColor(0x000000);

  public void drawTab() {
    Tab tab = tabs.get(tabidx);
    //print title
    ansi.setNormalTextColor();
    ansi.gotoPos((ansi.width - tab.display.length()) / 2, 2);
    System.out.print(tab.display);
    int w2 = ansi.width-2;
    int h3 = ansi.height-3;
    int pos;
    StringBuilder ln = new StringBuilder();
    int len = tab.txt.length();
    boolean selected = false;
    if (tab.sel_begin_y != -1) {
      if (tab.sel_begin_y < tab.dy) {
        selected = true;
      }
    }
    for(int y=0;y<h3;y++) {
      ansi.gotoPos(2, 3 + y);
      pos = (tab.dy + y) * 16;
      ln.setLength(0);
      ln.append(String.format("%08x", pos));
      ln.append(' ');
      for(int x=0;x<16;x++) {
        if (pos < len) {
          ln.append(String.format("%02x", (int)tab.txt.charAt(pos)));
        } else {
          ln.append("  ");
        }
        ln.append(' ');
        pos++;
      }
      ln.append(' ');
      pos = (tab.dy + y) * 16;
      for(int x=0;x<16;x++) {
        if (pos >= len) break;
        char ch = tab.txt.charAt(pos);
        if (ch > 127) ch = ASCII8.convert(ch);
        if (ch < 32) ch = '?';
        ln.append(ch);
        pos++;
      }
      while (ln.length() < w2) {
        ln.append(' ');
      }
      int sel_start = -1;
      int sel_end = -1;
      if (tab.dy + y == tab.sel_begin_y) {
        //start of selection
        if (tab.sel_begin_y == tab.sel_end_y) {
          //single line selection
          sel_start = tab.sel_begin_x;
          sel_end = tab.sel_end_x;
        } else {
          sel_start = tab.sel_begin_x;
          sel_end = 16;
          selected = true;
        }
      }
      else if (tab.dy + y == tab.sel_end_y) {
        //end of selection
        sel_start = 0;
        sel_end = tab.sel_end_x;
        selected = false;
      }
      else if (selected) {
        //full line selection
        sel_start = 0;
        sel_end = 16;
      }
      if (sel_start != -1) {
        //highlight selection
        ln.insert(9 + 16 * 3 + 1 + sel_end, text_normal);
        ln.insert(9 + 16 * 3 + 1 + sel_start, text_select);
        ln.insert(9 + sel_end * 3, text_normal);
        ln.insert(9 + sel_start * 3, text_select);
      }
      System.out.println(ln);
    }
    gotoCursor();
  }

  public void gotoCursor() {
    Tab tab = tabs.get(tabidx);
    int x = 0;
    int y = 0;
    if (tab.leftSide) {
      x = 2+8+1+tab.cx*3;
      if (!tab.leftNibble) x++;
      y = 3+tab.cy-tab.dy;
    } else {
      x = 2+8+1+16*3+1+tab.cx;
      y = 3+tab.cy-tab.dy;
    }
    ansi.gotoPos(x, y);
  }

  public void newTab() {
    Tab tab = new Tab();
    tabs.add(tab);
    tabidx = tabs.size() - 1;
  }

  public void switchSides() {
    Tab tab = tabs.get(tabidx);
    tab.leftSide = !tab.leftSide;
    drawTab();
  }

  public void enter() {
    moveDown(false);
    moveHome(false);
  }

  public void backspace() {
    Tab tab = tabs.get(tabidx);
    tab.dirty = true;
    if (tab.hasSelection()) {
      tab.deleteSelection(ansi.height);
    } else {
      if (tab.cx == 0 && tab.cy == 0) return;
      int pos = tab.cy * 16 + tab.cx - 1;
      tab.txt.delete(pos, pos + 1);
      tab.cx--;
      if (tab.cx == -1) {
        tab.cx = 15;
        tab.cy--;
        if (tab.cy == -1) {
          tab.cx = 0;
          tab.cy = 0;
        }
      }
      tab.leftNibble = true;
      showCursor();
    }
    drawTab();
  }

  public void delete(boolean draw) {
    Tab tab = tabs.get(tabidx);
    tab.dirty = true;
    if (tab.hasSelection()) {
      tab.deleteSelection(ansi.height);
    } else {
      int pos = tab.cy * 16 + tab.cx;
      tab.txt.delete(pos, pos + 1);
    }
    if (draw) drawTab();
  }

  public void checkBeyondEOF() {
    Tab tab = tabs.get(tabidx);
    int len = tab.txt.length();
    if (tab.cy * 16 + tab.cx > len) {
      tab.cy = len / 16;
      tab.cx = len % 16;
    }
  }

  public void showCursor() {
    Tab tab = tabs.get(tabidx);
    int dx1 = tab.dx;
    int dx2 = tab.dx + (ansi.width - 3);
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
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cx = 31;
    checkBeyondEOF();
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
    int length = tab.txt.length();
    if (tab.cy * 16 + tab.cx == length) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cy = length / 16;
    tab.cx = length - (tab.cy * 16);
    showCursor();
    if (shift) {
      tab.adjustSelection(true);
    }
    drawTab();
  }

  public void moveToOffset(int offset) {
    Tab tab = tabs.get(tabidx);
    tab.clearSelection();
    int length = tab.txt.length();
    if (offset > length) {
      offset = length;
    }
    tab.cy = offset / 16;
    tab.cx = offset % 16;
    checkBeyondEOF();
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
    checkBeyondEOF();
    showCursor();
    if (shift) {
      tab.adjustSelection(false);
    }
    drawTab();
  }

  public void moveDown(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    int lines = tab.txt.length() / 16;
    int chars = tab.txt.length() % 16;
    if (chars > 0) lines++;
    if (tab.cy == lines) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cy++;
    checkBeyondEOF();
    showCursor();
    if (shift) {
      tab.adjustSelection(true);
    }
    drawTab();
  }

  public void moveLeft(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (tab.cx == 0 && tab.cy == 0 && tab.leftNibble) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    if (tab.leftSide) {
      if (tab.leftNibble) {
        tab.leftNibble = false;
        tab.cx--;
        if (tab.cx == -1) {
          tab.cx = 15;
          tab.cy--;
          if (tab.cy == -1) {
            tab.cx = 0;
            tab.cy = 0;
          }
        }
      } else {
        tab.leftNibble = true;
      }
    } else {
      tab.cx--;
      if (tab.cx == -1) {
        tab.cx = 15;
        tab.cy--;
        if (tab.cy == -1) {
          tab.cx = 0;
          tab.cy = 0;
        }
      }
    }
    showCursor();
    if (shift) {
      tab.adjustSelection(false);
    }
    drawTab();
  }

  public void moveRight(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    if (tab.leftSide) {
      if (tab.leftNibble) {
        tab.leftNibble = false;
      } else {
        tab.leftNibble = true;
        tab.cx++;
        if (tab.cx == 16) {
          tab.cx = 0;
          tab.cy++;
        }
      }
    } else {
      tab.cx++;
      if (tab.cx == 16) {
        tab.cx = 0;
        tab.cy++;
      }
    }
    checkBeyondEOF();
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
    checkBeyondEOF();
    showCursor();
    if (shift) {
      tab.adjustSelection(false);
    }
    drawTab();
  }

  public void movePageDown(boolean shift) {
    Tab tab = tabs.get(tabidx);
    if (!shift) tab.clearSelection();
    int lines = tab.txt.length() / 16;
    int chars = tab.txt.length() % 16;
    if (chars > 0) lines++;
    if (tab.cy == lines) return;
    if (shift) {
      if (tab.hasSelection())
        tab.adjustSelectionCheck();
      else
        tab.startSelection();
    }
    tab.cy += ansi.height - 3;
    if (tab.cy > lines) tab.cy = lines;
    checkBeyondEOF();
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
    boolean leftSide = tab.leftSide;
    tab.leftSide = false;
    for(int a=0;a<ca.length;a++) {
      char ch = ca[a];
      if (ch == 10) {
        tab.cx = 0;
        tab.cy++;
      }
      if (ch < 32) continue;
      tab.insert(ch);
      //move right
      tab.cx++;
      if (tab.cx == 16) {
        tab.cy++;
      }
    }
    tab.leftSide = leftSide;
    tab.leftNibble = true;
    tab.dirty = true;
    checkBeyondEOF();
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
      case gotoOffset:
        String str = input.getText(0);
        input = null;
        if (str == null) return;
        int off = JF.atox(str);
        if (off < 0) off = 0;
        moveToOffset(off);
        break;
      case find:
        doFind(true);
        input = null;
        break;
      case replace:
        doReplace();
        break;
    }
  }

  public void doFind(boolean showError) {
    if (input != null) {
      find = input.getText(0);
    }
    found = false;
    Tab tab = tabs.get(tabidx);
    int start_line, start_char;
    if (tab.hasSelection()) {
      start_line = tab.sel_end_y;
      start_char = tab.sel_end_x;
    } else {
      start_line = tab.cy;
      start_char = tab.cx;
    }
    int start_pos = start_line * 16 + start_char;
    int idx = tab.txt.indexOf(find, start_pos);
    if (idx != -1) {
      found = true;
      int end_pos = idx + find.length();
      tab.setSelection(idx / 16, idx % 16, end_pos / 16, end_pos % 16);
      tab.cx = idx % 16;
      tab.cy = idx / 16;
      showCursor();
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

  public void gotoOffset() {
    inputType = InputType.gotoOffset;
    input = new InputDialog(ansi, "title", new String[] {"Enter hex offset"}, null, InputDialog.ENTER_ESC);
    input.draw();
  }
}

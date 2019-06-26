package javaforce.ansi.server;

/**
 * ANSI.java
 *
 * Created on Dec 17, 2018, 9:49 AM
 *
 * see https://en.wikipedia.org/wiki/ANSI_escape_code
 *
 * @author pquiring
 */

import java.util.*;
import java.awt.event.KeyEvent;

import javaforce.*;
import javaforce.jni.*;

public class ANSI {
  private final static char ESC = 0x1b;
  private StringBuffer buffer = new StringBuffer();
  private KeyEvents keyEvents;

  public int width;
  public int height;

  public static String repeat(int count, char ch) {
    if (count < 0) return "";
    char ca[] = new char[count];
    for(int a=0;a<count;a++) {
      ca[a] = ch;
    }
    return new String(ca);
  }

  public static String pad(String in, int maxLen) {
    if (in == null) return repeat(maxLen, ' ');
    if (in.length() > maxLen) {
      return in.substring(0, maxLen);
    } else {
      return in + repeat(maxLen - in.length(), ' ');
    }
  }

  public ANSI(KeyEvents ke) {
    keyEvents = ke;
    getConsoleSize();
  }

  public static void enableConsoleMode() {
    JFNative.load_ffmpeg = false;  //speed up startup on Linux
    if (JF.isWindows())
      WinNative.enableConsoleMode();
    else
      LnxNative.enableConsoleMode();
  }

  public static void disableConsoleMode() {
    if (JF.isWindows())
      WinNative.disableConsoleMode();
    else
      LnxNative.disableConsoleMode();
  }

  public boolean getConsoleSize() {
    int xy[];
    if (JF.isWindows()) {
      xy = WinNative.getConsoleSize();
    } else {
      xy = LnxNative.getConsoleSize();
    }
    boolean changed = width != xy[0] || height != xy[1];
    if (changed) {
      width = xy[0];
      height = xy[1];
    }
    return changed;
  }

  public boolean kbhit() {
    if (JF.isWindows()) {
      return WinNative.peekConsole();
    } else {
      return LnxNative.peekConsole();
    }
  }

  public char readConsole() {
    //System.in is buffered and not suitable for console apps
    char ch;
    if (JF.isWindows()) {
      ch = WinNative.readConsole();
    } else {
      ch = LnxNative.readConsole();
    }
    return ch;
  }

  public void flushConsole() {
    while (kbhit()) {
      readConsole();
    }
  }

  public void clrscr() {
    System.out.print(ESC + "[2J");
  }

  /** Move cursor to pos (1,1 = top right) */
  public static void gotoPos(int x, int y) {
    System.out.print(ESC + "[" + y + ";" + x + "H");
  }

  public static void drawBox1(int x, int y, int w, int h) {
    gotoPos(x,y);
    System.out.print(ASCII8.convert(218));
    for(int c=2;c<=w-1;c++) {
      System.out.print(ASCII8.convert(196));
    }
    System.out.print(ASCII8.convert(191));
    for(int c=1;c<=h-2;c++) {
      gotoPos(x,y+c);
      System.out.print(ASCII8.convert(179));
      gotoPos(x+w-1,y+c);
      System.out.print(ASCII8.convert(179));
    }
    gotoPos(x,y+h-1);
    System.out.print(ASCII8.convert(192));
    for(int c=2;c<=w-1;c++) {
      System.out.print(ASCII8.convert(196));
    }
    System.out.print(ASCII8.convert(217));
  }

  public static void drawBox2(int x, int y, int w, int h) {
    gotoPos(x,y);
    System.out.print(ASCII8.convert(201));
    for(int c=2;c<=w-1;c++) {
      System.out.print(ASCII8.convert(205));
    }
    System.out.print(ASCII8.convert(187));
    for(int c=1;c<=h-2;c++) {
      gotoPos(x,y+c);
      System.out.print(ASCII8.convert(186));
      gotoPos(x+w-1,y+c);
      System.out.print(ASCII8.convert(186));
    }
    gotoPos(x,y+h-1);
    System.out.print(ASCII8.convert(200));
    for(int c=2;c<=w-1;c++) {
      System.out.print(ASCII8.convert(205));
    }
    System.out.print(ASCII8.convert(188));
  }

  public void drawFill(int x, int y, int w, int h) {
    String ln = repeat(w, ' ');
    for(int a=0;a<h;a++) {
      gotoPos(x,y+a);
      System.out.print(ln);
    }
  }

  //dialog colors
  public static int fore_clr = 0x000000;
  public static int back_clr = 0xffffff;
  public static int field_clr = 0xaaaaaa;

  public static void setDialogColor() {
    setForeColor(fore_clr);
    setBackColor(back_clr);
  }

  public static void setFieldColor() {
    setForeColor(fore_clr);
    setBackColor(field_clr);
  }

  //normal text colors
  public static int text_clr = 0xffffff;
  public static int blue_clr = 0x0000ff;

  public static void setNormalTextColor() {
    setForeColor(text_clr);
    setBackColor(blue_clr);
  }

  public static void setSelectedTextColor() {
    setForeColor(blue_clr);
    setBackColor(text_clr);
  }

  public Field[] drawWindow(int x, int y, int w, int h, String txt[]) {
    ArrayList<Field> fields = new ArrayList<Field>();
    setForeColor(fore_clr);
    setBackColor(back_clr);
    drawBox1(x,y,w,h);
    int y1 = y+1;
    int y2 = y+h-1;
    int idx = 0;
    int w2 = w - 2;
    int x1 = x + 1;
    int fx = -1;
    int fy = -1;
    for(int py=y1;py<y2;py++) {
      String ln = txt[idx++];
      if (ln == null) ln = "";
      int len = ln.replaceAll("\\{checkbox\\}", "[ ]").length();
      int pad = w2 - len;
      int padleft = pad/2;
      int padright = pad - padleft;
      gotoPos(x1, py);
      if (ln.indexOf('[') != -1) {
        //input field(s)
        for(int pos=0;pos<len;pos++) {
          if (ln.charAt(pos) == '[') {
            fx = x1 + pos + padleft;
            fy = py;
            TextField field = new TextField();
            field.x = fx + 1;
            field.y = fy;
            String text = ln.substring(pos+1, ln.indexOf(']', pos));
            String trim = text.trim();
            field.setText(trim);
            field.cx = trim.length();
            field.width = text.length();
            fields.add(field);
          }
        }
        ln = ln.replaceAll("\\[", makeBackColor(field_clr) + "[");
        ln = ln.replaceAll("\\]", "]" + makeBackColor(back_clr));
      }
      if (ln.indexOf('<') != -1) {
        //button(s)
        for(int pos=0;pos<len;pos++) {
          if (ln.charAt(pos) == '<') {
            fx = x1 + pos + padleft;
            fy = py;
            Button field = new Button();
            field.x = fx;
            field.y = fy;
            field.cx = 1;
            field.action = ln.substring(pos+1, ln.indexOf('>', pos));
            fields.add(field);
          }
        }
        ln = ln.replaceAll("<", makeBackColor(field_clr) + "<");
        ln = ln.replaceAll(">", ">" + makeBackColor(back_clr));
      }
      if (ln.indexOf("{list}") != -1) {
        for(int pos=0;pos<len;pos++) {
          if (ln.substring(pos).startsWith("{list}")) {
            fx = x1 + pos + padleft;
            fy = py;
            List field = new List();
            field.x = fx;
            field.y = fy;
            field.cx = 0;
            field.cy = 0;
            field.dx = 1;
            field.dy = 1;
            fields.add(field);
          }
        }
        ln = ln.replaceAll("\\{list\\}", "");
      }
      if (ln.indexOf("{checkbox}") != -1) {
        for(int pos=0;pos<len;pos++) {
          if (ln.substring(pos).startsWith("{checkbox}")) {
            fx = x1 + pos + padleft;
            fy = py;
            CheckBox field = new CheckBox();
            field.x = fx + 1;
            field.y = fy;
            field.cx = 0;
            field.cy = 0;
            field.dx = 0;
            field.dy = 0;
            fields.add(field);
          }
        }
        ln = ln.replaceAll("\\{checkbox\\}", "[ ]");
      }
      System.out.print(repeat(padleft, ' ') + ln + repeat(padright, ' '));
    }
    if (fields.size() != 0) {
      //move to first field
      Field field = fields.get(0);
      field.gotoCurrentPos();
    }
    return fields.toArray(new Field[0]);
  }

  public static void drawList(int x, int y, int w, int h, String list[], int start, int selected) {
    setDialogColor();
    drawBox1(x,y,w,h);
    if (list == null) list = new String[0];
    int idx = start;
    int x1 = x + 1;
    int y1 = y + 1;
    int y2 = y + h - 2;
    int backclr = back_clr;
    int newclr = -1;
    for(int py = y1;py <= y2;py++) {
      String ln;
      if (idx < list.length) ln = list[idx]; else ln = "";
      gotoPos(x1, py);
      if (idx == selected) {
        newclr = field_clr;
      } else {
        newclr = back_clr;
      }
      if (newclr != backclr) {
        backclr = newclr;
        setBackColor(newclr);
      }
      System.out.print(pad(ln, w-2));
      idx++;
    }
  }

  public void drawMenu(int x, int y, String opts[], int selected) {
    int width = 0;
    int height = opts.length + 2;
    for(int a=0;a<opts.length;a++) {
      if (opts[a].length() > width) {
        width = opts[a].length();
      }
    }
    String div = repeat(width, '-');
    String format = "%-" + width + "s";
    width += 2;
    setForeColor(0x000000);
    setBackColor(0xffffff);
    drawBox1(x, y, width, height);
    x++;
    y++;
    int sely = -1;
    for(int a=0;a<opts.length;a++) {
      gotoPos(x, y);
      if (selected == a) {
        setForeColor(0xffffff);
        setBackColor(0x000000);
        sely = y;
      }
      if (opts[a].equals("-")) {
        System.out.print(div);
      } else {
        System.out.print(String.format(format, opts[a]));
      }
      if (selected == a) {
        setForeColor(0x000000);
        setBackColor(0xffffff);
      }
      y++;
    }
    gotoPos(x, sely);
  }

  public static String makeForeColor(int rgb) {
    int r = (rgb & 0xff0000) >> 16;
    int g = (rgb & 0xff00) >> 8;
    int b = (rgb & 0xff);
    return ESC + "[38;2;" + r + ";" + g + ";" + b + "m";
  }

  public static void setForeColor(int rgb) {
    System.out.print(makeForeColor(rgb));
  }

  public static String makeBackColor(int rgb) {
    int r = (rgb & 0xff0000) >> 16;
    int g = (rgb & 0xff00) >> 8;
    int b = (rgb & 0xff);
    return ESC + "[48;2;" + r + ";" + g + ";" + b + "m";
  }

  public static void setBackColor(int rgb) {
    System.out.print(makeBackColor(rgb));
  }

  /** Draw vertical scroll bar.
   * barSize and barPos are in percentage.
   */
  public void scrollVBar(int x, int y, int height, int barSize, int barPos) {
    if (barPos == 0) barPos = 1;
    if (barSize == 0) barSize = 1;
    if (barPos == 100) barPos = 99;
    int bar1 = barPos * height / 100;
    int bar2 = bar1 + (barSize * height / 100);
    setForeColor(0xffffff);
    setBackColor(0x000000);
    for(int dy = 0; dy < height; dy++) {
      int pos = dy;
      gotoPos(x, y + dy);
      if (pos >= bar1 && pos <= bar2) {
        System.out.print(ASCII8.convert(219));
      } else {
        System.out.print(ASCII8.convert(176));
      }
    }
  }

  /** Draw horizontal scroll bar.
   * barSize and barPos are in percentage.
   */
  public void scrollHBar(int x, int y, int width, int barSize, int barPos) {
    if (barPos == 0) barPos = 1;
    if (barSize == 0) barSize = 1;
    if (barPos == 100) barPos = 99;
    int bar1 = barPos * width / 100;
    int bar2 = bar1 + (barSize * width / 100);
    setForeColor(0xffffff);
    setBackColor(0x000000);
    gotoPos(x, y);
    for(int dx = 0; dx < width; dx++) {
      int pos = dx;
      if (pos >= bar1 && pos <= bar2) {
        System.out.print(ASCII8.convert(219));
      } else {
        System.out.print(ASCII8.convert(176));
      }
    }
  }

  private boolean hasDigit(int nums[], int digit) {
    for(int a=0;a<nums.length;a++) {
      if (nums[a] == digit) return true;
    }
    return false;
  }

  public void process() {
    char ch = readConsole();
    if (ch == 0) return;
    buffer.append(ch);
    int keyCode = 0;
    int keyMods = 0;
    int bufsize = buffer.length();
    if (buffer.charAt(0) != ESC) {
      keyEvents.keyTyped(buffer.charAt(0));
      buffer.setLength(0);
      return;
    }
    if (bufsize < 2) return;
    if (buffer.charAt(1) == '[') {
      if (bufsize < 3) return;
      char code = buffer.charAt(bufsize-1);
      if (code != '~' && !Character.isAlphabetic(code)) return;
      //decode numbers
      int numc = 0;
      if (Character.isDigit(buffer.charAt(2))) {
        numc++;
        for(int p=2;p<bufsize;p++) {
          if (buffer.charAt(p) == ';') numc++;
        }
      }
      int nums[] = new int[numc];
      int pos = 2;
      for(int num=0;num<numc;num++) {
        int numpos = pos;
        int numlen = 0;
        while (numpos < bufsize && Character.isDigit(buffer.charAt(numpos))) {
          numlen++;
          numpos++;
        }
        nums[num] = Integer.valueOf(buffer.substring(pos, pos + numlen));
        pos = numpos;
        pos++;  //skip ;
      }
      if (hasDigit(nums, 2)) keyMods = KeyEvent.SHIFT_MASK;
      if (hasDigit(nums, 3)) keyMods = KeyEvent.ALT_MASK;
      if (hasDigit(nums, 4)) keyMods = KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK;
      if (hasDigit(nums, 5)) keyMods = KeyEvent.CTRL_MASK;
      if (hasDigit(nums, 6)) keyMods = KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK;;
      if (hasDigit(nums, 7)) keyMods = KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK;
      if (hasDigit(nums, 8)) keyMods = KeyEvent.CTRL_MASK | KeyEvent.ALT_MASK | KeyEvent.SHIFT_MASK;;
      switch (code) {
        case 'A':  //up
          keyCode = KeyEvent.VK_UP;
          break;
        case 'B':  //down
          keyCode = KeyEvent.VK_DOWN;
          break;
        case 'C':  //right
          keyCode = KeyEvent.VK_RIGHT;
          break;
        case 'D':  //left
          keyCode = KeyEvent.VK_LEFT;
          break;
        case '~':  //f keys
          if (numc == 0) break;
          if (nums[0] >= 25) keyMods = KeyEvent.SHIFT_MASK; else keyMods = 0;
          switch (nums[0]) {
            case 1: keyCode = KeyEvent.VK_ESCAPE; break;  //custom
            case 2: keyCode = KeyEvent.VK_INSERT; break;
            case 3: keyCode = KeyEvent.VK_DELETE; break;
            case 5: keyCode = KeyEvent.VK_PAGE_UP; break;
            case 6: keyCode = KeyEvent.VK_PAGE_DOWN; break;
            case 15: keyCode = KeyEvent.VK_F5; break;
            case 16: keyCode = KeyEvent.VK_X; break;
            case 17: keyCode = KeyEvent.VK_F6; break;
            case 18: keyCode = KeyEvent.VK_F7; break;
            case 19: keyCode = KeyEvent.VK_F8; break;
            case 20: keyCode = KeyEvent.VK_F9; break;
            case 21: keyCode = KeyEvent.VK_F10; break;
            case 22: keyCode = KeyEvent.VK_X; break;
            case 23: keyCode = KeyEvent.VK_F11; break;
            case 24: keyCode = KeyEvent.VK_F12; break;
            //shift f keys
            case 25: keyCode = KeyEvent.VK_F1; break;
            case 26: keyCode = KeyEvent.VK_F2; break;
            case 27: keyCode = KeyEvent.VK_X; break;
            case 28: keyCode = KeyEvent.VK_F3; break;
            case 29: keyCode = KeyEvent.VK_F4; break;
            case 30: keyCode = KeyEvent.VK_X; break;
            case 31: keyCode = KeyEvent.VK_F5; break;
            case 32: keyCode = KeyEvent.VK_F6; break;
            case 33: keyCode = KeyEvent.VK_F7; break;
            case 34: keyCode = KeyEvent.VK_F8; break;
            default:
              //unknown code
              buffer.setLength(0);
              return;
          }
          break;
        case 'H':  //home
          keyCode = KeyEvent.VK_HOME;
          break;
        case 'F':  //end
          keyCode = KeyEvent.VK_END;
          break;
        case 'P':
          keyCode = KeyEvent.VK_F1;
          break;
        case 'Q':
          keyCode = KeyEvent.VK_F2;
          break;
        case 'R':
          keyCode = KeyEvent.VK_F3;
          break;
        case 'S':
          keyCode = KeyEvent.VK_F4;
          break;
        default:
          //unknown code
          buffer.setLength(0);
          return;
      }
    } else if (buffer.charAt(1) == 'O') {
      //NOTE : If user presses ALT+SHIFT+O they will get trapped in this switch statement until they press another key
      if (bufsize > 2) {
        switch (buffer.charAt(2)) {
          case 'P':
            keyCode = KeyEvent.VK_F1;
            break;
          case 'Q':
            keyCode = KeyEvent.VK_F2;
            break;
          case 'R':
            keyCode = KeyEvent.VK_F3;
            break;
          case 'S':
            keyCode = KeyEvent.VK_F4;
            break;
          default:
            //unknown code
            buffer.setLength(0);
            return;
        }
      }
    } else {
      char code = buffer.charAt(1);
      if (Character.isAlphabetic(code)) {
        keyMods = KeyEvent.ALT_MASK;
        keyCode = code;
      } else {
        //unknown code
        buffer.setLength(0);
        return;
      }
    }
    if (keyCode != 0) {
      keyEvents.keyPressed(keyCode, keyMods);
      buffer.setLength(0);
    }
  }
}

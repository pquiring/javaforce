/**
 * ANSI.java
 *
 * Created on August 3, 2007, 8:37 PM
 *
 * @author pquiring
 */

package javaforce.ansi.client;

import java.awt.event.KeyEvent;
import java.awt.Color;

import javaforce.*;
import static javaforce.Telnet.*;

public class ANSI {

  public static boolean debug = false;

  public ANSI(Screen screen, boolean telnet) {
    orgForeColor = new Color(screen.getForeColor());
    orgBackColor = new Color(screen.getBackColor());
  }

  public final static char ESC = 0x1b;

  public final static int[][] clrs = {
    //Black  ,Red     ,Green   ,Yellow  ,Blue    ,Magenta ,Cyan    ,White
    {0x000000,0x880000,0x008800,0x888800,0x000088,0x880088,0x008888,0x888888},  //low
    {0x444444,0xff0000,0x00ff00,0xffff00,0x0000ff,0xff00ff,0x00ffff,0xffffff}  //high
  };

  public boolean altfnt = false;  //use alternate font
  public boolean keypadMode = false;
  public char altcode = '[';  //prefix some key codes with this
  public int high = 0;   //high intensity color (0/1)
  private int savedx = -1, savedy = -1;

  private Color orgForeColor, orgBackColor;
  private boolean telnet;

  public int numc;
  public int[] nums = new int[16];

  public char encodeChar(char ch) {
    if (!altfnt) {
      if (ch > 127 && ch < 256) ch = ASCII8.convert(ch);
      return ch;
    } else {
      char ret = altfntchars[ch];
      if (ret == 0) ret = ch;
      if (ret > 127 && ret < 256) ret = ASCII8.convert(ret);
      return ret;
    }
  }

  public char[] encodeString(char[] buf, int buflen) {
    char[] ret = new char[buflen];
    for(int a=0;a<buflen;a++) ret[a] = encodeChar(buf[a]);
    return ret;
  }

  public void keyPressed(int keyCode, int keyMods, Screen screen) {
    //an easy way to find codes is to run "xterm sh" and press keys - "sh" doesn't understand ANSI and echo'es them back
    String str = null;
    if (keyMods == KeyEvent.CTRL_DOWN_MASK) {
      if ((keyCode >= KeyEvent.VK_A) && (keyCode <= KeyEvent.VK_Z)) {
        str = "" + (char)(keyCode - KeyEvent.VK_A + 1);
      }
      switch (keyCode) {
        case KeyEvent.VK_UP:   str = "" + ESC + "" + altcode + "1;5A"; break;
        case KeyEvent.VK_DOWN: str = "" + ESC + "" + altcode + "1;5B"; break;
        case KeyEvent.VK_RIGHT:str = "" + ESC + "" + altcode + "1;5C"; break;
        case KeyEvent.VK_LEFT: str = "" + ESC + "" + altcode + "1;5D"; break;
      }
    }
    if (keyMods == KeyEvent.ALT_DOWN_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_UP:   str = "" + ESC + "" + altcode + "1;3A"; break;
        case KeyEvent.VK_DOWN: str = "" + ESC + "" + altcode + "1;3B"; break;
        case KeyEvent.VK_RIGHT:str = "" + ESC + "" + altcode + "1;3C"; break;
        case KeyEvent.VK_LEFT: str = "" + ESC + "" + altcode + "1;3D"; break;
      }
    }
    if (keyMods == KeyEvent.SHIFT_DOWN_MASK) {
      switch (keyCode) {
        case KeyEvent.VK_UP:   str = "" + ESC + "" + altcode + "1;2A"; break;
        case KeyEvent.VK_DOWN: str = "" + ESC + "" + altcode + "1;2B"; break;
        case KeyEvent.VK_RIGHT:str = "" + ESC + "" + altcode + "1;2C"; break;
        case KeyEvent.VK_LEFT: str = "" + ESC + "" + altcode + "1;2D"; break;

        case KeyEvent.VK_F1: str = "" + ESC + "[25~"; break;  //KEY_F13
        case KeyEvent.VK_F2: str = "" + ESC + "[26~"; break;  //KEY_F14
        case KeyEvent.VK_F3: str = "" + ESC + "[28~"; break;  //KEY_F15
        case KeyEvent.VK_F4: str = "" + ESC + "[29~"; break;  //KEY_F16
        case KeyEvent.VK_F5: str = "" + ESC + "[31~"; break;  //KEY_F17
        case KeyEvent.VK_F6: str = "" + ESC + "[32~"; break;  //KEY_F18
        case KeyEvent.VK_F7: str = "" + ESC + "[33~"; break;  //KEY_F19
        case KeyEvent.VK_F8: str = "" + ESC + "[34~"; break;  //KEY_F20
      }
    }
    if (str != null) screen.output(str.toCharArray());
    if (keyMods != 0) return;
    switch (keyCode) {
      case KeyEvent.VK_UP:   str = "" + ESC + "" + altcode + "A"; break;
      case KeyEvent.VK_DOWN: str = "" + ESC + "" + altcode + "B"; break;
      case KeyEvent.VK_RIGHT:str = "" + ESC + "" + altcode + "C"; break;
      case KeyEvent.VK_LEFT: str = "" + ESC + "" + altcode + "D"; break;

      case KeyEvent.VK_HOME: str = "" + ESC + "[H"; break;
      case KeyEvent.VK_END: str = "" + ESC + "[F"; break;

      case KeyEvent.VK_F1:   str = "" + ESC + altcode + "P"; break;
      case KeyEvent.VK_F2:   str = "" + ESC + altcode + "Q"; break;
      case KeyEvent.VK_F3:   str = "" + ESC + altcode + "R"; break;
      case KeyEvent.VK_F4:   str = "" + ESC + altcode + "S"; break;
      case KeyEvent.VK_F5:
        if (telnet)
          str = "" + Telnet.IAC + Telnet.BRK;  //BREAK
        else
          str = "" + ESC + "[15~";
        break;
      case KeyEvent.VK_F6:   str = "" + ESC + "[17~"; break;
      case KeyEvent.VK_F7:   str = "" + ESC + "[18~"; break;
      case KeyEvent.VK_F8:   str = "" + ESC + "[19~"; break;
      case KeyEvent.VK_F9:   str = "" + ESC + "[20~"; break;
      case KeyEvent.VK_F10:  str = "" + ESC + "[21~"; break;
      case KeyEvent.VK_F11:  str = "" + ESC + "[23~"; break;
      case KeyEvent.VK_F12:  str = "" + ESC + "[24~"; break;

      case KeyEvent.VK_DELETE:     str = "" + ESC + "[3~"; break;
      case KeyEvent.VK_PAGE_UP:    str = "" + ESC + "[5~"; break;  //PREV
      case KeyEvent.VK_PAGE_DOWN:  str = "" + ESC + "[6~"; break;  //NEXT

      case KeyEvent.VK_PAUSE: str = "" + Telnet.IAC + Telnet.BRK; break;  //BREAK
    }
    if (str != null) screen.output(str.toCharArray());
  }

  public boolean decode(char[] code, int codelen, Screen screen) {
    if (debug) JFLog.log("ANSI.decode:" + codelen);
    int x,y;
    x = screen.getx();
    y = screen.gety();
    if (codelen < 2) return false;
    switch (code[1]) {
      case 'H':
        //add tab stop at cursor position
        JFLog.log("ANSI:Not implemented:" + code[1]);
        return true;
      case 'M':  //move cursor up one (scroll down if needed)
        if (y <= screen.gety1())
          screen.scrollDown(1);
        else
          screen.gotoPos(x, y - 1);
        return true;
      case 'D':  //move cursor down one (scroll up if needed)
        if (y >= screen.gety2())
          screen.scrollUp(1);
        else
          screen.gotoPos(x, y + 1);
        return true;
      case '7':
        //save cursor pos
        savedx = x;
        savedy = y;
        return true;
      case '8':
        //restore cursor pos
        if (savedx == -1) return true;
        screen.gotoPos(savedx, savedy);
        return true;
      case '=':
        keypadMode = true;
        return true;
      case '>':
        keypadMode = false;
        return true;
      case '#':
      case '(':
      case '[':
      case ']': break;
      default:
        JFLog.log("ANSI:Unknown code:" + code[1]);
        return true;  //ignore unknown code
    }
    if (codelen < 3) return false;
    switch (code[1]) {
      case '#':
/*
        switch (code[2]) {
          case '3':  //large font?
          case '4':  //small font?
        }
*/
        break;
      case '(':
        switch (code[2]) {
          case 'A': altfnt = false; break;  //UK char set
          case 'B': altfnt = false; break;  //US char set
          case '0': altfnt = true; break;  //line drawing char set
          case '1': altfnt = false; break;  //alt ROM char set
          case '2': altfnt = false; break;  //alt ROM special char set
        }
        break;
      case ']':  //operating system command
        //OSC is currently not supported
        if (code[codelen-1] == 7) return true;
        if (code[codelen-2] == ESC && code[codelen-1] == '\\') return true;
        return false;
      case '[':  //control sequence introducer
        char cmd = code[codelen-1];
        if (!(
          (cmd >= 'A' && cmd <= 'Z') ||
          (cmd >= 'a' && cmd <= 'z') ||
          (cmd == '~') || (cmd == '@')))
        {
          return false;
        }
        if (code[2] == '?') {
          decodeNums(code, codelen, 3);
          switch (cmd) {
            case 'h':  //set private functions
              for(int a=0;a<numc;a++) {
                switch (nums[a]) {
                  case 1: altcode = 'O'; break;  //cursor key mode : control/app functions
                  case 7: screen.setAutoWrap(false); break;
                  case 12: break;  //TODO : start blinking cursor
                  case 25: break;  //TODO : show cursor
                  case 1002: break;  //???
                  case 1006: break;  //???
                  case 1049: break;  //???
                  case 2004: break;  //bracketed paste mode
                  default: JFLog.log("ANSI:Unknown [ ? h:" + nums[a]);
                }
              }
              return true;
            case 'l':  //reset private functions
              for(int a=0;a<numc;a++) {
                switch (nums[a]) {
                  case 1: altcode = '['; break;  //cursor key mode : ANSI control sequences
                  case 7: screen.setAutoWrap(true); break;
                  case 12: break;  //TODO : stop blinking cursor
                  case 25: break;  //TODO : hide cursor
                  case 1002: break;  //???
                  case 1006: break;  //???
                  case 1049: break;  //???
                  case 2004: break;  //bracketed paste mode
                  default: JFLog.log("ANSI:Unknown [ ? l:" + nums[a]);
                }
              }
              return true;
            case 's':
              //???
              break;
            case 'r':
              //???
              break;
            default:
              JFLog.log("ANSI:Unknown [ ? code:" + code[codelen-1]);
              //ignore unknown ? code
              return true;
          }
        }
        decodeNums(code, codelen, 2);
        switch (cmd) {
          case 'J':
            if (numc == 0) nums[0] = 0;
            switch (nums[0]) {
              case 0:
                //from cursor to end of screen
                for(int a=x;a<=screen.getsx();a++) screen.setChar(a, y, ' ');  //erase partial line
                for(int b=y+1;b<=screen.getsy();b++) for(int a=1;a<=screen.getsx();a++) screen.setChar(a, b, ' ');
                break;
              case 1:
                //from begining to cursor
                for(int a=1;a<x;a++) screen.setChar(a, y, ' ');  //erase partial line
                for(int b=1;b<y;b++) for(int a=1;a<=screen.getsx();a++) screen.setChar(a, b, ' ');
                break;
              case 2:
                //entire screen (reposition cursor to (1,1) too)
                screen.clrscr();
                break;
            }
            break;
          case 'K':
            if (numc == 0) nums[0] = 0;
            switch (nums[0]) {
              case 0:
                //erase from cursor to end of line
                for(int a=x;a<=screen.getsx();a++) screen.setChar(a, y, ' ');
                break;
              case 1:
                //from beginning of line to cursor
                for(int a=1;a<x;a++) screen.setChar(a, y, ' ');
                break;
              case 2:
                //whole line
                for(int a=1;a<=screen.getsx();a++) screen.setChar(a, y, ' ');
                break;
            }
            break;
          case 'H':
          case 'f':
            if (numc == 2 && nums[1] == 0) numc = 1;
            if (numc == 1 && nums[0] == 0) numc = 0;
            switch (numc) {
              case 2:
                screen.gotoPos(min(screen.getsx(),nums[1]),min(screen.getsy(),nums[0]));
                break;
              case 1:
                screen.gotoPos(1, min(screen.getsy(),nums[0]));
                break;
              case 0:
                screen.gotoPos(1,1);
                break;
            }
            break;
          case 'A': //up
            if (numc == 0) nums[0] = 1; if (y-nums[0]>1 ) screen.gotoPos(x,y-nums[0]); else screen.gotoPos(x,1);
            break;
          case 'B': //down
            if (numc == 0) nums[0] = 1; if (y+nums[0]<screen.getsy()) screen.gotoPos(x,y+nums[0]); else screen.gotoPos(x,screen.getsy());
            break;
          case 'C': //forward
            if (numc == 0) nums[0] = 1; if (x+nums[0]<screen.getsx()) screen.gotoPos(x+nums[0],y); else screen.gotoPos(screen.getsx(),y);
            break;
          case 'D': //backwards
            if (numc == 0) nums[0] = 1; if (x-nums[0]>1 ) screen.gotoPos(x-nums[0],y); else screen.gotoPos(1,y);
            break;
          case 'L': //insert lines (only if within margin area)
            if ((y < screen.gety1()) || (y > screen.gety2())) break;
            if (numc == 0) nums[0] = 1;
            int oy1 = screen.gety1();
            screen.sety1(y);
            screen.scrollDown(nums[0]);
            screen.sety1(oy1);
            break;
          case 'r':
            //define rows that scroll (margin)
            if (numc != 2) break;
            screen.sety1(nums[0]);
            screen.sety2(nums[1]);
            break;
          case 'm':
            //colour
            if (numc == 0) {numc = 1; nums[0] = 0;}
            for(int a=0;a<numc;a++) {
              if (nums[a] == 1) {high = 1; continue;}  //bold
              if (nums[a] == 2) {high = 0; continue;}
            }
            for(int a=0;a<numc;a++) {
              if (nums[a] == 0) {
                //normal
                high = 0;
                screen.setBlinker(false);
                screen.setReverse(false);
                screen.setForeColor(orgForeColor.getRGB());
                screen.setBackColor(orgBackColor.getRGB());
                continue;
              }
//              if (nums[a] == 4) {continue;}  //underline (not implemented)
              if (nums[a] == 5) {screen.setBlinker(true); continue;}
              if (nums[a] == 7) {screen.setReverse(true); continue;}  //reverse
//              if (nums[a] == 8) {continue;}  //invisible (not implemented) [vt300]
//              if (nums[a] == 24) {continue;}  //not underline (not implemented)
              if (nums[a] == 25) {screen.setBlinker(false); continue;}
              if (nums[a] == 27) {screen.setReverse(false); continue;}  //Positive (not inverse)
//              if (nums[a] == 28) {continue;}  //visible (not implemented) [vt300]
              if ((nums[a] >= 30) && (nums[a] <= 37)) {screen.setForeColor(clrs[high][nums[a]-30]); continue;}
              if (nums[a] == 38) {
                //foreColor = r;g;b
                if (numc > 3) {
                  int r = nums[numc-3];
                  int g = nums[numc-2];
                  int b = nums[numc-1];
                  screen.setForeColor(r << 16 | g << 8 | b);
                }
                break;
              }
              if (nums[a] == 39) {screen.setForeColor(orgForeColor.getRGB()); continue;}  //default (org)
              if ((nums[a] >= 40) && (nums[a] <= 47)) {screen.setBackColor(clrs[0][nums[a]-40]); continue;}
              if (nums[a] == 48) {
                //backColor = r;g;b
                if (numc > 3) {
                  int r = nums[numc-3];
                  int g = nums[numc-2];
                  int b = nums[numc-1];
                  screen.setBackColor(r << 16 | g << 8 | b);
                }
                break;
              }
              if (nums[a] == 49) {screen.setBackColor(orgBackColor.getRGB()); continue;}  //default (org)
            }
            break;
          case 'n':
            //query cursor position "ESC[row;colR"
            if ((numc != 1) || (nums[0] != 6)) break;
            String str = "" + ESC + '[' + screen.gety() + ';' + screen.getx() + 'R';
            screen.output(str.toCharArray());
            break;
          case 'P':
            //delete nums[0] chars at cursor
            if (numc == 0) {numc = 1; nums[0] = 1;}
            for(int cnt=0;cnt<nums[0];cnt++) screen.delete();
            break;
          case '@':
            //insert nums[0] chars at cursor
            if (numc == 0) {numc = 1; nums[0] = 1;}
            for(int cnt=0;cnt<nums[0];cnt++) screen.insert();
            break;
          case 's':
            //save cursor pos
            savedx = screen.getx();
            savedy = screen.gety();
            break;
          case 'u':
            //restore cursor pos
            if (savedx == -1) return true;
            screen.gotoPos(savedx, savedy);
            break;
          case 'g':  //not implemented
            //0 = clear tab stop at cursor location
            //3 = clear all tab stops
            break;
          case 'd':  //line position absolute (default = 1)
            if (numc == 0) nums[0] = 1;
            screen.gotoPos(x, nums[0]);
            break;
          case 'e':  //line position relative (default = row+1)
            if (numc == 0) nums[0] = 1;
            screen.gotoPos(x, y + nums[0]);
            break;
          case 'G':  //column position absolute (default = 1)
            if (numc == 0) nums[0] = 1;
            screen.gotoPos(nums[0], y);
            break;
          case 'X':  //delete 'x' chars
            if (numc == 0) nums[0] = 1;
            int cnt = nums[0];
            while (cnt > 0) {
              screen.setChar(x, y, ' ');
              x++;
              if (x == screen.getsx()+1) {
                x = 1;
                y++;
                if (y == screen.getsy()+1) break;
              }
              cnt--;
            }
            break;
          default:
            //ignore unknown code
            break;
        }
    }
    return true;
  }
  private void decodeNums(char[] code, int codelen, int start) {
    //decode #s encoded into code[]
    int c=start,s,e,t,m;
    numc = 0;
    code[codelen] = 'x';
    while (c < codelen) {
      while ((code[c]==';')||(code[c]==',')) {
        nums[numc++] = 0;
        c++;
      }
      if (!((code[c]>='0') && (code[c]<='9'))) break;
      s=c;
      while ((code[c]>='0') && (code[c]<='9')) {
        c++;
      }
      e=c;
      t=0;
      m=1;
      while (c!=s) {
        c--;
        t+=(code[c]-'0') * m;
        m*=10;
      }
      nums[numc++]=t;
      c=e;
      if ((code[c] == ';') || (code[c] == ',')) c++;
    }
  }
  private int min(int v1, int v2) {
    if (v1 < v2) return v1;
    return v2;
  }
  private int max(int v1, int v2) {
    if (v1 > v2) return v1;
    return v2;
  }
  private static char[] altfntchars = {
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,  //0
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,  //16
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,  //32
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,  //48
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,  //64
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,  //80
    0,177,0,0,0,0,0,0,0,0,217,191,218,192,0,0,  //96
    0,196,0,0,195,180,193,194,179,0,0,0,0,0,0,0,  //112
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,  //128
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
  };
}

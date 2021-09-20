/**
 * Created : Mar 27, 2012
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class Backend implements KeyEventDispatcher {
  public Display display;
  public ArrayList<String> formula = new ArrayList<String>();
  public int radix = 10;
  public int fidx = 0;  //formula index
  public int mode = 0;  //0=Basic 1=Scientific 2=Programmer
  public boolean inDialog = false;

  private boolean error = false;
  private boolean deg = true;  //degrees (else radians)

  public Backend() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
  }

  private boolean isNumber(int idx) {
    return formula.get(idx).startsWith("#");
  }

  private boolean isOperation(int idx) {
    char ch = formula.get(idx).charAt(0);
    if (ch == '#') return false;
    if (ch == '(') return false;
    if (ch == ')') return false;
    return true;
  }

  private boolean isOpen(int idx) {
    return formula.get(idx).charAt(0) == '(';
  }

  private boolean isClose(int idx) {
    return formula.get(idx).charAt(0) == ')';
  }

  private boolean eqOperation(int idx, String op) {
    return formula.get(idx).equals(op);
  }

  private long getLong(int idx) {
    String str = formula.get(idx);
    int stridx = str.indexOf(",");
    if (stridx == -1)
      return Long.valueOf(str.substring(1), radix);  //assume current radix
    else
      return Long.valueOf(str.substring(1, stridx), Integer.valueOf(str.substring(stridx+1)));
  }

  private double getDouble(int idx) {
    return Double.valueOf(formula.get(idx).substring(1));
  }

  private double toRadians(double value) {
    if (!deg) return value;
    return Math.toRadians(value);
  }

  private void doEquals() {
    //calc formula
    boolean cont;
    double d1, d2, dr;
    long l1, l2, lr;
    System.out.println("doEquals");
    for(int a=0;a<formula.size();a++) {
      System.out.println(formula.get(a));
      if (formula.get(a).equals("pi")) {
        formula.set(a, "#" + Math.PI);
      }
    }
    while (formula.size() > 1) {
      int size = formula.size();
      cont = false;
      //look for 2 argument type functions (functions are backwards)
      for(int a=0;a<size-1;a++) {
        if (isNumber(a) && eqOperation(a+1, "x^2")) {
          formula.set(a, "#" + (Math.pow(getDouble(a), 2.0)));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "x^3")) {
          formula.set(a, "#" + (Math.pow(getDouble(a), 3.0)));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "sqrt")) {
          formula.set(a, "#" + (Math.sqrt(getDouble(a))));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "sin")) {
          formula.set(a, "#" + (Math.sin(toRadians(getDouble(a)))));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "cos")) {
          formula.set(a, "#" + (Math.cos(toRadians(getDouble(a)))));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "tan")) {
          formula.set(a, "#" + (Math.tan(toRadians(getDouble(a)))));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "log")) {  //or lg()
          formula.set(a, "#" + (Math.log10(toRadians(getDouble(a)))));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "ln")) {
          formula.set(a, "#" + (Math.log(toRadians(getDouble(a)))));
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "1/x")) {
          formula.set(a, "#" + (1.0 / getDouble(a)));
          formula.remove(a+1);
          cont = true;
          break;
        }
      }
      if (cont) continue;
      //look for 3 argument type operations in order of precedence
      for(int a=0;a<size-2;a++) {
        if (isNumber(a) && eqOperation(a+1, "x^y") && isNumber(a+2)) {
          formula.set(a, "#" + (Math.pow(getDouble(a), getDouble(a+2))));
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "x") && isNumber(a+2)) {
          if (mode == 2) {
            formula.set(a, "#" + (getLong(a) * getLong(a+2)) + ",10");
          } else {
            formula.set(a, "#" + (getDouble(a) * getDouble(a+2)));
          }
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "\u00f7") && isNumber(a+2)) {
          if (mode == 2) {
            formula.set(a, "#" + (getLong(a) / getLong(a+2)) + ",10");
          } else {
            formula.set(a, "#" + (getDouble(a) / getDouble(a+2)));
          }
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "+") && isNumber(a+2)) {
          if (mode == 2) {
            formula.set(a, "#" + (getLong(a) + getLong(a+2)) + ",10");
          } else {
            formula.set(a, "#" + (getDouble(a) + getDouble(a+2)));
          }
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "-") && isNumber(a+2)) {
          if (mode == 2) {
            formula.set(a, "#" + (getLong(a) - getLong(a+2)) + ",10");
          } else {
            formula.set(a, "#" + (getDouble(a) - getDouble(a+2)));
          }
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "AND") && isNumber(a+2)) {
          formula.set(a, "#" + (getLong(a) & getLong(a+2)) + ",10");
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "OR") && isNumber(a+2)) {
          formula.set(a, "#" + (getLong(a) | getLong(a+2)) + ",10");
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "XOR") && isNumber(a+2)) {
          formula.set(a, "#" + (getLong(a) ^ getLong(a+2)) + ",10");
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isNumber(a) && eqOperation(a+1, "MOD") && isNumber(a+2)) {
          formula.set(a, "#" + (getLong(a) % getLong(a+2)) + ",10");
          formula.remove(a+1);
          formula.remove(a+1);
          cont = true;
          break;
        }
        if (isOpen(a) && isNumber(a+1) && isClose(a+2)) {
          formula.remove(a);
          formula.remove(a+1);
          cont = true;
          break;
        }
      }
      if (cont) continue;
      //ERROR - formula not solveable
      formula.clear();
      display.setDisplay("Error:Bad formula");
      error = true;
      System.out.println("Error(1)");
      return;
    }
    if (formula.isEmpty()) formula.add("#0");
    if (!isNumber(0)) {
      formula.clear();
      display.setDisplay("Error:Bad formula");
      error = true;
      System.out.println("Error(2)");
      return;
    }
    String str = formula.get(0);
    if (mode == 2) {
      if (radix != 10) {
        str = "#" + Long.toString(getLong(0), radix).toUpperCase() + "," + radix;
      }
    } else {
      if (str.endsWith(".0")) {
        str = str.substring(0, str.length()-2);
      }
    }
    formula.set(0, str);
    System.out.println("=" + formula.get(0));
    display.setDisplay(formula.get(0).substring(1));
  }

  private void addDigit2(char digit) {
    if ((fidx == 1) && (isNumber(0))) {
      formula.remove(0);  //remove last result
      fidx--;
    }
    formula.set(fidx, formula.get(fidx) + digit);
  }

  public void addDigit(char digit) {
    if (error) return;
    if ((digit >= 'A') && (digit <= 'F') && mode != 2) return;  //keyboard
    if (mode == 2) {
      int value = 0;
      if ((digit >= 'A') && (digit <= 'F')) value = digit - 'A' + 10;
      if ((digit >= '0') && (digit <= '9')) value = digit - '0';
      if (value >= radix) return;
    }
    String str = formula.get(fidx);
    if (str.length() == 0) addDigit2('#');
    if (digit == '.') {
      if (mode == 2) return;  //keyboard
      if (str.indexOf(".") != -1) return;
      addDigit2('.');
    } else {
      addDigit2(digit);
    }
    display.setDisplay(formula.get(fidx).substring(1));
  }

  public void endEntry() {
    if (isNumber(fidx)) {
      String str = formula.get(fidx);
      if ((mode == 2) && (str.charAt(0) == '#') && (str.indexOf(",") == -1)) {
        //add radix if needed
        formula.set(fidx, formula.get(fidx) + "," + radix);
      }
      fidx++;
      formula.add("");
    }
  }

  public void addOperation(String op) {
    if (op.equals("AC")) {doAllClear(); return;}
    if (error) return;
    if ((fidx == 0) && (op.equals("-")) && formula.get(fidx).length() == 0) {
      op = "+/-";
    }
    if (op.equals("+/-")) {
      String str = formula.get(fidx);
      if (str.length() == 0) {
        addDigit2('#');
        str = "#";
      }
      if (str.length() == 1) {
        addDigit2('-');
        display.setDisplay(formula.get(fidx).substring(1));
        return;
      }
      if (str.length() > 1 && str.charAt(1) == '-') {
        str = "#" + str.substring(2);
        formula.set(fidx, str);
        display.setDisplay(formula.get(fidx).substring(1));
      } else {
        str = "#-" + str.substring(1);
        formula.set(fidx, str);
        display.setDisplay(formula.get(fidx).substring(1));
      }
      return;
    }
    endEntry();
    if (op.equals("=")) {
      try {
        if (formula.get(fidx).equals("")) formula.remove(fidx);
        doEquals();
        fidx = 1;
        formula.add("");
      } catch (Exception e) {
        System.out.println("" + e);
        e.printStackTrace();
        error = true;
        display.setDisplay("Error:" + e);
      }
    } else {
      formula.set(fidx, op);
      display.setDisplay(op);
      fidx++;
      formula.add("");
    }
  }

  public void doAllClear() {
    error = false;
    formula.clear();
    formula.add("");
    display.setDisplay("");
    fidx = 0;
  }

  public void doErase() {
    String txt = formula.get(fidx);
    int len = txt.length();
    if (len < 2) return;
    if (txt.charAt(0) != '#') return;
    txt = txt.substring(0, len-1);
    formula.set(fidx, txt);
    display.setDisplay(txt.substring(1));
  }

  public void paste() {
    JTextField tf = new JTextField();
    tf.paste();
    char ca[] = tf.getText().toCharArray();
    for(int a=0;a<ca.length;a++) {
      addDigit(ca[a]);
    }
  }

  public void setRadix(int newRadix) {
    if ((formula.size() == 2) && (formula.get(1).length() == 0)) formula.remove(1);
    if ((formula.size() == 1) && (formula.get(0).length() == 0)) formula.remove(0);
    if ((formula.size() != 1) || !isNumber(0)) {
      doAllClear();
    } else {
      formula.set(0, "#" + Long.toString(getLong(0), newRadix).toUpperCase() + "," + newRadix);
      display.setDisplay(formula.get(0).substring(1));
      fidx = 1;
      formula.add("");
    }
    radix = newRadix;
  }

  public void setDeg(boolean deg) {
    this.deg = deg;
  }

  public void about() {
    inDialog = true;
    JFAWT.showMessage("About", "jfcalc/" + CalculatorApp.version + "\n");
    inDialog = false;
  }

  public boolean dispatchKeyEvent(KeyEvent e) {
    if (inDialog) return false;
    int id = e.getID();
    char ch = e.getKeyChar();
    int cc = e.getKeyCode();
    int mod = e.getModifiers();
//JFLog.log("key:" + ch + "," + cc + "," + mod + ":" + inDialog);
    if (mod == KeyEvent.SHIFT_MASK) {
      switch (id) {
        case KeyEvent.KEY_TYPED:
          switch (ch) {
            case '+': addOperation("+"); break;
            case '(': addOperation("("); break;
            case ')': addOperation(")"); break;
            case '*': addOperation("x"); break;
          }
      }
    }
    if (mod != 0) return false;
    switch (id) {
      case KeyEvent.KEY_TYPED:
        switch (ch) {
          case 'a': addDigit('A'); break;
          case 'b': addDigit('B'); break;
          case 'c': addDigit('C'); break;
          case 'd': addDigit('D'); break;
          case 'e': addDigit('E'); break;
          case 'f': addDigit('F'); break;
          case '0': addDigit('0'); break;
          case '1': addDigit('1'); break;
          case '2': addDigit('2'); break;
          case '3': addDigit('3'); break;
          case '4': addDigit('4'); break;
          case '5': addDigit('5'); break;
          case '6': addDigit('6'); break;
          case '7': addDigit('7'); break;
          case '8': addDigit('8'); break;
          case '9': addDigit('9'); break;
          case '.': addDigit('.'); break;
          case '*': addOperation("x"); break;
          case '/': addOperation("\u00f7"); break;
          case '-': addOperation("-"); break;
          case '=': addOperation("="); break;
          case '+': addOperation("+"); break;  //keypad
        }
        break;
      case KeyEvent.KEY_PRESSED:
        switch (cc) {
          case KeyEvent.VK_ENTER: addOperation("="); break;
          case KeyEvent.VK_F1: about(); break;
          case KeyEvent.VK_F5: if (mode == 2) display.setRadix(16); break;
          case KeyEvent.VK_F6: if (mode == 2) display.setRadix(10); break;
          case KeyEvent.VK_F7: if (mode == 2) display.setRadix(8); break;
          case KeyEvent.VK_F8: if (mode == 2) display.setRadix(2); break;
          case KeyEvent.VK_ESCAPE: doAllClear(); break;
          case KeyEvent.VK_BACK_SPACE: doErase(); break;
        }
        break;
      case KeyEvent.KEY_RELEASED:
        break;
    }
    return false;
  }
}

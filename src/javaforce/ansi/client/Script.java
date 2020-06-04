/**
 * Scripting support for JFTerm.<br>
 * Language syntax:<br>
 *   Type "text"<br>
 *   Wait "text"<br>
 *   Sleep seconds<br>
 *   HitKey VK_key<br>
 *<br>
 * Example:<br>
 *   Wait "&gt;"<br>
 *   Type "username"<br>
 *   HitKey VK_ENTER<br>
 *   Wait "&gt;"<br>
 *   Type "password"<br>
 *   HitKey VK_ENTER<br>
 *<br>
 * Some VK_...: VK_ENTER, VK_TAB, VK_SPACE, etc.<br>
 */

package javaforce.ansi.client;

import javaforce.*;
import java.lang.reflect.*;
import java.io.*;
import javax.swing.*;

public class Script {
  private Script() {}  //private ctor - must call load()

  private String[] script;

  private int pos = 0;
  private int chpos = 0;

  private boolean waiting = false;
  private int waitsiz = 0;
  private char[] buf;
  private int buflen = 0;
  private String waitstr = null;

  private void nextLine() {
    pos++;
    chpos = 0;
  }

  public void input(char ch, Buffer buffer) {
    if (!waiting) return;
    if (buflen == waitsiz) {
      for(int a=0;a<buflen-1;a++) buf[a] = buf[a+1];
      buf[buflen-1] = ch;
    } else {
      buf[buflen++] = ch;
    }
    if (buflen != waitsiz) return;
    if (waitstr.equalsIgnoreCase(new String(buf))) {
      waiting = false;
      process(buffer);
    }
  }

  /**
   * Load and execute a script (*.scr)
   */
  public static Script load(BufferViewer buffer) {

    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.setMultiSelectionEnabled(false);
    chooser.setCurrentDirectory(new File(JF.getCurrentPath()));
    if (chooser.showOpenDialog(buffer) != JFileChooser.APPROVE_OPTION) return null;

    Script ret = new Script();

    try {
      FileInputStream fis = new FileInputStream(chooser.getSelectedFile().getAbsolutePath());
      int fs = fis.available();
      if (fs < 1) return null;
      byte[] data = new byte[fs];
      if (fis.read(data) != fs) throw new Exception();
      String str = new String(data);
      str = str.replaceAll("\r", "");  //get rid of Windows junk
      ret.script = str.split("\n");
    } catch (Exception e) {
      JFAWT.showError("File Error", "Unable to open script file");
      return null;
    }

    if (ret.process(buffer.buffer)) return null;
    return ret;
  }

  private boolean isWS(char ch) {
    if (ch == ' ') return true;
    if (ch == 9) return true;
    return false;
  }

  private String nextToken() {
    String ret = "";
    char ch;
    boolean quote = false;

    if (pos >= script.length) return "";
    if (chpos >= script[pos].length()) {pos++; chpos = 0;}
    if (pos >= script.length) return "";

    try {
      //skip leading whitespace
      while (isWS(script[pos].charAt(chpos))) {
        chpos++;
        if (chpos >= script[pos].length()) {pos++; chpos = 0;}
        if (pos >= script.length) return "";
      }
      while (true) {
        ch = script[pos].charAt(chpos++);
        if (!quote) {
          if (ch == '#') throw new Exception();  //skip rest of line (comment)
          if (isWS(ch)) return ret;
          if (ch == '\"') {quote = true; continue;}
        }
        if (ch == '\"') return ret;
        ret += ch;
      }
    } catch (Exception e) {
      pos++;
      chpos=0;
    }
    return ret;
  }

  public synchronized boolean process(Buffer buffer) {
    String cmd = "", arg  = "";
    if (waiting) return false;
    if (pos >= script.length) return true;
    while (true) {
      cmd = "";
      while (cmd.length() == 0) {
        if (pos >= script.length) return true;
        cmd = nextToken();
      }
      arg = "";
      while (arg.length() == 0) {
        if (pos >= script.length) return true;
        arg = nextToken();
      }
      if (cmd.equalsIgnoreCase("type")) {
        buffer.output(arg.toCharArray());
        continue;
      }
      if (cmd.equalsIgnoreCase("wait")) {
        waitstr = arg;
        waitsiz = waitstr.length();
        buf = new char[waitsiz];
        buflen = 0;
        waiting = true;
        return false;
      }
      if (cmd.equalsIgnoreCase("sleep")) {
        JF.sleep(JF.atoi(arg) * 1000);
      }
      if (cmd.equalsIgnoreCase("hitkey")) {
        try {
          java.awt.event.KeyEvent ke = new java.awt.event.KeyEvent(buffer.viewer, 0, 0, 0, 0, ' ');
          Class<?> c = ke.getClass();
          Field f = c.getField(arg);
          int keyCode = f.getInt(ke);
          switch (keyCode) {
            //case VK_...: buffer.keyPressed(new java.awt.event.KeyEvent(buffer, 0, 0, 0, keyCode, (char)keyCode));
            default: buffer.output(new char[] {(char)keyCode});
          }
        } catch (Exception e) {
          JFAWT.showError("Bad Script", "HitKey : " + arg);
        }
      }
    }
  }
};

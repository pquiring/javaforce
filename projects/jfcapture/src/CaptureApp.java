/**
 * Created : Apr 10, 2012
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import javax.swing.*;

import javaforce.*;

public class CaptureApp {
  public static void main(String args[]) {
    //exec scrot
    ArrayList<String> cmd = new ArrayList<String>();
    try {
      final File tmpFile = File.createTempFile("cap", ".png");
      cmd.add("scrot");
      cmd.add(tmpFile.getAbsolutePath());
      for(int a=0;a<args.length;a++) {
        cmd.add(args[a]);
      }
      Runtime.getRuntime().exec(cmd.toArray(new String[0]));
      if (!tmpFile.exists()) {
        JF.showError("Error", "Capture failed!");
        return;
      }
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          JFrame frame = new MainFrame(tmpFile.getAbsolutePath());
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

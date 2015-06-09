/** Applet for jPhoneLite. */

import java.io.*;
import java.net.*;

import javaforce.*;

public class PhoneApplet extends javax.swing.JApplet implements WindowController {
  public void init() {
    String userid = getParameter("userid");
    System.out.println("userid = " + userid);  //test
    if (userid != null) setupConfig(userid);
    String isJavaScript = getParameter("isJavaScript");
    Settings.isJavaScript = ((isJavaScript != null) && (isJavaScript.equals("true")));
    phone = new PhonePanel(this, true);
    setContentPane(phone);
    JF.loadCerts(getClass().getResourceAsStream("javaforce.crt")
      , getClass().getResourceAsStream("jphonelite.crt"), "jphonelite.sourceforge.net");
  }
  public void destroy() {
    Settings.saveSettings();
    System.exit(0);
  }
  public void setPanelSize() {}
  public void setPanelVisible() {}
  public void setPanelAlwaysOnTop(boolean state) {}
  public void setupConfig(String userid) {
    System.out.println("Loading config for userid : " + userid);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
        new URL(getCodeBase().toString() + "/jphonelite-getconfig.php?userid=" + userid).openStream()));
      FileOutputStream fos = new FileOutputStream(JF.getUserPath() + "/.jphone.xml");
      String line;
      do {
        line = reader.readLine();
        if (line == null) break;
        fos.write(line.getBytes());
      } while (true);
      fos.close();
    } catch (Exception e) {
      System.out.println("Error loading config : " + e);
    }
  }
  public void setPosition() {}
  private PhonePanel phone;
  public BasePhone getBasePhone() {return phone;}
}

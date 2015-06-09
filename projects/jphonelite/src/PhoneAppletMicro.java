/** Applet for jPhoneLite. */

import java.io.*;
import java.net.*;

import javaforce.*;

public class PhoneAppletMicro extends javax.swing.JApplet implements WindowController {
  public void init() {
    String userid = getParameter("userid");
    String number = getParameter("number");
//    System.out.println("userid = " + userid);  //test
    if (userid != null) setupConfig(userid);
    String isJavaScript = getParameter("isJavaScript");
    Settings.isJavaScript = ((isJavaScript != null) && (isJavaScript.equals("true")));
    phone = new PhoneMicro(this, number);
    setContentPane(phone);
  }
  public void destroy() {
    Settings.saveSettings();
  }
  public void setPanelSize() {}
  public void setPanelVisible() {}
  public void setPanelAlwaysOnTop(boolean state) {}
  public void setupConfig(String userid) {
    System.out.println("Loading config for userid : " + userid);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
        new URL(getCodeBase().toString() + "/jphonelite-getconfig.php?userid=" + userid).openStream()));
//        new URL(getCodeBase().toString() + "/test.xml").openStream()));  //test
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
  private PhoneMicro phone;
  public BasePhone getBasePhone() {return phone;}
}

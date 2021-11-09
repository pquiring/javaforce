import java.io.*;

import javaforce.*;

public class Settings {
  private static String filename = JF.getUserPath() + "/.jfrecorddesktop.xml";
  public static Settings current = new Settings();
  public static void load() {
    try {
      current = new Settings();
      XML xml = new XML();
      xml.read(new FileInputStream(filename));
      xml.writeClass(current);
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
    }
  }
  public static void save() {
    try {
      XML xml = new XML();
      xml.readClass("settings", current);
      xml.write(new FileOutputStream(filename));
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String mode = "audio";  //audio, noaudio, timelapse
  public boolean stereo = false;
  public int timelapse = 60;  //seconds
  public String videoBitRate = "1M";
  public String audioBitRate = "128k";
  public String recording = "file";  //file, broadcast
  public int port = 80;
  public int segmentSecs = 5;
}

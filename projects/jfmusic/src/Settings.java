/**
 *
 * @author pquiring
 *
 * Created : Mar 14, 2014
 */

import java.io.*;

import javaforce.*;

public class Settings {
  public String midiDevice = "";
  public int recordingMode = RECORD_OVERWRITE;
  public int recordingTrack = RECORD_SINGLE_TRACK;

  public static Settings current;

  public static int RECORD_SINGLE_TRACK = 0;
  public static int RECORD_MULTI_TRACK = 1;

  public static int RECORD_OVERWRITE = 0;
  public static int RECORD_INSERT = 1;

  public static void loadSettings() {
    String fn = JF.getUserPath() + "/.jfmusic.xml";
    try {
      current = new Settings();
      XML xml = new XML();
      xml.read(new FileInputStream(fn));
      xml.writeClass(current);

      JFLog.log("loadSettings ok");
    } catch (FileNotFoundException e) {
      JFLog.log("Config file does not exist, using default values.");
      current = new Settings();
    } catch (Exception e) {
      JFLog.log(e);
      current = new Settings();
    }
  }
  public static void saveSettings() {
    String fn = JF.getUserPath() + "/.jfmusic.xml";
    try {
      XML xml = new XML();
      xml.readClass("settings", current);
      xml.write(new FileOutputStream(fn));
    } catch (Exception e) {
      JF.showError("Error", "Save failed : " + e);
    }
  }
}

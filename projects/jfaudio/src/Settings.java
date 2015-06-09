/**
 *
 * @author pquiring
 *
 * Created : Apr 25, 2014
 */

import java.io.*;

import javaforce.*;

public class Settings {
  private static String filename = JF.getUserPath() + "/.jfaudio.xml";
  public static Settings current = new Settings();

  public int freq = 44100;  //recording freq
  public int channels = 1;  //1=mono, 2=stereo, etc.
  public String input = "<default>", output = "<default>";  //devices

  public static void loadSettings() {
    try {
      current = new Settings();
      XML xml = new XML();
      xml.read(new FileInputStream(filename));
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
    try {
      XML xml = new XML();
      xml.readClass("settings", current);
      xml.write(new FileOutputStream(filename));
    } catch (Exception e) {
      JF.showError("Error", "Save failed : " + e);
    }
  }

  public static String getInput() {
    if (current.input == null || current.input.length() == 0 || current.input.equals("<default>")) return null;
    return current.input;
  }

  public static String getOutput() {
    if (current.output == null || current.output.length() == 0 || current.output.equals("<default>")) return null;
    return current.output;
  }

}

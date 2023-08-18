/** Config
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Config {
  public static Config config;

  private static String configFolder = JF.getUserPath();
  private static String configFile = "/.jfpasswords.xml";

  public String safe = "";
  public int passwordGeneratorLength = 12;
  public boolean passwordGeneratorSymbols = true;
  public boolean passwordGeneratorAmbiguous = true;
  public boolean passwordGeneratorOne = false;  //one upper case, number, symbol
  public boolean reAuthOnShow = true;
  public boolean bWindowMax = false;
  public int WindowXSize = -1, WindowYSize = -1;
  public int WindowXPos = 0, WindowYPos = 0;

  public static void loadConfig() {
    config = new Config();
    try {
      XML xml = new XML();
      FileInputStream fis = new FileInputStream(configFolder + configFile);
      xml.read(fis);
      xml.writeClass(config);
    } catch (FileNotFoundException e1) {
      config = new Config();
    } catch (Exception e2) {
      JFLog.log(e2);
      config = new Config();
    }
  }

  public static void saveConfig() {
    try {
      XML xml = new XML();
      FileOutputStream fos = new FileOutputStream(configFolder + configFile);
      xml.readClass("jpassword", config);
      xml.write(fos);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

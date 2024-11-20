public class Settings {
  public transient static Settings settings = new Settings();

  public boolean bPreserve = true;
  public boolean bUnix = true;
  public String encoding = Encodings.utf8;
  public boolean bClean = false;
  public int fontSize = 12;
  public boolean bWindowMax = false;
  public int WindowXSize = 800, WindowYSize = 600;
  public int WindowXPos = 0, WindowYPos = 0;
  public int tabSize = 2;
  public boolean bAutoIndent = false;
  public boolean bLineWrap = false;
  public boolean bTabToSpaces = false;
  //text mode settings
  public int foreColor = 0xffffff;
  public int backColor = 0x000000;
  public int tabStops = 8;
  public String termType = "ANSI";
};

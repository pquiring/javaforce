package javaforce.ansi.client;

/** Telnet Settings
 *
 * @author peterq.admin
 */

import java.awt.*;

public class Settings {
  public Color foreColor = Color.black;
  public Color backColor = Color.white;
  public Color cursorColor = Color.black;
  public Color selectColor = Color.blue;

  public int scrollBack = 100;
  public int tabStops = 8;
  public boolean localEcho;
  public boolean autoSize;
  public boolean utf8;
  public boolean hasComm;  //COM port natives available
  public boolean autowrap;

  public Font fnt;
  public int fontWidth;
  public int fontHeight;
  public int fontDescent;

  //connection info
  public String host;
  public int port;

  public int sx, sy;  //screen size
  public String name;
  public String protocol = "telnet";
  public String termType = "vt100";
  public String termApp;  //bash

  //ssh info
  public String sshKey;  //file
  public String username, password;
  public boolean x11;

}

package javaforce.ansi.client;

/** Site Profile
 *
 * @author pquiring
 */

import java.awt.*;

public class Profile {
  public int foreColor = 0x000000;
  public int backColor = 0xffffff;
  public int selectColor = 0x0000ff;

  public int scrollBack = 100;
  public int tabStops = 8;
  public boolean localEcho;
  public boolean autoSize = true;
  public boolean utf8 = true;
  public boolean autowrap = true;

  public Font fnt;
  public int fontWidth;
  public int fontHeight;
  public int fontDescent;

  //connection info
  public String host;
  public int port;

  //com settings
  public String com;
  public int baud;

  public int sx, sy;  //screen size
  public String name;
  public String protocol = "telnet";
  public String termType = "vt100";
  public String termApp;  //bash
  public String[] termArgs = {null, "-i", "-l", null};  //arg[0] = termApp, null terminated

  //ssh info
  public String sshKey;  //file
  public String username, password;
  public boolean x11;
}

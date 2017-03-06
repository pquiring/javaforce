package jfcontrols.panels;

/**
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class Events {
  public static void click(Component c) {
    String func = (String)c.getProperty("func");
    String arg = (String)c.getProperty("arg");
    if (func == null || arg == null) return;
    switch (func) {
      case "changePanel":
        break;
      case "toggleBit":
        break;
      case "setBit":
        break;
      case "clearBit":
    }
  }
}

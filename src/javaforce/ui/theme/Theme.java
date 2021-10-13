package javaforce.ui.theme;

/** Base class for Themes.
 *
 * @author pquiring
 */

import javaforce.ui.*;

public class Theme {
  private static Theme current = new BasicTheme();
  private Color fore, back;

  public static void setTheme(Theme theme) {
    Theme.current = theme;
  }
  public static Theme getTheme() {
    return current;
  }

  public Color getForeColor() {return fore;}
  public Color getBackColor() {return back;}
  public void setForeColor(Color clr) {fore = clr;}
  public void setBackColor(Color clr) {back = clr;}
}

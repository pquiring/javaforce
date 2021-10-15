package javaforce.ui.theme;

/** Base class for Themes.
 *
 * @author pquiring
 */

import javaforce.ui.*;

public class Theme {
  private static Theme current = new BasicTheme();
  private Color fore;
  private Color back;
  private Color disabled;
  private Color selected;

  public static void setTheme(Theme theme) {
    Theme.current = theme;
  }
  public static Theme getTheme() {
    return current;
  }

  public Color getForeColor() {return fore;}
  public Color getBackColor() {return back;}
  public Color getDisabledColor() {return disabled;}
  public Color getSelectedColor() {return selected;}

  public void setForeColor(Color clr) {fore = clr;}
  public void setBackColor(Color clr) {back = clr;}
  public void setDisabledColor(Color clr) {disabled = clr;}
  public void setSelectedColor(Color clr) {selected = clr;}
}

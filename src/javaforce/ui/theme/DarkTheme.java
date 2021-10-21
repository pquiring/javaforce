package javaforce.ui.theme;

/** Dark Theme
 *
 * @author pquiring
 */

import javaforce.ui.Color;

public class DarkTheme extends Theme {
  public DarkTheme() {
    setForeColor(Color.WHITE);
    setEditColor(Color.DARK_GRAY);
    setBackColor(Color.BLACK);
    setDisabledColor(Color.GRAY);
    setSelectedColor(Color.LIGHT_BLUE);
  }
}

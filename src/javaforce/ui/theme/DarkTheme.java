package javaforce.ui.theme;

import javaforce.ui.Color;

/** Dark Theme
 *
 * @author pquiring
 */

public class DarkTheme extends Theme {
  public DarkTheme() {
    setForeColor(Color.WHITE);
    setEditColor(Color.DARK_GRAY);
    setBackColor(Color.BLACK);
    setDisabledColor(Color.GRAY);
    setSelectedColor(Color.LIGHT_BLUE);
  }
}

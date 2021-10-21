package javaforce.ui.theme;

import javaforce.ui.Color;

/**
 *
 * @author pquiring
 */

public class BasicTheme extends Theme {
  public BasicTheme() {
    setForeColor(Color.BLACK);
    setEditColor(Color.WHITE);
    setBackColor(Color.LIGHT_GRAY);
    setDisabledColor(Color.GRAY);
    setSelectedColor(Color.LIGHT_BLUE);
  }
}

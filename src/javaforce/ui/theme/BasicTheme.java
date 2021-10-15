package javaforce.ui.theme;

import javaforce.ui.Color;

/**
 *
 * @author pquiring
 */

public class BasicTheme extends Theme {
  public BasicTheme() {
    setForeColor(Color.BLACK);
    setBackColor(Color.WHITE);
    setDisabledColor(Color.GRAY);
    setSelectedColor(Color.LIGHT_BLUE);
  }
}

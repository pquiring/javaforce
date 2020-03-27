package javaforce.ansi.server;

/**
 *
 * @author pquiring
 */

import javaforce.ASCII8;

public class ProgressBar extends Field {
  private int value;  //percent

  public void draw() {
    gotoHomePos();
    int on = value * width / 100;
    int off = width - on;
    System.out.print(ANSI.repeat(on, ASCII8.convert(219)));
    System.out.print(ANSI.repeat(off, ASCII8.convert(177)));
  }
  public void setValue(int value) {
    this.value = value;
  }
  public int getValue() {
    return value;
  }
}

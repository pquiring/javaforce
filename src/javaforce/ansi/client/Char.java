/*
 * Char.java
 *
 * Created on August 2, 2007, 8:06 PM
 *
 * @author pquiring
 */

package javaforce.ansi.client;

import java.awt.Color;

public class Char {
  public Char(Color fc, Color bc) {
    ch = 0;
    this.fc = fc;
    this.bc = bc;
    blink = false;
  }
  public Char(Color fc, Color bc, boolean blink) {
    ch = 0;
    this.fc = fc;
    this.bc = bc;
    this.blink = blink;
  }
/*
  public void set(Char x) {
    ch = x.ch;
    fc = new Color(x.fc.getRGB());
    bc = new Color(x.bc.getRGB());
    blink = x.blink;
  }
*/
  public char ch;
  public Color fc;  //fore color
  public Color bc;  //back color
  public boolean blink;
}

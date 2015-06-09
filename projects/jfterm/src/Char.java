/*
 * Char.java
 *
 * Created on August 2, 2007, 8:06 PM
 *
 * @author pquiring
 */
import java.awt.Color;

public class Char {
  public Char() {
    ch = 0;
    fc = Settings.settings.foreColor;
    bc = Settings.settings.backColor;
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

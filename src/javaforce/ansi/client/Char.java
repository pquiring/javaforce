/*
 * Char.java
 *
 * Created on August 2, 2007, 8:06 PM
 *
 * @author pquiring
 */

package javaforce.ansi.client;

public class Char {
  public Char(int fc, int bc) {
    ch = 0;
    this.fc = fc;
    this.bc = bc;
    blink = false;
  }
  public Char(int fc, int bc, boolean blink) {
    ch = 0;
    this.fc = fc;
    this.bc = bc;
    this.blink = blink;
  }
  public char ch;
  public int fc;  //fore color
  public int bc;  //back color
  public boolean blink;
}

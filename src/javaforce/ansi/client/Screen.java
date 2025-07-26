package javaforce.ansi.client;

/** Screen interface
 *
 * @author pquiring
 */

public interface Screen {
  public void output(char[] buf);
  public int getForeColor();
  public int getBackColor();
  public void setForeColor(int newClr);
  public void setBackColor(int newClr);
  public int getsx();
  public int getsy();
  public int getx();
  public int gety();
  public int gety1();
  public int gety2();
  public void sety1(int v);
  public void sety2(int v);
  public void scrollUp(int cnt);
  public void scrollDown(int cnt);
  public void delete();
  public void insert();
  public void gotoPos(int x,int y);
  public void setChar(int cx, int cy, char ch);
  public void clrscr();
  public void setAutoWrap(boolean state);
  public void setBlinker(boolean state);
  public void setReverse(boolean state);
  public String getTermType();
}

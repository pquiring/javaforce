/**
 *
 * @author pquiring
 *
 * Created : Jan 20, 2014
 */

import java.awt.*;
import javax.swing.*;

import javaforce.*;
import javaforce.awt.*;

public class TableCell extends JComponent {
  private static Font fnt;
  private static int metrics[];
  private static int fx, fy;
  //... .. ..
  public TableCell(String value) {
    this.value = value;
    selection = 0;
  }
  public void paint(Graphics g) {
    int width = getWidth();
    int height = getHeight();
    g.setFont(fnt);
    if (selected) {
      g.setColor(new Color(0xb8cfe5));
      switch (selection) {
        case 0:
          g.fillRect(fx * 0, 0, fx * 3, height);
          break;
        case 1:
          g.fillRect(fx * 4, 0, fx * 2, height);
          break;
        case 2:
          g.fillRect(fx * 7, 0, fx * 2, height);
          break;
/*
        case 3:
          g.fillRect(fx * 8, 0, fx * 1, height);
          break;
        case 4:
          g.fillRect(fx * 9, 0, fx * 2, height);
          break;
*/
      }
    }
    g.setColor(Color.black);
    g.drawString(value, 0, metrics[1]);
  }
  public Dimension getPreferredSize() {
    return new Dimension(75, 20);
  }
  public String value;
  public boolean selected = false;
  public int selection;  //0=note 1=volcmd 2=volparam 3=fxcmd 4=fxparam
  public void setSelected(boolean state) {
    selected = state;
  }
  public void setSelection(int sel) {
    this.selection = sel;
  }
  public int getSelection() {
    return selection;
  }
  static {
    fnt = new Font("Lucida Console", 0, 12);
    metrics = JFAWT.getFontMetrics(fnt);  //width,ascent,descent
    fx = metrics[0];
    fy = metrics[1] + metrics[2];
  }
}

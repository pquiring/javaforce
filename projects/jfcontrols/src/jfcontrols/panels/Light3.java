package jfcontrols.panels;

/** Light (3 states)
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class Light3 extends Component {
  private int clr0, clr1, clrn;
  private int state;

  public Light3(int clr0, int clr1, int clrn) {
    this.clr0 = clr0;
    this.clr1 = clr1;
    this.clrn = clrn;
    setBorder(true);
    setBackColor(clr0);
  }

  public String html() {
    return "<div" + getAttrs() + "></div>";
  }

  public void setColor(int state) {
    this.state = state;
    if (state == 0)
      setBackColor(clr0);
    else if (state > 0)
      setBackColor(clr1);
    else
      setBackColor(clrn);
  }

  public void setColors(int clr0, int clr1, int clrn) {
    this.clr0 = clr0;
    this.clr1 = clr1;
    this.clrn = clrn;
    setColor(state);
  }
}

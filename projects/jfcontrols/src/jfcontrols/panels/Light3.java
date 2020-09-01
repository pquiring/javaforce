package jfcontrols.panels;

/** Light (3 states)
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class Light3 extends Component {
  private int clr0, clrP, clrN;
  private int state;

  public Light3(int clr0, int clrP, int clrN, int initState) {
    this.clr0 = clr0;
    this.clrP = clrP;
    this.clrN = clrN;
    setBorder(true);
    setColor(initState);
  }

  public String html() {
    return "<div" + getAttrs() + "></div>";
  }

  public void setColor(int state) {
    this.state = state;
    if (state == 0)
      setBackColor(clr0);
    else if (state > 0)
      setBackColor(clrP);
    else
      setBackColor(clrN);
  }

  public void setColors(int clr0, int clrP, int clrN) {
    this.clr0 = clr0;
    this.clrP = clrP;
    this.clrN = clrN;
    setColor(state);
  }
}

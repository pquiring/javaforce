package jfcontrols.panels;

/** Light (2 states)
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class Light extends Component {
  private int clr0, clr1;
  private boolean state;

  public Light(int clr0, int clr1) {
    this.clr0 = clr0;
    this.clr1 = clr1;
    setBorder(true);
    setBackColor(clr0);
  }

  public String html() {
    return "<div" + getAttrs() + "></div>";
  }

  public void setColor(boolean state) {
    this.state = state;
    setBackColor(state ? clr1 : clr0);
  }

  public void setColors(int clr0, int clr1) {
    this.clr0 = clr0;
    this.clr1 = clr1;
    setColor(state);
  }
}

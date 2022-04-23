package javaforce.webui;

/** Row - display components in a row.
 *
 * @author pquiring
 */

public class Row extends Container {
  public Row() {
    setDisplay("block");
    setMaxWidth();
  }
  public void add(Component comp) {
    super.add(comp);
    comp.setDisplay("inline-block");
    comp.setVerticalAlign(TOP);
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.setDisplay("inline-block");
    comp.setVerticalAlign(TOP);
  }
}

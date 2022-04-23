package javaforce.webui;

/** Column - display components in a column.
 *
 * @author pquiring
 */

public class Column extends Container {
  public Column() {
    setDisplay("block");
    setMaxHeight();
  }
  public void add(Component comp) {
    super.add(comp);
    comp.setDisplay("blcok");
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.setDisplay("block");
  }
}

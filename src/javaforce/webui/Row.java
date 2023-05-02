package javaforce.webui;

/** Row - display components in a row.
 *
 * @author pquiring
 */

public class Row extends Container {
  //TODO : use a Cell like ListBox - forcing "display = inline-block" might not always work
  public Row() {
    addClass("row");
    //setMaxWidth();
  }
  public void add(Component comp) {
    super.add(comp);
    comp.addClass("rowitem");
    comp.setVerticalAlign(TOP);
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.addClass("rowitem");
    comp.setVerticalAlign(TOP);
  }
}

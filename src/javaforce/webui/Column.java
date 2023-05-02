package javaforce.webui;

/** Column - display components in a column.
 *
 * @author pquiring
 */

public class Column extends Container {
  //TODO : use a Cell like ListBox - forcing "display = block" might not always work
  public Column() {
    addClass("column");
    //setMaxHeight();
  }
  public void add(Component comp) {
    super.add(comp);
    comp.addClass("columnitem");
  }
  public void add(int idx, Component comp) {
    super.add(idx, comp);
    comp.addClass("columnitem");
  }
}

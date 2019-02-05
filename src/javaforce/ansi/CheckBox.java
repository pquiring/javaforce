package javaforce.ansi;

/** CheckBox
 *
 * @author pquiring
 */

public class CheckBox extends Field {
  public boolean checked;

  public boolean isChecked() {
    return checked;
  }
  public void setChecked(boolean checked) {
    this.checked = checked;
  }
  public void toggle() {
    checked = !checked;
  }
  public void draw() {
    gotoCurrentPos();
    System.out.print(checked ? "X" : " ");
    super.draw();
  }
}

package javaforce.webui;

/** CheckBox
 *
 * @author pquiring
 */

public class CheckBox extends Component {
  public CheckBox(String text) {
    this.text = text;
  }
  public String html() {
    return "<input type=checkbox id='" + id + "' onclick='return onClick(this);' class=checkbox" + (selected ? " checked" : "") + "><label for='" + id + "' class='noselect'>" + text + "</label>";
  }
  private String text;
  private boolean selected;
  public void setText(String text) {
    this.text = text;
  }
  public void setSelected(boolean state) {
    if (selected == state) return;
    selected = state;
    client.sendEvent(id, "setSelected", new String[] {"state=" + selected});
  }
  public boolean isSelected() {
    return selected;
  }
  public void dispatchEvent(String event, String args[]) {
    if (event.equals("click")) {
      selected = !selected;
      if (click != null) click.onClick(this);
    }
  }
  private Click click;
  public void addClickListener(Click handler) {
    click = handler;
  }
  public static interface Click {
    public void onClick(CheckBox button);
  }
}

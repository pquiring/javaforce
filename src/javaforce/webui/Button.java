package javaforce.webui;

/** Button.java
 *
 * Generic button.
 *
 * @author pquiring
 */

public class Button extends Component {
  public Button(String text) {
    this.text = text;
  }
  public String html() {
    return "<button id='" + id + "' onclick='return onClick(this);' class=button>" + text + "</button>";
  }
  private String text;
  public void setText(String text) {
    this.text = text;
  }
  public void dispatchEvent(String event, String args[]) {
    if (event.equals("click")) {
      if (click != null) click.onClick(this);
    }
  }
  private Click click;
  public void addClickListener(Click handler) {
    click = handler;
  }
  public static interface Click {
    public void onClick(Button button);
  }
}

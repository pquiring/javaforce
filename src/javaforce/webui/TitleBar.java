package javaforce.webui;

/** TitleBar to be placed in PopupPanel
 *
 * @author pquiring
 */

public class TitleBar extends Container {
  private Label label;
  private Button button;
  private Pad pad;
  private PopupPanel panel;
  public TitleBar(String title, PopupPanel panel) {
    this.panel = panel;
    setClass("titlebar");
    label = new Label(title);
    label.addClass("defaultcursor");
    label.addClass("noselect");
    label.setColor(Color.white);
    add(label);
    pad = new Pad();
    add(pad);
    button = new Button("X");
    button.addClass("right");
    add(button);
    button.addClickListener((e, c) -> {
      panel.setVisible(false);
    });
  }
  public void init() {
    super.init();
    addEvent("onmousedown", "onmousedownPopupPanel(event, " + panel.id + ");");
  }
  public void setHeight(int h) {
    super.setHeight(h);
    label.setHeight(h);
    button.setWidth(h);
    button.setHeight(h);
  }
  public void setText(String title) {
    label.setText(title);
  }
}

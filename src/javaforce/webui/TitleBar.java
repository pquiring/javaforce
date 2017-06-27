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
    addEvent("onmousedown", "onmousedownPopupPanel(event, " + panel.id + ");");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + "'>");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
  public void setHeight(int h) {
    super.setHeight(h);
    button.setWidth(h);
    button.setHeight(h);
  }
  public void setText(String title) {
    label.setText(title);
  }
}

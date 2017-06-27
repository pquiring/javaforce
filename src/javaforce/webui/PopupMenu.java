package javaforce.webui;

/** Popup Menu
 *
 * @author pquiring
 */

public class PopupMenu extends Container {
  public MenuItem activeItem;
  public PopupMenu() {
    setClass("popupmenu");
    setDisplay("inline");
    addEvent("mousedown", "onMouseDown(event, this);");
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
  public void setVisible(boolean state) {
    super.setVisible(state);
    if (!state) {
      int cnt = count();
      for(int a=0;a<cnt;a++) {
        Component c = get(a);
        if (c instanceof Menu) {
          Menu m = (Menu)c;
          m.closeMenu();
        }
      }
    }
  }
  public void onMouseDown(String args[]) {
    client.popupMenuMouseDown = true;
  }
}

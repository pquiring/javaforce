package javaforce.webui;

/** Popup Menu
 *
 * @author pquiring
 */

public class PopupMenu extends Container {
  public MenuItem activeItem;
  public PopupMenu() {
    initInvisible();
    setClass("popupmenu");
    addEvent("mousedown", "onMouseDown(event, this);");
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
  public void onMouseDown(String[] args) {
    client.popupMenuMouseDown = true;
  }
}

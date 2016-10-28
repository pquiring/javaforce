package javaforce.webui;

/** Menu (place on MenuBar or PopupMenu)
 *
 * @author pquiring
 */

public class Menu extends MenuItem {
  public PopupMenu menu;
  private boolean inMenuBar;
  private boolean inMenu;
  public Menu(String text) {
    super(text);
    setClass("menu");
    menu = new PopupMenu();
    add(menu);
    addEvent("onmousemove", "onMouseMove(event, this);");
  }
  public void init() {
    if (parent == null) return;
    if (parent instanceof MenuBar) {
      inMenuBar = true;
    }
    if (parent instanceof PopupMenu) {
      inMenu = true;
    }
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append(super.html());
    sb.append(menu.html());
    return sb.toString();
  }
  public void add(MenuItem item) {
    menu.add(item);
  }
  public void onMouseDown(String args[]) {

  }
  public void onClick(String args[]) {
    client.sendEvent(id, "getpossize", null);
  }
  public void onPosSize(String args[]) {
    super.onPosSize(args);
    if (inMenuBar)
      menu.setPosition(x, y + height);
    else
      menu.setPosition(x + width, y);
    menu.setVisible(true);
    if (inMenuBar) {
      client.topPopupMenu = menu;
    }
  }
  public void onMouseMove(String args[]) {
    if (inMenu) {
      onClick(args);
    } else if (client.topPopupMenu != null && client.topPopupMenu != menu) {
      client.topPopupMenu.setVisible(false);
      onClick(args);
    }
  }
  public void closeMenu() {
    menu.setVisible(false);
  }
  public static void onMouseDownBody(Client client, String args[]) {
    //args : p=x,y
/*
    System.out.println("args=" + args.length);
    String f[] = args[0].substring(2).split(",");
    int mx = Integer.valueOf(f[0]);
    int my = Integer.valueOf(f[0]);
*/
    if (!client.popupMenuMouseDown) {
      if (client.topPopupMenu != null) {
        client.topPopupMenu.setVisible(false);
        client.topPopupMenu = null;
      }
    } else {
      client.popupMenuMouseDown = false;
    }
  }
}

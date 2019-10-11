package javaforce.webui;

/** Menu (place on MenuBar or PopupMenu)
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class Menu extends MenuItem {
  public PopupMenu popupMenu;
  private boolean inMenuBar;
  private boolean inMenu;
  public Menu(String text) {
    super(text);
    setClass("menu");
    popupMenu = new PopupMenu();
    add(popupMenu);
    addEvent("onmousemove", "onMouseMove(event, this);");
  }
  public void init() {
    super.init();
    if (parent == null) {
      System.out.println("Error:Menu.parent == null");
    }
    else if (parent instanceof MenuBar) {
      inMenuBar = true;
    }
    else if (parent instanceof PopupMenu) {
      inMenu = true;
    }
    else {
      System.out.println("Error:Menu.parent type unknown");
    }
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append(super.html());
    sb.append(popupMenu.html());
    return sb.toString();
  }
  public void add(MenuItem item) {
    popupMenu.add(item);
  }
  public void onMouseDown(String args[]) {

  }
  public void onClick(String args[], MouseEvent me) {
    sendEvent("getpossize", null);
  }
  public void onPosSize(String args[]) {
    super.onPosSize(args);
    if (inMenuBar)
      popupMenu.setPosition(x, y + height);
    else
      popupMenu.setPosition(x + width, y);
    popupMenu.setVisible(true);
    if (inMenuBar) {
      client.topPopupMenu = popupMenu;
    }
  }
  public void onMouseMove(String args[]) {
    if (!inMenu && client.topPopupMenu != null && client.topPopupMenu != popupMenu) {
      client.topPopupMenu.setVisible(false);
    }
    onClick(args, null);
  }
  public void closeMenu() {
    popupMenu.setVisible(false);
  }
  public static void onMouseDownBody(WebUIClient client, String args[]) {
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

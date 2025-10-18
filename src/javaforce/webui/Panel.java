package javaforce.webui;

/** Panel to display components.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.webui.event.*;

public class Panel extends Container {
  private int overflow = -1;
  public Panel() {
    addClass("panel");
    setFlexDirection(COLUMN);
    setOverflow(HIDDEN);
  }
  public int getOverflow() {
    return overflow;
  }
  public void setOverflow(int type) {
    switch (overflow) {
      case VISIBLE: removeClass("overflow-visible"); break;
      case HIDDEN: removeClass("overflow-hidden"); break;
      case SCROLL: removeClass("overflow-scroll"); break;
      case AUTO: removeClass("overflow-auto"); break;
    }
    overflow = type;
    switch (overflow) {
      case VISIBLE: addClass("overflow-visible"); break;
      case HIDDEN: addClass("overflow-hidden"); break;
      case SCROLL: addClass("overflow-scroll"); break;
      case AUTO: addClass("overflow-auto"); break;
    }
  }

  public void onLoaded(String[] args) {
    super.onLoaded(args);
    if (parent == null) invokeOnLoaded(this);
  }
  private static void invokeOnLoaded(Container c) {
    int cnt = c.count();
    for(int a=0;a<cnt;a++) {
      Component c2 = c.get(a);
      c2.onLoaded(null);
      if (c2 instanceof Container) {
        invokeOnLoaded((Container)c2);
      }
    }
  }

  public void init() {
    super.init();
    onInited(null);
  }

  protected void onInited(String[] args) {
    for(int a=0;a<inited.length;a++) {
      inited[a].inited(this);
    }
  }
  private Inited[] inited = new Inited[0];
  public void addInitedListener(Inited handler) {
    inited = Arrays.copyOf(inited, inited.length + 1);
    inited[inited.length-1] = handler;
  }
}

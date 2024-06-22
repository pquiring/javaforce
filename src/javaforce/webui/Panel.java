package javaforce.webui;

/** Panel to display components.
 *
 * @author pquiring
 */

public class Panel extends Container {
  private int overflow = -1;
  public Panel() {
    addClass("panel");
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
  public void init() {
    super.init();
    //if there is only one child component (excluding popup items) that is a panel than adjust it's size when this panel resizes
    int cnt = count();
    int pcnt = 0;
    Component pcmp = null;
    for(int a=0;a<cnt;a++) {
      Component child = get(a);
      if (child.isPopup()) continue;
      if (child instanceof Panel) {
        pcmp = child;
      }
      pcnt++;
    }
    if (pcnt == 1 && pcmp != null) {
      addEvent("onresize", "onresizePanel(event, this,\"" + pcmp.id + "\");");
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
}

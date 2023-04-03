package javaforce.webui;

/** Popup Panel (or Window)
 *
 * @author pquiring
 */

public class PopupPanel extends Panel {
  private TitleBar titleBar;
  private Block block;
  private boolean modal;
  public PopupPanel(String title) {
    initInvisible();
    titleBar = new TitleBar(title, this);
    add(titleBar);
    setClass("popuppanel");
    super.setVisible(false);
    modal = false;
    block = new Block();
    block.setClass("modal");
    setPosition(0, 0);
  }
  /** Modal windows block all other windows underneath it. */
  public void setModal(boolean state) {
    modal = state;
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    if (modal) {
      sb.append(block.html());
    }
    sb.append("<div" + getAttrs() + "'>");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</div>");
    return sb.toString();
  }
  public void setVisible(boolean state) {
    if (modal) {
      if (state) {
        client.sendEvent(block.id, "setzindex", new String[] {"idx=" + getClient().getZIndex()});
      } else {
        getClient().releaseZIndex();
      }
      block.setVisible(state);
    }
    if (state) {
      sendEvent("setzindex", new String[] {"idx=" + getClient().getZIndex()});
    } else {
      getClient().releaseZIndex();
    }
    super.setVisible(state);
  }
  public void setClient(WebUIClient client) {
    super.setClient(client);
    titleBar.setClient(client);
    block.setClient(client);
  }
  public void setTitleBarSize(int sz) {
    titleBar.setHeight(sz);
  }
  public void setTitle(String title) {
    titleBar.setText(title);
  }
}

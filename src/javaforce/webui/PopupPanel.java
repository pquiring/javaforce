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
    titleBar = new TitleBar(title, this);
    add(titleBar);
    setClass("popuppanel");
    display = "inline-flex";
    modal = false;
    block = new Block();
    block.setClass("modal");
  }
  /** Modal windows block all other windows underneath it. */
  public void setModal(boolean state) {
    modal = state;
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
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
      client.sendEvent(id, "setzindex", new String[] {"idx=" + getClient().getZIndex()});
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
  public void setTitleBarSize(String sz) {
    titleBar.setHeight(sz);
  }
}

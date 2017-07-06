package javaforce.webui;

/** Centered Panel to display components.
 *
 * @author pquiring
 */

public class CenteredPanel extends Panel {
  private void setSize() {
    WebUIClient client = getClient();
    setWidth(client.getWidth());
    setHeight(client.getHeight());
  }
  public void init() {
    setSize();
    getClient().addResizedListener((comp, width, height) -> {
      setSize();
    });
  }
  public CenteredPanel() {
    setAlign(CENTER);
    setVerticalAlign(CENTER);
    setDisplay("table-cell");  //does NOT work with percentage width/height
  }
}

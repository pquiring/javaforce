package javaforce.webui;

/** Layers Panel.
 *
 * All added Components will overlap.
 *
 * @author pquiring
 */

public class LayersPanel extends Container {
  public LayersPanel() {
    addClass("layersPanel");
  }
  public void add(Component c) {
    super.add(c);
    c.addClass("layer");
  }
}

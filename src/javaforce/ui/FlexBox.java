package javaforce.ui;

/** FlexBox - size auto fills parent width/height.
 *
 * @author pquiring
 */

import javaforce.*;

public class FlexBox extends Component {
  public FlexBox() {
  }

  public Dimension getMinSize() {
    return zero;
  }

  public void layout(LayoutMetrics metrics) {
    if (debug) JFLog.log("FlexBox.layout()" + metrics.pos.x + "," + metrics.pos.y + "@" + this);
    pos.x = metrics.pos.x;
    pos.y = metrics.pos.y;
    size.width = metrics.size.width;
    size.height = metrics.size.height;
  }
}

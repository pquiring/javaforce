package javaforce.ui;

/** Column - lays out components in a column.
 *
 * @author pquiring
 */

import javaforce.*;

public class Column extends Container {

  public Dimension getMinSize() {
    int width = 0;
    int height = 0;
    for(Component child : children) {
      Dimension size = child.getMinSize();
      if (size.width > width) width = size.width;
      height += size.height;
    }
    minSize.width = width;
    minSize.height = height;
    return minSize;
  }

  /** Lay out components in a column. */
  public void layout(LayoutMetrics metrics) {
    setPosition(metrics.pos);
    setSize(metrics.size);
    int min_y = 0;
    int flex_count = 0;
    int flex_size = 0;
    for(Component child : children) {
      if (child instanceof FlexBox) {
        flex_count++;
      } else if (child instanceof Container) {
        min_y += child.getMinHeight();
      } else {
        min_y += child.getMinHeight();
      }
    }
    if (flex_count > 0) {
      flex_size = (metrics.size.height - min_y) / flex_count;
      if (debug) JFLog.log("Column:flex_size=" + flex_size + ",min_y=" + min_y);
    }
    for(Component child : children) {
      child.setPosition(metrics.pos);
      if (child instanceof FlexBox) {
        metrics.pos.y += flex_size;
      } else if (child instanceof Container) {
        int org_width = metrics.size.width;
        int org_height = metrics.size.height;
        int org_x = metrics.pos.x;
        int org_y = metrics.pos.y;
        metrics.size.height = child.getMinHeight();
        child.layout(metrics);
        metrics.size.width = org_width;
        metrics.size.height = org_height;
        metrics.pos.x = org_x;
        metrics.pos.y = org_y;
        metrics.pos.y += child.getMinHeight();
      } else {
        child.layout(metrics);
        metrics.pos.y += child.getMinHeight();
      }
    }
  }
}

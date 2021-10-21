package javaforce.ui;

/** Scroll Box - provides a view port view into another larger component with scroll bars.
 *
 * @author pquiring
 */

import javaforce.*;

public class ScrollBox extends Component {
  private Image buffer;
  private ScrollBar vBar, hBar;
  private LayoutMetrics cmp_metrics = new LayoutMetrics();
  private Component cmp;
  private int offx, offy;

  public ScrollBox(Component cmp, int bars) {
    this.cmp = cmp;
    if ((bars & Direction.VERTICAL) != 0) {
      vBar = new ScrollBar(Direction.VERTICAL);
      vBar.setChangeListener((Component bar) -> {
        offy = vBar.getValue();
      });
    }
    if ((bars & Direction.HORIZONTAL) != 0) {
      hBar = new ScrollBar(Direction.HORIZONTAL);
      hBar.setChangeListener((Component bar) -> {
        offx = hBar.getValue();
      });
    }
  }

  public Component getComponent() {
    return cmp;
  }

  public Dimension getMinSize() {
    return size;
  }

  public void layout(LayoutMetrics metrics) {
    if (debug) JFLog.log("layout:" + metrics.size.width + "x" + metrics.size.height + "@" + metrics.pos.x + "," + metrics.pos.y + ":" + this);
    //layout component
    cmp_metrics.setPosition(0, 0);
    cmp_metrics.setSize(cmp.getMinSize());
    cmp.layout(cmp_metrics);
    //layout bars
    if (vBar != null) {
      cmp_metrics.setPosition(getX() + getWidth() - 16, getY());
      cmp_metrics.setSize(16, getHeight() - (hBar != null ? 16 : 0));
      vBar.layout(cmp_metrics);
    }
    if (hBar != null) {
      cmp_metrics.setPosition(getX(), getY() + getHeight() - 16);
      cmp_metrics.setSize(getWidth() - (vBar != null ? 16 : 0), 16);
      hBar.layout(cmp_metrics);
    }
  }

  public void setSize(int width, int height) {
    super.setSize(width, height);
    if (vBar != null) {
      vBar.setSize(16, height - (hBar != null ? 16 : 0));
      vBar.setFullsize(cmp.getMinHeight());
      vBar.setViewsize(getHeight() - (hBar != null ? 16 : 0));
    }
    if (hBar != null) {
      hBar.setSize(width - (vBar != null ? 16 : 0), 16);
      hBar.setFullsize(cmp.getMinWidth());
      hBar.setViewsize(getWidth() - (vBar != null ? 16 : 0));
    }
  }

  public void setStepsize(int size) {
    if (vBar != null) {
      vBar.setStepsize(size);
    }
    if (hBar != null) {
      hBar.setStepsize(size);
    }
  }

  public void render(Image image) {
    if (vBar != null) {
      vBar.render(image);
    }
    if (hBar != null) {
      hBar.render(image);
    }
    Dimension cmp_size = cmp.getMinSize();
    if (buffer == null) {
      buffer = new Image(cmp_size.width, cmp_size.height);
    }
    if (buffer.getWidth() != cmp_size.width || buffer.getHeight() != cmp_size.height) {
      buffer.setSize(cmp_size);
    }
    //render children into a seperate image
    cmp.render(buffer);
    //copy "area" into real image
    //source fullsize
    int sx = offx;
    int sy = offy;
    int sw = cmp_size.width;
    int sh = cmp_size.height;
    //dest viewport
    int dx = getX();
    int dy = getY();
    int dw = getWidth();
    if (vBar != null) {
      dw -= 16;
    }
    int dh = getHeight();
    if (hBar != null) {
      dh -= 16;
    }
    image.putPixels(buffer.getBuffer(), dx, dy, dw, dh, sx + (sy * sw), sw);
  }

  private static Point pt = new Point();

  public void mouseMove(int x, int y) {
    super.mouseMove(x, y);
    pt.x = x;
    pt.y = y;
    if (vBar != null) {
      if (vBar.isInside(pt)) {
        vBar.mouseMove(x,y);
        return;
      }
    }
    if (hBar != null) {
      if (hBar.isInside(pt)) {
        hBar.mouseMove(x,y);
        return;
      }
    }
    //need to adjust x/y based on offset
    cmp.mouseMove(x - pos.x + offx, y - pos.y + offy);
  }

  public void mouseDown(int button) {
    pt.x = getMouseX();
    pt.y = getMouseY();
    if (vBar != null) {
      if (vBar.isInside(pt)) {
        vBar.mouseDown(button);
        return;
      }
    }
    if (hBar != null) {
      if (hBar.isInside(pt)) {
        hBar.mouseDown(button);
        return;
      }
    }
    cmp.mouseDown(button);
  }

  public void mouseUp(int button) {
    pt.x = getMouseX();
    pt.y = getMouseY();
    if (vBar != null) {
      if (vBar.isInside(pt)) {
        vBar.mouseUp(button);
        return;
      }
    }
    if (hBar != null) {
      if (hBar.isInside(pt)) {
        hBar.mouseUp(button);
        return;
      }
    }
    cmp.mouseUp(button);
  }

  public void mouseScroll(int dx, int dy) {
    pt.x = getMouseX();
    pt.y = getMouseY();
    if (vBar != null) {
      if (vBar.isInside(pt)) {
        vBar.mouseScroll(dx, dy);
        return;
      }
    }
    if (hBar != null) {
      if (hBar.isInside(pt)) {
        hBar.mouseScroll(dx, dy);
        return;
      }
    }
    cmp.mouseScroll(dx, dy);
  }

  public String toString() {
    return "ScrollBox";
  }
}

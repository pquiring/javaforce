package javaforce.ui;

/** Scroll Box - provides a view port view into another larger component with scroll bars.
 *
 * @author pquiring
 */

import javaforce.*;

public class ScrollBox extends Component implements ScrollLink {
  private Image buffer;
  private ScrollBar vBar, hBar;
  private LayoutMetrics cmp_metrics = new LayoutMetrics();
  private Component cmp;
  private int offx, offy;
  private ScrollLink link;
  private int clientWidth;
  private int clientHeight;

  public ScrollBox(Component cmp, int bars) {
    this.cmp = cmp;
    if ((bars & Direction.VERTICAL) != 0) {
      vBar = new ScrollBar(Direction.VERTICAL);
      vBar.setChangeListener((Component bar) -> {
        if (link != null) {
          link.setClientY(vBar.getValue());
        } else {
          offy = vBar.getValue();
        }
      });
    }
    if ((bars & Direction.HORIZONTAL) != 0) {
      hBar = new ScrollBar(Direction.HORIZONTAL);
      hBar.setChangeListener((Component bar) -> {
        if (link != null) {
          link.setClientX(hBar.getValue());
        } else {
          offx = hBar.getValue();
        }
      });
    }
    cmp.setBorderStyle(LineStyle.NONE);
    setBorderStyle(LineStyle.SOLID);
    if (cmp instanceof ScrollLink) {
      link = (ScrollLink)cmp;
      link.setLink(this);
    }
    cmp.setParent(this);
  }

  public Component getComponent() {
    return cmp;
  }

  public Dimension getMinSize() {
    return size;
  }

  private Dimension cmp_size;

  public void layout(LayoutMetrics metrics) {
    //layout component
    super.layout(metrics);
    cmp_metrics.setPosition(0, 0);
    cmp_size = cmp.getMinSize();
    boolean linked = link != null;
    clientWidth = getWidth();
    if (vBar != null) {
      clientWidth -= 16;
    }
    if (cmp_size.width < clientWidth) {
      cmp_size.width = clientWidth;
    }
    if (cmp_size.width > clientWidth && linked) {
      cmp_size.width = clientWidth;
    }
    clientHeight = getHeight();
    if (hBar != null) {
      clientHeight -= 16;
    }
    if (cmp_size.height < clientHeight) {
      cmp_size.height = clientHeight;
    }
    if (cmp_size.height > clientHeight && linked) {
      cmp_size.height = clientHeight;
    }
    cmp_metrics.setSize(cmp_size);
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
      vBar.setViewsize(getHeight() - (hBar != null ? 16 : 0) - (getBorderStyle() != LineStyle.NONE ? 2 : 0));
    }
    if (hBar != null) {
      hBar.setSize(width - (vBar != null ? 16 : 0), 16);
      hBar.setFullsize(cmp.getMinWidth());
      hBar.setViewsize(getWidth() - (vBar != null ? 16 : 0) - (getBorderStyle() != LineStyle.NONE ? 2 : 0));
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

  public void setFullsize(Dimension size) {
    if (size.width < clientWidth) {
      size.width = clientWidth;
    }
    if (size.height < clientHeight) {
      size.height = clientHeight;
    }
    if (vBar != null) {
      vBar.setFullsize(size.height);
    }
    if (hBar != null) {
      hBar.setFullsize(size.width);
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
    int sx = link == null ? offx : 0;
    int sy = link == null ? offy : 0;
    int sw = cmp_size.width;
    int sh = cmp_size.height;
    //dest viewport
    int dx = getX();
    int dy = getY();
    int dw = getWidth();
    int dh = getHeight();
    if (getBorderStyle() != LineStyle.NONE) {
      image.setForeColor(getForeColor());
      image.setLineStyle(borderStyle);
      image.drawBox(dx, dy, dw, dh);
      image.setLineStyle(LineStyle.SOLID);
      dx++;
      dy++;
      dw -= 2;
      dh -= 2;
    }
    if (vBar != null) {
      dw -= 16;
    }
    if (dw > sw) {
      dw = sw;
    }
    if (hBar != null) {
      dh -= 16;
    }
    if (dh > sh) {
      dh = sh;
    }
    image.putPixels(buffer.getBuffer(), dx, dy, dw, dh, sx + (sy * sw), sw);
  }

  public void setLayer(int layer) {
    super.setLayer(layer);
    if (vBar != null) vBar.setLayer(layer);
    if (hBar != null) hBar.setLayer(layer);
    cmp.setLayer(layer);
  }

  public void resetOffset() {
    offx = 0;
    if (vBar != null) {
      vBar.setValue(0);
    }
    offy = 0;
    if (hBar != null) {
      hBar.setValue(0);
    }
    if (link != null) {
      link.setClientX(0);
      link.setClientY(0);
    }
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

  public int getClientX() {
    return offx;
  }

  public void setClientX(int value) {
    offx = value;
    if (hBar != null) {
      hBar.setValue(offx);
    }
  }

  public int getClientY() {
    return offy;
  }

  public void setClientY(int value) {
    offy = value;
    if (vBar != null) {
      vBar.setValue(offy);
    }
  }

  public void setLink(ScrollBox link) {
    //nop
  }

  public String toString() {
    return "ScrollBox";
  }
}

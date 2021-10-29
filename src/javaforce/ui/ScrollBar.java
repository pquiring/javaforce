package javaforce.ui;

/** Scroll Bar
 *
 * @author pquiring
 */

import javaforce.*;

public class ScrollBar extends Component {
  private int dir;
  private int value;
  private int viewsize;
  private int fullsize;
  private int v1, v2;
  private float scale;
  private boolean dragging;
  private int dragstart_mouse;
  private int dragstart_value;
  private int stepsize;
  private int pageSize;
  private ChangeListener change;

  public ScrollBar(int dir) {
    this.dir = dir;
    value = 0;
    viewsize = 10;
    fullsize = 100;
    stepsize = 1;
    pageSize = 3;
  }

  public Dimension getMinSize() {
    switch (dir) {
      case Direction.VERTICAL:
        if (getWidth() < 32) {
          setSize(32, 16);
        }
        break;
      case Direction.HORIZONTAL:
        if (getHeight() < 32) {
          setSize(16, 32);
        }
        break;
    }
    return super.getMinSize();
  }

  public int getFullsize() {
    return fullsize;
  }

  public void setFullsize(int size) {
    fullsize = size;
  }

  public int getViewsize() {
    return viewsize;
  }

  public void setViewsize(int size) {
    viewsize = size;
  }

  public int getStepsize() {
    return stepsize;
  }

  public void setStepsize(int size) {
    stepsize = size;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public void render(Image image) {
    super.render(image);
    int x1 = getX();
    int y1 = getY();
    int w = getWidth();
    int h = getHeight();
    switch (dir) {
      case Direction.VERTICAL: {
        image.drawImageBlend(Icons.getArrowUp(), x1, y1, true);

        //draw bar/button
        y1 += 16;
        float barsize = h - 34;
        scale = barsize / (float)fullsize;
        float p1 = value;
        float p2 = value + viewsize;
        p1 *= scale;
        p2 *= scale;
        v1 = (int)p1;
        v2 = (int)p2;
        image.drawBox(x1 + 0,y1     ,16,h - 32);
        y1++;
        image.setForeColor(Color.RED);
        image.drawBox(x1 + 1,y1 + v1,14,v2 - v1 + 1);
        y1 += h - 33;

        image.drawImageBlend(Icons.getArrowDown(), x1, y1, true);
        break;
      }
      case Direction.HORIZONTAL: {
        image.drawImageBlend(Icons.getArrowLeft(), x1, y1, true);

        //draw bar/button
        x1 += 16;
        float barsize = w - 34;
        scale = barsize / (float)fullsize;
        float p1 = value;
        float p2 = value + viewsize;
        p1 *= scale;
        p2 *= scale;
        v1 = (int)p1;
        v2 = (int)p2;
        image.drawBox(x1     ,y1 + 0,w - 16     ,16);
        x1++;
        image.setForeColor(Color.RED);
        image.drawBox(x1 + v1,y1 + 1,v2 - v1 + 1,14);
        x1 += w - 33;

        image.drawImageBlend(Icons.getArrowRight(), x1, y1, true);
        break;
      }
    }
    //adjust v1/v2 to fullsize of scrollbar
    v1 += 17;
    v2 += 17;
  }

  private void move(int steps) {
    if (fullsize <= viewsize) return;
    int newValue = value + (stepsize * steps);
    if (newValue < 0) newValue = 0;
    if (newValue + viewsize >= fullsize) newValue = fullsize - viewsize - 1;
    moveTo(newValue);
  }

  private void moveTo(int newValue) {
    if (fullsize <= viewsize) return;
    if (newValue + viewsize >= fullsize) newValue = fullsize - viewsize - 1;
    if (newValue < 0) newValue = 0;
    value = newValue;
    if (change != null) {
      change.changed(this);
    }
  }

  public void setChangeListener(ChangeListener listener) {
    change = listener;
  }

  public void mouseDown(int button) {
    if (button == MouseButton.LEFT) {
      int x1 = getX();
      int y1 = getY();
      int w = getWidth();
      int h = getHeight();
      int mx = getMouseX();
      int my = getMouseY();
      switch (dir) {
        case Direction.VERTICAL: {
          my -= y1;
          if (my <= 15) {
            //up button
            move(-1);
          } else if (my <= v1) {
            //page up
            move(-pageSize);
          } else if (my <= v2) {
            //button
            dragging = true;
            dragstart_mouse = my;
            dragstart_value = value;
          } else if (my <= h - 16) {
            //page down
            move(pageSize);
          } else {
            //down button
            move(1);
          }
          break;
        }
        case Direction.HORIZONTAL: {
          mx -= x1;
          if (mx <= 15) {
            //left button
            move(-1);
          } else if (mx <= v1) {
            //page left
            move(-pageSize);
          } else if (mx <= v2) {
            //button
            dragging = true;
            dragstart_mouse = mx;
            dragstart_value = value;
          } else if (mx <= h - 16) {
            //page right
            move(pageSize);
          } else {
            //right button
            move(1);
          }
          break;
        }
      }
    }
    super.mouseDown(button);
  }

  public void mouseUp(int button) {
    if (button == MouseButton.LEFT) {
      dragging = false;
    }
    super.mouseUp(button);
  }

  public void mouseMove(int x, int y) {
    super.mouseMove(x, y);
    int x1 = getX();
    int y1 = getY();
    int w = getWidth();
    int h = getHeight();
    int mx = getMouseX();
    int my = getMouseY();
    if (dragging) {
      switch (dir) {
        case Direction.VERTICAL: {
          my -= y1;
          float p = my - dragstart_mouse;
          p /= scale;
          moveTo(dragstart_value + (int)p);
          break;
        }
        case Direction.HORIZONTAL: {
          mx -= x1;
          float p = mx - dragstart_mouse;
          p /= scale;
          moveTo(dragstart_value + (int)p);
          break;
        }
      }
    }
  }
}

package javaforce.webui;

/** Canvas for 2D and 3D output.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.awt.JFImage;
import javaforce.JFLog;

public class Canvas extends Container {
  private Rectangle rect;
  public Canvas() {
    setStyle("contentEditable", "true");
    setStyle("outline", "none");
    addAttr("tabindex", "1");
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<canvas" + getAttrs());
    //the width and height must be specified
    sb.append(" width='" + width + "'");
    sb.append(" height='" + height + "'");
    sb.append(">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      sb.append(get(a).html());
    }
    sb.append("</canvas>");
    return sb.toString();
  }
  /** Tells the client to start WebGL on canvas. */
  public void initWebGL() {
    sendEvent("initwebgl", null);
  }
  /** Enables user drawn rectangle on canvas. Use addChangedListener() to get notification. */
  public void enableDrawRect() {
    addEvent("onmousedown", "onMouseDownCanvas(event, this);");
    addEvent("onmouseup", "onMouseUpCanvas(event, this);");
    addEvent("onmousemove", "onMouseMoveCanvasDrawRect(event, this);");
  }
  /** Returns coordinates of last user drawn rectangle. */
  public Rectangle getRect() {
    return rect;
  }
  /** Draw a rectangle on canvas. */
  public void drawRect(int clr, Rectangle rect) {
    sendEvent("drawrect", new String[] {"clr=#" + String.format("%06x", clr), "x=" + rect.x, "y=" + rect.y, "w=" + rect.width, "h=" + rect.height});
  }
  /** Draw an image on canvas. */
  public void drawImage(JFImage image, Point at) {
    try {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      image.savePNG(os);
      byte[] data = os.toByteArray();
      sendData(data);
      sendEvent("drawimage", new String[] {"x=" + at.x, "y=" + at.y});
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void onDrawRect(Rectangle rect) {
    this.rect = rect;
    onChanged(null);
  }
}

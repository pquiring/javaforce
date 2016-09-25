package javaforce.webui;

/** Table
 *
 * @author pquiring
 */

public class Table extends Container {
  private int width, height, cols, rows;
  private boolean border;
  public Table(int width, int height, int cols, int rows) {
    this.width = width;  //X
    this.height = height;  //Y
    this.cols = cols;  //X
    this.rows = rows;  //Y
  }
  public void setBorder(boolean state) {
    border = state;
  }
  private int get(Component comp, String key, int def) {
    Integer i = (Integer)comp.getProperty(key);
    if (i == null) return def;
    return i;
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<table id='" + id + "'");
    if (border) sb.append(" class='border'");
    sb.append(" style='width: " + (width * cols) + "px; height: " + (height * rows) + "px;'");
    sb.append(">");
    for(int y=0;y<rows;y++) {
      sb.append("<tr>");
      for(int x=0;x<cols;x++) {
        Component c = get(x,y);
        if (c != null) {
          int spanx = get(c, "spanx", 1);
          int spany = get(c, "spany", 1);
          sb.append("<td");
          if (border) {
            sb.append(" class='border'");
          }
          if (spanx > 1) {
            sb.append(" colspan=" + spanx);
          }
          if (spany > 1) {
            sb.append(" rowspan=" + spany);
          }
          int w = width * spanx;
          int h = height * spany;
          sb.append(" style='width:" + w + "px; height:" + h + "px;'");
          sb.append(">");
          sb.append(c.html());
          sb.append("</td>");
        } else {
          sb.append("<td style='width: " + width + "px; height: " + height + "px;'></td>");
        }
      }
      sb.append("</tr>");
    }
    sb.append("</table>");
    return sb.toString();
  }
  private Component get(int x,int y) {
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Component c = get(a);
      int cx = get(c, "x", -1);
      int cy = get(c, "y", -1);
      if (cx == x && cy == y) return c;
    }
    return null;
  }
  public void add(Component comp, int x, int y) {
    comp.setProperty("x", x);
    comp.setProperty("y", y);
    add(comp);
  }
  public void add(Component comp, int x, int y, int spanx, int spany) {
    comp.setProperty("x", x);
    comp.setProperty("y", y);
    comp.setProperty("spanx", spanx);
    comp.setProperty("spany", spany);
    add(comp);
  }
}

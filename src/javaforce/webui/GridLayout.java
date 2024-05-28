package javaforce.webui;

/** Grid Layout
 *
 * @author Peter Quiring
 */

public class GridLayout extends LayoutManager {
  private int cols, rows;
  private boolean border;
  private int[] layouts;  //column layouts

  /** GridLayout
   *
   * Default alignment for cells is CENTER.
   *
   * @param cols = # columns
   * @param rows = # rows
   */
  public GridLayout(int cols, int rows) {
    this.cols = cols;
    this.rows = rows;
    layouts = new int[cols];
    for(int y=0;y<cols;y++) {
      layouts[y] = CENTER;
    }
  }

  /** GridLayout
   *
   * @param cols = # columns
   * @param rows = # rows
   * @param align_cols = array of column align types (LEFT, CENTER or RIGHT)
   */
  public GridLayout(int cols, int rows, int[] align_cols) {
    this.cols = cols;
    this.rows = rows;
    layouts = new int[cols];
    if (align_cols.length != cols) {
      for(int y=0;y<cols;y++) {
        layouts[y] = CENTER;
      }
    } else {
      for(int y=0;y<cols;y++) {
        layouts[y] = align_cols[y];
      }
    }
  }

  private String align_classes[] = {
    "none", "left", "center", "right"
  };

  private class Cell extends Block {
    public int x,y,spanx,spany;
    private boolean border;
    private int align;

    public Cell(Component comp, int align, boolean border) {
      this.border = border;
      this.align = align;
      setClass(align_classes[align]);
      add(comp);
      if (border) {
        addClass("border");
      }
    }
    public String html() {
      StringBuilder sb = new StringBuilder();
      sb.append("<div" + getAttrs() + ">");
      sb.append(get(0).html());
      sb.append("</div>");
      return sb.toString();
    }
    public void setBorder(boolean state) {
      border = state;
      if (border) {
        addClass("border");
      } else {
        removeClass("border");
      }
    }
    public Component getComponent() {
      return get(0);
    }
  }

  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<table" + getAttrs() + ">");
    for(int y=0;y<rows;y++) {
      sb.append("<tr>");
      for(int x=0;x<cols;x++) {
        Cell cell = getCell(x, y, false);
        if (cell == null) continue;
        sb.append("<td>");
        sb.append(cell.html());
        sb.append("</td>");
      }
      sb.append("</tr>");
    }
    sb.append("</table>");
    return sb.toString();
  }

  public void setBorder(boolean state) {
    border = state;
    if (border) {
      addClass("border");
    } else {
      removeClass("border");
    }
  }

  private Cell getCell(int x,int y,boolean checkSpans) {
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Cell cell = (Cell)get(a);
      int x1 = cell.x;
      int y1 = cell.y;
      int x2 = x1;
      int y2 = y1;
      if (checkSpans) {
        x2 += cell.spanx - 1;
        y2 += cell.spany - 1;
      }
      if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
        return cell;
      }
    }
    return null;
  }
  public Component get(int x,int y,boolean checkSpans) {
    Cell cell = getCell(x,y,checkSpans);
    if (cell == null) return null;
    return cell.get(0);
  }
  public int getRows() {
    return rows;
  }
  public int getColumns() {
    return cols;
  }

  public void add(Component comp, int x, int y) {
    add(comp,x,y,1,1);
  }
  public void add(Component comp, int x, int y, int spanx, int spany) {
    Cell cell = new Cell(comp, layouts[x], border);
    cell.x = x;
    cell.y = y;
    cell.spanx = spanx;
    cell.spany = spany;
    add(cell);
  }
  public void addRow() {
    rows++;
  }
  public void addRow(Component[] cmps) {
    int x = 0;
    int y = rows;
    addRow();
    for(Component c : cmps) {
      add(c, x, y);
      x++;
    }
  }
  public void addRow(String[] strs) {
    int x = 0;
    int y = rows;
    addRow();
    for(String s : strs) {
      add(new Label(s), x, y);
      x++;
    }
  }
  public void addColumn() {
    cols++;
  }
  public void addColumn(Component[] cmps) {
    int x = cols;
    int y = 0;
    addColumn();
    for(Component c : cmps) {
      add(c, x, y);
      y++;
    }
  }
  public void addColumn(String[] strs) {
    int x = cols;
    int y = 0;
    addColumn();
    for(String s : strs) {
      add(new Label(s), x, y);
      y++;
    }
  }
}

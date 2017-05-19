package javaforce.webui;

/** Table
 *
 * @author pquiring
 */

public class Table extends Container {
  private int cellWidth, cellHeight, cols, rows;
  private boolean border;
  public Table(int cellWidth, int cellHeight, int cols, int rows) {
    this.cellWidth = cellWidth;  //X
    this.cellHeight = cellHeight;  //Y
    this.cols = cols;  //X
    this.rows = rows;  //Y
    setClass("table");
    setSize();
  }
  private class Cell extends Container {
    public int x,y,spanx,spany;
    public Cell(Component comp) {
      setClass("cell");
      add(comp);
    }
    public String html() {
      setSize(spanx * cellWidth, spany * cellHeight);
      //setPosition(x * width, y * height);
      setStyle("left", Integer.toString(x * cellWidth));
      setStyle("top", Integer.toString(y * cellHeight));
      StringBuffer sb = new StringBuffer();
      sb.append("<div" + getAttrs() + ">");
      sb.append(get(0).html());
      sb.append("</div>");
      return sb.toString();
    }
  }
  private void setSize() {
    setSize(cellWidth * cols, cellHeight * rows);
    sendEvent("setsize", new String [] {"w=" + (cellWidth * cols), "h=" + (cellHeight * rows)});
  }
  public void setBorder(boolean state) {
    border = state;
    if (border) {
      addClass("border");
    } else {
      removeClass("border");
    }
  }
  public String html() {
    StringBuffer sb = new StringBuffer();
    //using an actualy <table> proved to be too difficult once spans where implemented
    sb.append("<div" + getAttrs() + ">");
    for(int y=0;y<rows;y++) {
      for(int x=0;x<cols;x++) {
        Cell cell = getCell(x,y,false);
        if (cell == null) continue;
        sb.append(cell.html());
      }
    }
    sb.append("</div>");
    return sb.toString();
  }
  public void add(Component comp, int x, int y) {
    add(comp,x,y,1,1);
  }
  public void add(Component comp, int x, int y, int spanx, int spany) {
    String html;
    Cell cell = new Cell(comp);
    cell.x = x;
    cell.y = y;
    cell.spanx = spanx;
    cell.spany = spany;
    add(cell);
  }
  public void addRow() {
    rows++;
    setSize();
  }
  public void addColumn() {
    cols++;
    setSize();
  }
  /** Sets number of columns and rows. */
  public void setTableSize(int cols, int rows) {
    this.cols = cols;
    this.rows = rows;
    setSize();
  }
  public Component remove(int x,int y) {
    Cell cell = getCell(x,y,false);
    if (cell != null) {
      remove(cell);
      sendEvent("remove", new String[] {"child=" + cell.id});
      return cell.get(0);
    }
    return null;
  }
  public void setSpans(int x,int y,int spanx, int spany) {
    Cell cell = getCell(x,y,false);
    if (cell == null) return;
    cell.spanx = spanx;
    cell.spany = spany;
    cell.sendEvent("setsize", new String[] {"w=" + spanx * cellWidth, "h=" + spany * cellHeight});
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
}

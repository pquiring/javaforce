package javaforce.webui;

/** Table
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class Table extends Container implements Click {
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
  private class Cell extends Block {
    public int x,y,spanx,spany;
    private boolean border;
    public Cell(Component comp, boolean border) {
      this.border = border;
      setClass("cell");
      add(comp);
      if (border) {
        addClass("border");
      }
    }
    public String html() {
      setSize(spanx * cellWidth, spany * cellHeight);
      //setPosition(x * width, y * height);
      setStyle("left", Integer.toString(x * cellWidth));
      setStyle("top", Integer.toString(y * cellHeight));
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
    private boolean selected;
    public void setSelected(boolean state) {
      selected = state;
      if (state) {
        getComponent().sendEvent("addclass", new String[] {"cls=selected"});
      } else {
        getComponent().sendEvent("delclass", new String[] {"cls=selected"});
      }
    }
    public boolean isSelected() {
      return selected;
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
    StringBuilder sb = new StringBuilder();
    //using an actualy <table> proved to be too difficult once spans where implemented
    sb.append("<div" + getAttrs() + ">");
    Component[] cells = getAll();
    for(int a=0;a<cells.length;a++) {
      Cell cell = (Cell)cells[a];
      sb.append(cell.html());
    }
    sb.append("</div>");
    return sb.toString();
  }
  public void add(Component comp, int x, int y) {
    add(comp,x,y,1,1);
  }
  public void add(Component comp, int x, int y, int spanx, int spany) {
    Cell cell = new Cell(comp, border);
    cell.x = x;
    cell.y = y;
    cell.spanx = spanx;
    cell.spany = spany;
    add(cell);
    init(comp);
  }
  private void init(Component cmp) {
    cmp.addClickListener(this);
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
  public void remove(Component c) {
    //find cell with c
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Cell cell = (Cell)get(a);
      if (cell.get(0) == c) {
        super.remove(cell);
        cell.sendEvent("remove", new String[] {"child=" + cell.id});
        return;
      }
    }
  }
  public Component remove(int x,int y) {
    Cell cell = getCell(x,y,false);
    if (cell != null) {
      super.remove(cell);
      cell.sendEvent("remove", new String[] {"child=" + cell.id});
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
  private Cell getCell(int idx) {
    return (Cell)get(idx);
  }
  public int getSelectedRow() {
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Cell cell = getCell(idx);
      if (cell.isSelected()) return cell.y;
    }
    return -1;
  }
  public int getSelectedColumn() {
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Cell cell = getCell(idx);
      if (cell.isSelected()) return cell.x;
    }
    return -1;
  }
  public void onClick(MouseEvent me, Component cmp) {
    Cell cell = (Cell)cmp.getParent();
    if (cell.y == 0) return;  //header row
    if (me.ctrlKey) {
      cell.setSelected(!cell.isSelected());
    } else {
      //clear all other items
      int cnt = count();
      for(int idx=0;idx<cnt;idx++) {
        Cell o = getCell(idx);
        o.setSelected(o == cell);
      }
    }
    onChanged(null);
  }
}

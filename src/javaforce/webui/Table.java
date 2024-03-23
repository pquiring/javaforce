package javaforce.webui;

/** Table
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.webui.event.*;

public class Table extends Container implements Click {
  private int cellWidth, cellHeight, cols, rows;
  private boolean border;
  private boolean has_header;
  private int sel_mode;
  private int[] cellWidths;

  public static final int SELECT_CELL = 0;
  public static final int SELECT_ROW = 1;
  public static final int SELECT_COLUMN = 2;

  /** Create a table with uniform cells. */
  public Table(int cellWidth, int cellHeight, int cols, int rows) {
    this.cellWidth = cellWidth;  //X
    this.cellHeight = cellHeight;  //Y
    this.cols = cols;  //X
    this.rows = rows;  //Y
    setClass("table");
    setSize();
  }
  /** Create a table with different widths per column. */
  public Table(int[] cellWidths, int cellHeight, int cols, int rows) {
    this.cellWidths = cellWidths;  //X
    this.cellHeight = cellHeight;  //Y
    this.cols = cellWidths.length;  //X
    this.rows = rows;  //Y
    setClass("table");
    setSize();
  }
  private int getColWidth(int col) {
    if (cellWidths == null) {
      return cellWidth;
    } else {
      return cellWidths[col];
    }
  }
  private int getColPosition(int col) {
    if (cellWidths == null) {
      return col * cellWidth;
    } else {
      int total = 0;
      for(int x = 0;x<col;x++) {
        total += cellWidths[x];
      }
      return total;
    }
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
      setSize(spanx * getColWidth(x), spany * cellHeight);
      //setPosition(x * width, y * height);
      setStyle("left", Integer.toString(getColPosition(x)));
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
  private int getTotalWidth() {
    if (cellWidths == null) {
      return cellWidth * cols;
    } else {
      int total = 0;
      for(int a=0;a<cellWidths.length;a++) {
        total += cellWidths[a];
      }
      return total;
    }
  }
  private int getTotalHeight() {
    return cellHeight * rows;
  }
  private void setSize() {
    int totalWidth = getTotalWidth();
    int totalHeight = getTotalHeight();
    setSize(totalWidth, totalHeight);
    sendEvent("setsize", new String [] {"w=" + totalWidth, "h=" + totalHeight});
  }
  public void setBorder(boolean state) {
    border = state;
    if (border) {
      addClass("border");
    } else {
      removeClass("border");
    }
  }
  /** Sets selection mode : SELECT_... */
  public void setSelectionMode(int mode) {
    this.sel_mode = mode;
  }
  public int getSelectionMode() {
    return sel_mode;
  }
  public void setHeader(boolean state) {
    has_header = state;
  }
  public boolean getHeader() {
    return has_header;
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
    if (cellWidths != null) {
      return;
    }
    cols++;
    setSize();
  }
  public void addColumn(int width) {
    if (cellWidths == null) {
      return;
    }
    int idx = cellWidths.length;
    cellWidths = Arrays.copyOf(cellWidths, cellWidths.length + 1);
    cellWidths[idx] = width;
    cols++;
    setSize();
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
  public void removeRow(int row) {
    int cnt = count();
    if (has_header) row++;
    for(int a=0;a<cnt;) {
      Cell cell = (Cell)get(a);
      Component c = cell.get(0);
      if (cell.y == row) {
        remove(c);
        cnt--;
      } else {
        if (cell.y > row) {
          cell.y--;
        }
        a++;
      }
    }
    rows--;
  }
  public void removeColumn(int col) {
    int cnt = count();
    for(int a=0;a<cnt;) {
      Cell cell = (Cell)get(a);
      Component c = cell.get(0);
      if (cell.x == col) {
        remove(c);
        cnt--;
      } else {
        if (cell.x > col) {
          cell.x--;
        }
        a++;
      }
    }
    cols--;
  }
  public void removeAll() {
    super.removeAll();
    rows = 0;
    setSize();
  }
  public void setSpans(int x,int y,int spanx, int spany) {
    if (cellWidths != null) {
      //TODO
      return;
    }
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
      if (cell.isSelected()) return has_header ? cell.y - 1 : cell.y;
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
    if (has_header && cell.y == 0) return;
    if (me.ctrlKey) {
      switch (sel_mode) {
        case SELECT_CELL:
          cell.setSelected(!cell.isSelected());
          break;
        case SELECT_ROW: {
          int y = cell.y;
          int cnt = count();
          for(int idx=0;idx<cnt;idx++) {
            Cell o = getCell(idx);
            if (cell.y == y) {
              o.setSelected(!o.isSelected());
            }
          }
          break;
        }
        case SELECT_COLUMN: {
          int x = cell.x;
          int cnt = count();
          for(int idx=0;idx<cnt;idx++) {
            Cell o = getCell(idx);
            if (cell.x == x) {
              o.setSelected(!o.isSelected());
            }
          }
          break;
        }
      }
    } else {
      switch (sel_mode) {
        case SELECT_CELL: {
          int cnt = count();
          for(int idx=0;idx<cnt;idx++) {
            Cell o = getCell(idx);
            o.setSelected(o == cell);
          }
          break;
        }
        case SELECT_ROW: {
          int y = cell.y;
          int cnt = count();
          for(int idx=0;idx<cnt;idx++) {
            Cell o = getCell(idx);
            o.setSelected(o.y == y);
          }
          break;
        }
        case SELECT_COLUMN: {
          int x = cell.x;
          int cnt = count();
          for(int idx=0;idx<cnt;idx++) {
            Cell o = getCell(idx);
            o.setSelected(o.x == x);
          }
          break;
        }
      }
    }
    onChanged(null);
  }
}

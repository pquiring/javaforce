package javaforce.webui;

/** ListBox
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class ListBox extends ScrollPanel implements Click {

  private class Cell extends Block {
    public Cell(Component cmp) {
      add(cmp);
    }
    public Component getComponent() {
      Component cmp = get(0);
      return cmp;
    }
    private boolean selected;
    public void setSelected(boolean state) {
      selected = state;
      if (state) {
        sendEvent("addclass", new String[] {"cls=selected"});
      } else {
        sendEvent("delclass", new String[] {"cls=selected"});
      }
    }
    public boolean isSelected() {
      return selected;
    }
  }

  public ListBox() {
    addClass("listbox");
    addClass("height100");
  }

  public void update(int idx, String text) {
    Component c = getCell(idx).getComponent();
    if (!(c instanceof Label)) return;
    Label lbl = (Label)c;
    lbl.setText(text);
  }

  public void add(String item) {
    add(new Label(item));
  }

  public void add(int idx, String item) {
    add(idx, new Label(item));
  }

  public void add(Component item) {
    Cell cell = new Cell(item);
    cell.addClickListener(this);
    super.add(count(), cell);
  }

  public void add(int idx, Component item) {
    Cell cell = new Cell(item);
    cell.addClickListener(this);
    super.add(idx, cell);
  }

  private Cell getCell(int idx) {
    return (Cell)get(idx);
  }

  private Component getComponent(int idx) {
    return getCell(idx).getComponent();
  }

  public int getSelectedIndex() {
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Cell cell = getCell(idx);
      if (cell.isSelected()) return idx;
    }
    return -1;
  }

  public int getSelectedCount() {
    int selCount = 0;
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Cell cell = getCell(idx);
      if (cell.isSelected()) selCount++;
    }
    return selCount;
  }

  public int[] getSelectedIndices() {
    int selCount = getSelectedCount();
    int[] selIdx = new int[selCount];
    int selPos = 0;
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Cell cell = getCell(idx);
      if (cell.isSelected()) {
        selIdx[selPos++] = idx;
      }
    }
    return selIdx;
  }

  public String getSelectedItem() {
    int idx = getSelectedIndex();
    if (idx == -1) return null;
    Component cmp = getComponent(idx);
    if (cmp instanceof Label) {
      Label l = (Label)cmp;
      return l.text;
    }
    return cmp.toString();
  }

  public Component getSelectedComponent() {
    int idx = getSelectedIndex();
    if (idx == -1) return null;
    return getComponent(idx);
  }

  public void setSelectedIndex(int selidx) {
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Cell cell = getCell(idx);
      cell.setSelected(idx == selidx);
    }
  }

  public void onClick(MouseEvent me, Component cmp) {
    Cell cell = (Cell)cmp;
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

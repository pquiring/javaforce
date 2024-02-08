package javaforce.webui;

/** ListBox
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.event.*;

public class ListBox extends ScrollPanel implements Click {

  private static class Cell extends Block {
    public Cell(Component cmp) {
      add(cmp);
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
    init(item);
    super.add(new Cell(item));
  }

  public void add(int idx, Component item) {
    init(item);
    super.add(idx, new Cell(item));
  }

  private Cell getCell(int idx) {
    return (Cell)get(idx);
  }

  private Component getComponent(int idx) {
    return getCell(idx).getComponent();
  }

  private void init(Component cmp) {
    cmp.addClickListener(this);
  }

  public void init() {
    super.init();
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Component cmp = getComponent(idx);
      init(cmp);
    }
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
    Cell cell = (Cell)cmp.getParent();
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

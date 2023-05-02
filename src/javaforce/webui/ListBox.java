package javaforce.webui;

/** ListBox
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class ListBox extends ScrollPanel implements Click {

  public ListBox() {
    addClass("listbox");
    addClass("height100");
  }

  public void update(int idx, String text) {
    Component c = get(idx);
    if (!(c instanceof Label)) return;
    Label lbl = (Label)c;
    lbl.setText(text);
  }

  public void add(String item) {
    add(new Label(item));
  }

  public void add(Component item) {
    item.addClass("width100");
    item.setProperty("selected", "false");
    item.addClass("listboxitem");
    if (item.id != null) {
      init(item);
    }
    super.add(item);
  }

  public void add(int idx, Component item) {
    item.addClass("width100");
    item.setProperty("selected", "false");
    item.addClass("listboxitem");
    if (item.id != null) {
      init(item);
    }
    super.add(idx, item);
  }

  private void init(Component cmp) {
    cmp.addEvent("onclick", "onClick(event, " + cmp.id + ");");
    cmp.addClickListener(this);
  }

  public void init() {
    super.init();
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Component cmp = get(idx);
      init(cmp);
    }
  }

  public int getSelectedIndex() {
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Component cmp = get(idx);
      if (cmp.getProperty("selected").equals("true")) return idx;
    }
    return -1;
  }

  public int getSelectedCount() {
    int selCount = 0;
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Component cmp = get(idx);
      if (cmp.getProperty("selected").equals("true")) selCount++;
    }
    return selCount;
  }

  public int[] getSelectedIndices() {
    int selCount = getSelectedCount();
    int[] selIdx = new int[selCount];
    int selPos = 0;
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Component cmp = get(idx);
      if (cmp.getProperty("selected").equals("true")) {
        selIdx[selPos++] = idx;
      }
    }
    return selIdx;
  }

  public String getSelectedItem() {
    int idx = getSelectedIndex();
    if (idx == -1) return null;
    Component cmp = get(idx);
    if (cmp instanceof Label) {
      Label l = (Label)cmp;
      return l.text;
    }
    return cmp.toString();
  }

  public Component getSelectedComponent() {
    int idx = getSelectedIndex();
    if (idx == -1) return null;
    return get(idx);
  }

  public void setSelectedIndex(int selidx) {
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Component cmp = get(idx);
      if (idx == selidx) {
        if (cmp.getProperty("selected").equals("false")) {
          cmp.setProperty("selected", "true");
          cmp.sendEvent("addclass", new String[] {"cls=selected"});
        }
      } else {
        if (cmp.getProperty("selected").equals("true")) {
          cmp.setProperty("selected", "false");
          cmp.sendEvent("delclass", new String[] {"cls=selected"});
        }
      }
    }
  }

  public void onClick(MouseEvent me, Component cmp) {
    System.out.println("ListBox.onClick");
    String selected = (String)cmp.getProperty("selected");
    if (selected.equals("false")) {
      cmp.setProperty("selected", "true");
      cmp.sendEvent("addclass", new String[] {"cls=selected"});
    } else {
      cmp.setProperty("selected", "false");
      cmp.sendEvent("delclass", new String[] {"cls=selected"});
    }
    if (!me.ctrlKey) {
      //clear all other items
      ListBox list = (ListBox)cmp.parent;
      int cnt = list.count();
      for(int idx=0;idx<cnt;idx++) {
        Component o = list.get(idx);
        if (o == cmp) continue;
        selected = (String)o.getProperty("selected");
        if (selected.equals("true")) {
          o.setProperty("selected", "false");
          o.sendEvent("delclass", new String[] {"cls=selected"});
        }
      }
    }
    System.out.println("ListBox.onChanged");
    onChanged(null);
  }
}

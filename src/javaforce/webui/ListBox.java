package javaforce.webui;

/** ListBox
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class ListBox extends ScrollPanel implements Click {
  public ListBox() {
    addClass("listbox");
  }

  public void add(String item) {
    add(new Label(item));
  }

  public void update(int idx, String text) {
    Component c = get(idx);
    if (!(c instanceof Label)) return;
    Label lbl = (Label)c;
    lbl.setText(text);
  }

  public void add(Component item) {
    item.addClass("width100");
    item.setProperty("selected", "false");
    item.addClass("listboxitem");
    super.add(item);
  }

  public void init() {
    super.init();
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      Component c = get(a);
      c.addEvent("onclick", "onClick(event, " + c.id + ");");
      c.addClickListener(this);
    }
  }

  public int getSelectedIndex() {
    int cnt = count();
    for(int idx=0;idx<cnt;idx++) {
      Component c = get(idx);
      if (c.getProperty("selected").equals("true")) return idx;
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
      Component c = get(idx);
      if (idx == selidx) {
        if (c.getProperty("selected").equals("false")) {
          c.setProperty("selected", "true");
          c.sendEvent("addclass", new String[] {"cls=selected"});
        }
      } else {
        if (c.getProperty("selected").equals("true")) {
          c.setProperty("selected", "false");
          c.sendEvent("delclass", new String[] {"cls=selected"});
        }
      }
    }
  }

  public void onClick(MouseEvent e, Component c) {
    String selected = (String)c.getProperty("selected");
    if (selected.equals("false")) {
      c.setProperty("selected", "true");
      c.sendEvent("addclass", new String[] {"cls=selected"});
    } else {
      c.setProperty("selected", "false");
      c.sendEvent("delclass", new String[] {"cls=selected"});
    }
    if (!e.ctrlKey) {
      //clear all other items
      ListBox list = (ListBox)c.parent;
      int cnt = list.count();
      for(int a=0;a<cnt;a++) {
        Component o = list.get(a);
        if (o == c) continue;
        selected = (String)o.getProperty("selected");
        if (selected.equals("true")) {
          o.setProperty("selected", "false");
          o.sendEvent("delclass", new String[] {"cls=selected"});
        }
      }
    }
    onChanged(null);
  }
}

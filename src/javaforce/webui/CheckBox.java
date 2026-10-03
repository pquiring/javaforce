package javaforce.webui;

import javaforce.webui.event.*;

/** CheckBox
 *
 * @author pquiring
 */

public class CheckBox extends Container {
  private HTMLContainer input;
  private HTMLContainer label;
  private boolean selected;

  public CheckBox(String text) {
    input = new HTMLContainer("input");
    input.setEnclosed(false);
    input.addAttr("type", "checkbox");
    input.addEvent("onchange", "onCheckBoxChange(event, this);");
    input.addChangedListener((c) -> {
      selected = !selected;
      onChanged(new String[0]);
    });
    label = new HTMLContainer("label");
    label.setText(text);
    input.add(label);
    add(input);
    setClass("noselect");
  }

  public String html() {
    label.addAttr("for", "'" + input.id + "'");
    if (selected) {
      input.addAttr("checked", null);
    }
    StringBuilder html = new StringBuilder();
    html.append("<div" + getAttrs() + ">");
    int cnt = count();
    for(int a=0;a<cnt;a++) {
      html.append(get(a).html());
    }
    html.append("</div>");
    return html.toString();
  }
  public void setText(String text) {
    label.setText(text);
  }
  public void setSelected(boolean state) {
    if (selected == state) return;
    selected = state;
    input.sendEvent("setchecked", new String[] {"state=" + selected});
  }
  public boolean isSelected() {
    return selected;
  }
  public void setReadonly(boolean state) {
    input.setReadonly(state);
  }
  public void setDisabled(boolean state) {
    input.setDisabled(state);
  }
  public void addChangedListener(Changed handler) {
    input.addChangedListener(handler);
  }
  public void addClickListener(Click handler) {
    input.addClickListener(handler);
  }
}

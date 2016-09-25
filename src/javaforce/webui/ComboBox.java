package javaforce.webui;

/** ComboBox
 *
 * @author pquiring
 */

import java.util.*;

public class ComboBox extends Component {
  public ComboBox() {
    addEvent("onchange", "onComboBoxChange(this);");
  }

  private ArrayList<String> values = new ArrayList<String>();
  private ArrayList<String> texts = new ArrayList<String>();
  private int def = -1;

  private int index = -1;

  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<select id='" + id + "'" + getEvents() + ">");
    int cnt = values.size();
    for(int a=0;a<cnt;a++) {
      sb.append("<option value='" + values.get(a) + "'" + (a == def ? " selected" : "") + ">");
      sb.append(texts.get(a));
      sb.append("</option>");
    }
    sb.append("</select>");
    if (def != -1) {
      index = def;
    } else {
      index = 0;
    }
    return sb.toString();
  }

  public void add(String value, String text) {
    values.add(value);
    texts.add(text);
  }

  public void setDefault(int idx) {
    def = idx;
  }

  public int getSelected() {
    return index;
  }

  public void dispatchEvent(String event, String args[]) {
    if (event.equals("changed")) {
      int idx = args[0].indexOf("=");
      index = Integer.valueOf(args[0].substring(idx+1));
      if (change != null) change.onChange(this);
    }
  }

  private Change change;
  public void addChangeListener(Change handler) {
    change = handler;
  }
  public static interface Change {
    public void onChange(ComboBox combo);
  }
}

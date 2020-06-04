package javaforce.webui;

/** ComboBox
 *
 * @author pquiring
 */

import java.util.*;

public class ComboBox extends Component {
  public ComboBox() {
    addEvent("onchange", "onComboBoxChange(event, this);");
  }

  private ArrayList<String> values = new ArrayList<String>();
  private ArrayList<String> texts = new ArrayList<String>();

  private int index = -1;

  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<select" + getAttrs() + ">");
    int cnt = values.size();
    for(int a=0;a<cnt;a++) {
      sb.append("<option value='" + values.get(a) + "'" + (a == index ? " selected" : "") + ">");
      sb.append(texts.get(a));
      sb.append("</option>");
    }
    sb.append("</select>");
    return sb.toString();
  }

  public void add(String value, String text) {
    if (index == -1) index = 0;
    values.add(value);
    texts.add(text);
    if (client != null) {
      sendEvent("addoption", new String[] {"value=" + value, "text=" + text});
    }
  }

  public void clear() {
    for(int a=0;a<values.size();a++) {
      sendEvent("removeoption", new String[] {"idx=" + a});
    }
    index = -1;
    values.clear();
    texts.clear();
  }

  public int getSelectedIndex() {
    return index;
  }

  public String getSelectedValue() {
    int idx = getSelectedIndex();
    if (idx == -1) return null;
    return values.get(idx);
  }

  public String getSelectedText() {
    int idx = getSelectedIndex();
    if (idx == -1) return null;
    return texts.get(idx);
  }

  public void setSelectedIndex(int idx) {
    index = idx;
    sendEvent("setidx", new String[] {"idx=" + idx});
  }

  public void onChanged(String[] args) {
    int idx = args[0].indexOf("=");
    index = Integer.valueOf(args[0].substring(idx+1));
    super.onChanged(args);
  }
}

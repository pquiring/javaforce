package javaforce.webui;

/** ToggleButton
 *
 * Toggle button.
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class ToggleButton extends TextComponent {
  private boolean state;
  private int clrOff, clrOn;
  private Image img;
  public ToggleButton(String text) {
    this.text = text;
    setColors(Color.darkGrey, Color.grey);
    addEvent("onclick", "onClick(event, this);");
    setColor();
  }
  public ToggleButton(String text, int clrOff, int clrOn) {
    this.text = text;
    setColors(clrOff, clrOn);
    addEvent("onclick", "onClick(event, this);");
    setColor();
  }
  public ToggleButton(Resource img, String text) {
    this.img = new Image(img);
    add(this.img);
    this.text = text;
    setColors(Color.darkGrey, Color.grey);
    addEvent("onclick", "onClick(event, this);");
    setColor();
  }
  public ToggleButton(Resource img, String text, int clrOff, int clrOn) {
    this.img = new Image(img);
    add(this.img);
    this.text = text;
    setColors(clrOff, clrOn);
    addEvent("onclick", "onClick(event, this);");
    setColor();
  }
  private void setColor() {
    setBackColor(state ? clrOn : clrOff);
  }
  public void onClick(String[] args, MouseEvent me) {
    state = !state;
    setColor();
    onChanged(args);
    super.onClick(args, me);
  }
  public String html() {
    StringBuilder sb = new StringBuilder();
    sb.append("<button" + getAttrs() + ">");
    if (img != null) {
      sb.append(img.html());
    }
    if (text != null) {
      sb.append(text);
    }
    sb.append("</button>");
    return sb.toString();
  }
  public void setText(String text) {
    this.text = text;
    sendEvent("settext", new String[] {"text=" + text});
  }
  public void updateText(String text) {
    sendEvent("settext", new String[] {"text=" + text});
  }
  public void setColors(int clrOff, int clrOn) {
    this.clrOff = clrOff;
    this.clrOn = clrOn;
    setColor();
  }
  public void setSelected(boolean state) {
    this.state = state;
    setColor();
  }
  public boolean isSelected() {
    return state;
  }
}

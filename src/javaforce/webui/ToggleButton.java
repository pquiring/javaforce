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
  public ToggleButton(String text) {
    this.text = text;
    this.clrOff = Color.darkGrey;
    this.clrOn = Color.grey;
  }
  public ToggleButton(String text, int clrOff, int clrOn) {
    this.text = text;
    this.clrOff = clrOff;
    this.clrOn = clrOn;
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
    return "<button" + getAttrs() + ">" + text + "</button>";
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

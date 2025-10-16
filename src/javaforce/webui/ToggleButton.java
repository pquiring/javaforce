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
  private Icon icon;

  public ToggleButton(String text) {
    this.text = text;
    setColors(Color.darkGrey, Color.grey);
    addEvent("onclick", "onClick(event, this);");
    setColor();
    setClass("button");
  }
  public ToggleButton(String text, int clrOff, int clrOn) {
    this.text = text;
    setColors(clrOff, clrOn);
    addEvent("onclick", "onClick(event, this);");
    setColor();
    setClass("button");
  }
  public ToggleButton(Resource img, String text) {
    this.img = new Image(img);
    add(this.img);
    this.text = text;
    setColors(Color.darkGrey, Color.grey);
    addEvent("onclick", "onClick(event, this);");
    setColor();
    setClass("button");
  }
  public ToggleButton(Resource img, String text, int clrOff, int clrOn) {
    this.img = new Image(img);
    add(this.img);
    this.text = text;
    setColors(clrOff, clrOn);
    addEvent("onclick", "onClick(event, this);");
    setColor();
    setClass("button");
  }

  public ToggleButton(Icon icon, String text) {
    this.icon = icon;
    add(this.icon);
    this.text = text;
    setColors(Color.darkGrey, Color.grey);
    addEvent("onclick", "onClick(event, this);");
    setColor();
  }
  public ToggleButton(Icon icon, String text, int clrOff, int clrOn) {
    this.icon = icon;
    add(this.icon);
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
    StringBuilder html = new StringBuilder();
    html.append("<button" + getAttrs() + ">");
    if (img != null) {
      html.append(img.html());
    }
    if (icon != null) {
      html.append(icon.html());
    }
    if (text != null) {
      html.append(text);
    }
    html.append("</button>");
    return html.toString();
  }
  public void setText(String text) {
    this.text = text;
    sendEvent("settext", new String[] {"text=" + text});
  }
  public void update() {
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

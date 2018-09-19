/** Interface used to control the JFrame in jPhoneApp from jPhonePanel. */

public interface WindowController {
  public void setPanelSize();
  public void setPanelVisible();
  public void setPanelAlwaysOnTop(boolean state);
  public void setPosition();
}

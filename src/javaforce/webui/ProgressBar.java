package javaforce.webui;

/** Progress bar.
 *
 * @author pquiring
 */

public class ProgressBar extends Component {
  private int max;
  private int value;
  public ProgressBar(int max) {
    this.max = max;
    addAttr("max", "" + max);
  }
  public void setValue(int value) {
    this.value = value;
    addAttr("value", "" + value);
    client.sendEvent(id, "setvalue", new String[] {"value=" + value});
  }
  public String html() {
    return "<progress " + getAttrs() + "></progress>";
  }
}

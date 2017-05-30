package jfcontrols.tags;

/** Remote Tag
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;

public class RemoteTag extends MonitoredTag {

  private javaforce.controls.Tag remoteTag;

  public RemoteTag(int cid, String name, int type, boolean unsigned, boolean array_not_supported, SQL sql) {
    super(cid, name, type, unsigned, array_not_supported, sql);
    remoteTag = RemoteControllers.getTag(cid, name, type, sql);
  }

  public void updateRead(SQL sql) {
    String newValue = remoteTag.getValue();
    if (!newValue.equals(value)) {
      value = newValue;
      tagChanged();
    }
  }

  public void updateWrite(SQL sql) {
    if (dirty) {
      remoteTag.setValue(value);
      dirty = false;
    }
  }

  protected String readValue(int idx) {
    return null;
  }

  public String getValue(int idx) {
    return null;
  }

  public void setValue(String value, int idx) {
  }
}

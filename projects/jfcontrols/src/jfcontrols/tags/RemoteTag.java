package jfcontrols.tags;

/** Remote Tag
 *
 * @author pquiring
 */

import javaforce.*;

public class RemoteTag extends MonitoredTag {
  private int tid;
  private javaforce.controls.Tag remoteTag;
  private String value = "0";

  public RemoteTag(int cid, String name, int type, boolean unsigned, boolean array_not_supported, SQL sql) {
    super(type, unsigned, array_not_supported);
    tid = Integer.valueOf(sql.select1value("select id from tags where cid=" + cid + " and name=" + SQL.quote(name)));
    remoteTag = RemoteControllers.getTag(cid, name, type, sql);
  }

  public void updateRead(SQL sql) {
    String newValue = remoteTag.getValue();
    if (!newValue.equals(value)) {
      String oldValue = value;
      value = newValue;
      tagChanged(null, oldValue, newValue);
    }
  }

  public void updateWrite(SQL sql) {
    if (dirty) {
      remoteTag.setValue(value);
      dirty = false;
    }
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    dirty = true;
    this.value = value;
  }

  public TagBase getIndex(int idx) {
    JFLog.log("Error:RemoteTag.getIndex() called");
    return null;
  }

  public TagBase getMember(int idx) {
    JFLog.log("Error:RemoteTag.getMember() called");
    return null;
  }

  public int getMember(String name) {
    JFLog.log("Error:RemoteTag.getMember() called");
    return -1;
  }

  public int getTagID() {
    return tid;
  }

  public int getIndex() {
    return 0;
  }

  public boolean isMember() {
    return false;
  }

  public int getMember() {
    return 0;
  }

  public int getMemberIndex() {
    return 0;
  }
}

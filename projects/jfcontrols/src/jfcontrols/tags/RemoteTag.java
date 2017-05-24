package jfcontrols.tags;

/** Remote Tag
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.controls.*;

public class RemoteTag extends MonitoredTag {

  private javaforce.controls.Tag remoteTag;

  public RemoteTag(int cid, String name, int type, SQL sql) {
    super(cid, name, type, sql);
    remoteTag = RemoteControllers.getTag(cid, name, type, sql);
  }

  public void updateRead(SQL sql) {
    //TODO
  }

  public void updateWrite(SQL sql) {
    if (dirty) {
      //TODO
      dirty = false;
      tagChanged();
    }
  }
}
